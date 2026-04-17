package com.example.myapplication;

import static androidx.fragment.app.FragmentManager.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class ControlActivity extends AppCompatActivity implements Menuable, Notifiable{

    private Fragment[] tabFragments = { new Screen1Fragment(), new Screen2Fragment(),
            new Screen3Fragment(), new Screen4Fragment(),
            new Screen5Fragment(), new Screen6Fragment(),
            new Screen7Fragment() };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_control);

        Intent intent = getIntent();                                       // récupérer l'intent qui a lancé l'activité
        int menuNumber = intent.getIntExtra("index", 0);      // Récupérer l'index envoyé par MainActivity

        //envoyer l'index a MenuFragment via Bundle
        Bundle args = new Bundle();
        args.putInt("index", menuNumber);

        MenuFragment menuFragment = new MenuFragment();
        menuFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_menu, menuFragment)
                .replace(R.id.fragment_main, tabFragments[menuNumber])
                .commit();

        // L'appel à onMenuChange() sera déclenché automatiquement par MenuFragment
    }

    @Override
    public void onMenuChange(int index) {        // C'est L'ACTIVITÉ qui décide quoi faire
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_main, tabFragments[index])
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onClick(int numFragment) {
        Log.d(TAG, "clicked by fragment: "+ numFragment);
    }

    @Override
    public void onIncidentSelected(Issue incident) {
        Screen1Fragment destinationFragment = new Screen1Fragment(); // c'est le fragment de destination
        // il faut passer le text de l'element de la liste (dans Screen2) à Screen1 via bundle
        Bundle bundle = new Bundle();
        bundle.putParcelable("my_incident", incident);
        destinationFragment.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_main, destinationFragment)
                .addToBackStack(null) // pour le retour en arriere
                .commit();
    }

    @Override
    public void onDataChange(int numFragment, Object object, int actionCode, Object argsAction) {
        // Si ça vient du Fragment 2 (la liste)
        if (numFragment == 2) {

            // ACTION 1 : Clic sur une ligne pour voir le détail
            if (actionCode == 1) {
                Issue incidentRecu = (Issue) object; // Ici on transforme l'Object en Issue

                Screen1Fragment detailFrag = new Screen1Fragment();
                Bundle bundle = new Bundle();
                bundle.putParcelable("my_incident", incidentRecu);
                detailFrag.setArguments(bundle);

                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_main, detailFrag)
                        .addToBackStack(null)
                        .commit();
            }

            // ACTION 2 : Changement de la RatingBar
            if (actionCode == 2) {
                float nouvelleNote = (float) argsAction; // On récupère le float
                Issue incidentModifie = (Issue) object;
                Log.d("DEBUG", "L'incident " + incidentModifie.getTitle() + " a maintenant " + nouvelleNote + " étoiles");
            }
        }
    }


    @Override
    public void onFragmentDisplayed(int fragmentId) {

    }
}