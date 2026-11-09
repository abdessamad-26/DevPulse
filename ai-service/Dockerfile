FROM python:3.11-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY requirements.txt ./

RUN apt-get update \
    && apt-get upgrade -y \
    && rm -rf /var/lib/apt/lists/* \
    && python -m pip install --no-cache-dir --upgrade pip setuptools wheel jaraco.context \
    && python -m pip install --no-cache-dir -r requirements.txt \
    && python -m pip check

COPY app ./app

# Run as an unprivileged user: a compromised process must not be root in the container.
RUN useradd --system --no-create-home --uid 10001 appuser
USER appuser

EXPOSE 8000
CMD ["uvicorn","app.main:app","--host","0.0.0.0","--port","8000"]
