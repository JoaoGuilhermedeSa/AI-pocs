# Content Moderation API

AI-powered content moderation for text, images, and video using Spring AI + Ollama + faster-whisper.

## Requirements

### Runtime
- **Java 26** with `--enable-preview`
- **Maven 3.9+**
- **Ollama** running locally on `http://localhost:11434`
- **FFmpeg** on PATH (`winget install ffmpeg`)
- **Python 3.8+** with `faster-whisper` (`pip install faster-whisper`)

### Ollama Models
```cmd
ollama pull qwen2.5:7b     # text moderation
ollama pull llava:7b       # image & video frame moderation
```

## Run

```cmd
cd content-moderation
mvn clean package -DskipTests
mvn spring-boot:run
```

Open **http://localhost:8084**

## API

| Method | Endpoint | Body |
|--------|----------|------|
| POST | `/api/moderation/text` | `{ "text": "..." }` |
| POST | `/api/moderation/image` | multipart `file` (JPEG, PNG, WebP) |
| POST | `/api/moderation/video` | multipart `file` (MP4, MKV, WebM, AVI) |

## Response

```json
{
  "verdict": "SAFE",
  "categories": ["NONE"],
  "severity": "NONE",
  "confidence": 0.97,
  "explanation": "No harmful content detected."
}
```

**Verdict:** `SAFE` | `UNSAFE`  
**Categories:** `HATE_SPEECH` | `VIOLENCE` | `ADULT` | `SPAM` | `SELF_HARM` | `HARASSMENT` | `NONE`  
**Severity:** `NONE` | `LOW` | `MEDIUM` | `HIGH`

Video responses also include `visualResult`, `audioResult`, and `framesAnalyzed`.

## Configuration

Edit `src/main/resources/application.yml` to change models or FFmpeg/Whisper settings:

```yaml
moderation:
  models:
    text: qwen2.5:7b
    vision: llava:7b
  ffmpeg:
    path: ffmpeg
    fps: 1
    max-frames: 1000
  whisper:
    script: transcribe.py
    model: tiny        # tiny | base | small | medium | large-v3
    device: cuda       # cuda | cpu
```
