package com.aerophile.app;

import android.app.ProgressDialog;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aerophile.app.R;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.rest.spring.annotations.Rest;
import org.androidannotations.rest.spring.annotations.RestService;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		setTitle("Journée du " + journeeCourante.getDate());
	}

	@AfterViews
	void initialise() {
		// Changement de l'objet du mail
		SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
		String immatriculation = reglages.getString("IMMATRICULATION", "?");
		inputObjetEmail.setText(String.format(getString(R.string.envoie_email_objet_string), immatriculation, journeeCourante.getDate()));
	}

	@Click
	void buttonPdf() {
		pDialog = new ProgressDialog(this);
		pDialog.setMessage("Génération du PDF en cours...");
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
				data.add("appareil", Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID) );
				data.add("immatriculation", reglages.getString("IMMATRICULATION", "?"));
				data.add("lieu", reglages.getString("LIEU", "?"));
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
					message("Erreur serveur : " + retour.get("message").asText());
				}
			} catch (Exception e) {
				e.printStackTrace();
				if (pDialog.isShowing())
					pDialog.dismiss();
				message("Impossible de contacter le serveur, vérifiez votre connexion internet");
			}
		} else {
			if (pDialog.isShowing())
				pDialog.dismiss();
			message("Vérifiez votre connexion internet");
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
		Log.d("LIEN", lienPDF);
		startActivity(pdfActivity);
	}

	@Click
	void buttonEnvoie() {
		if(checkPremiereListe.isChecked() || checkSecondeListe.isChecked()) {

			pDialog = new ProgressDialog(this);
			pDialog.setMessage("Envoi du mail en cours...");
			pDialog.setCancelable(false);
			pDialog.show();

			requete("email");

		} else {
			message("Merci de préciser au moins une liste d'email");
		}
	}

	@UiThread
	void resultat(int code) {
		if(code == 0) {
			message("Mail bien envoyé");
		} else {
			journeeCourante.setObjetAttente(inputObjetEmail.getText().toString());
			journeeCourante.setAttente(code);
			message("Envoie impossible, mail mis en attente");
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
