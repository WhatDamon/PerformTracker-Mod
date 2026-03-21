#!/usr/bin/env python3
"""
Test server for PerformTracker network transmission testing.

Usage:
    python3 test_server.py                    # Default port 8080
    python3 test_server.py --port 9000        # Custom port
    python3 test_server.py --pretty            # Pretty print JSON
    python3 test_server.py --save logs/        # Save to file
"""

import argparse
import json
import sys
import os
from datetime import datetime
from http.server import HTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs

class MetricsHandler(BaseHTTPRequestHandler):
    pretty_print = False
    log_dir = None
    request_count = 0
    
    def log_message(self, format, *args):
        pass
    
    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == '/':
            self.send_html()
        elif parsed.path == '/stats':
            self.send_stats()
        else:
            self.send_error(404)
    
    def do_POST(self):
        if self.path != '/api/metrics':
            self.send_error(404, "Not Found")
            return
        
        try:
            content_length = int(self.headers.get('Content-Length', 0))
            body = self.rfile.read(content_length)
            
            if not body:
                self.send_error(400, "Empty body")
                return
            
            data = json.loads(body)
            MetricsHandler.request_count += 1
            
            self.process_metrics(data)
            
            self.send_response(200)
            self.send_header('Content-Type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(json.dumps({'status': 'ok', 'received': MetricsHandler.request_count}).encode())
            
        except json.JSONDecodeError as e:
            self.send_error(400, f"Invalid JSON: {e}")
        except Exception as e:
            self.send_error(500, str(e))
    
    def process_metrics(self, data):
        msg_type = data.get('type', 'unknown')
        session_id = data.get('sessionId', 'N/A')
        timestamp = data.get('unixTimestamp', 0)
        session_active = data.get('sessionActive', None)
        
        output = []
        output.append("=" * 60)
        output.append(f"[{datetime.now().strftime('%H:%M:%S')}] {msg_type.upper()}")
        output.append(f"Session ID: {session_id}")
        
        if timestamp:
            dt = datetime.fromtimestamp(timestamp)
            output.append(f"Timestamp: {dt.strftime('%Y-%m-%d %H:%M:%S')}")
        
        if session_active is not None:
            output.append(f"Session Active: {session_active}")
        
        if 'data' in data:
            d = data['data']
            output.append("\nMetrics:")
            output.append(f"  FPS:   {d.get('fps', 'N/A')}")
            output.append(f"  TPS:   {d.get('tps', 'N/A')}")
            output.append(f"  MSPT:  {d.get('mspt', 'N/A')}")
            output.append(f"  Status: {d.get('status', 'N/A')}")
        
        output.append("=" * 60)
        
        line = '\n'.join(output)
        print(line)
        
        if self.log_dir:
            self.save_log(data)
    
    def save_log(self, data):
        if not self.log_dir:
            return
        os.makedirs(self.log_dir, exist_ok=True)
        filename = f"{self.log_dir}/metrics_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        with open(filename, 'w') as f:
            json.dump(data, f, indent=2)
    
    def send_html(self):
        html = f"""<!DOCTYPE html>
<html>
<head>
    <title>PerformTracker Test Server</title>
    <style>
        body {{ font-family: monospace; background: #1a1a2e; color: #eee; padding: 20px; }}
        h1 {{ color: #00d9ff; }}
        .stats {{ background: #16213e; padding: 15px; border-radius: 8px; margin: 20px 0; }}
        .ok {{ color: #00ff88; }}
        .endpoint {{ color: #ffd700; }}
        pre {{ background: #0f0f23; padding: 15px; border-radius: 5px; overflow-x: auto; }}
        code {{ color: #ff6b6b; }}
    </style>
</head>
<body>
    <h1>PerformTracker Test Server</h1>
    <div class="stats">
        <p>Requests received: <span class="ok">{MetricsHandler.request_count}</span></p>
        <p>Server time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>
    </div>
    <h2>Expected Endpoint</h2>
    <p class="endpoint">POST <code>/api/metrics</code></p>
    <h2>Example Request Body</h2>
    <pre>{json.dumps({
        "type": "performance_metrics",
        "timestamp": 1710950400000,
        "unixTimestamp": 1710950400,
        "sessionId": "20240320_103200_a1b2c3",
        "sessionActive": True,
        "sampleNumber": 1,
        "data": {
            "fps": 60.0,
            "tps": 20.0,
            "mspt": 0.50,
            "status": "OK"
        }
    }, indent=2)}</pre>
</body>
</html>"""
        
        self.send_response(200)
        self.send_header('Content-Type', 'text/html')
        self.end_headers()
        self.wfile.write(html.encode())
    
    def send_stats(self):
        stats = {
            'request_count': MetricsHandler.request_count,
            'server_time': datetime.now().isoformat()
        }
        
        self.send_response(200)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(stats).encode())


def main():
    parser = argparse.ArgumentParser(description='PerformTracker Test Server')
    parser.add_argument('--port', '-p', type=int, default=31415, help='Port to listen on (default: 31415)')
    parser.add_argument('--pretty', action='store_true', help='Pretty print received JSON')
    parser.add_argument('--save', '-s', metavar='DIR', help='Save received data to directory')
    parser.add_argument('--host', default='0.0.0.0', help='Host to bind (default: 0.0.0.0)')
    
    args = parser.parse_args()
    
    MetricsHandler.pretty_print = args.pretty
    MetricsHandler.log_dir = args.save
    
    server = HTTPServer((args.host, args.port), MetricsHandler)
    
    print(f"""
╔══════════════════════════════════════════════════════════╗
║           PerformTracker Test Server                     ║
╠══════════════════════════════════════════════════════════╣
║  URL:      http://localhost:{args.port}                        ║
║  Endpoint: POST /api/metrics                            ║
║  Web UI:   http://localhost:{args.port}/                        ║
║  Stats:    http://localhost:{args.port}/stats                   ║
╠══════════════════════════════════════════════════════════╣
║  Config in Mod Menu:                                    ║
║    Enable Network Transmission: ✓                        ║
║    HTTP Endpoint URL: http://localhost:{args.port}/api/metrics ║
╠══════════════════════════════════════════════════════════╣
║  Press Ctrl+C to stop                                   ║
╚══════════════════════════════════════════════════════════╝
""")
    
    if args.save:
        print(f"Saving received data to: {args.save}/\n")
    
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\n\nShutting down...")
        server.shutdown()


if __name__ == '__main__':
    main()
