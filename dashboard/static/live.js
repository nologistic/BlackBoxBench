// 视图 1: Live Observer
import * as api from "./api.js";
import { $, esc, fmtElapsed, formatAction, traceAction, isClickLike, syncCanvas, showError } from "./util.js";

const RIPPLE_TTL = 3000;   // 涟漪保留 3 秒
const RING_MS = 1200;      // 单圈扩散时长

let sid = null;
let timers = [];
let raf = 0;
let ripples = [];          // {x, y, type, label, t0}
let lastActionKey = null;
let img, canvas;

export function start(sessionId) {
  sid = sessionId;
  ripples = [];
  lastActionKey = null;
  img = $("#liveImg");
  canvas = $("#liveOverlay");

  const refreshImg = () => { img.src = api.liveUrl(sid); };
  refreshImg();
  pollStatus();
  pollTrace();
  timers.push(setInterval(refreshImg, 700));
  timers.push(setInterval(pollStatus, 700));
  timers.push(setInterval(pollTrace, 1500));

  img.onload = () => syncCanvas(canvas, img);
  window.addEventListener("resize", onResize);
  raf = requestAnimationFrame(draw);
}

export function stop() {
  timers.forEach(clearInterval);
  timers = [];
  cancelAnimationFrame(raf);
  window.removeEventListener("resize", onResize);
  if (img) img.onload = null;
  sid = null;
}

const onResize = () => img && syncCanvas(canvas, img);

/* ---------- 状态轮询 ---------- */
async function pollStatus() {
  if (!sid) return;
  let st;
  try {
    st = await api.getStatus(sid);
  } catch (e) {
    showError(e.message);
    return;
  }
  renderStatus(st);

  // 会话结束(closed/failed)或归档(archived,只读)后停止一切轮询,保留最后状态
  if (st.status === "closed" || st.status === "failed" || st.status === "archived") {
    timers.forEach(clearInterval);
    timers = [];
    return;
  }

  const la = st.last_action;
  if (la && isClickLike(la)) {
    const key = `${la.step ?? ""}|${la.type}|${la.x},${la.y}|${la.timestamp ?? ""}`;
    if (key !== lastActionKey) {
      lastActionKey = key;
      ripples.push({
        x: la.x, y: la.y, type: la.type,
        label: `${la.type.toUpperCase()} ${la.x},${la.y}`,
        t0: performance.now(),
      });
    }
  }
}

function renderStatus(st) {
  const badge = $("#stBadge");
  badge.textContent = st.status ?? "—";
  badge.className = `badge ${st.status ?? ""}`;
  $("#stSid").textContent = st.session_id ?? sid ?? "";
  $("#stStep").textContent = st.step ?? "—";
  $("#stElapsed").textContent = fmtElapsed(st.elapsed_s ?? st.elapsed);
  $("#stFrame").textContent = st.current_frame ?? "—";
  $("#liveFrameTag").textContent =
    st.current_frame != null ? `frame ${st.current_frame}` : "";

  // 预算余量进度条(有 max 时按比例;无 max 时仅显示数值)
  const bud = st.budget ?? {};
  const max = st.budget_max ?? st.budget_limits ?? {};
  setBar("#bgActions", "#bgActionsTxt", bud.actions_remaining, max.max_actions);
  setBar("#bgSeconds", "#bgSecondsTxt", bud.seconds_remaining, max.max_duration_s);
  setBar("#bgObs", "#bgObsTxt", bud.observations_remaining, max.max_observations);

  const c = st.counts ?? {};
  $("#cStates").textContent = c.states ?? 0;
  $("#cFeatures").textContent = c.features ?? 0;
  $("#cEdges").textContent = c.edges ?? 0;
  $("#cHyps").textContent = c.hypotheses ?? 0;
  $("#cUnresolved").textContent = c.unresolved ?? 0;

  $("#lastAction").textContent = st.last_action
    ? `${st.last_action.step != null ? st.last_action.step + " " : ""}${formatAction(st.last_action)}`
    : "—";
}

function setBar(barSel, txtSel, remaining, maxV) {
  const bar = $(barSel);
  if (remaining == null) {
    $(txtSel).textContent = "—";
    bar.style.width = "0%";
    return;
  }
  $(txtSel).textContent = maxV ? `${remaining}/${maxV}` : String(remaining);
  if (maxV) {
    const pct = Math.max(0, Math.min(100, (remaining / maxV) * 100));
    bar.style.width = pct + "%";
    bar.className = "bar-fill" + (pct < 10 ? " crit" : pct < 25 ? " low" : "");
  } else {
    bar.style.width = "100%";
    bar.className = "bar-fill";
  }
}

/* ---------- Previous Actions ---------- */
async function pollTrace() {
  if (!sid) return;
  let trace;
  try {
    trace = await api.getTrace(sid);
  } catch (e) {
    showError(e.message);
    return;
  }
  const entries = (Array.isArray(trace) ? trace : trace.entries ?? [])
    .map(traceAction)
    .filter(Boolean);
  const recent = entries.slice(-50).reverse();
  $("#actionList").innerHTML = recent.map(e =>
    `<li><span class="step-no">${esc(e.step ?? "·")}</span>${esc(formatAction(e.action))}</li>`
  ).join("");
}

/* ---------- 涟漪绘制 ---------- */
function draw(now) {
  if (!sid) return;
  raf = requestAnimationFrame(draw);
  const scale = syncCanvas(canvas, img);
  if (!scale) return;
  const ctx = canvas.getContext("2d");
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  ripples = ripples.filter(r => now - r.t0 < RIPPLE_TTL);
  for (const r of ripples) {
    const age = now - r.t0;
    const fade = 1 - age / RIPPLE_TTL;
    const p = Math.min(1, age / RING_MS);
    const x = r.x * scale.sx, y = r.y * scale.sy;

    // 扩散圆环(double_click 画两圈)
    const rings = r.type === "double_click" ? 2 : 1;
    for (let i = 0; i < rings; i++) {
      const pp = Math.max(0, Math.min(1, p - i * 0.25));
      if (pp <= 0) continue;
      ctx.beginPath();
      ctx.arc(x, y, 8 + 46 * pp * scale.sx, 0, Math.PI * 2);
      ctx.strokeStyle = `rgba(79,140,255,${(0.9 * (1 - pp) * fade).toFixed(3)})`;
      ctx.lineWidth = 2.5;
      ctx.stroke();
    }

    // 准星
    const cross = 11 * scale.sx;
    ctx.beginPath();
    ctx.moveTo(x - cross, y); ctx.lineTo(x + cross, y);
    ctx.moveTo(x, y - cross); ctx.lineTo(x, y + cross);
    ctx.strokeStyle = `rgba(255,80,80,${(0.95 * fade).toFixed(3)})`;
    ctx.lineWidth = 2;
    ctx.stroke();

    // 动作文本标签
    ctx.font = "600 12px Consolas, monospace";
    const tw = ctx.measureText(r.label).width;
    const lx = Math.min(x + 14, canvas.width - tw - 14);
    const ly = Math.max(16, y - 26);
    ctx.fillStyle = `rgba(10,14,22,${(0.82 * fade).toFixed(3)})`;
    ctx.fillRect(lx - 5, ly - 12, tw + 10, 18);
    ctx.strokeStyle = `rgba(79,140,255,${(0.7 * fade).toFixed(3)})`;
    ctx.lineWidth = 1;
    ctx.strokeRect(lx - 5, ly - 12, tw + 10, 18);
    ctx.fillStyle = `rgba(158,197,255,${fade.toFixed(3)})`;
    ctx.fillText(r.label, lx, ly + 1);
  }
}
