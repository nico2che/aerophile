package com.aerophile.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.aerophile.app.R;

import org.androidannotations.annotations.EBean;
import org.androidannotations.rest.spring.annotations.RestService;
import org.springframework.util.MultiValueMap;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

@EBean
public class Api {

    @RestService
    Client restJournee;

    public String post(Context context, MultiValueMap<String, Object> data, String typeDonnees) {
        String retour = "";
        if(isOnline(context)) {
            try {
                Calendar cal = Calendar.getInstance();
                TimeZone tz = cal.getTimeZone();
                PackageManager manager = context.getPackageManager();
                PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
                data.add("appareil", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
                data.add("version", info.versionName);
                data.add("timezone", tz.getID());
            } catch (Exception e) {
                e.printStackTrace();
            }
            SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(context);
            retour = restJournee.envoieJournee(data, typeDonnees, reglages.getString("LANGUE", Locale.getDefault().toString()));
        } else {
            Toast.makeText(context, context.getString(R.string.erreur_connexion), Toast.LENGTH_SHORT).show();
        }
        return retour;
    }

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}
