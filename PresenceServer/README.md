# PresenceServer

## Overview
PresenceServer tracks whether users are online by caching heartbeats in Redis with a TTL. If a user's heartbeat key exists they're online, if it's expired they're offline. 
ChatServer sends heartbeats on behalf of all connected users on a regular interval. 
PresenceServer is internal-only - it sits behind an internal ALB and is never reachable directly from the browser. 
Presence queries from clients are proxied through ChatServer.


## REST API

### Internal (called by ChatServer only)
| Method | Endpoint              | Auth         | Description                              |
|--------|-----------------------|--------------|------------------------------------------|
| `POST` | `/presence/heartbeat` | Internal key | Mark a list of users as online           |

### External (called by ChatServer on behalf of clients)
| Method | Endpoint          | Auth | Description                                   |
|--------|-------------------|------|-----------------------------------------------|
| `POST` | `/presence/batch` | JWT  | Get online/offline status for a list of users |
| `GET`  | `/presence/{id}`  | JWT  | Get online/offline status for a single user   |


## How to run locally
1) Create the shared Docker network (once): `docker network create chatsystem`
2) `docker compose -f docker-compose.dev.yml build --no-cache`
3) `docker compose -f docker-compose.dev.yml up`
4) Navigate to `localhost:8002/docs`
5) For end-to-end testing move to ChatServer project and follow local testing instructions as well.


## How to push docker image to AWS ECR
1) `aws ecr get-login-password --region us-west-1 --profile chatsystem | docker login --username AWS --password-stdin 657083456388.dkr.ecr.us-west-1.amazonaws.com`
2) `docker build -t chatsystem-presenceserver .`
3) `docker tag chatsystem-presenceserver:latest 657083456388.dkr.ecr.us-west-1.amazonaws.com/chatsystem/presenceserver:latest`
4) `docker push 657083456388.dkr.ecr.us-west-1.amazonaws.com/chatsystem/presenceserver:latest`
5) For image update in ECS (if needed): `aws ecs update-service --cluster chatsystem --service chatsystem-presenceserver --force-new-deployment --profile chatsystem --region us-west-1`


## If packages are altered
Update `requirements.txt` by running `pip freeze > requirements.txt`


## How to update openapi.json
`python generate_openapi.py`


## How to test
1) `pip install -r requirements-dev.txt`
2) `pytest`
