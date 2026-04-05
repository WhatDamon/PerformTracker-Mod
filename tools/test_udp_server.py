#!/usr/bin/env python3
import argparse
import json
import os
import socket
import socketserver
from datetime import datetime


class UDPMetricsHandler:
    received_metrics = []
    sample_count = 0
    log_dir = None

    @classmethod
    def process_packet(cls, data, addr):
        try:
            metric_data = json.loads(data.decode("utf-8"))
            cls.sample_count += 1
            cls.received_metrics.append(metric_data)
            cls.print_metrics(metric_data)

            if cls.log_dir:
                cls.save_log(metric_data)
        except json.JSONDecodeError as e:
            print(f"[{datetime.now().strftime('%H:%M:%S')}] Invalid JSON from {addr}: {e}")
        except Exception as e:
            print(f"[{datetime.now().strftime('%H:%M:%S')}] Error processing packet from {addr}: {e}")

    @classmethod
    def print_metrics(cls, data):
        session_id = data.get("sessionId", "N/A")
        timestamp = data.get("timestamp", 0)
        sample_number = data.get("sampleNumber", 0)

        print("=" * 60)
        print(f"[{datetime.now().strftime('%H:%M:%S')}] METRICS (total: {cls.sample_count})")
        print(f"Session ID: {session_id}")
        print(f"Sample #:   {sample_number}")

        if timestamp:
            dt = datetime.fromtimestamp(timestamp / 1000)
            print(f"Timestamp:  {dt.strftime('%Y-%m-%d %H:%M:%S')}")

        if "data" in data:
            d = data["data"]
            print("\nMetrics:")
            if "fps" in d:
                print(f"  FPS:   {d.get('fps', 'N/A')}")
            if "tps" in d:
                print(f"  TPS:   {d.get('tps', 'N/A')}")
            if "mspt" in d:
                print(f"  MSPT:  {d.get('mspt', 'N/A')}")
            if "heapUsed" in d and "heapMax" in d:
                heap_used = d.get("heapUsed", 0)
                heap_max = d.get("heapMax", 0)
                print(f"  Heap:  {cls.format_memory(heap_used)} / {cls.format_memory(heap_max)}")
            if "cpu" in d:
                print(f"  CPU:   {d.get('cpu', 'N/A')}%")

        print("=" * 60)

    @classmethod
    def format_memory(cls, mb):
        if mb >= 1024:
            return f"{mb/1024:.1f}GiB"
        return f"{mb:.0f}MiB"

    @classmethod
    def save_log(cls, data):
        if not cls.log_dir:
            return
        os.makedirs(cls.log_dir, exist_ok=True)
        filename = f"{cls.log_dir}/metrics_{datetime.now().strftime('%Y%m%d_%H%M%S_%f')}.json"
        with open(filename, "w") as f:
            json.dump(data, f, indent=2)


class ThreadedUDPServer(socketserver.ThreadingMixIn, socketserver.UDPServer):
    allow_reuse_address = True
    daemon_threads = True


def run_continuous_server(host: str, port: int, log_dir: str = None):
    UDPMetricsHandler.log_dir = log_dir

    print(f"UDP Server listening on {host}:{port}")

    try:
        with ThreadedUDPServer((host, port), None) as server:
            server.RequestHandlerClass = lambda *args, **kwargs: None
            server.RequestHandlerClass.process_packet = staticmethod(UDPMetricsHandler.process_packet)
            
            print("\nWaiting for metrics (Ctrl+C to stop)...\n")
            
            class Handler(socketserver.BaseRequestHandler):
                def handle(self):
                    data = self.request[0]
                    UDPMetricsHandler.process_packet(data, self.client_address)
            
            server.RequestHandlerClass = Handler
            
            while True:
                server.handle_request()
    except KeyboardInterrupt:
        print("\n\nShutting down...")
    except Exception as e:
        print(f"[ERROR] Server error: {e}")
        return False

    return True


def run_single_server(host: str, port: int, timeout: int):
    print(f"Starting UDP test server on {host}:{port}")
    print(f"Waiting for metrics (timeout: {timeout}s)...")
    print("-" * 50)

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.settimeout(timeout)
    sock.bind((host, port))

    try:
        while True:
            try:
                data, addr = sock.recvfrom(4096)
                UDPMetricsHandler.process_packet(data, addr)
            except socket.timeout:
                break
    except Exception as e:
        print(f"[ERROR] Server error: {e}")
        return False
    finally:
        sock.close()

    if not UDPMetricsHandler.received_metrics:
        print("[WARN] No metrics received within timeout")
        return False

    print("-" * 50)
    print(f"\nReceived {len(UDPMetricsHandler.received_metrics)} metric sample(s)")
    return True


def main():
    parser = argparse.ArgumentParser(description="PerformTracker UDP Test Server")
    parser.add_argument("--host", default="localhost", help="Server host (default: localhost)")
    parser.add_argument("--port", "-p", type=int, default=31416, help="Server port (default: 31416)")
    parser.add_argument("--timeout", type=int, default=0, help="Timeout in seconds, 0 for continuous (default: 0)")
    parser.add_argument("--save", "-s", metavar="DIR", help="Save received data to directory")
    args = parser.parse_args()

    print(f"""
╔══════════════════════════════════════════════════════════╗
║           PerformTracker UDP Test Server                 ║
╠══════════════════════════════════════════════════════════╣
║  Protocol:  UDP                                          ║
║  Endpoint:  {args.host}:{args.port}                              ║
╠══════════════════════════════════════════════════════════╣
║  Press Ctrl+C to stop                                    ║
╚══════════════════════════════════════════════════════════╝
""")

    if args.save:
        print(f"Saving received data to: {args.save}/\n")

    if args.timeout > 0:
        run_single_server(args.host, args.port, args.timeout)
        print("\n" + "=" * 50)
        if UDPMetricsHandler.received_metrics:
            print("Test completed - metrics received successfully")
        else:
            print("Test failed - no metrics received")
    else:
        run_continuous_server(args.host, args.port, args.save)


if __name__ == "__main__":
    main()
