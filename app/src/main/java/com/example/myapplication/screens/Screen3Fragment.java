package com.example.myapplication.screens;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.Notifiable;
import com.example.myapplication.R;
import com.example.myapplication.factories.HighwayFactory;
import com.example.myapplication.factories.UrbanFactory;
import com.example.myapplication.issue.AccidentFactory;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Map;

public class Screen3Fragment extends Fragment {
    private static final long MAX_LOCATION_AGE_MS = 10 * 60 * 1000;

    private Notifiable notifiable;
    private AccidentFactory accidentFactory;
    private LocationManager locationManager;
    private Location currentLocation;
    private TextView locationStatusView;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    this::onLocationPermissionResult
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
        View view = inflater.inflate(R.layout.fragment_screen3, container, false);

        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        locationStatusView = view.findViewById(R.id.location_status);

        MaterialButtonToggleGroup toggleGroup = view.findViewById(R.id.toggleGroup);
        TextInputLayout layoutTitle = view.findViewById(R.id.layoutTitre);
        TextInputLayout layoutDesc = view.findViewById(R.id.layoutDesc);
        Button validationButton = view.findViewById(R.id.btnEnvoyer);

        toggleGroup.check(R.id.btnUrbain);
        requestLocationIfNeeded();

        validationButton.setOnClickListener(c -> {
            if (!isFormValid(layoutTitle, layoutDesc)) return;

            Location location = getUsableLocation();
            if (location == null) {
                Toast.makeText(
                        requireContext(),
                        "Position GPS en cours. Autorisez la localisation et patientez quelques secondes.",
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
                description = "Aucune description fournie.";
            }

            Issue issue = accidentFactory.createIssue(
                    title,
                    description,
                    location.getLongitude(),
                    location.getLatitude()
            );

            IssueRepository.getInstance().addIssue(issue);
            Toast.makeText(getContext(), "Incident signale avec position GPS !", Toast.LENGTH_SHORT).show();

            if (notifiable != null) {
                notifiable.onDataChange(3, issue, 0, "Incident signale, ouverture du detail");
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        stopLocationSearch();
        locationStatusView = null;
        super.onDestroyView();
    }

    private void onLocationPermissionResult(Map<String, Boolean> result) {
        if (hasLocationPermission()) {
            startLocationSearch();
        } else {
            setLocationStatus("Localisation refusee : impossible d'envoyer une position fiable.", 0xFFC62828);
        }
    }

    private void requestLocationIfNeeded() {
        if (hasLocationPermission()) {
            startLocationSearch();
            return;
        }

        setLocationStatus("Autorisez la localisation pour transmettre le lieu exact.", 0xFFC62828);
        locationPermissionLauncher.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void startLocationSearch() {
        if (!hasLocationPermission() || locationManager == null) return;

        setLocationStatus("Recherche de votre position GPS...", 0xFF757575);

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
            setLocationStatus("Permission localisation manquante.", 0xFFC62828);
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
            setLocationStatus("Position indisponible. Activez le GPS.", 0xFFF9A825);
            return;
        }

        setLocationStatus(
                String.format("Position trouvee : %.5f, %.5f", location.getLatitude(), location.getLongitude()),
                0xFF2E7D32
        );
    }

    private void setLocationStatus(String message, int color) {
        if (locationStatusView == null) return;
        locationStatusView.setText(message);
        locationStatusView.setTextColor(color);
    }

    private boolean isFormValid(TextInputLayout layoutTitle, TextInputLayout layoutDesc) {
        boolean isValid = true;

        if (readText(layoutTitle).isEmpty()) {
            layoutTitle.setError("Titre obligatoire");
            isValid = false;
        } else {
            layoutTitle.setError(null);
        }

        layoutDesc.setError(null);

        return isValid;
    }

    private String readText(TextInputLayout layout) {
        EditText editText = layout.getEditText();
        if (editText == null || editText.getText() == null) return "";
        return editText.getText().toString().trim();
    }
}
