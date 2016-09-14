package com.aerophile.app;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.aerophile.app.dao.JourneeDAO;
import com.aerophile.app.dao.VolDAO;
import com.aerophile.app.modeles.Journee;
import com.aerophile.app.modeles.Vol;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * A fragment representing a single Vol detail screen.
 * This fragment is either contained in a {@link VolListActivity}
 * in two-pane mode (on tablets) or a {@link VolDetailActivity}
 * on handsets.
 */

@EFragment(R.layout.fragment_vol_detail)
public class VolDetailFragment extends Fragment implements TimePickerDialog.OnTimeSetListener {

	public static final String ARG_ITEM_ID = "item_id";
	public static final String ARG_CHANGEMENT = "item_changement";
	public static final String ARG_DECOLLAGE_HEURE = "item_minute";
	public static final String ARG_DECOLLAGE_MINUTE = "item_heure";
	public static final String ARG_PILOTE = "item_pilote";
	public static final String ARG_PASSAGERS = "item_passagers";
	public static final String ARG_VENT = "item_vent";
	public static final String ARG_COMMENTAIRE = "item_commentaire";

    public long idVol;
	private Vol vol;
	private VolDAO daoVol;
	public boolean changeHeure;
	public int heureDecollage;
	public int minuteDecollage;
	private int heureAtterrissage;
	private int minuteAtterrissage;
	private String bouton;
	private Handler horlogeDecollage;
	private Handler horlogeAtterrissage;

	RafraichirListener rListener;

    public VolDetailFragment() {
    }

    public interface RafraichirListener {
	    long onRafraichirListe(long idVolSelected);
	    long onSupprimerVol(long idVolSelected);
	    long onNouveauVolListe(Vol vol);
    }

	public void setRafraichirListe(RafraichirListener listener) {
		rListener = listener;
	}

	@ViewById
	TextView textHeureDecollage;

	@ViewById
	TextView textHeureAtterrissage;

	@ViewById
	Button buttonDecollage;

	@ViewById
	Button buttonAtterrissage;

	@ViewById
	Button buttonHeureAtterrissage;

	@ViewById
	AutoCompleteTextView inputPilote;

	@ViewById
	EditText inputPassagers;

	@ViewById
	EditText inputVent;

	@ViewById
	EditText inputCommentaires;

	@ViewById
	Button buttonSupprimerVol;

	@ViewById
	Spinner spinnerCommentaires;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		changeHeure = false;
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            idVol = getArguments().getLong(ARG_ITEM_ID);
	        daoVol = new VolDAO(getContext());
	        daoVol.open();
        } else {
	        // Exit
	        getActivity().getFragmentManager().popBackStack();
        }
    }

	@Override
	public void onDestroyView(){
		super.onDestroyView();
		horlogeAtterrissage.removeCallbacksAndMessages(null);
		horlogeDecollage.removeCallbacksAndMessages(null);
	}

	@AfterViews
	public void initialisation() {
		inputVent.setKeyListener(DigitsKeyListener.getInstance("0123456789,."));
	    horlogeAtterrissage = new Handler();
	    horlogeDecollage = new Handler();
	    JourneeDAO journeeCourante = new JourneeDAO(getContext());
	    journeeCourante.open();
	    Journee journee = journeeCourante.getJourneeEnCours();
        if(idVol == 0) { // Nouveau vol
	        // On rend possible le décollage
	        buttonDecollage.setEnabled(true);

	        // On instancie un nouveau vol
	        vol = new Vol();

	        Vol ancienVol = daoVol.getDernierVol(journee.getId());

	        String pilote = "";
	        if(ancienVol.getPilote() != null && !ancienVol.getPilote().isEmpty()) {
		        pilote = ancienVol.getPilote();
	        } else {
		        if(journee.getPiloteValidation() != null && !journee.getPiloteValidation().isEmpty()) {
			        pilote = journee.getPiloteValidation();
		        }
	        }

			if (getArguments().containsKey(ARG_PASSAGERS)) {

				changeHeure = getArguments().getBoolean(ARG_CHANGEMENT);
				heureDecollage = getArguments().getInt(ARG_DECOLLAGE_HEURE);
				minuteDecollage = getArguments().getInt(ARG_DECOLLAGE_MINUTE);
				if(getArguments().getString(ARG_PILOTE) != null && !getArguments().getString(ARG_PILOTE).equals("")) {
				} else {
					inputPilote.setText(pilote);
				}
				inputPassagers.setText(getArguments().getString(ARG_PASSAGERS));
				inputVent.setText(getArguments().getString(ARG_VENT));
				inputCommentaires.setText(getArguments().getString(ARG_COMMENTAIRE));
				afficheHeure(textHeureDecollage, heureDecollage, minuteDecollage);

			} else {

				// On remet à zéro les valeurs
				inputPilote.setText(pilote);
				inputPassagers.setText("");
				inputVent.setText("0");
				inputCommentaires.setText("");
				changeHeure = false;
			}

	        // On cache les informations d'atterrissage
	        buttonAtterrissage.setVisibility(View.INVISIBLE);
	        textHeureAtterrissage.setVisibility(View.INVISIBLE);
	        buttonHeureAtterrissage.setVisibility(View.INVISIBLE);
	        buttonSupprimerVol.setVisibility(View.INVISIBLE);

			if(!changeHeure) {

				// On met l'heure de décollage en direct
				Runnable runnableD = new Runnable() {
					@Override
					public void run() {
						horlogeDecollage.postDelayed(this, 10000);
						miseAJourHeureDecollage();
					}
				};
				horlogeDecollage.postDelayed(runnableD, 10000);
				miseAJourHeureDecollage();
			}

	        // On met l'heure d'atterrissage en direct
	        Runnable runnableA = new Runnable() {
		        @Override
		        public void run() {
			        horlogeAtterrissage.postDelayed(this, 10000);
			        miseAJourHeureAtterrissage();
		        }
	        };
	        horlogeAtterrissage.postDelayed(runnableA, 10000);
	        miseAJourHeureAtterrissage();

	        spinnerCommentaires.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		        @Override
		        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			        if (position != 0) {
				        String[] commentairesTypes = getResources().getStringArray(R.array.vol_choix_commentaires);
				        inputCommentaires.setText(commentairesTypes[position]);
				        vol.setCommentaires(commentairesTypes[position]);
			        }
		        }

		        @Override
		        public void onNothingSelected(AdapterView<?> parent) {

		        }
	        });
			spinnerCommentaires.setSelection(0);

        } else { // Vol déjà présent

	        // On récupère le vol déjà présent dans la base de données
	        vol = daoVol.getVol(idVol);

	        // Désactive le bouton décollage
	        buttonDecollage.setEnabled(false);

	        // On révèle l'atterrissage
	        buttonAtterrissage.setVisibility(View.VISIBLE);
	        textHeureAtterrissage.setVisibility(View.VISIBLE);
	        buttonSupprimerVol.setVisibility(View.VISIBLE);

	        // En cours ? Sinon on désactive l'atterrissage
	        if(vol.getEnCours() == 0) {
		        buttonAtterrissage.setEnabled(false);
	        } else {
		        buttonAtterrissage.setEnabled(true);
	        }

	        // On initialise l'heure de décollage
	        String horaireDecollage = vol.getDateDecollage();
	        String[] heure_minute_d = horaireDecollage.split(":");
	        heureDecollage = Integer.valueOf(heure_minute_d[0]);
	        minuteDecollage = Integer.valueOf(heure_minute_d[1]);
	        textHeureDecollage.setText(horaireDecollage);

	        // On initialise l'heure d'atterrissage
	        // Si il y a déjà une heure d'atterrissage
	        if(vol.getDateAtterrissage() != null && !vol.getDateAtterrissage().isEmpty()) {
		        String horaireAtterrissage = vol.getDateAtterrissage();
		        String[] heure_minute_a = horaireAtterrissage.split(":");
		        heureAtterrissage = Integer.valueOf(heure_minute_a[0]);
		        minuteAtterrissage = Integer.valueOf(heure_minute_a[1]);
		        textHeureAtterrissage.setText(horaireAtterrissage);
		        buttonHeureAtterrissage.setVisibility(View.VISIBLE);
	        } else { // Si il n'y pas encore d'heure d'atterrissage
		        Runnable runnable = new Runnable() {
			        @Override
			        public void run() {
				        horlogeAtterrissage.postDelayed(this, 10000);
				        miseAJourHeureAtterrissage();
			        }
		        };
		        horlogeAtterrissage.postDelayed(runnable, 10000);
		        miseAJourHeureAtterrissage();
		        buttonHeureAtterrissage.setVisibility(View.INVISIBLE);
	        }

	        // Evenements qui enregistre chaque champs modifié dans la base de données interne
	        inputPilote.setText(vol.getPilote());
	        inputPilote.addTextChangedListener(new TextWatcher() {
		        public void afterTextChanged(Editable s) {
			        vol.setPilote(s.toString());
			        daoVol.modifierVol(vol, DatabaseHandler.VOL_PILOTE, s.toString());
			        Log.d("Pilotes", daoVol.getPilotes(vol.getIdJournee()).toString());
		        }

		        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		        }

		        public void onTextChanged(CharSequence s, int start, int before, int count) {
		        }
	        });
	        inputPassagers.setText(vol.getNombrePassagers());
	        inputPassagers.addTextChangedListener(new TextWatcher() {
		        public void afterTextChanged(Editable s) {
			        vol.setNombrePassagers(s.toString());
			        daoVol.modifierVol(vol, DatabaseHandler.VOL_NBRE_PASSAGER, s.toString());
			        ((VolListActivity) getActivity()).refreshBottomPassagers(idVol);
		        }

		        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		        }

		        public void onTextChanged(CharSequence s, int start, int before, int count) {
		        }
	        });
	        inputVent.setText(vol.getVitesseVent());
	        inputVent.addTextChangedListener(new TextWatcher() {
		        public void afterTextChanged(Editable s) {
			        vol.setVitesseVent(s.toString());
			        daoVol.modifierVol(vol, DatabaseHandler.VOL_VITESSE_VENT, s.toString());
		        }

		        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		        }

		        public void onTextChanged(CharSequence s, int start, int before, int count) {
		        }
	        });
	        spinnerCommentaires.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		        @Override
		        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
			        if (position != 0) {
				        String[] commentairesTypes = getResources().getStringArray(R.array.vol_choix_commentaires);
				        inputCommentaires.setText(commentairesTypes[position]);
				        vol.setCommentaires(commentairesTypes[position]);
				        daoVol.modifierVol(vol, DatabaseHandler.VOL_COMMENTAIRES, commentairesTypes[position]);
			        }
		        }

		        @Override
		        public void onNothingSelected(AdapterView<?> parent) {

		        }
	        });
	        inputCommentaires.setText(vol.getCommentaires());
	        inputCommentaires.addTextChangedListener(new TextWatcher() {
		        public void afterTextChanged(Editable s) {
			        vol.setCommentaires(s.toString());
			        daoVol.modifierVol(vol, DatabaseHandler.VOL_COMMENTAIRES, s.toString());
		        }

		        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		        }

		        public void onTextChanged(CharSequence s, int start, int before, int count) {
		        }
	        });
        }

	    ArrayList<String> pilotes = daoVol.getPilotes(journee.getId());

	    ArrayAdapter adapter = new ArrayAdapter(getContext(), android.R.layout.simple_list_item_1, pilotes);

	    inputPilote.setAdapter(adapter);
	    inputPilote.setThreshold(0);
		inputPilote.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				vol.setPilote(inputPilote.getText().toString());
				daoVol.modifierVol(vol, DatabaseHandler.VOL_PILOTE, inputPilote.getText().toString());
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
		inputPilote.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus)
					inputPilote.setText("");
			}
		});
		inputVent.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus)
					if(inputVent.getText().toString().equals("0"))
						inputVent.setText("");
			}
		});
    }

	@Override
	public void onResume() {
		super.onResume();
		if(!changeHeure && idVol == 0) {
			miseAJourHeureDecollage();
		}
	}

	void miseAJourHeureAtterrissage() {
		final Calendar c = Calendar.getInstance();
		heureAtterrissage = c.get(Calendar.HOUR_OF_DAY);
		minuteAtterrissage = c.get(Calendar.MINUTE);
		afficheHeure(textHeureAtterrissage, heureAtterrissage, minuteAtterrissage);
	}

	void miseAJourHeureDecollage() {
		final Calendar c = Calendar.getInstance();
		heureDecollage = c.get(Calendar.HOUR_OF_DAY);
		minuteDecollage = c.get(Calendar.MINUTE);
		afficheHeure(textHeureDecollage, heureDecollage, minuteDecollage);
	}

    @Click
    void buttonDecollage() {
        if (rListener != null) {
	        horlogeDecollage.removeCallbacksAndMessages(null);
	        vol.setPilote(inputPilote.getText().toString());
	        vol.setDateDecollage(textHeureDecollage.getText().toString());
	        vol.setNombrePassagers(inputPassagers.getText().toString());
	        vol.setVitesseVent(inputVent.getText().toString());
	        vol.setCommentaires(inputCommentaires.getText().toString());
	        // On enregistre le vol
	        idVol = rListener.onNouveauVolListe(vol);
	        // On relance avec le vol
	        initialisation();
        }
    }

	@Click
	void buttonAtterrissage() {
		if(vol.getPilote() != null && !vol.getPilote().isEmpty() && vol.getNombrePassagers() != null &&  !vol.getNombrePassagers().isEmpty()) {
			horlogeAtterrissage.removeCallbacksAndMessages(null);
			if(vol.getDateAtterrissage() == null) {
				final Calendar c = Calendar.getInstance();
				int heure = c.get(Calendar.HOUR_OF_DAY);
				int minute = c.get(Calendar.MINUTE);
				String nouvelleHeure = String.format(getString(R.string.vol_heure_holder), ((heure < 10) ? "0" + heure : String.valueOf(heure)), ((minute < 10) ? "0" + minute : String.valueOf(minute)));
				daoVol.modifierVol(vol, DatabaseHandler.VOL_DATE_ATTERRISSAGE, nouvelleHeure);
			} else {
				daoVol.modifierVol(vol, DatabaseHandler.VOL_DATE_ATTERRISSAGE, vol.getDateAtterrissage());
			}
			vol.setEnCours(0);
			daoVol.modifierVol(vol, DatabaseHandler.VOL_ENCOURS, "0");
			buttonAtterrissage.setEnabled(false);
			rListener.onRafraichirListe(0);
			idVol = 0;
			initialisation();
		} else {
			Toast.makeText(getContext(), getString(R.string.vol_champs_erreur), Toast.LENGTH_SHORT).show();
		}
	}

	@Click
	void buttonHeureDecollage() {
		bouton = "decollage";
		TimePickerDialog heure_decollage = new TimePickerDialog(getActivity(), this, heureDecollage, minuteDecollage, true);
		heure_decollage.show();
	}

	@Click
	void buttonHeureAtterrissage() {
		bouton = "atterrissage";
		TimePickerDialog heure_atterrissage = new TimePickerDialog(getActivity(), this, heureAtterrissage, minuteAtterrissage, true);
		heure_atterrissage.show();
	}

	public void onTimeSet(TimePicker view, int nouvelle_heure, int nouvelle_minute) {
		String nouvelleHeure;
		if(bouton.equals("decollage")) {
			// On arrête la mise à jour automatique de l'heure de décollage
			horlogeDecollage.removeCallbacksAndMessages(null);
			changeHeure = true;
			heureDecollage = nouvelle_heure;
			minuteDecollage = nouvelle_minute;
			nouvelleHeure = afficheHeure(textHeureDecollage, heureDecollage, minuteDecollage);
			vol.setDateDecollage(nouvelleHeure);
			daoVol.modifierVol(vol, DatabaseHandler.VOL_DATE_DECOLLAGE, nouvelleHeure);
			rListener.onRafraichirListe(vol.getId());
		} else {
			if(nouvelle_heure > heureDecollage || (nouvelle_heure == heureDecollage && nouvelle_minute >= minuteDecollage)) {
				heureAtterrissage = nouvelle_heure;
				minuteAtterrissage = nouvelle_minute;
				nouvelleHeure = afficheHeure(textHeureAtterrissage, heureAtterrissage, minuteAtterrissage);
				vol.setDateAtterrissage(nouvelleHeure);
				daoVol.modifierVol(vol, DatabaseHandler.VOL_DATE_ATTERRISSAGE, nouvelleHeure);
				rListener.onRafraichirListe(vol.getId());
			} else {
				Toast.makeText(getContext(), getString(R.string.vol_date_erreur), Toast.LENGTH_SHORT).show();
			}
		}
	}

	@Click
	void buttonSupprimerVol() {
		DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						idVol = rListener.onSupprimerVol(idVol);
						horlogeAtterrissage.removeCallbacksAndMessages(null);
						horlogeDecollage.removeCallbacksAndMessages(null);
						initialisation();
						break;
				}
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setMessage(getString(R.string.vol_supprimer_confirmation))
				.setPositiveButton(getString(R.string.oui), dialogClickListener)
				.setNegativeButton(getString(R.string.non), dialogClickListener).setCancelable(false).show();
	}

	public String afficheHeure(TextView widget, int heure, int minute) {
		String nouvelle_heure = "";
		if(widget != null && isAdded()) {
			nouvelle_heure = String.format(getString(R.string.vol_heure_holder), ((heure < 10) ? "0" + heure : String.valueOf(heure)), ((minute < 10) ? "0" + minute : String.valueOf(minute)));
			widget.setText(nouvelle_heure);
		} else {
			horlogeAtterrissage.removeCallbacksAndMessages(null);
			horlogeDecollage.removeCallbacksAndMessages(null);
		}
		return nouvelle_heure;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString("nbre_passagers", inputPassagers.getText().toString());
		outState.putString("vent", inputVent.getText().toString());
		outState.putString("commentaire", inputCommentaires.getText().toString());
	}

}

