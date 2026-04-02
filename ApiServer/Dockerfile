FROM python:3.14-slim AS build
WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN addgroup --system apiserver && adduser --system --ingroup apiserver apiserver
RUN chown -R apiserver:apiserver /app
USER apiserver

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
