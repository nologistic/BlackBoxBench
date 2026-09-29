# Reusable materials API

Start:

```powershell
python backend/server.py --port 7900
```

Static assets live under `/assets/...`, e.g. `/assets/images/products/camera.png`.

Main endpoints:

- `GET /api/health`、`GET /api/site`
- `POST /api/auth/login`、`GET|PATCH /api/auth/me`
- `GET /api/users`、`GET /api/users/{username}`
- `GET /api/products`、`GET /api/products/{id}`
- `GET|POST /api/cart`、`DELETE /api/cart/{product_id}`、`POST /api/checkout`
- `GET /api/articles`, `GET /api/articles/{slug}`, article comments
- `GET|POST /api/posts`, post comments/likes, `POST /api/follows/{username}`
- `GET|POST /api/messages`、`GET /api/notifications`、`GET /api/orders`
- `GET /api/media`

Authenticated endpoints use `Authorization: Bearer <token>`. Test accounts all use password
`demo123`, e.g. `linxi/demo123`. All data is fictional material.

After copying into `website_output/`, `data/library.db` is the writable database for the website,
and `seed/library.db` is the initial copy. Run
`python backend/server.py --reset` to reset the data before starting.
