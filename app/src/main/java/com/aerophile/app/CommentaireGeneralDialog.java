package com.aerophile.app;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

/**
 * Created by Nicolas on 12/05/2016.
 */
public class CommentaireGeneralDialog extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final JourneeDAO daoJournee = new JourneeDAO(getActivity());
		daoJournee.open();
		final Journee journeeCourante = daoJournee.getJourneeEnCours();
		LayoutInflater li = LayoutInflater.from(getActivity());
		View dialogView = li.inflate(R.layout.dialog_commentaire, null);
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
		alertDialogBuilder.setView(dialogView);
		final EditText inputCommentaireJournee = (EditText) dialogView.findViewById(R.id.inputCommentaireJournee);
		inputCommentaireJournee.setText(journeeCourante.getCommentaire());
		alertDialogBuilder
				.setCancelable(false)
				.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								journeeCourante.setCommentaire(inputCommentaireJournee.getText().toString());
								daoJournee.modifierJournee(journeeCourante);
							}
						});
		return alertDialogBuilder.create();
	}
}
