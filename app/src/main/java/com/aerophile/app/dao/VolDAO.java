package com.aerophile.app.dao;

/**
 * Created by Nicolas on 22/02/2016.
 */
import java.util.ArrayList;
import java.util.List;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.aerophile.app.DatabaseHandler;
import com.aerophile.app.modeles.Vol;

public class VolDAO {

	// Champs de la base de données
	private SQLiteDatabase database;
	private DatabaseHandler dbHelper;
	private String[] allColumns = { DatabaseHandler.VOL_KEY,
			DatabaseHandler.VOL_JOURNEE,
			DatabaseHandler.VOL_ENCOURS,
			DatabaseHandler.VOL_DATE_ATTERRISSAGE,
			DatabaseHandler.VOL_DATE_DECOLLAGE,
			DatabaseHandler.VOL_PILOTE,
			DatabaseHandler.VOL_COMMENTAIRES,
			DatabaseHandler.VOL_NBRE_PASSAGER,
			DatabaseHandler.VOL_VITESSE_VENT,
			DatabaseHandler.VOL_TIME_DECOLLAGE };

	public VolDAO(Context context) {
		dbHelper = new DatabaseHandler(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public static ContentValues preparerVol(Vol vol) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHandler.VOL_JOURNEE, vol.getIdJournee());
		values.put(DatabaseHandler.VOL_ENCOURS, vol.getEnCours());
		values.put(DatabaseHandler.VOL_DATE_ATTERRISSAGE, vol.getDateAtterrissage());
		values.put(DatabaseHandler.VOL_DATE_DECOLLAGE, vol.getDateDecollage());
		values.put(DatabaseHandler.VOL_PILOTE, vol.getPilote());
		values.put(DatabaseHandler.VOL_COMMENTAIRES, vol.getCommentaires());
		values.put(DatabaseHandler.VOL_NBRE_PASSAGER, vol.getNombrePassagers());
		values.put(DatabaseHandler.VOL_VITESSE_VENT, vol.getVitesseVent());
		values.put(DatabaseHandler.VOL_TIME_DECOLLAGE, vol.getTimeDecollage());
		return values;
	}

	public long enregistrerVol(Vol nouveauVol, int enCours) {
		nouveauVol.setEnCours(enCours);
		return database.insert(DatabaseHandler.VOL_TABLE_NAME, null, preparerVol(nouveauVol));
	}

	public boolean modifierVol(Vol vol, String champ, String value) {
		ContentValues values = new ContentValues();
		values.put(champ, value);
		database.update(DatabaseHandler.VOL_TABLE_NAME, values, DatabaseHandler.VOL_KEY + " = ?", new String[]{String.valueOf(vol.getId())});
		return true;
	}

	public boolean modifierVol(Vol vol, String champ, int value) {
		ContentValues values = new ContentValues();
		values.put(champ, value);
		database.update(DatabaseHandler.VOL_TABLE_NAME, values, DatabaseHandler.VOL_KEY + " = ?", new String[]{String.valueOf(vol.getId())});
		return true;
	}

	public void supprimerVol(long idVol) {
		System.out.println("Vol deleted with id: " + idVol);
		database.delete(DatabaseHandler.VOL_TABLE_NAME, DatabaseHandler.VOL_KEY
				+ " = " + idVol, null);
	}

	public ArrayList<String> getPilotes(long id) {
		ArrayList<String> pilotes = new ArrayList<>();
		Cursor cursor = database.query(DatabaseHandler.VOL_TABLE_NAME, new String[]{ DatabaseHandler.VOL_PILOTE },  DatabaseHandler.VOL_JOURNEE + " = " + id, null, DatabaseHandler.VOL_PILOTE, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			pilotes.add(cursor.getString(0));
			cursor.moveToNext();
		}
		cursor.close();
		return pilotes;
	}

	public List<Vol> getVolsByJournee(long idJournee) {
		List<Vol> vols = new ArrayList<>();
		Cursor cursor = database.query(DatabaseHandler.VOL_TABLE_NAME,
				allColumns, DatabaseHandler.VOL_JOURNEE + " = " + idJournee, null, null, null, "CASE WHEN time(" + DatabaseHandler.VOL_DATE_DECOLLAGE + ") < time('04:00') THEN 1 ELSE 0 END DESC, " + DatabaseHandler.VOL_DATE_DECOLLAGE + " DESC");

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Vol vol = cursorToVol(cursor);
			vols.add(vol);
			cursor.moveToNext();
		}
		cursor.close();
		return vols;
	}

	public Vol getDernierVol(long id){
		Cursor cursor = database.query(DatabaseHandler.VOL_TABLE_NAME, allColumns, DatabaseHandler.VOL_JOURNEE + " = " + id, null, null, null, DatabaseHandler.VOL_KEY + " DESC");
		cursor.moveToFirst();
		Vol vol = new Vol();
		if(!cursor.isAfterLast())
			vol = cursorToVol(cursor);
		cursor.close();
		return vol;
	}

	public Vol getVol(long id){
		Cursor cursor = database.query(DatabaseHandler.VOL_TABLE_NAME, allColumns, DatabaseHandler.VOL_KEY + " = " + id, null, null, null, null);
		cursor.moveToFirst();
		Vol vol = new Vol();
		if(!cursor.isAfterLast())
			vol = cursorToVol(cursor);
		cursor.close();
		return vol;
	}

	public boolean existeVolEnCours(){
		Cursor cursor = database.query(DatabaseHandler.VOL_TABLE_NAME, new String[]{DatabaseHandler.VOL_KEY}, DatabaseHandler.VOL_ENCOURS + " = 1", null, null, null, null);
		cursor.moveToFirst();
		boolean response = !cursor.isAfterLast();
		cursor.close();
		return response;
	}

	public Vol getVolEnCours(){
		Cursor cursor = database.query(DatabaseHandler.VOL_TABLE_NAME, allColumns, DatabaseHandler.VOL_ENCOURS + " = 1", null, null, null, null);
		cursor.moveToFirst();
		Vol vol = new Vol();
		vol.setId(0);
		if(!cursor.isAfterLast())
			vol = cursorToVol(cursor);
		cursor.close();
		return vol;
	}

	public static Vol cursorToVol(Cursor cursor) {
		Vol vol = new Vol();
		vol.setId(cursor.getLong(0));
		vol.setIdJournee(cursor.getLong(1));
		vol.setEnCours(cursor.getInt(2));
		vol.setDateAtterrissage(cursor.getString(3));
		vol.setDateDecollage(cursor.getString(4));
		vol.setPilote(cursor.getString(5));
		vol.setCommentaires(cursor.getString(6));
		vol.setNombrePassagers(cursor.getInt(7));
		vol.setVitesseVent(cursor.getString(8));
		if(cursor.getColumnCount() < 10)
			return vol;
		vol.setTimeDecollage(cursor.getString(9));
		return vol;
	}
}
