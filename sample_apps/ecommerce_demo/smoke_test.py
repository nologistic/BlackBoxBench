"""Smoke test for the Nimbus Market sample app.

Starts the app as a subprocess on a free port with a temporary data dir and
exercises the gateway guard, auth, cart, coupon, checkout and persistence
behaviours over HTTP (all requests carry the internal gateway header, like the
real controller gateway would).

Run from anywhere:

    python sample_apps/ecommerce_demo/smoke_test.py

Exits 0 when all checks pass, 1 otherwise.
"""

import shutil
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

import httpx

ROOT = Path(__file__).resolve().parents[2]
GATEWAY = {"X-BBB-Gateway": "smoke-test"}

passed = 0
failed = 0


def check(name, condition, detail=""):
    global passed, failed
    if condition:
        passed += 1
        print(f"  PASS  {name}")
    else:
        failed += 1
        print(f"  FAIL  {name}  {detail}")


def free_port():
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def start_server(port, data_dir):
    proc = subprocess.Popen(
        [sys.executable, "-m", "sample_apps.ecommerce_demo.app",
         "--port", str(port), "--data-dir", str(data_dir)],
        cwd=str(ROOT),
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
    )
    deadline = time.time() + 30
    while time.time() < deadline:
        if proc.poll() is not None:
            raise RuntimeError("server process exited during startup")
        try:
            r = httpx.get(f"http://127.0.0.1:{port}/", headers=GATEWAY,
                          timeout=1.0, trust_env=False)
            if r.status_code == 200:
                return proc
        except httpx.HTTPError:
            pass
        time.sleep(0.25)
    proc.kill()
    raise RuntimeError("server did not become ready in time")


def stop_server(proc):
    proc.terminate()
    try:
        proc.wait(timeout=10)
    except subprocess.TimeoutExpired:
        proc.kill()
        proc.wait(timeout=10)


def main():
    data_dir = Path(tempfile.mkdtemp(prefix="nm_smoke_"))
    port = free_port()
    base = f"http://127.0.0.1:{port}"
    proc = None
    try:
        proc = start_server(port, data_dir)
        print(f"server up on port {port}, data dir {data_dir}")

        # -- gateway guard ------------------------------------------------
        r = httpx.get(f"{base}/", timeout=5.0, trust_env=False)
        check("no gateway header -> 404", r.status_code == 404)
        check("no gateway header -> empty body", r.content == b"")
        check("server header stripped", "server" not in r.headers)

        r = httpx.get(f"{base}/", headers=GATEWAY, timeout=5.0, trust_env=False)
        check("with gateway header -> 200", r.status_code == 200)
        check("home shows brand", "Nimbus Market" in r.text)
        check("home shows seeded product", "Aurora Camera" in r.text)

        # -- search / catalog ---------------------------------------------
        r = httpx.get(f"{base}/?q=camera", headers=GATEWAY, timeout=5.0, trust_env=False)
        check("search matches product", "Aurora Camera" in r.text
              and "Nimbus Laptop" not in r.text)
        r = httpx.get(f"{base}/?q=zzzzz", headers=GATEWAY, timeout=5.0, trust_env=False)
        check("search empty state", "No products found" in r.text)
        r = httpx.get(f"{base}/?cat=Home", headers=GATEWAY, timeout=5.0, trust_env=False)
        check("category filter", "Vertex Mug" in r.text
              and "Aurora Camera" not in r.text)

        # -- anonymous session: empty cart --------------------------------
        anon = httpx.Client(headers=GATEWAY, follow_redirects=False,
                            timeout=5.0, trust_env=False)
        r = anon.get(f"{base}/cart")
        check("empty cart message", "Your cart is empty" in r.text)
        check("empty cart hides checkout button",
              "Proceed to Checkout" not in r.text)
        check("session cookie set", "nm_session" in anon.cookies)

        # -- unknown routes -> generic 404 page ---------------------------
        r = anon.get(f"{base}/no-such-page")
        check("unknown route -> 404 page", r.status_code == 404
              and "Page not found" in r.text)
        r = anon.get(f"{base}/product/999")
        check("unknown product -> 404 page", r.status_code == 404
              and "Page not found" in r.text)

        # -- login failure / success --------------------------------------
        r = anon.post(f"{base}/login",
                      data={"username": "alice", "password": "wrong"})
        check("login failure error bar",
              "Invalid username or password" in r.text)

        # -- checkout requires login --------------------------------------
        r = anon.get(f"{base}/checkout")
        check("anonymous checkout redirects to login",
              r.status_code == 303
              and r.headers["location"] == "/login?next=checkout")
        r = anon.get(f"{base}/login?next=checkout")
        check("checkout login prompt shown",
              "Please log in to continue checkout" in r.text)

        # -- add to cart (anonymous) --------------------------------------
        r = anon.post(f"{base}/cart/add",
                      data={"product_id": "4", "quantity": "2"})
        check("add to cart -> 303 to /cart", r.status_code == 303
              and r.headers["location"] == "/cart")
        r = anon.get(f"{base}/cart")
        check("cart shows added product", "Vertex Mug" in r.text)
        check("non-empty cart shows checkout button",
              "Proceed to Checkout" in r.text)

        # -- invalid quantity on add --------------------------------------
        r = anon.post(f"{base}/cart/add",
                      data={"product_id": "4", "quantity": "0"})
        check("quantity 0 rejected with error",
              "Please enter a quantity of at least 1." in r.text)
        r = anon.post(f"{base}/cart/add",
                      data={"product_id": "4", "quantity": "999"})
        check("quantity over stock rejected",
              "Only 30 left in stock." in r.text)

        # -- cart update validation ---------------------------------------
        r = anon.post(f"{base}/cart/update",
                      data={"product_id": "4", "quantity": "0"})
        check("cart update quantity 0 rejected",
              "Quantity must be at least 1." in r.text)

        # -- coupons --------------------------------------------------------
        r = anon.post(f"{base}/cart/coupon", data={"code": "BOGUS"})
        check("invalid coupon error", "Invalid coupon code." in r.text)
        check("invalid coupon keeps total", "$25.00" in r.text)
        r = anon.post(f"{base}/cart/coupon", data={"code": "SAVE10"})
        check("valid coupon -> 303", r.status_code == 303)
        r = anon.get(f"{base}/cart")
        check("discount row shown", "Discount (SAVE10)" in r.text
              and "-$2.50" in r.text and "$22.50" in r.text)

        # -- login merges anonymous cart ------------------------------------
        r = anon.post(f"{base}/login",
                      data={"username": "alice", "password": "alice123"})
        check("login success -> 303 to /", r.status_code == 303
              and r.headers["location"] == "/")
        r = anon.get(f"{base}/")
        check("header shows greeting", "Hi, Alice" in r.text)
        r = anon.get(f"{base}/cart")
        check("anonymous cart merged after login", "Vertex Mug" in r.text
              and "SAVE10" in r.text)

        # -- checkout validation --------------------------------------------
        r = anon.get(f"{base}/checkout")
        check("checkout form shown", "Shipping address" in r.text)
        r = anon.post(f"{base}/checkout/place",
                      data={"name": "", "address": "1 Main St",
                            "city": "", "zip": ""})
        check("missing fields rejected",
              "Please fill in all required fields: Full name, City, ZIP code."
              in r.text)

        # -- place order ----------------------------------------------------
        r = anon.post(f"{base}/checkout/place",
                      data={"name": "Alice Anderson", "address": "12 Maple Street",
                            "city": "Springfield", "zip": "90210"})
        check("place order -> 303 to confirmation", r.status_code == 303
              and r.headers["location"] == "/orders/1002")
        r = anon.get(f"{base}/orders/1002")
        check("confirmation page", "Order confirmed" in r.text
              and "Order #1002" in r.text and "$22.50" in r.text)
        r = anon.get(f"{base}/cart")
        check("cart cleared after order", "Your cart is empty" in r.text)
        r = anon.get(f"{base}/orders")
        check("orders list has new + seeded order",
              "Order #1002" in r.text and "Order #1001" in r.text)
        r = anon.get(f"{base}/product/4")
        check("stock decremented", "In stock: 28" in r.text)

        # -- logout -----------------------------------------------------------
        r = anon.get(f"{base}/logout")
        check("logout -> 303 to /", r.status_code == 303)
        r = anon.get(f"{base}/")
        check("logout clears greeting", "Hi, Alice" not in r.text)

        # -- persistence across restart ---------------------------------------
        anon.close()
        stop_server(proc)
        proc = start_server(port, data_dir)
        r = httpx.get(f"{base}/", headers=GATEWAY, timeout=5.0, trust_env=False)
        check("home ok after restart", r.status_code == 200)
        with httpx.Client(headers=GATEWAY, follow_redirects=False,
                          timeout=5.0, trust_env=False) as client:
            client.post(f"{base}/login",
                        data={"username": "alice", "password": "alice123"})
            r = client.get(f"{base}/orders")
            check("order persists across restart", "Order #1002" in r.text)
            r = client.get(f"{base}/product/4")
            check("stock persists across restart", "In stock: 28" in r.text)

        # -- reset: delete state.json -> seed state ----------------------------
        stop_server(proc)
        proc = None
        (data_dir / "state.json").unlink()
        proc = start_server(port, data_dir)
        with httpx.Client(headers=GATEWAY, follow_redirects=False,
                          timeout=5.0, trust_env=False) as client:
            client.post(f"{base}/login",
                        data={"username": "alice", "password": "alice123"})
            r = client.get(f"{base}/orders")
            check("reset restores seeded order only",
                  "Order #1001" in r.text and "Order #1002" not in r.text)
            r = client.get(f"{base}/product/4")
            check("reset restores stock", "In stock: 30" in r.text)

    finally:
        if proc is not None:
            stop_server(proc)
        shutil.rmtree(data_dir, ignore_errors=True)

    print(f"\n{passed} passed, {failed} failed")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
