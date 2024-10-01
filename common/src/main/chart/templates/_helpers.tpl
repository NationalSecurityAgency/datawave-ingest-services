{{/*
Volume Mounts
*/}}
{{- define "common.volumeMount" -}}
- mountPath: {{ .destination }}
  name: {{ .name }}
  {{- if .readOnly }}
  readOnly: true
  {{- end }}
{{- end }}

{{/*
Volumes
*/}}
{{- define "common.volume" -}}
- name: {{ .name }}
  {{- if eq .source.type "hostPath" }}
  hostPath:
    path: {{ .source.path }}
    type: Directory
  {{- else if eq .source.type "configmap" }}
  configMap:
    name: {{ .source.name }}
  {{- else if eq .source.type "projected" }}
  projected:
    sources:
    {{- range .source.maps }}
      - configMap:
          name: {{ . }}
    {{- end }}
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