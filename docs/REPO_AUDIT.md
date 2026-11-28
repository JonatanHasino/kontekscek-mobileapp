# REPO AUDIT — KonteksCek v2.1

Tanggal audit: 28 Nov 2026. Basis: kode aktual di `C:\Users\jonat\AndroidStudioProjects\KonteksCek`.

## Versi & konfigurasi aktual

| Item | Nilai aktual |
|---|---|
| Gradle wrapper | 9.5.0 |
| AGP | 9.3.3 (via `gradle/libs.versions.toml`) |
| Kotlin | 2.1.10 |
| KSP | 2.1.10-1.0.31 |
| Compose BOM | 2025.01.00 (dideklarasikan langsung di `app/build.gradle.kts`) |
| compileSdk / targetSdk | 35 / 35 |
| minSdk | 26 |
| Java target | 17 |
| Room | 2.8.5 (KSP) |
| Versi aplikasi sebelum rework | `versionCode 2`, `versionName 1.1.0` |
| Backend | `backend-ktor/Application.kt` saja; **tidak ada build script/wrapper/modul Gradle**, belum termasuk `settings.gradle.kts` |
| CI | `.github/workflows/android.yml` (artifact + GitHub Release pada tag `v*`) |
| Font | Plus Jakarta Sans + IBM Plex Mono (7 file TTF) |
| Schemas Room | **belum ada** sebelum T-01 |

## Status build & test (sebelum rework)

- `./gradlew :app:assembleDebug` → **SUCCESS**
- `./gradlew :app:testDebugUnitTest` → **SUCCESS** (7 test)
- APK debug pernah diuji di emulator: tema terang/gelap, navigasi tab, simpan PDF, backup JSON.

## Koreksi terhadap asumsi dokumen

1. Dokumen menyebut Compose BOM `2025.01.00` — **benar** untuk `app/build.gradle.kts`, walaupun `gradle/libs.versions.toml`
   mencantumkan versi yang lebih baru dan tidak dipakai modul app untuk Compose. Untuk rework, dependensi app tetap
   memakai BOM 2025.01.00 agar tidak mengubah baseline.
2. Dokumen menyebut `navigation-compose` terpasang tetapi tidak dipakai — **benar**.
3. Dokumen menyebut backend Ktor tidak terlihat di dump — sebagian benar: hanya ada `Application.kt`, tanpa build script.
   Backend **belum dapat dijalankan** dari repo ini tanpa membuat modul Gradle baru.
4. `versionCode`/`versionName` rework dinaikkan ke `3` / `2.0.0-beta1` (sesuai §7.2).
5. Folder sumber masih `com/example/kontekscek` untuk semua file lama; file baru rework ditaruh di
   `com/aldointelligence/kontekscek` sesuai AGENTS.md.
6. `PdfExtractorTest.kt` memang berisi kelas `VerificationEngineUnitTest` dan memakai refleksi ke method private.
7. `KonteksViewModel.purgeAllData()` hanya menghapus log; kartu, cache, dan preferensi belum dihapus.
8. `seedSampleCardIfEmpty()` masih aktif.
9. Klaim "100% lokal" masih ada di Dashboard, Settings, top bar, dan footer PDF.
10. `allowBackup=false` sudah benar; `backup_rules.xml`/`data_extraction_rules.xml` tidak direferensikan.

## Batasan sesi rework ini

- Jalur A diterapkan: UI Prompt Coach, Privacy Guard, linter lokal, klien HTTP, share/process text, klaim jujur,
  consent, purge lengkap. **Skema Room tidak diubah** (hanya `exportSchema=true` + schema 2).
- Riwayat analisis penuh (Room v3) = T-11 (Jalur B, belum dikerjakan).
- Backend produksi + LLM key = T-07a (kode backend disiapkan terpisah; deploy butuh env & hosting).
- Billing/kuota/auth (T-14/T-15), InputExtractor lanjutan (PDF/gambar/DOCX), dan NavHost penuh = Jalur B.
