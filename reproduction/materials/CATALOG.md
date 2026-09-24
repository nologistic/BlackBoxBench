# 素材目录

此目录是复现 Agent 的公共只读素材库。所有人物、品牌、内容和账号均为虚构测试数据。

## 可读内容

- `source.json`：结构化原始内容，包括用户、商品、文章、动态、评论、消息和订单。
- `generated/source.json`：构建时使用的规范化副本。
- `generated/manifest.json`：每个生成文件的大小和 SHA-256。
- `generated/library.db`：包含全部内容的 SQLite 初始数据库。
- `generated/images/avatars/`：6 张头像。
- `generated/images/products/`：8 张商品图。
- `generated/images/covers/`：5 张文章封面。
- `generated/images/posts/`：4 张社交图片。
- `generated/images/placeholders/`：通用 hero 和空状态图片。
- `generated/audio/`：通知、成功反馈和环境音 WAV。
- `generated/videos/`：商品展示和社交循环 MP4。
- `backend/server.py`：可选 FastAPI 后端模板。
- `backend/API.md`：后端接口说明。

## 使用规则

不要直接修改本目录。需要使用时，将相应文件复制到 `website_output/`：

```text
reproduction/materials/generated/images/...  ──copy──▶ website_output/public/...
reproduction/materials/generated/library.db  ──copy──▶ website_output/data/library.db
reproduction/materials/generated/library.db  ──copy──▶ website_output/seed/library.db
reproduction/materials/backend/               ──copy──▶ website_output/backend/...
```

测试账号统一使用密码 `demo123`，例如 `linxi/demo123`。素材不规定页面布局，Agent 应
根据 Functional Topology 决定信息架构、页面和交互。
