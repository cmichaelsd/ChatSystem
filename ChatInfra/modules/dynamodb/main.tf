resource "aws_dynamodb_table" "server_registry" {
  name         = "ServerRegistry"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "serverId"

  attribute {
    name = "serverId"
    type = "S"
  }

  server_side_encryption {
    enabled = true
  }

  tags = { Name = "${var.project_name}-server-registry" }
}

resource "aws_dynamodb_table" "user_connections" {
  name         = "UserConnections"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "userId"

  attribute {
    name = "userId"
    type = "S"
  }

  server_side_encryption {
    enabled = true
  }

  tags = { Name = "${var.project_name}-user-connections" }
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

  global_secondary_index {
    name            = "userId-index"
    hash_key        = "userId"
    projection_type = "KEYS_ONLY"
  }

  server_side_encryption {
    enabled = true
  }

  tags = { Name = "${var.project_name}-conversation-members" }
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

  server_side_encryption {
    enabled = true
  }

  tags = { Name = "${var.project_name}-messages" }
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

  server_side_encryption {
    enabled = true
  }

  tags = { Name = "${var.project_name}-pending-messages" }
}
