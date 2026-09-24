# Nimbus Market (ecommerce_demo)

Reference sample app for the black-box benchmark: a small server-rendered
e-commerce site ("Nimbus Market") with login, catalog search/filter, cart,
coupons, checkout and order history. Pure HTML forms and links, no front-end
framework, no external resources.

## Run

From the repository root:

```bash
python -m sample_apps.ecommerce_demo.app --port <N> --data-dir <D>
```

e.g. with the vendored interpreter:

```bash
vendor/python/python.exe -m sample_apps.ecommerce_demo.app --port 8123 --data-dir runs/demo_data
```

The server binds to `127.0.0.1` only. Every request must carry the internal
gateway header `X-BBB-Gateway` (any value); requests without it get an empty
404. `Server` / `X-Powered-By` response headers are stripped.

## State & reset

All state lives in a single file: `<data-dir>/state.json`
(users, products, orders, carts, sessions). It is created from the built-in
seed on first start; every write is flushed to disk immediately.

**Reset to seed state:** stop the server, delete `<data-dir>/state.json`,
start again.

## Seed data

- Users: `alice` / `alice123` (Alice Anderson, has one historical order
  #1001), `bob` / `bob123`.
- Products: 8 fixed products across Electronics / Home / Outdoors /
  Stationery (see `seed.py`), each with a fixed stock count.
- Coupons: `SAVE10` (10% off), `WELCOME5` ($5 off). All other codes are
  invalid.
- No active sessions, all carts empty.

## Behaviour notes

- Session cookie `nm_session` (httpOnly) ties the browser to a server-side
  session; anonymous carts persist across reloads and restarts. On login the
  anonymous cart is merged into the user's saved cart. Logout starts a fresh
  anonymous session with an empty cart.
- Order ids are sequential starting at #1001 (alice's seeded order), so the
  first order placed after a reset is always #1002.
- Placing an order decrements stock, clears the cart and redirects to the
  confirmation page.

## Verification

```bash
vendor/python/python.exe sample_apps/ecommerce_demo/smoke_test.py
```

starts the app on a free port with a temporary data dir and checks the
gateway guard, search/filter, auth, cart rules, coupons, checkout, order
placement, restart persistence and seed reset (44 checks).

## Files

- `app.py` — FastAPI app: gateway/session middleware, routes, entry point.
- `seed.py` — deterministic seed data.
- `state.py` — JSON file store.
- `templates/` — Jinja2 templates (all CSS inline in `base.html`).
- `LAYOUT.md` — pixel coordinates of key elements at 1440×900.
- `ground_truth.json` — **BENCHMARK-PRIVATE** functional topology ground
  truth; never served over HTTP, never mounted to an agent.
- `smoke_test.py` — repeatable end-to-end smoke test (httpx).
