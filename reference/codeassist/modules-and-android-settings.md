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
