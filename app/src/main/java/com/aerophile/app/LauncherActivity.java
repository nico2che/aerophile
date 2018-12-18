package com.aerophile.app;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.aerophile.app.dao.JourneeDAO;
import com.aerophile.app.modeles.Preferences_;
import com.aerophile.app.utils.Api;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Locale;

@EActivity(R.layout.activity_launcher)
public class LauncherActivity extends AppCompatActivity {

    private JourneeDAO daoJournee;

    @Bean
    Api api;

    @ViewById
    LinearLayout layoutOffline;

    @ViewById
    ProgressBar chargement;

    @Pref
    Preferences_ reglages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseAnalytics mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // ****
        // TODO: LIGNES A SUPPRIMER APRES LA VERSION 1.8
        SharedPreferences r = PreferenceManager.getDefaultSharedPreferences(this);
        if(!r.getString("CODE_SECURITE", "").isEmpty()) {
            reglages.edit()
                    .code().put(r.getString("CODE_SECURITE", ""))
                    .lieu().put(r.getString("LIEU", ""))
                    .immatriculation().put(r.getString("IMMATRICULATION", ""))
                    .premierEmail().put(r.getString("PRE_EMAIL", ""))
                    .secondEmail().put(r.getString("SEC_EMAIL", ""))
                    .langue().put(r.getString("LANGUE", Locale.getDefault().toString()))
                    .apply();
            SharedPreferences.Editor reglages_editor = r.edit();
            reglages_editor.putString("CODE_SECURITE", "");
            reglages_editor.apply();
        }
        // ****

        // Mise en place de la langue de l'application
        String langue = reglages.langue().getOr(Locale.getDefault().toString());
        Locale locale = new Locale(langue);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, String.valueOf(reglages.code().exists()));
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        // On vérifie si le code existe et s'il est correct
        if(!reglages.code().exists()) {
            AccueilActivity_.intent(this).start();
            finish();
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
            data.add("code", reglages.code().get());
            String json = api.post(this, data, "code");
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode retour = mapper.readTree(json);
                if (retour.get("statut").asInt(1) == 0) {
                    codeBon();
                } else {
                    AccueilActivity_.intent(this).start();
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
        daoJournee = new JourneeDAO(this);
        daoJournee.open();
        Log.d("AEROBUG", "Journee en cours ?? " + daoJournee.isJourneeEnCours());
        if(daoJournee.isJourneeEnCours()) {
            VolListActivity_.intent(this).idJournee(daoJournee.getJourneeEnCours().getId()).start();
            return;
        }
        if(!reglages.lieu().exists() || !reglages.immatriculation().exists()) {
            ReglagesActivity_.intent(this).start();
            return;
        }
        DemarrageActivity_.intent(this).start();
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
