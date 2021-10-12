#!/usr/bin/env bash
set -e
export TERM=ansi
export AWS_ACCESS_KEY_ID=foobar
export AWS_SECRET_ACCESS_KEY=foobar
export AWS_DEFAULT_REGION=eu-west-2
export PAGER=

aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name document-queue
aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name document-dlq
aws --endpoint-url=http://localhost:4576 sqs set-queue-attributes --queue-url "http://localhost:4576/queue/document-queue" --attributes '{"RedrivePolicy":"{\"maxReceiveCount\":\"3\", \"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:document-dlq\"}"}'

aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name audit-queue

aws --endpoint-url=http://localstack:4575 sns create-topic --name hocs-audit-topic
aws --endpoint-url=http://localstack:4575 sns subscribe --topic-arn arn:aws:sns:eu-west-2:123456789012:hocs-audit-topic --attributes RawMessageDelivery=true --protocol sqs --notification-endpoint arn:aws:sns:eu-west-2:123456789012:audit-queue

aws --endpoint-url=http://localstack:4572 s3 mb s3://untrusted-bucket
aws --endpoint-url=http://localstack:4572 s3 mb s3://trusted-bucket