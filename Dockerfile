FROM python:3.9-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install -r requirements.txt

COPY simple_web_server.py .

EXPOSE 8080

CMD ["python", "simple_web_server.py"]