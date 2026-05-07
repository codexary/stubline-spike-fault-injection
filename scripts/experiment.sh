#!/bin/sh
set -eu
kind=${1:-latency}
api=http://127.0.0.1:8474
# Recreate the proxy so every run has an empty toxic set, even after interruption.
if curl -fsS "$api/proxies/echo" >/dev/null 2>&1; then
  curl -fsS -X DELETE "$api/proxies/echo" >/dev/null
fi
curl -fsS -X POST "$api/proxies" -H 'Content-Type: application/json' -d '{"name":"echo","listen":"0.0.0.0:18666","upstream":"host.docker.internal:18082"}' >/dev/null
case "$kind" in
  latency) toxic='{"name":"fault","type":"latency","stream":"downstream","attributes":{"latency":800,"jitter":100}}' ;;
  bandwidth) toxic='{"name":"fault","type":"bandwidth","stream":"downstream","attributes":{"rate":16}}' ;;
  timeout) toxic='{"name":"fault","type":"timeout","stream":"downstream","attributes":{"timeout":0}}' ;;
  reset) toxic='{"name":"fault","type":"reset_peer","stream":"downstream","attributes":{"timeout":0}}' ;;
  *) echo 'use latency, bandwidth, timeout, or reset' >&2; exit 2 ;;
esac
curl -fsS -X POST "$api/proxies/echo/toxics" -H 'Content-Type: application/json' -d "$toxic" >/dev/null
printf 'active toxics: '
curl -fsS "$api/proxies/echo/toxics"
printf '\n'
./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.fault.FaultMain -Dexec.args=client
