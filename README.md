# HOCS Document Service

## Dependencies
 - AWS SQS queues (document input and case output) or localstack
 - AWS S3 buckets (untrusted and trusted)
 - HOCS Converter service 
 - ClamAV REST service
 
 ## Developing locally
 
 The service will run against localstack using the `local` Spring profile and the docker-compose services in `docker/docker-compose.yml`
 
 

  
 To start local instances run `./docker/docker-compose up`.
 
