# CodeAssist — Git ও Source Control

সর্বশেষ screenshot batch: ২০২৬-১১-২০  
পরিবেশ: CodeAssist IDE 3.46.0  
Repository: `teddypro1/Teddy-WebApp`  
ব্যবহারকারীর নির্ধারিত workflow: শুধু `main` branch।

এই নথি ব্যবহারকারীর সরবরাহ করা CodeAssist screenshot এবং তার ব্যাখ্যার ভিত্তিতে তৈরি। এটি IDE-এর সব সংস্করণের সাধারণ নির্দেশিকা নয়; এখানে কেবল দেখা UI ও নিশ্চিত তথ্য লেখা হয়েছে।

## 1. Source Control panel

প্রধান Source Control view-তে দেখা যায়:

- বর্তমান branch selector: `main`
- `Pull` এবং `Push` action
- Sync status: `Up to date`
- Clean working tree হলে check mark, `Nothing to commit`, এবং “Your working tree matches the last commit.”
- Commit form-এ `Commit message` field, `Amend last commit` checkbox এবং `Push after committing` checkbox রয়েছে।
- `Push after committing` সক্রিয় থাকলে commit করার পর remote-এ push করার নির্দেশনা UI-তে দেখানো হয়।
- কোনো ফাইল stage না থাকলে UI-তে `Stage a file to commit it.` বার্তা দেখা যায়।

Screenshot batch-এ commit short SHA `67d3089` দেখানো হয়েছিল। সেই সময়কার GitHub main history-র commit ছিল `67d3089856858efff228b25bfd8810c893441946`।

## 2. Branches screen

Branch selector খুললে Branches screen-এ:

- উপরে branch search field এবং নতুন branch তৈরির `+` action।
- `LOCAL` এবং `REMOTE` আলাদা তালিকায় দেখানো হয়।
- Current branch-এর পাশে check mark এবং `Current` লেখা থাকে।
- কোনো branch-এর পাশের তিন-ডট menu থেকে screenshot-এ পাওয়া action:
  - `Switch to this branch`
  - `Merge into main`
  - কিছু branch menu-তে `Delete branch)

### গুরুত্বপূর্ণ: stale/local branch entries

একটি screenshot-এ IDE-তে পুরোনো local ও remote-tracking entry দেখা গিয়েছিল, যেমন:

- `feature/in-app-update-build15`
- `fix/production-update-metadata-url`
- `release/v1.0.1-build20`
- সংশ্লিষ্ট `origin/*` entries

পরবর্তী screenshot-এ local list ছোট হয়ে `main` এবং `release/v1.0.1-build20` দেখায়। ব্যবহারকারী স্পষ্ট করেছেন যে প্রকৃত GitHub repository-তে কেবল `main` branch আছে; IDE-তে অন্য entries দেখা stale/local UI state বা cache inconsistency হতে পারে।

সেই screenshot batch-এর সময় live GitHub branch search-ও শুধু `main` ফিরিয়েছিল। তাই:

- IDE-এর পুরোনো branch entries-কে live GitHub branch ধরে নেওয়া যাবে না।
- ব্যবহারকারীর অনুমতি ছাড়া পুরোনো entry switch, merge বা delete করা যাবে না।
- নতুন feature/develop/release branch তৈরি করা যাবে না।
- সব ভবিষ্যৎ পরিবর্তন সরাসরি `main`-এ করতে হবে।
- কোনো কাজের আগে live GitHub branch list ও main HEAD যাচাই করতে হবে।

## 3. Repository three-dot menu

Branch selector-এর পাশে repository menu-তে screenshot-এ এই বিভাগ ও item দেখা গেছে।

### Repository

- **History** — অতীত commit browse করা।
- **Stashes** — সাময়িকভাবে পরিবর্তন stash করা।
- **GitHub** — GitHub account, publishing এবং pull request-সংক্রান্ত view।

### Sync

- **Fetch** — remote-এ নতুন commit আছে কি না পরীক্ষা করে; বর্ণনা অনুযায়ী working files পরিবর্তন করে না।
- **Refresh** — ফাইলের status নতুন করে পড়ে।

### Other

- **Add default .gitignore entries** — default `.gitignore` entry যোগ করার action।

## 4. History

History screen-এ commit-এর message, author, short SHA, সময় এবং সংশ্লিষ্ট branch label দেখা যায়।

Screenshot-এ সর্বশেষ দৃশ্যমান entry:

- Message: `Record Build 21 successful Build and test confirmation`
- Author: `teddypro1`
- Short SHA: `67d3089`
- Labels: `main`, `origin/main`

এর নিচে Build 21 confirmation, documentation organization এবং update-check implementation-সংক্রান্ত আগের commit-ও দৃশ্যমান ছিল। Screenshot-টি history list-এর আংশিক অংশ; সম্পূর্ণ commit history নয়।

## 5. Stashes

Stashes screen-এ:

- `Stash changes` section
- ঐচ্ছিক `Description` field
- `Include untracked files` checkbox
- `Stash changes` action

Screenshot-এ `Include untracked files` checkbox নির্বাচিত ছিল এবং নিচে `Nothing stashed.` দেখানো হয়েছিল। কোনো stash operation করা হয়নি।

## 6. GitHub integration

GitHub screen-এ screenshot অনুযায়ী:

- Account: `teddypro1`
- Pull requests section: `No open pull requests.`
- New pull request form: source হিসেবে `main` দেখানো।
- Remotes section:
  - Remote name: `origin`
  - URL: `https://github.com/teddypro1/Teddy-WebApp.git`

### Sign-in warning সম্পর্কে ব্যবহারকারীর ব্যাখ্যা

GitHub panel-এ saved sign-in আর গ্রহণযোগ্য নয়—এমন warning এবং `Sign in again` action দেখা গিয়েছিল। ব্যবহারকারী জানিয়েছেন, GitHub account ইতিমধ্যে Git operations-এর জন্য signed in এবং warning-টির কারণে কোনো কার্যকর সমস্যা হচ্ছে না।

অতএব:
- warning-টিকে নিজে থেকে blocker হিসেবে ধরা যাবে না।
- Git operation বাস্তবে ব্যর্থ না হলে অকারণে পুনরায় sign-in করতে বলা যাবে না।

## 7. GitHub Manage > Accounts

Manage-এ প্রবেশের পর Accounts screen-এ:

- `teddypro1` account দেখানো হয়েছে।
- Account status text: `Used for Git operations`
- `Sign in with GitHub` option
- Personal Access Token দিয়ে sign-in করার option
- Git identity-এর name ও email field
- Other Git servers-এর জন্য server, username এবং password/token field

Screenshot-এ ব্যক্তিগত email address দেখা গেলেও তা এই documentation-এ সংরক্ষণ করা হয়নি। কোনো token, password বা credential-ও সংরক্ষণ করা যাবে না।

## 8. Push status-এর screenshot evidence

একটি screenshot-এ `Pushing` label ও progress indicator দেখা যায়। পরের screenshot-এ `Pushed` confirmation দেখা যায়, এরপর panel-এ `Up to date` এবং `Nothing to commit` অবস্থা দেখা যায়।

এটি ওই মুহূর্তে push সম্পন্ন হয়ে clean working tree-তে ফেরার সঙ্গে সামঞ্জস্যপূর্ণ। Screenshot-এ push চলাকালে কোনো confirmation/error dialog দেখা যায়নি।

এটি Build success বা app runtime test-এর প্রমাণ নয়।

## 9. নিরাপদ কাজের নিয়ম

1. Source change-এর আগে live GitHub branch list যাচাই করো।
2. বর্তমান `main` HEAD এবং পরিবর্তনযোগ্য প্রতিটি ফাইলের বর্তমান SHA সংগ্রহ করো।
3. IDE-এর cached branch/status-কে একমাত্র source of truth হিসেবে ব্যবহার করো না।
4. একবারে একটি focused change-set করো এবং সরাসরি `main`-এ কাজ করো।
5. Commit/push-এর পর GitHub-এ commit ও file state যাচাই করো।
6. ব্যবহারকারী নিজে Build ও test করে নিশ্চিত না করা পর্যন্ত Build সফল বলবে না।
7. Production database, paid plan, release publication বা destructive action-এর জন্য প্রয়োজনীয় স্পষ্ট অনুমতি ছাড়া এগোবে না।

---

## পরবর্তী screenshot batch

এই ফাইলে আপাতত Source Control panel-এর screenshot-এ দেখা অংশ নথিভুক্ত হয়েছে। File Explorer, editor, search/navigation, project settings বা অন্য IDE section-এর screenshot এলে সেগুলো আলাদা reference ফাইলে যোগ করতে হবে। অদেখা menu বা control-এর আচরণ অনুমান করে লেখা যাবে না।
