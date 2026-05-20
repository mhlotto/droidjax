#!/usr/bin/env python3
"""Serve the DroidJax MathJax keyboard test page on the local network."""

from __future__ import annotations

import argparse
import functools
import http.server
import socket
from pathlib import Path


def local_ip() -> str:
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sock:
        try:
            sock.connect(("8.8.8.8", 80))
            return sock.getsockname()[0]
        except OSError:
            return "127.0.0.1"


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Serve tools/mathjax-test/index.html for Android keyboard testing.",
    )
    parser.add_argument("--host", default="0.0.0.0", help="Bind host.")
    parser.add_argument("--port", default=8765, type=int, help="Bind port.")
    args = parser.parse_args()

    directory = Path(__file__).resolve().parent
    handler = functools.partial(
        http.server.SimpleHTTPRequestHandler,
        directory=str(directory),
    )
    server = http.server.ThreadingHTTPServer((args.host, args.port), handler)
    ip = local_ip()

    print("DroidJax MathJax test server")
    print(f"Local:   http://127.0.0.1:{args.port}/")
    print(f"Network: http://{ip}:{args.port}/")
    print("Press Ctrl-C to stop.")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping server.")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
