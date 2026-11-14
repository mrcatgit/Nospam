# Architettura di NoSpam

## Vista d'insieme

```
          Chiamata in arrivo
                 │
                 ▼
   ┌───────────────────────────┐
   │  NoSpamScreeningService    │  (API Android ufficiale, pre-risposta)
   │  (screening/)              │  → NON apre mai l'audio
   └──────────────┬────────────┘
                  │ rawNumber (solo metadati)
                  ▼
   ┌───────────────────────────┐
   │  SpamDetector (detection/) │
   │  1. regole esplicite       │◄── BlockRule (blocklist/allowlist, community)
   │  2. euristiche su metadati │◄── HeuristicRules + NumberNormalizer
   └──────────────┬────────────┘
                  │ Decision(verdict, score, reasons)
     ┌────────────┴───────────────┐
     ▼                            ▼
  Risposta al sistema       Registro forense
  (ALLOW/SILENCE/BLOCK)     (CallRepository → Room)
                                  │
                                  ▼
                    EvidenceHasher: catena SHA-256
                                  │
                   ┌──────────────┴───────────────┐
                   ▼                              ▼
           EvidenceExporter              ComplaintReport
           (JSON firmato)                (esposto in italiano)
                   │                              │
                   └──────────► Fascicolo per Polizia Postale / AGCOM
```

## Moduli

### `screening/` — interfaccia col sistema
`NoSpamScreeningService` estende `android.telecom.CallScreeningService`. Android lo
invoca per ogni chiamata in arrivo quando l'app detiene il ruolo
`ROLE_CALL_SCREENING`. Risponde con un `CallResponse` (permetti / silenzia / rifiuta) e
registra l'evento. Opera **prima** della risposta: non ha e non chiede accesso all'audio.

### `detection/` — motore decisionale
- `NumberNormalizer`: usa **libphonenumber** per E.164, validità, area, tipo di linea.
- `HeuristicRules`: regole indipendenti dal contenuto (numero anonimo, spoofing,
  prefissi a sovrapprezzo, wangiri, chiamate ripetute…). Ognuna dà punti + categoria.
- `SpamDetector`: applica prima le regole esplicite, poi somma le euristiche in un
  punteggio 0–100 e mappa a verdetto (≥70 blocca, ≥40 silenzia).

### `data/` — persistenza (Room)
- `CallRecord`: evento con verdetto, categoria, punteggio, motivazioni e **hash della
  catena di custodia**.
- `BlockRule`: regole con tipo (numero/prefisso/regex/anonimo), azione e priorità.
- `CallRepository`: unico accesso ai dati; inserendo un record ne calcola l'hash
  agganciandolo all'ultimo, così la catena non è falsificabile dal chiamante.

### `forensic/` — integrità e export
- `EvidenceHasher`: catena SHA-256 (ogni record incorpora l'hash del precedente) +
  `verifyChain` per rilevare manomissioni.
- `EvidenceExporter`: produce l'`EvidenceBundle` JSON con sigillo e esito integrità,
  ri-verificabile da terzi.

### `report/` — fascicolo denuncia
- `ComplaintReport`: compone l'esposto in italiano con sintesi, numeri ricorrenti,
  cronologia metadati, esito integrità e richiesta all'autorità. Non inventa dati:
  lascia tra `[ ]` ciò che deve mettere l'utente.

### `ui/` — console Compose
- `MainActivity`: richiede il ruolo di screening e monta la UI.
- `ConsoleViewModel`: espone i flussi (eventi, regole, statistiche) e le azioni di export.
- `ConsoleScreen`: dashboard con statistiche e timeline.

## Scelte di privacy by design

- Permessi minimi, nessun `RECORD_AUDIO`.
- I dati restano sul dispositivo (`allowBackup=false`); l'export è un'azione esplicita
  dell'utente.
- Analisi solo su metadati, mai sul contenuto.
