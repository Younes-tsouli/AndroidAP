package com.example.myapplication.screens;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.Notifiable;
import com.example.myapplication.R;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.example.myapplication.issue.Priority;
import com.example.myapplication.issue.Status;

import org.osmdroid.api.IMapController;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IncidentMapFragment extends Fragment {
    private static final int NUM_FRAGMENT = 4;
    private static final double DEFAULT_LATITUDE = 43.6156;
    private static final double DEFAULT_LONGITUDE = 7.0718;
    private static final double DEFAULT_ZOOM = 17.0;
    private static final int MARKER_SIZE_DP = 34;
    private static final double CENTERED_THRESHOLD_METERS = 25.0;
    private static final String ARG_MAP_LATITUDE = "map_latitude";
    private static final String ARG_MAP_LONGITUDE = "map_longitude";
    private static final String ARG_MAP_ZOOM = "map_zoom";
    private static final String STATE_FILTER_RECEIVED = "filter_received";
    private static final String STATE_FILTER_NOT_SENT = "filter_not_sent";
    private static final String STATE_FILTER_SENT = "filter_sent";
    private static final String STATE_FILTER_RESOLVED = "filter_resolved";
    private static final ColorFilter SOFT_NIGHT_TILE_FILTER = new ColorMatrixColorFilter(new float[]{
            -0.55f, 0f, 0f, 0f, 210f,
            0f, -0.55f, 0f, 0f, 210f,
            0f, 0f, -0.55f, 0f, 210f,
            0f, 0f, 0f, 1f, 0f
    });

    private Notifiable notifiable;
    private MapView map;
    private ImageButton centerPersonalLocationButton;
    private LocationManager locationManager;
    private Location personalLocation;
    private List<Issue> allIncidents;
    private List<Issue> visibleIncidents;
    private IncidentListAdapter adapter;
    private boolean cameraRestored;
    private boolean showReceived = true;
    private boolean showAidNotSent = false;
    private boolean showAidSent = true;
    private boolean showResolved = false;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult
            );

    private final LocationListener personalLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            personalLocation = location;
            addMarkersToMap();
            centerOnPersonalPositionIfMapIsEmpty();
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            startPersonalLocationSearch();
        }
    };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) {
            notifiable = (Notifiable) context;
        } else {
            throw new AssertionError("L'activité doit implémenter Notifiable !");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        notifiable = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_incident_map, container, false);

        Configuration.getInstance().load(
                requireContext(),
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext())
        );
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        map = view.findViewById(R.id.map);
        centerPersonalLocationButton = view.findViewById(R.id.btn_center_personal_location);
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        ListView listViewIncidents = view.findViewById(R.id.incidents_list);

        restoreStatusFilters(savedInstanceState);
        allIncidents = IssueRepository.getInstance().getIssues();
        visibleIncidents = new ArrayList<>();
        adapter = new IncidentListAdapter(requireContext(), visibleIncidents);
        listViewIncidents.setAdapter(adapter);

        setupStatusFilters(view);
        centerPersonalLocationButton.setOnClickListener(click -> centerOnPersonalPosition());
        updateCenterButtonState();

        initializeMap();
        requestPersonalLocationIfNeeded();
        return view;
    }

    private void initializeMap() {
        configureMapTileSource();
        map.setMultiTouchControls(true);

        IMapController controller = map.getController();
        if (!restoreMapCamera(controller)) {
            controller.setZoom(DEFAULT_ZOOM);
            centerMap(controller);
        }

        addMarkersToMap();

        map.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateVisibleIncidents();
                updateCenterButtonState();
                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                updateVisibleIncidents();
                updateCenterButtonState();
                return true;
            }
        });

        updateVisibleIncidents();
    }

    private void restoreStatusFilters(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        showReceived = savedInstanceState.getBoolean(STATE_FILTER_RECEIVED, showReceived);
        showAidNotSent = savedInstanceState.getBoolean(STATE_FILTER_NOT_SENT, showAidNotSent);
        showAidSent = savedInstanceState.getBoolean(STATE_FILTER_SENT, showAidSent);
        showResolved = savedInstanceState.getBoolean(STATE_FILTER_RESOLVED, showResolved);
    }

    private void setupStatusFilters(View view) {
        CheckBox receivedFilter = view.findViewById(R.id.filter_received);
        CheckBox notSentFilter = view.findViewById(R.id.filter_not_sent);
        CheckBox sentFilter = view.findViewById(R.id.filter_sent);
        CheckBox resolvedFilter = view.findViewById(R.id.filter_resolved);

        receivedFilter.setChecked(showReceived);
        notSentFilter.setChecked(showAidNotSent);
        sentFilter.setChecked(showAidSent);
        resolvedFilter.setChecked(showResolved);

        receivedFilter.setOnCheckedChangeListener((buttonView, checked) -> {
            showReceived = checked;
            refreshFilteredIncidents();
        });
        notSentFilter.setOnCheckedChangeListener((buttonView, checked) -> {
            showAidNotSent = checked;
            refreshFilteredIncidents();
        });
        sentFilter.setOnCheckedChangeListener((buttonView, checked) -> {
            showAidSent = checked;
            refreshFilteredIncidents();
        });
        resolvedFilter.setOnCheckedChangeListener((buttonView, checked) -> {
            showResolved = checked;
            refreshFilteredIncidents();
        });
    }

    private void refreshFilteredIncidents() {
        addMarkersToMap();
        updateVisibleIncidents();
    }

    private void configureMapTileSource() {
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.getMapOverlay().setColorFilter(isNightMode() ? SOFT_NIGHT_TILE_FILTER : null);
        map.getMapOverlay().setLoadingBackgroundColor(color(
                isNightMode() ? R.color.app_surface_variant : R.color.app_background_alt
        ));
        map.getMapOverlay().setLoadingLineColor(color(R.color.app_divider));
    }

    private boolean isNightMode() {
        int mode = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return mode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void refreshIncidentsFromRepository() {
        allIncidents = IssueRepository.getInstance().getIssues();
        addMarkersToMap();
        updateVisibleIncidents();
    }

    private void centerMap(IMapController controller) {
        if (allIncidents != null) {
            for (Issue incident : allIncidents) {
                if (shouldShowIssue(incident)) {
                    controller.setCenter(new GeoPoint(incident.getLatitude(), incident.getLongitude()));
                    return;
                }
            }
        }

        controller.setCenter(new GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE));
    }

    private boolean restoreMapCamera(IMapController controller) {
        Bundle args = getArguments();
        if (args == null
                || !args.containsKey(ARG_MAP_LATITUDE)
                || !args.containsKey(ARG_MAP_LONGITUDE)
                || !args.containsKey(ARG_MAP_ZOOM)) {
            return false;
        }

        controller.setZoom(args.getDouble(ARG_MAP_ZOOM, DEFAULT_ZOOM));
        controller.setCenter(new GeoPoint(
                args.getDouble(ARG_MAP_LATITUDE, DEFAULT_LATITUDE),
                args.getDouble(ARG_MAP_LONGITUDE, DEFAULT_LONGITUDE)
        ));
        cameraRestored = true;
        return true;
    }

    @Nullable
    public Bundle getMapCameraState() {
        if (map == null) return null;

        IGeoPoint center = map.getMapCenter();
        if (center == null) return null;

        Bundle state = new Bundle();
        state.putDouble(ARG_MAP_LATITUDE, center.getLatitude());
        state.putDouble(ARG_MAP_LONGITUDE, center.getLongitude());
        state.putDouble(ARG_MAP_ZOOM, map.getZoomLevelDouble());
        return state;
    }

    private void addMarkersToMap() {
        if (map == null) return;
        map.getOverlays().clear();
        addPersonalPositionMarker();

        for (Issue issue : allIncidents) {
            if (!shouldShowIssue(issue)) continue;

            Marker marker = new Marker(map);
            marker.setPosition(new GeoPoint(issue.getLatitude(), issue.getLongitude()));
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setIcon(createScaledMarkerIcon(getMarkerColorForIssue(issue)));
            marker.setTitle(issue.getTitle());
            marker.setSnippet(issue.getDescription());
            marker.setDraggable(false);
            map.getOverlays().add(marker);
        }

        map.invalidate();
    }

    private boolean shouldShowIssue(Issue issue) {
        if (issue == null) return false;

        Status status = issue.getStatus();
        if (status == Status.RECEIVED) return showReceived;
        if (status == Status.AID_NOT_SENT) return showAidNotSent;
        if (status == Status.AID_SENT) return showAidSent;
        if (status == Status.RESOLVED) return showResolved;
        return true;
    }

    private void addPersonalPositionMarker() {
        if (personalLocation == null) return;

        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(personalLocation.getLatitude(), personalLocation.getLongitude()));
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(createScaledMarkerIcon(R.color.map_marker_blue));
        marker.setTitle(getString(R.string.map_my_position));
        marker.setSnippet(getString(R.string.map_personal_position));
        marker.setDraggable(false);
        map.getOverlays().add(marker);
    }

    private int getMarkerColorForIssue(Issue issue) {
        Priority priority = issue.getPriority();
        if (priority == Priority.CRITICAL) {
            return R.color.map_marker_red;
        }
        if (priority == Priority.HIGH) {
            return R.color.map_marker_yellow;
        }
        return R.color.map_marker_green;
    }

    private Drawable createScaledMarkerIcon(int colorRes) {
        Drawable source = ContextCompat.getDrawable(requireContext(), R.drawable.ic_map_marker);
        if (source == null) return null;

        source = DrawableCompat.wrap(source).mutate();
        DrawableCompat.setTint(source, color(colorRes));

        int sizePx = dpToPx(MARKER_SIZE_DP);
        Bitmap scaled = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(scaled);
        source.setBounds(0, 0, sizePx, sizePx);
        source.draw(canvas);

        BitmapDrawable drawable = new BitmapDrawable(getResources(), scaled);
        drawable.setBounds(0, 0, sizePx, sizePx);
        return drawable;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void requestPersonalLocationIfNeeded() {
        if (hasLocationPermission()) {
            startPersonalLocationSearch();
            return;
        }

        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void onLocationPermissionResult(Map<String, Boolean> result) {
        if (hasLocationPermission()) {
            startPersonalLocationSearch();
        }
    }

    private void startPersonalLocationSearch() {
        if (!hasLocationPermission() || locationManager == null) return;

        Location bestLastKnown = getBestLastKnownLocation();
        if (bestLastKnown != null) {
            personalLocation = bestLastKnown;
            addMarkersToMap();
            centerOnPersonalPositionIfMapIsEmpty();
            updateCenterButtonState();
        }

        requestLocationUpdates(LocationManager.GPS_PROVIDER);
        requestLocationUpdates(LocationManager.NETWORK_PROVIDER);
    }

    private void requestLocationUpdates(String provider) {
        try {
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(provider, 5000, 10, personalLocationListener);
            }
        } catch (SecurityException ignored) {
            // Permission retiree entre le test et la demande de mise a jour.
        } catch (IllegalArgumentException ignored) {
            // Provider indisponible sur cet appareil.
        }
    }

    private Location getBestLastKnownLocation() {
        if (!hasLocationPermission() || locationManager == null) return null;

        Location gpsLocation = getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location networkLocation = getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        if (gpsLocation == null) return networkLocation;
        if (networkLocation == null) return gpsLocation;
        return gpsLocation.getTime() >= networkLocation.getTime() ? gpsLocation : networkLocation;
    }

    private Location getLastKnownLocation(String provider) {
        try {
            return locationManager.getLastKnownLocation(provider);
        } catch (SecurityException ignored) {
            return null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean hasLocationPermission() {
        Context context = getContext();
        if (context == null) return false;

        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void centerOnPersonalPosition() {
        if (personalLocation == null) {
            requestPersonalLocationIfNeeded();
            Toast.makeText(requireContext(), getString(R.string.map_recentering), Toast.LENGTH_SHORT).show();
            return;
        }

        map.getController().setCenter(
                new GeoPoint(personalLocation.getLatitude(), personalLocation.getLongitude())
        );
        updateCenterButtonState();
    }

    private void centerOnPersonalPositionIfMapIsEmpty() {
        if (cameraRestored
                || map == null
                || personalLocation == null
                || (allIncidents != null && !allIncidents.isEmpty())) {
            return;
        }

        map.getController().setCenter(
                new GeoPoint(personalLocation.getLatitude(), personalLocation.getLongitude())
        );
        updateCenterButtonState();
    }

    private void updateCenterButtonState() {
        if (centerPersonalLocationButton == null || map == null || personalLocation == null) {
            if (centerPersonalLocationButton != null) {
                centerPersonalLocationButton.setColorFilter(color(R.color.map_target_default));
            }
            return;
        }

        IGeoPoint center = map.getMapCenter();
        double distance = distanceMeters(
                center.getLatitude(),
                center.getLongitude(),
                personalLocation.getLatitude(),
                personalLocation.getLongitude()
        );
        centerPersonalLocationButton.setColorFilter(
                color(distance <= CENTERED_THRESHOLD_METERS
                        ? R.color.map_target_centered
                        : R.color.map_target_default)
        );
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(requireContext(), colorRes);
    }

    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusMeters = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMeters * c;
    }

    private void updateVisibleIncidents() {
        BoundingBox bounds = map.getBoundingBox();
        if (bounds == null) return;

        visibleIncidents.clear();
        for (Issue issue : allIncidents) {
            if (shouldShowIssue(issue) && bounds.contains(issue.getLatitude(), issue.getLongitude())) {
                visibleIncidents.add(issue);
            }
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_FILTER_RECEIVED, showReceived);
        outState.putBoolean(STATE_FILTER_NOT_SENT, showAidNotSent);
        outState.putBoolean(STATE_FILTER_SENT, showAidSent);
        outState.putBoolean(STATE_FILTER_RESOLVED, showResolved);
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
        if (map != null) {
            map.onResume();
            refreshIncidentsFromRepository();
            updateCenterButtonState();
        }
    }

    @Override
    public void onPause() {
        if (map != null) map.onPause();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        stopPersonalLocationSearch();
        if (map != null) {
            map.onDetach();
            map = null;
        }
        centerPersonalLocationButton = null;
        locationManager = null;
        super.onDestroyView();
    }

    private void stopPersonalLocationSearch() {
        if (locationManager == null) return;

        try {
            locationManager.removeUpdates(personalLocationListener);
        } catch (SecurityException ignored) {
            // Rien a retirer si la permission n'est plus disponible.
        }
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
            convertView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.app_surface));
            title.setTextColor(ContextCompat.getColor(getContext(), R.color.app_text_primary));
            details.setTextColor(ContextCompat.getColor(getContext(), R.color.app_text_muted));

            if (incident != null) {
                title.setText(incident.getTitle());
                details.setText(getContext().getString(
                        R.string.map_visible_location_format,
                        incident.getLatitude(),
                        incident.getLongitude()
                ));
            }

            return convertView;
        }
    }
}
