# 🛡️ KonteksCek — Pemeriksa Kesesuaian Konteks & Pencatat Jejak Verifikasi AI

> **"Pakai AI boleh. Pastikan cocok konteks, dan simpan jejaknya dalam 5 menit."**

**KonteksCek** adalah aplikasi Android modern berbasis **Kotlin & Jetpack Compose** yang berfungsi sebagai *governance layer* (lapisan tata kelola dan verifikasi berbasis aturan) untuk membantu mahasiswa, *freelancer* muda, dan pekerja dalam memastikan hasil dari AI generatif sesuai dengan konteks *brief* serta dapat dipertanggungjawabkan secara etis.

Proyek ini dikembangkan oleh kelompok **3SEMA — AI (Aldo Intelligence)**, Program Studi Teknologi Informasi, Universitas Internasional Batam (UIB) pada mata kuliah *Perintisan Usaha Startup*.

---

## 🎯 Latar Belakang & Masalah yang Diselesaikan

Berdasarkan riset lapangan (wawancara, observasi, dan *immersion*) serta data sekunder, penggunaan AI dalam aktivitas akademik dan profesional menghadapi tiga hambatan utama:
1. **Ketidakkonsistenan Konteks:** Output AI sering melenceng dari *brief* proyek atau rubrik tugas ketika sesi berganti dan konteks tidak dijaga.
2. **Verifikasi Manual Tanpa Jejak:** Pengguna sudah melakukan pengecekan ulang (*debugging*, cek sumber, revisi), tetapi memakan waktu ekstra dan tidak meninggalkan catatan bukti proses bagi dosen, atasan, maupun klien.
3. **Risiko Privasi & Etika:** Mengunggah dokumen klien atau data rahasia ke layanan pengolah bahasa pihak ketiga berisiko melanggar kerahasiaan data (NDA / UU PDP).

Berbeda dengan pendeteksi tulisan AI (seperti Turnitin/GPTZero) yang hanya menebak *"apakah teks ini dibuat AI?"*, **KonteksCek** berfokus pada *"apakah hasil AI ini sudah sesuai konteks tugas dan diverifikasi oleh manusia?"*.

---

## 👥 Target Pengguna & Konsumen

### Pengguna Utama (*Users*)
Individu berusia **≥ 18 tahun** yang rutin memanfaatkan AI untuk belajar maupun bekerja:
* **Mahasiswa Aktif:** Mengerjakan makalah, laporan, riset, dan tugas *coding* yang memerlukan validasi sitasi serta kepatuhan etika akademik.
* **Freelancer Muda (UI/UX, Programmer, Content Creator):** Mengerjakan proyek klien dengan tuntutan akurasi *brief* dan kerahasiaan data tinggi.
* **Mahasiswa Magang & Pekerja Muda:** Menyusun laporan kerja, pengolahan data, dan tugas teknis yang membutuhkan akuntabilitas sumber kepada atasan.

### Segmen Konsumen (*Customers*)
* **B2C (Freemium & Pro):** Mahasiswa dan *freelancer* individu (Gratis untuk fitur inti; Paket Pro untuk templat lanjutan & ekspor PDF tanpa *watermark*).
* **B2B (Lisensi Tim/Institusi):** Program studi, laboratorium kampus, UKM/komunitas TI, dan agensi kecil untuk standardisasi literasi AI yang bertanggung jawab.

---

## ✨ Fitur Utama (Full CRUD & Local-First Privacy)

* **📇 CRUD Kartu Konteks (5 Isian):** Menyimpan *Tujuan*, *Audiens/Standar Mutu*, *Batas Etika & Sumber*, *Format/Bahasa*, dan *Data Terlarang* dalam satu kartu. Dilengkapi templat cepat (*Laporan, Kode, Desain, Konten*), pencarian, filter mode, duplikasi kartu, serta tombol **Salin Kartu 1 Klik** untuk ditempel ke *prompt* AI.
* **🔍 Pemindai Berbasis Aturan (*Rule-Based Engine*):** Mendeteksi potensi kebocoran data terlarang, pola kredensial sensitif, angka/klaim statistik tanpa rujukan sumber, serta sisa teks mentah AI secara **100% lokal di perangkat** tanpa mengirim teks ke server eksternal.
* **✅ Daftar Periksa Per Mode (≤ 5 Butir):** Panduan verifikasi terstruktur yang menyesuaikan standar tiap konteks: mode **Kuliah**, **Kerja**, dan **Klien** dengan status *Belum / Sudah / Diubah*, progress bar, dan reset cepat.
* **⏱️ Stopwatch & Catatan Jejak 3 Tahap:** Memantau durasi sesi (target **≤ 5 menit** per tugas) serta mendokumentasikan ringkasan proses **Diminta → Diperiksa → Diubah** dan kesimpulan verifikator.
* **📄 Ekspor & Bagikan PDF:** Menghasilkan dokumen ringkasan jejak verifikasi siap kirim ke dosen, atasan, atau klien langsung dari perangkat (*client-side PDF generation*), lengkap dengan pernyataan etika dan kontrol penghapusan data sesuai **UU PDP**.
* **📊 Dashboard Aktivitas:** Ringkasan jejak hari ini, total sesi, rata-rata durasi, isu tertangkap, grafik aktivitas 7 hari, dan *streak* kedisiplinan verifikasi.
* **🎨 Design System & Tema:** Token warna Terang/Gelap/Sistem, ukuran teks Normal/Besar yang tersimpan di perangkat, tipografi **Plus Jakarta Sans** & **IBM Plex Mono**, top bar dengan status privasi, serta komponen reusable (kartu, badge, banner, segmented control, empty/error state, skeleton).
* **💾 Cadangan Data Lokal:** Ekspor & impor seluruh Kartu Konteks + Jejak Verifikasi dalam satu file JSON, tanpa server.


---

## 🛠️ Arsitektur & Teknologi

* **Frontend Mobile:** Kotlin, Jetpack Compose, Material Design 3, Material Icons Extended, Navigation Compose.
* **Design System:** Token warna light/dark, tipografi Plus Jakarta Sans & IBM Plex Mono, komponen reusable — diadaptasi dari design system aplikasi **TokoJoJo Stock** (`tokojojo-cloud-pos-demo/apps/android`).
* **Backend Lokal (*Offline-First*):** Room Database (SQLite) untuk operasi CRUD Kartu Konteks & Riwayat Jejak, `RuleVerificationEngine` (Regex & Heuristic Analyzer), serta Android `PdfDocument` API.
* **Preferensi & Cadangan:** SharedPreferences untuk tema/ukuran teks/mode default, plus ekspor-impor JSON lokal.
* **Backend Server (Opsional):** Kotlin Ktor REST API untuk sinkronisasi templat *checklist* tim/Pro dan telemetri statistik anonim (tanpa menyimpan teks tugas pengguna).

---

## 🔄 Adopsi dari TokoJoJo Stock (`apps/android`)

KonteksCek dan TokoJoJo Stock sama-sama offline-first, tetapi basis kodenya berbeda. Yang diadopsi ke KonteksCek:

| Aspek | TokoJoJo Stock | Diterapkan di KonteksCek |
| :--- | :--- | :--- |
| Design token | `TokoJojoColors` + `LocalTokoColors` | `KonteksColors` + `LocalKonteksColors` (Terang/Gelap) |
| Tipografi | Plus Jakarta Sans + IBM Plex Mono | Font & skala teks yang sama, plus opsi Ukuran Besar |
| Komponen | `TokoCard`, `SectionHeader`, `InfoBanner`, `StatCard`, `SegmentedControl`, `SettingsRow`, `EmptyState`, `SkeletonBlock` | `KonteksCard`, `SectionHeader`, `InfoBanner`, `StatTile`, `SegmentedControl`, `SettingsRow`, `EmptyState`, `SkeletonBlock` |
| Tema | `ThemeMode` / `TextSize` + SharedPreferences | Idem, ditambah mode default & stopwatch otomatis |
| Shell aplikasi | Top bar + chip status + bottom bar | `KonteksTopBar` + chip "Lokal" + 5 tab (Beranda/Kartu/Cek AI/Jejak/Lainnya) |
| Layar pengaturan | `MoreScreen` | `SettingsScreen` (tampilan, verifikasi, data, privasi, tentang) |

Yang **tidak** diadopsi karena tidak relevan dengan domain verifikasi AI: Apollo GraphQL, Metro DI, Navigation3, kamera/ML Kit barcode, serta modul stok/gudang.


---

## 🚀 Cara Menjalankan Proyek

1. **Clone repositori ini:**
   ```bash
   git clone https://github.com/username-kamu/KonteksCek.git
   ```
2. **Buka di Android Studio** (versi Koala / Ladybug atau lebih baru dengan JDK 17).
3. Tunggu proses **Gradle Sync** selesai mengunduh dependensi Jetpack Compose dan Room.
4. Jalankan aplikasi pada Emulator Android (API 26+) atau perangkat fisik melalui tombol **Run (`Shift + F10`)**.

---

## 👨‍💻 Tim Pengembang — 3SEMA "AI (Aldo Intelligence)"

| Nama Anggota | NIM | Peran Utama |
| :--- | :---: | :--- |
| **Jonatan** | 2532035 | Product Lead, Arsitektur UI/UX & Dokumentasi |
| **Kevin Li** | 2532042 | User Research & Basis Aturan Mode Kuliah/Klien |
| **Vincent Yongky Pratama** | 2532039 | Tech & Ops, Mode Kerja & Sistem Ekspor PDF |
| **Aldo Kristian** | 2532015 | Growth, Validasi Pengguna & Metrik Pengujian |

---

> *Catatan Etika: PDF yang dihasilkan KonteksCek mencatat proses pemeriksaan manusia terhadap kesesuaian konteks, bukan jaminan kebenaran absolut atas keluaran AI.*