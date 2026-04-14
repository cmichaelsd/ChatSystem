output "cluster_id" {
  value = aws_ecs_cluster.main.id
}

output "execution_role_api_arn" {
  value = aws_iam_role.ecs_execution_api.arn
}

output "execution_role_chatserver_arn" {
  value = aws_iam_role.ecs_execution_chatserver.arn
}

output "execution_role_presenceserver_arn" {
  value = aws_iam_role.ecs_execution_presenceserver.arn
}

output "chatserver_task_role_arn" {
  value = aws_iam_role.chatserver_task.arn
}

output "apiserver_log_group" {
  value = aws_cloudwatch_log_group.apiserver.name
}

output "chatserver_log_group" {
  value = aws_cloudwatch_log_group.chatserver.name
}

output "presenceserver_log_group" {
  value = aws_cloudwatch_log_group.presenceserver.name
}
