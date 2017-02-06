package com.aerophile.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aerophile.app.dao.JourneeDAO;
import com.aerophile.app.dao.VolDAO;
import com.aerophile.app.modeles.Journee;
import com.aerophile.app.modeles.Vol;
import com.aerophile.app.utils.Api;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

@EActivity(R.layout.activity_launcher)
public class LauncherActivity extends AppCompatActivity {

    private JourneeDAO daoJournee;

    private String code;

    @Bean
    Api api;

    @ViewById
    LinearLayout layoutOffline;

    @ViewById
    ProgressBar chargement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);

        // On récupère le code de sécurité
        code = reglages.getString("CODE_SECURITE", "KO");

        // Mise en place de la langue de l'application
        String langue = reglages.getString("LANGUE", Locale.getDefault().toString());
        Locale locale = new Locale(langue);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        // On vérifie si le code existe et s'il est correct
        if(code.equals("KO")) {
            startAerophile(new Intent(this, AccueilActivity_.class));
        } else {
            checkCode();
        }
    }

    @Background
    @Click(R.id.buttonOffline)
    void checkCode() {
        toggleLayouts(true);
        if(api.isOnline(this)) {
            MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
            data.add("code", code);
            String json = api.post(this, data, "code");
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode retour = mapper.readTree(json);
                if (retour.get("statut").asInt(1) == 0) {
                    codeBon();
                } else {
                    startAerophile(new Intent(this, AccueilActivity_.class));
                }
            } catch (Exception e) {
                e.printStackTrace();
                toggleLayouts(false);
            }
        } else {
            toggleLayouts(false);
        }
    }

    void codeBon() {
        SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
        String lieu = reglages.getString("LIEU", "KO");
        String immatriculation = reglages.getString("IMMATRICULATION", "KO");
        daoJournee = new JourneeDAO(this);
        daoJournee.open();
        Log.d("AEROBUG", "Journee en cours ?? " + daoJournee.isJourneeEnCours());
        if(daoJournee.isJourneeEnCours()) {
            Intent journeeEnCours = new Intent(this, VolListActivity.class);
            journeeEnCours.putExtra("ID_JOURNEE", daoJournee.getJourneeEnCours().getId());
            startAerophile(journeeEnCours);
            return;
        }
        if(lieu.equals("KO") || immatriculation.equals("KO")) {
            startAerophile(new Intent(this, ReglagesActivity_.class));
            return;
        }
        startAerophile(new Intent(this, DemarrageActivity_.class));
    }

    @UiThread
    void startAerophile(Intent ecranDemarrage) {
        startActivity(ecranDemarrage);
        finish();
    }

    @UiThread
    void toggleLayouts(boolean enLigne) {
        if(enLigne) {
            chargement.setVisibility(View.VISIBLE);
            layoutOffline.setVisibility(View.GONE);
        } else {
            chargement.setVisibility(View.GONE);
            layoutOffline.setVisibility(View.VISIBLE);
        }
    }

    @UiThread
    void message(String texte) {
        Toast.makeText(this, texte, Toast.LENGTH_SHORT).show();
    }
}
