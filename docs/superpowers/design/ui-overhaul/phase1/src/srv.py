import http.server, os, sys
ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = sys.argv[2]
os.makedirs(OUT, exist_ok=True)
class H(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *a, **k):
        super().__init__(*a, directory=ROOT, **k)
    def do_POST(self):
        n = int(self.headers.get('Content-Length', 0))
        name = self.path.split('name=')[-1]
        with open(os.path.join(OUT, name), 'wb') as f:
            f.write(self.rfile.read(n))
        self.send_response(200); self.end_headers(); self.wfile.write(b'ok')
    def log_message(self, *a): pass
http.server.ThreadingHTTPServer(('127.0.0.1', int(sys.argv[1])), H).serve_forever()
