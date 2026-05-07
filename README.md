English | [简体中文](README.zh-CN.md)

Compose binds Toxiproxy control and proxy ports to loopback so other hosts cannot change the experiment.

# Toxiproxy fault injection

Run `./mvnw -q verify`. Start `SPIKE_BIND_ADDRESS=0.0.0.0 ./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.fault.FaultMain -Dexec.args='server 18082'` in one terminal and `docker compose up -d` in another. Then run `./scripts/experiment.sh latency`, `bandwidth`, `timeout`, or `reset`. The client prints HTTP status, bytes, elapsed milliseconds or the exception. Latency adds roughly 800 ms with jitter (timing failure); bandwidth caps the 64 KiB response at 16 KiB/s (timing failure); timeout causes a three-second client timeout (omission); reset closes the connection, resembling a crashed peer (crash/omission). Toxics are replaced between experiments. Run `docker compose down` when finished. Docker-tagged Testcontainers check: `./mvnw -q -Pdocker verify`.

The echo server defaults to loopback. The Docker proxy reaches the host through host.docker.internal, so this example explicitly opts into a wider bind; run it only on a trusted local network.

## License

This repository is licensed under the [MIT License](LICENSE).

## About this repository

This repository contains a focused Stubline experiment. The README documents its commands and observed results.

The code and documents in this repository were written with the help of AI tools; every recorded output comes from actually running them.

## Findings

The experiment results and their effect on the backend decision are recorded in [FINDINGS.md](FINDINGS.md).
