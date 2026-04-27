# Multimodal Vision

A multi-modal AI POC with two capabilities:

- **Analyze Image** — upload any image and ask questions about it. Powered by `llava:7b` running locally via Ollama.
- **Generate Image** — describe an image in text and generate it using FLUX.1-dev running locally via ComfyUI.

Nothing leaves your machine. No API keys, no cloud services.

## Prerequisites

- Java 26
- Maven
- [Ollama](https://ollama.com) running with `llava:7b` pulled:
  ```bash
  ollama pull llava:7b
  ```
- [ComfyUI](https://github.com/comfyanonymous/ComfyUI) (portable) running at `http://127.0.0.1:8188` with the following models:
  ```
  models/unet/   → flux1-dev-fp8.safetensors
  models/clip/   → t5xxl_fp8_e4m3fn.safetensors
                 → clip_l.safetensors
  models/vae/    → ae.safetensors
  ```

## Run

```bash
cd multimodal-vision
mvn spring-boot:run
```

Open [http://localhost:8082](http://localhost:8082)

## Configuration

`src/main/resources/application.yml`:

```yaml
vision:
  ollama:
    url: "http://localhost:11434"
    model: "llava:7b"
  comfyui:
    url: "http://127.0.0.1:8188"
```

The FLUX workflow is defined in `src/main/resources/flux-workflow.json` and can be edited directly to change model names, sampler settings, or add nodes without recompiling.
