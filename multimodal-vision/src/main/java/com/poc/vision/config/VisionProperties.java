package com.poc.vision.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "vision")
public class VisionProperties {

    private Ollama ollama = new Ollama();
    private ComfyUI comfyui = new ComfyUI();
    private Storage storage = new Storage();

    public Ollama getOllama() { return ollama; }
    public void setOllama(Ollama ollama) { this.ollama = ollama; }
    public ComfyUI getComfyui() { return comfyui; }
    public void setComfyui(ComfyUI comfyui) { this.comfyui = comfyui; }
    public Storage getStorage() { return storage; }
    public void setStorage(Storage storage) { this.storage = storage; }

    public static class Ollama {
        private String url = "http://localhost:11434";
        private String model = "llava:7b";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    public static class ComfyUI {
        private String url = "http://127.0.0.1:8188";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class Storage {
        private String tempDir = System.getProperty("java.io.tmpdir") + "/vision-poc";
        private int maxAgeMinutes = 60;

        public String getTempDir() { return tempDir; }
        public void setTempDir(String tempDir) { this.tempDir = tempDir; }
        public int getMaxAgeMinutes() { return maxAgeMinutes; }
        public void setMaxAgeMinutes(int maxAgeMinutes) { this.maxAgeMinutes = maxAgeMinutes; }
    }
}
