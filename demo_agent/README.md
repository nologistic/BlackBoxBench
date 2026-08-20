# Demo Exploration Agent

`run_demo.py` 是一个**脚本化校准策略**(scripted calibration policy):
人工看过一次 Reference App,把坐标写死(依据 `sample_apps/ecommerce_demo/LAYOUT.md`
与实际帧校准)。它的用途是验证 Benchmark 基础设施的完整链路:

```text
observe(像素) → 坐标动作 → evidence 校验的发现写入 → 增量拓扑 → finalize
```

它**不是**视觉推理能力的演示。它通过与其他任何 Agent 完全相同的
`/agent/{sid}/...` 通道工作,不接触 App 内部。

## 运行

```bash
# 先启动 controller
vendor/python/python.exe -m benchmark.server --port 7800
# 再跑 demo(会自己创建 session)
vendor/python/python.exe -m demo_agent.run_demo --controller http://127.0.0.1:7800
```

## 它做了什么(97 个动作)

1. 观察匿名首页 → `STATE: Anonymous home`
2. 搜索 "camera" → `FEATURE: Search products`;再搜 "zzzz" 验证空结果错误路径
3. 分类筛选 Electronics → `FEATURE: Filter products by category`
4. 进入商品详情 → `STATE: Product detail` + `FEATURE: View product detail`
5. 数量 1999 超库存 → 错误条(error case);改 2 → `FEATURE: Add to cart` + `MUTATES Cart`
6. 数量改 3 成功;改 0 被拒(error case)→ `FEATURE: Update quantity`
7. 无效券 BOGUS → 错误条;有效券 SAVE10 → 折扣行 → `FEATURE: Apply coupon`
8. **假设**: "购物车刷新后保留" → F5 探针 → confirmed + `PERSISTS_TO` 边
9. 匿名点 Checkout → 被重定向登录页 → `checkout REQUIRES authenticated`(先假设后证实)
10. 错误密码 → 错误条;正确凭证 → `FEATURE: Log in` + `TRANSITIONS_TO Authenticated home`
11. 空表单下单 → 校验错误;填表 → 下单 → `STATE: Order confirmation` + `MUTATES Order/Cart`
12. Orders 页(含 seed 历史单与新建单)→ Logout → 回匿名首页

产物: `runs/<session_id>/functional_topology.json` + `.md` 等全套 artifacts。

## 接入真实 VLM Agent

把 `run_demo.py` 的脚本段换成你的推理循环即可,SDK 调用完全一致:

```python
obs = env.observe()                    # PNG
action = my_vlm.decide(obs.screenshot_png, history)   # 你的模型
result = getattr(env, action.name)(**action.args)     # click/type/...
env.record_feature(..., evidence=[{"step": result.step, ...}])
```

详见 `docs/running_agent.md`。
