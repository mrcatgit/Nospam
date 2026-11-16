# Roadmap

Stato: **scaffold funzionale**. Le fondamenta (screening, rilevamento, registro forense,
export, console) ci sono; di seguito l'evoluzione prevista.

## Prossimi passi (alta priorità)
- [ ] **Lookup rubrica** nel `SpamDetector` (permesso `READ_CONTACTS`) per l'allowlist
      automatica dei contatti noti.
- [ ] **Lettura durate dal call log di sistema** (`READ_CALL_LOG`) per alimentare
      l'euristica `avgPrevDurationSec`.
- [x] **Schermata Regole**: creazione/eliminazione BlockRule dalla console.
- [x] **Export e condivisione reale**: esposto (.txt) + evidence bundle (.json) + file
      audio condivisi via `FileProvider` + `ACTION_SEND_MULTIPLE` (pulsante in console).
- [x] **PDF** del fascicolo (oltre al testo) con `PdfDocument`, incluso nella condivisione.

## Community & liste
- [ ] Import/aggiornamento blocklist community in formato standard (CSV/JSON) con firma.
- [ ] Contributo anonimo e volontario di segnalazioni (opt-in, pseudonimizzato).

## Qualità del rilevamento
- [ ] Punteggi calibrati su dataset reali di numerazioni IT.
- [ ] Rilevamento spoofing più fine (incoerenze piano di numerazione, neighbor spoofing).
- [ ] Classificatore on-device dei pattern di frequenza (nessun dato in cloud).

## Robustezza prove
- [ ] Marca temporale esterna opzionale (timestamp RFC 3161) per il sigillo del bundle.
- [ ] Esportazione firmata con chiave del dispositivo (Android Keystore).

## Registrazione (implementata, da rifinire)
- [x] Registrazione dal microfono (vivavoce) con foreground service e notifica.
- [x] Sigillo SHA-256 del file e inclusione nel fascicolo prove.
- [x] Proposta automatica di registrazione per chiamate sospette non bloccate
      (notifica con azione dallo screening; avvio sempre su tocco dell'utente).
- [x] Condivisione dei file audio nel fascicolo (via FileProvider).
- [x] Riproduzione in-app dei file (schermata Registrazioni: play/stop, condivisione
      del singolo file, eliminazione con conferma).
- [ ] Avvio registrazione con numero già collegato all'evento in corso del registro.

## Non in programma (fuori ambito per legge/etica)
- Intercettazione di comunicazioni **tra terzi** / accesso al flusso audio di rete.
- Qualsiasi azione offensiva verso i chiamanti.
- Tentativi di de-anonimizzazione dell'intestatario.

Vedi `docs/AMBITO_LEGALE_E_LIMITI.md`.
