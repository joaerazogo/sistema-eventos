terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region                      = "us-east-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    sqs = "http://localhost:4566"
  }
}

resource "aws_sqs_queue" "ticketing_dlq" {
  name                      = local.dlq_name
  message_retention_seconds = var.message_retention

  tags = var.tags
}

resource "aws_sqs_queue" "ticketing_queue" {
  name                       = local.queue_name
  visibility_timeout_seconds = var.visibility_timeout
  message_retention_seconds  = var.message_retention

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.ticketing_dlq.arn
    maxReceiveCount     = var.max_receive_count
  })

  tags = var.tags
}
