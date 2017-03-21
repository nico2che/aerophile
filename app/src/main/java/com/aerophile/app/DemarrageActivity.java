package com.aerophile.app;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.aerophile.app.dao.JourneeDAO;
import com.aerophile.app.dao.VolDAO;
import com.aerophile.app.modeles.Journee;
import com.aerophile.app.modeles.Vol;
import com.aerophile.app.utils.Api;
import com.aerophile.app.utils.Dates;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.androidannotations.annotations.AfterExtras;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@EActivity(R.layout.activity_demarrage)
public class DemarrageActivity extends AppCompatActivity implements DatePickerDialog.OnDateSetListener {

	public static int CHANGEMENT = 1;
	private static int MODIFICATION = 2;
	public final static int CALLBACK_APP = 1;
	public final static int CALLBACK_DEMARRAGE = 3;

	private int heureOuverture;
	private int minuteOuverture;

	private int heureFermeture;
	private int minuteFermeture;

	private ProgressDialog pDialog;

	private Journee journeeCourante;
	private JourneeDAO daoJournee;
	private boolean enCours;
	private boolean changement;

	private Calendar dateSelectionnee;

	private Journee nouvelleJournee;

	@Bean
	Api api;

    @ViewById
    TextView textDateHolder;

	@ViewById
	CheckBox checkLms;

	@ViewById
	TextView textLms;

	@ViewById
	TextView textLift;

	@ViewById
	EditText inputLift;

	@ViewById
	Button buttonPdf;

    @ViewById
    EditText inputTemperature;

	@ViewById
	CheckBox checkValidation;

	@ViewById
	EditText inputPiloteValidation;

	@ViewById
	Button buttonOuverture;

	@ViewById
	Button buttonFermeture;

	@ViewById
	CheckBox checkPasdeVol;

    @ViewById
    DrawView canvasDrawer;

	@ViewById
	ImageView imageSignature;

	@ViewById
	Button buttonSave;

	@Extra("EN_COURS")
	long idJourneeEnCours;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

	@AfterExtras
	void extras() {

		// On initialise sans changement pour l'instant
		changement = false;

		// On récupère la date actuelle
		dateSelectionnee = Calendar.getInstance();
		Log.d("AEROBUG", "[AE] " + dateSelectionnee.getTime() + "");

		// On ouvre la base de données
		daoJournee = new JourneeDAO(this);
		daoJournee.open();

		// On récupère les informations de la dernière journée
		Journee derniereJournee = daoJournee.getDerniereJournee();

		// Si on des horaires d'ouverture/fermeture de cette dernière journée, on les applique
		if(derniereJournee.getHeureOuverture() != null && !derniereJournee.getHeureOuverture().isEmpty()) {

			heureOuverture = Integer.parseInt(derniereJournee.getHeureOuverture().substring(0, 2));
			minuteOuverture = Integer.parseInt(derniereJournee.getHeureOuverture().substring(3, 5));

			heureFermeture = Integer.parseInt(derniereJournee.getHeureFermeture().substring(0, 2));
			minuteFermeture = Integer.parseInt(derniereJournee.getHeureFermeture().substring(3, 5));

		} else {

			// Sinon on initialise les horaires à 8h00 et 20h00
			heureOuverture = 8;
			minuteOuverture = 0;

			heureFermeture = 20;
			minuteFermeture = 0;
		}
	}

    @AfterViews
    void init() {

		Log.d("AEROBUG", "[AV] " + idJourneeEnCours);

	    // Si la journée est en cours
	    if (idJourneeEnCours != 0) {

		    // Alors on indique qu'on veut la modifier
		    setTitle(getString(R.string.demarrage_modifier_journee));
		    buttonSave.setText(getString(R.string.demarrage_modifier_journee));

		    // On récupère les informations de cette journée en cours
		    journeeCourante = daoJournee.getJourneeEnCours();

		    // On met à jour chaque champs de l'activité avec la journée courante
		    miseAJourInputs(journeeCourante);

	    } else {

		    // Si c'est une nouvelle journée
		    imageSignature.setVisibility(View.INVISIBLE);
		    canvasDrawer.setVisibility(View.VISIBLE);
		    buttonPdf.setVisibility(View.INVISIBLE);

		    verificationJournee(dateSelectionnee.getTime());
	    }

		// Actionne les listenner sur les deux checkbox (lift et pas de vol)
		checkLms.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				toggleLift();
			}
		});
		checkPasdeVol.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				togglePasDeVol();
			}
		});
    }

	public void miseAJourInputs(Journee journee) {
		miseAJourInputs(journee, false);
	}

	public void miseAJourInputs(Journee journee, boolean vide) {
		// Date de la journée sélectionnée
		textDateHolder.setText(Dates.dateToReadable(journee.getDate()));
		if (journee.getDate() != null) {
			dateSelectionnee.setTime(journee.getDate());
		}
		// Prévisualiser le PDF
		if(vide) {
			buttonPdf.setVisibility(View.INVISIBLE);
		} else {
			buttonPdf.setVisibility(View.VISIBLE);
		}
		// Température
		if(vide || journee.getTemperature() == 0) {
			inputTemperature.setText("");
		} else {
			inputTemperature.setText(String.valueOf(journee.getTemperature()));
		}
		// Validation Quotidienne
		checkValidation.setChecked(!vide);
		// Lift
		if(!vide && journee.getLift() != null && journee.getLift().equals("LMS")) {
			checkLms.setChecked(true);
			inputLift.setVisibility(View.INVISIBLE);
			textLift.setVisibility(View.INVISIBLE);
			inputLift.setText("");
		} else {
			checkLms.setChecked(false);
			inputLift.setVisibility(View.VISIBLE);
			textLift.setVisibility(View.VISIBLE);
			inputLift.setText(journee.getLift());
		}
		// Ouverture
		if (journee.getHeureOuverture() != null && !journee.getHeureOuverture().isEmpty()) {
			heureOuverture = Integer.parseInt(journee.getHeureOuverture().substring(0, 2));
			minuteOuverture = Integer.parseInt(journee.getHeureOuverture().substring(3, 5));
		}
		buttonOuverture.setText(formatHeure(heureOuverture, minuteOuverture));
		// Fermeture
		if (journee.getHeureFermeture() != null && !journee.getHeureFermeture().isEmpty()) {
			heureFermeture = Integer.parseInt(journee.getHeureFermeture().substring(0, 2));
			minuteFermeture = Integer.parseInt(journee.getHeureFermeture().substring(3, 5));
		}
		buttonFermeture.setText(formatHeure(heureFermeture, minuteFermeture));
		// Pas de vol ?
		if(journee.getPasdeVol() == 1) {
			checkPasdeVol.setChecked(true);
		} else {
			checkPasdeVol.setChecked(false);
		}
		togglePasDeVol();
		// Pilote
		inputPiloteValidation.setText(journee.getPiloteValidation());
		// Signature
		String signature = journee.getSignature();
		if (signature != null && !signature.isEmpty()) {
			canvasDrawer.empty = false;
			imageSignature.setImageBitmap(DrawView.importFrom64(signature));
			imageSignature.setVisibility(View.VISIBLE);
			canvasDrawer.setVisibility(View.INVISIBLE);
		} else {
			canvasDrawer.empty = true;
			imageSignature.setVisibility(View.INVISIBLE);
			canvasDrawer.setVisibility(View.VISIBLE);
		}
	}

	void verificationJournee(Date date) {

		// On regarde si une journée existe avec cette nouvelle date
		nouvelleJournee = daoJournee.getJourneeByDate(date.getTime());

		// Si elle existe
		if(nouvelleJournee.getId() != 0) {

			if(enCours) {

				if(nouvelleJournee.getId() != journeeCourante.getId()) {

					setTitle(getString(R.string.demarrage_charger_journee));
					buttonSave.setText(getString(R.string.demarrage_charger_journee));

					// On remet la bonne date
					nouvelleJournee.setDate(date);
					// On met à jour les champs de l'activité avec la nouvelle journée
					miseAJourInputs(nouvelleJournee);
					// Il y a un changement
					changement = true;

				} else {

					setTitle(getString(R.string.demarrage_modifier_journee));
					buttonSave.setText(getString(R.string.demarrage_modifier_journee));

					// On remet la bonne date
					journeeCourante.setDate(date);
					// On remet la journée courante
					miseAJourInputs(journeeCourante);
					// Il n'y a pas de changement
					changement = false;
				}

			} else {

				setTitle(getString(R.string.demarrage_charger_journee));
				buttonSave.setText(getString(R.string.demarrage_charger_journee));

				// On remet la bonne date
				nouvelleJournee.setDate(date);
				// On met à jour les champs de l'activité avec la nouvelle journée
				miseAJourInputs(nouvelleJournee);

				changement = true;
			}


		} else {

			// Il n'y a donc pas de changement
			changement = false;

			// Seulement si la journée est en cours
			if(enCours) {

				setTitle(getString(R.string.demarrage_modifier_journee));
				buttonSave.setText(getString(R.string.demarrage_modifier_journee));

				journeeCourante.setDate(date);

				// On remet les informations de la journée courante
				miseAJourInputs(journeeCourante);

			} else {

				setTitle(getString(R.string.demarrage_debut_journee));
				buttonSave.setText(getString(R.string.demarrage_debut_journee));

				// On remet les champs vide
				Journee journeeVide = new Journee();
				journeeVide.setDate(date);
				miseAJourInputs(journeeVide, true);
			}
		}
	}

	@Click
	void buttonDate() {
		DatePickerDialog date = new DatePickerDialog(DemarrageActivity.this, this,
				dateSelectionnee.get(Calendar.YEAR),
				dateSelectionnee.get(Calendar.MONTH),
				dateSelectionnee.get(Calendar.DAY_OF_MONTH)
		);
		date.show();
	}

	public void onDateSet(DatePicker view, int annee, int mois, int jour) {
		// On créer une date avec les valeurs entrées
		Calendar nouvelleDate = Calendar.getInstance();
		nouvelleDate.set(annee, mois, jour, 0, 0, 0);
		// On compare cette date avec l'heure actuelle
		if(nouvelleDate.compareTo(Calendar.getInstance()) > 0) {
			// Si la date entrée se trouve après maintenant -> donc demain, on interdit
			message(getString(R.string.envoie_erreur_date));
			return;
		}
		// On indique la nouvelle date sélectionnée
		dateSelectionnee.set(annee, mois, jour);
		// On change la journée en conséquence
		verificationJournee(dateSelectionnee.getTime());
	}

	@Click
	void buttonEffacer() {
		canvasDrawer.clear();
		imageSignature.setVisibility(View.INVISIBLE);
		canvasDrawer.setVisibility(View.VISIBLE);
	}

	@Click({R.id.buttonSave})
    void demarrerJournee() {

        String dateJournee = textDateHolder.getText().toString();
		String liftJournee = "";
		if(inputLift.getText().toString().isEmpty()) {
			if(checkLms.isChecked()) {
				liftJournee = "LMS";
			}
		} else {
			if(checkLms.isChecked()) {
				liftJournee = "LMS";
			} else {
				liftJournee = inputLift.getText().toString();
			}
		}
		String temperatureJournee = inputTemperature.getText().toString();
		String horaireOuverture = buttonOuverture.getText().toString();
		String horaireFermeture = buttonFermeture.getText().toString();
		String piloteValidation = inputPiloteValidation.getText().toString();
		String nouvelleSignature = DrawView.exportTo64(canvasDrawer);
		int pasDeVol = (checkPasdeVol.isChecked() ? 1 : 0);
		if(pasDeVol == 1) {
			liftJournee = "N/A";
		}

		if(!dateJournee.isEmpty() && !liftJournee.isEmpty() && !temperatureJournee.isEmpty() && checkValidation.isChecked() && !piloteValidation.isEmpty() && !canvasDrawer.empty) {

			if(changement) {

				// Si il y a changement, alors la journee courante n'est plus en cours
				daoJournee.aucuneJourneeEnCours();

				nouvelleJournee.setEnCours(1);
				nouvelleJournee.setDate(dateSelectionnee.getTime());
				nouvelleJournee.setTemperature(Integer.valueOf(temperatureJournee));
				nouvelleJournee.setLift(liftJournee);
				nouvelleJournee.setHeureOuverture(horaireOuverture);
				nouvelleJournee.setHeureFermeture(horaireFermeture);
				nouvelleJournee.setPiloteValidation(piloteValidation);
				nouvelleJournee.setPasDeVol(pasDeVol);
				if(canvasDrawer.getVisibility() == View.VISIBLE) {
					nouvelleJournee.setSignature(nouvelleSignature);
				}
				daoJournee.modifierJournee(nouvelleJournee);

				Toast.makeText(DemarrageActivity.this, getString(R.string.demarrage_chargement_effectue), Toast.LENGTH_SHORT).show();

				if(enCours) {

					// On enregistre
					daoJournee.modifierJournee(journeeCourante);

					setResult(CHANGEMENT);
					finish();

				} else {

					VolListActivity_.intent(this).idJournee((int) nouvelleJournee.getId()).start();
					finish();
				}

			} else if(enCours) {

		        journeeCourante.setDate(dateSelectionnee.getTime());
		        journeeCourante.setTemperature(Integer.valueOf(temperatureJournee));
		        journeeCourante.setLift(liftJournee);
				journeeCourante.setHeureOuverture(horaireOuverture);
				journeeCourante.setHeureFermeture(horaireFermeture);
				journeeCourante.setPiloteValidation(piloteValidation);
				journeeCourante.setPasDeVol(pasDeVol);
		        if(canvasDrawer.getVisibility() == View.VISIBLE) {
			        journeeCourante.setSignature(nouvelleSignature);
		        }
		        daoJournee.modifierJournee(journeeCourante);
		        Toast.makeText(DemarrageActivity.this, getString(R.string.demarrage_donnees_enregistrees), Toast.LENGTH_SHORT).show();
				setResult(MODIFICATION);
		        finish();

	        } else {

				Journee debutJournee = new Journee();
				debutJournee.setDate(dateSelectionnee.getTime());
				debutJournee.setLift(liftJournee);
				debutJournee.setTemperature(Integer.parseInt(temperatureJournee));
				debutJournee.setHeureOuverture(horaireOuverture);
				debutJournee.setHeureFermeture(horaireFermeture);
				debutJournee.setPiloteValidation(piloteValidation);
				debutJournee.setSignature(nouvelleSignature);
				debutJournee.setPasDeVol(pasDeVol);
				daoJournee.creerJournee(debutJournee);
				Intent ecranVols = new Intent(this, VolListActivity_.class);
				startActivity(ecranVols);
				finish();
	        }

        } else {

            Toast.makeText(DemarrageActivity.this, getString(R.string.demarrage_champs_obligatoires), Toast.LENGTH_SHORT).show();
        }
    }

	@Click
	void buttonPdf() {
		pDialog = new ProgressDialog(this);
		pDialog.setMessage(getString(R.string.envoie_generation_pdf));
		pDialog.setCancelable(false);
		pDialog.show();
		requete();
	}

	@Background
	void requete() {
		VolDAO daoVol = new VolDAO(this);
		daoVol.open();

		Journee journeeEnvoie;
		if(changement) {
			journeeEnvoie = nouvelleJournee;
		} else if(enCours) {
			journeeEnvoie = journeeCourante;
		} else {
			message(getString(R.string.envoie_erreur_journee_existe_pas));
			return;
		}

		if(daoVol.getDernierVol(journeeEnvoie.getId()).getEnCours() == 1) {
			if (pDialog.isShowing())
				pDialog.dismiss();
			message(getString(R.string.envoie_erreur_vol_en_cours));
			return;
		}

		if(!isOnline()) {
			if (pDialog.isShowing())
				pDialog.dismiss();
			message(getString(R.string.envoie_erreur_connexion));
			return;
		}

		MultiValueMap<String, Object> data = new LinkedMultiValueMap<>();
		List<Vol> vols = daoVol.getVolsByJournee(journeeEnvoie.getId());
		journeeEnvoie.VOLS.clear();
		for (Vol vol : vols) {
			journeeEnvoie.addVol(vol);
		}
		try {
			ObjectMapper mapper = new ObjectMapper();
			String donnees = mapper.writeValueAsString(journeeEnvoie);
			SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
			data.add("immatriculation", reglages.getString("IMMATRICULATION", "?"));
			data.add("lieu", reglages.getString("LIEU", "?"));
			data.add("journee", URLEncoder.encode(donnees, "utf-8"));
			String json = api.post(this, data, "pdf");
			mapper = new ObjectMapper();
			JsonNode retour = mapper.readTree(json);
			if(retour.get("statut").asInt(1) == 0) {
				afficherPDF(retour.get("pdf").asText());
			} else {
				if (pDialog.isShowing())
					pDialog.dismiss();
				message(String.format(getString(R.string.envoie_erreur_serveur_details), retour.get("message").asText()));
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			if (pDialog.isShowing())
				pDialog.dismiss();
			message(getString(R.string.envoie_erreur_serveur));
		} catch (IOException e) {
			e.printStackTrace();
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
	void buttonOuverture() {
		TimePickerQuart dialogueOuverture = new TimePickerQuart(this, new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int heure, int minute) {
				heureOuverture = heure;
				minuteOuverture = minute;
				buttonOuverture.setText(formatHeure(heure, minute));
			}
		}, heureOuverture, minuteOuverture, true);
		dialogueOuverture.show();
	}

	@Click
	void buttonFermeture() {
		TimePickerQuart dialogueFermeture = new TimePickerQuart(this, new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int heure, int minute) {
				heureFermeture = heure;
				minuteFermeture = minute;
				buttonFermeture.setText(formatHeure(heure, minute));
			}
		}, heureFermeture, minuteFermeture, true);
		dialogueFermeture.show();
	}

	private String formatHeure(int heure, int minute) {
		return String.format(getString(R.string.vol_heure_holder), ((heure < 10) ? "0" + heure : String.valueOf(heure)), ((minute < 10) ? "0" + minute : String.valueOf(minute)));
	}

	private void toggleLift() {
		if(checkLms.isChecked()) {
			inputLift.setVisibility(View.INVISIBLE);
			textLift.setVisibility(View.INVISIBLE);
		} else {
			inputLift.setVisibility(View.VISIBLE);
			textLift.setVisibility(View.VISIBLE);
		}
	}

	private void togglePasDeVol() {
		if(checkPasdeVol.isChecked()) {
			textLms.setVisibility(View.INVISIBLE);
			checkLms.setVisibility(View.INVISIBLE);
			textLift.setVisibility(View.INVISIBLE);
			inputLift.setVisibility(View.INVISIBLE);
		} else {
			textLms.setVisibility(View.VISIBLE);
			checkLms.setVisibility(View.VISIBLE);
			textLift.setVisibility(View.VISIBLE);
			inputLift.setVisibility(View.VISIBLE);
			toggleLift();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("date", dateSelectionnee.getTimeInMillis());
		outState.putString("lift", inputLift.getText().toString());
		outState.putString("temperature", inputTemperature.getText().toString());
		outState.putString("pilote", inputPiloteValidation.getText().toString());
		outState.putString("heureOuverture", buttonOuverture.getText().toString());
		outState.putString("heureFermeture", buttonFermeture.getText().toString());
		outState.putInt("pasdevol", (checkPasdeVol.isChecked() ? 1 : 0));
		outState.putInt("signature", canvasDrawer.getVisibility());
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedState) {
		super.onRestoreInstanceState(savedState);
		dateSelectionnee = Calendar.getInstance();
		dateSelectionnee.setTimeInMillis(savedState.getLong("date"));
		verificationJournee(dateSelectionnee.getTime());
		inputLift.setText(savedState.getString("lift"));
		inputTemperature.setText(savedState.getString("temperature"));
		inputPiloteValidation.setText(savedState.getString("pilote"));
		buttonOuverture.setText(savedState.getString("heureOuverture"));
		buttonFermeture.setText(savedState.getString("heureFermeture"));
		checkPasdeVol.setChecked(savedState.getInt("pasdevol") == 1);
		canvasDrawer.setVisibility((savedState.getInt("signature") == View.VISIBLE ? View.VISIBLE : View.INVISIBLE));
	}

	public boolean isOnline() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_demarrage, menu);
		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("CALLBACK", String.valueOf(requestCode));
		if (requestCode == CALLBACK_APP) {
			Intent restartDemarrage = new Intent();
			restartDemarrage.setClass(this, this.getClass());
			startActivity(restartDemarrage);
			finish();
		}
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_action_reglages:
				Intent ecranParametreApplication = new Intent(this, ReglagesActivity_.class);
				ecranParametreApplication.putExtra("EN_COURS", 1);
				startActivityForResult(ecranParametreApplication, CALLBACK_APP);
				return true;
			default:
				return false;
		}
	}
}