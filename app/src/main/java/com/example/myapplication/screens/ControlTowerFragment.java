package com.example.myapplication.screens;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.issue.EmergencyService;
import com.example.myapplication.Notifiable;
import com.example.myapplication.R;
import com.example.myapplication.issue.Priority;

import java.util.Arrays;
import java.util.List;

public class ControlTowerFragment extends Fragment {

    private Notifiable notifiable;

    // ── Statuts disponibles dans le Spinner ───────────────────────────────────

    // ── Niveaux de criticité ───────────────────────────────────────────────────

    private enum Severity {
        CRITIQUE, URGENT, STABLE, INFO;

        static Severity parse(String text) {
            if (text == null) return INFO;
            String upper = text.toUpperCase();
            if (upper.contains("CRITIQUE") || upper.contains("NIV.3") || upper.contains("CRITICAL"))
                return CRITIQUE;
            if (upper.contains("URGENT") || upper.contains("NIV.2"))
                return URGENT;
            if (upper.contains("STABLE") || upper.contains("NIV.1"))
                return STABLE;
            return INFO;
        }

        int colorRes() {
            switch (this) {
                case CRITIQUE: return R.color.severity_critical_color;
                case URGENT:   return R.color.severity_urgent_color;
                case STABLE:   return R.color.severity_stable_color;
                default:       return R.color.severity_info_color;
            }
        }

        int iconBackgroundRes() {
            switch (this) {
                case CRITIQUE: return R.color.severity_critical_surface;
                case URGENT:   return R.color.severity_urgent_surface;
                case STABLE:   return R.color.severity_stable_surface;
                default:       return R.color.severity_info_surface;
            }
        }

        int badgeStringRes() {
            switch (this) {
                case CRITIQUE: return R.string.severity_critical;
                case URGENT:   return R.string.severity_urgent;
                case STABLE:   return R.string.severity_stable;
                default:       return R.string.severity_info;
            }
        }

        /** Retourne l'index correspondant dans STATUS_OPTIONS. */
        int spinnerIndex() {
            switch (this) {
                case CRITIQUE: return 0;
                case URGENT:   return 1;
                case STABLE:   return 2;
                default:       return 2;
            }
        }

        Priority priority() {
            switch (this) {
                case CRITIQUE:
                    return Priority.CRITICAL;
                case URGENT:
                    return Priority.HIGH;
                case STABLE:
                case INFO:
                default:
                    return Priority.MEDIUM;
            }
        }
    }

    // ── Données parsées d'une alerte ──────────────────────────────────────────

    private static class Alert {
        final String title;
        final String subtitle;
        final Severity severity;

        Alert(String raw) {
            String[] parts = raw.split(" - ", 3);
            if (parts.length >= 2) {
                Severity s = Severity.parse(parts[0]);
                if (s != Severity.INFO) {
                    severity = s;
                    title    = parts.length >= 2 ? parts[1].trim() : raw;
                    subtitle = parts.length >= 3 ? parts[2].trim() : "";
                } else {
                    severity = Severity.parse(raw);
                    title    = parts[0].trim();
                    subtitle = parts[1].trim() + (parts.length >= 3 ? " - " + parts[2].trim() : "");
                }
            } else {
                severity = Severity.parse(raw);
                title    = raw;
                subtitle = "";
            }
        }

        /**
         * Reconstruit la chaîne brute avec le nouveau statut.
         * Format : "NIV.X - LABEL - Titre - Sous-titre"
         */
        static String rebuildRaw(String newStatus, String title, String subtitle) {
            String statusPrefix = newStatus.split(" - ")[0];
            StringBuilder sb = new StringBuilder(statusPrefix).append(" - ").append(title);
            if (subtitle != null && !subtitle.isEmpty()) {
                sb.append(" - ").append(subtitle);
            }
            return sb.toString();
        }
    }

    // ── Adaptateur personnalisé ───────────────────────────────────────────────

    private class AlertAdapter extends ArrayAdapter<String> {

        private final LayoutInflater inflater;
        private final List<String>   data;
        private final List<String>   statusOptions;

        AlertAdapter(@NonNull Context ctx, @NonNull List<String> items, @NonNull List<String> statusOptions) {
            super(ctx, R.layout.item_alert, items);
            this.inflater = LayoutInflater.from(ctx);
            this.data     = items;
            this.statusOptions = statusOptions;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_alert, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String raw = getItem(position);
            if (raw == null) return convertView;

            Alert alert = new Alert(raw);

            // ── Textes ────────────────────────────────────────────────────────
            holder.title.setText(alert.title);
            if (alert.subtitle.isEmpty()) {
                holder.subtitle.setVisibility(View.GONE);
            } else {
                holder.subtitle.setVisibility(View.VISIBLE);
                holder.subtitle.setText(alert.subtitle);
            }

            // ── Couleurs ──────────────────────────────────────────────────────
            int color = ContextCompat.getColor(getContext(), alert.severity.colorRes());
            holder.severityBar.setBackgroundColor(color);
            holder.icon.setBackgroundColor(ContextCompat.getColor(getContext(), alert.severity.iconBackgroundRes()));
            holder.icon.setColorFilter(color);
            holder.badge.setBackgroundColor(color);
            holder.badge.setText(getContext().getString(alert.severity.badgeStringRes()));

            // ── Spinner : affichage du statut actuel ──────────────────────────
            // On détache le listener avant de changer la sélection
            // pour éviter un déclenchement intempestif.
            holder.spinner.setTag(null);

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_item,
                    statusOptions
            );
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            holder.spinner.setAdapter(spinnerAdapter);
            holder.spinner.setSelection(alert.severity.spinnerIndex(), false);

            // On stocke la position dans le tag pour y accéder dans le listener
            holder.spinner.setTag(position);

            holder.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int spinnerPos, long id) {
                    // Ignore les callbacks déclenchés lors de l'initialisation
                    Object tag = holder.spinner.getTag();
                    if (tag == null) return;
                    int itemPosition = (int) tag;

                    String selectedStatus = statusOptions.get(spinnerPos);
                    String currentRaw     = data.get(itemPosition);
                    Alert  currentAlert   = new Alert(currentRaw);

                    // Ne rien faire si le statut n'a pas changé
                    if (currentAlert.severity.spinnerIndex() == spinnerPos) return;

                    // ── Reconstruction de la chaîne avec le nouveau statut ────
                    String newRaw = Alert.rebuildRaw(
                            selectedStatus,
                            currentAlert.title,
                            currentAlert.subtitle
                    );

                    // ── Sauvegarde dans EmergencyService ─────────────────────
                    Severity selectedSeverity = Severity.parse(selectedStatus);
                    EmergencyService.getInstance().updateAlertPriority(
                            itemPosition,
                            newRaw,
                            selectedSeverity.priority()
                    );

                    // Mise à jour locale de la liste pour refléter la couleur sans rechargement complet
                    data.set(itemPosition, newRaw);
                    notifyDataSetChanged();

                    Toast.makeText(
                            getContext(),
                            getContext().getString(R.string.alert_status_updated, selectedStatus),
                            Toast.LENGTH_SHORT
                    ).show();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) { /* rien */ }
            });

            return convertView;
        }

        class ViewHolder {
            final View      severityBar;
            final ImageView icon;
            final TextView  title;
            final TextView  subtitle;
            final TextView  badge;
            final Spinner   spinner;

            ViewHolder(View v) {
                severityBar = v.findViewById(R.id.alert_severity_bar);
                icon        = v.findViewById(R.id.alert_icon);
                title       = v.findViewById(R.id.alert_title);
                subtitle    = v.findViewById(R.id.alert_subtitle);
                badge       = v.findViewById(R.id.alert_severity_badge);
                spinner     = v.findViewById(R.id.alert_status_spinner);
            }
        }
    }

    // ── Fragment lifecycle ────────────────────────────────────────────────────

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Notifiable) {
            notifiable = (Notifiable) context;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_control_tower, container, false);

        ListView     listView   = view.findViewById(R.id.alerts_list_view);
        LinearLayout emptyState = view.findViewById(R.id.empty_state_layout);
        TextView     countBadge = view.findViewById(R.id.alerts_count_badge);

        List<String> alerts = EmergencyService.getInstance().getAlerts();

        if (alerts == null || alerts.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            countBadge.setText(R.string.placeholder_zero);
            return view;
        }

        countBadge.setText(String.valueOf(alerts.size()));

        List<String> statusOptions = Arrays.asList(getResources().getStringArray(R.array.alert_status_options));
        AlertAdapter adapter = new AlertAdapter(requireContext(), alerts, statusOptions);
        listView.setAdapter(adapter);

        // Tap sur un item → toast informatif (le Spinner gère déjà le changement de statut)
        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            Alert alert = new Alert(alerts.get(position));
            Toast.makeText(
                    requireContext(),
                    getString(R.string.alert_prefix, alert.title),
                    Toast.LENGTH_SHORT
            ).show();
        });

        return view;
    }
}
