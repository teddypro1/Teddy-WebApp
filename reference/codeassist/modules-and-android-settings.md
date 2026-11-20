# CodeAssist — Modules ও Android Module Settings

সর্বশেষ screenshot batch: ২০২৬-১১-২০  
পরিবেশ: CodeAssist IDE 3.46.0  
প্রকল্প: Teddy-WebApp (`com.teddy.bpositive`)

এই নথি ব্যবহারকারীর সরবরাহ করা screenshot থেকে দৃশ্যমান তথ্য সংরক্ষণ করে। পরবর্তী Build Features, Compiler Plugins ও Packaging screenshot না পাওয়া পর্যন্ত সেসব অংশের বিবরণ এখানে যোগ করা হবে না।

## 1. Modules screen

Modules screen-এ screenshot অনুযায়ী:

- একটি module card দেখা যায়: `app`।
- Module type: `Android Application`।
- Card-এ vertical three-dot menu এবং ডানদিকে details/navigation arrow রয়েছে।
- নিচের ডানদিকে `New module` action রয়েছে।
- Screenshot-এ `app` ছাড়া অন্য কোনো module তালিকাভুক্ত ছিল না।

## 2. app module — Settings tab

`app` module খুললে উপরে চারটি tab দেখা যায়:

1. `Settings`
2. `Build Features`
3. `Compiler plugins`
4. `Packaging`

বর্তমান screenshot-এ `Settings` tab নির্বাচিত। বাকি তিনটি tab-এর বিস্তারিত screenshot এখনো পাওয়া যায়নি।

### 2.1 General

- Output: `app/build/classes`
- Java version selector: `8`, `11`, `17`, `21`।
- Screenshot-এ `17` নির্বাচিত।
- Platform SDK: `Auto (android)`।

### 2.2 Source sets

নিচের source set-গুলো দেখা যায়:

| Source set | Path | দৃশ্যমান roots |
|---|---|---|
| `androidTest` | `app/src/androidTest` | `java`, `kotlin`, `resources` |
| `debug` | `app/src/debug` | `aidl`, `assets`, `java`, `jniLibs`, `kotlin`, `res`, `resources` |
| `main` | `app/src/main` | `aidl`, `assets`, `java`, `jniLibs`, `kotlin`, `res`, `resources` |
| `release` | `app/src/release` | `aidl`, `assets`, `java`, `jniLibs`, `kotlin`, `res`, `resources` |
| `test` | `app/src/test` | `java`, `kotlin`, `resources` |

`Add source root` action-ও দৃশ্যমান।

### 2.3 Android configuration

Screenshot-এ দেখা মানগুলো:

| Setting | Displayed value |
|---|---|
| Namespace | `com.teddy.bpositive` |
| Compile SDK | `36` |
| Min SDK | `26` |
| Target SDK | `36` |
| Manifest | `src/main/AndroidManifest.xml` |
| Version Code | `2` |
| Version Name | `1.0.1` |

নিচের toggle-গুলো screenshot-এ enabled অবস্থায় দেখা যায়:

- `Non Transitive RClass`
- `Is Application`
- `Compose`

এছাড়াও `Manifest Placeholders` এবং `Build Config Fields` section-এ `Add...` action দেখা যায়।

### 2.4 Build Types

দুটি build type দেখা যায়: `debug` এবং `release`।

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
- Proguard Files:
  - `proguard-android-optimize.txt`
  - `proguard-rules.pro`

এখানে কেবল signing configuration-এর নাম নথিভুক্ত করা হয়েছে। কোনো password, keystore secret বা credential সংরক্ষণ করা হয়নি।

`Add build type` action-ও দেখা যায়।

### 2.5 Add feature

Settings screen-এর নিচের অংশে:

- `Compose resources` — description-এ generated resources class ব্যবহার করে `composeResources/` থেকে strings, images ও fonts পড়ার কথা বলা হয়েছে; পাশে `Add` action।
- `Kotlin Multiplatform` — description-এ Android ও desktop target-সহ shared code-এর কথা বলা হয়েছে; পাশে `Add` action।

Screenshot-এ এই feature-গুলো যোগ করা হয়েছে এমন প্রমাণ নেই; শুধু Add option দৃশ্যমান।

## 3. Project-level context

Screenshot-এ প্রদর্শিত Android configuration বর্তমান project context-এ আগে থেকে নথিভুক্ত তথ্যের সঙ্গে সামঞ্জস্যপূর্ণ:

- Namespace: `com.teddy.bpositive`
- Java: `17`
- Compile/Target/Min SDK: `36 / 36 / 26`
- Version: `1.0.1 (versionCode 2)`

এটি IDE settings-এর screenshot evidence; এটি নিজে থেকে Gradle Build সফল হওয়া বা APK install/test হওয়ার প্রমাণ নয়।

## 4. পরবর্তী screenshot batch

এখনো অপেক্ষমাণ:

- `Build Features` tab
- `Compiler plugins` tab
- `Packaging` tab
- `app` module-এর three-dot menu, যদি ব্যবহারকারী সেটির screenshot দেন

নতুন screenshot পেলে এই reference-এ সংশ্লিষ্ট section যোগ করতে হবে। Screenshot-এ অনুপস্থিত option বা আচরণ অনুমান করে লেখা যাবে না।
