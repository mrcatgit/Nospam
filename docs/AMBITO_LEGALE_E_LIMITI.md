# Ambito legale ed etico di NoSpam

NoSpam è uno strumento **difensivo** e di **documentazione**. Questo documento spiega
cosa l'app fa e, soprattutto, cosa **non** fa — e perché. Leggerlo ti protegge: usare
strumenti "offensivi" contro i truffatori è reato anche quando la causa è giusta, e
rischia di rendere **inutilizzabili** le prove che raccogli.

## Il principio

> Tu raccogli prove e sbarri la porta. L'identificazione e la punizione dei responsabili
> spettano all'autorità, che ha i poteri di legge per farlo (acquisizione dei tabulati
> presso i gestori, rogatorie internazionali, sequestri).

Un privato non può sostituirsi all'autorità giudiziaria. Se ci prova, commette altri
reati e si espone a denunce da parte degli stessi truffatori.

## Cosa fa NoSpam (lecito)

- **Blocca/silenzia** le chiamate con le API ufficiali Android (`CallScreeningService`,
  `RoleManager.ROLE_CALL_SCREENING`). È il meccanismo previsto dal sistema operativo.
- **Analizza i soli metadati** della chiamata in arrivo (numero, formato, prefisso,
  frequenza) *prima* della risposta. Non apre mai l'audio.
- **Registra un log a prova di manomissione** (catena di hash SHA-256) sul tuo
  dispositivo, per i tuoi dati e le tue chiamate.
- **Genera un fascicolo** (JSON firmato + esposto in italiano) che puoi allegare a una
  segnalazione o denuncia.

## Cosa NON fa NoSpam (e perché non deve farlo)

| Richiesta frequente | Perché è esclusa |
|---|---|
| **Intercettare/registrare la conversazione dell'altro** | L'intercettazione di comunicazioni altrui è vietata (art. 617 ss. c.p.). Android 10+ blocca comunque l'audio delle chiamate alle app di terze parti. La registrazione *di nascosto* di terzi come "prova" è spesso inutilizzabile e può essere essa stessa reato. |
| **"Contrattaccare" il truffatore** (flood di chiamate/SMS, accessi ai suoi sistemi, malware) | Sono reati autonomi: accesso abusivo a sistema informatico (615-ter c.p.), danneggiamento (635-bis ss.), molestie (660 c.p.). Il movente difensivo **non** è una scusante. |
| **Identificare la persona fisica dietro un numero** | Un privato non può ottenere l'intestatario di una SIM né i tabulati: lo fa solo l'autorità giudiziaria. App che promettono "chi ti ha chiamato" con nome e indirizzo o violano il GDPR o mentono. NoSpam fornisce i *metadati* necessari all'autorità per farlo legalmente. |
| **Geolocalizzare il chiamante** | Non è tecnicamente possibile da un telefono di consumo e, se lo fosse, violerebbe la privacy. |

## Nota sulla registrazione delle TUE chiamate

In Italia puoi registrare una conversazione **a cui partecipi** e usarla come prova in
giudizio (sei uno dei soggetti della comunicazione). NoSpam **non** include questa
funzione perché dalla versione di Android 10 le API di registrazione delle chiamate sono
precluse alle app di terze parti e aggirare quel blocco significherebbe tecniche fragili e
contrarie alle policy. Se vuoi una prova audio, usa l'eventuale registratore integrato
del tuo telefono o il vivavoce con un secondo dispositivo, consapevole dei limiti d'uso.

## In sintesi

NoSpam ti dà il massimo di ciò che è **legale, robusto e credibile**: difesa automatica +
dossier probatorio. Il resto — cioè colpire i truffatori — è un lavoro che va consegnato a
chi ha gli strumenti giuridici per farlo, altrimenti il rischio ricade su di te.
