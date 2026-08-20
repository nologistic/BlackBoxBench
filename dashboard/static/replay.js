// 视图 3: Session Replay(基于 trace 的逐帧可视化重放)
import * as api from "./api.js";
import { $, formatAction, traceAction, isClickLike, syncCanvas, showError } from "./util.js";

const BASE_INTERVAL = 900; // 1× 时每步毫秒数

let sid = null;
let steps = [];        // [{step, action, afterFrame}]
let idx = 0;
let playing = false;
let timer = null;
let img, canvas;

export function start(sessionId) {
  sid = sessionId;
  steps = [];
  idx = 0;
  pause();
  img = $("#replayImg");
  canvas = $("#replayOverlay");
  img.onload = () => drawOverlay();
  $("#replayEmpty").hidden = true;

  $("#rpPlay").onclick = togglePlay;
  $("#rpPrev").onclick = () => { pause(); seek(idx - 1); };
  $("#rpNext").onclick = () => { pause(); seek(idx + 1); };
  $("#rpSpeed").onchange = () => { if (playing) { pause(); play(); } };
  $("#rpSlider").oninput = e => { pause(); seek(Number(e.target.value)); };

  loadTrace();
}

export function stop() {
  pause();
  if (img) img.onload = null;
  sid = null;
}

async function loadTrace() {
  let trace;
  try {
    trace = await api.getTrace(sid);
  } catch (e) {
    showError(e.message);
    return;
  }
  steps = (Array.isArray(trace) ? trace : trace.entries ?? [])
    .map(traceAction)
    .filter(e2 => e2 && e2.accepted && e2.afterFrame != null)
    .sort((a, b) => (a.step ?? 0) - (b.step ?? 0));

  const slider = $("#rpSlider");
  slider.max = Math.max(0, steps.length - 1);
  $("#replayMeta").textContent = `${steps.length} 个已接受 action`;

  if (steps.length === 0) {
    $("#replayEmpty").hidden = false;
    $("#rpPos").textContent = "0 / 0";
    img.removeAttribute("src");
    return;
  }
  seek(0);
}

function seek(i) {
  if (steps.length === 0) return;
  idx = Math.max(0, Math.min(steps.length - 1, i));
  $("#rpSlider").value = idx;
  $("#rpPos").textContent = `${idx + 1} / ${steps.length}`;
  img.src = api.frameUrl(sid, steps[idx].afterFrame);
}

function togglePlay() { playing ? pause() : play(); }

function play() {
  if (steps.length === 0) return;
  if (idx >= steps.length - 1) seek(0);  // 播到尾后再次播放则从头开始
  playing = true;
  $("#rpPlay").textContent = "⏸ 暂停";
  schedule();
}

function pause() {
  playing = false;
  clearTimeout(timer);
  timer = null;
  const btn = $("#rpPlay");
  if (btn) btn.textContent = "▶ 播放";
}

function schedule() {
  const speed = Number($("#rpSpeed").value) || 1;
  timer = setTimeout(() => {
    if (!playing) return;
    if (idx >= steps.length - 1) { pause(); return; }
    seek(idx + 1);
    schedule();
  }, BASE_INTERVAL / speed);
}

/* ---------- 叠加层: action 文本 + 点击位置标记 ---------- */
function drawOverlay() {
  if (!sid || steps.length === 0) return;
  const scale = syncCanvas(canvas, img);
  if (!scale) return;
  const ctx = canvas.getContext("2d");
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const cur = steps[idx];
  const label = `step ${cur.step ?? "?"} · ${formatAction(cur.action)}`;

  // 顶部动作标签
  ctx.font = "600 14px Consolas, monospace";
  const tw = ctx.measureText(label).width;
  ctx.fillStyle = "rgba(10,14,22,.85)";
  ctx.fillRect(10, 10, tw + 20, 26);
  ctx.strokeStyle = "rgba(79,140,255,.8)";
  ctx.lineWidth = 1;
  ctx.strokeRect(10, 10, tw + 20, 26);
  ctx.fillStyle = "#9ec5ff";
  ctx.fillText(label, 20, 28);

  // 点击位置标记(静态: 圆环 + 准星)
  const a = cur.action;
  if (isClickLike(a)) {
    const x = a.x * scale.sx, y = a.y * scale.sy;
    const r = 22 * scale.sx;
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.strokeStyle = "rgba(255,80,80,.95)";
    ctx.lineWidth = 2.5;
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(x, y, r * 0.45, 0, Math.PI * 2);
    ctx.strokeStyle = "rgba(79,140,255,.9)";
    ctx.lineWidth = 2;
    ctx.stroke();
    const cross = 13 * scale.sx;
    ctx.beginPath();
    ctx.moveTo(x - cross, y); ctx.lineTo(x + cross, y);
    ctx.moveTo(x, y - cross); ctx.lineTo(x, y + cross);
    ctx.strokeStyle = "rgba(255,80,80,.95)";
    ctx.lineWidth = 2;
    ctx.stroke();
  }
}
