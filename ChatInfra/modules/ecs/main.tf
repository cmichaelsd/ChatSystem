data "aws_caller_identity" "current" {}

resource "aws_ecs_cluster" "main" {
  name = var.project_name
}

resource "aws_cloudwatch_log_group" "apiserver" {
  name              = "/ecs/${var.project_name}/apiserver"
  retention_in_days = 365
}

resource "aws_cloudwatch_log_group" "chatserver" {
  name              = "/ecs/${var.project_name}/chatserver"
  retention_in_days = 365
}

resource "aws_cloudwatch_log_group" "presenceserver" {
  name              = "/ecs/${var.project_name}/presenceserver"
  retention_in_days = 365
}

# --- Execution role (used by ECS agent to pull images and inject secrets) ---

resource "aws_iam_role" "ecs_execution_api" {
  name = "${var.project_name}-ecs-execution-api"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
      Condition = {
        ArnLike = {
          "aws:SourceArn" = "arn:aws:ecs:${var.region}:${data.aws_caller_identity.current.account_id}:*"
        }
        StringEquals = {
          "aws:SourceAccount" = data.aws_caller_identity.current.account_id
        }
      }
    }]
  })
}

resource "aws_iam_role" "ecs_execution_chatserver" {
  name = "${var.project_name}-ecs-execution-chatserver"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
      Condition = {
        ArnLike = {
          "aws:SourceArn" = "arn:aws:ecs:${var.region}:${data.aws_caller_identity.current.account_id}:*"
        }
        StringEquals = {
          "aws:SourceAccount" = data.aws_caller_identity.current.account_id
        }
      }
    }]
  })
}

resource "aws_iam_role" "ecs_execution_presenceserver" {
  name = "${var.project_name}-ecs-execution-presenceserver"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
      Condition = {
        ArnLike = {
          "aws:SourceArn" = "arn:aws:ecs:${var.region}:${data.aws_caller_identity.current.account_id}:*"
        }
        StringEquals = {
          "aws:SourceAccount" = data.aws_caller_identity.current.account_id
        }
      }
    }]
  })
}


resource "aws_iam_role_policy_attachment" "ecs_execution_policy_api" {
  role       = aws_iam_role.ecs_execution_api.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy_attachment" "ecs_execution_policy_chatserver" {
  role       = aws_iam_role.ecs_execution_chatserver.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy_attachment" "ecs_execution_policy_presenceserver" {
  role       = aws_iam_role.ecs_execution_presenceserver.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecs_execution_secrets_api" {
  name = "${var.project_name}-ecs-secrets-api"
  role = aws_iam_role.ecs_execution_api.id

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

resource "aws_iam_role_policy" "ecs_execution_secrets_chatserver" {
  name = "${var.project_name}-ecs-secrets-chatserver"
  role = aws_iam_role.ecs_execution_chatserver.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = ["secretsmanager:GetSecretValue"]
      Resource = [
        var.jwt_secret_arn,
        var.internal_api_key_arn,
      ]
    }]
  })
}

resource "aws_iam_role_policy" "ecs_execution_secrets_presenceserver" {
  name = "${var.project_name}-ecs-secrets-presenceserver"
  role = aws_iam_role.ecs_execution_presenceserver.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = ["secretsmanager:GetSecretValue"]
      Resource = [
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
      Condition = {
        ArnLike = {
          "aws:SourceArn" = "arn:aws:ecs:${var.region}:${data.aws_caller_identity.current.account_id}:*"
        }
        StringEquals = {
          "aws:SourceAccount" = data.aws_caller_identity.current.account_id
        }
      }
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
        "dynamodb:DescribeTable",
        "dynamodb:GetItem",
        "dynamodb:BatchGetItem",
        "dynamodb:PutItem",
        "dynamodb:DeleteItem",
        "dynamodb:UpdateItem",
        "dynamodb:Query",
        "dynamodb:Scan",
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
        "sqs:DeleteQueue",
        "sqs:SendMessage",
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueUrl",
        "sqs:GetQueueAttributes",
      ]
      Resource = "arn:aws:sqs:${var.region}:${data.aws_caller_identity.current.account_id}:chat-server-*"
    }]
  })
}

resource "aws_iam_role_policy" "chatserver_xray" {
  name = "${var.project_name}-chatserver-xray"
  role = aws_iam_role.chatserver_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "xray:PutTraceSegments",
        "xray:PutTelemetryRecords",
        "xray:GetSamplingRules",
        "xray:GetSamplingTargets",
        "xray:GetSamplingStatisticSummaries",
      ]
      Resource = "*"
    }]
  })
}

# --- ApiServer task role ---

resource "aws_iam_role" "apiserver_task" {
  name = "${var.project_name}-apiserver-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
      Condition = {
        ArnLike = {
          "aws:SourceArn" = "arn:aws:ecs:${var.region}:${data.aws_caller_identity.current.account_id}:*"
        }
        StringEquals = {
          "aws:SourceAccount" = data.aws_caller_identity.current.account_id
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "apiserver_xray" {
  name = "${var.project_name}-apiserver-xray"
  role = aws_iam_role.apiserver_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "xray:PutTraceSegments",
        "xray:PutTelemetryRecords",
        "xray:GetSamplingRules",
        "xray:GetSamplingTargets",
        "xray:GetSamplingStatisticSummaries",
      ]
      Resource = "*"
    }]
  })
}

# --- PresenceServer task role ---

resource "aws_iam_role" "presenceserver_task" {
  name = "${var.project_name}-presenceserver-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
      Condition = {
        ArnLike = {
          "aws:SourceArn" = "arn:aws:ecs:${var.region}:${data.aws_caller_identity.current.account_id}:*"
        }
        StringEquals = {
          "aws:SourceAccount" = data.aws_caller_identity.current.account_id
        }
      }
    }]
  })
}

resource "aws_iam_role_policy" "presenceserver_xray" {
  name = "${var.project_name}-presenceserver-xray"
  role = aws_iam_role.presenceserver_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "xray:PutTraceSegments",
        "xray:PutTelemetryRecords",
        "xray:GetSamplingRules",
        "xray:GetSamplingTargets",
        "xray:GetSamplingStatisticSummaries",
      ]
      Resource = "*"
    }]
  })
}

# --- X-Ray daemon log groups ---

resource "aws_cloudwatch_log_group" "apiserver_xray" {
  name              = "/ecs/${var.project_name}/apiserver-xray"
  retention_in_days = 365
}

resource "aws_cloudwatch_log_group" "chatserver_xray" {
  name              = "/ecs/${var.project_name}/chatserver-xray"
  retention_in_days = 365
}

resource "aws_cloudwatch_log_group" "presenceserver_xray" {
  name              = "/ecs/${var.project_name}/presenceserver-xray"
  retention_in_days = 365
}
