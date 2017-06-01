package com.aerophile.app.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.provider.Settings;
import android.widget.Toast;

import com.aerophile.app.R;
import com.aerophile.app.modeles.Preferences_;

import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.androidannotations.rest.spring.annotations.RestService;
import org.springframework.util.MultiValueMap;

import java.util.Calendar;
import java.util.TimeZone;

@EBean
public class Api {

    @Pref
    Preferences_ reglages;

    @RestService
    Client restJournee;

    Context ctx;

    public String post(Context context, MultiValueMap<String, Object> data, String typeDonnees) {
        ctx = context;
        String retour = "";
        if(isOnline(ctx)) {
            try {
                Calendar cal = Calendar.getInstance();
                TimeZone tz = cal.getTimeZone();
                PackageManager manager = context.getPackageManager();
                PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
                data.add("appareil", Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
                data.add("version", info.versionName);
                data.add("timezone", tz.getID());
                retour = restJournee.envoieJournee(data, typeDonnees, reglages.langue().getOr("fr"));
            } catch (Exception e) {
                e.printStackTrace();
                message(ctx.getString(R.string.erreur_connexion));
            }
        } else {
            message(ctx.getString(R.string.erreur_connexion));
        }
        return retour;
    }

    public boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    @UiThread
    void message(String texte) {
        Toast.makeText(ctx, texte, Toast.LENGTH_SHORT).show();
    }
}
