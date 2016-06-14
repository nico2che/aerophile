package com.aerophile.app;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.aerophile.app.R;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

@EActivity(R.layout.activity_pdf)
public class PdfActivity extends AppCompatActivity {

	@ViewById
	WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTitle("Pré-visualisation du PDF");
	}

	@AfterViews
	void initialisation() {

		Bundle extra = getIntent().getExtras();

		if(extra.getString("LIEN_PDF") != null) {

			final ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage("Chargement du PDF...");
			dialog.setCancelable(false);
			dialog.show();
			webView.setWebViewClient(new WebViewClient(){
				public void onPageFinished(WebView view, String url) {
					if (dialog.isShowing())
						dialog.dismiss();
				}
			});
			webView.getSettings().setJavaScriptEnabled(true);
			webView.loadUrl("http://docs.google.com/gview?embedded=true&url=" + extra.getString("LIEN_PDF"));

		} else {

			finish();
		}
	}
}
