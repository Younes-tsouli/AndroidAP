package com.example.myapplication.screens;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.Manifest;
import android.content.pm.PackageManager;

import com.example.myapplication.Notifiable;
import com.example.myapplication.R;
import com.example.myapplication.issue.HighwayIssue;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.AccidentFactory;
import com.example.myapplication.issue.IssueRepository;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;

import java.util.ArrayList;
import java.util.List;

public class Screen5Fragment extends Fragment {
    private Notifiable notifiable;
    private final int NUM_FRAGMENT = 4; // Screen5 est le 5ème fragment (index 4)

    // Carte et contrôle
    private MapView map;
    private IMapController controller;
    private List<Issue> allIncidents;

    // ListView
    private ListView listViewIncidents;
    private IncidentListAdapter adapter;
    private List<Issue> visibleIncidents;

    // Constantes pour la géolocalisation par défaut (Biot, France)
    private static final double DEFAULT_LATITUDE = 43.6156;
    private static final double DEFAULT_LONGITUDE = 7.0718;
    private static final int DEFAULT_ZOOM = 17;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (requireActivity() instanceof Notifiable) {
            notifiable = (Notifiable) requireActivity();
        } else {
            throw new AssertionError("L'activité doit implémenter Notifiable !");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen5, container, false);

        // Initialiser OSMDroid configuration
// Configure le dossier de cache avec l'identifiant de ton application
        Configuration.getInstance().load(requireContext(), androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext()));
        // Récupérer les références des composants
        map = view.findViewById(R.id.map);
        listViewIncidents = view.findViewById(R.id.incidents_list);

        // 1. Récupérer tous les incidents depuis le Repository
        allIncidents = IssueRepository.getInstance().getIssues();

        // 2. Initialiser la liste des incidents visibles
        visibleIncidents = new ArrayList<>();

        // 3. INITIALISER L'ADAPTATEUR EN PREMIER
        adapter = new IncidentListAdapter(requireContext(), visibleIncidents);
        listViewIncidents.setAdapter(adapter);

        // 4. INITIALISER LA MAP EN DERNIER (comme ça, l'adaptateur existe déjà !)
        initializeMap();

        return view;
    }

    /**
     * Initialiser la carte OSM
     */
    private void initializeMap() {
        // Vérifier les permissions pour le stockage (nécessaire pour OSM)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        // Configurer la carte
        map.setMultiTouchControls(true); // Permettre le zoom avec 2 doigts
        controller = map.getController();

        // Centrer sur la position par défaut (Biot)
        GeoPoint startPoint = new GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);
        controller.setCenter(startPoint);
        controller.setZoom(DEFAULT_ZOOM);

        // Ajouter les markers pour tous les incidents
        addMarkersToMap();

        // Ajouter un listener pour la map (déplacement, zoom, etc.)
        map.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateVisibleIncidents();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                updateVisibleIncidents();
                return true;
            }
        });

        // Première mise à jour de la liste
        updateVisibleIncidents();
    }

    /**
     * Ajouter les markers de tous les incidents sur la carte
     */
    private void addMarkersToMap() {
        map.getOverlays().clear(); // Nettoyer les anciens markers

        for (Issue issue : allIncidents) {
            // Créer un marker pour chaque incident
            Marker marker = new Marker(map);

            // Définir la position du marker
            GeoPoint incidentLocation = new GeoPoint(
                    issue.getLatitude(),
                    issue.getLongitude()
            );
            marker.setPosition(incidentLocation);

            // Configurer l'apparence du marker
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(issue.getTitle()); // Titre du marker
            marker.setSnippet(issue.getDescription()); // Description (infobulle)
            marker.setDraggable(false);

            // Ajouter le marker à la carte
            map.getOverlays().add(marker);
        }

        map.invalidate(); // Rafraîchir la carte
    }

    /**
     * Mettre à jour la liste des incidents visibles sur la carte
     */
    private void updateVisibleIncidents() {
        // Récupérer la zone visible de la carte
        BoundingBox bounds = map.getBoundingBox();

        if (bounds != null) {
            // Filtrer les incidents qui sont dans la zone visible
            visibleIncidents.clear();
            for (Issue issue : allIncidents) {
                if (bounds.contains(issue.getLatitude(), issue.getLongitude())) {
                    visibleIncidents.add(issue);
                }
            }
        }

        // Notifier l'adaptateur du changement
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Notifier l'activité que le fragment s'affiche
        if (notifiable != null) {
            notifiable.onFragmentDisplayed(NUM_FRAGMENT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reprendre les ressources de la carte
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause les ressources de la carte
        map.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (map != null) {
            map.onDetach();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, on peut utiliser la carte
                map.invalidate();
            }
        }
    }

    /**
     * Adaptateur personnalisé pour afficher les incidents dans la ListView
     */
    public static class IncidentListAdapter extends ArrayAdapter<Issue> {
        public IncidentListAdapter(Context context, List<Issue> incidents) {
            super(context, android.R.layout.simple_list_item_2, incidents);
        }

        @Override
        public android.view.View getView(int position, android.view.View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = android.view.LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            Issue incident = getItem(position);

            android.widget.TextView title = convertView.findViewById(android.R.id.text1);
            android.widget.TextView details = convertView.findViewById(android.R.id.text2);

            if (incident != null) {
                title.setText(incident.getTitle());
                details.setText(String.format("Lat: %.4f, Lon: %.4f",
                        incident.getLatitude(), incident.getLongitude()));
            }

            return convertView;
        }
    }
}