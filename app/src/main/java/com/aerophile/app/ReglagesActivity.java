package com.aerophile.app;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.aerophile.app.modeles.Preferences_;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.Locale;

@EActivity(R.layout.activity_reglages)
public class ReglagesActivity extends AppCompatActivity {

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

    @ViewById
    TextView textVersion;

    @Pref
    Preferences_ reglages;

    @Extra("EN_COURS")
    int idJourneeEnCours;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @AfterViews
    void init() {
        try {
            PackageManager manager = this.getPackageManager();
            PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
            textVersion.setText(String.format(getString(R.string.reglages_version), info.versionName));
        } catch (Exception e) {
            textVersion.setVisibility(View.INVISIBLE);
        }
        inputLieu.setText(reglages.lieu().get());
        inputImmatriculation.setText(reglages.immatriculation().get());
        inputPreEmail.setText(reglages.premierEmail().get());
        inputSecEmail.setText(reglages.secondEmail().get());
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

        reglages.edit()
                .lieu().put(inputLieu.getText().toString())
                .immatriculation().put(inputImmatriculation.getText().toString())
                .premierEmail().put(inputPreEmail.getText().toString())
                .secondEmail().put(inputSecEmail.getText().toString())
                .langue().put(langue)
                .apply();

        Toast.makeText(ReglagesActivity.this, getString(R.string.demarrage_donnees_enregistrees), Toast.LENGTH_SHORT).show();

	    if(idJourneeEnCours != 0) {

		    finish();

	    } else {

            DemarrageActivity_.intent(this).start();
	    }
    }

    public void onBackPressed() {
        if(idJourneeEnCours != 0)
            this.finish();
    }
}
