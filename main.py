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
        <h1>🚀 Test Web Server Running!</h1>
        <p>서버가 정상적으로 실행중입니다.</p>
        <p><a href="/health">Health Check</a></p>
        <p><a href="/info">Server Info</a></p>
    </body>
    </html>
    '''


@app.route('/health')
def health():
    return jsonify({
        "status": "healthy",
        "timestamp": datetime.datetime.now().isoformat(),
        "message": "서버가 정상 작동중입니다"
    })


@app.route('/info')
def info():
    hostname = socket.gethostname()
    local_ip = socket.gethostbyname(hostname)

    return jsonify({
        "hostname": hostname,
        "local_ip": local_ip,
        "timestamp": datetime.datetime.now().isoformat(),
        "version": os.environ.get('APP_VERSION', 'v1.0'),
        "environment": os.environ.get('ENV', 'development')
    })


@app.route('/version')
def version():
    return jsonify({
        "version": os.environ.get('APP_VERSION', 'v1.0'),
        "build_time": os.environ.get('BUILD_TIME', 'unknown')
    })


if __name__ == '__main__':
    port = int(os.environ.get('PORT', 8080))
    app.run(host='0.0.0.0', port=port, debug=False)