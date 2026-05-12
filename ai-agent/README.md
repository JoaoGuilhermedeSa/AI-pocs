# AI Agent

An autonomous AI agent that browses the web, extracts information, and compiles research reports. Uses SearXNG for private, tracker-free search and JSoup for page extraction. Includes safety guardrails to prevent runaway execution.

---

## Requirements

- Java 26+
- Maven 3.9+
- Docker (for SearXNG)
- [Ollama](https://ollama.com) running locally with a model pulled

---

## Setup

### 1. Pull a model

```bash
ollama pull qwen2.5:7b
```

### 2. Start SearXNG

SearXNG requires a config file to enable its JSON API. The file is already included at `searxng/settings.yml`. Run the container with it mounted:

```powershell
docker run -d -p 8081:8080 --name searxng `
  -v "C:\path\to\ai-agent\searxng\settings.yml:/etc/searxng/settings.yml" `
  searxng/searxng
```

Replace `C:\path\to\ai-agent` with the actual path on your machine.

SearXNG is a self-hosted metasearch engine. It queries Google, Bing, DuckDuckGo and others on your behalf with no tracking and no API keys required. It will be available at `http://localhost:8081`.

Verify it is working before starting the app:
```
http://localhost:8081/search?q=test&format=json
```
This should return a JSON object. If it returns 403, the settings file was not picked up — stop and recreate the container with the volume mount above.

To stop it later: `docker stop searxng`
To start it again: `docker start searxng`

### 3. Build and run

```bash
cd ai-agent
mvn clean package -DskipTests
mvn spring-boot:run
```

Open `http://localhost:8080`.

---

## Usage

Give the agent a research task in plain language. It will autonomously search the web, read relevant pages, take notes, and compile an answer with cited sources.

Example tasks:
- "What are the latest developments in quantum computing?"
- "Compare the top JavaScript frameworks in 2025"
- "What is the current price of gold and why is it moving?"
- "Summarize recent news about space exploration"

For complex tasks the agent also saves a structured markdown report to the `reports/` folder.

---

## Safety Guardrails

| Guardrail | Default | Description |
|---|---|---|
| Max iterations | 20 | Hard cap on total tool calls per request |
| Max web fetches | 15/min | Rate limit on page downloads |
| URL blocklist | — | Comma-separated domains to block (configurable) |
| Private IP block | Always on | Blocks localhost, 127.x, 10.x, 192.168.x, 172.16-31.x |
| Protocol filter | Always on | Only http/https allowed |

---

## Configuration

All settings in `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `spring.ai.ollama.chat.model` | `qwen2.5:7b` | Ollama model |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | Ollama server URL |
| `agent.searxng.url` | `http://localhost:8081` | SearXNG instance URL |
| `agent.searxng.max-results` | `5` | Search results returned per query |
| `agent.safety.max-iterations` | `20` | Max tool calls per request |
| `agent.safety.max-fetch-per-minute` | `15` | Max page downloads per minute |
| `agent.safety.url-blocklist` | — | Comma-separated blocked domains |
| `agent.web.max-content-chars` | `10000` | Max characters extracted per page |
| `agent.web.timeout-seconds` | `10` | HTTP timeout for page fetches |
