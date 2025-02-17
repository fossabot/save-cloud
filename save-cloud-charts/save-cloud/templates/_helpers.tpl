{{- define "common.labels" -}}
io.kompose.service: {{ .service.name }}
version: {{ .Values.dockerTag }}
env: {{ .Values.env }}
prometheus-job: {{ .service.imageName }}
{{- end }}

{{/* Common Linux user configuration for spring-boot created containers, where user is cnb:cnb */}}
{{- define "spring-boot.securityContext" -}}
securityContext:
  runAsUser: 1000
  runAsGroup: 1000
  fsGroup: 1000
{{- end }}

{{/* Common configuration of Kubernetes related things in spring-boot */}}
{{- define "spring-boot.management" -}}
startupProbe:
  # give spring-boot enough time to start
  httpGet:
    path: /actuator/health/liveness
    port: {{ .containerPort }}
  failureThreshold: 10
  periodSeconds: 10
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: {{ .containerPort }}
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port:  {{ .containerPort }}
lifecycle:
  preStop:
    exec:
      # wait till spring finishes gracefully with `server.shutdown=graceful`
      command: ["sh", "-c", "sleep 10"]
{{- end }}

{{/* Common configuration of deployment for spring-boot microservice */}}
{{- define "spring-boot.common" -}}
image: '{{ .Values.imageRegistry }}/{{ .service.imageName }}:{{ .Values.dockerTag }}'
ports:
  - name: http
    containerPort:  {{ .service.containerPort }}
{{- end }}

{{- define "spring-boot.common.env" -}}
- name: SPRING_PROFILES_ACTIVE
  value: {{ or .service.profile .Values.profile }}
{{- end }}

{{- define "spring-boot.config-volume-mount" -}}
name: config-volume
mountPath: /home/cnb/config
{{- end }}

{{- define "spring-boot.config-volume" -}}
name: config-volume
configMap:
  name: {{ .service.name }}-config
  items:
    - key: application.properties
      path: application.properties
{{- end}}