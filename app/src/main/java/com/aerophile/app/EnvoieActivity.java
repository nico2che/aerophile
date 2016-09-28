package com.aerophile.app;

import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aerophile.app.dao.JourneeDAO;
import com.aerophile.app.dao.VolDAO;
import com.aerophile.app.modeles.Journee;
import com.aerophile.app.modeles.Vol;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.rest.spring.annotations.RestService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.net.URLEncoder;
import java.util.Date;
import java.util.List;

@EActivity(R.layout.activity_envoie)
public class EnvoieActivity extends AppCompatActivity {

    public static int FINISH = 3;
    public static int QUITTER = 4;

	private JourneeDAO daoJournee;
	private Journee journeeCourante;

	private ProgressDialog pDialog;

	@RestService
	JourneeClient restJournee;

	@ViewById
	EditText inputObjetEmail;

	@ViewById
	CheckBox checkPremiereListe;

	@ViewById
	CheckBox checkSecondeListe;

	@ViewById
	TextView textDateEnvoie;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if(getSupportActionBar() != null){
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		daoJournee = new JourneeDAO(this);
		daoJournee.open();
		journeeCourante = daoJournee.getJourneeEnCours();

		VolDAO daoVol = new VolDAO(this);
		daoVol.open();
		List<Vol> vols = daoVol.getVolsByJournee(journeeCourante.getId());
		for (Vol vol : vols) {
			journeeCourante.addVol(vol);
		}

		// Chagement du titre
		setTitle(String.format(getString(R.string.envoie_titre), journeeCourante.getDate()));
	}

	@AfterViews
	void initialise() {
		// Changement de l'objet du mail
		SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
		String immatriculation = reglages.getString("IMMATRICULATION", "?");
		inputObjetEmail.setText(String.format(getString(R.string.envoie_email_objet_string), immatriculation, journeeCourante.getDate()));
		if(journeeCourante.getDateEnvoie() != null) {
			textDateEnvoie.setText(String.format(getString(R.string.envoie_mail_date_envoie_ok), JourneeDAO.dateToStringHuman(journeeCourante.getDateEnvoie())));
		} else {
			textDateEnvoie.setText(getString(R.string.envoie_mail_date_envoie_ko));
		}
	}

	@Click
	void buttonPdf() {
		pDialog = new ProgressDialog(this);
		pDialog.setMessage(getString(R.string.envoie_generation_pdf));
		pDialog.setCancelable(false);
		pDialog.show();
		requete("pdf");
	}

	@Click
	void buttonQuitter() {
		journeeCourante.setEnCours(0);
		daoJournee.modifierJournee(journeeCourante);
        setResult(QUITTER);
        finish();
	}

	@Background
	void requete(String type) {
		if(isOnline()) {
			MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
			try {
				PackageManager manager = this.getPackageManager();
				PackageInfo info = manager.getPackageInfo(this.getPackageName(), 0);
				ObjectMapper mapper = new ObjectMapper();
				String donnees = mapper.writeValueAsString(journeeCourante);
				SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
				if(type.equals("email")) {
					data.add("objet_email", inputObjetEmail.getText().toString());
					if(checkPremiereListe.isChecked())
						data.add("premiere_liste_emails", reglages.getString("PRE_EMAIL", ""));
					if(checkSecondeListe.isChecked())
						data.add("seconde_liste_emails", reglages.getString("SEC_EMAIL", ""));
				}
				data.add("appareil", Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID));
				data.add("immatriculation", reglages.getString("IMMATRICULATION", "?"));
				data.add("lieu", reglages.getString("LIEU", "?"));
				data.add("version", info.versionName);
				data.add("journee", URLEncoder.encode(donnees, "utf-8"));
				String json = restJournee.envoieJournee(data, type);
				mapper = new ObjectMapper();
				JsonNode retour = mapper.readTree(json);
				if(retour.get("statut").asInt(1) == 0) {
					if(type.equals("pdf")) {
						afficherPDF(retour.get("pdf").asText());
					} else {
						resultat(0);
						return;
					}
				} else {
					if (pDialog.isShowing())
						pDialog.dismiss();
					message(String.format(getString(R.string.envoie_erreur_serveur_details), retour.get("message").asText()));
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (pDialog.isShowing())
					pDialog.dismiss();
				message(getString(R.string.envoie_erreur_connexion));
			}
		} else {
			if (pDialog.isShowing())
				pDialog.dismiss();
			message(getString(R.string.envoie_erreur_connexion));
		}
		if(type.equals("email")) {
			if(checkPremiereListe.isChecked() && checkSecondeListe.isChecked())
				resultat(3);
			else if(checkPremiereListe.isChecked())
				resultat(1);
			else if(checkSecondeListe.isChecked())
				resultat(2);
		}
	}

	@UiThread
	void message(String texte) {
		Toast.makeText(this, texte, Toast.LENGTH_SHORT).show();
	}

	@UiThread
	void afficherPDF(String lienPDF) {
		if (pDialog.isShowing())
			pDialog.dismiss();
		Intent pdfActivity = new Intent(this, PdfActivity_.class);
		pdfActivity.putExtra("LIEN_PDF", lienPDF);
		startActivity(pdfActivity);
	}

	@Click
	void buttonEnvoie() {
		if(checkPremiereListe.isChecked() || checkSecondeListe.isChecked()) {

			pDialog = new ProgressDialog(this);
			pDialog.setMessage(getString(R.string.envoie_envoie_mail));
			pDialog.setCancelable(false);
			pDialog.show();

			requete("email");

		} else {
			message(getString(R.string.envoie_erreur_liste_email));
		}
	}

	@UiThread
	void resultat(int code) {
		if(code == 0) {
			message(getString(R.string.envoie_mail_envoye));
			journeeCourante.setDateEnvoie(new Date());
		} else {
			journeeCourante.setObjetAttente(inputObjetEmail.getText().toString());
			journeeCourante.setAttente(code);
			message(getString(R.string.envoie_erreur_attente_email));
		}
		journeeCourante.setEnCours(0);
		daoJournee.modifierJournee(journeeCourante);

		if (pDialog.isShowing())
			pDialog.dismiss();

		setResult(FINISH);
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				Intent upIntent = NavUtils.getParentActivityIntent(this);
				if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
					TaskStackBuilder.create(this)
							.addNextIntentWithParentStack(upIntent)
							.startActivities();
				} else {
					NavUtils.navigateUpTo(this, upIntent);
				}
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
	}
}
