# Liveness Detection Service

A Python Flask microservice for face-based biometric liveness detection. Provides multiple detection methods to verify that a live person is present during identity verification.

## Features

- Face recognition and comparison
- Blink detection
- Movement detection
- Finger count detection
- Speech recognition (via OpenAI Whisper)
- Anti-spoofing detection
- MinIO object storage integration

## Tech Stack

- Python 3.10+ with Flask
- MediaPipe (face/hand detection)
- DeepFace (face embedding and comparison)
- OpenCV (video processing)
- OpenAI Whisper (speech recognition)
- TensorFlow/Keras (deep learning)
- MinIO (cloud storage)

## Prerequisites

- Python 3.10+
- pip
- FFmpeg (for video processing)

## Setup

### Local Development

```bash
pip3 install -r requirements.txt
python3 app.py
```

### Docker

```bash
docker build --tag liveliness .
docker run -p 8000:5000 liveliness
```

### As part of the full stack

In the root `docker-compose.yml` this service is **opt-in** — it sits behind the `face` compose
profile `face-biometrics`, so the default `docker compose up` skips it (it's the heaviest image to
build and the IdP doesn't depend on it). Start it only when you need face-liveness flows:

```bash
docker compose --profile face-biometrics up -d --build
```

> ⚠️ **First build is slow** — native libs (ffmpeg, OpenCV/dlib build deps) plus large ML model
> weights (DeepFace, Whisper) mean the initial build can take **20+ minutes** (longer on arm64 /
> emulated builds). Cached afterwards.

### Dev mode (against the shared dev infra)

Start the backing infra once from the repo root, then run this service on the host (needs `ffmpeg`
+ Python 3.10+ locally):

```bash
docker compose -f docker-compose.dev.yml up -d        # minio on localhost:9000
cp .env.example .env                                  # then set the values below
```

`.env` pointing at the dev MinIO:

```bash
PORT=5000
MINIO_HOST=localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET=minioadmin123
FACE_TOLERANCE=0.68
```

```bash
./start.sh    # creates venv, installs requirements, runs waitress on :5000
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `MINIO_HOST` | MinIO object storage hostname |
| `MINIO_ACCESS_KEY` | MinIO access key |
| `MINIO_SECRET` | MinIO secret key |
| `FACE_TOLERANCE` | Face comparison tolerance (default: 0.68) |

## Notes

- `mediapipe==0.8.9.1` may not work on all environments. Use `mediapipe==0.10.1` as an alternative.

## License

Apache License 2.0 - see [LICENSE](./LICENSE) for details.
