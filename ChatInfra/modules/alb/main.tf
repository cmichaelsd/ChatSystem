# --- Public ALB (ApiServer + PresenceServer) ---

resource "aws_lb" "public_alb" {
  name               = "${var.project_name}-public-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [var.alb_sg_id]
  subnets            = var.public_subnet_ids
}

resource "aws_lb_target_group" "apiserver" {
  name        = "${var.project_name}-apiserver"
  port        = 8000
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = "/health"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "apiserver" {
  load_balancer_arn = aws_lb.public_alb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.apiserver.arn
  }
}

# --- Internal ALB (PresenceServer — only ChatServer talks to it) ---

resource "aws_lb" "internal_alb" {
  name               = "${var.project_name}-internal-alb"
  internal           = true
  load_balancer_type = "application"
  security_groups    = [var.alb_sg_id]
  subnets            = var.public_subnet_ids
}

resource "aws_lb_target_group" "presenceserver" {
  name        = "${var.project_name}-presenceserver"
  port        = 8000
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  health_check {
    path                = "/health"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "presenceserver" {
  load_balancer_arn = aws_lb.internal_alb.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.presenceserver.arn
  }
}

# --- Public NLB (ChatServer WebSocket) ---

resource "aws_lb" "chatserver_nlb" {
  name               = "${var.project_name}-chatserver-nlb"
  internal           = false
  load_balancer_type = "network"
  subnets            = var.public_subnet_ids
}

resource "aws_lb_target_group" "chatserver" {
  name                 = "${var.project_name}-chatserver"
  port                 = 8080
  protocol             = "TCP"
  vpc_id               = var.vpc_id
  target_type          = "ip"
  preserve_client_ip   = false

  health_check {
    protocol            = "HTTP"
    path                = "/health"
    port                = "8080"
    interval            = 30
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }
}

resource "aws_lb_listener" "chatserver" {
  load_balancer_arn = aws_lb.chatserver_nlb.arn
  port              = 80
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.chatserver.arn
  }
}
