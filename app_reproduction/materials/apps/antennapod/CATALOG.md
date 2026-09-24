# AntennaPod 复现补充素材

本目录是 `antennapod`（播客订阅与播放）复现工作区的目标专属素材包，
挂载为只读的 `/materials/app`。所有内容均为虚构测试数据。

## 实体素材

- `podcast_feed.rss`：带 iTunes 扩展标签的虚构播客源（1 季 6 集），
  可直接用于"添加播客"演示。
- `episodes.json`：6 个单集的结构化数据（标题/时长/日期/简介/章节），
  适合直接嵌入工程。

## 非实体补充信息

见 `SUPPLEMENT.md`：播客 RSS 的 iTunes 扩展字段、单集与章节模型、
队列/下载/播放状态、订阅管理与 OPML。

## 使用规则

不要修改本目录。需要使用时将文件复制进 `/workspace` 工程内再引用；
播放用的音频样本复用 `/materials/mobile/audio/` 的三个 WAV 文件。
