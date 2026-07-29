# Fin.Est — Project Context

Personal finance tracker for Android. Manual income/expense entry + automatic capture of bank
transaction SMS (with a notification and a review queue), per-user cloud sync. Built for personal
sideload use (no Play Store).

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
| Local DB | Room 2.7.1 (KSP), schemas exported to `app/schemas` |
| Cloud | Firebase Auth (Google Sign-In via Credential Manager) + Firestore |
| Navigation | navigation-compose 2.9.0 |
| Charts | hand-rolled Compose (`presentation/components/charts`) — no chart library |
| Gradle | AGP 8.9.3, version catalog at [libs.versions.toml](gradle/libs.versions.toml) |

Single module: `:app`. Dependencies come from the version catalog, except a few inline
`implementation("…")` lines at the bottom of [app/build.gradle.kts](app/build.gradle.kts)
(credentials, play-services-auth, material-icons-extended).

```bash
./gradlew :app:assembleDebug
```

```bash
./gradlew :app:testDebugUnitTest
```

```bash
./gradlew :app:installDebug
```

Notes:
- Both `kotlin-kapt` and KSP are applied; only KSP processes annotations (Room + Hilt).
  `kapt { correctErrorTypes = true }` is vestigial.
- **Windows quirk on the dev machine:** the Gradle daemon dies with
  `java.io.IOException: Unable to establish loopback connection` when `TMP`/`TEMP` point under
  `C:\Users\…\AppData\Local\Temp` (AF_UNIX socket files are blocked there, so `Pipe.open()` fails
  for any JVM). Prefix builds with a temp dir on another volume if you hit it:
  `TMP='D:\Projects\.claude-build-tmp' TEMP='D:\Projects\.claude-build-tmp' ./gradlew …`.
  Android Studio is unaffected.

---

## 2. Architecture

MVVM + repository, layered by package:

```
com.pkoder.finest
├── App.kt                     @HiltAndroidApp; creates the notification channel
├── MainActivity.kt            @AndroidEntryPoint; hosts Compose, runtime permissions,
│                              routes notification taps into SmsViewModel
├── auth/                      self-contained auth slice
│   ├── data/repository/       AuthRepository, AuthRepositoryImpl, AuthResult
│   ├── di/AuthModule.kt       binds AuthRepositoryImpl → AuthRepository
│   ├── domain/model/          UserData
│   └── presentation/          SignInScreen, AuthViewModel
├── data/
│   ├── local/                 FinanceDatabase, Migrations, dao/, entities/
│   └── repository/            FinanceRepository, SyncReconcile, UserProfileRepository
├── di/                        AppModule (Room, DAOs, @ApplicationScope), FirebaseModule,
│                              ApplicationScope (qualifier), ReceiverEntryPoint
├── domain/model/              PendingTransaction, TransactionType, TransactionOptions
├── notification/              TransactionNotifier, NotificationActionReceiver
├── presentation/
│   ├── components/            AmountTextField, DropdownField, CommonComponents,
│   │                          MoneyComponents, charts/
│   ├── navigation/            NavRoutes, NavigationGraph, BottomNavigationBar
│   ├── screens/               MainScreen (shell), Dashboard, History, Stats, Review,
│   │                          About(profile), entry/EntrySheets
│   ├── ui/theme/              FinEstTheme, MoneyColors, Dimens/Shapes, Type
│   ├── util/                  Formatters, Period
│   └── viewmodel/             FinanceViewModel, SmsViewModel
└── sms/                       SmsBroadcastReceiver, SmsParser, SmsParts
```

**Data flow:** Room is the single source of truth.
`DAO Flow → repository → ViewModel.stateIn → collectAsState`. Screens never trigger reloads;
`FinanceViewModel.refresh()` only talks to the network. `FinanceViewModel`, `SmsViewModel` and
`AuthViewModel` are hoisted once (in `MainScreen`/`NavigationGraph`) and passed down, so every
screen sees the same instance.

---

## 3. Data model

### Room — `finance_db`, version 4, `exportSchema = true`

| Entity | Table | Primary key | Fields |
|---|---|---|---|
| `DebitEntryEntity` | `debit_entries` | `firestoreId: String` | category, paymentMethod, bank, amount, description?, timestamp, **synced** |
| `CreditEntryEntity` | `credit_entries` | `firestoreId: String` | source, amount, timestamp, **synced** |
| `PendingTransactionEntity` | `pending_transactions` | `id: String` (SHA-256 of the SMS) | type, amount, bank, paymentMethod, category, source, description, timestamp, rawSms |

- **Real migrations only.** `fallbackToDestructiveMigration` is gone;
  [Migrations.kt](app/src/main/java/com/pkoder/finest/data/local/Migrations.kt) holds
  `MIGRATION_3_4` (adds `synced`, back-fills `synced = 0` for legacy `local_%` ids). Add a new
  `Migration` for every schema change — a version bump without one now throws instead of wiping data.
- `firestoreId` and `synced` are `@get:Exclude`d, so remote documents contain only real fields and
  rows read back from the server default to `synced = true`.
- Debit/credit DAOs use `REPLACE`; the pending DAO uses `IGNORE` and its `insert` returns the rowId
  (`-1` = duplicate), which is how the SMS receiver decides whether to post a notification.

### Firestore layout

```
users/{uid}                       ← UserData doc (written on sign-in by UserProfileRepository)
users/{uid}/debit_entries/{id}
users/{uid}/credit_entries/{id}
```

Document ids are generated **client-side** (`collection.document().id`) and reused as the Room
primary key, so retries and re-syncs are idempotent. Pending SMS rows stay local.

---

## 4. Key flows

### Auth
`AuthViewModel` seeds state from `FirebaseAuth.currentUser`; `AuthResult.SignedOut` is the
signed-out state (not `Error`). `SignInScreen` uses Credential Manager +
`GetSignInWithGoogleOption` with `R.string.web_client_id`, and handles `NoCredentialException` /
`GetCredentialCancellationException` distinctly. On success the profile doc is written and
`syncNow()` runs (failures there are logged, never demoting a successful sign-in). Sign-out clears
local data, cancels notifications and resets the nav stack.

### Write path (local-first)
1. Generate the Firestore document id offline.
2. Insert into Room with `synced = false` → the UI updates immediately.
3. Push to Firestore with an 8 s timeout; on ack, flip `synced = true`.

Firestore only acks writes against the server, so awaiting one while offline would hang forever —
hence the timeout plus the queue.

### `FinanceRepository.syncNow()`
1. Upload every `synced = 0` row with `set()` on its own id (legacy `local_…` ids get a fresh
   document and the old row is deleted).
2. Pull both collections; rows with unsynced local edits are **not** overwritten.
3. Prune local synced rows missing from the snapshot (deleted on another device) —
   [`idsToPrune`](app/src/main/java/com/pkoder/finest/data/repository/SyncReconcile.kt) is pure and
   unit-tested. Unsynced rows are never pruned.

Called from `FinanceViewModel.refresh()` (init + pull-to-refresh on the dashboard) and after sign-in.

### SMS capture → notification → review
1. `SmsBroadcastReceiver` is **manifest-declared only** (`SMS_RECEIVED` is exempt from the
   implicit-broadcast ban, so it fires in every app state). It pulls dependencies via
   `EntryPointAccessors` + [ReceiverEntryPoint](app/src/main/java/com/pkoder/finest/di/ReceiverEntryPoint.kt) —
   `@AndroidEntryPoint` on a receiver would need `super.onReceive()`, which Kotlin cannot express.
2. `joinSmsParts` stitches multipart messages by sender before parsing, so a >160-char bank alert
   is one transaction, not two halves.
3. `SmsParser.parse(sender, body, timestampMillis)` uses the **SMS's own timestamp** and derives the
   row id from `sha256(sender|body|timestamp)` — a duplicated broadcast collides and `IGNORE` drops it.
4. Only a genuinely new row triggers `TransactionNotifier.notifyPending`.
5. The notification carries **Approve** / **Reject** actions (handled by
   `NotificationActionReceiver` without opening the app, via the shared
   `FinanceRepository.approvePending`) and an `EXTRA_PENDING_ID` content intent. `MainActivity`
   (launchMode `singleTop`, plus `onNewIntent`) forwards that id to `SmsViewModel.requestReview`,
   `MainScreen` navigates to Review, and Review highlights the card and opens its detail sheet.
6. Resolving a transaction in-app cancels its notification.

### SmsParser rules ([sms/SmsParser.kt](app/src/main/java/com/pkoder/finest/sms/SmsParser.kt))
- Bank: sender-id map (`SBIINB`, `HDFCBK`, `BOBSMS`, …) then a body-keyword map for numeric
  shortcodes. **Unknown bank ⇒ ignored.** Only SBI, HDFC, BOB today.
- Type: debit keywords beat credit keywords (UPI "Dr."/"Cr." pairs are debits for us).
- Amount: `Rs./INR/₹` regex, fallback `"debited by 5849.25"`.
- Payment method: UPI (keyword or UPI-id regex), NEFT, IMPS, RTGS, ATM, credit/debit card, else
  "Bank Transfer".
- Description: UPI id if present, else the text after "to"/"from".

---

## 5. UI map

`MainActivity` → `FinEstTheme` → `MainScreen` → `NavigationGraph`.

`MainScreen` is the shell: one `CenterAlignedTopAppBar` (title per route + profile avatar), one
`NavigationBar`, one `SnackbarHost`, and the add-entry FAB (Home/History only). Screens are content
only — they do not nest their own Scaffold.

| Route | Screen | Content |
|---|---|---|
| `sign_in` | `SignInScreen` | Logo, tagline, Google button; no bars |
| `home` | `DashboardScreen` | Balance hero (month income − expense), income/expense tiles, "N payments need review" prompt, recent activity, pull-to-refresh |
| `history` | `HistoryScreen` | Search + type/period filter chips, day headers with daily net, tap to edit, delete with an **Undo** snackbar (the write is deferred until the snackbar closes) |
| `stats` | `StatsScreen` | Expense/Income toggle, period chips, donut + breakdown bars, month-by-month bars |
| `review` | `ReviewScreen` | Pending SMS cards, expandable raw SMS, Approve/Reject, "Reject all", detail sheet for corrections |
| `about` | `AboutScreen` | Avatar/email, sync state + unsynced count, totals, version, Sign out |

Add and edit both use `screens/entry/EntrySheets.kt` (`AddEntrySheet` with an Expense/Income
segmented toggle, `EditDebitSheet`, `EditCreditSheet`) so the field logic exists once.

Dropdown vocabulary lives in
[TransactionOptions](app/src/main/java/com/pkoder/finest/domain/model/TransactionOptions.kt); the
manual forms and the SMS review sheet share it, and `withCurrent()` keeps a parser-produced value
(e.g. "IMPS") selectable even if it is not in the list.

---

## 6. Conventions

- **Hilt everywhere.** Broadcast receivers are the one exception and use `ReceiverEntryPoint`
  instead of field injection. Long-running receiver work runs in the injected
  `@ApplicationScope CoroutineScope`, wrapped in `goAsync()`.
- ViewModels expose `StateFlow`; lists come from `repository.observeX().stateIn(...)`. No manual
  reload calls, no `LaunchedEffect { load() }`.
- Coroutines only — `suspend` DAOs, `.await()` for Firebase tasks, nothing blocking the main thread.
- Repository logs remote failures and always keeps the local write; log tags are the class name.
- **Money and dates go through [Formatters.kt](app/src/main/java/com/pkoder/finest/presentation/util/Formatters.kt)**
  (`asMoney`, `asSignedMoney`, `asDateTime`, `asRelativeDay`, …). `asMoney` renders Indian grouping
  (`₹1,23,456.78`) via an explicit `groupIndianDigits`, because the JVM's `en-IN` currency format
  groups in thousands and `DecimalFormat` supports only one grouping size. Never format money inline.
- **Colour:** Material 3 dynamic colour on Android 12+ for chrome; income/expense semantics come from
  `moneyColors` (`LocalMoneyColors`), which is fixed for light and dark so money never changes
  meaning between devices. Charts use `moneyColors.chartPalette`.
- Spacing/shapes come from `Spacing` and `FinEstShapes` (`ui/theme/Dimens.kt`).
- Notification strings live in `strings.xml`; most in-screen strings are still inline literals.

---

## 7. Testing

`app/src/test` (JVM, no extra dependencies):

| Test | Covers |
|---|---|
| `SmsParserTest` | SBI/HDFC/BOB samples, debit-beats-credit, currency + fallback amounts, unknown bank → null, UPI description, dedupe-id stability |
| `SmsPartsTest` | multipart joining, and that a joined message parses as one transaction |
| `SyncReconcileTest` | `idsToPrune` — including that an empty snapshot never touches unsynced rows |
| `FormattingTest` | amount-input sanitising (decimals!), Indian grouping, negative money, `periodStart` boundaries |

`app/src/androidTest/.../MigrationTest.kt` builds a real v3 database with raw SQL, opens it with
`MIGRATION_3_4`, and asserts rows survive, `synced` is back-filled from the `local_` prefix, and the
pending queue is intact. **It needs a connected device/emulator** (`./gradlew :app:connectedDebugAndroidTest`)
and has not been executed in this environment.

---

## 8. Known issues / gotchas

1. **Offline delete can resurrect.** Deleting a synced row while offline removes it locally, but the
   Firestore delete fails and the next pull brings it back. A tombstone table would fix it.
2. **`clearLocalData()` on sign-out drops unsynced rows.** The profile screen warns when any exist;
   there is no forced upload before sign-out.
3. **Manual entries are stamped "now".** No date picker yet, so a back-dated expense needs editing
   after the fact (SMS-derived rows do carry the real SMS time).
4. **Only SBI, HDFC and BOB are recognised.** Any other sender is ignored outright; adding a bank
   means extending both maps in `SmsParser`.
5. **Dependency versions are behind** (lint lists ~57 upgrades, incl. Compose BOM and AGP). Left
   alone deliberately — upgrading is its own change with its own testing.
6. **Launcher-icon template leftovers.** `drawable/ic_launcher_{background,foreground}.png` are
   unused duplicates of the mipmap assets, and the adaptive icon has no `monochrome` layer.
7. **Secrets in the repo.** `app/google-services.json` and the OAuth `web_client_id` in
   `strings.xml` are committed by choice for a personal sideload app. Do not treat the repo as
   private-safe if that changes.
8. **In-screen strings are still literals** — only notification strings moved to `strings.xml`.
9. **Firestore has no security rules in this repo.** Per-user isolation relies on the client writing
   under `users/{uid}`; rules should be enforced server-side too.

---

## 9. Status & roadmap

Done: manual entry via FAB sheet, dashboard, searchable/filterable history with undo, Compose
charts, Google auth, Room + Firestore sync with an offline queue and remote-delete reconciliation,
SMS capture with dedupe and multipart handling, captured-payment notifications with shade actions
and deep-link review, dark theme, unit tests + a migration test.

Pending: bulk import of historical SMS from the inbox, smarter auto-categorisation (keyword or
Claude API), CSV export + share, date picker for manual entries, signed release APK, tombstones for
offline deletes.

Branches: `master` (main), `IR1`, `IR2`, `IR2.O`, `IR2.H` (current). `.temp/` holds `AGENT.md`
(working agreement) and `FinEst_BuildPlan.md` (original phased plan); both are gitignored.
