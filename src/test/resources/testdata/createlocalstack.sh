#!/usr/bin/env bash

aws --endpoint-url=http://localhost:4576 sqs create-queue --queue-name cs-dev-document-sqs
aws --endpoint-url=http://localhost:4572 s3 mb s3://cs-dev-untrusted-s3
aws --endpoint-url=http://localhost:4572 s3 mb s3://cs-dev-trusted-s3