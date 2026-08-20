"""Demo exploration agent for BlackBoxBench (Phase I acceptance).

This is a *scripted calibration policy*: a human looked at the reference app
once and wrote down coordinates (see sample_apps/ecommerce_demo/LAYOUT.md).
It exists to validate the benchmark infrastructure end-to-end — pixels-only
observation, HID actions, evidence-checked discovery, incremental topology —
not to demonstrate visual reasoning. A real VLM agent uses the exact same SDK
loop (observe -> reason over PNG -> click/type -> record_*), see
docs/running_agent.md.

Everything this script "knows" about the app is entered through the same
agent channel any agent would use; it never touches app internals.

Run:  python -m demo_agent.run_demo --controller http://127.0.0.1:7800
"""
from __future__ import annotations

import argparse
import sys
import time

import httpx

from blackbox_bench_sdk import Environment

APP_ID = "ecommerce_demo"

# ------------------------------------------------------------- layout anchors
# (from sample_apps/ecommerce_demo/LAYOUT.md @1440x900, dpr=1)
LOGO = (136, 32)
SEARCH_INPUT = (760, 32)
SEARCH_BTN = (984, 32)
CART_LINK = (1100, 32)
LOGIN_LINK = (1227, 32)
ORDERS_LINK = (1262, 95)   # logged-in header account block wraps below the bar
LOGOUT_LINK = (1315, 95)

CAT_ELECTRONICS = (222, 104)
CARD1_VIEW = (150, 388)

DETAIL_QTY = (700, 362)
DETAIL_ADD = (846, 362)
# error banner shifts the detail content down by 80px (verified on real frames)
DETAIL_QTY_ERR = (700, 442)
DETAIL_ADD_ERR = (846, 442)

CART_ROW1_QTY = (820, 192)      # no banner
CART_ROW1_UPDATE = (892, 192)
CART_ROW1_QTY_B = (820, 272)    # with banner (page shifts +80)
CART_ROW1_UPDATE_B = (892, 272)
CART_COUPON = (220, 266)        # n=1 item, no banner
CART_COUPON_APPLY = (368, 266)
CART_COUPON_B = (220, 346)      # with banner
CART_COUPON_APPLY_B = (368, 346)
CART_CHECKOUT = (1140, 362)     # n=1, no discount row
CART_CHECKOUT_DISCOUNT = (1140, 390)  # n=1, with discount row

LOGIN_USER = (720, 192)
LOGIN_PASS = (720, 262)
LOGIN_BTN = (720, 316)

SHIP_NAME = (440, 198)
SHIP_ADDR = (440, 270)
SHIP_CITY = (440, 342)
SHIP_ZIP = (440, 414)
PLACE_ORDER = (264, 470)
# banner present -> wrap shifts +80
SHIP_NAME_B = (440, 278)
SHIP_ADDR_B = (440, 350)
SHIP_CITY_B = (440, 422)
SHIP_ZIP_B = (440, 494)
PLACE_ORDER_B = (264, 550)


class Demo:
    def __init__(self, controller: str, keep_open: bool = False):
        self.controller = controller.rstrip("/")
        self.keep_open = keep_open
        self.env: Environment | None = None
        self.cur_frame = 0
        self.sid = ""

    # ---------------------------------------------------------- plumbing

    def create_session(self) -> str:
        with httpx.Client(timeout=120, trust_env=False) as client:
            r = client.post(f"{self.controller}/api/sessions", json={
                "app_id": APP_ID,
                "budget": {"max_actions": 400, "max_duration_s": 1800,
                           "max_observations": 800},
            })
        r.raise_for_status()
        return r.json()["session_id"]

    def obs(self):
        o = self.env.observe()
        self.cur_frame = o.frame_id
        return o

    def act(self, fn, *args, desc: str = "", **kw):
        """Execute an action; build the evidence record for it."""
        before = self.cur_frame
        r = fn(*args, **kw)
        self.cur_frame = r.frame_id
        ev = {"step": r.step, "before_frame": before,
              "action": desc or self._desc(fn, args, kw),
              "after_frame": r.frame_id}
        return r, ev

    @staticmethod
    def _desc(fn, args, kw) -> str:
        name = fn.__name__
        if args:
            return f"{name}({','.join(str(a) for a in args)})"
        return f"{name}({kw})"

    def clear_and_type(self, pos, text: str, backspaces: int = 8,
                       desc_prefix: str = ""):
        """Focus a field, erase whatever is in it, type new text.
        Backspace on an empty field is a harmless no-op."""
        self.act(self.env.click, *pos, desc=f"click{pos}")
        for _ in range(backspaces):
            self.act(self.env.key_press, "Backspace", desc="key_press(Backspace)")
        r, ev = self.act(self.env.type_text, text,
                         desc=f'type_text("{desc_prefix or text}")')
        return r, ev

    # ---------------------------------------------------------- the run

    def run(self) -> dict:
        self.sid = self.create_session()
        print(f"[demo] session: {self.sid}")
        self.env = Environment(self.controller, self.sid, timeout=120)
        e = self.env

        # ---------------- 1. anonymous home
        o = self.obs()
        home_frame = o.frame_id
        st_home_anon = e.record_state(
            "Anonymous home", "未登录首页: 搜索框、分类筛选、商品网格、Cart (0)、Login 链接",
            visual_evidence=[home_frame],
            observed_elements=["搜索框", "分类 pills", "商品卡片网格", "Cart (0)", "Login"],
            confidence=0.98)
        print("[demo] state:", st_home_anon)

        # ---------------- 2. search (hit + empty edge case)
        self.act(e.click, *SEARCH_INPUT, desc="click(search 输入框)")
        self.act(e.type_text, "camera", desc='type_text("camera")')
        r, ev_search = self.act(e.click, *SEARCH_BTN, desc="click(Search 按钮)")
        o = self.obs()
        st_search = e.record_state(
            "Search results for 'camera'", "搜索结果页: 仅剩 Aurora Camera 一个商品",
            visual_evidence=[o.frame_id], entry_conditions=["在首页搜索 camera"],
            confidence=0.95)
        ft_search = e.record_feature(
            "Search products", "按关键词过滤商品列表",
            behavior={"preconditions": ["首页可见搜索框"],
                      "trigger": {"type": "type+click", "target_description": "搜索框输入关键词后点击 Search"},
                      "inputs": [{"name": "关键词", "kind": "text"}],
                      "postconditions": ["商品列表仅显示匹配商品"]},
            evidence=[ev_search], confidence=0.93)
        e.record_edge(ft_search, st_search, "TRANSITIONS_TO",
                      evidence=[ev_search], confidence=0.9)
        # empty-result edge case
        self.act(e.click, *SEARCH_INPUT, desc="click(search 输入框)")
        for _ in range(10):
            self.act(e.key_press, "Backspace", desc="key_press(Backspace)")
        self.act(e.type_text, "zzzz", desc='type_text("zzzz")')
        r, ev_noresult = self.act(e.click, *SEARCH_BTN, desc="click(Search 按钮)")
        o = self.obs()
        e.revise("feature", ft_search, "update", fields={
            "error_cases": ["搜索无匹配时显示空结果文案 'No products found'，不报错"],
            "evidence": [ev_search, ev_noresult]})
        print("[demo] feature: search (+empty case)")

        # ---------------- 3. category filter
        self.act(e.click, *LOGO, desc="click(logo 回首页)")
        r, ev_cat = self.act(e.click, *CAT_ELECTRONICS, desc="click(Electronics 分类)")
        ft_cat = e.record_feature(
            "Filter products by category", "点击分类 pill 只显示该分类商品",
            behavior={"trigger": {"type": "click", "target_description": "分类 pill"},
                      "postconditions": ["网格只含该分类商品"]},
            evidence=[ev_cat], confidence=0.9)

        # ---------------- 4. product detail
        r, ev_view = self.act(e.click, *CARD1_VIEW, desc="click(第一个商品的 View)")
        o = self.obs()
        st_detail = e.record_state(
            "Product detail", "商品详情页: 大图/名称/价格/库存/数量输入/Add to Cart",
            visual_evidence=[o.frame_id], entry_conditions=["点击商品 View 链接"],
            confidence=0.97)
        ft_view = e.record_feature(
            "View product detail", "从列表进入商品详情页",
            behavior={"trigger": {"type": "click", "target_description": "商品卡片 View 链接"},
                      "postconditions": ["显示该商品价格/库存/描述"]},
            evidence=[ev_view], confidence=0.95)
        e.record_edge(ft_view, st_detail, "TRANSITIONS_TO",
                      evidence=[ev_view], confidence=0.93)

        # ---------------- 5. add_to_cart: over-stock error, then success
        self.act(e.click, *DETAIL_QTY, desc="click(数量输入框)")
        self.act(e.type_text, "999", desc='type_text("999")  # 数量变为 1999')
        r, ev_overstock = self.act(e.click, *DETAIL_ADD, desc="click(Add to Cart)")
        o = self.obs()  # error bar visible -> content shifted +80
        # fix quantity: clear 4 chars, type 2 (shifted coordinates)
        self.act(e.click, *DETAIL_QTY_ERR, desc="click(数量输入框)")
        for _ in range(4):
            self.act(e.key_press, "Backspace", desc="key_press(Backspace)")
        self.act(e.type_text, "2", desc='type_text("2")')
        r, ev_add = self.act(e.click, *DETAIL_ADD_ERR, desc="click(Add to Cart)")
        o = self.obs()
        data_cart = e.record_data("Cart", "服务端持久化的购物车", confidence=0.9)
        ft_add = e.record_feature(
            "Add product to cart", "从详情页把指定数量商品加入购物车",
            behavior={"preconditions": ["商品详情页"],
                      "trigger": {"type": "click", "target_description": "Add to Cart 按钮"},
                      "inputs": [{"name": "数量", "kind": "number",
                                  "constraints": "1 到库存上限"}],
                      "postconditions": ["跳转到购物车页", "商品出现在购物车行"],
                      "error_cases": ["数量超过库存时显示错误条且不修改购物车"]},
            evidence=[ev_add], confidence=0.95)
        e.revise("feature", ft_add, "update", fields={
            "evidence": [ev_add, ev_overstock]})
        e.record_edge(ft_add, data_cart, "MUTATES", evidence=[ev_add],
                      confidence=0.95)
        print("[demo] feature: add_to_cart (+over-stock error)")

        # ---------------- 6. cart state, quantity update (valid + zero error)
        o = self.obs()
        st_cart = e.record_state(
            "Cart with items", "购物车非空: 商品行/数量框/Update/Remove/优惠券/合计/Checkout",
            visual_evidence=[o.frame_id], entry_conditions=["成功加入商品"],
            confidence=0.97)
        e.record_edge(ft_add, st_cart, "TRANSITIONS_TO", evidence=[ev_add],
                      confidence=0.93)
        # valid update 2 -> 3
        self.act(e.click, *CART_ROW1_QTY, desc="click(行1数量框)")
        self.act(e.key_press, "Backspace", desc="key_press(Backspace)")
        self.act(e.type_text, "3", desc='type_text("3")')
        r, ev_upd = self.act(e.click, *CART_ROW1_UPDATE, desc="click(Update)")
        ft_upd = e.record_feature(
            "Update item quantity", "修改购物车行数量并更新小计",
            behavior={"trigger": {"type": "click", "target_description": "行内 Update 按钮"},
                      "inputs": [{"name": "数量", "kind": "number", "constraints": ">=1"}],
                      "postconditions": ["小计与合计按新数量重算"]},
            evidence=[ev_upd], confidence=0.92)
        e.record_edge(ft_upd, data_cart, "MUTATES", evidence=[ev_upd],
                      confidence=0.9)
        # zero-quantity error
        self.act(e.click, *CART_ROW1_QTY, desc="click(行1数量框)")
        self.act(e.key_press, "Backspace", desc="key_press(Backspace)")
        self.act(e.type_text, "0", desc='type_text("0")')
        r, ev_upd_err = self.act(e.click, *CART_ROW1_UPDATE, desc="click(Update)")
        o = self.obs()
        e.revise("feature", ft_upd, "update", fields={
            "error_cases": ["数量为 0 时显示错误条，购物车保持不变"],
            "evidence": [ev_upd, ev_upd_err]})
        print("[demo] feature: update_quantity (+zero error)")

        # ---------------- 7. coupon: invalid then valid
        self.act(e.click, *CART_LINK, desc="click(Cart 链接重载无横幅购物车)")
        self.act(e.click, *CART_COUPON, desc="click(优惠券输入框)")
        self.act(e.type_text, "BOGUS", desc='type_text("BOGUS")')
        r, ev_coupon_bad = self.act(e.click, *CART_COUPON_APPLY,
                                    desc="click(Apply)")
        o = self.obs()
        st_coupon_err = e.record_state(
            "Cart with invalid coupon", "无效优惠券: 红色错误条, 合计不变",
            visual_evidence=[o.frame_id], entry_conditions=["输入无效优惠券并 Apply"],
            confidence=0.9)
        # valid coupon — banner present -> footer shifted +80
        self.act(e.click, *CART_COUPON_B, desc="click(优惠券输入框)")
        self.act(e.type_text, "SAVE10", desc='type_text("SAVE10")')
        r, ev_coupon_ok = self.act(e.click, *CART_COUPON_APPLY_B,
                                   desc="click(Apply)")
        o = self.obs()
        ft_coupon = e.record_feature(
            "Apply coupon", "输入优惠券码修改订单金额",
            behavior={"trigger": {"type": "click", "target_description": "Apply 按钮"},
                      "inputs": [{"name": "优惠券码", "kind": "text"}],
                      "postconditions": ["有效码出现折扣行, 合计减少"],
                      "error_cases": ["无效码显示错误条且合计不变"]},
            evidence=[ev_coupon_ok, ev_coupon_bad], confidence=0.92)
        e.record_edge(ft_coupon, data_cart, "MUTATES",
                      evidence=[ev_coupon_ok], confidence=0.85)
        print("[demo] feature: coupon (invalid + valid)")

        # ---------------- 8. persistence hypothesis: cart survives refresh
        hyp = e.record_hypothesis(
            "购物车内容在页面刷新(F5)后仍然保留(服务端持久化)",
            confidence=0.5, next_probe="按 F5 刷新后观察购物车行是否仍在")
        r, ev_f5 = self.act(e.key_press, "F5", desc="key_press(F5) 刷新页面")
        o = self.obs()
        e.resolve_hypothesis(hyp, "confirmed",
                             note="刷新后购物车行与数量仍显示",
                             evidence=[ev_f5])
        e.record_edge(data_cart, st_cart, "PERSISTS_TO",
                      evidence=[ev_f5], confidence=0.85,
                      note="刷新后购物车非空状态保持")
        print("[demo] hypothesis confirmed: cart persists across refresh")

        # ---------------- 9. checkout requires authentication
        hyp2 = e.record_hypothesis(
            "Checkout 可能要求先登录", confidence=0.5,
            next_probe="匿名状态点击 Proceed to Checkout 观察去向")
        self.act(e.click, *CART_LINK, desc="click(Cart 链接)")
        r, ev_checkout_anon = self.act(e.click, *CART_CHECKOUT_DISCOUNT,
                                       desc="click(Proceed to Checkout)")
        o = self.obs()
        st_login = e.record_state(
            "Login page", "登录页: 用户名/密码/Log in; 顶部提示 'Please log in to continue checkout'",
            visual_evidence=[o.frame_id], entry_conditions=["匿名点击 Checkout"],
            confidence=0.96)
        data_session = e.record_data("User session", "登录态会话", confidence=0.85)
        ft_checkout = e.record_feature(
            "Proceed to checkout", "从购物车进入结账流程",
            behavior={"preconditions": ["购物车非空"],
                      "trigger": {"type": "click", "target_description": "Proceed to Checkout 按钮"},
                      "postconditions": ["已登录: 进入收货表单"],
                      "error_cases": ["未登录: 重定向到登录页并提示需要先登录"]},
            evidence=[ev_checkout_anon], confidence=0.9)
        st_auth = e.record_state(
            "Authenticated area", "登录后的区域(结账表单/订单页)", [],
            ["登录成功"], confidence=0.7)
        e.record_edge(ft_checkout, st_auth, "REQUIRES",
                      evidence=[ev_checkout_anon], confidence=0.88,
                      note="匿名点击被重定向到登录页")
        e.resolve_hypothesis(hyp2, "confirmed", evidence=[ev_checkout_anon],
                             note="匿名 checkout 被重定向至登录页")
        print("[demo] edge: checkout REQUIRES auth (hypothesized -> confirmed)")

        # ---------------- 10. login: wrong password error, then success
        self.act(e.click, *LOGIN_USER, desc="click(用户名)")
        self.act(e.type_text, "alice", desc='type_text("alice")')
        self.act(e.click, *LOGIN_PASS, desc="click(密码)")
        self.act(e.type_text, "wrongpass", desc='type_text("********")')
        r, ev_login_bad = self.act(e.click, *LOGIN_BTN, desc="click(Log in)")
        o = self.obs()  # error bar
        # success — clear fields first (they may retain values)
        self.clear_and_type(LOGIN_USER, "alice", desc_prefix="alice")
        self.clear_and_type(LOGIN_PASS, "alice123", backspaces=10,
                            desc_prefix="********")
        r, ev_login = self.act(e.click, *LOGIN_BTN, desc="click(Log in)")
        o = self.obs()
        st_home_auth = e.record_state(
            "Authenticated home", "登录后首页: header 显示 'Hi, Alice' 与 Orders/Logout",
            visual_evidence=[o.frame_id], entry_conditions=["登录成功"],
            confidence=0.97)
        ft_login = e.record_feature(
            "Log in", "用户名密码认证",
            behavior={"trigger": {"type": "click", "target_description": "Log in 按钮"},
                      "inputs": [{"name": "用户名", "kind": "text"},
                                 {"name": "密码", "kind": "text"}],
                      "postconditions": ["成功: 回首页且 header 变为已登录态"],
                      "error_cases": ["错误凭证: 停留登录页并显示 'Invalid username or password'"],
                      "persistent_effects": ["刷新后登录态保留(会话 cookie)"]},
            evidence=[ev_login, ev_login_bad], confidence=0.95)
        e.record_edge(ft_login, st_home_auth, "TRANSITIONS_TO",
                      evidence=[ev_login], confidence=0.94)
        e.record_edge(ft_login, data_session, "MUTATES", evidence=[ev_login],
                      confidence=0.9)
        print("[demo] feature: login (error + success)")

        # ---------------- 11. checkout form: validation error then place order
        self.act(e.click, *CART_LINK, desc="click(Cart 链接)")
        r, ev_checkout_auth = self.act(e.click, *CART_CHECKOUT_DISCOUNT,
                                       desc="click(Proceed to Checkout)")
        o = self.obs()
        st_checkout = e.record_state(
            "Checkout form", "收货表单: 姓名/地址/城市/邮编 + 订单摘要 + Place Order",
            visual_evidence=[o.frame_id], entry_conditions=["已登录且购物车非空"],
            confidence=0.96)
        # empty-form validation error
        r, ev_place_empty = self.act(e.click, *PLACE_ORDER,
                                     desc="click(Place Order 空表单)")
        o = self.obs()
        # fill the form (banner shifted the wrap +80)
        self.clear_and_type(SHIP_NAME_B, "Alice Anderson", backspaces=2)
        self.clear_and_type(SHIP_ADDR_B, "12 Cloud Street", backspaces=2)
        self.clear_and_type(SHIP_CITY_B, "Sky City", backspaces=2)
        self.clear_and_type(SHIP_ZIP_B, "10001", backspaces=2)
        r, ev_place = self.act(e.click, *PLACE_ORDER_B, desc="click(Place Order)")
        o = self.obs()
        st_confirm = e.record_state(
            "Order confirmation", "下单确认页: 'Order confirmed' + 订单号 + 条目表",
            visual_evidence=[o.frame_id], entry_conditions=["收货表单校验通过"],
            confidence=0.97)
        data_order = e.record_data("Order", "已下单订单", confidence=0.92)
        ft_place = e.record_feature(
            "Place order", "提交收货信息创建订单",
            behavior={"preconditions": ["已登录", "购物车非空"],
                      "trigger": {"type": "click", "target_description": "Place Order 按钮"},
                      "inputs": [{"name": "姓名", "kind": "text"},
                                 {"name": "地址", "kind": "text"},
                                 {"name": "城市", "kind": "text"},
                                 {"name": "邮编", "kind": "text"}],
                      "postconditions": ["创建订单并跳转确认页", "购物车清空", "库存扣减"],
                      "error_cases": ["必填字段缺失时显示错误条且不创建订单"]},
            evidence=[ev_place, ev_place_empty], confidence=0.94)
        e.record_edge(ft_place, st_confirm, "TRANSITIONS_TO",
                      evidence=[ev_place], confidence=0.94)
        e.record_edge(ft_place, data_order, "MUTATES", evidence=[ev_place],
                      confidence=0.92)
        e.record_edge(ft_place, data_cart, "MUTATES", evidence=[ev_place],
                      confidence=0.9, note="下单后购物车清空")
        e.record_edge(ft_place, st_auth, "REQUIRES", evidence=[ev_checkout_anon],
                      confidence=0.85)
        print("[demo] feature: place_order (validation error + success)")

        # ---------------- 12. orders page + logout
        r, ev_orders = self.act(e.click, *ORDERS_LINK, desc="click(Orders 链接)")
        o = self.obs()
        st_orders = e.record_state(
            "Orders page", "订单列表页: 历史订单与刚创建的订单",
            visual_evidence=[o.frame_id], entry_conditions=["已登录点击 Orders"],
            confidence=0.95)
        ft_orders = e.record_feature(
            "View orders", "查看当前用户全部订单",
            behavior={"preconditions": ["已登录"],
                      "trigger": {"type": "click", "target_description": "Orders 链接"},
                      "postconditions": ["列出订单号/条目/金额"]},
            evidence=[ev_orders], confidence=0.9)
        e.record_edge(ft_orders, st_auth, "REQUIRES", evidence=[ev_orders],
                      confidence=0.8)
        r, ev_logout = self.act(e.click, *LOGOUT_LINK, desc="click(Logout)")
        o = self.obs()
        ft_logout = e.record_feature(
            "Log out", "退出登录, 回到匿名态且购物车清空",
            behavior={"trigger": {"type": "click", "target_description": "Logout 链接"},
                      "postconditions": ["header 恢复 Login 链接", "会话结束"]},
            evidence=[ev_logout], confidence=0.92)
        e.record_edge(ft_logout, st_home_anon, "TRANSITIONS_TO",
                      evidence=[ev_logout], confidence=0.9)
        e.record_edge(ft_logout, data_session, "MUTATES",
                      evidence=[ev_logout], confidence=0.88)
        print("[demo] feature: orders + logout")

        # ---------------- 13. finalize
        result = e.finalize()
        print(f"[demo] finalized: {result['summary']}")
        return {"session_id": self.sid, **result}


def main() -> None:
    p = argparse.ArgumentParser()
    p.add_argument("--controller", default="http://127.0.0.1:7800")
    args = p.parse_args()
    try:  # preflight: controller reachable?
        with httpx.Client(timeout=5, trust_env=False) as c:
            c.get(f"{args.controller.rstrip('/')}/api/apps").raise_for_status()
    except Exception:
        print(f"[demo] 无法连接 Controller ({args.controller})。\n"
              f"       请先启动: vendor/python/python.exe -m benchmark.server "
              f"--port 7800", file=sys.stderr)
        sys.exit(2)
    demo = Demo(args.controller)
    try:
        result = demo.run()
    except Exception as exc:
        print(f"[demo] FAILED: {exc!r}", file=sys.stderr)
        raise
    print(f"[demo] DONE — artifacts in runs/{result['session_id']}/")


if __name__ == "__main__":
    main()
