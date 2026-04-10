resource "aws_ecs_cluster" "main" {
  name = var.project_name
}

resource "aws_cloudwatch_log_group" "apiserver" {
  name              = "/ecs/${var.project_name}/apiserver"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "chatserver" {
  name              = "/ecs/${var.project_name}/chatserver"
  retention_in_days = 7
}

resource "aws_cloudwatch_log_group" "presenceserver" {
  name              = "/ecs/${var.project_name}/presenceserver"
  retention_in_days = 7
}

# --- Execution role (used by ECS agent to pull images and inject secrets) ---

resource "aws_iam_role" "ecs_execution" {
  name = "${var.project_name}-ecs-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution_policy" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecs_execution_secrets" {
  name = "${var.project_name}-ecs-secrets"
  role = aws_iam_role.ecs_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = ["secretsmanager:GetSecretValue"]
      Resource = [
        var.db_secret_arn,
        var.jwt_secret_arn,
        var.internal_api_key_arn,
      ]
    }]
  })
}

# --- ChatServer task role (needs DynamoDB + SQS access) ---

resource "aws_iam_role" "chatserver_task" {
  name = "${var.project_name}-chatserver-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy" "chatserver_dynamodb" {
  name = "${var.project_name}-chatserver-dynamodb"
  role = aws_iam_role.chatserver_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "dynamodb:GetItem",
        "dynamodb:PutItem",
        "dynamodb:DeleteItem",
        "dynamodb:UpdateItem",
        "dynamodb:Query",
        "dynamodb:DescribeTable",
        "dynamodb:CreateTable",
      ]
      Resource = var.dynamodb_table_arns
    }]
  })
}

resource "aws_iam_role_policy" "chatserver_sqs" {
  name = "${var.project_name}-chatserver-sqs"
  role = aws_iam_role.chatserver_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "sqs:CreateQueue",
        "sqs:SendMessage",
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueUrl",
        "sqs:GetQueueAttributes",
      ]
      Resource = "arn:aws:sqs:${var.region}:657083456388:chat-server-*"
    }]
  })
}
