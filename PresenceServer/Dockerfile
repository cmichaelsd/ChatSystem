FROM python:3.14-slim
WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

RUN addgroup --system presenceserver && adduser --system --ingroup presenceserver presenceserver
RUN chown -R presenceserver:presenceserver /app
USER presenceserver

EXPOSE 8000
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
