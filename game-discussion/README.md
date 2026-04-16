# Game Design Discussion

A multi-AI debate system for game design ideas. Send a message and watch multiple AI participants respond simultaneously — each streaming their take in real time.

## How it works

- You type a question or pitch a game design idea
- All selected AI participants respond in parallel via SSE streaming
- Each response is attributed and color-coded by participant
- The full conversation history (including all AI responses) is passed as context on every new message — AIs can reference and challenge each other's points
- Toggle which participants respond per message using the pills in the header

## Participants

| Participant | Type | How it runs |
|---|---|---|
| QWEN 3.5 | Local LLM | Ollama HTTP API (`/api/chat`) |
| Claude | CLI | `claude -p "..."` via shell |
| Gemini | CLI | `gemini.cmd -p "..."` via shell |

## Prerequisites

- Java 26
- Maven
- [Ollama](https://ollama.com) running locally with `qwen3.5:9b` pulled
- [Claude Code](https://claude.ai/code) CLI installed (`claude` on PATH)
- [Gemini CLI](https://github.com/google-gemini/gemini-cli) installed via npm (`gemini.cmd` on PATH)

```bash
ollama pull qwen3.5:9b
```

## Run

```bash
cd game-discussion
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080)

## Configuration

All participants are configured in `src/main/resources/application.yml`:

```yaml
discussion:
  ollama:
    enabled: true
    model: qwen3.5:9b
    url: http://localhost:11434

  claude-cli:
    enabled: true
    command: claude        # use claude.cmd on Windows if needed

  gemini-cli:
    enabled: true
    command: gemini.cmd    # npm installs .cmd wrappers on Windows
```

Set `enabled: false` to disable any participant. CLI `command` must be the exact executable name found by `where <name>` on Windows or `which <name>` on Unix.

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/discussion/sessions` | Create a new session |
| `POST` | `/api/discussion/message` | Send a message (returns SSE stream) |
| `GET` | `/api/discussion/participants` | List configured participants |
| `GET` | `/api/discussion/sessions/{id}` | Get session history |

### SSE event format

Each `data:` line in the stream is a JSON object:

```json
{ "participantId": "claude", "participantName": "Claude", "type": "token", "content": "text chunk" }
{ "participantId": "claude", "participantName": "Claude", "type": "done",  "content": "" }
{ "participantId": "ollama", "participantName": "QWEN 2.5", "type": "error", "content": "Ollama error: ..." }
```

`type` is one of `token`, `done`, or `error`.
