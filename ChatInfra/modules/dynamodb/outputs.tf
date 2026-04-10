output "table_arns" {
  value = [
    aws_dynamodb_table.server_registry.arn,
    aws_dynamodb_table.user_connections.arn,
    aws_dynamodb_table.conversation_members.arn,
    aws_dynamodb_table.messages.arn,
    aws_dynamodb_table.pending_messages.arn,
  ]
}
