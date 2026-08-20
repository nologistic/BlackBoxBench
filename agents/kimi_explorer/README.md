# Kimi VLM Explorer Agent

这是**真实模型驱动**的探索 Agent:把 `observe()` 的 PNG 截图喂给 Kimi 多模态模型,
模型输出结构化 JSON 决策(thought + action + discoveries),harness 执行并校验证据,
运行过程在 harness 控制台实时输出,画面在 Dashboard 实时可见。

与 `demo_agent/` 的区别: demo 是坐标写死的脚本,用于验证基础设施;这里的每一步
都由模型看截图后自行决定。

## 快速开始

```powershell
# 终端 1: 启动 controller
vendor/python/python.exe -m benchmark.server --port 7800

# 终端 2: 提供 key 并运行
$env:BBB_VLM_API_KEY="sk-..."        # Kimi Code Console 创建的 API Key
vendor/python/python.exe -m agents.kimi_explorer.run
```

完成后 artifacts 在 `runs/<session_id>/`(拓扑、轨迹、视频等全套)。

## 配置(全部环境变量,密钥不落盘)

| 变量 | 默认 | 说明 |
|---|---|---|
| `BBB_VLM_API_KEY` | (必填) | Kimi Code Console 的 API Key;也读 `KIMI_API_KEY` |
| `BBB_VLM_BASE_URL` | `https://api.kimi.com/coding/v1` | Kimi Code 会员端点(OpenAI 兼容)。可换 Kimi Platform `https://api.moonshot.cn/v1` |
| `BBB_VLM_MODEL` | `k3` | 模型 ID。Kimi Platform 侧可换视觉模型 |
| `BBB_VLM_MAX_TOKENS` | 4096 | |
| `BBB_VLM_TEMPERATURE` | (不下发) | k3 仅允许 temperature=1;默认不传该参数,需要时显式设置 |
| `BBB_VLM_HISTORY_IMAGES` | 6 | 上下文里保留的最近截图数(更早的降级为文本) |

CLI 参数: `--controller` `--app-id` `--session`(接入已有会话)
`--max-turns 250` `--max-actions 400` `--minutes 30`。

## 工作方式

```text
observe(PNG)
  → prompt = 系统规则 + 截图 + 预算/最近动作/拓扑摘要/未决问题
  → Kimi 输出 JSON: {thought, action, discoveries[], done}
  → thought 打印到控制台(harness 自身的运行输出)
  → env.<action>(...)           → 像素级执行
  → env.record_*(discoveries)   → evidence 自动绑定真实 step/帧
  → 循环直至 done 或预算耗尽 → finalize
```

### 给模型的证据捷径

- discovery 里写 `"evidence": "last"` → harness 自动替换为本回合动作的
  真实 `{step, before_frame, action, after_frame}`;
- `"visual_evidence": "current"` → 自动替换为当前截图的 frame_id。

这样模型不需要自己记帧号,evidence 校验照样全部通过。

### 健壮性

- 非 JSON 输出 → 自动反馈纠错重试;
- 动作/发现被拒(400/422)→ 错误原因注入下一轮 prompt 让模型修正;
- 同一动作连续重复 4 次 → 注入"换方向"提示;
- VLM API 连续失败 >8 次 → 安全终止;
- 预算耗尽/会话关闭 → 自动 finalize,保留全部已发现内容。

## 换其他 VLM

实现一个带 `decide(messages) -> str` 的对象(OpenAI chat 消息格式,图片为
base64 data URL part),传给 `Explorer(env, your_client)` 即可,无需改其他代码。

## 离线测试

`tests/test_vlm_agent.py` 用脚本化 MockVLM 跑通整个 harness(无需 key)。
