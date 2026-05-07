[English](FINDINGS.md) | 简体中文

# 实验结论

## 观察

Toxiproxy 脚本分别对本地 HTTP echo 服务施加延迟、限速、超时和连接重置。客户端报告状态码、字节数、耗时或异常。README 记录了约 800 毫秒的额外延迟、把 64 KiB 响应限制到 16 KiB/s、三秒客户端超时，以及连接被直接关闭的情况。Docker 测试覆盖代理路径。

## 对后端的影响

这些故障在客户端呈现不同的结果。后端调用支付方时要设置时间上限，并区分明确的响应与未收到回复；仅凭超时或连接重置，无法判断是否已经扣款。参见[后端 ADR 0005：Bound payment provider calls](https://github.com/codexary/stubline-backend/blob/main/docs/adr/0005-bound-payment-provider-calls.md)。
