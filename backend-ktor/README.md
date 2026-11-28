# KonteksCek Backend (Ktor)

Endpoint `/api/v1/analyze` untuk Prompt Coach. Server **stateless pada konten**:
teks prompt/jawaban/brief tidak disimpan dan tidak dicatat di log.

## Menjalankan lokal

```bash
# dari folder backend-ktor
../gradlew run
# atau dari root repo
./gradlew -p backend-ktor run
```

Default memakai `FakeLlmProvider` (tanpa API key) agar aplikasi bisa diuji.
Health check:

```bash
curl http://localhost:8080/health
```

Analisis contoh:

```bash
curl -X POST http://localhost:8080/api/v1/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "requestId":"demo-1",
    "depth":"FAST",
    "prompt":"buatin laporan tentang AI",
    "clientVersion":"2.0.0-beta1"
  }'
```

Emulator Android memakai `http://10.0.2.2:8080` (build debug mengizinkan cleartext).

## Memakai LLM sungguhan

Isi environment (jangan commit):

```bash
LLM_PROVIDER=openai
LLM_API_KEY=...
LLM_BASE_URL=https://api.openai.com/v1
LLM_MODEL_FAST=gpt-4o-mini
LLM_MODEL_DEEP=gpt-4o
LLM_JSON_MODE=true
PORT=8080
```

`LLM_BASE_URL` bisa diarahkan ke provider OpenAI-compatible lain (Groq,
OpenRouter, dsb). Nama model **wajib** dicek di halaman resmi provider.

## Batas & kuota (T-07a minimal)

- `MAX_DAILY_PER_CLIENT` (default 60) dan `MAX_DAILY_GLOBAL` (default 300).
- `ALLOW_PRO_FEATURES=false` menolak `depth=DEEP` dan `brief` (403 PRO_REQUIRED).
- Body maksimal ±300 KB; prompt ≤6.000, jawaban ≤12.000, brief ≤20.000 karakter.
- `requestId` idempoten selama 10 menit.
- Kuota dipotong hanya setelah analisis sukses.

## Deploy

```bash
docker build -t kontekscek-api ./backend-ktor
docker run --rm -p 8080:8080 --env-file .env kontekscek-api
```

Letakkan di belakang HTTPS reverse proxy (Caddy/Nginx/PaaS). Isi `X-Forwarded-For`
agar rate limit per klien bekerja. Isi URL publik hasil deploy ke aplikasi:
**Pengaturan → Server analisis**, atau build dengan
`-Pkontekscek.apiBaseUrl=https://api.domainmu.com`.

## Yang belum ada (roadmap T-07b/T-07c)

Auth perangkat/JWT, database kuota, billing Google Play, `/me`, `DELETE /me`,
dan panel admin kuota. Tanpa auth ini, jangan gunakan server untuk produksi publik.
