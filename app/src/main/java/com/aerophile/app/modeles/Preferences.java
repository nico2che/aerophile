package com.aerophile.app.modeles;

import org.androidannotations.annotations.sharedpreferences.SharedPref;

@SharedPref(value=SharedPref.Scope.UNIQUE)
public interface Preferences {

    String code();
    String lieu();
    String immatriculation();
    String premierEmail();
    String secondEmail();
    String langue();
}
