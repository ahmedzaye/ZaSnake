FROM python:3.12-slim
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt
COPY main.py ./main.py
# The deployment must inject PORT, GOOGLE_CLOUD_PROJECT, PGS_WEB_CLIENT_ID and
# PGS_WEB_CLIENT_SECRET via Cloud Run + Secret Manager. No secrets are baked in.
CMD ["sh", "-c", "exec gunicorn --bind 0.0.0.0:${PORT:-8080} --workers 2 --threads 8 --timeout 60 main:app"]
