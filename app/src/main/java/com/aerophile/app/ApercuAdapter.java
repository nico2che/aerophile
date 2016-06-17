package com.aerophile.app;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.aerophile.app.modeles.Vol;

import java.util.List;

public class ApercuAdapter extends ArrayAdapter<Vol> {

	Context context;
	int layoutResourceId;
	List<Vol> data = null;
	String commentaireGeneral;

	public ApercuAdapter(Context context, int layoutResourceId, List<Vol> data, String commentaire) {
		super(context, layoutResourceId, data);
		this.layoutResourceId = layoutResourceId;
		this.context = context;
		this.data = data;
		commentaireGeneral = commentaire;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(this.data.size() - 1 != position) {
			if(convertView == null) {
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				convertView = inflater.inflate(layoutResourceId, parent, false);
			}
			TextView heures = (TextView) convertView.findViewById(R.id.textHeures);
			TextView pilote = (TextView) convertView.findViewById(R.id.textPilote);
			TextView passagers = (TextView) convertView.findViewById(R.id.textPassagers);
			TextView vent = (TextView) convertView.findViewById(R.id.textVent);
			TextView commentaires = (TextView) convertView.findViewById(R.id.textCommentaires);
			Vol vol = data.get(position);
			heures.setText(String.format(getContext().getResources().getString(R.string.apercu_heures_format), vol.getDateDecollage(), (vol.getDateAtterrissage() != null && !vol.getDateAtterrissage().isEmpty() ? vol.getDateAtterrissage() : getContext().getResources().getString(R.string.apercu_vol_actuel))));
			pilote.setText(vol.getPilote());
			passagers.setText(vol.getNombrePassagers());
			vent.setText(vol.getVitesseVent() != null && !vol.getVitesseVent().isEmpty() ? String.format(getContext().getResources().getString(R.string.apercu_vent_format), vol.getVitesseVent()) : "-");
			commentaires.setText(vol.getCommentaires() != null && !vol.getCommentaires().isEmpty() ? vol.getCommentaires() : "-");
		} else {
			if(convertView == null) {
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				convertView = inflater.inflate(getContext().getResources().getLayout(R.layout.listview_commentaire), parent, false);
			}
			if(convertView != null) {
				TextView textCommentaire = (TextView) convertView.findViewById(R.id.textCommentaireGeneral);
				if(textCommentaire != null) textCommentaire.setText(this.commentaireGeneral);
			}
		}
		return convertView;
	}
}