package com.aerophile.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import com.aerophile.app.R;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

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
    }

    @Click
    void buttonSave() {
        SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor reglages_editor = reglages.edit();
        reglages_editor.putString("LIEU", inputLieu.getText().toString());
        reglages_editor.putString("IMMATRICULATION", inputImmatriculation.getText().toString());
        reglages_editor.putString("PRE_EMAIL", inputPreEmail.getText().toString());
        reglages_editor.putString("SEC_EMAIL", inputSecEmail.getText().toString());
        reglages_editor.apply();

        Toast.makeText(ReglagesActivity.this, "Données enregistrées", Toast.LENGTH_SHORT).show();

	    if(enCours) {

		    finish();

	    } else {

		    Intent ecranDemarrage = new Intent(this, DemarrageActivity_.class);
		    startActivity(ecranDemarrage);
	    }
    }
}
