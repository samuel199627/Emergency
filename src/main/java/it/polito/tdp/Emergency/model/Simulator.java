package it.polito.tdp.Emergency.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import it.polito.tdp.Emergency.model.Event.EventType;
import it.polito.tdp.Emergency.model.Paziente.CodiceColore;

public class Simulator {

	// PARAMETRI DI SIMULAZIONE (alcuni costanti e altri forniti da qualche altra parte)
	// numero studi medici che possiamo anche settare dall'esterno, ma che qui sono dei valori 
	//di default
	private int NS = 5; 
	// upper bound numero di pazienti (12 ore diviso un intervallo di arrivo di 5 minuti)
	//ci serve per simulare i pazienti iniziali che arrivano ed e' per non sforare troppo 
	//anche se con l'orario tecnicamente secondo me non era necessario questo upper bound
	//in quanto una volta che andiamo oltre l'ora non abbiamo piu' eventi che vengono generati.
	private int NP = 150; 
	// intervallo tra i pazienti che rendiamo modificabile da fuori e che qui e' un default
	private Duration T_ARRIVAL = Duration.ofMinutes(5); 

	//impostiamo queste durate fisse e non modificabili (prese dalle slides)
	private final Duration DURATION_TRIAGE = Duration.ofMinutes(5);
	private final Duration DURATION_WHITE = Duration.ofMinutes(10);
	private final Duration DURATION_YELLOW = Duration.ofMinutes(15);
	private final Duration DURATION_RED = Duration.ofMinutes(30);
	private final Duration TIMEOUT_WHITE = Duration.ofMinutes(90);
	private final Duration TIMEOUT_YELLOW = Duration.ofMinutes(30);
	private final Duration TIMEOUT_RED = Duration.ofMinutes(60);
	//impostiamo l'orario di simulazione di 12 ore
	private final LocalTime oraInizio = LocalTime.of(8, 0);
	private final LocalTime oraFine = LocalTime.of(20, 0);
	//ogni quanto si controlla che uno studio e' libero oppure se sono tutti occupati
	private final Duration TICK_TIME = Duration.ofMinutes(5);

	// OUTPUT DA CALCOLARE che sono i vari pazienti che arrivano dove finiscono
	//e li vogliamo calcolare al variare del numero di studi medici nelle varie
	//simulazioni.
	private int pazientiTot;
	private int pazientiDimessi;
	private int pazientiAbbandonano;
	private int pazientiMorti;

	// STATO DEL SISTEMA (deve rappresentare i pazienti che sono nella struttura)
	//i pazienti si spostano da una parte all'altra del sistema
	private List<Paziente> pazienti;
	private PriorityQueue<Paziente> attesa ; // post-triage prima di essere chiamati
	private int studiLiberi ;

	private CodiceColore coloreAssegnato;

	// CODA DEGLI EVENTI che dobbiamo gestire
	private PriorityQueue<Event> queue;

	// INIZIALIZZAZIONE
	//azzerare le strutture dati e azzerare tutti i contatori (tranne quelli fissi e impostabili
	//dall'esterno) e generare gli eventi iniziali. Poi faccio partire la simulazione.
	public void init() {
		//potevo fare un clear era uguale
		this.queue = new PriorityQueue<>();
		
		this.pazienti = new ArrayList<>();
		this.attesa = new PriorityQueue<>();
		//azzero i contatori
		this.pazientiTot = 0;
		this.pazientiDimessi = 0;
		this.pazientiAbbandonano = 0;
		this.pazientiMorti = 0;
		
		this.studiLiberi = this.NS ;

		this.coloreAssegnato = CodiceColore.WHITE;

		// generiamo eventi iniziali (che sono quelli di arrivo al pronto soccorso con il 
		//codice in maniera ciclica)
		int nPaz = 0;
		//il primo paziente arriva all'apertura
		LocalTime oraArrivo = this.oraInizio;
		//nPaz < this.NP dice che non posso andare oltre 150 pazienti simulati (comodita')
		while (nPaz < this.NP && oraArrivo.isBefore(this.oraFine)) {
			//il codice colore e' solo dopo il triage
			Paziente p = new Paziente(oraArrivo, CodiceColore.UNKNOWN);
			this.pazienti.add(p);
			//genero l'evento di arrivo in cui dico (quando evento, quale evento, chi evento)
			Event e = new Event(oraArrivo, EventType.ARRIVAL, p);
			//aggiungiamo l'evento alla coda di eventi
			queue.add(e);

			nPaz++;
			//imposto il prossimo arrivo al pronto soccorso
			oraArrivo = oraArrivo.plus(T_ARRIVAL);
		}
		
		// Genera TICK iniziale
		queue.add(new Event(this.oraInizio, EventType.TICK, null));
	}

	public int getPazientiAbbandonano() {
		return pazientiAbbandonano;
	}

	public int getPazientiTot() {
		return pazientiTot;
	}

	public int getPazientiDimessi() {
		return pazientiDimessi;
	}

	public int getPazientiMorti() {
		return pazientiMorti;
	}

	// ESECUZIONE
	//estraggo un evento dalla coda, lo analizzo e poi succede tutto il resto quando vado a 
	//processare l'evento che e' accaduto
	public void run() {
		//vado avanti fino ad esaurimento della coda
		//quindi qui 
		while (!this.queue.isEmpty()) {
			Event e = this.queue.poll();
			//System.out.println(e + " Free studios "+this.studiLiberi);
			processEvent(e);
		}
	}

	private void processEvent(Event e) {

		Paziente paz = e.getPaziente();

		switch (e.getType()) {
		case ARRIVAL:
			// arriva un paziente: tra 5 minuti sarà finito il triage
			queue.add(new Event(e.getTime().plus(DURATION_TRIAGE), 
					EventType.TRIAGE, paz));
			this.pazientiTot++;
			break;

		case TRIAGE:
			// assegna codice colore
			paz.setColore(nuovoCodiceColore());
			
			// mette in lista d'attesa
			attesa.add(paz) ;

			// schedula timeout
			if(paz.getColore()==CodiceColore.WHITE)
				queue.add(new Event(e.getTime().plus(TIMEOUT_WHITE),
						EventType.TIMEOUT, paz));
			else if(paz.getColore()==CodiceColore.YELLOW)
				queue.add(new Event(e.getTime().plus(TIMEOUT_YELLOW),
						EventType.TIMEOUT, paz));
			else if(paz.getColore()==CodiceColore.RED)
				queue.add(new Event(e.getTime().plus(TIMEOUT_RED),
						EventType.TIMEOUT, paz));
			break;

		case FREE_STUDIO:
			if(this.studiLiberi==0) // non ci sono studi liberi
				break ;
			Paziente prossimo = attesa.poll();
			if(prossimo != null) {
				// fallo entrare
				this.studiLiberi-- ;
				
				// schedula uscita dallo studio
				if(prossimo.getColore()==CodiceColore.WHITE)
					queue.add(new Event(e.getTime().plus(DURATION_WHITE),
							EventType.TREATED, prossimo));
				else if(prossimo.getColore()==CodiceColore.YELLOW)
					queue.add(new Event(e.getTime().plus(DURATION_YELLOW),
							EventType.TREATED, prossimo));
				else if(prossimo.getColore()==CodiceColore.RED)
					queue.add(new Event(e.getTime().plus(DURATION_RED),
							EventType.TREATED, prossimo));
			}
			break;
			
		case TREATED:
			// libera lo studio
			this.studiLiberi++ ;
			paz.setColore(CodiceColore.OUT);
			
			this.pazientiDimessi++;
			this.queue.add(new Event(e.getTime(), EventType.FREE_STUDIO, null));
			break;
			
		case TIMEOUT:
			// esci dalla lista d'attesa
			boolean eraPresente = attesa.remove(paz);
			if(!eraPresente)
				break;
			
			switch(paz.getColore()) {
			case WHITE:
				//va a casa
				this.pazientiAbbandonano++;
				break;
			case YELLOW:
				// diventa RED
				paz.setColore(CodiceColore.RED);
				attesa.add(paz);
				queue.add(new Event(e.getTime().plus(DURATION_RED),
						EventType.TIMEOUT, paz));
				break;
			case RED:
				// muore
				this.pazientiMorti++;
				paz.setColore(CodiceColore.OUT);

				break;
			}
			break;
			
		case TICK:
			if(this.studiLiberi > 0) {
				this.queue.add(new Event(e.getTime(),
						EventType.FREE_STUDIO, null));
			}
			
			if(e.getTime().isBefore(LocalTime.of(23, 30)))
				this.queue.add(new Event(e.getTime().plus(this.TICK_TIME),
					EventType.TICK, null));
			break;
		}
	}

	private CodiceColore nuovoCodiceColore() {
		CodiceColore nuovo = coloreAssegnato;

		if (coloreAssegnato == CodiceColore.WHITE)
			coloreAssegnato = CodiceColore.YELLOW;
		else if (coloreAssegnato == CodiceColore.YELLOW)
			coloreAssegnato = CodiceColore.RED;
		else
			coloreAssegnato = CodiceColore.WHITE;

		return nuovo;
	}

	public int getNS() {
		return NS;
	}

	public void setNS(int nS) {
		NS = nS;
	}

	public int getNP() {
		return NP;
	}

	public void setNP(int nP) {
		NP = nP;
	}

	public Duration getT_ARRIVAL() {
		return T_ARRIVAL;
	}

	public void setT_ARRIVAL(Duration t_ARRIVAL) {
		T_ARRIVAL = t_ARRIVAL;
	}
}
