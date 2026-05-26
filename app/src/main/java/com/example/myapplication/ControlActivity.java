package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.issue.Issue;
import com.example.myapplication.menu.MenuFragment;
import com.example.myapplication.menu.Menuable;
import com.example.myapplication.screens.Screen1Fragment;
import com.example.myapplication.screens.Screen2Fragment;
import com.example.myapplication.screens.Screen3Fragment;
import com.example.myapplication.screens.Screen4Fragment;
import com.example.myapplication.screens.Screen5Fragment;
import com.example.myapplication.screens.VictimHomeFragment;

public class ControlActivity extends AppCompatActivity implements Menuable, Notifiable {

    private static final String TAG = "ControlActivity";
    public static final String EXTRA_INDEX = "index";
    public static final String EXTRA_ROLE = "role";
    public static final String ROLE_VICTIM = "victim";
    public static final String ROLE_RESCUE = "rescue";
    private static final String ARG_INCIDENT = "my_incident";

    private String currentRole = ROLE_VICTIM;
    private Fragment[] tabFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_control);
        applySystemBarInsets();
        findViewById(R.id.home_button).setOnClickListener(view -> goHome());

        Intent intent = getIntent();
        currentRole = intent.getStringExtra(EXTRA_ROLE);
        if (!ROLE_RESCUE.equals(currentRole)) {
            currentRole = ROLE_VICTIM;
        }

        tabFragments = createFragmentsForRole(currentRole);
        int menuNumber = sanitizeIndex(intent.getIntExtra(EXTRA_INDEX, getDefaultIndexForRole(currentRole)));
        updateHeaderTitle();

        Bundle args = new Bundle();
        args.putInt(EXTRA_INDEX, menuNumber);
        args.putString(EXTRA_ROLE, currentRole);

        MenuFragment menuFragment = new MenuFragment();
        menuFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_menu, menuFragment)
                .replace(R.id.fragment_main, tabFragments[menuNumber])
                .commit();
    }

    @Override
    public void onMenuChange(int index) {
        int safeIndex = sanitizeIndex(index);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_main, tabFragments[safeIndex])
                .addToBackStack(null)
                .commit();

        MenuFragment menuFragment = (MenuFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_menu);
        if (menuFragment != null) {
            menuFragment.setExternalIndex(safeIndex);
        }
    }

    @Override
    public void onClick(int numFragment) {
        Log.d(TAG, "clicked by fragment: " + numFragment);
    }

    @Override
    public void onIncidentSelected(Issue incident) {
        openIssueDetail(incident);
    }

    @Override
    public void onDataChange(int numFragment, Object object, int actionCode, Object argsAction) {
        if (numFragment == 2) {
            handleIssueListAction(object, actionCode, argsAction);
            return;
        }

        if (numFragment == 3 && object instanceof Issue) {
            showMessage(argsAction);
            openIssueDetail((Issue) object);
        }
    }

    @Override
    public void onFragmentDisplayed(int fragmentId) {
    }

    private void handleIssueListAction(Object object, int actionCode, Object argsAction) {
        if (!(object instanceof Issue)) return;

        Issue issue = (Issue) object;
        if (actionCode == 1) {
            openIssueDetail(issue);
        } else if (actionCode == 2 && argsAction instanceof Float) {
            Log.d(TAG, "Status change for " + issue.getTitle() + " rating=" + argsAction);
        }
    }

    private void openIssueDetail(Issue issue) {
        if (issue == null) return;

        Screen1Fragment detailFragment = new Screen1Fragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_INCIDENT, issue);
        detailFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_main, detailFragment)
                .addToBackStack(null)
                .commit();

        MenuFragment menuFragment = (MenuFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_menu);
        if (menuFragment != null) {
            menuFragment.setExternalIndex(ROLE_VICTIM.equals(currentRole) ? 2 : 1);
        }
    }

    private int sanitizeIndex(int index) {
        if (index < 0 || index >= tabFragments.length) return 0;
        if (ROLE_RESCUE.equals(currentRole) && (index == 0 || index == 2)) {
            return getDefaultIndexForRole(currentRole);
        }
        if (ROLE_VICTIM.equals(currentRole) && index == 4) {
            return getDefaultIndexForRole(currentRole);
        }
        return index;
    }

    private void showMessage(Object message) {
        if (message != null) {
            Toast.makeText(this, message.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void goHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void applySystemBarInsets() {
        View root = findViewById(R.id.control_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });
    }

    private Fragment[] createFragmentsForRole(String role) {
        if (ROLE_RESCUE.equals(role)) {
            return new Fragment[]{
                    new Screen2Fragment(),
                    new Screen2Fragment(),
                    new Screen3Fragment(),
                    new Screen5Fragment(),
                    new Screen4Fragment()
            };
        }

        return new Fragment[]{
                new VictimHomeFragment(),
                new Screen3Fragment(),
                new Screen2Fragment(),
                new Screen5Fragment()
        };
    }

    private int getDefaultIndexForRole(String role) {
        return ROLE_RESCUE.equals(role) ? 1 : 0;
    }

    private void updateHeaderTitle() {
        TextView title = findViewById(R.id.control_title);
        title.setText(ROLE_RESCUE.equals(currentRole) ? "MODE SECOURS" : "MODE UTILISATEUR");
    }
}
