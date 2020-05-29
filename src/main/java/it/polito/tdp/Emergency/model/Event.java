package it.polito.tdp.Emergency.model;

import java.time.LocalTime;

public class Event implements Comparable<Event>{
	
	//ecco i vari tipi di eventi che possono accadere (uno per ogni tipo di freccia 
	public enum EventType {
		ARRIVAL,  // arriva un nuovo paziente
		TRIAGE,  // è stato assegnato codice colore e vado in sala d'attesa
		FREE_STUDIO, // si libera uno studio e chiamo un paziente
		TREATED, // paziente trattato e dimesso
		TIMEOUT, // attesa eccessiva in sala d'aspetto (e poi in base al paziente valutiamo che
		//cosa succede)
		TICK, // evento periodico per verificare se ci sono studi vuoti
	}
	
	//lavoriamo in una giornata quindi serve solo l'ora come caratteristica temporale
	//questi primi due parametri sono quelli di default per tutti gli eventi
	private LocalTime time ;
	private EventType type ;
	//informazione aggiuntiva sull'evento che dipende a quale paziente accade in sostanza
	//e ci permette di estrarre il codice colore del paziente ad esempio
	private Paziente paziente ;
	

	/**
	 * @param time
	 * @param type
	 */
	public Event(LocalTime time, EventType type, Paziente paziente) {
		super();
		this.time = time;
		this.type = type;
		this.paziente = paziente;
	}
	
	public LocalTime getTime() {
		return time;
	}

	public EventType getType() {
		return type;
	}

	//deleghiamo il compareTo tutto al tempo che e' quello che sistema in coda prioritaria
	@Override
	public int compareTo(Event other) {
		return this.time.compareTo(other.time);
	}

	public Paziente getPaziente() {
		return paziente;
	}

	
	@Override
	public String toString() {
		return "Event ["+time + ", " + type + ", " + paziente + "]";
	}

}
