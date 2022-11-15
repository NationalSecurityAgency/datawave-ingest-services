{{/*
Expand the name of the chart.
*/}}
{{- define "ingest.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "ingest.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "ingest.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "ingest.labels" -}}
helm.sh/chart: {{ include "ingest.chart" . }}
{{ include "ingest.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "ingest.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ingest.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "ingest.serviceAccountName" -}}
{{- if .Values.configuration.serviceAccount.create }}
{{- default (include "ingest.fullname" .) .Values.configuration.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.configuration.serviceAccount.name }}
{{- end }}
{{- end }}

{{- define "ingest.messaging.serviceAccountName" -}}
{{- if .Values.messaging.serviceAccount.create }}
{{- default (include "ingest.fullname" .) .Values.messaging.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.messaging.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Volume Mounts
*/}}
{{- define "ingest.volumeMount" -}}
- mountPath: {{ .destination }}
  name: {{ .name }}
  {{- if .readOnly }}
  readOnly: true
  {{- end }}
{{- end }}

{{/*
Volumes
*/}}
{{- define "ingest.volume" -}}
- name: {{ .name }}
  {{- if eq .source.type "hostPath" }}
  hostPath:
    path: {{ .source.path }}
    type: Directory
  {{- else if eq .source.type "configmap" }}
  configMap:
    name: {{ .source.name }}
  {{- else if eq .source.type "secret" }}
  secret:
    secretName: {{ .source.name }}
    optional: false
    {{- if .source.keys }}
    items:
      {{- range .source.keys }}
      - key: {{ .name }}
        path: {{ .path }}
      {{- end }}
    {{- end }}
  {{- else }}
  emptyDir: {}
  {{- end }}
{{- end }}