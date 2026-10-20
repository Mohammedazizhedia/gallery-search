# Gallery Search

Offline gallery search app (Android, Kotlin, Jetpack Compose). Arabic and English follow the system language.

## Build the APK on GitHub
1. Create a new GitHub repository and upload everything in this folder (keep the `.github` folder).
2. Open the **Actions** tab, pick **Build APK**, then **Run workflow** (it also runs on every push to `main`).
3. When it finishes, open the run and download the artifact **GallerySearch-debug-apk**, unzip it, and install `app-debug.apk`.

## Stages
1. Project, theme, bilingual UI, gallery grid (done)
2. Duplicate and corrupted-file scan with progress (done)
3. CLIP smart search, Room index, WorkManager
4. Review screen, safe delete with the app's own restorable trash (done, test build: one file per delete)
5. Glance widgets
6. Offline voice search
7. Optional online AI module (free keys first, then paid)
