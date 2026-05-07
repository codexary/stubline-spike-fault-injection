English | [简体中文](FINDINGS.zh-CN.md)

# Findings

## What we observed

The Toxiproxy scripts apply latency, bandwidth, timeout and reset separately to a local HTTP echo server. The client reports response status, byte count, elapsed time or exception. The README records roughly 800 ms added latency, a 64 KiB body limited to 16 KiB/s, a three-second client timeout, and a reset that closes the connection. The Docker test exercises the proxy path.

## Backend decision

These failures have different client observations. A bounded payment-provider call must distinguish a definite response from a missing reply; a timeout or reset alone cannot establish whether a charge was executed. See [backend ADR 0005 — Bound payment provider calls](https://github.com/codexary/stubline-backend/blob/main/docs/adr/0005-bound-payment-provider-calls.md).
