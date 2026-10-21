# Dragonfly

Dragonfly è un'app Android per le finanze personali che porta sul telefono le funzionalità di [Firefly III](https://github.com/firefly-iii/firefly-iii). Tutti i dati restano sul dispositivo e l'app funziona senza connessione. Se vuoi, puoi sincronizzarli tra più dispositivi con un account.

- **Piattaforma:** Android 8.0 (API 26) – Android 17 (API 37)
- **Linguaggio:** Kotlin 2.4 · **UI:** Jetpack Compose · **Dati:** Room, offline-first
- **Architettura:** ispirata a [Now in Android](https://github.com/android/nowinandroid): moduli per livello e per funzionalità, flusso dati unidirezionale, Hilt

---

## Indice

1. [Funzionalità](#funzionalità)
2. [Primo avvio](#primo-avvio)
3. [Architettura](#architettura)
4. [Moduli](#moduli)
5. [Stack tecnologico](#stack-tecnologico)
6. [Configurazione e build](#configurazione-e-build)
7. [Sincronizzazione con Supabase](#sincronizzazione-con-supabase)
8. [Import da Firefly III e da CSV](#import-da-firefly-iii-e-da-csv)
9. [Test](#test)
10. [Convenzioni](#convenzioni)
11. [Limiti noti](#limiti-noti)

---

## Funzionalità

### Conti e transazioni

- **Conti** di attività (conto corrente, risparmio, carta, contanti) e di passività (debiti, prestiti, mutui). Ogni conto ha un saldo iniziale e il saldo corrente si calcola dai movimenti. Il **patrimonio netto** è mostrato per valuta.
- **Spese, entrate e trasferimenti.** Per una spesa bastano importo e descrizione. Il conto è già scelto (quello usato più di recente) e si cambia con un tocco. Non c'è un beneficiario da inserire.
- **Trasferimenti tra valute**, con l'importo ricevuto dal conto di destinazione.
- **Transazioni divise:** una spesa in più parti, ognuna con importo, descrizione, categoria, budget e tag. Negli elenchi compaiono come una riga sola con il titolo e il totale.
- **Elenco mensile** con totali di entrate e uscite, **ricerca testuale** e **filtri** per tipo, categoria, tag, budget e conto.
- **Dettaglio del conto** con saldo, entrate e uscite e i movimenti visti da quel conto.

### Organizzazione

- **Categorie** e **tag**, creati scrivendone il nome, con lo speso del mese per categoria.
- **Regole automatiche:** condizioni su descrizione, conto, importo, tipo e note, in modalità "tutte" o "almeno una". Le azioni impostano categoria, tag, budget o bolletta, oppure cambiano descrizione e note. Le regole valgono per le nuove transazioni (le modifiche manuali non vengono mai sovrascritte). Si possono applicare anche a quelle esistenti, con un'anteprima.

### Pianificazione

- **Budget mensili** con speso, rimanente e quota giornaliera, più le uscite senza budget.
- **Transazioni ricorrenti** (giorni, settimane, mesi, anni; fino a una data o per un numero di volte). Vengono create da sole anche ad app chiusa, con una **notifica** che le elenca.
- **Bollette e abbonamenti** con scadenze, importo minimo e massimo, stato del periodo (pagata, in scadenza, in ritardo) e pagamento rapido.
- **Salvadanai:** obiettivi di risparmio dentro un conto, con quota mensile suggerita e disponibile del conto.

### Analisi

- **Home** con patrimonio e andamento a 12 mesi, entrate e uscite degli ultimi 6 mesi, spese per categoria, budget, bollette in scadenza e salvadanai.
- **Report** per mese, trimestre o anno: panoramica con confronto sul periodo precedente e tasso di risparmio, spese ed entrate per categoria, budget e tag.

### Dati

- **Backup** JSON completo e **ripristino**: il file viene validato prima e applicato in modo atomico.
- **Backup automatico** settimanale in una cartella a scelta, anche su Google Drive. Vengono tenuti gli ultimi 5 file.
- **Esportazione CSV** delle transazioni, apribile con Excel, Fogli Google o Numbers.
- **Import CSV** dagli estratti conto bancari e **import completo da Firefly III** (vedi [sotto](#import-da-firefly-iii-e-da-csv)).
- **Sincronizzazione** tra dispositivi con Supabase (vedi [sotto](#sincronizzazione-con-supabase)).

### Sicurezza e privacy

- **Blocco dell'app** con impronta, volto o PIN del telefono, all'avvio e ogni volta che l'app torna in primo piano.
- **Anteprima nascosta nelle app recenti**, attiva di default.
- **Notifiche private:** sulla schermata di blocco non si vedono gli importi.

### Interfaccia

- **Design minimal**, con tema chiaro e scuro che segue il sistema.
- Navigazione con cinque schede: **Home, Conti, Transazioni, Budget, Altro**. In "Altro" trovi account, report, bollette, salvadanai, ricorrenti, categorie, tag, regole, notifiche, sicurezza e backup.

---

## Primo avvio

1. Compare la schermata di **Login**. Puoi accedere con **Google** o con un **codice via email**, oppure scegliere **"Continua senza account"**. La scelta viene ricordata, e puoi accedere più tardi da *Altro > Account e sincronizzazione*.
2. Crea un conto in *Conti*, oppure importa i dati da Firefly III o da un CSV in *Altro > Backup e dati*.
3. Se vuoi, attiva il blocco biometrico in *Altro > Sicurezza*.

---

## Architettura

L'app ha tre livelli. Le dipendenze vanno verso il basso, mentre i dati risalgono come `Flow` osservabili.

```
┌─────────────────────────────────────────────────────────────┐
│ UI  ·  feature:*  +  app                                      │
│ Schermate Compose senza stato · ViewModel con StateFlow<UiState> │
└──────────────▲───────────────────────────┬──────────────────┘
               │ Flow                      │ chiamate
┌──────────────┴───────────────────────────▼──────────────────┐
│ Dati  ·  core:data (repository)                               │
└──────────────▲───────────────────────────┬──────────────────┘
               │                           │
┌──────────────┴───────────────────────────▼──────────────────┐
│ Sorgenti  ·  core:database (Room) · core:datastore · core:network (Supabase) │
└─────────────────────────────────────────────────────────────┘
```

### Pattern principali

| Pattern | Come è applicato |
|---|---|
| **MVVM + flusso unidirezionale** | Ogni schermata ha un ViewModel che combina i `Flow` dei repository in uno stato immutabile (`StateFlow`, con `SharingStarted.WhileSubscribed(5_000)`). La UI lo raccoglie con `collectAsStateWithLifecycle()` e invia gli eventi come chiamate di metodo. Le schermate sono divise in `XScreen` (con ViewModel) e `XContent` (solo stato e callback, usato dalle anteprime). |
| **Repository, fonte di verità unica** | Le feature vedono solo le interfacce `XRepository` di `core:data`. Le letture sono `Flow`, le scritture `suspend`. Dopo una scrittura la UI si aggiorna da sola, perché Room ripubblica i `Flow`. |
| **Offline-first** | Tutto funziona sul database locale. La rete serve solo alla sincronizzazione in background. |
| **Dependency injection** | Hilt: binding per modulo, `@HiltViewModel`, `@HiltWorker`. I ViewModel che usano "oggi" ricevono un `Clock`, così i test sono deterministici. |
| **Dominio puro** | `core:model` è Kotlin/JVM senza Android. Contiene ricorrenze, stato delle bollette, motore delle regole, parser CSV e Firefly e gestione del denaro, testati sulla JVM. |
| **Atomicità** | `DatabaseTransactionRunner` (Room `withTransaction`) per le operazioni su più righe: import, ripristino, ricorrenti, transazioni divise. |
| **Navigazione type-safe** | Route `@Serializable` per destinazione. Ogni feature espone `NavGraphBuilder.xScreen()` e `NavController.navigateToX()`, e il grafo si compone in `app`. |
| **Design system chiuso** | Le feature disegnano solo con i componenti `Df*` di `core:designsystem`. Material 3 è una dipendenza `implementation` di quel modulo e non è visibile alle feature. |

### Scelte sui dati

- **Importi in unità minori** (`Long`, €42,50 = `4250`), mai `Double`.
- **Importi sempre positivi**, con la direzione data dal tipo (uscita, entrata, trasferimento), come in Firefly III.
- **Date come giorni dall'epoca**: nessun problema di fusi orari, raggruppamenti per mese in SQL.
- **Saldi calcolati in SQL**, mai salvati.
- **Partita doppia semplificata:** ogni movimento va da un conto a un altro. Spese ed entrate passano da un conto tecnico "Senza nome", invisibile nell'app, così l'utente non deve indicare un beneficiario.

---

## Moduli

```
app
core/  model · database · datastore · data · network · designsystem · ui · work · testing
feature/  home · accounts · transactions · classification · budgets · bills · piggybanks
          rules · reports · backup · csvimport · sync · lock · settings
build-logic/  convention plugin
supabase/migrations/  schema SQL del server
```

| Modulo | Contenuto |
|---|---|
| `app` | `DragonflyApp` (Hilt, WorkManager, pianificazione dei worker), `MainActivity` (tema, anteprima nascosta), `MainActivityViewModel` (blocco e login), `DragonflyAppRoot`, `DragonflyNavHost` e le 5 schede. |
| `core:model` | Modelli e logica pura: `Transaction`, `TransactionDraft`, `RecurrenceRule`, `Bill.statusOn()`, `Rule`, `PiggyBankSummary`, `Money.kt`, `CsvImport.kt`, `FireflyImport.kt`, `Sync.kt`. |
| `core:database` | `DragonflyDatabase` (Room, versione 10, migrazioni automatiche, schemi in `core/database/schemas`), entità, DAO (`TransactionDao`, `AccountDao`, `InsightsDao`…), `sync/` (trigger e `SyncStore`). |
| `core:datastore` | `UserPreferencesDataSource` (Preferences DataStore). |
| `core:network` | `SyncNetworkDataSource` e l'implementazione Supabase (Auth e PostgREST, motore OkHttp). |
| `core:data` | Repository: conti, transazioni (con `RuleApplier`), categorie, tag, budget, bollette, salvadanai, ricorrenti, regole, statistiche, backup, import, sincronizzazione, preferenze. |
| `core:designsystem` | `DfTheme` (colori, tipografia, spaziature) e componenti `Df*`: scaffold, card, liste, pulsanti, campi, menu, chip, schede, dialoghi, grafici, barra di navigazione. |
| `core:ui` | Composable che conoscono il dominio: `TransactionRow`, `DayHeader`, testi delle ricorrenze, permesso di notifica. |
| `core:work` | `RecurringTransactionsWorker` con `RecurringNotifier`, `SyncWorker`, `AutoBackupWorker`. |
| `core:testing` | Fake in memoria di tutti i repository e `MainDispatcherRule`. |
| `feature:*` | Una funzionalità per modulo, con schermate, ViewModel e navigazione. |

### Convention plugin (`build-logic`)

| Plugin | Uso |
|---|---|
| `dragonfly.android.application` / `.application.compose` | Modulo `app` |
| `dragonfly.android.library` / `.library.compose` | Librerie Android, con o senza Compose |
| `dragonfly.android.feature` | Ogni feature: library, compose, hilt e dipendenze comuni |
| `dragonfly.android.hilt` | Hilt e KSP |
| `dragonfly.android.room` | Room, KSP ed esportazione degli schemi |
| `dragonfly.jvm.library` | Moduli Kotlin puri (`core:model`) |

SDK, Java 17 e opzioni del compilatore sono definiti una sola volta in `build-logic`. Le versioni stanno in `gradle/libs.versions.toml`.

---

## Stack tecnologico

| Tecnologia | Versione | Perché |
|---|---|---|
| Kotlin | 2.4.21 | Linguaggio unico; coroutine, sealed class per gli stati |
| Android Gradle Plugin | 9.4.1 | SDK 37 e Kotlin compilato direttamente da AGP |
| Gradle | 9.8.1 | Version catalog, configuration cache, build parallela |
| Jetpack Compose (BOM) | 2026.09.00 | UI dichiarativa; Material 3 solo dentro il design system |
| Navigation Compose | 2.10.2 | Navigazione type-safe con route `@Serializable` |
| Lifecycle | 2.11.0 | ViewModel, raccolta dei Flow legata al ciclo di vita, eventi di processo |
| Hilt | 2.60.1 | DI verificata a compile time, integrata con ViewModel e WorkManager |
| Room | 2.8.5 | SQL controllato a compile time, Flow reattivi, migrazioni automatiche |
| DataStore | 1.2.1 | Preferenze asincrone e transazionali |
| WorkManager | 2.12.0 | Ricorrenti, backup e sincronizzazione anche ad app chiusa |
| kotlinx.serialization | 1.11.0 | Route, backup JSON, record di sincronizzazione |
| Coroutines | 1.11.0 | Concorrenza e Flow |
| Biometric | 1.1.0 | Blocco con impronta, volto o PIN |
| DocumentFile | 1.1.0 | Cartelle del backup automatico (Storage Access Framework) |
| supabase-kt | 3.8.0 | Login e sincronizzazione (Auth, PostgREST) |
| Ktor OkHttp | 3.5.1 | Motore HTTP di supabase-kt |
| Credential Manager · Google ID | 1.6.0 · 1.2.1 | Accesso con Google |
| JUnit 4, coroutines-test, Turbine, Room Testing | — | Test unitari e su emulatore |

Una spiegazione dettagliata di classi, pattern e scelte è in *Dragonfly-architettura.pdf*, nella libreria del progetto.

---

## Configurazione e build

### Requisiti

- Android Studio con **JDK 17+** (quello incluso va bene)
- Android SDK **37**

### `secrets.properties`

Crea `secrets.properties` nella root del progetto. Il file è escluso da git.

```properties
# Sincronizzazione (facoltativa): progetto Supabase
SUPABASE_URL=https://<id-progetto>.supabase.co
SUPABASE_KEY=<Publishable key, da Project Settings > API Keys>

# Accesso con Google (facoltativo): Client ID del client OAuth "Web application"
GOOGLE_WEB_CLIENT_ID=<numero>-<id>.apps.googleusercontent.com
```

Senza questi valori l'app compila e funziona lo stesso, solo in locale. La sincronizzazione risulta "non configurata" e il pulsante Google non compare.

### Comandi

```bash
./gradlew assembleDebug                 # APK di debug
./gradlew testDebugUnitTest :core:model:test   # test unitari
./gradlew connectedDebugAndroidTest     # test su emulatore o dispositivo
./gradlew lintDebug                     # lint
```

> Con più dispositivi collegati, limita i test strumentali a uno con `ANDROID_SERIAL=<id> ./gradlew connectedDebugAndroidTest`.

---

## Sincronizzazione con Supabase

### Come funziona

- **Tracciamento locale.** Trigger SQLite (`core:database/sync/SyncTriggers.kt`) registrano in `sync_meta` ogni inserimento, modifica ed eliminazione di tutte le tabelle sincronizzate, cascate comprese. Ogni riga riceve un id globale (UUID). I DAO non cambiano.
- **Una tabella remota generica**, `sync_records`, con i dati di ogni riga in JSON. I riferimenti tra righe (per esempio il conto di una transazione) viaggiano come id globali, ricavati dalle chiavi esterne dichiarate da SQLite.
- **Ultima modifica vince.** Un trigger sul server ignora gli aggiornamenti più vecchi di quello salvato. In ricezione, una modifica locale più recente viene mantenuta.
- **Quando sincronizza:** all'apertura e alla chiusura dell'app, ogni ora e con *Sincronizza ora*. Serve la rete, e in caso di errore ritenta con backoff.
- **Isolamento:** Row Level Security, per cui ogni utente legge e scrive solo le proprie righe.

### Configurare il progetto Supabase

1. **Schema:** in *SQL Editor* esegui `supabase/migrations/20261019000000_sync_records.sql`.
2. **Codice via email:** in *Authentication > Emails* aggiungi `{{ .Token }}` ai modelli *Magic link* e *Confirm signup*.
   > Senza un SMTP proprio, Supabase invia email solo ai membri del progetto e con un limite orario. Per altri utenti configura un SMTP in *Authentication > SMTP Settings*.
3. **Accesso con Google:**
   1. In Google Cloud > *APIs & Services > Credentials* configura la schermata di consenso e crea due client OAuth:
      - **Web application**: il suo Client ID va in `GOOGLE_WEB_CLIENT_ID`, e Client ID e secret vanno in Supabase.
      - **Android**: pacchetto `com.salco.dragonfly` e SHA-1 della chiave di firma (`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android` per quella di debug).
   2. In Supabase > *Authentication > Sign In / Providers > Google* attivalo con Client ID e secret del client Web.
4. Aggiungi le chiavi a `secrets.properties` e ricompila.

> Se l'accesso con Google fallisce con *"[28444] Developer console is not set up correctly"*, controlla che `GOOGLE_WEB_CLIENT_ID` sia il Client ID del client **Web** (finisce con `.apps.googleusercontent.com`) e che il client Android usi lo SHA-1 corretto. Il motivo esatto è nel logcat con il tag `GoogleSignIn`.

---

## Import da Firefly III e da CSV

Si trovano in *Altro > Backup e dati > Importa transazioni da CSV*.

### Export di Firefly III

Esporta le transazioni da Firefly III in CSV e scegli il file: l'app lo **riconosce da solo** e lo importa per intero, senza mappare le colonne.

- Crea i conti mancanti con il loro **saldo iniziale**.
- Importa uscite, entrate, **trasferimenti**, **riconciliazioni** e **transazioni divise**, con categoria, budget e tag.
- Legge gli importi a 12 decimali di Firefly, che hanno sempre segno negativo, ricavando la direzione dal tipo.
- **Non applica le regole**, perché i dati restano come in Firefly.
- Salta i movimenti già presenti, quindi reimportare lo stesso file non duplica nulla. Due righe identiche nello stesso file vengono importate entrambe.
- Crea i budget **disattivati**, perché l'export non contiene gli importi. Bollette, regole e salvadanai vanno ricreati.

### CSV bancario

Per gli estratti conto l'app riconosce codifica (UTF-8 o Windows-1252), separatore, intestazione, formato della data e ruolo delle colonne, compresi i nomi delle banche italiane come *Dare* e *Avere*. Ogni colonna si può correggere. L'anteprima separa righe valide, doppie e non valide. L'import va su un conto scelto, è atomico e applica le regole.

---

## Test

| Tipo | Dove | Cosa copre |
|---|---|---|
| Unitari (JVM) | `core/model/src/test`, `feature/*/src/test`, `core/work/src/test` | Ricorrenze, bollette, regole, denaro, CSV, Firefly, transazioni divise, report; ViewModel con i fake di `core:testing` |
| Strumentali | `core/database/src/androidTest`, `core/data/src/androidTest` | DAO e saldi, **tutte le migrazioni** 1→10, sincronizzazione tra due database (conflitti, cancellazioni a cascata, riferimenti in ritardo), backup, import, ricorrenti, salvadanai, statistiche, regole |

I test dei ViewModel raccolgono lo stato in `backgroundScope` con `UnconfinedTestDispatcher`, perché `WhileSubscribed` emette solo quando qualcuno osserva, e usano un `Clock` fisso.

---

## Convenzioni

- **Lingua:** codice e commenti in inglese, interfaccia in italiano.
- **Una schermata = `XScreen` + `XContent` + `XViewModel`.** Lo stato è una `data class` o `sealed interface` immutabile.
- **Nessun Material diretto nelle feature:** solo componenti `Df*`.
- **Schema del database:** ogni modifica aumenta `DragonflyDatabase.VERSION`, aggiunge una `@AutoMigration` (o una migrazione manuale), esporta lo schema e aggiunge un test in `MigrationTest`.
- **Segreti** solo in `secrets.properties`.
- **Branch** `feat/<nome>` con una PR verso `main`.

---

## Limiti noti

- La sincronizzazione risolve i conflitti per riga ("ultima modifica vince"): due modifiche a campi diversi della stessa riga fatte offline non si fondono.
- I budget hanno un importo fisso al mese, senza rollover.
- Backup, export e database locale non sono cifrati.
- Per l'accesso Google in release serve un client OAuth Android con lo SHA-1 della chiave di produzione.
