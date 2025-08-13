#!/usr/bin/env python3

from flask import Flask, jsonify
import socket
import datetime
import os

app = Flask(__name__)


@app.route('/')
def home():
    return '''
    <html>
    <head><title>Test Web Server</title></head>
    <body>
        <h1>Test Web Server</h1>
        <h1>Version : ECS 5</h1>
    </body>
    </html>
    '''



if __name__ == '__main__':
    port = int(os.environ.get('PORT', 8080))
    app.run(host='0.0.0.0', port=port, debug=False)