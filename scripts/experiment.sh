#!/bin/sh
set -eu
kind=${1:-latency}
api=http://127.0.0.1:8474
curl -fsS -X POST "$api/proxies" -H 'Content-Type: application/json' -d '{"name":"echo","listen":"0.0.0.0:18666","upstream":"host.docker.internal:18082"}' >/dev/null || true
curl -fsS -X DELETE "$api/proxies/echo/toxics/fault" >/dev/null 2>&1 || true
case "$kind" in
  latency) toxic='{"name":"fault","type":"latency","stream":"downstream","attributes":{"latency":800,"jitter":100}}' ;;
  bandwidth) toxic='{"name":"fault","type":"bandwidth","stream":"downstream","attributes":{"rate":16}}' ;;
  timeout) toxic='{"name":"fault","type":"timeout","stream":"downstream","attributes":{"timeout":0}}' ;;
  reset) toxic='{"name":"fault","type":"reset_peer","stream":"downstream","attributes":{"timeout":0}}' ;;
  *) echo 'use latency, bandwidth, timeout, or reset' >&2; exit 2 ;;
esac
curl -fsS -X POST "$api/proxies/echo/toxics" -H 'Content-Type: application/json' -d "$toxic" >/dev/null
./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.fault.FaultMain -Dexec.args=client
