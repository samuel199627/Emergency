package it.polito.tdp.Emergency.model;

import java.time.LocalTime;

/**
 * Rappresenta le informazioni su ciascun paziente nel sistema.
 * faccio implementare Comparable per inserire i pazienti in una lista prioritaria della sala di attesa
 * che non centra nulla con la coda degli eventi e serve solo per permettere di avere un ordine tra i 
 * pazienti che sono in sala di attesa.
 * @author Fulvio
 *
 */
public class Paziente implements Comparable<Paziente>{
	
	//essendo pubblica e' come se fosse una classe normale staccata e quindi ci posso anche accedere
	//dal simulatore creando variabili di quel tipo
	public enum CodiceColore {
		UNKNOWN, // non lo so ancora perché il paziente non ancora finito il triage
		WHITE,
		YELLOW,
		RED,
		BLACK,
		OUT,
	}
	
	//ci serve per determinare la posizione a parita' di codice colore nella sala di attesa
	private LocalTime oraArrivo ;
	private CodiceColore colore ;
	/**
	 * @param oraArrivo
	 * @param colore
	 */
	public Paziente(LocalTime oraArrivo, CodiceColore colore) {
		super();
		this.oraArrivo = oraArrivo;
		this.colore = colore;
	}
	public LocalTime getOraArrivo() {
		return oraArrivo;
	}

	public CodiceColore getColore() {
		return colore;
	}
	public void setColore(CodiceColore colore) {
		this.colore = colore;
	}
	
	//mi serve per avere l'ordinamento nella sala di attesa, la cui priorita' e' per 
	//codice colore e a parita' e' per ora di arrivo
	@Override
	public int compareTo(Paziente other) {
		//se hanno codice colore uguale passa per primo chi arriva per primo
		if(this.colore==other.colore) {
			//delego al comparatore di tempo che mette prima chi arriva prima
			return this.oraArrivo.compareTo(other.oraArrivo); 
		} else if(this.colore==CodiceColore.RED) {
			//il numero negativo mi dice che il primo è minore del secondo e quindi deve venire prima
			return -1 ;
		} else if(other.colore==CodiceColore.RED) {
			return +1 ;
		} else if(this.colore==CodiceColore.YELLOW) {
			return -1 ;
		} else if(other.colore==CodiceColore.YELLOW) {
			return +1 ;
		}
		
		//non dovremo mai arrivare qui perche' dovremmo capitare in uno dei casi precedenti
		//e quindi invece di mettere un return fasullo mettiamo un'eccezione per accorgerci se
		//qualcosa non va
		throw new RuntimeException("Comparator<Persona> failed") ;
	}
	
	@Override
	public String toString() {
		return "Paziente [" + oraArrivo + ", " + colore + "]";
	}
	
	
	
}
