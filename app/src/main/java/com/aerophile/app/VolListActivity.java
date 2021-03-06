package com.aerophile.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aerophile.app.dao.JourneeDAO;
import com.aerophile.app.dao.VolDAO;
import com.aerophile.app.modeles.Journee;
import com.aerophile.app.modeles.Preferences_;
import com.aerophile.app.modeles.Vol;
import com.aerophile.app.utils.Dates;

import org.androidannotations.annotations.AfterExtras;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@EActivity(R.layout.activity_vol_list)
public class VolListActivity extends AppCompatActivity
        implements VolListFragment.Callbacks {

	public VolListFragment list;

	private VolDAO daoVol;
	private Journee journeeCourante;
	private JourneeDAO daoJournee;
	private int pasDeVol = 0;

	public int positionActuelle;

	private VolDetailFragment fragment;

	private long itemSelected;

	public final static int CALLBACK_JOURNEE = 0;
	public final static int CALLBACK_APP = 1;
	public final static int CALLBACK_ENVOIE = 2;

	@ViewById
	View ecranPrincipal;

	@ViewById
	View ecranPasVol;

	@ViewById
	TextView textBottomPassagers;

	@ViewById
	Button buttonFinDeJournee;

	@Extra("JOURNEE")
	long idJournee;

    @Pref
    Preferences_ reglages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@AfterExtras
	public void extras() {
		daoVol = new VolDAO(this);
		daoVol.open();

		daoJournee = new JourneeDAO(this);
		daoJournee.open();

		if(idJournee != 0) {
			journeeCourante = daoJournee.get(idJournee);
			Log.d("AEROBUG", "Chargement de la journée via INTENT (" + idJournee + ")");
		} else {
			journeeCourante = daoJournee.getJourneeEnCours();
			idJournee = journeeCourante.getId();
			Log.d("AEROBUG", "Chargement de la journée via DB (" + journeeCourante.getId() + ")");
		}
	}

	@AfterViews
	public void view() {

		setTitle(Dates.dateToReadable(journeeCourante.getDate()));

		TextView textImmatriculation = (TextView) findViewById(R.id.textBottomImmat);
		if(textImmatriculation != null) {
			textImmatriculation.setText(String.format(getString(R.string.vol_immatriculation_ballon), reglages.immatriculation().get()));
		}

		EditText inputCommentairePasVol = (EditText) findViewById(R.id.inputPasVolCommentaires);
		if(inputCommentairePasVol != null) {
			inputCommentairePasVol.setText(journeeCourante.getCommentairePasDeVol());
		}

		if(inputCommentairePasVol != null) {
			inputCommentairePasVol.addTextChangedListener(new TextWatcher() {
				public void afterTextChanged(Editable s) {
					journeeCourante.setCommentairePasDeVol(s.toString());
					daoJournee.modifierJournee(journeeCourante);
				}

				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				public void onTextChanged(CharSequence s, int start, int before, int count) {
				}
			});
		}

		if(journeeCourante.getPasdeVol() == 1) {
			togglePasDeVol();
		}

		refreshBottomPassagers(-1);

		// In two-pane mode, list items should be given the
		// 'activated' state when touched.
		list = (VolListFragment) getSupportFragmentManager()
				.findFragmentById(R.id.vol_list);
		list.setActivateOnItemClick(true);
    }

	@Click
	void buttonFinDeJournee() {
		if(daoVol.getDernierVol(journeeCourante.getId()).getEnCours() == 1) {
			Toast.makeText(getApplicationContext(), getString(R.string.vol_vols_erreur), Toast.LENGTH_SHORT).show();
		} else {
			EnvoieActivity_.intent(this).idJournee(idJournee).startForResult(CALLBACK_ENVOIE);
		}
	}

	public void refreshBottomPassagers(long position) {
		List<Vol> volsJournee = daoVol.getVolsByJournee(journeeCourante.getId());
		int nombrePassagers = 0;
		int temps = 0;
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.FRANCE);
		if(volsJournee.size() > 0) {
			for (Vol vol : volsJournee) {
				if(vol.getNombrePassagers() != 0) {
					nombrePassagers += vol.getNombrePassagers();
				}
				if(vol.getDateAtterrissage() != null && vol.getDateDecollage() != null && !vol.getDateDecollage().isEmpty() && !vol.getDateAtterrissage().isEmpty()) {
					Date heureAtterrissage = new Date();
					Date heureDecollage = new Date();
					try {
						heureAtterrissage = sdf.parse(vol.getDateAtterrissage());
						heureDecollage = sdf.parse(vol.getDateDecollage());
					} catch (ParseException e) {
						e.printStackTrace();
					}
					temps += Math.abs(heureAtterrissage.getTime() - heureDecollage.getTime()) / 1000;
				}
			}
		}
		String heures = "0h";
		if(temps > 0) {
			int heure = temps / 3600;
			int minute = (temps % 3600) / 60;
			heures = heure + "h" + ((minute < 10) ? "0" + minute : minute);
		}
		textBottomPassagers.setText(String.format(getString(R.string.vol_stats), volsJournee.size(), nombrePassagers, heures));
		if(position != -1)
			itemSelected = position;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CALLBACK_JOURNEE) {
			if(resultCode == DemarrageActivity.CHANGEMENT) {
				Intent intent = getIntent();
				finish();
				startActivity(intent);
			} else {
				int ancienVol = journeeCourante.getPasdeVol();
				journeeCourante = daoJournee.getJourneeEnCours();
				if(journeeCourante.getPasdeVol() != ancienVol) {
					togglePasDeVol();
				}
				setTitle(Dates.dateToReadable(journeeCourante.getDate()));
			}
		}
		if (requestCode == CALLBACK_APP) {
			VolListActivity_.intent(this).idJournee(idJournee).start();
			finish();
		}
		if (requestCode == CALLBACK_ENVOIE) {
			if(resultCode == EnvoieActivity.FINISH) {
				finish();
			}
			if(resultCode == EnvoieActivity.QUITTER) {
				DemarrageActivity_.intent(this).start();
				finish();
			}
		}
	}

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
	    switch (item.getItemId()) {
		    case R.id.menu_action_application:
			    Intent ecranParametreApplication = new Intent(this, ReglagesActivity_.class);
				Log.d("AEROBUG", "idJournee : " + idJournee);
			    ecranParametreApplication.putExtra("EN_COURS", idJournee);
			    startActivityForResult(ecranParametreApplication, CALLBACK_APP);
			    return true;
		    case R.id.menu_action_journee:
			    Intent ecranParametreJournee = new Intent(this, DemarrageActivity_.class);
			    ecranParametreJournee.putExtra("EN_COURS", idJournee);
			    startActivityForResult(ecranParametreJournee, CALLBACK_JOURNEE);
			    return true;
		    case R.id.menu_action_apercu:
			    Intent ecranApercuJournee = new Intent(this, ApercuActivity_.class);
			    startActivity(ecranApercuJournee);
			    return true;
		    case R.id.menu_commentaire_journee:
			    new CommentaireGeneralDialog().show(getSupportFragmentManager(), "tag_");
			    return true;
		    default:
			    return false;
	    }
    }

	// Quand on change l'option "Pas de vol aujourd'hui" dans les réglages de la journée
	private void togglePasDeVol() {
		if(pasDeVol == 0) { // Si actuellement il y a des vols
			ecranPrincipal.setVisibility(View.INVISIBLE);
			textBottomPassagers.setVisibility(View.INVISIBLE);
			ecranPasVol.setVisibility(View.VISIBLE);
			journeeCourante.setPasDeVol(1);
			daoJournee.modifierJournee(journeeCourante);
			pasDeVol = 1;
		} else { // Si actuellement il n'y a pas de vol
			ecranPrincipal.setVisibility(View.VISIBLE);
			textBottomPassagers.setVisibility(View.VISIBLE);
			ecranPasVol.setVisibility(View.INVISIBLE);
			journeeCourante.setPasDeVol(0);
			daoJournee.modifierJournee(journeeCourante);
			pasDeVol = 0;
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.menu_journee, menu);
	    return true;
    }

    @Override
    public void onItemSelected(long id) {

		fragment = (VolDetailFragment) getSupportFragmentManager().findFragmentByTag("volFrag");

		if(fragment != null) {

			fragment.setRafraichirListe(list);

			if(fragment.idVol != 0 && !fragment.buttonAtterrissage.isEnabled()) {
				if(fragment.inputPilote.getText().toString().equals("")) {
					Toast.makeText(this, getString(R.string.vol_pilote_erreur), Toast.LENGTH_SHORT).show();
					list.setActivatedPosition(positionActuelle);
					return;
				}
				if(fragment.inputPassagers.getText().toString().equals("")) {
					Toast.makeText(this, getString(R.string.vol_passagers_erreur), Toast.LENGTH_SHORT).show();
					list.setActivatedPosition(positionActuelle);
					return;
				}
				if(fragment.inputVent.getText().toString().equals("")) {
					Toast.makeText(this, getString(R.string.vol_vent_erreur), Toast.LENGTH_SHORT).show();
					list.setActivatedPosition(positionActuelle);
					return;
				}
			}
		}

		if(fragment == null || (id == 0 && itemSelected != 0) || id != 0) {

			positionActuelle = list.positionActuelle;
			itemSelected = id;

			Bundle arguments = new Bundle();
			arguments.putLong(VolDetailFragment.ARG_ITEM_ID, id);

			fragment = new VolDetailFragment_();
			fragment.setArguments(arguments);
			fragment.setRafraichirListe(list);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.vol_detail_container, fragment, "volFrag")
					.commit();
		}
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("item", -1);
		if(fragment != null) {
			outState.putLong("item", fragment.idVol);
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedState) {
		super.onRestoreInstanceState(savedState);
		if(savedState.getLong("item") != -1) {
			onItemSelected(savedState.getLong("item"));
		}
	}
}
