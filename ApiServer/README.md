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
4) Apply migrations: `docker compose -f docker-compose.dev.yml exec apiserver alembic upgrade head`
5) Navigate to `localhost:8001/docs`
6) For end-to-end testing move to ChatServer project and follow local testing instructions as well.


## When models change
1) Generate a migration: `docker compose -f docker-compose.dev.yml exec apiserver alembic revision --autogenerate -m "<WHAT_CHANGED>"`
2) Apply it: `docker compose -f docker-compose.dev.yml exec apiserver alembic upgrade head`


## If packages are altered
Update `requirements.txt` by running `pip freeze > requirements.txt`


## How to update openapi.json
`python generate_openapi.py`
