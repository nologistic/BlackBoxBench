"""Reusable local fixture API template for generated websites."""
from __future__ import annotations

import argparse
import json
import os
import secrets
import shutil
import sqlite3
from datetime import datetime, timezone
from pathlib import Path

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, ConfigDict, Field

ROOT = Path(__file__).resolve().parent.parent
DB_PATH = Path(os.environ.get("REPRO_DB", ROOT / "data" / "library.db"))
SEED_DB = ROOT / "seed" / "library.db"
ASSETS = ROOT / "public" / "assets"


def _db() -> sqlite3.Connection:
    connection = sqlite3.connect(DB_PATH)
    connection.row_factory = sqlite3.Row
    connection.execute("PRAGMA foreign_keys=ON")
    return connection


def _rows(query: str, params: tuple = ()) -> list[dict]:
    with _db() as db:
        return [dict(row) for row in db.execute(query, params).fetchall()]


def _row(query: str, params: tuple = ()) -> dict | None:
    with _db() as db:
        value = db.execute(query, params).fetchone()
        return dict(value) if value else None


def _current_user(request: Request) -> dict:
    header = request.headers.get("authorization", "")
    if not header.lower().startswith("bearer "):
        raise HTTPException(401, "missing_bearer_token")
    token = header.split(" ", 1)[1]
    user = _row("""
        SELECT u.id, u.username, u.display_name, u.email, u.role, u.bio,
               u.location, u.avatar
        FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token=?
    """, (token,))
    if not user:
        raise HTTPException(401, "invalid_session")
    return user


class LoginRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    username: str
    password: str


class CartRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    product_id: int
    quantity: int = Field(ge=1, le=99)


class PostRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    content: str = Field(min_length=1, max_length=1000)
    media: str | None = None


class CommentRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    content: str = Field(min_length=1, max_length=500)


class ProfileRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    display_name: str = Field(min_length=1, max_length=80)
    bio: str = Field(max_length=300)
    location: str = Field(max_length=80)


class MessageRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")
    recipient_id: int
    content: str = Field(min_length=1, max_length=1000)


app = FastAPI(title="Reproduction Sandbox Fixture API", version="1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origin_regex=r"https?://(localhost|127\.0\.0\.1)(:\d+)?",
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
app.mount("/assets", StaticFiles(directory=str(ASSETS)), name="assets")


@app.get("/api/health")
def health():
    return {"ok": True, "database": DB_PATH.name}


@app.get("/api/site")
def site():
    values = _rows("SELECT key,value_json FROM site ORDER BY key")
    return {item["key"]: json.loads(item["value_json"]) for item in values}


@app.post("/api/auth/login")
def login(payload: LoginRequest):
    user = _row("""
        SELECT id,username,display_name,email,role,bio,location,avatar
        FROM users WHERE username=? AND password=?
    """, (payload.username, payload.password))
    if not user:
        raise HTTPException(401, "invalid_credentials")
    token = secrets.token_urlsafe(24)
    with _db() as db:
        db.execute("INSERT INTO sessions VALUES (?,?,?)", (
            token, user["id"], datetime.now(timezone.utc).isoformat()))
        db.commit()
    return {"token": token, "user": user}


@app.get("/api/auth/me")
def me(request: Request):
    return _current_user(request)


@app.patch("/api/auth/me")
def update_me(payload: ProfileRequest, request: Request):
    user = _current_user(request)
    with _db() as db:
        db.execute("UPDATE users SET display_name=?,bio=?,location=? WHERE id=?", (
            payload.display_name, payload.bio, payload.location, user["id"]))
        db.commit()
    return _current_user(request)


@app.get("/api/users")
def users():
    return _rows("""
        SELECT id,username,display_name,role,bio,location,avatar
        FROM users ORDER BY id
    """)


@app.get("/api/users/{username}")
def user(username: str):
    value = _row("""
        SELECT id,username,display_name,role,bio,location,avatar
        FROM users WHERE username=?
    """, (username,))
    if not value:
        raise HTTPException(404, "user_not_found")
    return value


@app.get("/api/products")
def products(q: str = "", category: str = ""):
    where, params = [], []
    if q:
        where.append("(name LIKE ? OR description LIKE ?)")
        params.extend([f"%{q}%", f"%{q}%"])
    if category:
        where.append("category=?")
        params.append(category)
    sql = "SELECT * FROM products"
    if where:
        sql += " WHERE " + " AND ".join(where)
    return _rows(sql + " ORDER BY id", tuple(params))


@app.get("/api/products/{product_id}")
def product(product_id: int):
    value = _row("SELECT * FROM products WHERE id=?", (product_id,))
    if not value:
        raise HTTPException(404, "product_not_found")
    return value


@app.get("/api/cart")
def cart(request: Request):
    user = _current_user(request)
    items = _rows("""
        SELECT c.product_id,c.quantity,p.name,p.price_cents,p.image,p.stock
        FROM cart_items c JOIN products p ON p.id=c.product_id
        WHERE c.user_id=? ORDER BY p.id
    """, (user["id"],))
    return {"items": items, "total_cents": sum(
        item["quantity"] * item["price_cents"] for item in items)}


@app.post("/api/cart")
def add_cart(payload: CartRequest, request: Request):
    user = _current_user(request)
    product_value = _row("SELECT stock FROM products WHERE id=?",
                         (payload.product_id,))
    if not product_value:
        raise HTTPException(404, "product_not_found")
    if payload.quantity > product_value["stock"]:
        raise HTTPException(409, "insufficient_stock")
    with _db() as db:
        db.execute("""
            INSERT INTO cart_items(user_id,product_id,quantity) VALUES (?,?,?)
            ON CONFLICT(user_id,product_id) DO UPDATE SET quantity=excluded.quantity
        """, (user["id"], payload.product_id, payload.quantity))
        db.commit()
    return cart(request)


@app.delete("/api/cart/{product_id}")
def remove_cart(product_id: int, request: Request):
    user = _current_user(request)
    with _db() as db:
        db.execute("DELETE FROM cart_items WHERE user_id=? AND product_id=?",
                   (user["id"], product_id))
        db.commit()
    return cart(request)


@app.post("/api/checkout")
def checkout(request: Request):
    user = _current_user(request)
    current = cart(request)
    if not current["items"]:
        raise HTTPException(409, "empty_cart")
    with _db() as db:
        for item in current["items"]:
            stock = db.execute("SELECT stock FROM products WHERE id=?",
                               (item["product_id"],)).fetchone()[0]
            if stock < item["quantity"]:
                raise HTTPException(409, "insufficient_stock")
        next_id = db.execute("SELECT COALESCE(MAX(id),9000)+1 FROM orders").fetchone()[0]
        created = datetime.now(timezone.utc).isoformat()
        db.execute("INSERT INTO orders VALUES (?,?,?,?,?)", (
            next_id, user["id"], "paid", current["total_cents"], created))
        for item in current["items"]:
            db.execute("INSERT INTO order_items VALUES (?,?,?,?)", (
                next_id, item["product_id"], item["quantity"], item["price_cents"]))
            db.execute("UPDATE products SET stock=stock-? WHERE id=?",
                       (item["quantity"], item["product_id"]))
        db.execute("DELETE FROM cart_items WHERE user_id=?", (user["id"],))
        db.commit()
    return {"order_id": next_id, "status": "paid",
            "total_cents": current["total_cents"]}


@app.get("/api/articles")
def articles(tag: str = ""):
    items = _rows("""
        SELECT a.*,u.display_name AS author_name,u.avatar AS author_avatar
        FROM articles a JOIN users u ON u.id=a.author_id
        ORDER BY a.published_at DESC
    """)
    for item in items:
        item["tags"] = json.loads(item.pop("tags_json"))
    return [item for item in items if not tag or tag in item["tags"]]


@app.get("/api/articles/{slug}")
def article(slug: str):
    items = articles()
    value = next((item for item in items if item["slug"] == slug), None)
    if not value:
        raise HTTPException(404, "article_not_found")
    value["comments"] = _rows("""
        SELECT c.*,u.display_name AS author_name,u.avatar AS author_avatar
        FROM comments c JOIN users u ON u.id=c.author_id
        WHERE c.target_type='article' AND c.target_id=? ORDER BY c.created_at
    """, (value["id"],))
    return value


@app.post("/api/articles/{slug}/comments")
def add_article_comment(slug: str, payload: CommentRequest, request: Request):
    user = _current_user(request)
    value = _row("SELECT id FROM articles WHERE slug=?", (slug,))
    if not value:
        raise HTTPException(404, "article_not_found")
    with _db() as db:
        next_id = db.execute("SELECT COALESCE(MAX(id),400)+1 FROM comments").fetchone()[0]
        created = datetime.now(timezone.utc).isoformat()
        db.execute("INSERT INTO comments VALUES (?,?,?,?,?,?)", (
            next_id, "article", value["id"], user["id"], payload.content, created))
        db.commit()
    return _row("SELECT * FROM comments WHERE id=?", (next_id,))


@app.get("/api/posts")
def posts():
    return _rows("""
        SELECT p.*,u.username,u.display_name,u.avatar,
          (SELECT COUNT(*) FROM comments c
           WHERE c.target_type='post' AND c.target_id=p.id) AS comment_count
        FROM posts p JOIN users u ON u.id=p.author_id
        ORDER BY p.created_at DESC
    """)


@app.post("/api/posts")
def create_post(payload: PostRequest, request: Request):
    user = _current_user(request)
    with _db() as db:
        next_id = db.execute("SELECT COALESCE(MAX(id),300)+1 FROM posts").fetchone()[0]
        created = datetime.now(timezone.utc).isoformat()
        db.execute("INSERT INTO posts VALUES (?,?,?,?,?,0)",
                   (next_id, user["id"], payload.content, payload.media, created))
        db.commit()
    return _row("SELECT * FROM posts WHERE id=?", (next_id,))


@app.get("/api/posts/{post_id}/comments")
def post_comments(post_id: int):
    return _rows("""
        SELECT c.*,u.display_name AS author_name,u.avatar AS author_avatar
        FROM comments c JOIN users u ON u.id=c.author_id
        WHERE c.target_type='post' AND c.target_id=? ORDER BY c.created_at
    """, (post_id,))


@app.post("/api/posts/{post_id}/comments")
def add_comment(post_id: int, payload: CommentRequest, request: Request):
    user = _current_user(request)
    if not _row("SELECT id FROM posts WHERE id=?", (post_id,)):
        raise HTTPException(404, "post_not_found")
    with _db() as db:
        next_id = db.execute("SELECT COALESCE(MAX(id),400)+1 FROM comments").fetchone()[0]
        created = datetime.now(timezone.utc).isoformat()
        db.execute("INSERT INTO comments VALUES (?,?,?,?,?,?)", (
            next_id, "post", post_id, user["id"], payload.content, created))
        db.commit()
    return _row("SELECT * FROM comments WHERE id=?", (next_id,))


@app.post("/api/posts/{post_id}/like")
def like_post(post_id: int, request: Request):
    _current_user(request)
    with _db() as db:
        changed = db.execute("UPDATE posts SET likes=likes+1 WHERE id=?",
                             (post_id,)).rowcount
        db.commit()
    if not changed:
        raise HTTPException(404, "post_not_found")
    return _row("SELECT id,likes FROM posts WHERE id=?", (post_id,))


@app.post("/api/follows/{username}")
def follow(username: str, request: Request):
    user = _current_user(request)
    target = _row("SELECT id FROM users WHERE username=?", (username,))
    if not target:
        raise HTTPException(404, "user_not_found")
    if target["id"] == user["id"]:
        raise HTTPException(409, "cannot_follow_self")
    with _db() as db:
        db.execute("INSERT OR IGNORE INTO follows VALUES (?,?)",
                   (user["id"], target["id"]))
        db.commit()
    return {"following": True, "username": username}


@app.get("/api/messages")
def messages(request: Request):
    user = _current_user(request)
    return _rows("""
        SELECT m.*,s.display_name AS sender_name,r.display_name AS recipient_name
        FROM messages m JOIN users s ON s.id=m.sender_id
        JOIN users r ON r.id=m.recipient_id
        WHERE m.sender_id=? OR m.recipient_id=? ORDER BY m.created_at
    """, (user["id"], user["id"]))


@app.post("/api/messages")
def send_message(payload: MessageRequest, request: Request):
    user = _current_user(request)
    if not _row("SELECT id FROM users WHERE id=?", (payload.recipient_id,)):
        raise HTTPException(404, "recipient_not_found")
    with _db() as db:
        next_id = db.execute("SELECT COALESCE(MAX(id),500)+1 FROM messages").fetchone()[0]
        created = datetime.now(timezone.utc).isoformat()
        db.execute("INSERT INTO messages VALUES (?,?,?,?,?,0)", (
            next_id, user["id"], payload.recipient_id, payload.content, created))
        db.commit()
    return _row("SELECT * FROM messages WHERE id=?", (next_id,))


@app.get("/api/notifications")
def notifications(request: Request):
    user = _current_user(request)
    return _rows("SELECT * FROM notifications WHERE user_id=? ORDER BY created_at DESC",
                 (user["id"],))


@app.get("/api/orders")
def orders(request: Request):
    user = _current_user(request)
    values = _rows("SELECT * FROM orders WHERE user_id=? ORDER BY created_at DESC",
                   (user["id"],))
    for value in values:
        value["items"] = _rows("""
            SELECT i.*,p.name,p.image FROM order_items i
            JOIN products p ON p.id=i.product_id WHERE i.order_id=?
        """, (value["id"],))
    return values


@app.get("/api/media")
def media():
    return _rows("SELECT * FROM media ORDER BY type,id")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=7900)
    parser.add_argument("--reset", action="store_true",
                        help="restore the writable database from seed before start")
    args = parser.parse_args()
    if args.reset:
        shutil.copy2(SEED_DB, DB_PATH)
    import uvicorn
    uvicorn.run(app, host=args.host, port=args.port)


if __name__ == "__main__":
    main()
