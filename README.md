# NoSpam — Console Antispam & Raccolta Prove

**NoSpam** è un'app Android (Kotlin + Jetpack Compose) pensata per chi è esasperato da
chiamate spam e tentativi di truffa telefonica. Blocca e filtra le chiamate indesiderate
con le API ufficiali di Android, tiene un registro **a prova di manomissione** degli
eventi e genera un **fascicolo di segnalazione/denuncia** pronto da consegnare alla
**Polizia Postale** e all'**AGCOM**.

> ⚖️ **Filosofia del progetto.** NoSpam è uno strumento **difensivo e di documentazione**.
> Serve a *sbarrare la porta* e a *mettere insieme le prove* in modo che un'autorità con
> poteri di legge possa risalire ai responsabili. Non intercetta conversazioni, non
> "attacca" i truffatori e non tenta di de-anonimizzarli: quelle attività sono reato in
> Italia e, se fatte da un privato, distruggono anche il valore delle prove raccolte.
> Vedi [`docs/AMBITO_LEGALE_E_LIMITI.md`](docs/AMBITO_LEGALE_E_LIMITI.md).

---

## Cosa fa

| Modulo | Funzione |
|---|---|
| **Screening chiamate** | `CallScreeningService` di sistema: blocca, silenzia o lascia passare in base alle regole. |
| **Motore di rilevamento** | Euristiche su numero/prefisso/pattern + blocklist locale e liste community importabili. |
| **Registro forense** | Ogni evento è firmato con hash concatenato (catena tipo blockchain leggera) → manomissioni rilevabili. |
| **Console di comando** | Dashboard Compose: statistiche, timeline, gestione regole, esportazioni. |
| **Generatore denuncia** | Produce un fascicolo strutturato (PDF/JSON) con i dati richiesti da Polizia Postale / AGCOM. |

## Cosa NON fa (per scelta e per legge)

- ❌ Non registra né intercetta il **contenuto** delle chiamate (vietato da Android 10+ e, verso terzi inconsapevoli, dall'art. 617 c.p.).
- ❌ Non effettua "contrattacchi", flooding, o accessi a sistemi dei truffatori.
- ❌ Non pretende di identificare la persona fisica dietro un numero: fornisce **metadati e prove**, l'identificazione legale spetta all'autorità.

## Struttura del progetto

```
Nospam/
├── app/                      # Modulo applicazione Android
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/nospam/
│       │   ├── NoSpamApp.kt
│       │   ├── data/         # Room: entità, DAO, repository
│       │   ├── detection/    # Motore euristico di rilevamento spam
│       │   ├── screening/    # CallScreeningService di sistema
│       │   ├── forensic/     # Hashing, catena di custodia, export prove
│       │   ├── report/       # Generatore fascicolo denuncia
│       │   └── ui/           # Console Compose
│       └── res/
├── docs/
│   ├── ARCHITETTURA.md
│   ├── GUIDA_SEGNALAZIONE.md       # Come e dove denunciare in Italia
│   ├── AMBITO_LEGALE_E_LIMITI.md   # Confini legali ed etici
│   └── ROADMAP.md
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Requisiti

- Android Studio (Ladybug o successivo)
- Android SDK 34 (minSdk 29 — Android 10, dove `CallScreeningService` è pienamente supportato)
- JDK 17

## Build

Il modo più semplice è **aprire la cartella in Android Studio**: scaricherà SDK e
dipendenze e genererà il Gradle wrapper automaticamente.

Da riga di comando, genera una volta il wrapper (serve una installazione di Gradle 8.5+)
e poi compila:

```bash
gradle wrapper --gradle-version 8.9   # solo la prima volta
./gradlew :app:assembleDebug
```

L'APK di debug finisce in `app/build/outputs/apk/debug/`.

Per usare lo screening, al primo avvio l'app chiede di diventare la **app predefinita per
l'ID chiamante & spam** (impostazione di sistema Android): è il meccanismo ufficiale e
non richiede permessi invasivi.

## Licenza

MIT — vedi `LICENSE`. Usa questo software nel rispetto delle leggi applicabili.
