locals {
  queue_name = "ticketing-queue-${var.env}"
  dlq_name   = "ticketing-dlq-${var.env}"
}
