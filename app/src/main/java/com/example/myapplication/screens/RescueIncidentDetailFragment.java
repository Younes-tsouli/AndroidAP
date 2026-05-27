package com.example.myapplication.screens;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.Notifiable;
import com.example.myapplication.NotificationController;
import com.example.myapplication.R;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.example.myapplication.issue.Priority;
import com.example.myapplication.issue.Status;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.File;
import java.util.Map;

public class RescueIncidentDetailFragment extends Fragment {
    private static final String ARG_INCIDENT = "my_incident";
    private static final int ACTION_DECISION_SAVED = 1;

    private Issue currentIssue;
    private Notifiable notifiable;
    private LocationManager locationManager;
    private TextView distanceView;
    private boolean locationPermissionRequested;

    private final LocationListener rescueLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            showDistance(location);
            stopLocationSearch();
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            updateDistance();
        }
    };

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult
            );

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) {
            notifiable = (Notifiable) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        notifiable = null;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_rescue_incident_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        currentIssue = readIssueFromArguments();
        if (currentIssue == null) return;

        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        distanceView = view.findViewById(R.id.rescue_detail_distance);

        TextView titleView = view.findViewById(R.id.rescue_detail_title);
        TextView descriptionView = view.findViewById(R.id.rescue_detail_description);
        TextView statusView = view.findViewById(R.id.rescue_detail_status);
        TextView timeView = view.findViewById(R.id.rescue_detail_time);
        MaterialButtonToggleGroup severityGroup = view.findViewById(R.id.rescue_severity_group);
        MaterialButton primaryAction = view.findViewById(R.id.rescue_action_primary);
        MaterialButton secondaryAction = view.findViewById(R.id.rescue_action_secondary);

        titleView.setText(currentIssue.getTitle());
        descriptionView.setText(currentIssue.getDescription());
        statusView.setText(getString(R.string.status_prefix, getStatusLabel(currentIssue.getStatus())));
        timeView.setText(getString(R.string.rescue_time_prefix, getElapsedTimeLabel()));
        showIssuePhoto(view);
        setupSeverityGroup(severityGroup);
        setupActions(primaryAction, secondaryAction);
        updateDistance();
    }

    @Override
    public void onDestroyView() {
        stopLocationSearch();
        distanceView = null;
        locationManager = null;
        super.onDestroyView();
    }

    private Issue readIssueFromArguments() {
        Bundle args = getArguments();
        if (args == null) return null;

        Issue bundledIssue = args.getParcelable(ARG_INCIDENT);
        if (bundledIssue == null) return null;

        Issue repositoryIssue = IssueRepository.getInstance().findIssueById(bundledIssue.getId());
        return repositoryIssue != null ? repositoryIssue : bundledIssue;
    }

    private void showIssuePhoto(View view) {
        if (!currentIssue.hasPhoto()) return;

        File photo = new File(currentIssue.getPhotoPath());
        if (!photo.exists()) return;

        View photoLabel = view.findViewById(R.id.rescue_photo_label);
        View photoCard = view.findViewById(R.id.rescue_photo_card);
        ImageView photoView = view.findViewById(R.id.rescue_detail_photo);
        Uri photoUri = Uri.fromFile(photo);
        photoLabel.setVisibility(View.VISIBLE);
        photoCard.setVisibility(View.VISIBLE);
        photoView.setImageURI(photoUri);
        photoCard.setOnClickListener(click -> showPhotoFullscreen(photoUri));
        photoView.setOnClickListener(click -> showPhotoFullscreen(photoUri));
    }

    private void showPhotoFullscreen(Uri photoUri) {
        Dialog dialog = new Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView fullscreenPhoto = new ImageView(requireContext());
        fullscreenPhoto.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        fullscreenPhoto.setBackgroundColor(Color.BLACK);
        fullscreenPhoto.setContentDescription(getString(R.string.desc_close_photo_fullscreen));
        fullscreenPhoto.setImageURI(photoUri);
        fullscreenPhoto.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullscreenPhoto.setOnClickListener(click -> dialog.dismiss());

        dialog.setContentView(fullscreenPhoto);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setStatusBarColor(Color.BLACK);
            dialog.getWindow().setNavigationBarColor(Color.BLACK);
        }
    }

    private void setupSeverityGroup(MaterialButtonToggleGroup severityGroup) {
        severityGroup.check(getSeverityButtonId(currentIssue.getPriority()));
        severityGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            currentIssue.setPriority(getPriorityForButtonId(checkedId));
        });
    }

    private void setupActions(MaterialButton primaryAction, MaterialButton secondaryAction) {
        Status status = currentIssue.getStatus();

        if (status == Status.RECEIVED) {
            secondaryAction.setVisibility(View.VISIBLE);
            primaryAction.setText(R.string.rescue_action_send);
            secondaryAction.setText(R.string.rescue_action_do_not_send);
            primaryAction.setOnClickListener(view -> saveDecision(Status.AID_SENT));
            secondaryAction.setOnClickListener(view -> saveDecision(Status.AID_NOT_SENT));
            return;
        }

        secondaryAction.setVisibility(View.GONE);
        if (status == Status.AID_NOT_SENT) {
            primaryAction.setText(R.string.rescue_action_send);
            primaryAction.setOnClickListener(view -> saveDecision(Status.AID_SENT));
        } else if (status == Status.AID_SENT) {
            primaryAction.setText(R.string.rescue_action_finish);
            primaryAction.setOnClickListener(view -> saveDecision(Status.RESOLVED));
        } else {
            primaryAction.setText(R.string.rescue_action_restore_sent);
            primaryAction.setOnClickListener(view -> saveDecision(Status.AID_SENT));
        }
    }

    private void saveDecision(Status newStatus) {
        if (currentIssue.getStatus() != newStatus) {
            currentIssue.setStatus(newStatus);
            NotificationController.notifyStatusUpdateForUser(requireContext(), currentIssue);
        }

        Toast.makeText(requireContext(), R.string.rescue_decision_saved, Toast.LENGTH_SHORT).show();
        if (notifiable != null) {
            notifiable.onDataChange(5, currentIssue, ACTION_DECISION_SAVED, newStatus);
        }
    }

    private int getSeverityButtonId(Priority priority) {
        if (priority == Priority.CRITICAL) {
            return R.id.rescue_severity_critical;
        }
        if (priority == Priority.HIGH) {
            return R.id.rescue_severity_urgent;
        }
        return R.id.rescue_severity_stable;
    }

    private Priority getPriorityForButtonId(int buttonId) {
        if (buttonId == R.id.rescue_severity_critical) {
            return Priority.CRITICAL;
        }
        if (buttonId == R.id.rescue_severity_urgent) {
            return Priority.HIGH;
        }
        return Priority.MEDIUM;
    }

    private void updateDistance() {
        if (distanceView == null) return;

        if (!hasLocationPermission()) {
            distanceView.setText(R.string.rescue_distance_unavailable);
            if (!locationPermissionRequested) {
                locationPermissionRequested = true;
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
            return;
        }

        Location location = getBestLastKnownLocation();
        if (location == null) {
            distanceView.setText(R.string.rescue_distance_unavailable);
            startLocationSearch();
            return;
        }

        showDistance(location);
    }

    private void showDistance(Location location) {
        if (distanceView == null || location == null || currentIssue == null) return;

        double distance = distanceMeters(
                location.getLatitude(),
                location.getLongitude(),
                currentIssue.getLatitude(),
                currentIssue.getLongitude()
        );

        if (distance < 1000) {
            distanceView.setText(getString(R.string.rescue_distance_meters_format, Math.round(distance)));
        } else {
            distanceView.setText(getString(R.string.rescue_distance_km_format, distance / 1000.0));
        }
    }

    private void onLocationPermissionResult(Map<String, Boolean> result) {
        updateDistance();
    }

    private void startLocationSearch() {
        if (!hasLocationPermission() || locationManager == null) return;

        requestLocationUpdates(LocationManager.GPS_PROVIDER);
        requestLocationUpdates(LocationManager.NETWORK_PROVIDER);
    }

    private void requestLocationUpdates(String provider) {
        try {
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(provider, 3000, 5, rescueLocationListener);
            }
        } catch (SecurityException ignored) {
            // Permission retiree entre le test et la demande de mise a jour.
        } catch (IllegalArgumentException ignored) {
            // Provider indisponible sur cet appareil.
        }
    }

    private void stopLocationSearch() {
        if (locationManager == null) return;

        try {
            locationManager.removeUpdates(rescueLocationListener);
        } catch (SecurityException ignored) {
            // Rien a retirer si la permission n'est plus disponible.
        }
    }

    private Location getBestLastKnownLocation() {
        if (locationManager == null) return null;

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

    private String getElapsedTimeLabel() {
        long elapsedMillis = Math.max(0, System.currentTimeMillis() - currentIssue.getTimestamp());
        long minutes = elapsedMillis / 60000;
        if (minutes < 1) {
            return getString(R.string.rescue_time_now);
        }
        if (minutes < 60) {
            return getString(R.string.rescue_time_minutes_format, minutes);
        }
        return getString(R.string.rescue_time_hours_format, minutes / 60);
    }

    private String getStatusLabel(Status status) {
        switch (status) {
            case AID_NOT_SENT:
                return getString(R.string.status_not_sent);
            case AID_SENT:
                return getString(R.string.status_sent);
            case RESOLVED:
                return getString(R.string.status_resolved);
            case RECEIVED:
            default:
                return getString(R.string.status_received);
        }
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
}
