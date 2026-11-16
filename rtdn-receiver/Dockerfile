# Build from repository root:
# docker build -f rtdn-receiver/Dockerfile .
# NEVER deploy without private Cloud Run IAM + verified Pub/Sub push identity.
FROM python:3.12-slim
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY rtdn-receiver/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY backend/play_refund_events.py ./play_refund_events.py
COPY rtdn-receiver/rtdn_push_receiver.py ./rtdn_push_receiver.py
CMD ["sh", "-c", "exec gunicorn --bind 0.0.0.0:${PORT:-8080} --workers 2 --threads 4 --timeout 60 rtdn_push_receiver:app"]
