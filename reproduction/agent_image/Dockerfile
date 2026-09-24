# syntax=docker/dockerfile:1@sha256:ecfaec9ed6d810b56388c508f4121597bfbba70d41a6dfeee4d8cad5f295fc32
# Generic, networkless website implementation workbench.  It contains no
# reference app, exploration runtime, benchmark SDK, or target-specific code.
FROM debian:bookworm-slim@sha256:88200866dfff7ea7f5cbcb6ec7c8a701889efe6fe859fe64d6990e4b07ea4171

ENV DEBIAN_FRONTEND=noninteractive PYTHONUNBUFFERED=1

RUN set -eu; \
    attempt=1; \
    while true; do \
      if apt-get -o Acquire::Retries=5 update \
        && apt-get -o Acquire::Retries=5 install -y --no-install-recommends \
          ca-certificates; then \
        break; \
      fi; \
      if [ "$attempt" -ge 3 ]; then exit 1; fi; \
      attempt=$((attempt + 1)); \
      sleep 2; \
    done; \
    rm -rf /var/lib/apt/lists/*; \
    sed -i 's|http://deb.debian.org|https://deb.debian.org|g' /etc/apt/sources.list.d/debian.sources

RUN set -eu; \
    attempt=1; \
    while true; do \
      if apt-get -o Acquire::Retries=5 update \
        && apt-get -o Acquire::Retries=5 install -y --no-install-recommends \
          bash coreutils findutils file diffutils \
          python3 python3-pip python3-venv sqlite3; then \
        break; \
      fi; \
      if [ "$attempt" -ge 3 ]; then exit 1; fi; \
      attempt=$((attempt + 1)); \
      sleep 2; \
    done; \
    rm -rf /var/lib/apt/lists/*; \
    python3 -m venv /opt/repro-venv; \
    /opt/repro-venv/bin/pip install --no-cache-dir --retries 5 \
      fastapi==0.115.12 uvicorn==0.34.2 pydantic==2.11.4 \
      jinja2==3.1.6 pillow==11.2.1; \
    useradd --create-home --uid 1000 reproducer; \
    mkdir -p /workspace /input /materials /exploration; \
    chown reproducer:reproducer /workspace /input /materials /exploration

ENV PATH="/opt/repro-venv/bin:${PATH}"

COPY entrypoint.sh /entrypoint.sh
RUN chmod 0555 /entrypoint.sh

USER reproducer
WORKDIR /workspace
ENTRYPOINT ["/entrypoint.sh"]
