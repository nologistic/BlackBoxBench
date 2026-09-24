"""Loopback-only public-web proxy for the trusted reference browser.

The browser has no direct semantic channel to the Agent.  This proxy exists
only to let the trusted pixel-producing browser reach explicitly public HTTP
and HTTPS destinations while rejecting loopback, private, link-local,
reserved, multicast and otherwise non-global addresses on every connection.
"""
from __future__ import annotations

import argparse
import ipaddress
import selectors
import socket
import socketserver
from urllib.parse import urlsplit

MAX_HEADER = 64 * 1024
ALLOWED_PORTS = {80, 443}


class Denied(ValueError):
    pass


def _authority(value: str, default_port: int) -> tuple[str, int]:
    try:
        parsed = urlsplit("//" + value)
        port = parsed.port or default_port
    except ValueError as exc:
        raise Denied("invalid authority") from exc
    if not parsed.hostname or parsed.username is not None or parsed.password is not None:
        raise Denied("invalid authority")
    if port not in ALLOWED_PORTS:
        raise Denied("port is not allowed")
    return parsed.hostname.rstrip(".").lower(), port


def _public_addresses(host: str, port: int) -> list[tuple]:
    try:
        records = socket.getaddrinfo(host, port, type=socket.SOCK_STREAM)
    except socket.gaierror as exc:
        raise Denied("hostname did not resolve") from exc
    unique: list[tuple] = []
    seen: set[tuple] = set()
    for family, socktype, proto, _canonname, sockaddr in records:
        try:
            address = ipaddress.ip_address(sockaddr[0])
        except ValueError as exc:
            raise Denied("resolver returned an invalid address") from exc
        if not address.is_global:
            raise Denied("destination is not public")
        key = (family, socktype, proto, sockaddr)
        if key not in seen:
            seen.add(key)
            unique.append(key)
    if not unique:
        raise Denied("hostname did not resolve")
    return unique


def _connect_public(host: str, port: int) -> socket.socket:
    last_error: OSError | None = None
    # Connect to the exact addresses just validated instead of resolving the
    # hostname a second time (DNS-rebinding defense).
    for family, socktype, proto, sockaddr in _public_addresses(host, port):
        remote = socket.socket(family, socktype, proto)
        remote.settimeout(15)
        try:
            remote.connect(sockaddr)
            remote.settimeout(None)
            return remote
        except OSError as exc:
            last_error = exc
            remote.close()
    raise Denied("public destination is unreachable") from last_error


def _read_header(client: socket.socket) -> tuple[bytes, bytes]:
    data = bytearray()
    while b"\r\n\r\n" not in data:
        block = client.recv(8192)
        if not block:
            break
        data.extend(block)
        if len(data) > MAX_HEADER:
            raise Denied("request header is too large")
    marker = data.find(b"\r\n\r\n")
    if marker < 0:
        raise Denied("incomplete request header")
    return bytes(data[:marker + 4]), bytes(data[marker + 4:])


def _relay(left: socket.socket, right: socket.socket) -> None:
    selector = selectors.DefaultSelector()
    selector.register(left, selectors.EVENT_READ, right)
    selector.register(right, selectors.EVENT_READ, left)
    try:
        while selector.get_map():
            for key, _mask in selector.select(timeout=60):
                source = key.fileobj
                target = key.data
                try:
                    data = source.recv(65536)
                except OSError:
                    data = b""
                if not data:
                    try:
                        selector.unregister(source)
                    except KeyError:
                        pass
                    try:
                        target.shutdown(socket.SHUT_WR)
                    except OSError:
                        pass
                    continue
                target.sendall(data)
    finally:
        selector.close()


class ProxyHandler(socketserver.BaseRequestHandler):
    def handle(self) -> None:
        self.request.settimeout(20)
        remote: socket.socket | None = None
        try:
            header, remainder = _read_header(self.request)
            lines = header[:-4].split(b"\r\n")
            method_raw, target_raw, version = lines[0].split(b" ", 2)
            method = method_raw.decode("ascii", "strict").upper()
            target = target_raw.decode("ascii", "strict")

            if method == "CONNECT":
                host, port = _authority(target, 443)
                remote = _connect_public(host, port)
                self.request.sendall(b"HTTP/1.1 200 Connection Established\r\n\r\n")
                if remainder:
                    remote.sendall(remainder)
            else:
                parsed = urlsplit(target)
                if parsed.scheme not in {"http", "https"} or not parsed.netloc:
                    raise Denied("proxy requests must use an absolute URL")
                default_port = 80 if parsed.scheme == "http" else 443
                host, port = _authority(parsed.netloc, default_port)
                if parsed.scheme == "https":
                    raise Denied("HTTPS must use CONNECT")
                remote = _connect_public(host, port)
                origin = parsed.path or "/"
                if parsed.query:
                    origin += "?" + parsed.query
                filtered = []
                for line in lines[1:]:
                    name = line.partition(b":")[0].strip().lower()
                    if name not in {b"proxy-authorization", b"proxy-connection", b"connection"}:
                        filtered.append(line)
                outgoing = (method_raw + b" " + origin.encode("ascii") + b" " + version
                            + b"\r\n" + b"\r\n".join(filtered)
                            + b"\r\nConnection: close\r\n\r\n" + remainder)
                remote.sendall(outgoing)
            self.request.settimeout(None)
            _relay(self.request, remote)
        except (Denied, UnicodeError, ValueError, OSError):
            try:
                self.request.sendall(
                    b"HTTP/1.1 403 Forbidden\r\nConnection: close\r\n"
                    b"Content-Length: 0\r\n\r\n")
            except OSError:
                pass
        finally:
            if remote is not None:
                remote.close()


class ProxyServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--listen", default="127.0.0.1:8300")
    args = parser.parse_args()
    host, port_text = args.listen.rsplit(":", 1)
    with ProxyServer((host, int(port_text)), ProxyHandler) as server:
        server.serve_forever()


if __name__ == "__main__":
    main()
