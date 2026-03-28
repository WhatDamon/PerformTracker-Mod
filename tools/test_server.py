#!/usr/bin/env python3
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
            header_data = b''
            while b'\r\n\r\n' not in header_data:
                chunk = client_socket.recv(1)
                if not chunk:
                    return
                header_data += chunk
            
            header_str = header_data.decode('utf-8')
            headers = {}
            request_line = header_str.split('\r\n')[0]
            method, path, _ = request_line.split(' ', 2)
            
            for line in header_str.split('\r\n')[1:]:
                if ':' in line:
                    key, value = line.split(':', 1)
                    headers[key.strip().lower()] = value.strip()
            
            if method == 'POST' and path == '/api/metrics':
                content_length = int(headers.get('content-length', 0))
                if content_length > 0:
                    body = b''
                    while len(body) < content_length:
                        chunk = client_socket.recv(content_length - len(body))
                        if not chunk:
                            break
                        body += chunk
                    cls.process_body(body)
                response = cls.build_response('{"status":"ok"}', keep_alive=True)
                client_socket.sendall(response)
            else:
                response = cls.build_response('{"error":"not found"}', keep_alive=False)
                client_socket.sendall(response)
                             
        except Exception as e:
            pass
        finally:
            try:
                client_socket.close()
            except:
                pass
    
    @classmethod
    def build_response(cls, body, keep_alive=True):
        body_bytes = body.encode('utf-8')
        response = (
            b'HTTP/1.1 200 OK\r\n'
            b'Content-Type: application/json\r\n'
            b'Content-Length: %d\r\n' % len(body_bytes) +
            b'Access-Control-Allow-Origin: *\r\n'
        )
        if keep_alive:
            response += b'Connection: keep-alive\r\n'
        else:
            response += b'Connection: close\r\n'
        response += b'\r\n' + body_bytes
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
            if 'heapUsed' in d and 'heapMax' in d:
                heap_used = d.get('heapUsed', 0)
                heap_max = d.get('heapMax', 0)
                print(f"  Heap:  {cls.format_memory(heap_used)} / {cls.format_memory(heap_max)}")
            if 'cpu' in d:
                cpu = d.get('cpu', -1)
                if cpu >= 0:
                    print(f"  CPU:   {cpu:.1f}%")
                else:
                    print(f"  CPU:   N/A")
        
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
║  Endpoints:                                              ║
║    POST /api/metrics    - Receive metrics                ║
╠══════════════════════════════════════════════════════════╣
║  Config in Mod Menu:                                     ║
║    Enable Network Transmission: ✓                        ║
║    HTTP Endpoint URL: http://localhost:{args.port}             ║
╠══════════════════════════════════════════════════════════╣
║  Commands:                                               ║
║    /ptracker deviceinfo  - Show local device info        ║
╠══════════════════════════════════════════════════════════╣
║  Keep-Alive: Enabled                                     ║
║  Threading: Enabled (handles concurrent requests)        ║
╠══════════════════════════════════════════════════════════╣
║  Press Ctrl+C to stop                                    ║
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
