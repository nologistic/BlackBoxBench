# Feeder 复现补充素材

本目录是 `feeder`（RSS/Atom/JSON 订阅阅读器）复现工作区的目标专属素材包，
挂载为只读的 `/materials/app`。所有条目均为虚构测试数据。

## 实体素材

- `feeds/blackbox_times.rss`：RSS 2.0 格式的虚构资讯源（5 条）。
- `feeds/tech_digest.atom`：Atom 格式的虚构技术源（4 条）。
- `feeds/design_notes.json`：JSON Feed 1.1 格式的虚构设计源（4 条）。
- `subscriptions.opml`：包含上述三个源与两个额外虚构源的订阅列表，
  可直接用于“OPML 导入”演示。

## 非实体补充信息

见 `SUPPLEMENT.md`：三种 Feed 格式的结构要点与统一的 Feed/Article 数据
模型、OPML 格式、同步与“新文章”判定规则、图片字段来源。

## 使用规则

不要修改本目录。需要使用时将文件复制进 `/workspace` 工程内再引用；
文章配图可复用 `/materials/common/images/covers/` 与 `posts/` 的虚构图片。
