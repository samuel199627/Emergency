package it.polito.tdp.Emergency.model;

import java.time.Duration;

import java.time.temporal.ChronoUnit;

/*
 	Secondo esercizio sulle sole simulazioni che prende spunto da quello delle slides. 
 	Abbiamo un pronto soccorso da simulare che e' organizzato in due stadi di accoglienza. Il
 	triage e' l'accoglienza e dove viene assegnato il codice di pericolo e poi si aspetta in 
 	coda in attesa che un medico chiami per ricevere.
 	
 	NS=numero di studi (numero di medici che operano in parallelo)
 	
 	Abbiamo la lista di paziente gestita in base al codice.
 	
 	Quello che simuliamo e' la seguente struttutura:
 	
 	BIANCO -> tranquillo e non peggiora se aspetta. Quando aspetta troppo se ne va a casa
 	GIALLO -> se aspetta peggiora e sale di categoria (mentre e' in coda)
 	ROSSO -> se aspetta troppo muore (mentre e' in coda)
 	NERO -> paziente muore (che non arriva al pronto soccorso)
 	
 	Abbiamo un tempo di esecuzione di triage, un tempo in sala di aspetto e il tempo di esecuzione in
 	studio.
 	
 	Il modello del mondo tratta dei pazienti e come sono distribuiti nei vari posti in cui 
 	possono essere.
 	
 	Supponiamo che nello studio medico se vengo processato non posso più morire.
 	
 	Ipotizzando la durata della simulazione, ipotizziamo un tasso di arrivo di clienti e noi
 	vogliamo cercare di capire il numero ideale di studi da tenere aperti.
 	
 	Per semplificare facciamo arrivare ciclicamente i pazienti, uno per ognuno dei 3 tipi 
 	ciclicamente (BIANCO->GIALLO->ROSSO->BIANCO->...).
 	
 	Quello che cerchiamo:
 	-numero di dimessi
 	-numero di abbandoni
 	-numero di morti
 */

public class TestSimulator {

	public static void main(String[] args) {
		Simulator sim = new Simulator() ;
		//CON 2 STUDI ABBIAMO I SEGUENTI RISULTATI
		/*
		 	** STATISTICHE **
			Studi medici: 2
			Pazienti:     150
			Dimessi:      38
			Morti:        65
			Abbandonano:  47
		 */
		sim.setNS(2);
		//CON 4 STUDI ABBIAMO I SEGUENTI RISULTATI
		/*
		 	** STATISTICHE **
			Studi medici: 4
			Pazienti:     150
			Dimessi:      75
			Morti:        31
			Abbandonano:  44
		 */
		//sim.setNS(4);
		//CON 5 STUDI ABBIAMO I SEGUENTI RISULTATI
		/*
		 	** STATISTICHE **
			Studi medici: 5
			Pazienti:     150
			Dimessi:      111
			Morti:        0
			Abbandonano:  39
		*/
		//sim.setNS(5);
		sim.setT_ARRIVAL(Duration.ofMinutes(3));
		sim.init();
		sim.run();
		
		//ECCO I PARAMETRI DI FINE SIMULAZIONE CHE POSSIAMO ANDARE AD ESTRARRE DAL SIMULATORE
		System.out.println("** STATISTICHE **") ;
		System.out.format("Studi medici: %d\n", sim.getNS());
		System.out.format("Pazienti:     %d\n", sim.getPazientiTot());
		System.out.format("Dimessi:      %d\n", sim.getPazientiDimessi());
		System.out.format("Morti:        %d\n", sim.getPazientiMorti());
		System.out.format("Abbandonano:  %d\n", sim.getPazientiAbbandonano());
	}

}
