# Roadmap

Stato: **scaffold funzionale**. Le fondamenta (screening, rilevamento, registro forense,
export, console) ci sono; di seguito l'evoluzione prevista.

## Prossimi passi (alta priorità)
- [ ] **Lookup rubrica** nel `SpamDetector` (permesso `READ_CONTACTS`) per l'allowlist
      automatica dei contatti noti.
- [ ] **Lettura durate dal call log di sistema** (`READ_CALL_LOG`) per alimentare
      l'euristica `avgPrevDurationSec`.
- [ ] **Schermata Regole**: creazione/modifica/eliminazione BlockRule dalla console.
- [ ] **Export reale su file**: condivisione del JSON e del PDF via `FileProvider` +
      `ACTION_SEND` (export testuale già pronto nel ViewModel).
- [ ] **PDF** del fascicolo (oltre al testo) con `PdfDocument`.

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

## Non in programma (fuori ambito per legge/etica)
- Registrazione/intercettazione di conversazioni.
- Qualsiasi azione offensiva verso i chiamanti.
- Tentativi di de-anonimizzazione dell'intestatario.

Vedi `docs/AMBITO_LEGALE_E_LIMITI.md`.
