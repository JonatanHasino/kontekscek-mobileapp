# AGENTS.md — KonteksCek

Baca `docs/REWORK_V2.md` (v2.1) dan `docs/REPO_AUDIT.md` sebelum bekerja. Kerjakan HANYA task `T-xx` yang diminta.

Aturan keras:

- DILARANG mengubah `version` Room atau skema sebelum T-01 selesai. DILARANG memakai `fallbackToDestructiveMigration(...)` polos.
  Tiap perubahan skema = Migration + test dari `schemas/*.json`. Nama DB tetap `kontekscek_local_db`.
- Jangan ubah `applicationId`/`namespace` `com.aldointelligence.kontekscek`, FileProvider authority, nama kolom/tabel lama,
  nama file & key SharedPreferences lama (`kontekscek_preferences`), atau field entity (dipakai Gson untuk backup v1).
- File Kotlin BARU taruh di folder `src/**/java/com/aldointelligence/kontekscek/` (folder lama `com/example` baru dipindah di T-02).
- Jangan menambah fitur ke `KonteksViewModel` (dibekukan). Buat ViewModel baru.
- Jangan menaruh API key/secret di repo, BuildConfig, APK, atau log. Secret hanya env var server.
- Backend tidak boleh menyimpan/mencatat isi prompt/jawaban/file.
- `kotlinx.serialization` untuk kode baru; Gson hanya untuk `BackupManager` & `parseChecklist` lama (jangan campur pada class yang sama).
- Versi library baru harus kompatibel dengan versi Kotlin/AGP/Compose yang tercatat di `docs/REPO_AUDIT.md`. Jika ragu, berhenti dan tanya.
- Jangan menulis string UI yang menyatakan "100% lokal" untuk fitur Cek AI. Semua string UI Bahasa Indonesia, kalimat pendek.
- Tiap task: tulis test, jalankan `./gradlew testDebugUnitTest assembleDebug` sampai hijau, tampilkan hasilnya, lalu berhenti.
- Jika spesifikasi bertabrakan dengan kode nyata: JANGAN menebak. Catat di `docs/REPO_AUDIT.md` bagian "Koreksi" dan minta konfirmasi.
- Jangan menyatakan fitur selesai jika AC belum terpenuhi.
