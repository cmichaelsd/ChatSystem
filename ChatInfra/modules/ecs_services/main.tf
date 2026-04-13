# --- ApiServer ---

resource "aws_ecs_task_definition" "apiserver" {
  family                   = "${var.project_name}-apiserver"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = var.execution_role_arn

  container_definitions = jsonencode([{
    name      = "apiserver"
    image     = "657083456388.dkr.ecr.${var.region}.amazonaws.com/chatsystem/apiserver:latest"
    essential = true

    portMappings = [{
      containerPort = 8000
      protocol      = "tcp"
    }]

    environment = [
      { name = "CHAT_SERVER_URL", value = "http://${var.chatserver_nlb_dns_name}" },
      { name = "CORS_ORIGINS", value = "https://${var.cloudfront_domain}" },
    ]

    secrets = [
      { name = "DATABASE_URL", valueFrom = "${var.db_secret_arn}:database_url::" },
      { name = "SECRET_KEY", valueFrom = var.jwt_secret_arn },
      { name = "INTERNAL_API_KEY", valueFrom = var.internal_api_key_arn },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = var.apiserver_log_group
        awslogs-region        = var.region
        awslogs-stream-prefix = "apiserver"
      }
    }
  }])
}

resource "aws_ecs_service" "apiserver" {
  name            = "${var.project_name}-apiserver"
  cluster         = var.cluster_id
  task_definition = aws_ecs_task_definition.apiserver.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [var.ecs_tasks_sg_id]
  }

  load_balancer {
    target_group_arn = var.apiserver_tg_arn
    container_name   = "apiserver"
    container_port   = 8000
  }
}

# --- PresenceServer ---

resource "aws_ecs_task_definition" "presenceserver" {
  family                   = "${var.project_name}-presenceserver"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 256
  memory                   = 512
  execution_role_arn       = var.execution_role_arn

  container_definitions = jsonencode([{
    name      = "presenceserver"
    image     = "657083456388.dkr.ecr.${var.region}.amazonaws.com/chatsystem/presenceserver:latest"
    essential = true

    portMappings = [{
      containerPort = 8000
      protocol      = "tcp"
    }]

    environment = [
      { name = "REDIS_URL", value = "redis://${var.redis_address}:6379" }
    ]

    secrets = [
      { name = "SECRET_KEY", valueFrom = var.jwt_secret_arn },
      { name = "INTERNAL_API_KEY", valueFrom = var.internal_api_key_arn },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = var.presenceserver_log_group
        awslogs-region        = var.region
        awslogs-stream-prefix = "presenceserver"
      }
    }
  }])
}

resource "aws_ecs_service" "presenceserver" {
  name            = "${var.project_name}-presenceserver"
  cluster         = var.cluster_id
  task_definition = aws_ecs_task_definition.presenceserver.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [var.ecs_tasks_sg_id]
  }

  load_balancer {
    target_group_arn = var.presenceserver_tg_arn
    container_name   = "presenceserver"
    container_port   = 8000
  }
}

# --- ChatServer ---

resource "aws_ecs_task_definition" "chatserver" {
  family                   = "${var.project_name}-chatserver"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = var.execution_role_arn
  task_role_arn            = var.chatserver_task_role_arn

  container_definitions = jsonencode([{
    name      = "chatserver"
    image     = "657083456388.dkr.ecr.${var.region}.amazonaws.com/chatsystem/chatserver:latest"
    essential = true

    portMappings = [{
      containerPort = 8080
      protocol      = "tcp"
    }]

    environment = [
      { name = "PRESENCE_SERVER_URL", value = "http://${var.internal_alb_dns_name}" },
      { name = "CORS_ORIGINS", value = "https://${var.cloudfront_domain}" },
    ]

    secrets = [
      { name = "JWT_SECRET", valueFrom = var.jwt_secret_arn },
      { name = "INTERNAL_API_KEY", valueFrom = var.internal_api_key_arn },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = var.chatserver_log_group
        awslogs-region        = var.region
        awslogs-stream-prefix = "chatserver"
      }
    }
  }])
}

resource "aws_ecs_service" "chatserver" {
  name            = "${var.project_name}-chatserver"
  cluster         = var.cluster_id
  task_definition = aws_ecs_task_definition.chatserver.arn
  desired_count   = 2
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = var.private_subnet_ids
    security_groups = [var.ecs_tasks_sg_id]
  }

  load_balancer {
    target_group_arn = var.chatserver_tg_arn
    container_name   = "chatserver"
    container_port   = 8080
  }
}
