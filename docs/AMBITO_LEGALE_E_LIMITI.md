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

## Registrazione delle TUE chiamate (funzione inclusa)

In Italia puoi registrare una conversazione **a cui partecipi** e usarla come prova in
giudizio: sei uno dei soggetti della comunicazione, quindi non è "intercettazione". Non è
richiesto il consenso dell'altro interlocutore. NoSpam **include** questa funzione, con
un limite tecnico importante e trasparente:

- Da **Android 10** le app di terze parti **non possono** accedere al flusso audio della
  telefonata (`AudioSource.VOICE_CALL` è riservato alle app di sistema).
- NoSpam quindi registra **dal microfono**: con il **vivavoce** attivo può catturare
  acusticamente entrambe le voci. La riuscita e la qualità dipendono dal dispositivo e
  dall'ambiente; su alcuni telefoni, per le restrizioni Android, la registrazione può
  risultare silenziosa. Verifica sempre sul tuo dispositivo (vedi `REGISTRAZIONE.md`).
- La registrazione è **sempre avviata e fermata manualmente dall'utente**, con una
  notifica persistente visibile per tutta la durata. Nessun avvio automatico o nascosto.
- Ogni file audio è **sigillato con SHA-256** e incluso nel fascicolo prove.

Questo NON è intercettazione: non capta comunicazioni fra terzi e non accede alla rete.
Resta buona norma, dove il contesto lo richiede, avvisare l'interlocutore che la chiamata
è registrata; per l'uso come prova in sede penale/civile da parte di chi partecipa alla
conversazione, l'avviso non è un requisito di legge.

Dettagli operativi: vedi [`REGISTRAZIONE.md`](REGISTRAZIONE.md).

## In sintesi

NoSpam ti dà il massimo di ciò che è **legale, robusto e credibile**: difesa automatica +
dossier probatorio. Il resto — cioè colpire i truffatori — è un lavoro che va consegnato a
chi ha gli strumenti giuridici per farlo, altrimenti il rischio ricade su di te.
