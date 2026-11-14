# Registrazione delle chiamate

NoSpam permette di registrare una chiamata **a cui partecipi**, per conservarne una prova
audio da allegare alla denuncia. Questa pagina spiega come funziona, i limiti e l'uso
corretto.

## Come funziona (in breve)

- Nella console, il pulsante **Registra / Ferma** avvia e ferma la registrazione.
- La registrazione gira in un **foreground service** con **notifica persistente**: è
  sempre evidente che è in corso.
- L'audio è catturato dal **microfono**. Per registrare anche la voce dell'altro
  interlocutore devi attivare il **VIVAVOCE** del telefono.
- Al termine il file (`.m4a`) viene salvato nello storage privato dell'app e **sigillato
  con SHA-256**; compare nelle statistiche e nel fascicolo prove.

## Perché serve il vivavoce (limite tecnico di Android)

Da Android 10 le app di terze parti **non possono** accedere al flusso audio della
telefonata: l'API `AudioSource.VOICE_CALL` è riservata alle app di sistema (e ai dialer
di alcuni produttori). Non esiste un metodo di terze parti affidabile e conforme per
captare direttamente l'audio di rete. NoSpam quindi registra ciò che arriva al
**microfono**: in vivavoce questo include la voce dell'interlocutore riprodotta
dall'altoparlante.

Alternative se vuoi qualità migliore:
- Usa il **registratore di chiamata integrato** nel dialer, se il tuo telefono lo offre
  (es. app Telefono di Google in alcuni Paesi, MIUI, One UI, ecc.).
- Usa un **secondo dispositivo** per registrare in vivavoce.

## È legale?

Sì, in Italia, perché **sei parte della conversazione**. La registrazione fatta da un
interlocutore è utilizzabile come prova. Non è "intercettazione" (vietata è la captazione
di comunicazioni **tra terzi**). Non è richiesto il consenso dell'altra persona.

Cosa NON fare:
- Non registrare conversazioni **tra altre persone** di cui non fai parte: è reato.
- Non diffondere pubblicamente la registrazione: l'uso è per **difesa e denuncia**, non
  per pubblicazione. La diffusione può violare privacy e altre norme.

## Integrità della prova

- Al termine della registrazione viene calcolato lo **SHA-256** del file.
- L'hash finisce nel registro e nel fascicolo JSON: chiunque può ricalcolarlo sul file
  per verificare che non sia stato modificato dopo l'acquisizione.
- Conserva il **file originale** insieme al suo hash. Se converti o ritagli l'audio,
  l'hash cambia: allega sempre l'originale.

## Permessi richiesti

- `RECORD_AUDIO` (microfono) — chiesto al primo avvio.
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` — per registrare in modo
  affidabile con notifica visibile.
- `POST_NOTIFICATIONS` (Android 13+) — per mostrare la notifica di registrazione.

Nessun permesso dà accesso al flusso audio della telefonata: l'app non lo usa.
