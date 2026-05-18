package com.example.myapplication.screens;

import android.content.Context;
import android.graphics.Color;
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
import androidx.fragment.app.Fragment;

import com.example.myapplication.issue.EmergencyService;
import com.example.myapplication.Notifiable;
import com.example.myapplication.R;

import java.util.Arrays;
import java.util.List;

public class Screen4Fragment extends Fragment {

    private Notifiable notifiable;

    // ── Statuts disponibles dans le Spinner ───────────────────────────────────

    /** Liste des statuts proposés dans le menu déroulant. */
    private static final List<String> STATUS_OPTIONS = Arrays.asList(
            "NIV.3 - CRITIQUE",
            "NIV.2 - URGENT",
            "NIV.1 - STABLE",
            "INFO"
    );

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

        int color() {
            switch (this) {
                case CRITIQUE: return Color.parseColor("#C62828");
                case URGENT:   return Color.parseColor("#F9A825");
                case STABLE:   return Color.parseColor("#2E7D32");
                default:       return Color.parseColor("#757575");
            }
        }

        int iconBackground() {
            switch (this) {
                case CRITIQUE: return Color.parseColor("#FFEBEE");
                case URGENT:   return Color.parseColor("#FFF8E1");
                case STABLE:   return Color.parseColor("#E8F5E9");
                default:       return Color.parseColor("#F5F5F5");
            }
        }

        String badge() {
            switch (this) {
                case CRITIQUE: return "CRITIQUE";
                case URGENT:   return "URGENT";
                case STABLE:   return "STABLE";
                default:       return "INFO";
            }
        }

        /** Retourne l'index correspondant dans STATUS_OPTIONS. */
        int spinnerIndex() {
            switch (this) {
                case CRITIQUE: return 0;
                case URGENT:   return 1;
                case STABLE:   return 2;
                default:       return 3;
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
                    subtitle = parts[1].trim() + (parts.length >= 3 ? " — " + parts[2].trim() : "");
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
            StringBuilder sb = new StringBuilder(newStatus).append(" - ").append(title);
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

        AlertAdapter(@NonNull Context ctx, @NonNull List<String> items) {
            super(ctx, R.layout.item_alert, items);
            this.inflater = LayoutInflater.from(ctx);
            this.data     = items;
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
            int color = alert.severity.color();
            holder.severityBar.setBackgroundColor(color);
            holder.icon.setBackgroundColor(alert.severity.iconBackground());
            holder.icon.setColorFilter(color);
            holder.badge.setBackgroundColor(color);
            holder.badge.setText(alert.severity.badge());

            // ── Spinner : affichage du statut actuel ──────────────────────────
            // On détache le listener avant de changer la sélection
            // pour éviter un déclenchement intempestif.
            holder.spinner.setTag(null);

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    getContext(),
                    android.R.layout.simple_spinner_item,
                    STATUS_OPTIONS
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

                    String selectedStatus = STATUS_OPTIONS.get(spinnerPos);
                    String currentRaw     = data.get(itemPosition);
                    Alert  currentAlert   = new Alert(currentRaw);

                    // Ne rien faire si le statut n'a pas changé
                    if (selectedStatus.equals(currentRaw.split(" - ")[0])) return;

                    // ── Reconstruction de la chaîne avec le nouveau statut ────
                    String newRaw = Alert.rebuildRaw(
                            selectedStatus,
                            currentAlert.title,
                            currentAlert.subtitle
                    );

                    // ── Sauvegarde dans EmergencyService ─────────────────────
                    // EmergencyService doit exposer : updateAlert(int index, String newRaw)
                    EmergencyService.getInstance().updateAlert(itemPosition, newRaw);

                    // Mise à jour locale de la liste pour refléter la couleur sans rechargement complet
                    data.set(itemPosition, newRaw);
                    notifyDataSetChanged();

                    Toast.makeText(
                            getContext(),
                            "Statut mis à jour : " + selectedStatus,
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

        View view = inflater.inflate(R.layout.fragment_screen4, container, false);

        ListView     listView   = view.findViewById(R.id.alerts_list_view);
        LinearLayout emptyState = view.findViewById(R.id.empty_state_layout);
        TextView     countBadge = view.findViewById(R.id.alerts_count_badge);

        List<String> alerts = EmergencyService.getInstance().getAlerts();

        if (alerts == null || alerts.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            countBadge.setText("0");
            return view;
        }

        countBadge.setText(String.valueOf(alerts.size()));

        AlertAdapter adapter = new AlertAdapter(requireContext(), alerts);
        listView.setAdapter(adapter);

        // Tap sur un item → toast informatif (le Spinner gère déjà le changement de statut)
        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            Alert alert = new Alert(alerts.get(position));
            Toast.makeText(
                    requireContext(),
                    "Alerte : " + alert.title,
                    Toast.LENGTH_SHORT
            ).show();
        });

        return view;
    }
}