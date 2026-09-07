# 复现工程周记

> 每周五更新，记录本周进展与下周计划。

## 本周进展

| 日期 | 事项 | 状态 |
|---|---|---|
| 周一 | 沙盒素材结构定稿 | 完成 |
| 周三 | 网络策略改为 skill 纪律 | 完成 |
| 周五 | 评测清单转换脚本 | 进行中 |

## 关键代码

```
fun nextOccurrence(rule: String, today: LocalDate): LocalDate {
    // 从今天起向后找第一个符合规则的日期
    return generateSequence(today) { it.plusDays(1) }
        .first { matches(rule, it) }
}
```

## 待办

- [x] 提交素材包清单评审
- [ ] 补充电子书样例
- [ ] 撰写评审 few-shot

## 图片引用

示例图片（应用内资源）：

![湖边](./lake.png)

---

标签：`工作` `周记`
