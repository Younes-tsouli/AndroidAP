package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.issue.EmergencyService;
import com.example.myapplication.issue.Issue;
import com.example.myapplication.issue.IssueRepository;
import com.example.myapplication.menu.MenuFragment;
import com.example.myapplication.menu.Menuable;
import com.example.myapplication.screens.ControlTowerFragment;
import com.example.myapplication.screens.IncidentDetailFragment;
import com.example.myapplication.screens.IncidentListFragment;
import com.example.myapplication.screens.IncidentMapFragment;
import com.example.myapplication.screens.QuickReportFragment;
import com.example.myapplication.screens.VictimHomeFragment;

public class ControlActivity extends AppCompatActivity implements Menuable, Notifiable {

    private static final String TAG = "ControlActivity";
    public static final String EXTRA_INDEX = "index";
    public static final String EXTRA_ROLE = "role";
    public static final String ROLE_VICTIM = "victim";
    public static final String ROLE_RESCUE = "rescue";
    private static final String ARG_INCIDENT = "my_incident";
    private static final String STATE_ROLE = "state_role";
    private static final String STATE_INDEX = "state_index";
    private static final String STATE_DETAIL_ISSUE_ID = "state_detail_issue_id";
    private static final String STATE_MAP_CAMERA = "state_map_camera";

    private String currentRole = ROLE_VICTIM;
    private int currentIndex = 0;
    private String detailIssueId;
    private Bundle restoredMapCameraState;
    private Fragment[] tabFragments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeController.applySavedMode(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_control);
        EmergencyService.getInstance().initialize(getApplicationContext());
        applySystemBarInsets();
        ThemeController.setupThemeToggle(this, findViewById(R.id.theme_button));
        findViewById(R.id.home_button).setOnClickListener(view -> goHome());

        Intent intent = getIntent();
        currentRole = savedInstanceState != null
                ? savedInstanceState.getString(STATE_ROLE, ROLE_VICTIM)
                : intent.getStringExtra(EXTRA_ROLE);
        if (!ROLE_RESCUE.equals(currentRole)) {
            currentRole = ROLE_VICTIM;
        }
        restoredMapCameraState = savedInstanceState != null
                ? savedInstanceState.getBundle(STATE_MAP_CAMERA)
                : null;

        tabFragments = createFragmentsForRole(currentRole);
        currentIndex = sanitizeIndex(savedInstanceState != null
                ? savedInstanceState.getInt(STATE_INDEX, getDefaultIndexForRole(currentRole))
                : intent.getIntExtra(EXTRA_INDEX, getDefaultIndexForRole(currentRole)));
        detailIssueId = savedInstanceState != null
                ? savedInstanceState.getString(STATE_DETAIL_ISSUE_ID)
                : null;
        updateHeaderTitle();

        Bundle args = new Bundle();
        args.putInt(EXTRA_INDEX, currentIndex);
        args.putString(EXTRA_ROLE, currentRole);

        MenuFragment menuFragment = new MenuFragment();
        menuFragment.setArguments(args);

        Fragment initialFragment = createInitialMainFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_menu, menuFragment)
                .replace(R.id.fragment_main, initialFragment)
                .commit();
    }

    @Override
    public void onMenuChange(int index) {
        int safeIndex = sanitizeIndex(index);
        currentIndex = safeIndex;
        detailIssueId = null;
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

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ROLE, currentRole);
        outState.putInt(STATE_INDEX, currentIndex);
        outState.putString(STATE_DETAIL_ISSUE_ID, detailIssueId);

        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_main);
        if (currentFragment instanceof IncidentMapFragment) {
            Bundle mapCameraState = ((IncidentMapFragment) currentFragment).getMapCameraState();
            if (mapCameraState != null) {
                outState.putBundle(STATE_MAP_CAMERA, mapCameraState);
            }
        }
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

        currentIndex = ROLE_VICTIM.equals(currentRole) ? 2 : 1;
        detailIssueId = issue.getId();
        IncidentDetailFragment detailFragment = createIssueDetailFragment(issue);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_main, detailFragment)
                .addToBackStack(null)
                .commit();

        MenuFragment menuFragment = (MenuFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_menu);
        if (menuFragment != null) {
            menuFragment.setExternalIndex(currentIndex);
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

    private Fragment createInitialMainFragment() {
        if (detailIssueId == null) {
            return tabFragments[currentIndex];
        }

        Issue issue = IssueRepository.getInstance().findIssueById(detailIssueId);
        if (issue == null) {
            detailIssueId = null;
            return tabFragments[currentIndex];
        }
        return createIssueDetailFragment(issue);
    }

    private IncidentDetailFragment createIssueDetailFragment(Issue issue) {
        IncidentDetailFragment detailFragment = new IncidentDetailFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_INCIDENT, issue);
        bundle.putString(EXTRA_ROLE, currentRole);
        detailFragment.setArguments(bundle);
        return detailFragment;
    }

    private Fragment[] createFragmentsForRole(String role) {
        if (ROLE_RESCUE.equals(role)) {
            return new Fragment[]{
                    withRole(new IncidentListFragment(), role),
                    withRole(new IncidentListFragment(), role),
                    withRole(new QuickReportFragment(), role),
                    withRole(new IncidentMapFragment(), role),
                    withRole(new ControlTowerFragment(), role)
            };
        }

        return new Fragment[]{
                withRole(new VictimHomeFragment(), role),
                withRole(new QuickReportFragment(), role),
                withRole(new IncidentListFragment(), role),
                withRole(new IncidentMapFragment(), role)
        };
    }

    private Fragment withRole(Fragment fragment, String role) {
        Bundle args = new Bundle();
        args.putString(EXTRA_ROLE, role);
        if (fragment instanceof IncidentMapFragment && restoredMapCameraState != null) {
            args.putAll(restoredMapCameraState);
        }
        fragment.setArguments(args);
        return fragment;
    }

    private int getDefaultIndexForRole(String role) {
        return ROLE_RESCUE.equals(role) ? 1 : 0;
    }

    private void updateHeaderTitle() {
        TextView title = findViewById(R.id.control_title);
        title.setText(getString(ROLE_RESCUE.equals(currentRole) ? R.string.mode_rescue : R.string.mode_user));
    }
}
