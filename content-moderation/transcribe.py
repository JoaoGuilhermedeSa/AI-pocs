"""
Transcribes an audio file using faster-whisper and prints the transcript to stdout.
Usage: python transcribe.py <audio_path> [model_size] [device]
"""
import sys
from faster_whisper import WhisperModel

audio_path = sys.argv[1]
model_size = sys.argv[2] if len(sys.argv) > 2 else "tiny"
device     = sys.argv[3] if len(sys.argv) > 3 else "cuda"

compute_type = "float16" if device == "cuda" else "int8"

model = WhisperModel(model_size, device=device, compute_type=compute_type)
segments, _ = model.transcribe(audio_path, beam_size=5)

for segment in segments:
    print(segment.text, end="", flush=True)
