package com.aerophile.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.aerophile.app.R;

public class AccueilActivity extends AppCompatActivity {

    private static String SECURITY_CODE = "aerophile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accueil);

        final EditText textCode = (EditText) findViewById(R.id.code);
        Button boutonVerification = (Button) findViewById(R.id.valider);
        boutonVerification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(textCode.getText().toString().equals(SECURITY_CODE)) {
                    SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(AccueilActivity.this);
                    SharedPreferences.Editor reglages_editor = reglages.edit();
                    reglages_editor.putString("CODE_SECURITE", "OK");
                    reglages_editor.apply();
                    Intent ecranReglages = new Intent(AccueilActivity.this, ReglagesActivity_.class);
                    startActivity(ecranReglages);
                    finish();
                } else {
                    Toast.makeText(AccueilActivity.this, "Code de sécurité incorrect", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
