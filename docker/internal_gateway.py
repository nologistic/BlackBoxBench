#!/usr/bin/env python3
"""Internal reverse gateway for the reference container.

Sits between the kiosk browser and the reference app:

    chromium --host-resolver-rules="MAP reference-app.internal 127.0.0.1, ..."
        -> http://reference-app.internal:8200   (this gateway, loopback only)
        -> http://127.0.0.1:8100                (reference app, +X-BBB-Gateway)

Responsibilities (docs/api_contract.md §4, docs/security_model.md T3):
- Inject `X-BBB-Gateway: <secret>` on every forwarded request so the app
  accepts it (the app 404s any request without the header).
- Scrub fingerprinting response headers (`Server`, `X-Powered-By`) so no
  implementation detail ever reaches the rendered pixels (threat T5).

Standard library only — the reference image installs no extra dependency for
this component. It proxies byte streams without inspecting semantics; it is
NOT reachable from outside the container (binds 127.0.0.1).

NOTE: statically reviewed only — not executed on the authoring machine
(no Docker / no Linux X stack available there).
"""
from __future__ import annotations

import argparse
import http.client
import logging
import os
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

log = logging.getLogger("bbb-gateway")

# Headers we never forward verbatim from the browser side.
_DROP_REQUEST_HEADERS = {
    "host",            # rewritten to the app authority below
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailer",
    "transfer-encoding",
    "upgrade",
    "x-bbb-gateway",   # never trust a client-supplied gateway header
}

# Headers stripped from app responses (docs/api_contract.md §4).
_DROP_RESPONSE_HEADERS = {
    "server",
    "x-powered-by",
    "connection",
    "keep-alive",
    "transfer-encoding",
}


class _GatewayHandler(BaseHTTPRequestHandler):
    """Forward every request to the app with the gateway header injected."""

    # Silence per-request stderr logging; keep access noise out of container logs.
    def log_message(self, fmt: str, *args) -> None:  # noqa: A003
        log.debug("gateway %s", fmt % args)

    # send_response() would otherwise re-add a `Server: BaseHTTP/... Python/...`
    # header — exactly the fingerprint the scrub list exists to remove (T5).
    def send_response(self, code: int, message: str | None = None) -> None:
        self.log_request(code)
        self.send_response_only(code, message)
        self.send_header("Date", self.date_time_string())

    def _proxy(self) -> None:
        target_host, target_port = self.server.app_authority  # type: ignore[attr-defined]
        secret = self.server.gateway_secret  # type: ignore[attr-defined]

        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length) if length > 0 else None

        out_headers = {
            k: v
            for k, v in self.headers.items()
            if k.lower() not in _DROP_REQUEST_HEADERS
        }
        out_headers["Host"] = f"{target_host}:{target_port}"
        out_headers["X-BBB-Gateway"] = secret

        conn = http.client.HTTPConnection(target_host, target_port, timeout=30)
        try:
            conn.request(self.command, self.path, body=body, headers=out_headers)
            resp = conn.getresponse()
            payload = resp.read()
        except (OSError, http.client.HTTPException) as exc:
            log.warning("upstream error: %s", exc)
            self.send_error(502, "Bad Gateway")
            return
        finally:
            conn.close()

        self.send_response(resp.status)
        for key, value in resp.getheaders():
            if key.lower() in _DROP_RESPONSE_HEADERS:
                continue
            # BaseHTTPRequestHandler manages Content-Length itself below.
            if key.lower() == "content-length":
                continue
            self.send_header(key, value)
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(payload)

    # Wire the real handler in for every supported method (explicit, auditable).
    do_GET = _proxy
    do_POST = _proxy
    do_PUT = _proxy
    do_PATCH = _proxy
    do_DELETE = _proxy
    do_HEAD = _proxy
    do_OPTIONS = _proxy


class _GatewayServer(ThreadingHTTPServer):
    daemon_threads = True
    allow_reuse_address = True

    def __init__(self, listen: tuple[str, int], app_authority: tuple[str, int], secret: str):
        super().__init__(listen, _GatewayHandler)
        self.app_authority = app_authority
        self.gateway_secret = secret


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--listen", default="127.0.0.1:8200",
                        help="host:port the browser reaches (loopback only)")
    parser.add_argument("--target", default="127.0.0.1:8100",
                        help="host:port of the reference app")
    parser.add_argument("--secret-env", default="BBB_GATEWAY_SECRET",
                        help="name of the env var holding the gateway secret")
    args = parser.parse_args(argv)

    secret = os.environ.get(args.secret_env, "")
    if not secret:
        log.error("%s is not set; refusing to start", args.secret_env)
        return 2

    listen_host, _, listen_port = args.listen.rpartition(":")
    target_host, _, target_port = args.target.rpartition(":")
    server = _GatewayServer(
        (listen_host or "127.0.0.1", int(listen_port)),
        (target_host or "127.0.0.1", int(target_port)),
        secret,
    )
    log.info("gateway %s -> %s (header injected)", args.listen, args.target)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0


if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="[gateway] %(message)s")
    sys.exit(main())
