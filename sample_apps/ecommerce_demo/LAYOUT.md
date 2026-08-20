# Nimbus Market — Layout Reference (1440×900 viewport)

All coordinates are CSS pixels, origin top-left, derived directly from the
stylesheet in `templates/base.html`. The browser runs at `devicePixelRatio=1`,
zoom 100%, no browser chrome, so CSS pixels == screenshot pixels.

## Global geometry

- Content column: `width:1200px; margin:0 auto` → x range **[120, 1320]** ((1440−1200)/2 = 120).
- Header: `position:fixed; height:64px` → bar occupies **(0,0)–(1440,64)** on every page.
- `.main { padding-top:64px }` → page content starts at y=64.
- Banner (`.error-bar` / `.notice-bar`): `margin:16px 0 24px; height:40px` → box **(120,80)–(1320,120)**, total footprint 80px. At most one banner is rendered at a time. On flow-layout pages (home, cart, checkout, orders) everything below shifts down by exactly **+80px** when a banner is present; on absolutely-positioned pages (product detail, login) nothing moves.
- Coordinates marked `≈` depend on rendered text width (Arial metrics); the element's **left/top edge is always exact**, only its right edge varies.

## Header (every page)

Positions are `left/top` inside `.header-inner` (origin x=120, y=0).

| Element | CSS rule | Box (x1,y1)–(x2,y2) |
|---|---|---|
| Logo (cloud SVG, links to home) | `.logo left:0 top:16`, 32×32 | **(120,16)–(152,48)** |
| Brand "Nimbus Market" | `.brand left:44`, `line-height:64` | (164,0)–(≈310,64); text center y=32 |
| Search input | `.search-form left:460 top:16`, input 360×32 | **(580,16)–(940,48)** |
| Search button | inside form at `left:368`, 72×32 | **(948,16)–(1020,48)** |
| "Cart (N)" link | `.header-cart left:950`, `line-height:64` | (1070,0)–(≈1132,64); text center y=32 |
| "Login" link (anonymous) | `.header-account left:1080` + `margin-left:10` | (1210,0)–(≈1244,64) |
| Greeting + Orders + Logout (logged in) | `.header-account left:1080` | "Hi, Alice" text at x=1200; "Orders" ≈x=1277; "Logout" ≈x=1342; all y 0–64 |

## Home `/`

- Category pills (`.cat-row`: padding-top 24, links 32px tall): occupy **y 88–120**. "All" pill starts at **x=120**; subsequent pills (Electronics, Home, Outdoors, Stationery) follow with 8px gaps; pill widths are text-dependent (≈43–105px).
- Product grid (`.grid`): starts at **y=120** (64 + 56 cat-row).
- Cards (`.card` 285×300, `float:left`, margin-right 20 except every 4th):
  - Card at grid column c (0–3), row r: top-left **(120 + 305c, 120 + 320r)**.
  - Row 1: y 120–420; row 2: y 440–740.
  - Column x positions: **120, 425, 730, 1035**.
  - Card 1 "Aurora Camera": (120,120)–(405,420); card 5 "Harbor Lamp": (120,440)–(405,740).
- Inside a card with top-left (cx,cy) — absolute offsets from CSS:
  - Image placeholder (inline SVG): (cx,cy)–(cx+285,cy+170)
  - Name: (cx+12, cy+182) · Category: (cx+12, cy+206) · Price: (cx+12, cy+226)
  - **"View" link: (cx+12, cy+262)**, ≈36px wide → e.g. card 1 View at **(132,382)–(≈168,≈379+17)**
- Empty search state: "No products found" text top at **y=180** (120 + 60 padding), horizontally centered around x=720.

## Product detail `/product/{id}`

`.detail` children are absolutely positioned (origin x=120, y=64); a banner does not move them.

| Element | CSS rule | Box / top-left |
|---|---|---|
| "← Back to products" | `.back-link left:0 top:24` | **(120,88)** |
| Image placeholder | `.detail-img left:0 top:56`, 400×320 | **(120,120)–(520,440)** |
| Product name | `.detail-name left:440 top:56` | (560,120) |
| Price | `.detail-price top:104` | (560,168) |
| "In stock: N" | `.detail-stock top:140` | (560,204) |
| Description | `.detail-desc top:176`, width 700 | (560,240)–(1260,≈300) |
| "Quantity" label | `.qty-label left:440 top:280`, height 36 | (560,344)–(≈620,380) |
| Quantity input | `.qty-input left:540 top:280`, 80×36 | **(660,344)–(740,380)** |
| "Add to Cart" button | `.add-btn left:636 top:280`, 180×36 | **(756,344)–(936,380)** |
| Error bar (if any) | banner rule | **(120,80)–(1320,120)** |

## Cart `/cart`

- Title "Shopping Cart" (`.page-title`): text at y≈88 (block y 64–120).
- Table (`.data-table`): top **y=120**, x 120–1320. Header row y 120–160. Item row *i* (1-based): top **160 + 64·(i−1)**, height 64.
- Columns: Product x 120–600 · Unit price 600–780 · Quantity 780–1080 · Subtotal 1080–1224 · Remove 1224–1320. Cell content starts at column x+12, vertically centered.
- Row 1 controls: quantity input **(792,176)–(848,208)**; Update button **(856,176)–(928,208)**; Remove button **(1236,176)–(1308,208)**.
- Footer (`.cart-footer`, margin-top 24): top y = **184 + 64n** for n item rows. With n=1 → y=248:
  - Coupon input **(120,248)–(320,284)**; Apply button **(328,248)–(408,284)**
  - Totals box: (960,248)–(1320,≈424) (`right:0; width:360`)
  - "Proceed to Checkout" button (976,340)–(1304,384); with a discount row it moves to (976,368)–(1304,412)
- Empty cart: "Your cart is empty." at **(120,144)**; "Continue shopping" link at ≈(120,188). No checkout button is rendered.
- Error bar (invalid coupon / quantity): (120,80)–(1320,120); table and footer shift down +80.

## Login `/login`

Card is absolutely positioned; a banner does not move it.

- Card (`.login-card left:400 top:80`, width 400, padding 24): **(520,144)–(920,≈424)**
- "Log in" heading: y 168–200
- Username label: y 216–236; input **(544,236)–(896,272)**
- Password label: y 288–308; input **(544,308)–(896,344)**
- "Log in" button: **(544,360)–(896,400)**
- Notice "Please log in to continue checkout" / error "Invalid username or password": banner at **(120,80)–(1320,120)**

## Checkout `/checkout`

`.checkout-wrap` children absolute (origin x=120, y=64); a banner shifts the wrap down +80.

- Shipping form box (`.ship-form left:0 top:24`, width 640, padding 24): **(120,88)–(760,≈516)**
- "Shipping address" heading: y 112–144
- Full name: label y 160–180, input **(144,180)–(736,216)**
- Address: label y 232–252, input **(144,252)–(736,288)**
- City: label y 304–324, input **(144,324)–(736,360)**
- ZIP code: label y 376–396, input **(144,396)–(736,432)**
- "Place Order" button: **(144,448)–(384,492)** (240×44)
- Order summary box (`.order-summary right:0 top:24`, width 440, padding 16): **(880,88)–(1320,…)**; heading y 104–136; one 28px row per item starting y≈136; Subtotal, optional Discount, Total rows follow.

## Orders `/orders`

- Title "Your Orders": text y≈88 (block y 64–120).
- Table: top y=120, x 120–1320; header row y 120–160; row *i* top = 160 + 64·(i−1).
- Columns: Order x 120–360 · Items 360–840 · Total 840–1080 · View link 1080–1320 (content x+12, vertically centered → row 1 "View" at ≈(1092,185)).

## Order confirmation `/orders/{id}`

- "Order confirmed" title (`.confirm-title`): text y≈88 (block y 64–120).
- "Order #N · Thank you, …" line (`.order-meta`, height 32): y 120–152.
- Items table (`.confirm-table`, margin-top 16): top **y=168**; header 168–208; rows 64px each.
- Columns: Product x 120–720 · Unit price 720–900 · Quantity 900–1080 · Subtotal 1080–1320.
- Totals box and ship-to line: 24px below the table bottom; totals box x 960–1320.
- "Continue shopping" link: below the totals block.

## 404 page

- "Page not found" heading: `.notfound padding-top:120` → top **y=184**, centered around x=720.
