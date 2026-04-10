resource "aws_dynamodb_table" "server_registry" {
  name         = "ServerRegistry"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "serverId"

  attribute {
    name = "serverId"
    type = "S"
  }

  tags = { Name = "chatsystem-server-registry" }
}

resource "aws_dynamodb_table" "user_connections" {
  name         = "UserConnections"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "userId"

  attribute {
    name = "userId"
    type = "S"
  }

  tags = { Name = "chatsystem-user-connections" }
}

resource "aws_dynamodb_table" "conversation_members" {
  name         = "ConversationMembers"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "conversationId"
  range_key    = "userId"

  attribute {
    name = "conversationId"
    type = "S"
  }

  attribute {
    name = "userId"
    type = "S"
  }

  tags = { Name = "chatsystem-conversation-members" }
}

resource "aws_dynamodb_table" "messages" {
  name         = "Messages"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "conversationId"
  range_key    = "sentAt"

  attribute {
    name = "conversationId"
    type = "S"
  }

  attribute {
    name = "sentAt"
    type = "S"
  }

  tags = { Name = "chatsystem-messages" }
}

resource "aws_dynamodb_table" "pending_messages" {
  name         = "PendingMessages"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "userId"
  range_key    = "messageId"

  attribute {
    name = "userId"
    type = "S"
  }

  attribute {
    name = "messageId"
    type = "S"
  }

  tags = { Name = "chatsystem-pending-messages" }
}
