package com.aerophile.app;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.aerophile.app.dao.JourneeDAO;
import com.aerophile.app.dao.VolDAO;
import com.aerophile.app.modeles.Journee;
import com.aerophile.app.modeles.Vol;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

	private static DatabaseHandler mInstance = null;

	private static final String DATABASE_NAME = "aerophile.db";
	private static final int DATABASE_VERSION = 20;

	private static final boolean DATABASE_ERASE_ALL = false;

	// Journée
	public static final String JOURNEE_TABLE_NAME = "journee";
	public static final String JOURNEE_KEY = "id";
	public static final String JOURNEE_DATE = "date";
	public static final String JOURNEE_LIFT = "lift";
	public static final String JOURNEE_ENCOURS = "en_cours";
	public static final String JOURNEE_TEMPERATURE = "temperature";
	public static final String JOURNEE_PASDEVOL = "pas_de_vol";
	public static final String JOURNEE_SIGNATURE = "signature";
	public static final String JOURNEE_COMMENTAIRE = "commentaire";
	public static final String JOURNEE_COMMENTAIRE_PASDEVOL = "commentaire_pasdevol";
	public static final String JOURNEE_PILOTE_VALIDATION = "pilote_validation";
	public static final String JOURNEE_HEURE_OUVERTURE = "heure_ouverture";
	public static final String JOURNEE_HEURE_FERMETURE = "heure_fermeture";
	public static final String JOURNEE_ATTENTE = "envoie_attente";
	public static final String JOURNEE_ATTENTE_OBJET = "attente_objet";
	public static final String JOURNEE_DATE_ENVOIE = "date_envoie";

	public static final String JOURNEE_TABLE_CREATE =
			"CREATE TABLE " + JOURNEE_TABLE_NAME + " (" +
					JOURNEE_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					JOURNEE_ENCOURS + " INTEGER, " +
					JOURNEE_DATE + " TEXT, " +
					JOURNEE_LIFT + " TEXT, " +
					JOURNEE_TEMPERATURE + " INTEGER, " +
					JOURNEE_PASDEVOL + " INTEGER, " +
					JOURNEE_SIGNATURE + " TEXT, " +
					JOURNEE_COMMENTAIRE + " TEXT, " +
					JOURNEE_COMMENTAIRE_PASDEVOL + " TEXT, " +
					JOURNEE_PILOTE_VALIDATION + " TEXT, " +
					JOURNEE_HEURE_OUVERTURE + " TEXT, " +
					JOURNEE_HEURE_FERMETURE + " TEXT, " +
					JOURNEE_ATTENTE + " INTEGER, " +
					JOURNEE_ATTENTE_OBJET + " TEXT, " +
					JOURNEE_DATE_ENVOIE + " DATETIME);";

	// Vol
	public static final String VOL_TABLE_NAME = "vol";
	public static final String VOL_KEY = "id";
	public static final String VOL_JOURNEE = "id_journee";
	public static final String VOL_ENCOURS = "en_cours";
	public static final String VOL_DATE_ATTERRISSAGE = "date_atterrissage";
	public static final String VOL_DATE_DECOLLAGE = "date_decollage";
	public static final String VOL_TIME_DECOLLAGE = "time_decollage";
	public static final String VOL_PILOTE = "pilote";
	public static final String VOL_NBRE_PASSAGER = "nombre_passager";
	public static final String VOL_VITESSE_VENT = "vitesse_vent";
	public static final String VOL_COMMENTAIRES = "commentaires";
	public static final String VOL_TABLE_CREATE =
			"CREATE TABLE " + VOL_TABLE_NAME + " (" +
					VOL_KEY + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
					VOL_JOURNEE + " INTEGER, " +
					VOL_ENCOURS + " INTEGER, " +
					VOL_DATE_ATTERRISSAGE + " TEXT, " +
					VOL_DATE_DECOLLAGE + " TEXT, " +
					VOL_PILOTE + " TEXT, " +
					VOL_COMMENTAIRES + " TEXT, " +
					VOL_NBRE_PASSAGER + " INTEGER, " +
					VOL_VITESSE_VENT + " INTEGER, " +
					VOL_TIME_DECOLLAGE + " TEXT);";

	private DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public static DatabaseHandler getInstance(Context ctx) {

		// Use the application context, which will ensure that you
		// don't accidentally leak an Activity's context.
		// See this article for more information: http://bit.ly/6LRzfx
		if (mInstance == null) {
			mInstance = new DatabaseHandler(ctx.getApplicationContext());
		}
		return mInstance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(JOURNEE_TABLE_CREATE);
		db.execSQL(VOL_TABLE_CREATE);
	}

	public static final String JOURNEE_TABLE_DROP = "DROP TABLE IF EXISTS " + JOURNEE_TABLE_NAME + ";";
	public static final String VOL_TABLE_DROP = "DROP TABLE IF EXISTS " + VOL_TABLE_NAME + ";";

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		if(DATABASE_ERASE_ALL) {

			db.execSQL(JOURNEE_TABLE_DROP);
			db.execSQL(VOL_TABLE_DROP);
			onCreate(db);

		} else {

			List<Journee> journees = new ArrayList<>();
			Cursor cursorJournee = db.rawQuery("SELECT * FROM " + JOURNEE_TABLE_NAME, null);
			if(cursorJournee.moveToFirst()) {
				while (!cursorJournee.isAfterLast()) {
					journees.add(JourneeDAO.cursorToJournee(cursorJournee));
					cursorJournee.moveToNext();
				}
			}
			cursorJournee.close();

			List<Vol> vols = new ArrayList<>();
			Cursor cursorVol = db.rawQuery("SELECT * FROM " + VOL_TABLE_NAME, null);
			if(cursorVol.moveToFirst()) {
				while (!cursorVol.isAfterLast()) {
					vols.add(VolDAO.cursorToVol(cursorVol));
					cursorVol.moveToNext();
				}
			}
			cursorVol.close();

			db.execSQL(JOURNEE_TABLE_DROP);
			db.execSQL(VOL_TABLE_DROP);

			onCreate(db);

			for (Journee journee : journees) {
				db.insert(JOURNEE_TABLE_NAME, null, JourneeDAO.preparerJournee(journee));
			}

			for (Vol vol : vols) {
				db.insert(VOL_TABLE_NAME, null, VolDAO.preparerVol(vol));
			}
		}
	}
}
