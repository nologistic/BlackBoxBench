# Feeder 复现补充信息（非实体素材）

## 三种 Feed 格式 → 统一数据模型

Feeder 的核心能力是把三种格式统一转换为相同的 Feed/Article 模型，
在列表与阅读器中表现一致。对应字段：

| 模型字段 | RSS 2.0 | Atom | JSON Feed 1.1 |
|---|---|---|---|
| 订阅源标题 | `channel/title` | `feed/title` | `title` |
| 订阅源描述 | `channel/description` | `feed/subtitle` | `description` |
| 条目标题 | `item/title` | `entry/title` | `items[].title` |
| 条目链接 | `item/link` | `entry/link@href` | `items[].url` |
| 发布日期 | `item/pubDate`（RFC 822） | `entry/updated`（RFC 3339） | `items[].date_published`（RFC 3339） |
| 正文 | `item/description`（HTML） | `entry/content` 或 `summary` | `items[].content_html` |
| 唯一标识 | `item/guid` | `entry/id` | `items[].id` |

- 日期解析注意两种格式：RFC 822（`Mon, 07 Sep 2026 08:00:00 GMT`）与
  RFC 3339（`2026-09-07T08:00:00Z`）。
- 正文按 HTML 渲染（标题/段落/图片/链接），无法识别的标签降级为纯文本。

## OPML 订阅列表

- 导入：解析 `<outline type="rss" text="显示名" xmlUrl="订阅地址"/>`，
  每个 outline 建立一个订阅。
- 导出：当前全部订阅写回同样结构；导入导出 round-trip 后订阅集合不变。

## 同步与“新文章”判定

- 手动刷新：立即拉取全部订阅；下拉列表或按钮触发。
- 定时同步：每小时/每天等周期选项，可叠加“仅 Wi-Fi”“仅充电时”条件，
  条件不满足时跳过本轮。
- **新文章判定**：条目唯一标识（guid/id）此前未见过 → 新文章；已见过的
  标识即使内容更新也不再算“新”；关闭某订阅的通知后仍会同步文章，
  只是不产生通知。
- 已读/未读：进入阅读即标记已读；列表可按未读过滤。

## 离线行为

- 成功同步过的 Feed 与文章缓存到本地，断网后仍可完整阅读。
- 断网时手动刷新应显示明确的失败提示，不清空已有缓存。

## 复现行为要点

- 添加订阅：输入地址 → 拉取解析 → 出现在左侧订阅列表（标题 + 未读数）。
- 三种格式混在一个订阅列表中，列表与阅读器的交互完全一致。
- 文章列表按时间倒序；未读条目有视觉标记；点击进入阅读页可返回。
- OPML 导入后订阅列表新增对应条目；导出的文件可再导入还原。
