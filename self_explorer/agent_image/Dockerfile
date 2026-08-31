# syntax=docker/dockerfile:1@sha256:ecfaec9ed6d810b56388c508f4121597bfbba70d41a6dfeee4d8cad5f295fc32
# Deliberately tiny, untrusted workshop for the self-built-tool mode.
# IMPORTANT: this directory is the entire Docker build context. Repository
# source, the managed MCP server, SDK and Controller cannot be copied in.
FROM debian:bookworm-slim@sha256:88200866dfff7ea7f5cbcb6ec7c8a701889efe6fe859fe64d6990e4b07ea4171

ENV DEBIAN_FRONTEND=noninteractive PYTHONDONTWRITEBYTECODE=1

RUN apt-get update && apt-get install -y --no-install-recommends \
      bash coreutils findutils file \
      python3 gcc g++ make libc6-dev \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --uid 1000 explorer \
    && mkdir -p /workspace \
    && chown explorer:explorer /workspace

COPY entrypoint.sh /entrypoint.sh
RUN chmod 0555 /entrypoint.sh

USER explorer
WORKDIR /workspace
ENTRYPOINT ["/entrypoint.sh"]
