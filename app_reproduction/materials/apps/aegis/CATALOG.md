# Aegis 复现补充素材

本目录是 `aegis`（双因素认证令牌 App）复现工作区的目标专属素材包，挂载为
只读的 `/materials/app`。所有条目均为虚构测试数据，不包含任何真实账号。

## 实体素材

- `otpauth_uris.txt`：12 条虚构的 otpauth:// 迁移 URI，覆盖不同服务商、
  账号与分组，可直接作为“扫描导入/手动输入”演示数据。
- `entries.json`：与上表对应的条目结构化数据（名称/服务商/分组/密钥/位数/
  周期），适合直接嵌入工程或生成 UI 列表。
- 条目图标可复用 `/materials/common/images/avatars/` 的虚构头像。

## 非实体补充信息

见 `SUPPLEMENT.md`：TOTP 概念、otpauth:// URI 规范、Base32 密钥格式、
验证码行为（位数/周期/倒计时）等复现必要信息。

## 使用规则

不要修改本目录。需要使用时将文件复制进 `/workspace` 工程内再引用。
