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
	//questi sono i tempi necessari per curare una persona nello studio in base al colore
	private final Duration DURATION_TRIAGE = Duration.ofMinutes(5);
	private final Duration DURATION_WHITE = Duration.ofMinutes(10);
	private final Duration DURATION_YELLOW = Duration.ofMinutes(15);
	private final Duration DURATION_RED = Duration.ofMinutes(30);
	//durate di attesa prima del cambio di colore (oppure l'uscita nel caso del bianco) e ce le
	//siamo inventate queste durate
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
	//i pazienti si spostano da una parte all'altra del sistema.
	//Questa lista tiene tutti i pazienti che passano nel pronto soccorso, e' il nostro catalogo.
	private List<Paziente> pazienti;
	// coda prioritaria (che non centra niente con la coda degli eventi perche' quelli vanno
	//avanti con il tempo per conto loro) post-triage  che mi permette di sapere chi e'
	//il prossimo paziente ad essere chiamato
	private PriorityQueue<Paziente> attesa ; 	
	private int studiLiberi ;

	//CodiceColore e' la variabile di tipo enum che e' presente dentro paziente, ma ce la andiamo a 
	//creare come una classe qualunque pubblica e quindi e' come se fosse una classe staccata a parte
	//che possiamo accedere da ovunque e quindi anche da qui, dal simulatore.
	private CodiceColore coloreAssegnato;
	//La stessa cosa accade per la classe enum EventType alla quale possiamo accedere da qui perche' 
	//e' stata creata come pubblica.

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

		//inizializzo il codice per il primo paziente che arriva e serve per la gestione ciclica dei 
		//colori di arrivo
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
		
		// Genera TICK iniziale che fa partire l'auto generazione delle verifiche di avere uno
		//studio libero o tutti occupati
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
		//quindi qui per come abbiamo impostato abbiamo che dopo le ore 20 i pazienti non arrivano
		//piu' al pronto soccorso, ma chi c'e' dentro viene comunque ancora processato.
		//Se per assurdo volessi che alle 20 tutta la simulazione si interrompesse, allora oltre alla
		//condizione di avere la coda vuota, dovrei avere la condizione che analizza l'ora dell'evento
		//e che analizza che sia piu' piccola delle 20, altrimenti non va ad estrarre l'elemento e 
		//ad analizzarlo interrompendo di fatto la simulazione.
		while (!this.queue.isEmpty()) {
			//con 'poll' ricordiamo che estraggo, cioe' proprio tolgo l'elemento dalla coda e lo
			//posso andare ad analizzare (svuotando appunto man mano la coda)
			Event e = this.queue.poll();
			//System.out.println(e + " Free studios "+this.studiLiberi);
			processEvent(e);
		}
	}

	//quasi sempre e' uno switch a seconda del tipo di evento
	private void processEvent(Event e) {

		Paziente paz = e.getPaziente();

		switch (e.getType()) {
		case ARRIVAL:
			// arriva un paziente: lo mando in triage e tra 5 minuti sarà finito il triage
			//l'evento non si modifica mai, se ne creano di nuovi una volta estratti. Quindi
			//qui creo un nuovo evento con un nuovo tempo.
			queue.add(new Event(e.getTime().plus(DURATION_TRIAGE), 
					EventType.TRIAGE, paz));
			//e' arrivato un nuovo paziente e quindi devo aggiornare 
			this.pazientiTot++;
			break;

		case TRIAGE:
			// assegna codice colore al paziente e lo mette in attesa
			paz.setColore(nuovoCodiceColore());
			
			// mette in lista d'attesa (quella nella sala di attesa dopo il triage)
			//che e' una coda prioritaria in base al colore (e in caso di uguaglianza il tempo)
			attesa.add(paz) ;

			// schedula timeout in cui creo un nuovo evento in base al timeout del colore che ho
			//perche' se non si verifica nulla e il paziente resta in sala di attesa dopo il timeout 
			//qualcosa succede e comunque io ho sempre il riferimento al paziente che e' lo stesso per
			//la coda prioritaria e per l'evento e quindi riesco sempre a tracciare tutto.
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
			
			/*
			 	Senza l'ausilio del tipo di evento periodico TICK questo evento non verrebbe mai chiamato
			 	la prima volta perche' verrebbe solo chiamato dall'uscita dallo studio (che non potrebbe mai
			 	avvenire in quanto non verrebbe mai detto che c'e' qualche studio libero).
			 	Quindi l'idea e' appunto di avere un evento che ogni 5 minuti viene chiamato (quindi lo 
			 	inizializzo subito e poi si autoalimenta) e che va a controllare se c'e' qualche studio 
			 	libero e se si' allora scateno istantaneamente un evento di tipo FREE_STUDIO che mi va a 
			 	prendere il primo paziente in coda.
			 	Senza il TICK in piu' una volta che avevo uno studio libero, ma nessun paziente in lista di
			 	attesa, non saremmo piu' andati avanti perche' non ci sarebbe stata nessuna verifica dopo un
			 	certo tempo per verificare che qualche paziente era presente.
			 */
			
			
			//evento generato quando si libera uno studio di un medico
			//e passa avanti il prossimo paziente in lista di attesa
			if(this.studiLiberi==0) // non ci sono studi liberi
				break ;
			
			//estraggo il paziente che ha priorita' di passare dalla lista di attesa
			Paziente prossimo = attesa.poll();
			if(prossimo != null) {
				// fallo entrare
				this.studiLiberi-- ;
				
				// schedula uscita dallo studio (con la durata del trattamento in base al 
				//colore del paziente)
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
			// un paziente e' stato trattato, libera lo studio 
			this.studiLiberi++ ;
			paz.setColore(CodiceColore.OUT);
			
			this.pazientiDimessi++;
			//aggiungo l'evento che libera lo studio, che avviene subito e che non e' associato a 
			//nessun paziente in quanto quando ho l'evento free studio il paziente me lo vado a 
			//prendere io dalla lista di attesa.
			this.queue.add(new Event(e.getTime(), EventType.FREE_STUDIO, null));
			break;
			
		case TIMEOUT:
			//alla fine del timeout, cioe' quando accade un evento di tipo timeout succedono cose
			//diverse in base al tipo di colore del paziente associato all'evento
			
			// esci dalla lista d'attesa quindi lo tolgo dalla lista di attesa in quanto se e' un 
			//bianco se ne va proprio a casa, se e' un rosso muore e quindi in nessuno dei due casi 
			//rientra nella lista di attesa, mentre da giallo diventa rosso e lo rimetto nella lista
			//di attesa con il nuovo codice colore che cambia la sua priorita'. Quindi serve in 
			//qualunque caso toglierlo e rimetterlo nella coda prioritaria e mai modificare solamente
			//perche' con la modifica la priorita' non cambia in quanto e' valutata all'inserimento.
			//Nella lista dei pazienti lui c'e' sempre, lo tolgo solo dalla lista di attesa con
			//il remove.
			//Prima che scada il timeout il paziente poteva essere passato in uno studio e quindi 
			//poteva essere gia' stato tolto dalla lista di attesa e in quel caso quindi mi salvo questa
			//cosa tenendo il ritorno dell'operazione remove che mi dice se e' andato tutto a buon fine.
			//Se ritorna true allora significa che il paziente non era stato chiamato da nessun medico e
			//ha aspettato in coda fino alla fine del timeout. Se ritorna false il paziente era gia' 
			//stato curato e quindi non deve piu' essere riconsiderato.
			boolean eraPresente = attesa.remove(paz);
			if(!eraPresente)
				break;
			
			switch(paz.getColore()) {
			case WHITE:
				//va a casa e non lo rimetto nella lista di attesa
				this.pazientiAbbandonano++;
				break;
			case YELLOW:
				// diventa RED, lo rimetto in lista di attesa e scateno un altro evento di tipo timeout
				//che riparte al cambio di colore 
				paz.setColore(CodiceColore.RED);
				attesa.add(paz);
				queue.add(new Event(e.getTime().plus(DURATION_RED),
						EventType.TIMEOUT, paz));
				break;
			case RED:
				// muore e non lo rimetto nella lista di attesa
				this.pazientiMorti++;
				paz.setColore(CodiceColore.OUT);

				break;
			}
			break;
			
		case TICK:
			//e' un evento che si auto rigenera da solo
			
			//se c'e' uno studio libero, genero un evento ora che dice che c'e' uno studio libero
			if(this.studiLiberi > 0) {
				this.queue.add(new Event(e.getTime(),
						EventType.FREE_STUDIO, null));
			}
			
			//mi genero il prossimo evento periodico per controllare se ci sono studi liberi
			if(e.getTime().isBefore(LocalTime.of(23, 30)))
				this.queue.add(new Event(e.getTime().plus(this.TICK_TIME),
					EventType.TICK, null));
			break;
		}
	}

	//metodo che assegna il colore in maniera ciclica tra i 3 possibili codice colori che possono
	//verificarsi
	private CodiceColore nuovoCodiceColore() {
		//mi salvo l'ultimo colore che avevo assegnato
		//e inizialmente lo metto a bianco nell'inizializzazione
		CodiceColore nuovo = coloreAssegnato;

		//qui imposto il colore per il prossimo giro
		if (coloreAssegnato == CodiceColore.WHITE)
			coloreAssegnato = CodiceColore.YELLOW;
		else if (coloreAssegnato == CodiceColore.YELLOW)
			coloreAssegnato = CodiceColore.RED;
		else
			coloreAssegnato = CodiceColore.WHITE;

		//ritorno il colore che avevo prima della modifica
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
