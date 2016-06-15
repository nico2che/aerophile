package com.aerophile.app;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.aerophile.app.modeles.Journee;
import com.aerophile.app.modeles.Vol;

import java.util.List;

public class VolListFragment extends ListFragment implements VolDetailFragment.RafraichirListener {

    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private Callbacks mCallbacks = sDummyCallbacks;

    private ArrayAdapter<Vol> adapter;

    private JourneeDAO daoJournee;
	private VolDAO daoVol;
    private Journee journeeCourante;
    public int positionActuelle;

    private int mActivatedPosition = ListView.INVALID_POSITION;

    public interface Callbacks {
        void onItemSelected(long id);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(long id) {
        }
    };

    public VolListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    daoJournee = new JourneeDAO(getContext());
	    daoJournee.open();

	    daoVol = new VolDAO(getContext());
	    daoVol.open();

        journeeCourante = daoJournee.getJourneeEnCours();

	    if(!daoVol.existeVolEnCours()) {

		    // Nouveau vol en haut
		    Vol nouveauVol = new Vol();
		    nouveauVol.setId(0);
		    nouveauVol.setDateDecollage("Nouveau vol");
		    journeeCourante.addVol(nouveauVol);
	    }

	    // Tous les autres vols
	    List<Vol> vols = daoVol.getVolsByJournee(journeeCourante.getId());
	    for (Vol vol: vols) {
		    journeeCourante.addVol(vol);
	    }

	    adapter = new ArrayAdapter<>(
			    getActivity(),
			    android.R.layout.simple_list_item_activated_1,
			    android.R.id.text1,
                journeeCourante.getVols());

	    setListAdapter(adapter);
        //setActivatedPosition(1);
    }

	// Ajout d'un nouveau vol dans la liste
    public long onNouveauVolListe(Vol nouveauVol) {
        // On ajoute l'ID de la journée actuelle
	    nouveauVol.setIdJournee(journeeCourante.getId());
	    // On enregistre le vol et on récupère son ID
        long idVol = daoVol.enregistrerVol(nouveauVol, 1);
	    // On stock cet ID dans le vol
	    nouveauVol.setId(idVol);
	    // Et on enregistre le vol dans la journée actuelle
	    journeeCourante.addVol(nouveauVol);
	    // On rafraichit la listeview
        onRafraichirListe(idVol);
	    // Enfin, on retourne l'ID du vol
        return idVol;
    }

	// Mise à jour de la liste (changement d'heure, et d'ordre)
    public long onRafraichirListe(long idVolSelected) {
	    journeeCourante.removeVols();
	    List<Vol> vols = daoVol.getVolsByJournee(journeeCourante.getId());
	    int position = 0;
	    long idVol = 0;
	    if(!vols.isEmpty()) {
            int i = 0;
		    if(!daoVol.existeVolEnCours()) {
			    // Nouveau vol en haut
			    Vol nouveauVol = new Vol();
			    nouveauVol.setId(0);
			    nouveauVol.setDateDecollage("Nouveau vol");
			    journeeCourante.addVol(nouveauVol);
                i = 1;
		    }
		    for (Vol vol: vols) {
			    journeeCourante.addVol(vol);
			    if(vol.getId() == idVolSelected) {
				    position = i;
				    idVol = vol.getId();
			    }
			    i++;
		    }
	    } else {
		    Vol nouveauVol = new Vol();
		    nouveauVol.setId(0);
		    nouveauVol.setDateDecollage("Nouveau vol");
		    journeeCourante.addVol(nouveauVol);
	    }
	    adapter.notifyDataSetChanged();
        setActivatedPosition(position);
	    ((VolListActivity)getActivity()).refreshBottomPassagers(idVol);
	    return idVol;
    }

    public long onSupprimerVol(long idVol) {
        daoVol.supprimerVol(idVol);
        return onRafraichirListe(0);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
            Log.d("tourne", "OOOOH ! : " + savedInstanceState.getString("nbre_passagers"));
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);
        positionActuelle = position;
        mCallbacks.onItemSelected(journeeCourante.getVols().get(position).getId());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    public void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }
}
