"""Seed data for the Nimbus Market sample app.

Everything here is fixed and deterministic: the same seed always produces the
same catalog, users, coupons and historical order.
"""


def build_seed() -> dict:
    """Return the full initial application state (also used for reset)."""
    products = [
        {
            "id": 1,
            "name": "Aurora Camera",
            "price_cents": 39900,
            "category": "Electronics",
            "stock": 12,
            "color": "#4a6fa5",
            "description": "Compact digital camera with a 24MP sensor, optical "
                           "image stabilizer and a dedicated low-light mode.",
        },
        {
            "id": 2,
            "name": "Nimbus Laptop",
            "price_cents": 89900,
            "category": "Electronics",
            "stock": 5,
            "color": "#3d5a80",
            "description": "Lightweight 14-inch laptop with all-day battery, "
                           "16GB of memory and a matte anti-glare display.",
        },
        {
            "id": 3,
            "name": "Echo Keyboard",
            "price_cents": 5900,
            "category": "Electronics",
            "stock": 20,
            "color": "#628a9e",
            "description": "Full-size mechanical keyboard with quiet tactile "
                           "switches and a detachable braided cable.",
        },
        {
            "id": 4,
            "name": "Vertex Mug",
            "price_cents": 1250,
            "category": "Home",
            "stock": 30,
            "color": "#b5838d",
            "description": "Stoneware mug with a matte glaze, 350ml capacity. "
                           "Dishwasher and microwave safe.",
        },
        {
            "id": 5,
            "name": "Harbor Lamp",
            "price_cents": 4500,
            "category": "Home",
            "stock": 8,
            "color": "#d9a05b",
            "description": "Warm-glow desk lamp with a linen shade and an "
                           "inline dimmer switch.",
        },
        {
            "id": 6,
            "name": "Summit Bottle",
            "price_cents": 2400,
            "category": "Outdoors",
            "stock": 25,
            "color": "#6b9e78",
            "description": "Insulated stainless steel bottle, 750ml. Keeps "
                           "drinks cold for 24 hours or hot for 12.",
        },
        {
            "id": 7,
            "name": "Trail Compass",
            "price_cents": 1875,
            "category": "Outdoors",
            "stock": 10,
            "color": "#c1666b",
            "description": "Liquid-filled hiking compass with a rotating bezel "
                           "and a breakaway lanyard.",
        },
        {
            "id": 8,
            "name": "Folio Notebook",
            "price_cents": 999,
            "category": "Stationery",
            "stock": 40,
            "color": "#8d7fb8",
            "description": "A5 hardcover notebook with 192 dotted pages of "
                           "100gsm paper and an elastic closure.",
        },
    ]

    users = {
        "alice": {
            "password": "alice123",
            "full_name": "Alice Anderson",
            "first_name": "Alice",
        },
        "bob": {
            "password": "bob123",
            "full_name": "Bob Baker",
            "first_name": "Bob",
        },
    }

    coupons = {
        "SAVE10": {"type": "percent", "value": 10},
        "WELCOME5": {"type": "fixed", "value": 500},
    }

    # One pre-existing historical order for alice (order ids start at #1001).
    orders = [
        {
            "id": 1001,
            "user": "alice",
            "items": [
                {"product_id": 3, "name": "Echo Keyboard", "price_cents": 5900, "qty": 1},
                {"product_id": 4, "name": "Vertex Mug", "price_cents": 1250, "qty": 2},
            ],
            "subtotal_cents": 8400,
            "discount_cents": 0,
            "total_cents": 8400,
            "coupon": None,
            "shipping": {
                "name": "Alice Anderson",
                "address": "12 Maple Street",
                "city": "Springfield",
                "zip": "90210",
            },
        }
    ]

    return {
        "users": users,
        "products": products,
        "coupons": coupons,
        "orders": orders,
        "carts": {},
        "sessions": {},
        "next_order_id": 1002,
    }
