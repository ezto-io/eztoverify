ARG PYTHON3_IMAGE_TAG=3.11.5-slim-bookworm

FROM python:${PYTHON3_IMAGE_TAG} as base

USER 0

WORKDIR /opt

RUN pip3 install --no-cache-dir --upgrade pip && \
    python3 -m venv liveliness

WORKDIR /opt/liveliness

COPY . .

USER 1001

FROM python:${PYTHON3_IMAGE_TAG}

USER 0

COPY --from=base --chown=1001:0 /opt/liveliness /opt/liveliness

# Pre-place whatever model weights are committed in models/. base.pt (Whisper) and
# vgg_face_weights.h5 (DeepFace VGG-Face) are NOT committed (large) — they are fetched
# at build time below via the libraries' own canonical URLs. Guards keep the build
# working whether or not a given file is present in the build context.
RUN mkdir -p /.cache/whisper /.deepface/weights && \
    for f in base.pt; do \
      [ -f "/opt/liveliness/models/$f" ] && mv "/opt/liveliness/models/$f" /.cache/whisper/ || true; \
    done && \
    for f in vgg_face_weights.h5 res10_300x300_ssd_iter_140000.caffemodel deploy.prototxt \
             2.7_80x80_MiniFASNetV2.pth 4_0_0_80x80_MiniFASNetV1SE.pth; do \
      [ -f "/opt/liveliness/models/$f" ] && mv "/opt/liveliness/models/$f" /.deepface/weights/ || true; \
    done

WORKDIR /opt/liveliness

ENV PYTHONUSERBASE /opt/liveliness/ 
ENV PATH ${PATH}:/opt/liveliness/bin
ENV FLASK_APP=app.py
ENV MPLCONFIGDIR /opt/liveliness/.config

USER 0

# NB: no `apt-get upgrade` — on this slim base it upgrades bash and update-alternatives
# fails on a stripped man-page path (bash-builtins.7.gz) → exit 100. The pinned base is
# patched enough for a dev image; just install the build/runtime deps we need.
RUN apt-get update -y && \
    apt-get install -y --no-install-recommends gcc make libgl1-mesa-dev g++ glib2.0 cmake ffmpeg && \
    apt-get clean && rm -rf /var/lib/apt/lists/* && \
    chown -R 1001:0 /opt/liveliness && \
    chmod -R u+rwX /opt/liveliness && \
    chmod -R g=u /opt/liveliness

# openai-whisper's sdist setup.py does `import pkg_resources`, which the latest
# setuptools (>=81) removed. pip's isolated PEP517 build env pulls the newest
# setuptools by default, so pin it for BOTH the main env and the build-isolation env
# (PIP_CONSTRAINT applies to build deps) to a version that still ships pkg_resources.
RUN printf 'setuptools<80\nwheel\n' > /tmp/pip-constraints.txt && \
    pip3 install --no-cache-dir --upgrade "setuptools<80" wheel && \
    PIP_CONSTRAINT=/tmp/pip-constraints.txt \
      pip3 install --no-cache-dir --use-pep517 --user -r requirements.txt

# Make ~ resolve to / so both this build step and the runtime app find the weights
# under /.cache/whisper and /.deepface (matches where they are placed/downloaded).
ENV HOME=/ DEEPFACE_HOME=/

# Fetch the weights not committed to the repo, using each library's own canonical URL.
# Whisper "base" -> /.cache/whisper/base.pt ; DeepFace VGG-Face -> /.deepface/weights/.
# Each step is skipped if the file was already pre-placed (dropped into models/).
RUN python3 - <<'PY' && \
    chown -R 1001:0 /.cache /.deepface && chmod -R g=u /.cache /.deepface
import os, whisper
from deepface import DeepFace
if not os.path.exists('/.cache/whisper/base.pt'):
    whisper._download(whisper._MODELS['base'], '/.cache/whisper', False)
if not os.path.exists('/.deepface/weights/vgg_face_weights.h5'):
    DeepFace.build_model('VGG-Face')
PY

USER 1001

EXPOSE 5000

CMD ["waitress-serve", "--host", "0.0.0.0", "--port", "5000", "app:app"]
