[English](README.md) | 简体中文

Compose 将 Toxiproxy 控制端口与代理端口绑定到本机回环地址，避免其他主机修改实验。

# Toxiproxy 故障注入

运行 `./mvnw -q verify`。分别在两个终端启动 `SPIKE_BIND_ADDRESS=0.0.0.0 ./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.fault.FaultMain -Dexec.args='server 18082'` 和 `docker compose up -d`。然后运行 `./scripts/experiment.sh latency`、`bandwidth`、`timeout` 或 `reset`。客户端输出 HTTP 状态、字节数、耗时或异常。latency 增加约 800 ms 和 jitter（时序故障）；bandwidth 将 64 KiB 响应限制在 16 KiB/s（时序故障）；timeout 使客户端在三秒后超时（遗漏）；reset 关闭连接，类似对端崩溃（崩溃/遗漏）。每次实验会替换 toxic。完成后运行 `docker compose down`。Docker 标记的 Testcontainers 测试：`./mvnw -q -Pdocker verify`。

echo 服务器默认绑定本机回环地址。Docker 代理通过 host.docker.internal 访问宿主机，所以上述示例显式启用更宽的监听范围；仅在可信本地网络运行。

## 许可证

本仓库采用 [MIT License](LICENSE)。

## 关于本仓库

本仓库包含一项针对 Stubline 的独立实验。README 说明运行命令与观察结果。

本仓库的代码与文档借助 AI 工具编写；记录的输出都来自实际运行。
