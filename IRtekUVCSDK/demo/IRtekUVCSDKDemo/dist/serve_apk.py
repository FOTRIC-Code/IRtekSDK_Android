from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import os

os.chdir(os.path.dirname(os.path.abspath(__file__)))


class Handler(SimpleHTTPRequestHandler):
    def end_headers(self):
        if self.path.endswith(".apk"):
            self.send_header(
                "Content-Disposition",
                'attachment; filename="IRtekUVCSDKDemo.apk"',
            )
        super().end_headers()


if __name__ == "__main__":
    ThreadingHTTPServer(("0.0.0.0", 8765), Handler).serve_forever()
