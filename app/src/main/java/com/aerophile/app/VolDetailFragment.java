package com.aerophile.app;

import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Chronometer;
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
 * on handsets.
 */

@EFragment(R.layout.fragment_vol_detail)
public class VolDetailFragment extends Fragment implements TimePickerDialog.OnTimeSetListener {

	public static final String ARG_ITEM_ID = "item_id";

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

	private Bundle savedInstance;

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
	Chronometer chronometre;

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
			if(savedInstanceState != null) {
				idVol = savedInstanceState.getLong("id_vol");
				if(idVol == 0) {
					savedInstance = savedInstanceState;
				}
			}
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

			if (savedInstance != null) {

				changeHeure = savedInstance.getBoolean("changement");
				heureDecollage = savedInstance.getInt("heure_decollage");
				minuteDecollage = savedInstance.getInt("minute_decollage");
				if(savedInstance.getString("pilote") != null && savedInstance.getString("pilote") != "") {
					inputPilote.setText(savedInstance.getString("pilote"));
				} else {
					inputPilote.setText(pilote);
				}
				inputPassagers.setText(savedInstance.getString("nbre_passagers"));
				inputVent.setText(savedInstance.getString("vent"));
				inputCommentaires.setText(savedInstance.getString("commentaire"));
				afficheHeure(textHeureDecollage, heureDecollage, minuteDecollage);

			} else {

				// On remet à zéro les valeurs
				inputPilote.setText(pilote);
				inputPassagers.setText("0");
				inputVent.setText("0");
				inputCommentaires.setText("");
				changeHeure = false;
			}

	        // On cache les informations d'atterrissage
	        buttonAtterrissage.setVisibility(View.INVISIBLE);
			toggleChronometre(false);
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
			buttonAtterrissage.setEnabled(vol.getEnCours() != 0);
			toggleChronometre(vol.getEnCours() != 0);

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
		        }

		        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		        }

		        public void onTextChanged(CharSequence s, int start, int before, int count) {
		        }
	        });
	        inputPassagers.setText(String.valueOf(vol.getNombrePassagers()));
	        inputPassagers.addTextChangedListener(new TextWatcher() {
		        public void afterTextChanged(Editable s) {
                    int nbrePas = s.toString().equals("") ? 0 : Integer.parseInt(s.toString());
			        vol.setNombrePassagers(nbrePas);
			        daoVol.modifierVol(vol, DatabaseHandler.VOL_NBRE_PASSAGER, nbrePas);
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
        inputPassagers.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    if(inputPassagers.getText().toString().equals("0"))
                        inputPassagers.setText("");
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
	        vol.setNombrePassagers(inputPassagers.getText().toString().equals("") ? 0 : Integer.parseInt(inputPassagers.getText().toString()));
	        vol.setVitesseVent(inputVent.getText().toString());
	        vol.setCommentaires(inputCommentaires.getText().toString());
			vol.setTimeDecollage(String.valueOf(SystemClock.elapsedRealtime()));
	        // On enregistre le vol
	        idVol = rListener.onNouveauVolListe(vol);
	        // On relance avec le vol
	        initialisation();
        }
    }

	void toggleChronometre(boolean actif) {
		if(actif) {
			chronometre.setVisibility(View.VISIBLE);
			if(vol.getTimeDecollage() != null) {
				chronometre.setBase(Long.valueOf(vol.getTimeDecollage()));
				chronometre.start();
			}
		} else {
			chronometre.setVisibility(View.INVISIBLE);
			chronometre.stop();
		}
	}

	@Click
	void buttonAtterrissage() {
		if(vol.getPilote() != null && !vol.getPilote().isEmpty() && inputPassagers.getText().toString() != "") {
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
			savedInstance = null;
            getActivity().getSupportFragmentManager().popBackStack();
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
			// On a changé l'heure (pour la rotation de l'écran et ne pas remettre l'heure auto)
			changeHeure = true;
			if(vol.getTimeDecollage() != null) {
				// On met à jour le chronomètre
				Calendar ancienChrono = Calendar.getInstance();
				ancienChrono.set(Calendar.HOUR_OF_DAY, heureDecollage);
				ancienChrono.set(Calendar.MINUTE, minuteDecollage);
				Calendar nouveauChrono = Calendar.getInstance();
				nouveauChrono.set(Calendar.HOUR_OF_DAY, nouvelle_heure);
				nouveauChrono.set(Calendar.MINUTE, nouvelle_minute);
				long nouvelleBase = Long.valueOf(vol.getTimeDecollage()) - (ancienChrono.getTimeInMillis() - nouveauChrono.getTimeInMillis());
				vol.setTimeDecollage(String.valueOf(nouvelleBase));
				daoVol.modifierVol(vol, DatabaseHandler.VOL_TIME_DECOLLAGE, vol.getTimeDecollage());
				chronometre.setBase(nouvelleBase);
			}
			// On stock les nouvelles heures
			heureDecollage = nouvelle_heure;
			minuteDecollage = nouvelle_minute;
			// On affiche avec un format lisible la nouvelle heure
			nouvelleHeure = afficheHeure(textHeureDecollage, heureDecollage, minuteDecollage);
			// On enregistre la nouvelle heure
			vol.setDateDecollage(nouvelleHeure);
			daoVol.modifierVol(vol, DatabaseHandler.VOL_DATE_DECOLLAGE, nouvelleHeure);
			// On met à jour la liste des vols en précisant celui qui est actif
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
		outState.putLong("id_vol", idVol);
		outState.putBoolean("changement", changeHeure);
		outState.putInt("heure_decollage", heureDecollage);
		outState.putInt("minute_decollage", minuteDecollage);
		outState.putString("pilote", inputPilote.getText().toString());
		outState.putString("nbre_passagers", inputPassagers.getText().toString());
		outState.putString("vent", inputVent.getText().toString());
		outState.putString("commentaire", inputCommentaires.getText().toString());
	}
}

