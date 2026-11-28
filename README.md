# 🛡️ KonteksCek — Prompt Coach

<p align="center">
  <img src="design/logo-preview.png" width="180" alt="Logo KonteksCek — bubble chat dengan centang" />
</p>

> **Prompt-mu dicek. Dibenerin. Hasil AI lebih tepat.**
>
> Tempel/bagikan prompt + jawaban AI → dapatkan diagnosis, prompt perbaikan, dan cek kesesuaian jawaban.

**KonteksCek v2.0** adalah aplikasi Android (Kotlin + Jetpack Compose) untuk membantu mahasiswa,
freelancer, dan pekerja memakai AI dengan lebih sadar konteks. Alur utama bukan lagi formulir
administrasi, melainkan **Prompt Coach**: masuk dengan prompt yang sudah ada, keluar dengan prompt
yang lebih jelas dan jawaban yang sudah dicek.

Dikembangkan oleh **3SEMA “AI” (Aldo Intelligence)** — Program Studi Teknologi Informasi,
Universitas Internasional Batam (UIB).

---

## ✨ Yang baru di v2

- **Cek & Perbaiki dalam satu layar:** tempel prompt (dan opsional jawaban AI), lalu dapat skor 0–100,
  daftar masalah, dan prompt perbaikan siap salin/bagikan.
- **Privacy Guard lokal:** API key, private key, JWT, password, email, nomor HP, kartu (Luhn), NIK,
  dan kata terlarang disensor **di perangkat sebelum** teks dikirim. Pratinjau sensor selalu ditampilkan.
- **Persetujuan eksplisit:** pengguna melihat apa yang dikirim, ke mana, dan bisa menarik persetujuan
  kapan saja di Pengaturan.
- **Lint lokal instan (offline):** skor, masalah, prompt perbaikan, dan cek jawaban tetap jalan tanpa
  server. Berguna saat offline atau saat backend belum diatur.
- **Share Sheet & Process Text:** pilih teks di ChatGPT/Gemini/Chrome/WhatsApp → “KonteksCek”.
  Parser chat export memisahkan prompt dan jawaban otomatis (dengan konfirmasi).
- **Cek jawaban AI:** “Menjawab: Ya/Sebagian/Tidak”, daftar ketidaksesuaian, dan prompt tindak lanjut.
- **Requirement coverage:** lampirkan brief/persyaratan (maks. 15 baris) untuk melihat mana yang
  terpenuhi/sebagian/belum (lint lokal atau server Pro).
- **Riwayat analisis terakhir** + arsip jejak lama, ekspor PDF jejak, cadangan/impor JSON.
- **Tema Terang/Gelap/Sistem**, ukuran teks Besar, edge-to-edge, aksesibilitas touch target ≥48dp.
- **Mode Manual lama tetap tersedia** (lokal penuh): Kartu Konteks, checklist, stopwatch, PDF jejak.

> Catatan klaim: analisis AI mengirim teks **yang sudah disensor** ke server KonteksCek dan penyedia
> model AI sebagai pemroses. Server tidak menyimpan isi teks. Hanya Mode Manual & lint lokal yang
> berjalan sepenuhnya di perangkat. Tidak ada klaim “lolos deteksi AI” atau jaminan kebenaran.

---

## 🧱 Arsitektur

```
Share / Process Text / Tempel
        │
        ▼
InputExtractor + ChatExportParser (lokal)
        │
        ▼
PrivacyGuard (lokal, sensor before send)
        │
        ├── ada temuan → Pratinjau → user setuju
        ▼
AnalyzeClient (OkHttp + kotlinx.serialization)
        │  POST /api/v1/analyze (HTTPS, requestId)
        ▼
Ktor backend ──► LlmProvider (OpenAI-compatible / Fake)
        │         system prompt + JSON schema ketat
        ▼
Hasil: skor, isu, prompt perbaikan, output check, coverage
        │
        ├── simpan hasil terakhir (SharedPreferences, opsional)
        └── offline/error → LocalPromptLinter (fallback)
```

- **Android:** Kotlin, Compose, Room (data lama), OkHttp, kotlinx.serialization, ML Kit (ekstraksi
  lokal), SharedPreferences (`kontekscek_preferences`).
- **Backend:** Ktor + kotlinx.serialization, stateless pada konten, rate limit & kuota harian,
  `requestId` idempoten, provider LLM di balik interface `LlmProvider`.
- **Tidak ada API key** di APK/repo/BuildConfig/log. Secret hanya environment server.
- **Room v2 tidak diubah** pada rework ini (hanya `exportSchema=true`). Riwayat analisis penuh
  (Room v3 + migration) adalah roadmap Jalur B.

---

## 🚀 Menjalankan Android

1. Buka folder ini di Android Studio (JDK 17, Android SDK 35).
2. Sync Gradle, lalu Run ke emulator/perangkat **Android 8.0 (API 26)+**.
3. Tanpa server, aplikasi memakai **lint lokal**. Untuk analisis AI:
   - jalankan backend (lihat bawah), lalu
   - **Pengaturan → Server analisis** → isi `http://10.0.2.2:8080` (emulator) atau URL HTTPS backend.
   - Atau build dengan `-Pkontekscek.apiBaseUrl=https://api.domainmu.com`.

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🖥️ Menjalankan backend

```bash
cd backend-ktor
../gradlew run          # FakeLlmProvider, tanpa API key
```

Atau dari root: `./gradlew -p backend-ktor run`.

```bash
curl http://localhost:8080/health
```

Untuk LLM sungguhan, isi env (jangan commit):

```bash
LLM_PROVIDER=openai
LLM_API_KEY=...
LLM_BASE_URL=https://api.openai.com/v1
LLM_MODEL_FAST=gpt-4o-mini
LLM_MODEL_DEEP=gpt-4o
```

Detail kontrak, kuota, batas input, dan deploy Docker ada di
[`backend-ktor/README.md`](backend-ktor/README.md).

---

## 🤖 CI & Rilis APK

[![Android CI](https://github.com/JonatanHasino/kontekscek-mobileapp/actions/workflows/android.yml/badge.svg)](https://github.com/JonatanHasino/kontekscek-mobileapp/actions/workflows/android.yml)

Workflow `.github/workflows/android.yml`:

- push/PR ke `main` → unit test + `assembleDebug` + build backend.
- Setiap run menyimpan artifact **`kontekscek-apk`** (30 hari).
- Push tag `v*` → **GitHub Release** otomatis berisi APK + `SHA256SUMS.txt`.
- Tag yang mengandung `beta`/`rc` ditandai **pre-release**.

Rilis versi baru:

```bash
# 1. ubah versionCode & versionName di app/build.gradle.kts
git add . && git commit -m "feat: ..."
git push origin main

git tag v2.0.0-beta3     # atau v2.0.0 untuk stabil
git push origin v2.0.0-beta3
```

APK release memakai **debug key** untuk sideloading (bukan Play Store), seperti build demo.
URL server bisa diatur lewat repository variable **`KONTEKSCEK_API_BASE_URL`** di GitHub
(Settings → Secrets and variables → Actions → Variables) sebelum build APK rilis.

Halaman rilis: https://github.com/JonatanHasino/kontekscek-mobileapp/releases

---

## 🗂️ Struktur penting

```
app/src/main/java/com/aldointelligence/kontekscek/
  data/remote/           AnalyzeClient, DTO, mapper toleran
  data/AiPreferences.kt  consent, server URL, draft, last analysis
  domain/guard/          PrivacyGuard (sensor lokal)
  domain/lint/           LocalPromptLinter (fallback offline)
  domain/input/          ChatExportParser
  domain/model/          AnalysisResult, Issue, OutputCheck, dll.
  ui/screens/CheckScreen.kt, ResultScreen.kt   ← alur Prompt Coach
  ui/viewmodel/CheckViewModel.kt               ← pipeline + state
backend-ktor/            Ktor /api/v1/analyze + LlmProvider
docs/REWORK_V2.md        spesifikasi rework lengkap
docs/REPO_AUDIT.md       audit kode & koreksi
docs/DB_RULES.md         aturan Room/migration
AGENTS.md                aturan kerja untuk kontributor/agent
```

Mode lama (Beranda, Preset Konteks, Cek Manual, Arsip Jejak) tetap ada dan dapat dibuka dari
**Pengaturan → Mode & data lama**. `KonteksViewModel` lama dibekukan; fitur baru ada di
`CheckViewModel`.

---

## 🧪 Testing

- 66+ unit test: PrivacyGuard (≥20 kasus), LocalPromptLinter (≥15), ChatExportParser,
  AnalyzeClient (MockWebServer), mapper DTO, defaults/backup.
- Migration Room & share intent instrumented test: roadmap Jalur B.
- Backend diuji dengan `FakeLlmProvider` + `curl` (`/health`, validasi, kuota, idempoten).

---

## 🗺️ Roadmap (Jalur B)

- T-02 pindahkan folder sumber ke `com/aldointelligence` + rapikan tes.
- T-11 Room v3 `analyses` + migration test + riwayat analisis penuh + backup v2.
- T-12 `InputExtractor` PDF/gambar/DOCX dengan progres & batas ukuran.
- T-13 pecah ViewModel + NavHost/back stack penuh.
- T-14 auth perangkat, kuota server, `/me`, `DELETE /me`.
- T-15 billing Google Play + PaywallSheet (tanpa fitur Pro palsu).
- T-16 PDF Jejak Prompt (sebelum/sesudah) + watermark Free.
- T-17 insights, streak, pengingat opt-in, telemetri opt-in.
- T-18 golden set evaluasi LLM, T-19 hardening Play Store.

---

## 👨‍💻 Tim

| Nama | NIM | Peran |
| :--- | :---: | :--- |
| **Jonatan** | 2532035 | Product Lead, Arsitektur UI/UX & Dokumentasi |
| **Kevin Li** | 2532042 | User Research & Basis Aturan |
| **Vincent Yongky Pratama** | 2532039 | Tech & Ops, Mode Kerja & Ekspor PDF |
| **Aldo Kristian** | 2532015 | Growth, Validasi Pengguna & Metrik |

> Saran dari aplikasi dibuat otomatis dan bisa salah. Pengguna tetap pengambil keputusan.
> Aplikasi ini bukan pendeteksi tulisan AI dan tidak menjamin kebenaran absolut.
