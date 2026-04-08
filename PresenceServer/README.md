# PresenceServer

## Overview
The PresenceServer basically just caches heartbeats and expires them automatically after a set period of time.
If the users heartbeat exists they are online, if not they are offline.


## How to push docker image to AWS ECR
1) `aws ecr get-login-password --region us-west-1 | docker login --username AWS --password-stdin 657083456388.dkr.ecr.us-west-1.amazonaws.com`
2) `docker build -t chatsystem-presenceserver .`
3) `docker tag chatsystem-presenceserver:latest 657083456388.dkr.ecr.us-west-1.amazonaws.com/chatsystem/presenceserver:latest`
4) `docker push 657083456388.dkr.ecr.us-west-1.amazonaws.com/chatsystem/presenceserver:latest`


## If packages are altered
Update `requirements.txt` by running `pip freeze > requirements.txt`


## How to update openapi.json
`python generate_openapi.py`


## How to test
1) `pip install -r requirements-dev.txt`
2) `pytest`
