package com.example.myapplication.screens;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.myapplication.Notifiable;
import com.example.myapplication.NotificationController;
import com.example.myapplication.R;
import com.example.myapplication.factories.HighwayFactory;
import com.example.myapplication.factories.UrbanFactory;
import com.example.myapplication.issue.AccidentFactory;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class QuickReportFragment extends Fragment {
    private static final long MAX_LOCATION_AGE_MS = 10 * 60 * 1000;
    private static final String PHOTO_PATH = "photo_path";
    private static final String PENDING_PHOTO_PATH = "pending_photo_path";

    private Notifiable notifiable;
    private AccidentFactory accidentFactory;
    private LocationManager locationManager;
    private Location currentLocation;
    private TextView locationStatusView;
    private TextView photoStatusView;
    private ImageView photoPreview;
    private String photoPath;
    private File pendingPhotoFile;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult
            );

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    this::onCameraPermissionResult
            );

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.TakePicture(),
                    this::onPhotoTaken
            );

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            currentLocation = location;
            updateLocationStatus(location);
        }

        @Override
        public void onProviderEnabled(@NonNull String provider) {
            startLocationSearch();
        }

        @Override
        public void onProviderDisabled(@NonNull String provider) {
            updateLocationStatus(null);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) return;

        photoPath = savedInstanceState.getString(PHOTO_PATH);
        String pendingPath = savedInstanceState.getString(PENDING_PHOTO_PATH);
        if (pendingPath != null) {
            pendingPhotoFile = new File(pendingPath);
        }
    }

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_quick_report, container, false);

        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        locationStatusView = view.findViewById(R.id.location_status);
        photoStatusView = view.findViewById(R.id.photo_status);
        photoPreview = view.findViewById(R.id.photo_preview);

        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroup);
        TextInputLayout layoutTitle = view.findViewById(R.id.layoutTitre);
        TextInputLayout layoutDesc = view.findViewById(R.id.layoutDesc);
        Button validationButton = view.findViewById(R.id.btnEnvoyer);
        View photoButton = view.findViewById(R.id.btnPhoto);
        ScrollView signalScroll = view.findViewById(R.id.signal_scroll);
        configureTitleKeyboard(layoutTitle.getEditText(), layoutDesc.getEditText());

        toggleGroup.check(R.id.btnUrbain);
        requestLocationIfNeeded();
        updatePhotoPreview();

        photoButton.setOnClickListener(c -> requestCameraIfNeeded());

        validationButton.setOnClickListener(c -> {
            if (!isFormValid(layoutTitle, layoutDesc)) {
                showMissingTitleFeedback(signalScroll, layoutTitle);
                return;
            }

            Location location = getUsableLocation();
            if (location == null) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.toast_waiting_gps),
                        Toast.LENGTH_LONG
                ).show();
                requestLocationIfNeeded();
                return;
            }

            if (toggleGroup.getCheckedButtonId() == R.id.btnAutoroute) {
                accidentFactory = new HighwayFactory();
            } else {
                accidentFactory = new UrbanFactory();
            }

            String title = readText(layoutTitle);
            String description = readText(layoutDesc);
            if (description.isEmpty()) {
                description = getString(R.string.default_description);
            }

            Issue issue = accidentFactory.createIssue(
                    title,
                    description,
                    location.getLongitude(),
                    location.getLatitude()
            );
            if (hasAttachedPhoto()) {
                issue.setPhotoPath(photoPath);
            }

            IssueRepository.getInstance().addIssue(issue);
            NotificationController.notifyNewAccidentForRescue(requireContext(), issue);
            Toast.makeText(
                    getContext(),
                    issue.hasPhoto()
                            ? getString(R.string.toast_report_sent_with_photo)
                            : getString(R.string.toast_report_sent),
                    Toast.LENGTH_SHORT
            ).show();

            if (notifiable != null) {
                notifiable.onDataChange(3, issue, 0, null);
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        stopLocationSearch();
        locationStatusView = null;
        photoStatusView = null;
        photoPreview = null;
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PHOTO_PATH, photoPath);
        if (pendingPhotoFile != null) {
            outState.putString(PENDING_PHOTO_PATH, pendingPhotoFile.getAbsolutePath());
        }
    }

    private void onLocationPermissionResult(Map<String, Boolean> result) {
        if (hasLocationPermission()) {
            startLocationSearch();
        } else {
            setLocationStatus(getString(R.string.gps_denied), color(R.color.severity_critical_color));
        }
    }

    private void onCameraPermissionResult(boolean isGranted) {
        if (isGranted) {
            takeAccidentPhoto();
            return;
        }

        Toast.makeText(
                requireContext(),
                getString(R.string.camera_denied),
                Toast.LENGTH_LONG
        ).show();
    }

    private void requestLocationIfNeeded() {
        if (hasLocationPermission()) {
            startLocationSearch();
            return;
        }

        setLocationStatus(getString(R.string.gps_authorize), color(R.color.severity_critical_color));
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void requestCameraIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            takeAccidentPhoto();
            return;
        }

        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void takeAccidentPhoto() {
        try {
            pendingPhotoFile = File.createTempFile(
                    "accident_",
                    ".jpg",
                    requireContext().getCacheDir()
            );
            Uri photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    pendingPhotoFile
            );
            takePictureLauncher.launch(photoUri);
        } catch (IOException e) {
            pendingPhotoFile = null;
            Toast.makeText(
                    requireContext(),
                    getString(R.string.photo_file_error),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void onPhotoTaken(boolean success) {
        if (success && pendingPhotoFile != null && pendingPhotoFile.exists()) {
            photoPath = pendingPhotoFile.getAbsolutePath();
            pendingPhotoFile = null;
            updatePhotoPreview();
            Toast.makeText(requireContext(), getString(R.string.toast_photo_added), Toast.LENGTH_SHORT).show();
            return;
        }

        if (pendingPhotoFile != null && pendingPhotoFile.exists()) {
            pendingPhotoFile.delete();
        }
        pendingPhotoFile = null;
        updatePhotoPreview();
    }

    private void startLocationSearch() {
        if (!hasLocationPermission() || locationManager == null) return;

        setLocationStatus(getString(R.string.gps_searching), color(R.color.app_text_muted));

        Location bestLastKnown = getBestLastKnownLocation();
        if (isRecentEnough(bestLastKnown)) {
            currentLocation = bestLastKnown;
            updateLocationStatus(bestLastKnown);
        }

        requestUpdates(LocationManager.GPS_PROVIDER);
        requestUpdates(LocationManager.NETWORK_PROVIDER);
    }

    private void requestUpdates(String provider) {
        try {
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(provider, 3000, 5, locationListener);
            }
        } catch (SecurityException ignored) {
            setLocationStatus(getString(R.string.gps_blocked), color(R.color.severity_critical_color));
        } catch (IllegalArgumentException ignored) {
            // Provider non disponible sur cet appareil.
        }
    }

    private void stopLocationSearch() {
        if (locationManager == null) return;
        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException ignored) {
            // Rien a retirer si la permission n'est plus disponible.
        }
    }

    private Location getUsableLocation() {
        if (isRecentEnough(currentLocation)) return currentLocation;

        Location bestLastKnown = getBestLastKnownLocation();
        if (isRecentEnough(bestLastKnown)) {
            currentLocation = bestLastKnown;
            updateLocationStatus(bestLastKnown);
            return bestLastKnown;
        }

        return null;
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

    private boolean isRecentEnough(Location location) {
        return location != null
                && System.currentTimeMillis() - location.getTime() <= MAX_LOCATION_AGE_MS;
    }

    private void updateLocationStatus(Location location) {
        if (location == null) {
            setLocationStatus(getString(R.string.gps_enable), color(R.color.app_warning));
            return;
        }

        setLocationStatus(
                getString(R.string.gps_found),
                color(R.color.app_success)
        );
    }

    private void setLocationStatus(String message, int color) {
        if (locationStatusView == null) return;
        locationStatusView.setText(message);
        locationStatusView.setTextColor(color);
    }

    private void updatePhotoPreview() {
        if (photoStatusView == null || photoPreview == null) return;

        if (!hasAttachedPhoto()) {
            photoPreview.setVisibility(View.GONE);
            photoPreview.setImageDrawable(null);
            photoStatusView.setText(R.string.no_photo);
            photoStatusView.setTextColor(color(R.color.app_text_muted));
            return;
        }

        File photo = new File(photoPath);
        photoPreview.setVisibility(View.VISIBLE);
        photoPreview.setImageURI(null);
        photoPreview.setImageURI(Uri.fromFile(photo));
        photoStatusView.setText(R.string.photo_ready);
        photoStatusView.setTextColor(color(R.color.app_success));
    }

    private boolean hasAttachedPhoto() {
        return photoPath != null && new File(photoPath).exists();
    }

    private boolean isFormValid(TextInputLayout layoutTitle, TextInputLayout layoutDesc) {
        boolean isValid = true;

        if (readText(layoutTitle).isEmpty()) {
            layoutTitle.setError(getString(R.string.title_required));
            isValid = false;
        } else {
            layoutTitle.setError(null);
        }

        layoutDesc.setError(null);

        return isValid;
    }

    private void configureTitleKeyboard(EditText titleEditText, EditText descriptionEditText) {
        if (titleEditText == null || descriptionEditText == null) return;

        titleEditText.setSingleLine(true);
        titleEditText.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                descriptionEditText.requestFocus();
                return true;
            }
            return false;
        });
    }

    private void showMissingTitleFeedback(ScrollView scrollView, TextInputLayout layoutTitle) {
        EditText titleEditText = layoutTitle.getEditText();
        if (titleEditText != null) {
            titleEditText.requestFocus();
        } else {
            layoutTitle.requestFocus();
        }

        Toast.makeText(
                requireContext(),
                getString(R.string.toast_title_required),
                Toast.LENGTH_LONG
        ).show();

        scrollToView(scrollView, layoutTitle);
    }

    private void scrollToView(ScrollView scrollView, View target) {
        if (scrollView == null || target == null) return;

        scrollView.post(() -> {
            int top = target.getTop();
            View parent = target.getParent() instanceof View ? (View) target.getParent() : null;
            while (parent != null && parent != scrollView) {
                top += parent.getTop();
                parent = parent.getParent() instanceof View ? (View) parent.getParent() : null;
            }
            scrollView.smoothScrollTo(0, Math.max(0, top - 24));
        });
    }

    private String readText(TextInputLayout layout) {
        EditText editText = layout.getEditText();
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }

    private int color(int colorRes) {
        return ContextCompat.getColor(requireContext(), colorRes);
    }
}
