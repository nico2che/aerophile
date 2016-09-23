package com.aerophile.app;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.aerophile.app.dao.JourneeDAO;
import com.aerophile.app.dao.VolDAO;
import com.aerophile.app.modeles.Journee;
import com.aerophile.app.modeles.Vol;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@EActivity(R.layout.activity_apercu)
public class ApercuActivity extends AppCompatActivity {

	@ViewById
	ListView listView;

	@ViewById
	TextView labelTotalVols;

	@ViewById
	TextView labelTotalHeures;

	@ViewById
	TextView labelTotalPassagers;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@AfterViews
	void initialisation() {

		// On ouvre la base de données
		JourneeDAO daoJournee = new JourneeDAO(this);
		daoJournee.open();
		Journee journeeCourante = daoJournee.getJourneeEnCours();

		VolDAO daoVol = new VolDAO(this);
		daoVol.open();
		List<Vol> vols = daoVol.getVolsByJournee(journeeCourante.getId());

		setTitle(String.format(getResources().getString(R.string.apercu_titre), journeeCourante.getDate()));

		int passagers = 0;
		int temps = 0;
		for(Vol vol : vols) {
			if(vol.getNombrePassagers() != null && !vol.getNombrePassagers().isEmpty()) {
				passagers += Integer.parseInt(vol.getNombrePassagers());
			}
			if (vol.getDateDecollage() != null && vol.getDateAtterrissage() != null) {
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.FRANCE);
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

		String heures = "0h";
		if(temps > 0) {
			int heure = temps / 3600;
			int minute = (temps % 3600) / 60;
			heures = heure + "h" + ((minute < 10) ? "0" + minute : minute);
		}

		labelTotalVols.setText(String.format(getString(R.string.apercu_footer_vols), vols.size()));
		labelTotalHeures.setText(String.format(getString(R.string.apercu_footer_heures), heures));
		labelTotalPassagers.setText(String.format(getString(R.string.apercu_footer_passagers), passagers));

		String commentaire = getResources().getString(R.string.apercu_aucun_commentaire);
		if(journeeCourante.getCommentaire() != null && !journeeCourante.getCommentaire().isEmpty()) {
			commentaire = journeeCourante.getCommentaire();
		}

		ApercuAdapter adapter = new ApercuAdapter(this, R.layout.listview_apercu, vols);
		listView.setAdapter(adapter);

		View view = getLayoutInflater().inflate(R.layout.listview_commentaire, null);
		if(view != null) {
			TextView textCommentaire = (TextView) view.findViewById(R.id.textCommentaireGeneral);
			if(textCommentaire != null) textCommentaire.setText(commentaire);
		}
		listView.addFooterView(view);
	}
}
