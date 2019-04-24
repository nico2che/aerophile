package com.aerophile.app;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebView;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_privacy)
public class PrivacyActivity extends AppCompatActivity {

    @ViewById
    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.privacy));
    }

    @AfterViews
    void initialisation() {
        webView.loadUrl("file:///android_asset/privacy.html");
    }
}
