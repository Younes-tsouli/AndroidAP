package com.example.myapplication.screens;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.myapplication.Notifiable;
import com.example.myapplication.R;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

public class Screen5Fragment extends Fragment {
    private static final int NUM_FRAGMENT = 4;
    private static final double DEFAULT_LATITUDE = 43.6156;
    private static final double DEFAULT_LONGITUDE = 7.0718;
    private static final double DEFAULT_ZOOM = 17.0;

    private Notifiable notifiable;
    private MapView map;
    private List<Issue> allIncidents;
    private List<Issue> visibleIncidents;
    private IncidentListAdapter adapter;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) {
            notifiable = (Notifiable) context;
        } else {
            throw new AssertionError("L'activite doit implementer Notifiable !");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        notifiable = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screen5, container, false);

        Configuration.getInstance().load(
                requireContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        );
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        map = view.findViewById(R.id.map);
        ListView listViewIncidents = view.findViewById(R.id.incidents_list);

        allIncidents = IssueRepository.getInstance().getIssues();
        visibleIncidents = new ArrayList<>();
        adapter = new IncidentListAdapter(requireContext(), visibleIncidents);
        listViewIncidents.setAdapter(adapter);

        initializeMap();
        return view;
    }

    private void initializeMap() {
        map.setMultiTouchControls(true);

        IMapController controller = map.getController();
        controller.setZoom(DEFAULT_ZOOM);
        centerMap(controller);

        addMarkersToMap();

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

        updateVisibleIncidents();
    }

    private void centerMap(IMapController controller) {
        if (allIncidents == null || allIncidents.isEmpty()) {
            controller.setCenter(new GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE));
            return;
        }

        Issue firstIncident = allIncidents.get(0);
        controller.setCenter(new GeoPoint(firstIncident.getLatitude(), firstIncident.getLongitude()));
    }

    private void addMarkersToMap() {
        map.getOverlays().clear();

        for (Issue issue : allIncidents) {
            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(issue.getLatitude(), issue.getLongitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(issue.getTitle());
            marker.setSnippet(issue.getDescription());
            marker.setDraggable(false);
            map.getOverlays().add(marker);
        }

        map.invalidate();
    }

    private void updateVisibleIncidents() {
        BoundingBox bounds = map.getBoundingBox();
        if (bounds == null) return;

        visibleIncidents.clear();
        for (Issue issue : allIncidents) {
            if (bounds.contains(issue.getLatitude(), issue.getLongitude())) {
                visibleIncidents.add(issue);
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (notifiable != null) {
            notifiable.onFragmentDisplayed(NUM_FRAGMENT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    public void onPause() {
        if (map != null) map.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        if (map != null) {
            map.onDetach();
            map = null;
        }
        super.onDestroyView();
    }

    public static class IncidentListAdapter extends ArrayAdapter<Issue> {
        public IncidentListAdapter(Context context, List<Issue> incidents) {
            super(context, android.R.layout.simple_list_item_2, incidents);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(android.R.layout.simple_list_item_2, parent, false);
            }

            Issue incident = getItem(position);
            TextView title = convertView.findViewById(android.R.id.text1);
            TextView details = convertView.findViewById(android.R.id.text2);

            if (incident != null) {
                title.setText(incident.getTitle());
                details.setText(String.format(
                        "Lat: %.4f, Lon: %.4f",
                        incident.getLatitude(),
                        incident.getLongitude()
                ));
            }

            return convertView;
        }
    }
}
