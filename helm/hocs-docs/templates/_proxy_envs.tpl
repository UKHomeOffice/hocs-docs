{{- define "proxy.envs" }}
- name: HTTP2
  value: 'TRUE'
- name: PROXY_SERVICE_HOST
  value: '127.0.0.1'
- name: PROXY_SERVICE_PORT
  value: "{{ .Values.app.port }}"
- name: NAXSI_USE_DEFAULT_RULES
  value: 'FALSE'
- name: ENABLE_UUID_PARAM
  value: 'FALSE'
- name: HTTPS_REDIRECT
  value: 'FALSE'
- name: BASIC_AUTH
  value: /etc/nginx/authsecrets/htpasswd
- name: SERVER_CERT
  value: /certs/tls.pem
- name: SERVER_KEY
  value: /certs/tls-key.pem
- name: CLIENT_MAX_BODY_SIZE
  value: '52'
{{- end -}}
