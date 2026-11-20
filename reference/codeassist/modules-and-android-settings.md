# CodeAssist — Modules ও Android Module Settings

সর্বশেষ screenshot batch: ২০২৬-১১-২০  
পরিবেশ: CodeAssist IDE 3.46.0  
প্রকল্প: Teddy-WebApp (`com.teddy.bpositive`)

এই নথি ব্যবহারকারীর সরবরাহ করা screenshot থেকে দৃশ্যমান তথ্য সংরক্ষণ করে। Screenshot-এ যা দেখা যায়নি, তা অনুমান করে যোগ করা হয়নি। CodeAssist-এ ব্যবহারকারী “module.toml” বলতে আলাদা কোনো UI file নয়, নিচে বর্ণিত app module-এর Settings ও সংশ্লিষ্ট configuration tab-গুলোকে বোঝান। GitHub repository-তে `app/module.toml` নামে একটি project configuration file আছে; সেটি CodeAssist UI-তে আলাদা file হিসেবে দেখানো হয়নি।

## 1. Modules screen

- একটি module card: `app`
- Module type: `Android Application`
- Card-এ vertical three-dot menu এবং details/navigation arrow রয়েছে।
- নিচের ডানদিকে `New module` action রয়েছে।
- Screenshot-এ `app` ছাড়া অন্য কোনো module তালিকাভুক্ত ছিল না।

## 2. app module — Settings tab

### 2.1 General

- Output: `app/build/classes`
- Java version selector: `8`, `11`, `17`, `21`; নির্বাচিত `17`
- Platform SDK: `Auto (android)`

### 2.2 Source sets

| Source set | Path | দৃশ্যমান roots |
|---|---|---|
| `androidTest` | `app/src/androidTest` | `java`, `kotlin`, `resources` |
| `debug` | `app/src/debug` | `aidl`, `assets`, `java`, `jniLibs`, `kotlin`, `res`, `resources` |
| `main` | `app/src/main` | `aidl`, `assets`, `java`, `jniLibs`, `kotlin`, `res`, `resources` |
| `release` | `app/src/release` | `aidl`, `assets`, `java`, `jniLibs`, `kotlin`, `res`, `resources` |
| `test` | `app/src/test` | `java`, `kotlin`, `resources` |

`Add source root` action-ও দৃশ্যমান।

### 2.3 Android configuration

| Setting | Screenshot value |
|---|---|
| Namespace | `com.teddy.bpositive` |
| Compile SDK | `36` |
| Min SDK | `26` |
| Target SDK | `36` |
| Manifest | `src/main/AndroidManifest.xml` |
| Version Code | `2` |
| Version Name | `1.0.1` |

Enabled toggles: `Non Transitive RClass`, `Is Application`, `Compose`।  
`Manifest Placeholders` ও `Build Config Fields` section-এ `Add...` action দেখা যায়।

### 2.4 Build Types

দুটি build type: `debug` এবং `release`।

**debug**
- Name: `debug`
- Debuggable: enabled
- Minify Enabled: disabled
- Shrink Resources: disabled

**release**
- Name: `release`
- Signing Config label: `teddywebapprelease`
- Debuggable: disabled
- Minify Enabled: disabled
- Shrink Resources: disabled
- Proguard Files: `proguard-android-optimize.txt`, `proguard-rules.pro`

`Add build type` action-ও দৃশ্যমান।

### 2.5 Add feature

- `Compose resources`: generated resources class ব্যবহার করে `composeResources/` থেকে strings, images ও fonts পড়ার feature; পাশে `Add` action।
- `Kotlin Multiplatform`: Android ও desktop target-সহ shared code-এর feature; পাশে `Add` action।

Screenshot-এ এগুলো যুক্ত করা হয়েছে এমন প্রমাণ নেই; শুধু Add option দৃশ্যমান।

## 3. Build Features tab

Tab-এর বর্ণনা: “Turn build features on per module. Enabling one adds the dependencies it needs.”

| Feature | Screenshot state | দৃশ্যমান কাজ |
|---|---|---|
| View Binding | Off | Layout অনুযায়ী type-safe binding class তৈরি করে; ViewBinding runtime যোগ করে |
| BuildConfig | On | `<namespace>.BuildConfig` তৈরি করে; DEBUG, APPLICATION_ID, VERSION_CODE, VERSION_NAME ও custom buildConfigFields অন্তর্ভুক্ত |

BuildConfig-এর custom fields `Settings` tab-এর Build Config Fields section, default config, build type বা flavor-এ যোগ করা যায় বলে UI-তে উল্লেখ আছে।

## 4. Compiler plugins tab

Tab-এর বর্ণনা অনুযায়ী compiler plugins build time-এ code transform করে; plugin চালু করলে তার runtime classpath-এ থাকলে স্বয়ংক্রিয়ভাবে প্রয়োগ হয়।

| Plugin | Screenshot state | UI-তে বর্ণিত কাজ |
|---|---|---|
| Jetpack Compose | On | `@Composable` function compile করে; Compose compiler plugin |
| Kotlin Serialization | Off | `@Serializable` class-এর serializer তৈরি করে |
| Parcelize | Off | `@Parcelize` class-এর Parcelable implementation তৈরি করে |
| Room | Off | Room-এর `@Database`/`@Dao` implementation তৈরি; KSP processor |
| Room 3 | Off | Room 3-এর `@Database`/`@Dao` implementation তৈরি; KSP processor |
| Moshi | Off | `@JsonClass(generateAdapter = true)` class-এর JSON adapter তৈরি; KSP processor |
| Hilt / Dagger | Off | Hilt/Dagger dependency-injection component তৈরি |
| Glide | Off | `@GlideModule` থেকে Glide API তৈরি; KSP processor |

Screenshot-এ Room 3, Moshi, Hilt/Dagger ও Glide-এর card পুনরাবৃত্ত অবস্থায়ও দেখা গেছে। একই নামের পুনরাবৃত্তি আলাদা plugin configuration কি না, screenshot থেকে নিশ্চিত হওয়া যায় না; তাই তালিকায় সেগুলোকে একবার করে বর্ণনা করা হয়েছে।

## 5. Packaging tab

Tab-এর বর্ণনা: একাধিক input (module, sub-module, AAR বা library) থেকে আসা Java resources ও native libraries APK-তে কীভাবে একত্র হবে তা নির্ধারণ করা যায়। Pattern-গুলো APK-relative glob; `**` folder অতিক্রম করে, `*` একটি folder-এর মধ্যে থাকে, leading `/` ঐচ্ছিক।

### 5.1 Java resources

দৃশ্যমান নিয়ন্ত্রণ:
- **Exclude:** path মিলে গেলে file বাদ দেয়।
- **Pick first:** প্রথম matching file রাখে, পরের duplicate বাদ দেয়।
- **Merge:** duplicate resource concatenate করে।

UI-তে default হিসেবে “Always excluded by default (24)” এবং “Always merged by default (1)” দেখা যায়।

### 5.2 Native libraries

- **Exclude:** matching native library বাদ দেয়।
- **Pick first:** duplicate থাকলে প্রথম matching file রাখে।
- UI-তে native libraries “packaged as-is (no NDK stripping on device)” বলে উল্লেখ আছে।

Screenshot-এ Native libraries section একাধিকবার দৃশ্যমান; অতিরিক্ত section-গুলোর সুনির্দিষ্ট পার্থক্য বোঝা যায়নি।

### 5.3 Compression

- **Don't compress:** নির্দিষ্ট suffix-এর file APK-তে uncompressed রাখার pattern যোগ করা যায়।
- বর্ণনায় `AssetManager.openFd()` ও memory-mapping-এর জন্য uncompressed file-এর প্রয়োজনীয়তার কথা বলা হয়েছে।
- “Never compressed by default (34)” তালিকা দৃশ্যমান।

Screenshot-এ কোনো custom Exclude, Pick first, Merge বা Don't compress pattern যোগ করা হয়েছে এমন প্রমাণ নেই।

## 6. Signing tab ও Keystore Manager

Signing tab-এর নির্দেশনা: প্রতিটি build type-এর জন্য keystore নির্বাচন করতে হয়; কোনো build type-এ keystore assign না থাকলে সেটি debug keystore দিয়ে sign হয়।

Screenshot-এ দেখা assignment:
- **debug:** `debug (default)` নির্বাচিত; `TeddyWebAppRelease` বিকল্প হিসেবে আছে।
- **release:** `TeddyWebAppRelease` নির্বাচিত; `debug (default)` বিকল্প হিসেবে আছে।

### 6.1 Keystore Manager

Manager-এ:
- `Create`, `Import` এবং `Assign to a build (project signing)` action রয়েছে।
- বিদ্যমান keystore card-এর নাম: `TeddyWebAppRelease`
- File label: `teddywebapprelease.jks`
- Key alias: `teddywebapp`
- Certificate subject text-এ Country `BD`, Organization `Teddy Universe`, CN `Teddy` দেখা যায়।
- Expiry আনুমানিক 2051 দেখানো হয়েছে।

Certificate-এর SHA fingerprints, password বা অন্য কোনো signing secret এই documentation-এ রাখা হয়নি।

### 6.2 New Keystore form

Create form-এ দৃশ্যমান field:
- Name (default text: `release`)
- Key alias (default text: `key0`)
- Password (কমপক্ষে ৬ অক্ষর; store ও key সুরক্ষার জন্য)
- Full name (CN)
- Organization (optional)
- Country code (optional)
- Validity (years; default `25`)
- `Create keystore` ও `Cancel` action

এটি form-এর field description; নতুন keystore তৈরি করার নির্দেশনা নয়। Existing production signing key-এর বদলে নতুন key ব্যবহার করলে update compatibility নষ্ট হতে পারে।

## 7. Dependencies tab

Dependencies screen-এ list/tree view control, refresh action, grid-style control এবং `+` action দেখা যায়। Dependency যোগ করার drawer-এ:
- Search field: `Search or paste group:name:version`
- Scope selector: default `implementation · All variants`
- Quick Add: Firebase (BoM + Analytics), Play Services Auth (Google Sign-In), Maps (Google Maps SDK), Location (Fused location provider)
- Other sources: Module এবং Local file (.jar/.aar)

### 7.1 Dependency list screenshot

প্রাথমিক তালিকায় দৃশ্যমান dependency:
- `kotlin-stdlib` (local jar)
- `androidx.activity:activity-compose:1.13.0`
- `androidx.compose.ui:ui:1.11.2`
- `androidx.compose.foundation:foundation:1.11.2`
- `androidx.compose.material3:material3:1.4.0`
- `androidx.compose.ui:ui-tooling-preview:1.11.2`
- `com.squareup.okhttp3:okhttp:4.12.0`
- `io.coil-kt:coil-compose:2.7.0`
- `com.google.firebase:firebase-messaging:26.0.2`

প্রথম তালিকার screenshot-এ “8 version conflicts to review” এবং “+40 auto-resolved” দেখা যায়।

### 7.2 Quick Add-এর পরের screenshot state

ব্যবহারকারী Firebase এবং Play Services Auth Quick Add-এ tap করার পরের screenshot-এ অতিরিক্তভাবে দেখা যায়:
- `com.google.firebase:firebase-bom:33.7.0`
- `com.google.firebase:firebase-analytics:22.1.2`
- `com.google.android.gms:play-services-auth:21.2.0`

সেই screenshot-এ conflict banner “12 version conflicts to review” এবং “+46 auto-resolved” দেখায়। Conflict banner নিজে থেকে Build failure প্রমাণ করে না; এটি version resolution review করার ইঙ্গিত।

### 7.3 Dependency usage cross-check ও cleanup recommendation

GitHub `main`-এ থাকা project source ও `app/module.toml` পরীক্ষা করে দেখা হয়েছে:
- `firebase-messaging:26.0.2` ঘোষিত dependency এবং app source-এ Firebase Messaging API ব্যবহৃত হয়। Push notification/token lifecycle-এর জন্য এটি প্রয়োজনীয়; **এটি সরানো যাবে না**।
- বর্তমান source-এ Firebase Analytics API ব্যবহারের প্রমাণ পাওয়া যায়নি।
- বর্তমান source-এ Google Sign-In / Play Services Auth API ব্যবহারের প্রমাণ পাওয়া যায়নি।
- বর্তমান repository dependency declaration-এ Firebase BOM নেই; Messaging dependency-র version সরাসরি নির্দিষ্ট করা আছে।

তাই screenshot-এ সদ্য Quick Add হওয়া `firebase-bom`, `firebase-analytics` ও `play-services-auth` ব্যবহার না করলে CodeAssist Dependency Manager থেকে সরিয়ে ফেলা যুক্তিযুক্ত। Cleanup করার সময় `firebase-messaging:26.0.2` অক্ষত রাখতে হবে। Removal-এর পর list screenshot ও Build verification ছাড়া cleanup সম্পন্ন বা Build successful বলা যাবে না।

এই সুপারিশ বর্তমান GitHub source ও সরবরাহ করা screenshot-এর ভিত্তিতে; ভবিষ্যতে Analytics বা Google Sign-In feature যোগ হলে প্রয়োজনীয় dependency আবার যোগ করা যেতে পারে।

## 8. Project-level context ও verification boundary

Screenshot-এ প্রদর্শিত Android configuration:
- Namespace: `com.teddy.bpositive`
- Java: `17`
- Compile/Target/Min SDK: `36 / 36 / 26`
- Version: `1.0.1 (versionCode 2)`
- Release signing assignment: `TeddyWebAppRelease`

এগুলো IDE settings-এর screenshot evidence; নিজে থেকে Gradle Build সফল হওয়া বা APK install/test হওয়ার প্রমাণ নয়। GitHub repository-র tracked `app/module.toml` এবং CodeAssist UI screenshot-কে পৃথক evidence হিসেবে বিবেচনা করতে হবে।


## 8. app module — Dependencies (পরবর্তী screenshot batch ও user cleanup)

### 8.1 Dependency Manager controls

Dependencies tab-এর Add drawer-এ:
- Search: `Search or paste group:name:version`
- Default scope: `implementation · All variants`
- Scope options: `implementation`, `api`, `compileOnly`, `runtimeOnly`, `testImplementation`, `natives`
- Variant options: `All variants`, `debug`, `release`
- Quick Add: Firebase (BoM + Analytics), Play Services Auth, Maps, Location
- Other sources: Module এবং Local file (.jar/.aar)

### 8.2 Dependency version conflict resolution

Build & Dependencies settings-এ “Dependency conflicts” selector দেখা যায়:
- `Newest` — screenshot-এ নির্বাচিত
- `Direct wins`
- `Fail on conflict`

Dependency list screenshot-এ conflict review banner ছিল; conflict count নিজে Build failure-এর প্রমাণ নয়।

### 8.3 User-reported cleanup

ব্যবহারকারী জানিয়েছেন, Quick Add থেকে ভুল করে যোগ হওয়া তিনটি dependency সরিয়ে দিয়েছেন। পরবর্তী dependency-list screenshot-এ আগের Quick Add-পরবর্তী তালিকার তুলনায় `firebase-bom`, `firebase-analytics` এবং `play-services-auth` আর দেখা যায়নি; `firebase-messaging:26.0.2` তালিকায় রয়ে গেছে। এটি screenshot ও user report-ভিত্তিক cleanup evidence, Build verification নয়।

বর্তমান app source-এর push notification implementation-এর জন্য Firebase Messaging প্রয়োজনীয়; এটি সরানো যাবে না। ভবিষ্যতে Analytics বা Google Sign-In feature ব্যবহার করলে সংশ্লিষ্ট dependency প্রয়োজন অনুযায়ী পুনরায় যোগ করতে হবে।

## 9. Settings menu — Build & Run

Settings menu-তে দৃশ্যমান category:
- Appearance & Editor
- Code Intelligence
- Build & Run
- Extensions
- System

এই batch-এ Build & Run-এর চারটি sub-screen দেখা গেছে।

### 9.1 Build & Dependencies

Dependency conflict strategy-এর বিকল্প ও নির্বাচিত অবস্থা §8.2-এ নথিভুক্ত।

### 9.2 Preview

**Preview Sandbox**-এ চারটি control screenshot-এ On:
- Block file access — preview code-এর file read/write বন্ধ করে; blocked call null ফেরায় এবং preview problem chip-এ তালিকাভুক্ত হয়।
- Block network access — socket ও HTTP connection বন্ধ করে।
- Block Android system calls — Activity/Service launch, broadcast, system service, ContentResolver ও SharedPreferences ব্যবহার বন্ধ করে; resource ও density read উপলভ্য থাকে।
- Block process & reflection — process execution, System.exit, native library loading ও reflective member invocation বন্ধ করে।

**Preview Process:**
- Render in a separate process — On।
- বর্ণনা অনুযায়ী @Preview আলাদা `:preview` OS process-এ render হয়; remote failure হলে in-process renderer fallback হতে পারে। Off করলে সব preview in-process render হয় এবং preview crash IDE-কে প্রভাবিত করতে পারে।

এগুলো screenshot-এ দৃশ্যমান IDE preference; app runtime security configuration নয়।

### 9.3 Build Runtime

Screenshot-এ দৃশ্যমান বর্তমান নির্বাচন:
- Build in a separate process — On।
- Build notifications — `Enable` action দৃশ্যমান; notification permission/setting enabled বলে screenshot প্রমাণ করে না।
- R8 execution — `Forked VM` নির্বাচিত; বিকল্প `In-process`।
- Low memory mode — `Auto` নির্বাচিত; বিকল্প `On`, `Off`।
- Download prebuilt libraries — `Automatically` নির্বাচিত; বিকল্প `Wi-Fi only`, `Off`।
- Forward app logs — On; বর্ণনা অনুযায়ী debug build-এর log stream IDE Logcat-এ পাঠায়, release build পরিবর্তন করে না।
- Kotlin compiler execution — `Compiler VM` নির্বাচিত; বিকল্প `In-process`।
- Kotlin compiler VM heap — 1536 MB।
- Advanced:
  - Off-heap dexing threshold — 8 MB।
  - Dex merge batch size — 6000 classes।
  - Max concurrent dex forks — 0।
  - Kotlin compiler VMs — 1।

UI-তে কিছু setting “next time you open a project” বা “next build” থেকে কার্যকর হবে বলা আছে। Screenshot state-কে Build performance বা successful Build-এর প্রমাণ হিসেবে ধরা যাবে না।

### 9.4 SDK Manager

**Android SDK platforms**:
- Android SDK Platform 37.2 — 64 MB, Latest।
- Android SDK Platform 36.1 — 63 MB, Your project।
- Android SDK Platform 36 — 62 MB, Your project।
- `Show all versions (18)` action।

**Android SDK sources**:
- Sources for Android 37.2 — 51 MB, Latest।
- Sources for Android 36.1 — 49 MB, Your project।
- Sources for Android 36 — 49 MB, Your project।
- `Show all versions (24)` action।

**JDK**:
- Device-এ Android Runtime (ART) চলে, JDK নয়; IDE bundled toolchain দিয়ে Java compile করে।
- JDK 17 sources ও JDK 21 sources download action দৃশ্যমান।

“Your project” label project-এর সঙ্গে সংশ্লিষ্ট version বোঝায়; screenshot থেকে সব listed platform/source ইতোমধ্যে installed—এমন সিদ্ধান্ত নেওয়া যায় না। Download arrow দৃশ্যমান থাকায় installation state আলাদাভাবে যাচাই করতে হবে।

## 10. Settings menu — System > Privacy & Data

Screenshot-এ দৃশ্যমান বর্তমান state:

### Ads
- Show ads — On।
- Full-screen ads — On; বর্ণনায় long builds চলাকালে এবং finished lessons-এর পরে occasional full-screen ads বলা হয়েছে।

### Privacy
- Share performance analytics — Off।
- বর্ণনা: anonymous usage counts, performance ও ad metrics share করে; code বা file names নয়।

### Storage
- Clear caches — dependency, language ও preview cache পরিষ্কার করে; source files নয়।
- View logs — recent editor, analysis ও build activity দেখায়।
- Back up projects — প্রতিটি project একটি ZIP ফাইলে export করে।

এই controls-এর screenshot state-ই নথিভুক্ত; কোনো cache clear, log open বা backup action সম্পন্ন হয়েছে বলে ধরে নেওয়া হয়নি।
