package com.aerophile.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.aerophile.app.modeles.Vol;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VolListActivity extends AppCompatActivity
        implements VolListFragment.Callbacks {

    private boolean mTwoPane;

	public VolListFragment list;

	View ecranPrincipal;
	View ecranPasVol;

	private VolDAO daoVol;
	private Journee journeeCourante;
	private TextView textPassagers;
	private JourneeDAO daoJournee;
	private int pasDeVol = 0;

	private boolean saving;
	private boolean changeHeure;
	private int heureDecollage;
	private int minuteDecollage;
	private String pilote;
	private String nombrePassagers;
	private String vent;
	private String commentaire;
	public int positionActuelle;

	private VolDetailFragment fragment;

	private long itemSelected;

	public final static int CALLBACK_JOURNEE = 0;
	public final static int CALLBACK_APP = 1;
	public final static int CALLBACK_ENVOIE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vol_list);

        if (findViewById(R.id.vol_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
			saving = false;

	        daoVol = new VolDAO(this);
	        daoVol.open();

	        daoJournee = new JourneeDAO(this);
	        daoJournee.open();

	        journeeCourante = daoJournee.getJourneeEnCours();
	        setTitle(journeeCourante.getDate());


	        ecranPrincipal = findViewById(R.id.ecranPrincipal);
	        ecranPasVol = findViewById(R.id.ecranPasVol);
	        textPassagers = (TextView) findViewById(R.id.textBottomPassagers);
	        Button finDeJournee = (Button)findViewById(R.id.buttonFinDeJournee);

	        if(finDeJournee != null) {
		        finDeJournee.setOnClickListener(new View.OnClickListener() {
			        @Override
			        public void onClick(View v) {
				        if(daoVol.getDernierVol(journeeCourante.getId()).getEnCours() == 1) {
					        Toast.makeText(getApplicationContext(), getString(R.string.vol_vols_erreur), Toast.LENGTH_SHORT).show();
				        } else {
					        Intent ecranEnvoie = new Intent(VolListActivity.this, EnvoieActivity_.class);
					        startActivityForResult(ecranEnvoie, CALLBACK_ENVOIE);
				        }
			        }
		        });
	        }

	        SharedPreferences reglages = PreferenceManager.getDefaultSharedPreferences(this);
	        TextView textImmatriculation = (TextView) findViewById(R.id.textBottomImmat);
	        if(textImmatriculation != null) {
		        textImmatriculation.setText(String.format(getString(R.string.vol_immatriculation_ballon), reglages.getString("IMMATRICULATION", "?")));
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
    }

	public void refreshBottomPassagers(long position) {
		List<Vol> volsJournee = daoVol.getVolsByJournee(journeeCourante.getId());
		int nombrePassagers = 0;
		int temps = 0;
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.FRANCE);
		if(volsJournee.size() > 0) {
			for (Vol vol : volsJournee) {
				if(vol.getNombrePassagers() != null && !vol.getNombrePassagers().isEmpty()) {
					nombrePassagers += Integer.parseInt(vol.getNombrePassagers());
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
		textPassagers.setText(String.format(getString(R.string.vol_stats), volsJournee.size(), nombrePassagers, heures));
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
				setTitle("Aérophile - " + journeeCourante.getDate());
			}
		}
		if (requestCode == CALLBACK_APP) {
			Intent restartVols = new Intent();
			restartVols.setClass(this, this.getClass());
			startActivity(restartVols);
			finish();
		}
		if (requestCode == CALLBACK_ENVOIE) {
			if(resultCode == EnvoieActivity.FINISH) {
				finish();
			}
			if(resultCode == EnvoieActivity.QUITTER) {
				Intent ecranDemarrageJournee = new Intent(this, DemarrageActivity_.class);
				startActivity(ecranDemarrageJournee);
				finish();
			}
		}
	}

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
	    switch (item.getItemId()) {
		    case R.id.menu_action_application:
			    Intent ecranParametreApplication = new Intent(this, ReglagesActivity_.class);
			    ecranParametreApplication.putExtra("EN_COURS", 1);
			    startActivityForResult(ecranParametreApplication, CALLBACK_APP);
			    return true;
		    case R.id.menu_action_journee:
			    Intent ecranParametreJournee = new Intent(this, DemarrageActivity_.class);
			    ecranParametreJournee.putExtra("EN_COURS", 1);
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

	private void togglePasDeVol() {
		if(pasDeVol == 0) { // Si actuellement il y a des vols
			ecranPrincipal.setVisibility(View.INVISIBLE);
			textPassagers.setVisibility(View.INVISIBLE);
			ecranPasVol.setVisibility(View.VISIBLE);
			journeeCourante.setPasDeVol(1);
			daoJournee.modifierJournee(journeeCourante);
			pasDeVol = 1;
		} else { // Si actuellement il n'y a pas de vol
			ecranPrincipal.setVisibility(View.VISIBLE);
			textPassagers.setVisibility(View.VISIBLE);
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
		if(fragment != null) {
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
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
			positionActuelle = list.positionActuelle;
	        itemSelected = id;
            Bundle arguments = new Bundle();
            arguments.putLong(VolDetailFragment.ARG_ITEM_ID, id);
			if(saving) {
				arguments.putBoolean(VolDetailFragment.ARG_CHANGEMENT, changeHeure);
				arguments.putInt(VolDetailFragment.ARG_DECOLLAGE_HEURE, heureDecollage);
				arguments.putInt(VolDetailFragment.ARG_DECOLLAGE_MINUTE, minuteDecollage);
				arguments.putString(VolDetailFragment.ARG_PILOTE, pilote);
				arguments.putString(VolDetailFragment.ARG_PASSAGERS, nombrePassagers);
				arguments.putString(VolDetailFragment.ARG_VENT, vent);
				arguments.putString(VolDetailFragment.ARG_COMMENTAIRE, commentaire);
				saving = false;
			}
            fragment = new VolDetailFragment_();
            fragment.setArguments(arguments);
            fragment.setRafraichirListe(list);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.vol_detail_container, fragment)
                    .commit();

        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            Intent detailIntent = new Intent(this, VolDetailActivity.class);
            detailIntent.putExtra(VolDetailFragment.ARG_ITEM_ID, id);
            startActivity(detailIntent);
        }
    }

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putLong("item", itemSelected);
		if(fragment != null) {
			outState.putBoolean("changement", fragment.changeHeure);
			outState.putInt("heure_decollage", fragment.heureDecollage);
			outState.putInt("minute_decollage", fragment.minuteDecollage);
			outState.putString("pilote", fragment.inputPilote.getText().toString());
			outState.putString("nbre_passagers", fragment.inputPassagers.getText().toString());
			outState.putString("vent", fragment.inputVent.getText().toString());
			outState.putString("commentaire", fragment.inputCommentaires.getText().toString());
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedState) {
		super.onRestoreInstanceState(savedState);
		changeHeure = savedState.getBoolean("changement");
		heureDecollage = savedState.getInt("heure_decollage");
		minuteDecollage = savedState.getInt("minute_decollage");
		pilote = savedState.getString("pilote");
		nombrePassagers = savedState.getString("nbre_passagers");
		vent = savedState.getString("vent");
		commentaire = savedState.getString("commentaire");
		saving = true;
		onItemSelected(savedState.getLong("item"));
	}
}
