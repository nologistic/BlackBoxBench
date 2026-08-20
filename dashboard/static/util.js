// 通用小工具
export const $ = (sel, root = document) => root.querySelector(sel);

export function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, c => ({
    "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
  }[c]));
}

export function fmtElapsed(sec) {
  if (sec == null || isNaN(sec)) return "—";
  sec = Math.floor(sec);
  const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), s = sec % 60;
  const mm = String(m).padStart(2, "0"), ss = String(s).padStart(2, "0");
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`;
}

// 把 action 对象格式化为 "CLICK 817,428" 风格
export function formatAction(a) {
  if (a == null) return "—";
  if (typeof a === "string") return a;
  const t = (a.type || "?").toUpperCase();
  switch (a.type) {
    case "click":
    case "double_click":
    case "move_pointer":
    case "mouse_down":
    case "mouse_up":
      return `${t} ${a.x},${a.y}`;
    case "drag":
      return `DRAG ${a.x1},${a.y1}→${a.x2},${a.y2}`;
    case "type_text":
      return `TYPE_TEXT "${a.text ?? ""}"`;
    case "key_press":
    case "key_down":
    case "key_up":
      return `${t} ${a.key ?? ""}`;
    case "scroll":
      return `SCROLL ${a.dx ?? 0},${a.dy ?? 0}`;
    case "wait":
      return `WAIT ${a.ms ?? 0}ms`;
    default:
      return t + (a.x != null ? ` ${a.x},${a.y}` : "");
  }
}

// 从 trace 条目里归一化出 action 信息;非 action 条目返回 null
export function traceAction(e) {
  if (!e || typeof e !== "object") return null;
  if (e.kind && e.kind !== "action") return null;
  if (e.type === "observation" || e.type === "observe") return null;
  const action = e.action ?? (e.type && e.type !== "action" ? e : null);
  if (!action) return null;
  return {
    step: e.step ?? null,
    action,
    accepted: e.accepted !== false,
    beforeFrame: e.before_frame ?? e.beforeFrame ?? null,
    afterFrame: e.after_frame ?? e.afterFrame ?? null,
    timestamp: e.timestamp ?? null,
  };
}

export const isClickLike = a =>
  a && (a.type === "click" || a.type === "double_click") && a.x != null && a.y != null;

// 帧坐标同步缩放:把 1440×900 原始坐标映射到 canvas 实际像素
export function syncCanvas(canvas, img, frameW = 1440, frameH = 900) {
  const w = img.clientWidth, h = img.clientHeight;
  if (!w || !h) return null;
  if (canvas.width !== w || canvas.height !== h) {
    canvas.width = w;
    canvas.height = h;
  }
  return { sx: w / frameW, sy: h / frameH };
}

/* ---------- 弹层 ---------- */
export function openModal(html, { wide = false } = {}) {
  const root = $("#modalRoot");
  root.innerHTML = `<div class="modal${wide ? " wide" : ""}">${html}</div>`;
  root.hidden = false;
  root.onclick = e => { if (e.target === root) closeModal(); };
  return root.firstElementChild;
}
export function closeModal() {
  const root = $("#modalRoot");
  root.hidden = true;
  root.innerHTML = "";
}

let errTimer = null;
export function showError(msg) {
  const banner = $("#errorBanner");
  $("#errorText").textContent = String(msg);
  banner.hidden = false;
  clearTimeout(errTimer);
  errTimer = setTimeout(() => { banner.hidden = true; }, 8000);
}
