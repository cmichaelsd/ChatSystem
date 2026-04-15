# ApiServer

## Overview
An API Server which handles authorization and HTTP requests.
Additionally, there are S2S calls, API Server will contact Chat Server to update Dynamo with conversation data.


## REST API

### Auth
| Method | Endpoint          | Description              |
|--------|-------------------|--------------------------|
| `POST` | `/auth/register`  | Register a new user      |
| `POST` | `/auth/login`     | Login and receive a JWT  |

### Users
| Method | Endpoint          | Description              |
|--------|-------------------|--------------------------|
| `GET`  | `/users/me`       | Get current user         |
| `GET`  | `/users/{id}`     | Get a user by ID         |

### Groups
| Method   | Endpoint                              | Description                  |
|----------|---------------------------------------|------------------------------|
| `GET`    | `/groups`                             | List groups for current user |
| `POST`   | `/groups`                             | Create a group               |
| `GET`    | `/groups/{id}`                        | Get a group by ID            |
| `DELETE` | `/groups/{id}`                        | Delete a group               |
| `GET`    | `/groups/{id}/members`                | List group members           |
| `POST`   | `/groups/{id}/members/{userId}`       | Add a member to a group      |
| `DELETE` | `/groups/{id}/members/{userId}`       | Remove a member from a group |


## How to run locally
1) Create the shared Docker network (once): `docker network create chatsystem`
2) `docker compose -f docker-compose.dev.yml build --no-cache`
3) `docker compose -f docker-compose.dev.yml up`
4) Navigate to `localhost:8001/docs`
5) For end-to-end testing move to ChatServer project and follow local testing instructions as well.


## How to push docker image to AWS ECR
1) `aws ecr get-login-password --region us-west-1 | docker login --username AWS --password-stdin 657083456388.dkr.ecr.us-west-1.amazonaws.com`
2) `docker build -t chatsystem-apiserver .`
3) `docker tag chatsystem-apiserver:latest 657083456388.dkr.ecr.us-west-1.amazonaws.com/chatsystem/apiserver:latest`
4) `docker push 657083456388.dkr.ecr.us-west-1.amazonaws.com/chatsystem/apiserver:latest`
5) For image update in ECS (if needed): `aws ecs update-service --cluster chatsystem --service chatsystem-apiserver --force-new-deployment`


## If packages are altered
Update `requirements.txt` by running `pip freeze > requirements.txt`


## How to update openapi.json
`python generate_openapi.py`


## How to test
1) `pip install -r requirements-dev.txt`
2) `pytest`
