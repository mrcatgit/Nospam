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
**microfono**: in vivavoce questo **può** includere la voce dell'interlocutore riprodotta
dall'altoparlante.

> ⚠️ **Non è garantito su tutti i telefoni.** Durante una chiamata cellulare, le
> restrizioni Android sull'[input audio condiviso](https://developer.android.com/media/platform/sharing-audio-input)
> possono far sì che un'app ordinaria riceva **silenzio** o non riesca ad avviare la
> registrazione, anche in vivavoce. La riuscita dipende dal modello e dalla versione di
> Android: **verifica sul tuo dispositivo** facendo una prova prima di affidarti alla
> registrazione come prova, e riascolta subito il file per controllare che l'audio ci sia.

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

## Popup sopra la chiamata (opzionale)

Da **Impostazioni → "Mostra popup grande sopra la chiamata"** puoi avere, al posto della
notifica, un riquadro ampio sopra la schermata di chiamata con numero, rischio e
motivazioni, e i pulsanti grandi:
- **● Registra / ⏹ Ferma** (si alterna in base allo stato),
- **Rispondi** e **Riaggancia**,
- **Chiudi**.

Richiede il permesso **"Mostra sopra altre app"** (c'è un pulsante in Impostazioni per
concederlo). Senza il permesso si usa la notifica.

**Rispondi/Riaggancia** richiedono il permesso **telefono** (`ANSWER_PHONE_CALLS`) e
**dipendono dal dispositivo**: su alcuni modelli possono non avere effetto. Provalo sul
tuo telefono.

## Riascoltare le registrazioni

Nella console, tocca l'icona **Registrazioni** (in alto): apre l'elenco con, per ciascuna,
numero, data, durata e l'inizio dell'hash. Da lì puoi:
- **Riprodurre/fermare** l'audio in-app (play/stop);
- **Condividere** il singolo file (es. inviarlo via email alla denuncia);
- **Eliminare** la registrazione (con conferma: rimuove file e voce nel registro).

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
