# ChatServer

## Overview
This ChatServer handles WebSocket connections and message delivery for the chat system.

A chat server will register itself and its SQS queue URL with DynamoDB.

When a user joins a given chat server the user is associated with the server in DynamoDB.
Also, a mapping is kept of each user and their WebSocket session at runtime.

When a user sends a message, the target user's server is found from DynamoDB and the associated
SQS queue URL is used to route the message to that server for delivery.

If the target user is offline the message is saved to a pending messages table in DynamoDB
and delivered when the user reconnects.

Both 1-to-1 and group chat use the same conversation model. A conversation has members stored
in DynamoDB and a message sent to a conversation is fanned out to all members. Offline behavior
is the same — undelivered messages are saved as pending.

Message history is persisted to DynamoDB on every send and can be fetched via the REST API.


## DynamoDB Tables

### ServerRegistry
| Attribute      | Key | Type   | Description                        |
|----------------|-----|--------|------------------------------------|
| `serverId`     | PK  | String | Unique server identifier           |
| `queueUrl`     |     | String | SQS queue URL for this server      |
| `ip`           |     | String | Server IP address                  |
| `registeredAt` |     | String | ISO-8601 timestamp of registration |

### UserConnections
| Attribute  | Key | Type   | Description                               |
|------------|-----|--------|-------------------------------------------|
| `userId`   | PK  | String | Unique user identifier                    |
| `serverId` |     | String | Server the user is currently connected to |

### ConversationMembers
| Attribute        | Key | Type   | Description                    |
|------------------|-----|--------|--------------------------------|
| `conversationId` | PK  | String | Unique conversation identifier |
| `userId`         | SK  | String | Member of the conversation     |

### Messages
| Attribute        | Key | Type   | Description                          |
|------------------|-----|--------|--------------------------------------|
| `conversationId` | PK  | String | Conversation this message belongs to |
| `sentAt`         | SK  | String | ISO-8601 timestamp of send time      |
| `fromUserId`     |     | String | Sender's user ID                     |
| `content`        |     | String | Message content                      |

### PendingMessages
| Attribute        | Key | Type   | Description                              |
|------------------|-----|--------|------------------------------------------|
| `userId`         | PK  | String | Recipient user ID (offline at send time) |
| `messageId`      | SK  | String | UUID for the pending message             |
| `fromUserId`     |     | String | Sender's user ID                         |
| `conversationId` |     | String | Conversation this message belongs to     |
| `content`        |     | String | Message content                          |
| `sentAt`         |     | String | ISO-8601 timestamp of send time          |


## How to run locally
1) `docker compose -f docker-compose.dev.yml build --no-cache`
2) `docker compose -f docker-compose.dev.yml up`
3) Seed a conversation into DynamoDB:
```
curl -X POST http://localhost:8080/dev/conversations \
  -H "Content-Type: application/json" \
  -d '{"conversationId":"conv1","memberIds":["userA","userB"]}'
```
4) In one terminal: `websocat ws://localhost:8080/chat?user_id=userA`
5) In another terminal: `websocat ws://localhost:8081/chat?user_id=userB`
6) Send a message from userA: `{"conversationId":"conv1","content":"hello"}`
7) Fetch message history: `curl http://localhost:8080/conversations/conv1/messages`


## Dev paths
For simplicity there are dev paths available for local testing, you can see how to use them in the run locally section.
They exist just to help set up data to ensure the service actually works.


## How to test
`./gradlew test clean`


## How to lint
- `./gradlew ktlintCheck`
- `./gradlew ktlintFormat`