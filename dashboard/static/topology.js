// 视图 2: Topology Graph(内联 SVG 有向图 + 节点详情 + evidence 帧查看)
import * as api from "./api.js";
import { $, esc, openModal, showError } from "./util.js";

const SVG_W = 1160;
const NODE_W = 280, NODE_H = 56, V_GAP = 16, TOP_PAD = 46, BOT_PAD = 24;
const COLS = ["STATE", "FEATURE", "DATA"];
const COL_X = { STATE: 30, FEATURE: 440, DATA: 850 };
const COLORS = { STATE: "#4f8cff", FEATURE: "#3fb950", DATA: "#a371f7" };
const FILLS = { STATE: "#16233f", FEATURE: "#12281a", DATA: "#241a38" };

let sid = null;
let timers = [];
let graph = null;      // 最近一次 topology JSON
let selectedId = null;

export function start(sessionId) {
  sid = sessionId;
  graph = null;
  selectedId = null;
  $("#nodeDetail").innerHTML = `<span class="dim">点击图中节点查看详情</span>`;
  $("#evidenceViewer").hidden = true;
  $("#topoWrap").addEventListener("click", onSvgClick);
  pollTopology();
  pollHypotheses();
  timers.push(setInterval(pollTopology, 1500));
  timers.push(setInterval(pollHypotheses, 1500));
}

export function stop() {
  timers.forEach(clearInterval);
  timers = [];
  $("#topoWrap").removeEventListener("click", onSvgClick);
  sid = null;
}

/* ---------- 数据轮询 ---------- */
async function pollTopology() {
  if (!sid) return;
  try {
    graph = await api.getTopology(sid);
  } catch (e) {
    showError(e.message);
    return;
  }
  renderGraph();
  renderUnresolved();
  if (selectedId) renderDetail();   // 数据更新后刷新详情

  // 会话结束后 topology 不再变化,停止轮询
  const st = await api.getStatus(sid).catch(() => null);
  if (st && (st.status === "closed" || st.status === "failed")) {
    timers.forEach(clearInterval);
    timers = [];
  }
}

async function pollHypotheses() {
  if (!sid) return;
  try {
    const hyps = await api.getHypotheses(sid);
    renderHypotheses(Array.isArray(hyps) ? hyps : hyps.hypotheses ?? []);
  } catch (e) {
    showError(e.message);
  }
}

/* ---------- 布局: 三列,按 created_step 排序均布 ---------- */
function layout(nodes) {
  const pos = {};
  const groups = { STATE: [], FEATURE: [], DATA: [] };
  for (const n of nodes) (groups[n.type] ?? groups.DATA).push(n);
  let maxRows = 0;
  for (const t of COLS) {
    const g = groups[t].sort((a, b) => (a.created_step ?? 0) - (b.created_step ?? 0));
    maxRows = Math.max(maxRows, g.length);
    g.forEach((n, i) => {
      pos[n.id] = { x: COL_X[t], y: TOP_PAD + i * (NODE_H + V_GAP), node: n };
    });
  }
  return { pos, height: TOP_PAD + maxRows * (NODE_H + V_GAP) + BOT_PAD };
}

/* ---------- 渲染 ---------- */
function renderGraph() {
  const nodes = graph?.nodes ?? [];
  const edges = graph?.edges ?? [];
  const wrap = $("#topoWrap");

  const cs = graph?.coverage_summary;
  $("#topoMeta").textContent =
    `${nodes.length} 节点 · ${edges.length} 边` +
    (cs?.confirmed_ratio != null ? ` · confirmed ${(cs.confirmed_ratio * 100).toFixed(0)}%` : "");

  if (nodes.length === 0) {
    wrap.innerHTML = `<div class="empty-inline dim">拓扑为空 —— Agent 尚未提交 discovery。</div>`;
    return;
  }

  const { pos, height } = layout(nodes);
  let svg = `<svg viewBox="0 0 ${SVG_W} ${height}" xmlns="http://www.w3.org/2000/svg">`;
  svg += `<defs>
    <marker id="arr" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
      <path d="M0,0 L10,5 L0,10 z" fill="#8b94a7"></path>
    </marker>
    <marker id="arr-rej" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
      <path d="M0,0 L10,5 L0,10 z" fill="#f85149"></path>
    </marker>
  </defs>`;

  for (const t of COLS) {
    svg += `<text class="col-header" x="${COL_X[t] + NODE_W / 2}" y="26" text-anchor="middle" fill="${COLORS[t]}">${t}</text>`;
  }

  // 边
  for (const e of edges) {
    const s = pos[e.source], t = pos[e.target];
    if (!s || !t) continue;
    const conf = Math.max(0, Math.min(1, e.confidence ?? 0.5));
    const rejected = e.status === "rejected";
    const hypothesized = e.status === "hypothesized" || e.status === "uncertain";
    const w = 1 + 3 * conf;
    const op = 0.3 + 0.7 * conf;
    const stroke = rejected ? "#f85149" : "#8b94a7";
    const dash = rejected ? `stroke-dasharray="7 5"` : hypothesized ? `stroke-dasharray="4 4"` : "";

    // 起点/终点:按相对方位取左右边缘中点
    let x1, y1 = s.y + NODE_H / 2, x2, y2 = t.y + NODE_H / 2;
    if (t.x > s.x) { x1 = s.x + NODE_W; x2 = t.x; }
    else if (t.x < s.x) { x1 = s.x; x2 = t.x + NODE_W; }
    else { x1 = s.x + NODE_W; x2 = t.x + NODE_W; }  // 同列:右侧绕出
    const dx = Math.max(50, Math.abs(x2 - x1) * 0.45);
    const d = `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`;
    const mx = (x1 + x2) / 2, my = (y1 + y2) / 2 - 5;

    svg += `<path d="${d}" fill="none" stroke="${stroke}" stroke-width="${w.toFixed(1)}"
      stroke-opacity="${op.toFixed(2)}" ${dash}
      marker-end="url(#${rejected ? "arr-rej" : "arr"})"><title>${esc(e.id ?? "")} ${esc(e.type)} (${(conf * 100).toFixed(0)}%, ${esc(e.status ?? "")})</title></path>`;
    svg += `<text class="edge-label" x="${mx}" y="${my}" text-anchor="middle" opacity="${op.toFixed(2)}">${esc(e.type)}</text>`;
  }

  // 节点
  for (const t of COLS) {
    for (const id of Object.keys(pos)) {
      const p = pos[id];
      if (p.node.type !== t) continue;
      const n = p.node;
      const conf = Math.max(0, Math.min(1, n.confidence ?? 0));
      const dashed = n.status === "hypothesized" || n.status === "uncertain";
      const sel = id === selectedId ? " selected" : "";
      const name = n.name ?? n.id;
      const shown = name.length > 30 ? name.slice(0, 29) + "…" : name;
      svg += `<g class="node${sel}" data-id="${esc(id)}">
        <rect class="node-box" x="${p.x}" y="${p.y}" width="${NODE_W}" height="${NODE_H}" rx="9"
          fill="${FILLS[t]}" stroke="${COLORS[t]}" stroke-width="1.6"
          ${dashed ? `stroke-dasharray="6 4"` : ""}></rect>
        <text class="node-name" x="${p.x + 12}" y="${p.y + 23}">${esc(shown)}</text>
        <text class="node-conf" x="${p.x + 12}" y="${p.y + 42}">confidence ${(conf * 100).toFixed(0)}% · ${esc(n.status ?? "—")}</text>
        <title>${esc(n.id)}</title>
      </g>`;
    }
  }

  svg += `</svg>`;
  wrap.innerHTML = svg;
}

function onSvgClick(e) {
  const g = e.target.closest("g.node");
  if (!g) return;
  selectedId = g.dataset.id;
  $("#evidenceViewer").hidden = true;
  renderGraph();      // 重绘选中高亮
  renderDetail();
}

/* ---------- 节点详情 ---------- */
function renderDetail() {
  const n = (graph?.nodes ?? []).find(x => x.id === selectedId);
  const box = $("#nodeDetail");
  if (!n) { box.innerHTML = `<span class="dim">点击图中节点查看详情</span>`; return; }

  const conf = Math.max(0, Math.min(1, n.confidence ?? 0));
  let html = `
    <div class="nd-head">
      <span class="nd-name">${esc(n.name ?? n.id)}</span>
      <span class="chip ${esc(n.type)}">${esc(n.type)}</span>
      <span class="chip st-${esc(n.status ?? "")}">${esc(n.status ?? "—")}</span>
    </div>
    <div class="nd-kv"><span>ID</span><b class="mono">${esc(n.id)}</b></div>
    <div class="nd-kv"><span>Confidence</span><b class="mono">${(conf * 100).toFixed(0)}%</b></div>
    ${n.created_step != null ? `<div class="nd-kv"><span>Created Step</span><b class="mono">${esc(n.created_step)}</b></div>` : ""}
    ${n.updated_step != null ? `<div class="nd-kv"><span>Updated Step</span><b class="mono">${esc(n.updated_step)}</b></div>` : ""}
    ${n.description ? `<div class="nd-desc">${esc(n.description)}</div>` : ""}`;

  // STATE 附加字段
  if (n.type === "STATE") {
    html += listSection("Entry Conditions", n.entry_conditions);
    html += listSection("Observed Elements", n.observed_elements);
    html += framesSection("Visual Evidence", n.visual_evidence);
  }

  // FEATURE behavior
  if (n.type === "FEATURE" && n.behavior) {
    const b = n.behavior;
    html += `<div class="nd-sec"><h4>Behavior</h4>`;
    html += listSection("Preconditions", b.preconditions, true);
    if (b.trigger) {
      const tr = typeof b.trigger === "object"
        ? `${b.trigger.type ?? ""}${b.trigger.target_description ? " · " + b.trigger.target_description : ""}`
        : String(b.trigger);
      html += `<div class="nd-kv"><span>Trigger</span><b>${esc(tr)}</b></div>`;
    }
    if (Array.isArray(b.inputs) && b.inputs.length) {
      html += `<div class="nd-sec"><h4>Inputs</h4><ul>` + b.inputs.map(i =>
        `<li>${esc(i.name ?? "")}${i.kind ? ` <span class="dim">(${esc(i.kind)})</span>` : ""}${i.constraints ? ` — ${esc(i.constraints)}` : ""}</li>`
      ).join("") + `</ul></div>`;
    }
    html += listSection("Postconditions", b.postconditions, true);
    html += listSection("Persistent Effects", b.persistent_effects, true);
    html += listSection("Constraints", b.constraints, true);
    html += listSection("Error Cases", b.error_cases, true);
    html += `</div>`;
  }

  // Evidence 列表
  const ev = Array.isArray(n.evidence) ? n.evidence : [];
  html += `<div class="nd-sec"><h4>Evidence(${ev.length})</h4>`;
  if (ev.length === 0) {
    html += `<div class="dim" style="font-size:12px">无 evidence(hypothesized 节点允许为空)</div>`;
  } else {
    ev.forEach((e2, i) => {
      html += `<button class="ev-item" data-ev="${i}">
        step ${esc(e2.step ?? "?")} · before frame ${esc(e2.before_frame ?? "?")} · ${esc(e2.action ?? "")} · after frame ${esc(e2.after_frame ?? "?")}
      </button>`;
    });
  }
  html += `</div>`;

  box.innerHTML = html;
  box.querySelectorAll(".ev-item").forEach(btn => {
    btn.onclick = () => showEvidence(ev[Number(btn.dataset.ev)]);
  });
  box.querySelectorAll("[data-frame]").forEach(el => {
    el.onclick = () => zoomFrame(Number(el.dataset.frame));
  });
}

function listSection(title, arr, nested = false) {
  if (!Array.isArray(arr) || arr.length === 0) return "";
  const body = arr.map(x => `<li>${esc(typeof x === "object" ? JSON.stringify(x) : x)}</li>`).join("");
  return nested
    ? `<div class="nd-sec"><h4>${esc(title)}</h4><ul>${body}</ul></div>`
    : `<div class="nd-sec"><h4>${esc(title)}</h4><ul>${body}</ul></div>`;
}

function framesSection(title, frames) {
  if (!Array.isArray(frames) || frames.length === 0) return "";
  const body = frames.map(f =>
    `<button class="ev-item" data-frame="${esc(f)}">frame ${esc(f)}</button>`).join("");
  return `<div class="nd-sec"><h4>${esc(title)}</h4>${body}</div>`;
}

/* ---------- Evidence before/after 并排查看 ---------- */
function showEvidence(e2) {
  const viewer = $("#evidenceViewer");
  viewer.hidden = false;
  viewer.innerHTML = `
    <div class="sub-title mono">step ${esc(e2.step ?? "?")} · ${esc(e2.action ?? "")}</div>
    <div class="ev-pair">
      <figure class="ev-frame">
        <img src="${api.frameUrl(sid, e2.before_frame)}" data-zoom="${esc(e2.before_frame)}" alt="before">
        <figcaption>before · frame ${esc(e2.before_frame ?? "?")}</figcaption>
      </figure>
      <figure class="ev-frame">
        <img src="${api.frameUrl(sid, e2.after_frame)}" data-zoom="${esc(e2.after_frame)}" alt="after">
        <figcaption>after · frame ${esc(e2.after_frame ?? "?")}</figcaption>
      </figure>
    </div>`;
  viewer.querySelectorAll("img[data-zoom]").forEach(img => {
    img.onclick = () => zoomFrame(Number(img.dataset.zoom));
  });
  viewer.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

function zoomFrame(frameId) {
  if (frameId == null || isNaN(frameId)) return;
  openModal(`<h3>Frame ${esc(frameId)}</h3>
    <img class="zoom" src="${api.frameUrl(sid, frameId)}" alt="frame ${esc(frameId)}">`, { wide: true });
}

/* ---------- 未决问题 / 假设 ---------- */
function renderUnresolved() {
  const list = graph?.unresolved_questions ?? [];
  const ul = $("#unresolvedList");
  if (list.length === 0) {
    ul.innerHTML = `<li class="hyp-empty">无未决问题</li>`;
    return;
  }
  ul.innerHTML = list.map(q => `
    <li>
      <span class="chip st-${esc(q.status ?? "")} hyp-st">${esc(q.status ?? "?")}</span>
      <span>${esc(q.statement ?? "")}</span>
      <span class="hyp-conf">${q.confidence != null ? (q.confidence * 100).toFixed(0) + "%" : ""}</span>
    </li>`).join("");
}

function renderHypotheses(hyps) {
  const ul = $("#hypList");
  if (hyps.length === 0) {
    ul.innerHTML = `<li class="hyp-empty">无假设</li>`;
    return;
  }
  ul.innerHTML = hyps.map(h => `
    <li title="${esc(h.next_probe ? "next probe: " + h.next_probe : "")}">
      <span class="chip st-${esc(h.status ?? "")} hyp-st">${esc(h.status ?? "?")}</span>
      <span>${esc(h.statement ?? "")}</span>
      <span class="hyp-conf">${esc(h.hypothesis_id ?? "")} ${h.confidence != null ? "· " + (h.confidence * 100).toFixed(0) + "%" : ""}</span>
    </li>`).join("");
}
