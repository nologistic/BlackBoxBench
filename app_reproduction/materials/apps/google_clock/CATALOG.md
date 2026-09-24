# Google Clock 复现补充素材

本目录是 `google_clock` 复现工作区的目标专属素材包，挂载为只读的
`/materials/app`。所有内容均为虚构与公开规范。

## 实体素材

- `world_clocks.json`：24 个虚构友好城市与 UTC 偏移表，覆盖整点与
  半点时区，用于世界时钟/家乡时间功能。

## 非实体补充信息

见 `SUPPLEMENT.md`：重复闹钟的摘要文案生成规则、下一次触发计算、
世界时钟与家乡时间语义、计时器/秒表行为规范。

## 使用规则

不要修改本目录。需要使用时将文件复制进 `/workspace` 工程内再引用；
闹钟铃声可复用 `/materials/mobile/audio/` 的 WAV 样本。
