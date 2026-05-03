# Personal AI Assistant

A conversational AI assistant that can schedule calendar events, send emails, and manage tasks through natural language. Built with Spring Boot + Spring AI + Ollama (local LLM) + Google Calendar/Gmail APIs.

---

## Requirements

- Java 26+
- Maven 3.9+
- [Ollama](https://ollama.com) running locally with `qwen2.5:14b` pulled
- A Google account (free)

---

## Build and Run

```bash
# Pull the model if you haven't already
ollama pull qwen2.5:14b

# Build
cd personal-ai-assistant
mvn clean package -DskipTests

# Run
mvn spring-boot:run
```

Open `http://localhost:8080` in your browser.

Tasks work out of the box. For Calendar and Gmail, follow the Google setup below first.

---

## Google Calendar and Gmail Setup

### 1. Create a Google Cloud Project

1. Go to [console.cloud.google.com](https://console.cloud.google.com)
2. Click the project dropdown at the top → **New Project**
3. Give it a name (e.g. `personal-ai-assistant`) and click **Create**

### 2. Enable the APIs

1. In the left menu go to **APIs & Services** → **Library**
2. Search for **Google Calendar API** → click it → **Enable**
3. Go back to the Library, search for **Gmail API** → click it → **Enable**

### 3. Configure the OAuth Consent Screen

1. Go to **APIs & Services** → **OAuth consent screen**
2. Select **External** → **Create**
3. Fill in:
   - App name: `Personal AI Assistant`
   - User support email: your Gmail address
   - Developer contact email: your Gmail address
4. Click **Save and Continue** through the remaining steps (no scopes needed here)
5. On the **Test users** step, click **Add users** and add your Gmail address
6. Click **Save and Continue** → **Back to Dashboard**

### 4. Create OAuth 2.0 Credentials

1. Go to **APIs & Services** → **Credentials**
2. Click **+ Create Credentials** → **OAuth client ID**
3. Application type: **Desktop app**
4. Name: `personal-ai-assistant` → **Create**
5. Click **Download JSON** on the dialog that appears

### 5. Place the Credentials File

Rename the downloaded file to `credentials.json` and place it in the project root:

```
personal-ai-assistant/
    credentials.json   <-- here
    pom.xml
    src/
```

### 6. Authorize on First Run

Start the application with `mvn spring-boot:run`. On the first run:

1. A browser window will open automatically asking you to sign in with Google
2. Select your account and click **Allow**
3. The browser will show a success message and you can close it
4. The app stores the token in a `tokens/` folder — subsequent restarts skip this step

---

## Features

| Capability | Example prompts |
|---|---|
| Calendar | "What do I have this week?" / "Schedule a meeting with john@example.com tomorrow at 3pm" |
| Email | "Show my latest emails" / "Send an email to jane@example.com about the project update" |
| Tasks | "Add a task: review quarterly report, due Friday" / "Show my pending tasks" / "Mark task a1b2c3d4 as done" |

---

## Configuration

All settings are in `src/main/resources/application.properties`:

| Property | Default | Description |
|---|---|---|
| `spring.ai.ollama.chat.model` | `qwen2.5:14b` | Ollama model to use |
| `spring.ai.ollama.base-url` | `http://localhost:11434` | Ollama server URL |
| `google.credentials.file` | `credentials.json` | Path to your OAuth credentials file |
| `google.tokens.directory` | `tokens` | Where the OAuth token is stored after first login |
| `google.user.email` | `test@test.com` | Your Gmail address (used as the sender) |
