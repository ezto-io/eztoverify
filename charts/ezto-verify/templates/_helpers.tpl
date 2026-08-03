{{/*
Expand the name of the chart.
*/}}
{{- define "ezto-verify.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
Truncated at 63 chars because Kubernetes name fields are limited to this
(by the DNS naming spec).
*/}}
{{- define "ezto-verify.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Common labels. Call as: {{ include "ezto-verify.labels" (dict "root" . "component" "idp") }}
*/}}
{{- define "ezto-verify.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .root.Chart.Name .root.Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{ include "ezto-verify.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .root.Release.Service }}
{{- end -}}

{{/*
Selector labels. Call as: {{ include "ezto-verify.selectorLabels" (dict "root" . "component" "idp") }}
*/}}
{{- define "ezto-verify.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ezto-verify.name" .root }}
app.kubernetes.io/instance: {{ .root.Release.Name }}
app.kubernetes.io/component: {{ .component }}
{{- end -}}

{{/*
Public scheme: https when ingress TLS is enabled, else http.
*/}}
{{- define "ezto-verify.publicScheme" -}}
{{- if .Values.ingress.tls.enabled -}}
https
{{- else -}}
http
{{- end -}}
{{- end -}}

{{/*
Public base URL: derived from ingress host+scheme when ingress is enabled,
otherwise idp.localUrl verbatim (local port-forward case).
*/}}
{{- define "ezto-verify.publicUrl" -}}
{{- if .Values.ingress.enabled -}}
{{- printf "%s://%s" (include "ezto-verify.publicScheme" .) .Values.ingress.host -}}
{{- else -}}
{{- .Values.idp.localUrl -}}
{{- end -}}
{{- end -}}

{{/*
idp is pinned to /auth (Keycloak 18 fork context path). MUST end in /auth.
*/}}
{{- define "ezto-verify.idpUrl" -}}
{{- printf "%s/auth" (include "ezto-verify.publicUrl" .) -}}
{{- end -}}

{{/*
Postgres host: bundled service name, or external.host (required when not bundled).
*/}}
{{- define "ezto-verify.pgHost" -}}
{{- if .Values.postgres.enabled -}}
{{- printf "%s-postgres" (include "ezto-verify.fullname" .) -}}
{{- else -}}
{{- required "postgres.external.host is required when postgres.enabled is false" .Values.postgres.external.host -}}
{{- end -}}
{{- end -}}

{{/*
MinIO base URL: bundled service, or external.url (required when not bundled).
*/}}
{{- define "ezto-verify.minioUrl" -}}
{{- if .Values.minio.enabled -}}
{{- printf "http://%s-minio:9000" (include "ezto-verify.fullname" .) -}}
{{- else -}}
{{- required "minio.external.url is required when minio.enabled is false" .Values.minio.external.url -}}
{{- end -}}
{{- end -}}

{{/*
Elasticsearch host: bundled service name, or external.host (required when not bundled).
*/}}
{{- define "ezto-verify.esHost" -}}
{{- if .Values.elasticsearch.enabled -}}
{{- printf "%s-elasticsearch" (include "ezto-verify.fullname" .) -}}
{{- else -}}
{{- required "elasticsearch.external.host is required when elasticsearch.enabled is false" .Values.elasticsearch.external.host -}}
{{- end -}}
{{- end -}}

{{/*
Elasticsearch port: bundled default 9200, or external.port.
*/}}
{{- define "ezto-verify.esPort" -}}
{{- if .Values.elasticsearch.enabled -}}
9200
{{- else -}}
{{- .Values.elasticsearch.external.port -}}
{{- end -}}
{{- end -}}

{{/*
Elasticsearch scheme: bundled default http, or external.scheme.
*/}}
{{- define "ezto-verify.esScheme" -}}
{{- if .Values.elasticsearch.enabled -}}
http
{{- else -}}
{{- .Values.elasticsearch.external.scheme -}}
{{- end -}}
{{- end -}}

{{/*
Per-service image pull policy override, falling back to the chart-wide default.
Call as: {{ include "ezto-verify.imagePullPolicy" (dict "root" . "policy" .Values.idp.image.pullPolicy) }}
*/}}
{{- define "ezto-verify.imagePullPolicy" -}}
{{- if .policy -}}
{{- .policy -}}
{{- else -}}
{{- .root.Values.image.pullPolicy -}}
{{- end -}}
{{- end -}}

{{/*
Whether the SSL-init Job runs: explicit idp.sslInitJob.enabled string, else
auto-enabled when ingress TLS termination is not handling SSL for us.
*/}}
{{- define "ezto-verify.sslInitEnabled" -}}
{{- $v := .Values.idp.sslInitJob.enabled -}}
{{/* `and` short-circuits (Go text/template ≥1.18), so `eq $v ""` is only
     evaluated once $v is already known to be a string — safe against the
     bool/string comparison panic when $v is a real boolean override. */}}
{{- if and (kindIs "string" $v) (eq $v "") -}}
{{- not .Values.ingress.tls.enabled -}}
{{- else -}}
{{- $v -}}
{{- end -}}
{{- end -}}
