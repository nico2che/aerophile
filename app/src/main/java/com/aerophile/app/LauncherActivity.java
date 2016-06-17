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
import android.widget.Toast;

import com.aerophile.app.dao.JourneeDAO;
import com.aerophile.app.dao.VolDAO;
import com.aerophile.app.modeles.Journee;
import com.aerophile.app.modeles.Vol;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.rest.spring.annotations.RestService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

@EActivity
public class LauncherActivity extends AppCompatActivity {

    private JourneeDAO daoJournee;
    private Journee journeeAttente;

    @RestService
    JourneeClient restJournee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean envoieAttente = true;
        SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
        String code = reglages.getString("CODE_SECURITE", "KO");
        String lieu = reglages.getString("LIEU", "KO");
        String immatriculation = reglages.getString("IMMATRICULATION", "KO");

        String langue = reglages.getString("LANGUE", Locale.getDefault().toString());
        Locale locale = new Locale(langue);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        daoJournee = new JourneeDAO(this);
        daoJournee.open();
        Intent ecranDemarrage = new Intent(this, DemarrageActivity_.class);
        if(daoJournee.isJourneeEnCours()) {
            ecranDemarrage = new Intent(this, VolListActivity.class);
        }
        if(lieu.equals("KO") || immatriculation.equals("KO")) {
            ecranDemarrage = new Intent(this, ReglagesActivity_.class);
            envoieAttente = false;
        }
        if(code.equals("KO")) {
            ecranDemarrage = new Intent(this, AccueilActivity.class);
            envoieAttente = false;
        }
        if(envoieAttente) {
            journeeAttente = daoJournee.getJourneeEnAttente();
	        if(journeeAttente.getAttente() != 0) {
                message(getString(R.string.envoie_mail_attente));
		        requete();
	        } else {
		        daoJournee.close();
	        }
        } else {
	        daoJournee.close();
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
