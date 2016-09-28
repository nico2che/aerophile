package com.aerophile.app.modeles;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Nicolas on 17/02/2016.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Vol {
	@JsonProperty("id")
	private long id;
	private long idJournee;
	@JsonProperty("dateDecollage")
	private String dateDecollage;
	private String timeDecollage;
	@JsonProperty("dateAtterrissage")
	private String dateAtterrissage;
	private int enCours;
	private String pilote;
	private int nombrePassagers;
	private String vitesseVent;
	private String commentaires;

	public void setId(long id) {
		this.id = id;
	}
	public long getId() {
		return this.id;
	}

	public void setIdJournee(long idJournee) {
		this.idJournee = idJournee;
	}
	public long getIdJournee() {
		return this.idJournee;
	}

	public void setPilote(String pilote) {
		this.pilote = pilote;
	}
	public String getPilote() {
		return this.pilote;
	}

	public void setEnCours(int enCours) {
		this.enCours = enCours;
	}
	public int getEnCours() {
		return this.enCours;
	}

	public void setDateAtterrissage(String dateAtterrissage) {
		this.dateAtterrissage = dateAtterrissage;
	}
	public String getDateAtterrissage() {
		return this.dateAtterrissage;
	}

	public void setDateDecollage(String dateDecollage) {
		this.dateDecollage = dateDecollage;
	}
	public String getDateDecollage() {
		return this.dateDecollage;
	}

	public void setTimeDecollage(String timeDecollage) {
		this.timeDecollage = timeDecollage;
	}
	public String getTimeDecollage() {
		return this.timeDecollage;
	}

	public void setNombrePassagers(int nombrePassagers) {
		this.nombrePassagers = nombrePassagers;
	}
	public int getNombrePassagers() {
		return nombrePassagers;
	}

	public void setVitesseVent(String vitesseVent) {
		this.vitesseVent = vitesseVent;
	}
	public String getVitesseVent() {
		return vitesseVent;
	}

	public void setCommentaires(String commentaires) {
		this.commentaires = commentaires;
	}
	public String getCommentaires() {
		return commentaires;
	}

	@Override
	public String toString() {
		if(this.dateAtterrissage != null && !this.dateAtterrissage.isEmpty()) {
			return this.dateDecollage + " - " + this.dateAtterrissage;
		} else {
			return this.dateDecollage;
		}
	}
}
