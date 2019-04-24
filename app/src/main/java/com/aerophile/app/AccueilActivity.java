package com.aerophile.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aerophile.app.modeles.Preferences_;
import com.aerophile.app.utils.Api;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@EActivity(R.layout.activity_accueil)
public class AccueilActivity extends AppCompatActivity {

    private ProgressDialog pDialog;

    @Bean
    Api api;

    @ViewById
    EditText code;

    @ViewById
    Button valider;

    @Pref
    Preferences_ reglages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Click
    void valider() {
        pDialog = new ProgressDialog(this);
        pDialog.setMessage(getString(R.string.chargement));
        pDialog.setCancelable(false);
        pDialog.show();
        checkCode();
    }

    @Background
    void checkCode() {
        String codeEntre = code.getText().toString();
        if(codeEntre.isEmpty()) {
            if (pDialog.isShowing())
                pDialog.dismiss();
            return;
        }
        MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
        data.add("code", codeEntre);
        String json = api.post(this, data, "code");
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode retour = mapper.readTree(json);
            if (retour.get("statut").asInt(1) == 0) {
                reglages.code().put(codeEntre);
                if (pDialog.isShowing())
                    pDialog.dismiss();
                ReglagesActivity_.intent(this).start();
                finish();
            } else {
               message(getString(R.string.accueil_code_incorrect));
            }
        } catch (Exception e) {
            e.printStackTrace();
            message(getString(R.string.erreur_connexion));
        }
    }

    @UiThread
    void message(String texte) {
        if (pDialog.isShowing())
            pDialog.dismiss();
        Toast.makeText(this, texte, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_accueil, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        Intent privacyActivity = new Intent(this, PrivacyActivity_.class);
        startActivity(privacyActivity);
        return true;
    }
}
