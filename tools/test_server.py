#!/usr/bin/env python3
"""
Test server for PerformTracker network transmission testing.

Usage:
    python3 test_server.py                    # Default port 31415
    python3 test_server.py --port 9000        # Custom port
    python3 test_server.py --save logs/       # Save to file
"""

import argparse
import json
import os
import socket
import threading
from datetime import datetime

class PerformTrackerHandler:
    log_dir = None
    sample_count = 0
    
    @classmethod
    def handle(cls, client_socket, client_address):
        try:
            client_socket.settimeout(30)
            
            while True:
                try:
                    header_data = b''
                    while b'\r\n\r\n' not in header_data:
                        chunk = client_socket.recv(1)
                        if not chunk:
                            return
                        header_data += chunk
                    
                    header_str = header_data.decode('utf-8')
                    headers = {}
                    
                    for line in header_str.split('\r\n'):
                        if ':' in line:
                            key, value = line.split(':', 1)
                            headers[key.strip().lower()] = value.strip()
                    
                    content_length = int(headers.get('content-length', 0))
                    
                    if content_length > 0:
                        body = b''
                        while len(body) < content_length:
                            chunk = client_socket.recv(content_length - len(body))
                            if not chunk:
                                break
                            body += chunk
                        
                        cls.process_body(body)
                    
                    keep_alive = headers.get('connection', '').lower() != 'close'
                    response = cls.build_response(keep_alive)
                    client_socket.sendall(response)
                    
                    if not keep_alive:
                        return
                        
                except socket.timeout:
                    return
                    
        except Exception as e:
            pass
        finally:
            try:
                client_socket.close()
            except:
                pass
    
    @classmethod
    def build_response(cls, keep_alive):
        body = b'{"status":"ok"}'
        response = (
            b'HTTP/1.1 200 OK\r\n'
            b'Content-Type: application/json\r\n'
            b'Content-Length: 11\r\n'
            b'Access-Control-Allow-Origin: *\r\n'
        )
        if keep_alive:
            response += b'Connection: keep-alive\r\n'
        else:
            response += b'Connection: close\r\n'
        response += b'\r\n' + body
        return response
    
    @classmethod
    def process_body(cls, body):
        try:
            data = json.loads(body.decode('utf-8'))
            cls.sample_count += 1
            cls.print_metrics(data)
            
            if cls.log_dir:
                cls.save_log(data)
        except json.JSONDecodeError:
            print(f"[{datetime.now().strftime('%H:%M:%S')}] Invalid JSON: {body.decode('utf-8')[:50]}...")
    
    @classmethod
    def print_metrics(cls, data):
        session_id = data.get('sessionId', 'N/A')
        timestamp = data.get('timestamp', 0)
        sample_number = data.get('sampleNumber', 0)
        
        print("=" * 60)
        print(f"[{datetime.now().strftime('%H:%M:%S')}] METRICS (total: {cls.sample_count})")
        print(f"Session ID: {session_id}")
        print(f"Sample #: {sample_number}")
        
        if timestamp:
            dt = datetime.fromtimestamp(timestamp / 1000)
            print(f"Timestamp: {dt.strftime('%Y-%m-%d %H:%M:%S')}")
        
        if 'data' in data:
            d = data['data']
            print("\nMetrics:")
            if 'fps' in d:
                print(f"  FPS:   {d.get('fps', 'N/A')}")
            if 'tps' in d:
                print(f"  TPS:   {d.get('tps', 'N/A')}")
            if 'mspt' in d:
                print(f"  MSPT:  {d.get('mspt', 'N/A')}")
        
        print("=" * 60)
    
    @classmethod
    def save_log(cls, data):
        if not cls.log_dir:
            return
        os.makedirs(cls.log_dir, exist_ok=True)
        filename = f"{cls.log_dir}/metrics_{datetime.now().strftime('%Y%m%d_%H%M%S_%f')}.json"
        with open(filename, 'w') as f:
            json.dump(data, f, indent=2)


def start_server(host, port):
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    server_socket.bind((host, port))
    server_socket.listen(5)
    server_socket.settimeout(1)
    
    print(f"Server listening on {host}:{port}")
    
    running = True
    while running:
        try:
            client_socket, client_address = server_socket.accept()
            thread = threading.Thread(target=PerformTrackerHandler.handle, args=(client_socket, client_address))
            thread.daemon = True
            thread.start()
        except socket.timeout:
            continue
        except KeyboardInterrupt:
            running = False
    
    server_socket.close()


def main():
    parser = argparse.ArgumentParser(description='PerformTracker Test Server')
    parser.add_argument('--port', '-p', type=int, default=31415, help='Port to listen on (default: 31415)')
    parser.add_argument('--save', '-s', metavar='DIR', help='Save received data to directory')
    parser.add_argument('--host', default='0.0.0.0', help='Host to bind (default: 0.0.0.0)')
    
    args = parser.parse_args()
    
    PerformTrackerHandler.log_dir = args.save
    
    print(f"""
╔══════════════════════════════════════════════════════════╗
║           PerformTracker Test Server                     ║
╠══════════════════════════════════════════════════════════╣
║  URL:      http://localhost:{args.port}                        ║
║  Endpoint: POST /api/metrics                            ║
╠══════════════════════════════════════════════════════════╣
║  Config in Mod Menu:                                    ║
║    Enable Network Transmission: ✓                       ║
║    HTTP Endpoint URL: http://localhost:{args.port}/api/metrics  ║
╠══════════════════════════════════════════════════════════╣
║  Keep-Alive: Enabled                                   ║
║  Threading: Enabled (handles concurrent requests)      ║
╠══════════════════════════════════════════════════════════╣
║  Press Ctrl+C to stop                                   ║
╚══════════════════════════════════════════════════════════╝
""")
    
    if args.save:
        print(f"Saving received data to: {args.save}/\n")
    
    try:
        start_server(args.host, args.port)
    except KeyboardInterrupt:
        print("\n\nShutting down...")


if __name__ == '__main__':
    main()
