# 可复用素材 API

启动：

```powershell
python backend/server.py --port 7900
```

静态素材位于 `/assets/...`，例如 `/assets/images/products/camera.png`。

主要接口：

- `GET /api/health`、`GET /api/site`
- `POST /api/auth/login`、`GET|PATCH /api/auth/me`
- `GET /api/users`、`GET /api/users/{username}`
- `GET /api/products`、`GET /api/products/{id}`
- `GET|POST /api/cart`、`DELETE /api/cart/{product_id}`、`POST /api/checkout`
- `GET /api/articles`、`GET /api/articles/{slug}`、文章评论
- `GET|POST /api/posts`、动态评论/点赞、`POST /api/follows/{username}`
- `GET|POST /api/messages`、`GET /api/notifications`、`GET /api/orders`
- `GET /api/media`

需要登录的接口使用 `Authorization: Bearer <token>`。测试账号均使用密码
`demo123`，例如 `linxi/demo123`。所有数据均为虚构素材。

复制到 `website_output/` 后，`data/library.db` 是网页的可写数据库，
`seed/library.db` 是初始副本。运行
`python backend/server.py --reset` 可在启动前复位数据。
