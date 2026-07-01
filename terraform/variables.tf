variable "aws_region" {
  default = "ap-northeast-2"
}

variable "project_name" {
  default = "deoham"
}

variable "ec2_instance_type" {
  default = "t3.micro"
}

variable "key_pair_name" {
  description = "AWS EC2 Key Pair 이름 (콘솔에서 미리 생성 후 입력)"
  type        = string
}

variable "s3_bucket_name" {
  default = "ondo-2026-itstime"
}
