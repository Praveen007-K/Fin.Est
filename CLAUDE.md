# Fin.Est — Project Context

Personal finance tracker for Android. Manual income/expense entry + automatic capture of
bank transaction SMS, with per-user cloud sync. Built for personal sideload use (no Play Store).

> This file is the durable context for anyone (human or AI) picking up the codebase.
> Keep it updated when architecture, conventions, or known issues change.

---

## 1. Stack & Build

| Item | Value |
|---|---|
| Language | Kotlin 2.1.21 |
| UI | Jetpack Compose (Material 3) — **not** XML/Fragments |
| Package / applicationId | `com.pkoder.finest` |
| compileSdk / targetSdk | 35 |
| minSdk | 24 |
| Java / jvmTarget | 1.8 |
| DI | Hilt 2.56.2 (KSP) |
| Local DB | Room 2.7.1 (KSP) |
| Cloud | Firebase Auth (Google Sign-In via Credential Manager) + Firestore |
| Navigation | navigation-compose 2.9.0 |
| Charts | MPAndroidChart v3.1.0 (via JitPack, wrapped in `AndroidView`) |
| Gradle | AGP 8.9.3, version catalog at [libs.versions.toml](gradle/libs.versions.toml) |

Single module: `:app`. Dependencies are declared through the version catalog, except a few
inline `implementation("…")` lines at the bottom of [app/build.gradle.kts](app/build.gradle.kts)
(credentials, play-services-auth, MPAndroidChart, material-icons-extended).

Build / install:

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:installDebug
```

Note: both `kotlin-kapt` and KSP plugins are applied; only KSP actually processes annotations
(Room + Hilt). `kapt { correctErrorTypes = true }` is vestigial.

---

## 2. Architecture

MVVM + repository, loosely layered by package:

```
com.pkoder.finest
├── App.kt                     @HiltAndroidApp
├── MainActivity.kt            @AndroidEntryPoint, hosts Compose tree, SMS permissions/receiver
├── auth/                      self-contained auth feature slice
│   ├── data/repository/       AuthRepository + Impl, AuthResult sealed class
│   ├── di/AuthModule.kt       binds AuthRepositoryImpl → AuthRepository
│   ├── domain/model/          UserData
│   └── presentation/          SignInScreen, AuthViewModel
├── data/
│   ├── dummy/DummyData.kt     leftover sample data (mostly commented out)
│   ├── local/                 FinanceDatabase, dao/, entities/
│   └── repository/            FinanceRepository (Room+Firestore), UserProfileRepository
├── di/                        AppModule (Room+DAOs), FirebaseModule (Auth, Firestore)
├── domain/model/              DebitEntry, CreditEntry, PendingTransaction, TransactionType
├── presentation/
│   ├── components/            AmountTextField, DropdownField, GoogleSignInButton (empty stub)
│   ├── navigation/            NavRoutes, NavigationGraph, BottomNavigationBar, AppDrawer, NavItem
│   ├── screens/               Main, Home, Stats, History, Review, About + tabs/
│   ├── ui/theme/              FinEstTheme (dynamic color, dark by system)
│   └── viewmodel/             FinanceViewModel, SmsViewModel
└── sms/                       SmsBroadcastReceiver, SmsParser
```

Data flow: **Composable → ViewModel (StateFlow) → FinanceRepository → Firestore then Room.**
There is no `Flow`/`LiveData` from Room; DAOs expose `suspend` one-shot reads and the
ViewModel re-queries via `loadAllEntries()` after every mutation and on screen entry
(`LaunchedEffect(Unit)` in Home/Stats/History).

---

## 3. Data model

### Room — `finance_db`, version 3, `exportSchema = false`

| Entity | Table | Primary key | Fields |
|---|---|---|---|
| `DebitEntryEntity` | `debit_entries` | `firestoreId: String` | category, paymentMethod, bank, amount, description?, timestamp |
| `CreditEntryEntity` | `credit_entries` | `firestoreId: String` | source, amount, timestamp |
| `PendingTransactionEntity` | `pending_transactions` | `id: String` (UUID) | type, amount, bank, paymentMethod, category, source, description, timestamp, rawSms |

The DB is built with `fallbackToDestructiveMigration(dropAllTables = true)` in
[AppModule.kt](app/src/main/java/com/pkoder/finest/di/AppModule.kt) — **any schema change wipes
local data**. Cloud data survives because debit/credit entries are re-fetched from Firestore;
pending SMS transactions are local-only and would be lost.

Debit/credit DAOs use `OnConflictStrategy.REPLACE`; the pending DAO uses `IGNORE` (dedupe).

### Firestore layout

```
users/{uid}/debit_entries/{autoId}
users/{uid}/credit_entries/{autoId}
users/{uid}                       ← UserData doc (UserProfileRepository)
```

The entity is written verbatim, so each remote doc also carries a redundant empty
`firestoreId` field; on read, the real `doc.id` is copied into the entity. This doc id is the
Room primary key, which is what keeps re-sync idempotent.

`PendingTransaction` (domain, `domain/model/PendingTransaction.kt`) is the SMS-derived
candidate; `TransactionType` is `DEBIT | CREDIT`. Pending rows are **not** synced to Firestore.

---

## 4. Key flows

### Auth
`AuthViewModel` seeds its state from `FirebaseAuth.currentUser`, so a signed-in user skips the
sign-in screen. `SignInScreen` uses **Credential Manager** + `GetSignInWithGoogleOption` with
`R.string.web_client_id`, hands the `GoogleIdTokenCredential` to `AuthViewModel.handleSignIn`,
which exchanges it for a Firebase credential. `NavigationGraph` picks its start destination
from `authState`. Sign-out (About screen) calls `repository.signOut()` **and**
`financeRepository.clearLocalData()` so the next user does not see stale rows, then resets the
nav stack with `popUpTo(0)`.

### Write path (`FinanceRepository.insertDebit/insertCredit`)
1. `add()` to Firestore under the current user → get doc id.
2. Insert into Room with `firestoreId = docId`.
3. On failure, still insert locally with `firestoreId = "local_<millis>"` (offline fallback).
   These local rows are never reconciled/uploaded later — see Known issues.

Read path (`getAllDebits/getAllCredits`): fetch remote → `insertAll` into Room → return the
Room query. Update/delete: mutate Firestore in `try`, always mutate Room in `finally`.

### SMS capture
1. `SmsBroadcastReceiver` is declared in the manifest (`SMS_RECEIVED`, priority 1000,
   `BROADCAST_SMS` permission) **and** registered at runtime by `MainActivity` after the
   `RECEIVE_SMS`/`READ_SMS` runtime grant.
2. `SmsParser.parse(sender, body)` returns a `PendingTransaction` or `null`.
3. The receiver persists the pending row (building its **own** Room instance, not the Hilt
   singleton) and also invokes the static `SmsBroadcastReceiver.onTransactionParsed` callback,
   which `MainActivity` wires to `SmsViewModel.addPending` for live UI updates.
4. `ReviewScreen` (bottom-nav "Review", badge shows pending count) lets the user Approve →
   converts to a Debit/Credit entry through `FinanceRepository` and deletes the pending row, or
   Reject → deletes the pending row.

### SmsParser rules ([sms/SmsParser.kt](app/src/main/java/com/pkoder/finest/sms/SmsParser.kt))
- Bank resolution: sender-id map (`SBIINB`, `HDFCBK`, `BOBSMS`, …) then a body-keyword map
  (handles numeric shortcodes). **Unknown bank ⇒ message ignored.** Only SBI, HDFC, BOB today.
- Type: debit keywords win over credit keywords (UPI "Dr."/"Cr." pairs are debits for us).
- Amount: `Rs./INR/₹` regex, fallback `"debited by 5849.25"` style.
- Payment method: UPI (keyword or UPI-id regex), NEFT, IMPS, RTGS, ATM, credit/debit card,
  else "Bank Transfer".
- Description: UPI id if present, else text after "to"/"from".
- Debits default to category `"Uncategorized"`; credits set `source = bank`.

---

## 5. UI map

`MainActivity` → `FinEstTheme` → `MainScreen` (Scaffold + bottom bar) → `NavigationGraph`.

| Route | Screen | Content |
|---|---|---|
| `sign_in` | `SignInScreen` | Google sign-in; bottom bar hidden here |
| `home` | `HomeScreen` | Tabs Debit/Credit → `DebitScreen`/`CreditScreen` entry forms |
| `stats` | `StatsScreen` | Tabs → `DebitStatsTab`/`CreditStatsTab`: total card, pie (category/source), bar (monthly) |
| `history` | `HistoryScreen` | Tabs Debit/Credit lists, edit via `ModalBottomSheet`, delete via confirm dialog |
| `review` | `ReviewScreen` | Pending SMS transactions, Approve/Reject, badge count |
| `about` | `AboutScreen` | Version, logged-in email, Sign Out |

Shared composables `EmptyState`, `DeleteConfirmDialog`, `EditDebitSheet`, `EditCreditSheet`
live at the bottom of [HistoryScreen.kt](app/src/main/java/com/pkoder/finest/presentation/screens/HistoryScreen.kt)
and are reused by other screens (e.g. `ReviewScreen` uses `EmptyState`).

Theme uses Material 3 dynamic color on Android 12+, falling back to the purple template
palette; dark mode follows the system.

Dropdown option lists are hardcoded in the tab composables:
categories `Housing, Food, Transport, Utilities, Dependents, Entertainment, Health, Finance`;
payment methods `UPI, Cash, Card, Net Banking`; banks `SBI, BOB, HDFC`;
income sources `Salary, Freelance, Gift, Other`.

---

## 6. Conventions

- Hilt for every dependency; no manual instantiation in UI/VM code
  (the SMS receiver is the one exception — see Known issues).
- ViewModels expose `StateFlow` via a private `MutableStateFlow`; collect with
  `collectAsState()`.
- Coroutines only — `viewModelScope.launch`, `suspend` DAOs, `kotlinx-coroutines-play-services`
  `.await()` for Firebase tasks. No callbacks, nothing blocking the main thread.
- Repository swallows remote errors: `try` remote / `Log.e` on failure / `finally` local write,
  so the app keeps working offline. Log tags are the class name (`FinanceRepository`,
  `AuthRepositoryImpl`, `SmsBroadcastReceiver`, …).
- Screens obtain view models with `hiltViewModel()`; the exception is `SmsViewModel`, hoisted
  in `MainScreen` and passed down so the bottom-bar badge and `ReviewScreen` share one instance.
- UI strings are written inline in composables today (only `app_name` and `web_client_id` are
  in `strings.xml`) — `.temp/AGENT.md` asks for `strings.xml`; the code does not follow it yet.
- Money is formatted as `"₹${"%.2f".format(amount)}"`; dates as
  `SimpleDateFormat("dd MMM yyyy, hh:mm a")`.

---

## 7. Known issues / gotchas

1. **Duplicate SMS handling.** The receiver is registered both in the manifest and at runtime,
   so while the app is in the foreground `onReceive` runs twice for one message. Each run calls
   `SmsParser.parse`, which mints a fresh `UUID`, so the `IGNORE` conflict strategy does not
   dedupe them — two pending rows can appear. Dedupe should key on `rawSms + timestamp`, or the
   runtime registration should be dropped.
2. **Receiver builds its own Room instance** instead of using the Hilt singleton
   (`Room.databaseBuilder(...)` inside `onReceive`). Two open instances of the same DB file and
   a rebuild per message. Fix with a Hilt `EntryPointAccessors` lookup.
3. **`local_<millis>` rows never reach Firestore.** Offline-created entries stay local-only and
   are dropped on sign-out (`clearLocalData`) or a destructive migration. No retry/queue exists.
4. **Destructive migrations.** Bumping `FinanceDatabase.version` drops all tables, including
   pending SMS transactions.
5. **Per-screen ViewModel instances.** `HomeScreen`, `StatsScreen`, `HistoryScreen` each call
   `hiltViewModel()` inside their own nav destination, so each gets its own `FinanceViewModel`
   and must re-run `loadAllEntries()`; data does not propagate between screens automatically.
6. **Secrets in the repo.** `app/google-services.json` and the OAuth `web_client_id` in
   `strings.xml` are committed. Acceptable for a personal sideload app; do not treat the repo
   as private-safe if that changes.
7. **Dead / stub code.** `GoogleSignInButton.kt` is an empty composable, `AppDrawer.kt` and
   `NavItem.kt` are unused, `DummyData.kt` is largely commented out, `domain/model/DebitEntry`
   and `CreditEntry` are unused (entities are passed straight to the UI).
8. **Tests.** Only the generated `ExampleUnitTest` / `ExampleInstrumentedTest` exist.
   `SmsParser` is a pure object and is the obvious first unit-test target.
9. **Docs drift.** `.temp/AGENT.md` (gitignored) says the UI is XML/Fragments and minSdk 26;
   both are wrong — it is Compose and minSdk 24.

---

## 8. Status & roadmap

Done: manual debit/credit entry, Room persistence, Firestore per-user sync, Google auth,
stats with pie/bar charts, transaction history with edit/delete, SMS receive + parse + review
queue with badge, dark theme.

Pending (from `.temp/FinEst_BuildPlan.md`): bulk import of historical SMS from the inbox,
smarter auto-categorization (keyword or Claude API), date-range filters and search, CSV export
+ share, signed release APK.

Branches: `master` (main), `IR1`, `IR2`, `IR2.O`, `IR2.H` (current — SMS parsing & review).
`.temp/` holds `AGENT.md` (working agreement, partly stale) and `FinEst_BuildPlan.md`
(original phased plan); both are gitignored.
