output "ec2_public_ip" {
  description = "EC2 고정 퍼블릭 IP (GitHub Actions EC2_HOST에 입력)"
  value       = aws_eip.app.public_ip
}

output "ec2_ssh_command" {
  description = "SSH 접속 명령어"
  value       = "ssh -i <키파일.pem> ubuntu@${aws_eip.app.public_ip}"
}

output "s3_bucket_name" {
  description = "S3 버킷 이름"
  value       = aws_s3_bucket.app.bucket
}

output "github_actions_access_key_id" {
  description = "GitHub Actions용 AWS_ACCESS_KEY (Secrets에 등록)"
  value       = aws_iam_access_key.github_actions.id
}

output "github_actions_secret_access_key" {
  description = "GitHub Actions용 AWS_SECRET_KEY (Secrets에 등록)"
  value       = aws_iam_access_key.github_actions.secret
  sensitive   = true
}
