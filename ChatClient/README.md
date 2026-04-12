# ChatClient

## Overview
React + TypeScript frontend for the chat system. Talks to ApiServer over HTTP for auth, groups, messages, and presence. Connects directly to ChatServer over WebSocket for real-time messaging.

State is managed with Zustand. Presence is polled every 30 seconds. WebSocket reconnects automatically on disconnect.


## How to run locally
Make sure ApiServer and ChatServer are running first.

1) `npm install`
2) `npm run dev`
3) Open `http://localhost:5173`

The dev server uses `http://localhost:8001` for the API and `ws://localhost:8080/chat` for WebSocket by default — no `.env` file needed.


## How to deploy to AWS
After running `terraform apply` in `ChatInfra/`:

```bash
./ChatInfra/deploy-client.sh
```

Reads Terraform outputs, builds the client with the real URLs, syncs to S3, and invalidates the CloudFront cache. The live URL is output as `chatclient_url` from Terraform.
