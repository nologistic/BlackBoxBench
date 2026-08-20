# 一次性 mock server: 按 docs/api_contract.md §3 返回假数据,并把 static/ 挂到 /
# 仅用于 Dashboard 静态自检,不进入生产代码路径。
# 用法: ../vendor/python/python.exe mock_server.py [port]
import io
import math
import sys
import time

from fastapi import FastAPI, Response
from fastapi.staticfiles import StaticFiles
from PIL import Image, ImageDraw, ImageFont
from pathlib import Path

PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 7811
W, H = 1440, 900
START = time.time()

app = FastAPI(title="BBB Dashboard Mock")

# ---------------------------------------------------------------- 假会话状态
SESSIONS = {
    "sess_mock_0001": {
        "session_id": "sess_mock_0001",
        "app_id": "ecommerce_demo",
        "status": "running",
        "created": START,
    }
}

BUDGET_MAX = {"max_actions": 500, "max_duration_s": 1800, "max_observations": 1000}

ACTION_POOL = [
    {"type": "click", "x": 812, "y": 431},
    {"type": "move_pointer", "x": 640, "y": 300},
    {"type": "click", "x": 350, "y": 612},
    {"type": "type_text", "text": "无线耳机"},
    {"type": "key_press", "key": "Enter"},
    {"type": "scroll", "dx": 0, "dy": 320},
    {"type": "double_click", "x": 511, "y": 208},
    {"type": "click", "x": 1204, "y": 96},
    {"type": "wait", "ms": 500},
    {"type": "click", "x": 700, "y": 760},
]


def fake_step(sid: str) -> int:
    sess = SESSIONS[sid]
    if sess["status"] != "running":
        return sess.get("final_step", 96)
    # step 随运行时间推进,封顶 180
    return min(180, 40 + int((time.time() - sess["created"]) / 2))


# ---------------------------------------------------------------- PNG 生成(PIL)
_font_big = ImageFont.load_default(size=42)
_font_mid = ImageFont.load_default(size=24)
_font_small = ImageFont.load_default(size=16)


def make_frame(frame_id: int, live: bool = False) -> bytes:
    img = Image.new("RGB", (W, H), (18, 22, 32))
    d = ImageDraw.Draw(img)
    # 模拟 UI: 顶栏 / 侧栏 / 卡片网格
    d.rectangle([0, 0, W, 64], fill=(28, 36, 54))
    d.rectangle([0, 64, 220, H], fill=(24, 30, 46))
    for i in range(3):
        for j in range(2):
            x0 = 260 + i * 390
            y0 = 110 + j * 360
            d.rectangle([x0, y0, x0 + 340, y0 + 300], outline=(60, 74, 110), width=2)
            d.rectangle([x0, y0, x0 + 340, y0 + 180], fill=(34, 44, 66))
    title = f"{'LIVE · ' if live else ''}FRAME {frame_id:06d}"
    d.text((30, 16), "MockShop 示例应用", font=_font_mid, fill=(220, 228, 240))
    d.text((W // 2 - 220, H // 2 - 30), title, font=_font_big, fill=(120, 160, 255))
    d.text((W // 2 - 220, H // 2 + 30), "1440 x 900 · mock 帧",
           font=_font_small, fill=(130, 140, 160))
    if live:
        # 模拟 live.png 叠加: 光标
        t = time.time() - START
        cx = int(W / 2 + 300 * math.sin(t))
        cy = int(H / 2 + 180 * math.cos(t * 0.7))
        d.line([cx - 14, cy, cx + 14, cy], fill=(255, 90, 90), width=3)
        d.line([cx, cy - 14, cx, cy + 14], fill=(255, 90, 90), width=3)
        d.ellipse([cx - 20, cy - 20, cx + 20, cy + 20], outline=(255, 90, 90), width=2)
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


# ---------------------------------------------------------------- /api/apps + sessions
@app.get("/api/apps")
def list_apps():
    return [{"id": "ecommerce_demo", "description": "示例电商 reference app"},
            {"id": "todo_demo", "description": "示例待办应用"}]


@app.get("/api/sessions")
def list_sessions():
    return [
        {**{k: v for k, v in s.items() if k != "created"},
         "step": fake_step(sid)}
        for sid, s in SESSIONS.items()
    ]


@app.post("/api/sessions")
def create_session(body: dict = None):
    body = body or {}
    sid = f"sess_mock_{len(SESSIONS) + 1:04d}"
    SESSIONS[sid] = {
        "session_id": sid,
        "app_id": body.get("app_id", "ecommerce_demo"),
        "status": "running",
        "created": time.time(),
    }
    return {"session_id": sid}


@app.get("/api/sessions/{sid}")
def session_status(sid: str):
    if sid not in SESSIONS:
        return Response('{"detail":"session_not_found","message":"mock"}',
                        status_code=404, media_type="application/json")
    sess = SESSIONS[sid]
    step = fake_step(sid)
    elapsed = time.time() - sess["created"]
    last_action = ACTION_POOL[step % len(ACTION_POOL)]
    return {
        "session_id": sid,
        "app_id": sess["app_id"],
        "status": sess["status"],
        "step": step,
        "elapsed_s": round(elapsed, 1),
        "current_frame": step + 3,
        "budget": {
            "actions_remaining": max(0, BUDGET_MAX["max_actions"] - step),
            "seconds_remaining": max(0, int(BUDGET_MAX["max_duration_s"] - elapsed)),
            "observations_remaining": max(0, BUDGET_MAX["max_observations"] - step * 2),
        },
        "budget_max": BUDGET_MAX,
        "counts": {"states": 3, "features": 3, "edges": 5, "hypotheses": 2, "unresolved": 1},
        "last_action": {"step": step, "timestamp": "2026-08-18T13:00:00Z", **last_action},
    }


@app.post("/api/sessions/{sid}/reset")
def reset_session(sid: str):
    if sid in SESSIONS:
        SESSIONS[sid]["created"] = time.time()
        SESSIONS[sid]["status"] = "running"
    return {"ok": True, "event": "reset"}


@app.post("/api/sessions/{sid}/close")
def close_session(sid: str):
    if sid in SESSIONS:
        SESSIONS[sid]["status"] = "closed"
        SESSIONS[sid]["final_step"] = fake_step(sid)
    return {"ok": True, "video_path": "runs/mock/video.webm"}


@app.post("/api/sessions/{sid}/replay")
def replay(sid: str, mode: str = "deterministic"):
    return {
        "mode": mode,
        "session_id": sid,
        "steps_replayed": 96,
        "actions_accepted": 96,
        "final_frame_match": True,
        "first_mismatch_step": None,
        "duration_s": 42.7,
        "report_path": f"runs/{sid}/replay_report.json",
    }


# ---------------------------------------------------------------- 帧
@app.get("/api/sessions/{sid}/live.png")
def live_png(sid: str):
    return Response(make_frame(fake_step(sid) + 3, live=True), media_type="image/png")


@app.get("/api/sessions/{sid}/frames/{frame_id}.png")
def frame_png(sid: str, frame_id: int):
    return Response(make_frame(frame_id), media_type="image/png")


# ---------------------------------------------------------------- trace
@app.get("/api/sessions/{sid}/trace")
def trace(sid: str):
    entries = []
    n = min(fake_step(sid), 120)
    for step in range(1, n + 1):
        entries.append({
            "kind": "observation", "frame_id": step + 2,
            "timestamp": f"2026-08-18T12:{step // 60:02d}:{step % 60:02d}Z",
        })
        entries.append({
            "kind": "action", "step": step,
            "action": ACTION_POOL[step % len(ACTION_POOL)],
            "accepted": True,
            "before_frame": step + 2, "after_frame": step + 3,
            "timestamp": f"2026-08-18T12:{step // 60:02d}:{step % 60:02d}.5Z",
        })
    return entries


# ---------------------------------------------------------------- topology / hypotheses
@app.get("/api/sessions/{sid}/topology")
def topology(sid: str):
    ev1 = {"step": 47, "before_frame": 49, "action": "click(812,431)", "after_frame": 50}
    ev2 = {"step": 63, "before_frame": 65, "action": "click(350,612)", "after_frame": 66}
    ev3 = {"step": 88, "before_frame": 90, "action": "key_press(Enter)", "after_frame": 91}
    return {
        "app_id": "ecommerce_demo",
        "graph_version": "1.0",
        "session_id": sid,
        "generated_at": "2026-08-18T13:00:00Z",
        "nodes": [
            {"id": "state_home", "type": "STATE", "name": "Home (guest)",
             "description": "未登录首页,展示商品网格与搜索框。",
             "visual_evidence": [5, 12],
             "entry_conditions": ["初始状态 S0"],
             "observed_elements": ["搜索框", "商品卡片网格", "登录入口"],
             "confidence": 0.95, "status": "confirmed",
             "created_step": 2, "updated_step": 12},
            {"id": "state_product_detail", "type": "STATE", "name": "Product Detail",
             "description": "商品详情状态,可见加入购物车按钮。",
             "visual_evidence": [50],
             "entry_conditions": ["从首页点击商品卡片"],
             "observed_elements": ["商品图", "价格", "Add to Cart 按钮", "数量输入框"],
             "confidence": 0.92, "status": "confirmed",
             "created_step": 47, "updated_step": 50, "evidence": [ev1]},
            {"id": "state_cart_nonempty", "type": "STATE", "name": "Cart with items",
             "description": "购物车非空,角标计数 ≥ 1。",
             "visual_evidence": [66],
             "entry_conditions": ["至少一次成功的 add_to_cart"],
             "observed_elements": ["购物车角标", "商品行", "删除按钮"],
             "confidence": 0.88, "status": "hypothesized",
             "created_step": 63, "updated_step": 66, "evidence": [ev2]},
            {"id": "feature_search_product", "type": "FEATURE", "name": "Search products",
             "description": "在搜索框输入关键词并回车,商品网格过滤为匹配结果。",
             "behavior": {
                 "preconditions": ["state_home"],
                 "trigger": {"type": "key_press", "target_description": "搜索框内回车"},
                 "inputs": [{"name": "keyword", "kind": "string", "constraints": "非空"}],
                 "postconditions": ["商品网格仅显示匹配商品"],
                 "persistent_effects": [],
                 "constraints": ["空关键词不过滤"],
                 "error_cases": ["无匹配时显示空态提示"],
             },
             "evidence": [ev3], "confidence": 0.9, "status": "confirmed",
             "created_step": 88, "updated_step": 91},
            {"id": "feature_add_to_cart", "type": "FEATURE", "name": "Add product to cart",
             "description": "点击 Add to Cart 后购物车计数加一并出现该商品。",
             "behavior": {
                 "preconditions": ["state_product_detail"],
                 "trigger": {"type": "click", "target_description": "Add to Cart 按钮"},
                 "inputs": [{"name": "quantity", "kind": "number", "constraints": "1-99"}],
                 "postconditions": ["购物车计数 +1", "商品出现在购物车"],
                 "persistent_effects": ["刷新后购物车仍保留该商品"],
                 "constraints": [],
                 "error_cases": ["数量为 0 时提示错误且不修改购物车"],
             },
             "evidence": [ev1, ev2], "confidence": 0.95, "status": "confirmed",
             "created_step": 47, "updated_step": 66},
            {"id": "feature_checkout", "type": "FEATURE", "name": "Checkout",
             "description": "从购物车发起结算;疑似需要登录。",
             "behavior": {
                 "preconditions": ["state_cart_nonempty"],
                 "trigger": {"type": "click", "target_description": "Checkout 按钮"},
                 "inputs": [],
                 "postconditions": ["出现结算表单或登录提示"],
                 "persistent_effects": [],
                 "constraints": [],
                 "error_cases": [],
             },
             "evidence": [], "confidence": 0.45, "status": "hypothesized",
             "created_step": 70, "updated_step": 70},
            {"id": "data_cart", "type": "DATA", "name": "Cart",
             "description": "服务器端持久化的购物车。",
             "confidence": 0.8, "status": "confirmed",
             "created_step": 63, "updated_step": 66, "evidence": [ev2]},
            {"id": "data_user_session", "type": "DATA", "name": "User Session",
             "description": "登录会话,影响 checkout 可用性。",
             "confidence": 0.5, "status": "uncertain",
             "created_step": 70, "updated_step": 70, "evidence": []},
        ],
        "edges": [
            {"id": "edge_0001", "source": "feature_add_to_cart", "target": "state_cart_nonempty",
             "type": "TRANSITIONS_TO", "evidence": [ev2], "confidence": 0.9, "status": "confirmed"},
            {"id": "edge_0002", "source": "feature_add_to_cart", "target": "data_cart",
             "type": "MUTATES", "evidence": [ev2], "confidence": 0.95, "status": "confirmed"},
            {"id": "edge_0003", "source": "feature_checkout", "target": "data_user_session",
             "type": "REQUIRES", "evidence": [], "confidence": 0.45, "status": "hypothesized"},
            {"id": "edge_0004", "source": "feature_search_product", "target": "state_home",
             "type": "TRANSITIONS_TO", "evidence": [ev3], "confidence": 0.6, "status": "uncertain"},
            {"id": "edge_0005", "source": "state_cart_nonempty", "target": "data_cart",
             "type": "PERSISTS_TO", "evidence": [], "confidence": 0.35, "status": "rejected"},
        ],
        "unresolved_questions": [
            {"hypothesis_id": "hyp_0003",
             "statement": "购物车可能在刷新后仍然保留",
             "status": "uncertain", "confidence": 0.4},
        ],
        "coverage_summary": {
            "states": 3, "features": 3, "data_entities": 2, "edges": 5,
            "confirmed_ratio": 0.63, "actions_used": 96,
        },
    }


@app.get("/api/sessions/{sid}/hypotheses")
def hypotheses(sid: str):
    return [
        {"hypothesis_id": "hyp_0003", "statement": "购物车可能在刷新后仍然保留",
         "status": "uncertain", "confidence": 0.4,
         "next_probe": "加入一件商品后按 F5 刷新", "evidence": []},
        {"hypothesis_id": "hyp_0004", "statement": "未登录时 checkout 会被引导至登录",
         "status": "unverified", "confidence": 0.55,
         "next_probe": "购物车非空时点击 Checkout 观察是否出现登录表单", "evidence": []},
    ]


@app.get("/api/sessions/{sid}/metrics")
def metrics(sid: str):
    return {"session_id": sid, "actions": fake_step(sid), "coverage": 0.63}


# ---------------------------------------------------------------- 静态 SPA(最后挂载)
STATIC_DIR = Path(__file__).parent / "static"
app.mount("/", StaticFiles(directory=str(STATIC_DIR), html=True), name="static")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=PORT, log_level="warning")
