package com.aerophile.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.widget.Toast;

import com.aerophile.app.JourneeClient;
import com.aerophile.app.R;

import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.EIntentService;
import org.androidannotations.annotations.Extra;
import org.androidannotations.rest.spring.annotations.RestService;
import org.springframework.util.MultiValueMap;

import java.util.Locale;

@EBean
public class Api {

    @RestService
    JourneeClient restJournee;

    public String post(Context context, MultiValueMap<String, Object> data, String typeDonnees) {
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            data.add("appareil", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
            data.add("version", info.versionName);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, context.getString(R.string.envoie_erreur_attente_serveur), Toast.LENGTH_SHORT).show();
        }
        SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(context);
        return restJournee.envoieJournee(data, typeDonnees, reglages.getString("LANGUE", Locale.getDefault().toString()));
    }
}
