package com.aerophile.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.aerophile.app.R;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.util.Locale;

@EActivity(R.layout.activity_reglages)
public class ReglagesActivity extends AppCompatActivity {

    private boolean enCours = false;

    @ViewById
    EditText inputLieu;

    @ViewById
    EditText inputImmatriculation;

    @ViewById
    EditText inputPreEmail;

    @ViewById
    EditText inputSecEmail;

    @ViewById
    Spinner spinnerLangue;

    public void onBackPressed() {
        if(enCours)
            this.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        enCours = (extras != null && extras.getInt("EN_COURS") == 1);
    }

    @AfterViews
    void init() {
        SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
        inputLieu.setText(reglages.getString("LIEU", ""));
        inputImmatriculation.setText(reglages.getString("IMMATRICULATION", ""));
        inputPreEmail.setText(reglages.getString("PRE_EMAIL", ""));
        inputSecEmail.setText(reglages.getString("SEC_EMAIL", ""));
        spinnerLangue.setSelection(((ArrayAdapter<String>)spinnerLangue.getAdapter()).getPosition(getString(R.string.reglages_langue)));
    }

    @Click
    void buttonSave() {

        String langue;
        switch (spinnerLangue.getSelectedItemPosition()) {
            case 0:
                langue = "fr";
                break;
            case 1:
                langue = "en";
                break;
            default:
                langue = "fr";
                break;
        }
        Locale locale = new Locale(langue);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());

        SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor reglages_editor = reglages.edit();
        reglages_editor.putString("LIEU", inputLieu.getText().toString());
        reglages_editor.putString("IMMATRICULATION", inputImmatriculation.getText().toString());
        reglages_editor.putString("PRE_EMAIL", inputPreEmail.getText().toString());
        reglages_editor.putString("SEC_EMAIL", inputSecEmail.getText().toString());
        reglages_editor.putString("LANGUE", langue);
        reglages_editor.apply();

        Toast.makeText(ReglagesActivity.this, getString(R.string.demarrage_donnees_enregistrees), Toast.LENGTH_SHORT).show();

	    if(enCours) {

		    finish();

	    } else {

		    Intent ecranDemarrage = new Intent(this, DemarrageActivity_.class);
		    startActivity(ecranDemarrage);
	    }
    }
}
