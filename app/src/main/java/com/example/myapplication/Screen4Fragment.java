package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

public class Screen4Fragment extends Fragment {

    private Notifiable notifiable;

    // ── Niveaux de criticité ───────────────────────────────────────────────────

    /** Extrait le niveau de criticité depuis le texte de l'alerte. */
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

        /** Couleur de la barre latérale et du badge. */
        int color() {
            switch (this) {
                case CRITIQUE: return Color.parseColor("#C62828"); // rouge
                case URGENT:   return Color.parseColor("#F9A825"); // ambre
                case STABLE:   return Color.parseColor("#2E7D32"); // vert
                default:       return Color.parseColor("#757575"); // gris
            }
        }

        /** Fond de l'icône (version claire de la couleur). */
        int iconBackground() {
            switch (this) {
                case CRITIQUE: return Color.parseColor("#FFEBEE");
                case URGENT:   return Color.parseColor("#FFF8E1");
                case STABLE:   return Color.parseColor("#E8F5E9");
                default:       return Color.parseColor("#F5F5F5");
            }
        }

        /** Label court affiché dans le badge. */
        String badge() {
            switch (this) {
                case CRITIQUE: return "CRITIQUE";
                case URGENT:   return "URGENT";
                case STABLE:   return "STABLE";
                default:       return "INFO";
            }
        }
    }

    // ── Données parsées d'une alerte ──────────────────────────────────────────

    private static class Alert {
        final String title;
        final String subtitle;
        final Severity severity;

        Alert(String raw) {
            // Exemple de format attendu : "CRITIQUE - Accident A9 - 3 véhicules impliqués"
            // Ou texte libre : on affiche tout dans le titre.
            String[] parts = raw.split(" - ", 3);
            if (parts.length >= 2) {
                // Le premier segment peut être la criticité ou un titre court
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
    }

    // ── Adaptateur personnalisé ───────────────────────────────────────────────

    private static class AlertAdapter extends ArrayAdapter<String> {

        private final LayoutInflater inflater;

        AlertAdapter(@NonNull Context ctx, @NonNull List<String> items) {
            super(ctx, R.layout.item_alert, items);
            inflater = LayoutInflater.from(ctx);
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

            // Textes
            holder.title.setText(alert.title);
            if (alert.subtitle.isEmpty()) {
                holder.subtitle.setVisibility(View.GONE);
            } else {
                holder.subtitle.setVisibility(View.VISIBLE);
                holder.subtitle.setText(alert.subtitle);
            }

            // Couleurs selon la criticité
            int color = alert.severity.color();
            holder.severityBar.setBackgroundColor(color);
            holder.icon.setBackgroundColor(alert.severity.iconBackground());
            holder.icon.setColorFilter(color);
            holder.badge.setBackgroundColor(color);
            holder.badge.setText(alert.severity.badge());

            return convertView;
        }

        /** ViewHolder pattern pour éviter les appels répétés à findViewById. */
        static class ViewHolder {
            final View      severityBar;
            final ImageView icon;
            final TextView  title;
            final TextView  subtitle;
            final TextView  badge;

            ViewHolder(View v) {
                severityBar = v.findViewById(R.id.alert_severity_bar);
                icon        = v.findViewById(R.id.alert_icon);
                title       = v.findViewById(R.id.alert_title);
                subtitle    = v.findViewById(R.id.alert_subtitle);
                badge       = v.findViewById(R.id.alert_severity_badge);
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

        ListView         listView    = view.findViewById(R.id.alerts_list_view);
        LinearLayout     emptyState  = view.findViewById(R.id.empty_state_layout);
        TextView         countBadge  = view.findViewById(R.id.alerts_count_badge);

        // Récupération des alertes depuis le Singleton EmergencyService
        List<String> alerts = EmergencyService.getInstance().getAlerts();

        // État vide
        if (alerts == null || alerts.isEmpty()) {
            listView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            countBadge.setText("0");
            return view;
        }

        // Mise à jour du badge compteur
        countBadge.setText(String.valueOf(alerts.size()));

        // Adaptateur custom
        AlertAdapter adapter = new AlertAdapter(requireContext(), alerts);
        listView.setAdapter(adapter);

        // Tap sur un item → notification + toast
        listView.setOnItemClickListener((parent, itemView, position, id) -> {
            String raw = alerts.get(position);
            Alert alert = new Alert(raw);

            Toast.makeText(
                    requireContext(),
                    "Alerte sélectionnée : " + alert.title,
                    Toast.LENGTH_SHORT
            ).show();
        });

        return view;
    }
}