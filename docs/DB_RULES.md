# DB RULES — KonteksCek

Database: Room `kontekscek_local_db`, versi skema saat ini **2**.

## Aturan wajib

1. **Jangan** menaikkan `@Database(version = ...)` tanpa `Migration` eksplisit.
2. **Dilarang** memakai `fallbackToDestructiveMigration(...)` polos. Gunakan varian `From`/`OnDowngrade` hanya untuk
   versi yang memang sudah tidak didukung.
3. Setiap perubahan skema:
   - ubah entity/DAO,
   - naikkan versi,
   - tambahkan `Migration(n, n+1)` yang SQL-nya disalin dari `schemas/.../n+1.json`,
   - tambahkan instrumentation test migrasi (`MigrationTestHelper`),
   - commit file schema JSON.
4. Nama file database **tidak boleh berubah**: `kontekscek_local_db`.
5. Nama tabel/kolom lama **tidak boleh berubah**: `context_cards`, `verification_logs`, dan semua kolomnya.
6. `exportSchema = true` wajib dipertahankan. Lokasi schema: `app/schemas`.

## Schema 2 (baseline)

Dibuat oleh T-01 tanpa mengubah versi. File: `app/schemas/com.aldointelligence.kontekscek.data.local.KonteksDatabase/2.json`.
Berisi `context_cards` dan `verification_logs` persis seperti entity saat ini. Schema ini menjadi dasar test migrasi
ketika nanti (Jalur B/T-11) ditambahkan tabel `analyses` di versi 3.
