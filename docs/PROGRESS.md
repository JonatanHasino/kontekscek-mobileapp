# PROGRESS — Rework v2.1 (Prompt Coach)

Update: 28 Nov 2026. Basis: `docs/REWORK_V2.md`.

## Selesai (Jalur A)

| Task | Status | Bukti / catatan |
|---|---|---|
| T-00 Audit repo | ✅ | `docs/REPO_AUDIT.md`; versi Kotlin 2.1.10, AGP 9.3.3, Gradle 9.5.0; build+test hijau |
| T-01 Room safety net | ✅ | `exportSchema=true` + `ksp room.schemaLocation`; `app/schemas/.../2.json`; `docs/DB_RULES.md`; versi DB tetap 2 |
| T-03a Konfigurasi | ✅ | plugin serialization, OkHttp, BuildConfig `API_BASE_URL`, network security config, `values-night`, share/process-text intent |
| T-04 PrivacyGuard | ✅ | `domain/guard/PrivacyGuard.kt`; 22 unit test (termasuk “animasi” ≠ NIM, JWT/ghp_/AKIA/private key/email/HP/kartu Luhn/NIK) |
| T-05 LocalPromptLinter | ✅ | `domain/lint/LocalPromptLinter.kt`; 18 unit test; offline fallback |
| T-06 DTO + AnalyzeClient | ✅ | `data/remote/**`; MockWebServer test: 200 valid, field hilang/enum asing, 429, 500, BAD_RESPONSE, timeout, schema baru |
| T-07a Backend minimal | ✅ (kode) | `backend-ktor` Ktor 3, `/health` + `/api/v1/analyze`, `FakeLlmProvider`, `OpenAiCompatibleProvider`, system prompt, validasi, rate limit, idempoten; **belum di-deploy publik** |
| T-08 CheckViewModel + Check/Result + nav | ✅ | 3 tab (Cek/Riwayat/Lainnya), consent, privacy preview, loading, offline, quota, error, input panjang, klarifikasi, highlight evidence |
| T-09 Share Sheet & Process Text | ✅ | `ACTION_SEND` + `ACTION_PROCESS_TEXT`, `singleTop`, `onNewIntent`, dialog 2-chip, parser chat export |
| T-10 Klaim & kebersihan | ✅ | klaim diperbaiki, consent + tarik consent, purge kartu+log+cache+prefs analisis, seed sample dihapus, PDF footer diperbarui |

## Verifikasi

- `./gradlew :app:testDebugUnitTest` → **66 test, 0 gagal**
- `./gradlew :app:assembleDebug` → **APK berhasil**
- `backend-ktor: ../gradlew build` → **berhasil** (JDK 21)
- Uji emulator: local lint, remote AI (Fake provider via `10.0.2.2:8080`), consent,
  privacy preview email, share intent, score delta, quota, dark/light theme.
- `git grep -i "100% lokal"` → tidak ada untuk fitur Cek AI.
- `git grep "fallbackToDestructiveMigration("` → tidak ada varian polos.

## Belum (Jalur B)

- T-02 pindahkan folder `com/example` → `com/aldointelligence` (sengaja ditunda sesuai spec).
- T-11 Room v3 `analyses`, migration 2→3, riwayat analisis penuh, backup v2 berisi analisis.
- T-12 InputExtractor lanjutan (PDF/gambar/DOCX, progress, batas memori).
- T-13 NavHost/back stack, pecah ViewModel lama.
- T-14 auth/kuota klien, T-15 billing, T-16 PDF Jejak Prompt, T-17 insights/reminder.
- Deploy backend HTTPS + secret LLM + kebijakan privasi publik + Data Safety Play.
