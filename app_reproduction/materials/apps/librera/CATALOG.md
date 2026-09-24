# Librera Reader 复现补充素材

本目录是 `librera`（电子书阅读器）复现工作区的目标专属素材包，
挂载为只读的 `/materials/app`。所有内容均为虚构测试数据。

## 实体素材

- `library.json`：虚构书库（12 本书，覆盖 EPUB/PDF/FB2/MOBI 四种
  格式、多作者、不同阅读进度与标签），是书库功能的**必要数据**。
- `sample_book.json`：一本虚构 EPUB 的章节结构与正文段落样例，
  阅读器渲染可直接以此组织内容。

## 非实体补充信息

见 `SUPPLEMENT.md`：格式支持矩阵、书库扫描与「文件夹作为书籍」、
阅读模式与翻页、书签/高亮、TTS、配置 Profile、备份迁移。

## 使用规则

不要修改本目录。需要使用时将文件复制进 `/workspace` 工程内再引用；
书籍封面可复用 `/materials/mobile/images/covers/` 的虚构封面图。
