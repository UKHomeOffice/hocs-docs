{{- define "deployment.envs" }}
- name: JAVA_OPTS
  value: '{{ tpl .Values.app.env.javaOpts . }}'
{{- if not .Values.proxy.enabled }}
- name: SERVER_SSL_KEY_STORE_TYPE
  value: 'PKCS12'
- name: SERVER_SSL_KEY_STORE_PASSWORD
  value: 'changeit'
- name: SERVER_SSL_KEY_STORE
  value: 'file:/etc/keystore/keystore.jks'
- name: SERVER_COMPRESSION_ENABLED
  value: 'true'
- name: SERVER_SSL_ENABLED
  value: 'true'
{{- end }}
- name: SERVER_PORT
  value: '{{ include "hocs-app.port" . }}'
- name: SPRING_PROFILES_ACTIVE
  value: '{{ tpl .Values.app.env.springProfiles . }}'
- name: CLAMAV_ROOT
  value: '{{ tpl .Values.app.env.clamAvRoot . }}'
- name: HOCSCONVERTER_ROOT
  value: '{{ tpl .Values.app.env.converterRoot . }}'
- name: DB_HOST
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-docs-rds
      key: host
- name: DB_PORT
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-docs-rds
      key: port
- name: DB_NAME
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-docs-rds
      key: name
- name: DB_SCHEMA_NAME
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-docs-rds
      key: schema_name
- name: DB_USERNAME
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-docs-rds
      key: user_name
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-docs-rds
      key: password
- name: DOCS_QUEUE_NAME
  value: '{{ tpl .Values.app.env.docsQueueName . }}'
- name: DOCS_QUEUE_DLQ_NAME
  value: '{{ tpl .Values.app.env.docsQueueNameDlq . }}'
- name: DOCS_AWS_SQS_ACCESS_KEY
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-document-sqs
      key: access_key_id
- name: DOCS_AWS_SQS_SECRET_KEY
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-document-sqs
      key: secret_access_key
- name: DOCS_TRUSTEDS3BUCKETNAME
  value: '{{ tpl .Values.app.env.trustedBucketName . }}'
- name: DOCS_UNTRUSTEDS3BUCKETNAME
  value: '{{ tpl .Values.app.env.untrustedBucketName . }}'
- name: TRUSTED_AWS_S3_ACCESS_KEY
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-trusted-s3
      key: access_key_id
- name: TRUSTED_AWS_S3_SECRET_KEY
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-trusted-s3
      key: secret_access_key
- name: DOCS_TRUSTEDS3BUCKETKMSKEYID
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-trusted-s3
      key: kms_key_id
- name: UNTRUSTED_AWS_S3_ACCESS_KEY
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-untrusted-s3
      key: access_key_id
- name: UNTRUSTED_AWS_S3_SECRET_KEY
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-untrusted-s3
      key: secret_access_key
- name: AUDIT_TOPIC_NAME
  value: '{{ tpl .Values.app.env.auditTopicName . }}'
- name: AUDIT_AWS_SNS_ACCESS_KEY
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-audit-sqs
      key: access_key_id
- name: AUDIT_AWS_SNS_SECRET_KEY
  valueFrom:
    secretKeyRef:
      name: {{ .Release.Namespace }}-audit-sqs
      key: secret_access_key
- name: AUDITING_DEPLOYMENT_NAME
  valueFrom:
    fieldRef:
      fieldPath: metadata.name
- name: AUDITING_DEPLOYMENT_NAMESPACE
  valueFrom:
    fieldRef:
      fieldPath: metadata.namespace
{{- end -}}
