# File Upload API

Simple Spring Boot REST API for learning how multipart uploads can be stored on local disk while keeping metadata in PostgreSQL.

## What it does

- Stores uploaded file bytes on the local filesystem under `./uploads`
- Saves file metadata in PostgreSQL with:
  - `id`
  - `name`
  - `type`
  - `size`
  - `date`
- Exposes endpoints to upload, list metadata, fetch metadata by id, and download file content

## Stack

- Java 21
- Spring Boot 3.5
- PostgreSQL
- Docker Compose

## Run with Docker

```bash
docker compose up --build
```

The API will be available at `http://localhost:8081`.

## Endpoints

```bash
curl -X POST http://localhost:8081/api/files \
  -F "file=@/Users/dhev/Downloads/Test Images/test-img.png"

curl http://localhost:8081/api/files
curl http://localhost:8081/api/files/{id}
curl http://localhost:8081/api/files/{id}/content --output downloaded-file
```

## Run tests

```bash
./mvnw test
```
