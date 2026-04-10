output "cluster_id" {
  value = aws_ecs_cluster.main.id
}

output "execution_role_arn" {
  value = aws_iam_role.ecs_execution.arn
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
