#!/usr/bin/env bash

aws --endpoint-url=http://localhost:4572 s3 cp ./sample.txt s3://cs-dev-untrusted-s3/ --metadata '{"originalName":"sample.txt"}'
aws --endpoint-url=http://localhost:4572 s3 cp ./sample.rtf s3://cs-dev-untrusted-s3/ --metadata '{"originalName":"sample.rtf"}'
aws --endpoint-url=http://localhost:4572 s3 cp ./sample.doc s3://cs-dev-untrusted-s3/ --metadata '{"originalName":"sample.doc"}'
aws --endpoint-url=http://localhost:4572 s3 cp ./sample.docx s3://cs-dev-untrusted-s3/ --metadata '{"originalName":"sample.docx"}'
aws --endpoint-url=http://localhost:4572 s3 cp ./sample.html s3://cs-dev-untrusted-s3/ --metadata '{"originalName":"sample.html"}'
aws --endpoint-url=http://localhost:4572 s3 cp ./sample.test s3://cs-dev-untrusted-s3/ --metadata '{"originalName":"sample.test"}'

sleep 2

aws --endpoint-url=http://localhost:4576 sqs send-message --queue-url http://localhost:4576/queue/cs-dev-document-sqs \
 --message-body '{"externalReferenceUUID":"4321","documentDisplayName":"sample.txt","documentUUID":"txt.dummyId","fileLink":"sample.txt"}'

aws --endpoint-url=http://localhost:4576 sqs send-message --queue-url http://localhost:4576/queue/cs-dev-document-sqs \
 --message-body '{"externalReferenceUUID":"4321","documentDisplayName":"sample.rtf","documentUUID":"rtf.dummyId","fileLink":"sample.rtf"}'

aws --endpoint-url=http://localhost:4576 sqs send-message --queue-url http://localhost:4576/queue/cs-dev-document-sqs \
 --message-body '{"externalReferenceUUID":"4321","documentDisplayName":"sample.doc","documentUUID":"doc.dummyId","fileLink":"sample.doc"}'

aws --endpoint-url=http://localhost:4576 sqs send-message --queue-url http://localhost:4576/queue/cs-dev-document-sqs \
 --message-body '{"externalReferenceUUID":"4321","documentDisplayName":"sample.docx","documentUUID":"docx.dummyId","fileLink":"sample.docx"}'

aws --endpoint-url=http://localhost:4576 sqs send-message --queue-url http://localhost:4576/queue/cs-dev-document-sqs \
 --message-body '{"externalReferenceUUID":"4321","documentDisplayName":"sample.html","documentUUID":"html.dummyId","fileLink":"sample.html"}'
