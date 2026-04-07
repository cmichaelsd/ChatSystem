# PresenceServer

## Overview
The PresenceServer basically just caches heartbeats and expires them automatically after a set period of time.
If the users heartbeat exists they are online, if not they are offline.


## If packages are altered
Update `requirements.txt` by running `pip freeze > requirements.txt`


## How to update openapi.json
`python generate_openapi.py`


## How to test
1) `pip install -r requirements-dev.txt`
2) `pytest`
