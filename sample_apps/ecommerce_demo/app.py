"""Nimbus Market - reference sample e-commerce app for BlackBoxBench.

Server-rendered (Jinja2) store driven entirely by plain HTML forms and links.
Run with:

    python -m sample_apps.ecommerce_demo.app --port <N> --data-dir <D>

Every request must carry the internal gateway header ``X-BBB-Gateway``;
requests without it receive an empty 404.
"""

import argparse
import secrets
import urllib.parse
from pathlib import Path

import uvicorn
from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import HTMLResponse, RedirectResponse, Response
from fastapi.templating import Jinja2Templates
from starlette.exceptions import HTTPException as StarletteHTTPException
from starlette.middleware.base import BaseHTTPMiddleware

from .state import Store

TEMPLATES_DIR = Path(__file__).resolve().parent / "templates"
SESSION_COOKIE = "nm_session"

templates = Jinja2Templates(directory=str(TEMPLATES_DIR))
templates.env.filters["money"] = lambda cents: f"${cents / 100:,.2f}"


# --------------------------------------------------------------------------
# Middleware
# --------------------------------------------------------------------------

class GatewayMiddleware(BaseHTTPMiddleware):
    """Reject anything that did not come through the internal gateway."""

    async def dispatch(self, request, call_next):
        if "x-bbb-gateway" not in request.headers:
            return Response(status_code=404)
        response = await call_next(request)
        for header in ("server", "x-powered-by"):
            if header in response.headers:
                del response.headers[header]
        return response


class SessionMiddleware(BaseHTTPMiddleware):
    """Attach a server-side session (cookie ``nm_session``) to every request."""

    async def dispatch(self, request, call_next):
        store: Store = request.app.state.store
        token = request.cookies.get(SESSION_COOKIE)
        created = False
        if not token or token not in store.data["sessions"]:
            token = secrets.token_hex(16)
            store.data["sessions"][token] = {"user": None}
            store.save()
            created = True
        request.state.session_token = token
        request.state.session = store.data["sessions"][token]
        response = await call_next(request)
        if created:
            response.set_cookie(SESSION_COOKIE, token, httponly=True, path="/")
        return response


# --------------------------------------------------------------------------
# Helpers
# --------------------------------------------------------------------------

async def read_form(request: Request) -> dict:
    """Parse an application/x-www-form-urlencoded body without extra deps."""
    body = (await request.body()).decode("utf-8", "replace")
    pairs = urllib.parse.parse_qs(body, keep_blank_values=True)
    return {key: values[0] for key, values in pairs.items()}


def find_product(store: Store, product_id: int):
    for product in store.data["products"]:
        if product["id"] == product_id:
            return product
    return None


def cart_key(session: dict, token: str) -> str:
    if session["user"]:
        return f"user:{session['user']}"
    return token


def get_cart(store: Store, request: Request) -> dict:
    key = cart_key(request.state.session, request.state.session_token)
    carts = store.data["carts"]
    if key not in carts:
        carts[key] = {"items": {}, "coupon": None}
    return carts[key]


def cart_view(store: Store, cart: dict):
    """Return (rows, subtotal, discount, total) for template rendering."""
    rows = []
    subtotal = 0
    for pid_str, qty in cart["items"].items():
        product = find_product(store, int(pid_str))
        if product is None:
            continue
        line_total = product["price_cents"] * qty
        subtotal += line_total
        rows.append({
            "product": product,
            "qty": qty,
            "line_total": line_total,
        })
    discount = 0
    code = cart.get("coupon")
    if code and code in store.data["coupons"]:
        coupon = store.data["coupons"][code]
        if coupon["type"] == "percent":
            discount = subtotal * coupon["value"] // 100
        else:
            discount = min(coupon["value"], subtotal)
    return rows, subtotal, discount, subtotal - discount


def base_ctx(request: Request, **extra) -> dict:
    store: Store = request.app.state.store
    session = request.state.session
    user = None
    if session["user"]:
        record = store.data["users"][session["user"]]
        user = {
            "username": session["user"],
            "first_name": record["first_name"],
            "full_name": record["full_name"],
        }
    cart = get_cart(store, request)
    ctx = {
        "request": request,
        "user": user,
        "cart_count": sum(cart["items"].values()),
        "q": "",
        "error": None,
        "notice": None,
    }
    ctx.update(extra)
    return ctx


def render(request: Request, template: str, status_code: int = 200, **extra):
    return templates.TemplateResponse(
        request, template, base_ctx(request, **extra), status_code=status_code
    )


def not_found() -> StarletteHTTPException:
    return StarletteHTTPException(status_code=404)


# --------------------------------------------------------------------------
# App factory
# --------------------------------------------------------------------------

def create_app(data_dir: str) -> FastAPI:
    app = FastAPI(title="Nimbus Market", docs_url=None, redoc_url=None,
                  openapi_url=None)
    app.state.store = Store(data_dir)

    app.add_middleware(SessionMiddleware)
    app.add_middleware(GatewayMiddleware)  # outermost: runs first

    @app.exception_handler(StarletteHTTPException)
    async def http_exception_handler(request: Request, exc: StarletteHTTPException):
        if exc.status_code == 404:
            return render(request, "404.html", status_code=404)
        return Response(status_code=exc.status_code)

    @app.exception_handler(RequestValidationError)
    async def validation_exception_handler(request: Request, exc: RequestValidationError):
        return render(request, "404.html", status_code=404)

    # ------------------------------------------------------------------
    # Catalog
    # ------------------------------------------------------------------

    @app.get("/", response_class=HTMLResponse)
    def home(request: Request, q: str = "", cat: str = ""):
        store: Store = request.app.state.store
        products = store.data["products"]
        categories = []
        for product in products:
            if product["category"] not in categories:
                categories.append(product["category"])

        query = q.strip()
        selected = cat.strip()
        visible = products
        if selected:
            visible = [p for p in visible if p["category"] == selected]
        if query:
            needle = query.lower()
            visible = [p for p in visible if needle in p["name"].lower()]

        return render(
            request, "home.html",
            products=visible, categories=categories, q=query, cat=selected,
        )

    @app.get("/product/{pid}", response_class=HTMLResponse)
    def product_detail(request: Request, pid: int):
        store: Store = request.app.state.store
        product = find_product(store, pid)
        if product is None:
            raise not_found()
        return render(request, "product.html", p=product, qty=1)

    # ------------------------------------------------------------------
    # Cart
    # ------------------------------------------------------------------

    @app.post("/cart/add")
    async def cart_add(request: Request):
        store: Store = request.app.state.store
        form = await read_form(request)
        try:
            pid = int(form.get("product_id", ""))
        except ValueError:
            raise not_found()
        product = find_product(store, pid)
        if product is None:
            raise not_found()

        raw_qty = form.get("quantity", "")
        try:
            qty = int(raw_qty)
        except ValueError:
            qty = 0

        cart = get_cart(store, request)
        in_cart = cart["items"].get(str(pid), 0)
        error = None
        if qty < 1:
            error = "Please enter a quantity of at least 1."
        elif qty + in_cart > product["stock"]:
            error = f"Only {product['stock']} left in stock."
        if error:
            return render(request, "product.html", p=product, qty=raw_qty,
                          error=error)

        cart["items"][str(pid)] = in_cart + qty
        store.save()
        return RedirectResponse("/cart", status_code=303)

    @app.get("/cart", response_class=HTMLResponse)
    def cart_page(request: Request):
        store: Store = request.app.state.store
        cart = get_cart(store, request)
        rows, subtotal, discount, total = cart_view(store, cart)
        return render(request, "cart.html", rows=rows, subtotal=subtotal,
                      discount=discount, total=total, coupon=cart.get("coupon"))

    @app.post("/cart/update")
    async def cart_update(request: Request):
        store: Store = request.app.state.store
        form = await read_form(request)
        try:
            pid = int(form.get("product_id", ""))
        except ValueError:
            raise not_found()
        product = find_product(store, pid)
        cart = get_cart(store, request)
        if product is None or str(pid) not in cart["items"]:
            raise not_found()

        try:
            qty = int(form.get("quantity", ""))
        except ValueError:
            qty = 0

        error = None
        if qty < 1:
            error = ("Quantity must be at least 1. "
                     "Use the Remove button to delete an item.")
        elif qty > product["stock"]:
            error = f"Only {product['stock']} left in stock."
        if error:
            rows, subtotal, discount, total = cart_view(store, cart)
            return render(request, "cart.html", rows=rows, subtotal=subtotal,
                          discount=discount, total=total,
                          coupon=cart.get("coupon"), error=error)

        cart["items"][str(pid)] = qty
        store.save()
        return RedirectResponse("/cart", status_code=303)

    @app.post("/cart/remove")
    async def cart_remove(request: Request):
        store: Store = request.app.state.store
        form = await read_form(request)
        try:
            pid = int(form.get("product_id", ""))
        except ValueError:
            raise not_found()
        cart = get_cart(store, request)
        if str(pid) in cart["items"]:
            del cart["items"][str(pid)]
            store.save()
        return RedirectResponse("/cart", status_code=303)

    @app.post("/cart/coupon")
    async def cart_coupon(request: Request):
        store: Store = request.app.state.store
        form = await read_form(request)
        cart = get_cart(store, request)
        if not cart["items"]:
            return RedirectResponse("/cart", status_code=303)
        code = form.get("code", "").strip().upper()
        if code in store.data["coupons"]:
            cart["coupon"] = code
            store.save()
            return RedirectResponse("/cart", status_code=303)
        rows, subtotal, discount, total = cart_view(store, cart)
        return render(request, "cart.html", rows=rows, subtotal=subtotal,
                      discount=discount, total=total, coupon=cart.get("coupon"),
                      error="Invalid coupon code.")

    # ------------------------------------------------------------------
    # Auth
    # ------------------------------------------------------------------

    @app.get("/login", response_class=HTMLResponse)
    def login_page(request: Request, next: str = ""):
        if request.state.session["user"]:
            return RedirectResponse("/", status_code=303)
        notice = None
        if next == "checkout":
            notice = "Please log in to continue checkout"
        return render(request, "login.html", next=next, notice=notice,
                      username="")

    @app.post("/login", response_class=HTMLResponse)
    async def login_submit(request: Request):
        store: Store = request.app.state.store
        form = await read_form(request)
        username = form.get("username", "").strip()
        password = form.get("password", "")
        next_target = form.get("next", "")

        record = store.data["users"].get(username)
        if record is None or record["password"] != password:
            return render(request, "login.html", next=next_target,
                          username=username,
                          error="Invalid username or password")

        session = request.state.session
        token = request.state.session_token
        session["user"] = username

        # Merge the anonymous session cart into the user's persistent cart.
        carts = store.data["carts"]
        anon = carts.pop(token, None)
        if anon:
            user_cart = carts.setdefault(f"user:{username}",
                                         {"items": {}, "coupon": None})
            for pid_str, qty in anon["items"].items():
                user_cart["items"][pid_str] = (
                    user_cart["items"].get(pid_str, 0) + qty
                )
            if not user_cart["coupon"] and anon["coupon"]:
                user_cart["coupon"] = anon["coupon"]
        store.save()

        if next_target == "checkout":
            return RedirectResponse("/checkout", status_code=303)
        return RedirectResponse("/", status_code=303)

    @app.get("/logout")
    def logout(request: Request):
        store: Store = request.app.state.store
        old_token = request.state.session_token
        store.data["sessions"].pop(old_token, None)
        store.data["carts"].pop(old_token, None)  # anonymous cart, if any
        new_token = secrets.token_hex(16)
        store.data["sessions"][new_token] = {"user": None}
        store.save()
        response = RedirectResponse("/", status_code=303)
        response.set_cookie(SESSION_COOKIE, new_token, httponly=True, path="/")
        return response

    # ------------------------------------------------------------------
    # Checkout & orders
    # ------------------------------------------------------------------

    @app.get("/checkout", response_class=HTMLResponse)
    def checkout_page(request: Request):
        store: Store = request.app.state.store
        if not request.state.session["user"]:
            return RedirectResponse("/login?next=checkout", status_code=303)
        cart = get_cart(store, request)
        if not cart["items"]:
            return RedirectResponse("/cart", status_code=303)
        rows, subtotal, discount, total = cart_view(store, cart)
        return render(request, "checkout.html", rows=rows, subtotal=subtotal,
                      discount=discount, total=total, coupon=cart.get("coupon"),
                      form={})

    @app.post("/checkout/place", response_class=HTMLResponse)
    async def checkout_place(request: Request):
        store: Store = request.app.state.store
        if not request.state.session["user"]:
            return RedirectResponse("/login?next=checkout", status_code=303)
        cart = get_cart(store, request)
        if not cart["items"]:
            return RedirectResponse("/cart", status_code=303)

        form = await read_form(request)
        fields = {
            "name": form.get("name", "").strip(),
            "address": form.get("address", "").strip(),
            "city": form.get("city", "").strip(),
            "zip": form.get("zip", "").strip(),
        }
        labels = {"name": "Full name", "address": "Address",
                  "city": "City", "zip": "ZIP code"}
        missing = [labels[key] for key, value in fields.items() if not value]
        rows, subtotal, discount, total = cart_view(store, cart)
        if missing:
            return render(
                request, "checkout.html", rows=rows, subtotal=subtotal,
                discount=discount, total=total, coupon=cart.get("coupon"),
                form=fields,
                error="Please fill in all required fields: "
                      + ", ".join(missing) + ".",
            )

        order_id = store.data["next_order_id"]
        store.data["next_order_id"] = order_id + 1
        order = {
            "id": order_id,
            "user": request.state.session["user"],
            "items": [
                {"product_id": row["product"]["id"], "name": row["product"]["name"],
                 "price_cents": row["product"]["price_cents"], "qty": row["qty"]}
                for row in rows
            ],
            "subtotal_cents": subtotal,
            "discount_cents": discount,
            "total_cents": total,
            "coupon": cart.get("coupon"),
            "shipping": fields,
        }
        store.data["orders"].append(order)
        for row in rows:
            row["product"]["stock"] -= row["qty"]
        cart["items"] = {}
        cart["coupon"] = None
        store.save()
        return RedirectResponse(f"/orders/{order_id}", status_code=303)

    @app.get("/orders", response_class=HTMLResponse)
    def orders_page(request: Request):
        store: Store = request.app.state.store
        username = request.state.session["user"]
        if not username:
            return RedirectResponse("/login?next=orders", status_code=303)
        orders = [o for o in store.data["orders"] if o["user"] == username]
        return render(request, "orders.html", orders=orders)

    @app.get("/orders/{oid}", response_class=HTMLResponse)
    def order_detail(request: Request, oid: int):
        store: Store = request.app.state.store
        username = request.state.session["user"]
        if not username:
            return RedirectResponse("/login?next=orders", status_code=303)
        order = next(
            (o for o in store.data["orders"]
             if o["id"] == oid and o["user"] == username),
            None,
        )
        if order is None:
            raise not_found()
        return render(request, "order_confirm.html", order=order)

    return app


def main() -> None:
    parser = argparse.ArgumentParser(description="Nimbus Market sample app")
    parser.add_argument("--port", type=int, required=True)
    parser.add_argument("--data-dir", required=True)
    args = parser.parse_args()

    app = create_app(args.data_dir)
    uvicorn.run(app, host="127.0.0.1", port=args.port, log_level="warning",
                server_header=False)


if __name__ == "__main__":
    main()
