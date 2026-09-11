# Simple Dockerfile for AI service (FastAPI)
FROM python:3.11-slim
WORKDIR /app
COPY ai-service/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt || true
COPY ai-service ./ai-service
WORKDIR /app/ai-service
EXPOSE 8000
CMD ["uvicorn","app.main:app","--host","0.0.0.0","--port","8000"]
