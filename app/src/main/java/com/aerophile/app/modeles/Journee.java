package com.aerophile.app.modeles;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Nicolas on 11/01/2016.
 * */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Journee implements Serializable {

    private long id;
    private int enCours;
    private Date date;
    private String lift;
    private int temperature;
    private int pasDeVol;
    private String signature;
    private String piloteValidation;
    private String commentaire;
    private String commentairePasDeVol;
    private String heureOuverture;
    private String heureFermeture;
    private int attente;
    private String objetAttente;
    private Date dateEnvoie;
    @JsonProperty("vols")
    public List<Vol> VOLS;

    @JsonCreator
    public Journee() {
        this.VOLS = new ArrayList<>();
    }


    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public int getEnCours() {
        return enCours;
    }
    public void setEnCours(int id) {
        this.enCours = id;
    }

    public void setDate(Date date) {
        this.date = date;
    }
    public Date getDate() {
        return this.date;
    }

    public void setLift(String lift) {
        this.lift = lift;
    }
    public String getLift() {
        return this.lift;
    }

    public void setTemperature(int temperature) {
        this.temperature = temperature;
    }
    public int getTemperature() {
        return this.temperature;
    }

    public void setPasDeVol(int pasDeVol) {
        this.pasDeVol = pasDeVol;
    }
    public int getPasdeVol() {
        return this.pasDeVol;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
    public String getSignature() {
        return this.signature;
    }

    public void setPiloteValidation(String piloteValidation) {
        this.piloteValidation = piloteValidation;
    }
    public String getPiloteValidation() {
        return this.piloteValidation;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }
    public String getCommentaire() {
        return this.commentaire;
    }

    public void setCommentairePasDeVol(String commentairePasDeVol) {
        this.commentairePasDeVol = commentairePasDeVol;
    }
    public String getCommentairePasDeVol() {
        return this.commentairePasDeVol;
    }

    public void setHeureOuverture(String heureOuverture) {
        this.heureOuverture = heureOuverture;
    }
    public String getHeureOuverture() {
        return this.heureOuverture;
    }

    public void setHeureFermeture(String heureFermeture) {
        this.heureFermeture = heureFermeture;
    }
    public String getHeureFermeture() {
        return this.heureFermeture;
    }

    public void setAttente(int attente) {
        this.attente = attente;
    }
    public int getAttente() {
        return this.attente;
    }

    public void setObjetAttente(String objetAttente) {
        this.objetAttente = objetAttente;
    }
    public String getObjetAttente() {
        return this.objetAttente;
    }

    public void setDateEnvoie(Date dateEnvoie) {
        this.dateEnvoie = dateEnvoie;
    }
    public Date getDateEnvoie() {
        return this.dateEnvoie;
    }

    public void addVol(Vol vol) {
	    this.VOLS.add(vol);
    }
    public List<Vol> getVols(){
        return this.VOLS;
    }
    public void removeVols(){
        this.VOLS.clear();
    }
}
