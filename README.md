# LearnEase AI

**An on-device RAG (Retrieval-Augmented Generation) study assistant for Android.**
Upload a PDF, and ask questions about it — answered by an LLM running directly on your phone.

---

## What it does

1. **Upload a PDF** — pick any PDF from your device.
2. **Extract & chunk** — text is pulled out page-by-page (via PdfBox-Android) and split into overlapping ~500-character chunks, breaking on sentence boundaries where possible.
3. **Embed** — each chunk is converted into a 768-dimension vector using Google's `gemini-embedding-001` API.
4. **Store** — chunks + their embeddings are saved locally in a Room database (SQLite), keyed by a SHA-256 hash of the file so the same PDF is never processed twice.
5. **Ask a question** — your question is embedded the same way, then compared against every stored chunk using cosine similarity to find the top-5 most relevant passages.
6. **Answer, on-device** — those passages are stitched into a prompt and handed to **Gemma**, running locally on the phone through Google's **LiteRT-LM** runtime. The answer streams back token-by-token.

## Current status

| Pipeline stage | Where it runs today |
|---|---|
| PDF text extraction | 📱 On-device |
| Chunking | 📱 On-device |
| **Embedding** | ☁️ **Cloud** (Gemini Embedding API) |
| Similarity search / retrieval | 📱 On-device |
| **Answer generation (LLM)** | 📱 **On-device** (Gemma via LiteRT-LM) |

**Goal: fully offline.** The LLM already runs locally — and the `feature/localEmbedding` branch now also runs embeddings on-device, closing the last cloud dependency. The fully offline pipeline (extraction → embedding → retrieval → generation) can now run with zero network access and zero data leaving the device.

## Architecture

```
PDF File
   │
   ▼
PdfExatractor (PdfBox-Android)  ──── page-by-page extraction, avoids OOM on large files
   │
   ▼
Chunker  ──── streaming, whitespace-normalizing, sentence-aware splitter
   │
   ▼
Embedding (Gemini API, cloud) ──── 768-dim vectors, rate-limit-aware with retry/backoff
   │
   ▼
Room DB (SQLite)  ──── DocumentEntity + ChunkEntity, embeddings stored as BLOBs
   │
   ▼
[ User asks a question ]
   │
   ▼
Query Embedding (Gemini API, RETRIEVAL_QUERY)
   │
   ▼
CosineSimilarity.rankTopK  ──── pure Kotlin, brute-force top-K search over stored chunks
   │
   ▼
PromptBuilder  ──── assembles a grounded, context-only system prompt
   │
   ▼
LocalLlmEngine (Gemma, LiteRT-LM) ──── on-device inference, streamed response
   │
   ▼
Chat UI (Jetpack Compose)
```

### Module layout

```
data/
 ├─ local/       Room database — DocumentEntity, ChunkEntity, DAOs, FloatArray↔ByteArray converters
 ├─ remote/      Retrofit + Gemini embedding API client
 └─ repository/  DocumentRepositoryImpl (ingestion pipeline), ChatRepository (RAG Q&A)
domain/
 ├─ Chunker            memory-efficient text chunking
 ├─ CosineSimilarity    vector similarity + top-K ranking
 ├─ PromptBuilder       grounded prompt construction
 └─ LocalLlmEngine      Gemma / LiteRT-LM session management
pdf/               PDF text extraction
ui/                Jetpack Compose screens: Upload, Detail, Chat — MVVM with Hilt-injected ViewModels
di/                Hilt modules (Database, Network, Application)
```

## Tech stack

- **Kotlin**, Jetpack Compose, Material 3
- **Hilt** for dependency injection
- **Room** for local persistence (documents, chunks, embeddings)
- **Retrofit + OkHttp** for the Gemini embedding API
- **PdfBox-Android** for PDF text extraction
- **LiteRT-LM** (`com.google.ai.edge.litertlm`) running **Gemma** for on-device generation
- **Coroutines + Flow** throughout, including token-streamed chat responses

## Setup

1. Clone the repo.
2. Add your Gemini API key to a `local.properties` file at the project root (used only for embeddings, not the LLM):
   ```
   GEMINI_API_KEY=your_key_here
   ```
3. Place a compatible `.litertlm` Gemma model file (e.g. `gemma-4-E2B-it.litertlm`) in `app/src/main/assets/`.
4. Build and run — minSdk 24, targetSdk 36.

## Why on-device LLM + cloud embedding (for now)?

Running the **generation** step on-device was prioritized first because it's the part most sensitive to privacy (your questions and the model's reasoning never need to leave the phone) and the part users interact with directly — instant, streaming answers with no API latency or per-token cost. Embedding was left on the cloud as an interim step since it's a one-time cost per document and off-the-shelf embedding APIs are currently more accurate and far lighter to integrate than shipping a second on-device model. The next step — replacing that with a local embedding model — closes the loop.

## On-device limitations & how they're being addressed

Moving the full pipeline on-device isn't just "swap the API call" — it runs into real constraints:

- **Model size vs. app size / RAM.** Embedding models and Gemma itself add tens to hundreds of MB, and both need to be resident in memory during use. `LocalLlmEngine` already handles this by lazily copying the model from assets to internal storage on first run and initializing it once (`Singleton`), rather than reloading it per request.
- **Inference speed on constrained hardware.** Phones vary wildly in NPU/GPU support; without hardware acceleration, generation and embedding both get slower and drain battery faster. This is why `LiteRT-LM` (built for edge inference) was chosen over a generic runtime — it's designed to exploit whatever acceleration a given device offers.
- **Cold-start latency.** Loading a multi-hundred-MB model file takes time. The app kicks off `localLlm.initialize()` in the background on `ViewModel` `init` rather than blocking the UI, so the model is warm by the time the user starts a chat.
- **Concurrent embedding without rate limits.** Cloud embedding still has to respect API rate limits — the ingestion pipeline processes chunks in small concurrent "waves" (default 3 at a time) with exponential backoff on `429` responses, which will map directly onto on-device batching once embeddings move locally (no more backoff needed, but batching for speed still matters).
- **Memory pressure from large PDFs.** Extracting a whole PDF into one string can OOM on large documents. `extractTextPageByPage` streams page-by-page instead of loading the full document text into memory at once.
- **Vector search at scale.** Cosine similarity here is brute-force over every stored chunk — fine for a handful of documents, but will need an on-device index (e.g. quantization or an ANN structure) once users accumulate many documents, since both retrieval speed and stored-vector size scale linearly today.

## Benefits of going fully offline

- **Privacy**: documents and questions never leave the device — no cloud logging, no third-party data handling.
- **No internet dependency**: usable anywhere, including exam halls, flights, or areas with poor connectivity — relevant for a study assistant.
- **No per-request API cost or key management** once the embedding step is local.
- **Lower latency** for the full pipeline, since there's no network round-trip on either embedding or generation.

## What Next ?

-  Implement the on-device embedding model — remove the last cloud dependency
-  Scalable embedding flow — handle growing document/chunk counts efficiently as the local index grows
-  Handle large PDFs more robustly (very high page counts, large file sizes)
- Support more input types beyond PDF (e.g. notes, other document formats)
-  Optimize app latency across the pipeline (extraction, embedding, retrieval, generation)

