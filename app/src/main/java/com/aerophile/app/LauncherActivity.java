package com.aerophile.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.rest.spring.annotations.RestService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

@EActivity(R.layout.activity_launcher)
public class LauncherActivity extends AppCompatActivity {

    private JourneeDAO daoJournee;
    private Journee journeeAttente;

    private String code;

    @RestService
    JourneeClient restJournee;

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
            startAerophile(new Intent(this, AccueilActivity_.class), false);
        } else {
            checkCode();
        }
    }

    @Background
    @Click(R.id.buttonOffline)
    void checkCode() {
        toggleLayouts(true);
        if(isOnline()) {
            MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
            data.add("code", code);
            String json = restJournee.envoieJournee(data, "code");
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode retour = mapper.readTree(json);
                if (retour.get("statut").asInt(1) == 0) {
                    codeBon();
                } else {
                    startAerophile(new Intent(this, AccueilActivity_.class), false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                toggleLayouts(false);
            }
        } else {
            toggleLayouts(false);
        }
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

    void codeBon() {
        SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
        String lieu = reglages.getString("LIEU", "KO");
        String immatriculation = reglages.getString("IMMATRICULATION", "KO");
        daoJournee = new JourneeDAO(this);
        daoJournee.open();
        if(daoJournee.isJourneeEnCours()) {
            startAerophile(new Intent(this, VolListActivity.class), false);
        }
        if(lieu.equals("KO") || immatriculation.equals("KO")) {
            startAerophile(new Intent(this, ReglagesActivity_.class), false);
        }
        startAerophile(new Intent(this, DemarrageActivity_.class), true);
    }

    @UiThread
    void startAerophile(Intent ecranDemarrage, boolean envoieAttente) {
        if(envoieAttente && daoJournee != null) {
            journeeAttente = daoJournee.getJourneeEnAttente();
            if(journeeAttente.getAttente() != 0) {
                message(getString(R.string.envoie_mail_attente));
                requete();
            } else {
                daoJournee.close();
            }
        }
        startActivity(ecranDemarrage);
        finish();
    }

    @Background
    void requete() {
        if(isOnline()) {
	        VolDAO daoVol = new VolDAO(this);
	        daoVol.open();
            MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
	        List<Vol> vols = daoVol.getVolsByJournee(journeeAttente.getId());
	        for (Vol vol : vols) {
		        journeeAttente.addVol(vol);
	        }
            try {
                PackageManager manager = this.getPackageManager();
                PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
                ObjectMapper mapper = new ObjectMapper();
                String donnees = mapper.writeValueAsString(journeeAttente);
                SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
                data.add("objet_email", journeeAttente.getObjetAttente());
                data.add("appareil", Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID) );
                data.add("immatriculation", reglages.getString("IMMATRICULATION", "?"));
                data.add("lieu", reglages.getString("LIEU", "?"));
                data.add("version", info.versionName);
                data.add("journee", URLEncoder.encode(donnees, "utf-8"));
                if(journeeAttente.getAttente() == 3) {
                    data.add("premiere_liste_emails", reglages.getString("PRE_EMAIL", ""));
                    data.add("seconde_liste_emails", reglages.getString("SEC_EMAIL", ""));
                } else if(journeeAttente.getAttente() == 2) {
                    data.add("seconde_liste_emails", reglages.getString("SEC_EMAIL", ""));
                } else if(journeeAttente.getAttente() == 1) {
                    data.add("premiere_liste_emails", reglages.getString("PRE_EMAIL", ""));
                }
                String json = restJournee.envoieJournee(data, "email");
                mapper = new ObjectMapper();
                JsonNode retour = mapper.readTree(json);
                if(retour.get("statut").asInt(1) == 0) {
                    resultat(0);
	                return;
                } else {
                    message(String.format(getString(R.string.envoie_erreur_attente_serveur_details), retour.get("message").asText()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                message(getString(R.string.envoie_erreur_attente_serveur));
            }
        } else {
            message(getString(R.string.envoie_erreur_attente_connexion));
        }
        resultat(1);
    }

    @UiThread
    void resultat(int code) {
        if(code == 0) {
            message(getString(R.string.envoie_mail_envoye));
            journeeAttente.setAttente(0);
            daoJournee.modifierJournee(journeeAttente);
	        daoJournee.close();
        } else {
            message(getString(R.string.envoie_erreur_attente_reessayez));
        }
    }

    @UiThread
    void message(String texte) {
        Toast.makeText(this, texte, Toast.LENGTH_SHORT).show();
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}
