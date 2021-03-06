package com.aerophile.app.dao;


/**
 * Created by Nicolas on 08/02/2016.
 */
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.aerophile.app.DatabaseHandler;
import com.aerophile.app.modeles.Journee;
import com.aerophile.app.utils.Dates;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class JourneeDAO {

	// Champs de la base de données
	private SQLiteDatabase database;
	private DatabaseHandler dbHelper;
	private String[] allColumns = { DatabaseHandler.JOURNEE_KEY,
			DatabaseHandler.JOURNEE_ENCOURS,
			DatabaseHandler.JOURNEE_DATE,
			DatabaseHandler.JOURNEE_LIFT,
			DatabaseHandler.JOURNEE_TEMPERATURE,
			DatabaseHandler.JOURNEE_PASDEVOL,
			DatabaseHandler.JOURNEE_SIGNATURE,
			DatabaseHandler.JOURNEE_COMMENTAIRE,
			DatabaseHandler.JOURNEE_COMMENTAIRE_PASDEVOL,
			DatabaseHandler.JOURNEE_PILOTE_VALIDATION,
			DatabaseHandler.JOURNEE_HEURE_OUVERTURE,
			DatabaseHandler.JOURNEE_HEURE_FERMETURE,
			DatabaseHandler.JOURNEE_ATTENTE,
			DatabaseHandler.JOURNEE_ATTENTE_OBJET,
			DatabaseHandler.JOURNEE_DATE_ENVOIE };

	public JourneeDAO(Context context) {
		dbHelper = DatabaseHandler.getInstance(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public static ContentValues preparerJournee(Journee journee) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHandler.JOURNEE_ENCOURS, journee.getEnCours());
		values.put(DatabaseHandler.JOURNEE_DATE, journee.getDate().getTime());
		values.put(DatabaseHandler.JOURNEE_LIFT, journee.getLift());
		values.put(DatabaseHandler.JOURNEE_TEMPERATURE, journee.getTemperature());
		values.put(DatabaseHandler.JOURNEE_PASDEVOL, journee.getPasdeVol());
		values.put(DatabaseHandler.JOURNEE_SIGNATURE, journee.getSignature());
		values.put(DatabaseHandler.JOURNEE_COMMENTAIRE, journee.getCommentaire());
		values.put(DatabaseHandler.JOURNEE_COMMENTAIRE_PASDEVOL, journee.getCommentairePasDeVol());
		values.put(DatabaseHandler.JOURNEE_PILOTE_VALIDATION, journee.getPiloteValidation());
		values.put(DatabaseHandler.JOURNEE_HEURE_OUVERTURE, journee.getHeureOuverture());
		values.put(DatabaseHandler.JOURNEE_HEURE_FERMETURE, journee.getHeureFermeture());
		values.put(DatabaseHandler.JOURNEE_ATTENTE, journee.getAttente());
		values.put(DatabaseHandler.JOURNEE_ATTENTE_OBJET, journee.getObjetAttente());
		values.put(DatabaseHandler.JOURNEE_DATE_ENVOIE, dateToString(journee.getDateEnvoie()));
		return values;
	}

	public Journee creerJournee(Journee journee) {
		journee.setEnCours(1);
		long insertId = database.insert(DatabaseHandler.JOURNEE_TABLE_NAME, null, preparerJournee(journee));
		Cursor cursor = database.query(DatabaseHandler.JOURNEE_TABLE_NAME,
				allColumns, DatabaseHandler.JOURNEE_KEY + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		if(!cursor.isAfterLast())
			journee = cursorToJournee(cursor);
		cursor.close();
		return journee;
	}

	public Integer getCountOfJourneeEnCours() {
		Cursor cursor = database.query(DatabaseHandler.JOURNEE_TABLE_NAME,
				allColumns, DatabaseHandler.JOURNEE_ENCOURS + " = 1", null, null, null, null);
		Integer counter = cursor.getCount();
		cursor.close();
		return counter;
	}

	public Journee getJourneeEnCours() {
		Cursor cursor = database.query(DatabaseHandler.JOURNEE_TABLE_NAME,
				allColumns, DatabaseHandler.JOURNEE_ENCOURS + " = 1", null, null, null, null);
		Journee nouvelleJournee = new Journee();
		cursor.moveToFirst();
		if(!cursor.isAfterLast())
			nouvelleJournee = cursorToJournee(cursor);
		cursor.close();
		return nouvelleJournee;
	}

	public Journee getJourneeByDate(Long date) {
		Calendar jour = Calendar.getInstance();
		jour.setTimeInMillis(date);
		jour.set(Calendar.HOUR, 0);
		jour.set(Calendar.MINUTE, 0);
		jour.set(Calendar.MILLISECOND, 0);
		Long debutDate = jour.getTimeInMillis();
		jour.add(Calendar.DATE, 1);
		Long finDate = jour.getTimeInMillis();
		Cursor cursor = database.query(DatabaseHandler.JOURNEE_TABLE_NAME, allColumns, DatabaseHandler.JOURNEE_DATE + " BETWEEN " + debutDate + " AND " + finDate, null, null, null, null);
		Journee journee = new Journee();
		cursor.moveToFirst();
		if(!cursor.isAfterLast())
			journee = cursorToJournee(cursor);
		cursor.close();
		return journee;
	}

	public Journee get(long id) {
		Cursor cursor = database.query(DatabaseHandler.JOURNEE_TABLE_NAME, allColumns, DatabaseHandler.JOURNEE_KEY + " = " + id, null, null, null, null);
		Journee journee = new Journee();
		cursor.moveToFirst();
		if(!cursor.isAfterLast())
			journee = cursorToJournee(cursor);
		cursor.close();
		return journee;
	}

	public boolean isJourneeEnCours(){
		Cursor cursor = database.query(DatabaseHandler.JOURNEE_TABLE_NAME, new String[] { DatabaseHandler.JOURNEE_KEY }, DatabaseHandler.JOURNEE_ENCOURS + " = 1", null, null, null, null);
		boolean response = (cursor != null) && (cursor.getCount() > 0);
		if(cursor != null)
			cursor.close();
		return response;
	}

	public Journee getJourneeEnAttente(){
		Cursor cursor = database.query(DatabaseHandler.JOURNEE_TABLE_NAME, allColumns, DatabaseHandler.JOURNEE_ATTENTE + " != 0", null, null, null, null);
		cursor.moveToFirst();
		Journee journee = new Journee();
		if(!cursor.isAfterLast())
			journee = cursorToJournee(cursor);
		cursor.close();
		return journee;
	}

	public boolean modifierJournee(Journee journee) {
		database.update(DatabaseHandler.JOURNEE_TABLE_NAME, preparerJournee(journee), DatabaseHandler.JOURNEE_KEY + " = ?", new String[]{String.valueOf(journee.getId())});
		return true;
	}

	public boolean aucuneJourneeEnCours() {
		ContentValues values = new ContentValues();
		values.put(DatabaseHandler.JOURNEE_ENCOURS, 0);
		database.update(DatabaseHandler.JOURNEE_TABLE_NAME, values, null, null);
		return true;
	}

	public Journee getDerniereJournee(){
		Cursor cursor = database.query(DatabaseHandler.JOURNEE_TABLE_NAME, allColumns, null, null, null, null, DatabaseHandler.JOURNEE_KEY + " DESC");
		cursor.moveToFirst();
		Journee journee = new Journee();
		if(!cursor.isAfterLast())
			journee = cursorToJournee(cursor);
		cursor.close();
		return journee;
	}

	public static String dateToString(Date date) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		if(date == null)
			return null;
		return dateFormat.format(date);
	}

	public static Journee cursorToJournee(Cursor cursor) {
		Journee journee = new Journee();
		journee.setId(cursor.getLong(0));
		journee.setEnCours(cursor.getInt(1));
		journee.setDate(Dates.stringToDate(cursor.getString(2)));
		journee.setLift(cursor.getString(3));
		journee.setTemperature(cursor.getInt(4));
		journee.setPasDeVol(cursor.getInt(5));
		journee.setSignature(cursor.getString(6));
		journee.setCommentaire(cursor.getString(7));
		journee.setCommentairePasDeVol(cursor.getString(8));
		journee.setPiloteValidation(cursor.getString(9));
		if(cursor.getColumnCount() < 11)
			return journee;
		journee.setHeureOuverture(cursor.getString(10));
		if(cursor.getColumnCount() < 12)
			return journee;
		journee.setHeureFermeture(cursor.getString(11));
		if(cursor.getColumnCount() < 13)
			return journee;
		journee.setAttente(cursor.getInt(12));
		if(cursor.getColumnCount() < 14)
			return journee;
		journee.setObjetAttente(cursor.getString(13));
		if(cursor.getColumnCount() < 15)
			return journee;
		journee.setDateEnvoie(Dates.stringToDate(cursor.getString(14)));
		return journee;
	}
}
