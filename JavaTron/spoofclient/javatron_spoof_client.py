# This file was entirely handled by ChatGPT as a testing tool for the server. I claim none of it. -Maxwell Nield

import argparse
import os
import queue
import random
import socket
import string
import threading
import time
from datetime import datetime
from typing import Optional, List


def now_stamp() -> str:
    return datetime.now().strftime("%Y-%m-%d_%H%M%S")


def pretty_ts() -> str:
    return datetime.now().strftime("%m-%d-%Y | %H:%M:%S")


def ensure_logs_dir(logs_dir: str) -> None:
    os.makedirs(logs_dir, exist_ok=True)


def safe_filename(text: str) -> str:
    keep = []
    for ch in text:
        if ch.isalnum() or ch in ("-", "_", "."):
            keep.append(ch)
        else:
            keep.append("_")
    return "".join(keep)


def random_name(prefix: str) -> str:
    suffix = "".join(random.choice(string.ascii_lowercase + string.digits) for _ in range(6))
    return f"{prefix}{suffix}"


class ClientLogger:
    def __init__(self, logs_dir: str, client_tag: str):
        ensure_logs_dir(logs_dir)
        filename = f"client_{safe_filename(client_tag)}_{now_stamp()}.log"
        self.path = os.path.join(logs_dir, filename)
        self.lock = threading.Lock()
        with open(self.path, "w", encoding="utf-8") as f:
            f.write(f"[{pretty_ts()}] Log start: {client_tag}\n")

    def log(self, line: str) -> None:
        full = f"[{pretty_ts()}] {line}"
        with self.lock:
            with open(self.path, "a", encoding="utf-8") as f:
                f.write(full + "\n")
        print(full)


def parse_lines_script(script: str) -> List[str]:
    # Split on literal "\n" sequences or actual newlines
    if "\\n" in script and "\n" not in script:
        parts = script.split("\\n")
    else:
        parts = script.splitlines()
    return [p.strip() for p in parts if p.strip()]


def send_line(sock: socket.socket, text: str) -> None:
    data = (text + "\n").encode("utf-8")
    sock.sendall(data)


def recv_loop(
    sock: socket.socket,
    stop_event: threading.Event,
    log: ClientLogger,
    tag: str,
    rematch_vote: str,
    rematch_vote_delay: float,
) -> None:
    sock.settimeout(0.5)
    buffer = b""
    while not stop_event.is_set():
        try:
            chunk = sock.recv(4096)
            if not chunk:
                log.log(f"{tag} <- (server closed connection)")
                stop_event.set()
                return
            buffer += chunk
            while b"\n" in buffer:
                line, buffer = buffer.split(b"\n", 1)
                text = line.decode("utf-8", errors="replace")
                log.log(f"{tag} <- {text}")
                # Rematch voting support
                if rematch_vote and text.startswith("S_REMATCH_PROMPT|"):
                    vote = rematch_vote.strip().upper()
                    if vote == "RANDOM":
                        vote = random.choice(["YES", "NO"])
                    if vote in ("YES", "NO"):
                        try:
                            if rematch_vote_delay > 0:
                                time.sleep(rematch_vote_delay)
                            send_line(sock, f"C_REMATCH_VOTE|{vote}")
                            log.log(f"{tag} -> C_REMATCH_VOTE|{vote}")
                        except Exception as ex:
                            log.log(f"{tag} -> (rematch vote send error) {ex}")

        except socket.timeout:
            continue
        except Exception as ex:
            log.log(f"{tag} <- (recv error) {ex}")
            stop_event.set()
            return


def keepalive_loop(
    sock: socket.socket,
    stop_event: threading.Event,
    log: ClientLogger,
    tag: str,
    interval_sec: float,
    ping_cmd: Optional[str],
) -> None:
    # Sends a periodic ping/keepalive line, if ping_cmd is provided.
    # If your server expects a specific ping format, set --ping-cmd accordingly.
    if not ping_cmd:
        return

    while not stop_event.is_set():
        time.sleep(interval_sec)
        if stop_event.is_set():
            return
        try:
            send_line(sock, ping_cmd)
            log.log(f"{tag} -> {ping_cmd}")
        except Exception as ex:
            log.log(f"{tag} -> (keepalive send error) {ex}")
            stop_event.set()
            return


def turn_spam_loop(
    sock: socket.socket,
    stop_event: threading.Event,
    log: ClientLogger,
    tag: str,
    turn_cmd: Optional[str],
    rate_hz: float,
    pattern: str,
) -> None:
    """
    Sends turn commands periodically to simulate gameplay input.
    pattern: e.g. "L,R,L,R" or "L,L,R,S" (S = straight/no-op if your protocol supports it)
    turn_cmd should include "{DIR}" placeholder, e.g. "C_TURN {DIR}"
    """
    if not turn_cmd:
        return

    dirs = [p.strip() for p in pattern.split(",") if p.strip()]
    if not dirs:
        dirs = ["L", "R"]

    i = 0
    delay = max(0.01, 1.0 / max(0.1, rate_hz))

    while not stop_event.is_set():
        time.sleep(delay)
        if stop_event.is_set():
            return

        chosen = dirs[i % len(dirs)]
        i += 1

        line = turn_cmd.replace("{DIR}", chosen)
        try:
            send_line(sock, line)
            log.log(f"{tag} -> {line}")
        except Exception as ex:
            log.log(f"{tag} -> (turn send error) {ex}")
            stop_event.set()
            return


def run_one_client(args: argparse.Namespace, client_index: int) -> None:
    name = args.name if args.name else random_name(args.name_prefix)
    if args.clients > 1:
        name = f"{name}_{client_index+1}"

    tag = f"[{name}]"
    log = ClientLogger(args.logs_dir, name)

    stop_event = threading.Event()

    hello_line = None
    if args.hello_cmd:
        hello_line = args.hello_cmd.replace("{NAME}", name)

    findmatch_line = None
    if args.findmatch_cmd:
        findmatch_line = args.findmatch_cmd.replace("{NAME}", name)

    # Build initial script (highest priority is --script; otherwise use hello/findmatch)
    script_lines: List[str] = []
    if args.script:
        script_lines = [ln.replace("{NAME}", name) for ln in parse_lines_script(args.script)]
    else:
        if hello_line:
            script_lines.append(hello_line)
        if findmatch_line:
            script_lines.append(findmatch_line)

    # Connect
    try:
        sock = socket.create_connection((args.host, args.port), timeout=5.0)
    except Exception as ex:
        log.log(f"{tag} connect failed to {args.host}:{args.port}: {ex}")
        return

    log.log(f"{tag} connected to {args.host}:{args.port}")

    # Start receiver
    recv_thread = threading.Thread(
        target=recv_loop,
        args=(sock, stop_event, log, tag, args.rematch_vote, args.rematch_vote_delay),
        daemon=True,
    )
    recv_thread.start()

    # Send initial script
    for line in script_lines:
        try:
            send_line(sock, line)
            log.log(f"{tag} -> {line}")
        except Exception as ex:
            log.log(f"{tag} -> (send error) {ex}")
            stop_event.set()
            break
        time.sleep(args.script_delay)

    # Optional keepalive pings
    keepalive_thread = threading.Thread(
        target=keepalive_loop,
        args=(sock, stop_event, log, tag, args.ping_interval, args.ping_cmd),
        daemon=True,
    )
    keepalive_thread.start()

    # Optional turn spam
    turn_thread = threading.Thread(
        target=turn_spam_loop,
        args=(sock, stop_event, log, tag, args.turn_cmd, args.turn_rate_hz, args.turn_pattern),
        daemon=True,
    )
    turn_thread.start()

    # Hold / disconnect logic
    start_time = time.time()
    disconnect_time = None
    if args.disconnect_after > 0:
        disconnect_time = start_time + args.disconnect_after

    # Optional early random disconnect to test cleanup
    early_disconnect = False
    if args.disconnect_rate > 0.0 and random.random() < args.disconnect_rate:
        early_disconnect = True

    while not stop_event.is_set():
        time.sleep(0.25)

        if early_disconnect and (time.time() - start_time) > random.uniform(0.5, max(0.5, args.disconnect_after / 2 if args.disconnect_after > 0 else 5.0)):
            log.log(f"{tag} early disconnect triggered")
            break

        if disconnect_time and time.time() >= disconnect_time:
            log.log(f"{tag} disconnect_after reached")
            break

    # Close
    stop_event.set()
    try:
        sock.shutdown(socket.SHUT_RDWR)
    except Exception:
        pass
    try:
        sock.close()
    except Exception:
        pass

    log.log(f"{tag} disconnected; log saved to {log.path}")


def main() -> None:
    parser = argparse.ArgumentParser(description="JavaTron TCP client spoofer (line-based protocol).")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=7777)

    parser.add_argument("--clients", type=int, default=1, help="Number of clients to run in this process.")
    parser.add_argument("--name", default="", help="Exact client name (if empty, random).")
    parser.add_argument("--name-prefix", default="spoof_")

    parser.add_argument("--logs-dir", default="logs", help="Folder to write client logs into.")
    parser.add_argument("--script", default="", help="Custom lines to send (use {NAME}). Use \\n to separate lines.")
    parser.add_argument("--script-delay", type=float, default=0.15, help="Delay between script lines in seconds.")

    # Common protocol knobs (set these to match your Protocol.java)
    parser.add_argument("--hello-cmd", default="C_HELLO {NAME}", help="HELLO command line (use {NAME}). Empty disables.")
    parser.add_argument("--findmatch-cmd", default="C_FIND_MATCH", help="Find match command line. Empty disables.")
    parser.add_argument("--ping-cmd", default="C_PING", help="Ping/keepalive command line. Empty disables keepalive.")
    parser.add_argument("--ping-interval", type=float, default=5.0, help="Seconds between keepalive pings.")

    # Optional simulated gameplay input
    parser.add_argument("--turn-cmd", default="", help="Turn command with {DIR}, e.g. 'C_TURN {DIR}'. Empty disables.")
    parser.add_argument("--turn-rate-hz", type=float, default=8.0, help="How often to send turns.")
    parser.add_argument("--turn-pattern", default="L,R,L,R", help="Comma pattern: L,R,S etc.")
    # Optional rematch voting (server sends S_REMATCH_PROMPT after S_MATCH_END)
    parser.add_argument(
        "--rematch-vote",
        default="YES",
        help="Vote to send when S_REMATCH_PROMPT is received: YES, NO, RANDOM, or empty to disable.",
    )
    parser.add_argument(
        "--rematch-vote-delay",
        type=float,
        default=0.35,
        help="Seconds to wait before sending C_REMATCH_VOTE after receiving S_REMATCH_PROMPT.",
    )


    # Disconnect behaviors
    parser.add_argument("--disconnect-after", type=float, default=12.0, help="Seconds before disconnect (0 = never).")
    parser.add_argument("--disconnect-rate", type=float, default=0.0, help="0..1 chance each client disconnects early.")

    args = parser.parse_args()

    threads: List[threading.Thread] = []
    for i in range(max(1, args.clients)):
        t = threading.Thread(target=run_one_client, args=(args, i), daemon=False)
        threads.append(t)
        t.start()
        time.sleep(0.1)

    for t in threads:
        t.join()


if __name__ == "__main__":
    main()