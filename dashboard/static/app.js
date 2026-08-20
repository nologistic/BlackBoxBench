// 主控: 视图切换 / 会话选择 / 顶栏操作 / 全局会话列表轮询
import * as api from "./api.js";
import { $, esc, showError, openModal, closeModal } from "./util.js";
import * as liveView from "./live.js";
import * as topoView from "./topology.js";
import * as replayView from "./replay.js";

const views = { live: liveView, topology: topoView, replay: replayView };
const SECTIONS = { live: "#view-live", topology: "#view-topology", replay: "#view-replay" };

const state = { sid: null, view: "live", sessions: [] };
let activeView = null;

/* ---------- 弹层(openModal/closeModal 来自 util.js) ---------- */

/* ---------- 视图切换 ---------- */
function setView(name) {
  if (state.view === name || !views[name]) return;
  state.view = name;
  history.replaceState(null, "", "#/" + name);
  document.querySelectorAll("#viewTabs .tab").forEach(b =>
    b.classList.toggle("active", b.dataset.view === name));
  mountView();
}

function mountView() {
  if (activeView) { activeView.stop(); activeView = null; }
  Object.values(SECTIONS).forEach(sel => { $(sel).hidden = true; });
  $("#view-empty").hidden = true;

  if (!state.sid) { $("#view-empty").hidden = false; return; }
  const mod = views[state.view];
  $(SECTIONS[state.view]).hidden = false;
  mod.start(state.sid);
  activeView = mod;
}

function setSession(sid) {
  if (state.sid === sid) return;
  state.sid = sid;
  mountView();
  updateSessionBar();
}

/* ---------- 会话列表轮询 ---------- */
async function pollSessions() {
  try {
    const list = await api.getSessions();
    state.sessions = Array.isArray(list) ? list : (list.sessions ?? []);
    renderSessionSelect();
    if (!state.sid && state.sessions.length > 0) {
      setSession(sessionId(state.sessions[0]));
    } else if (state.sid && state.sessions.length > 0 &&
               !state.sessions.some(s => sessionId(s) === state.sid)) {
      // 当前会话已不在列表中(例如服务重启),回退到第一个
      setSession(sessionId(state.sessions[0]));
    }
    updateSessionBar();
  } catch (e) {
    showError(e.message);
  }
}

const sessionId = s => (typeof s === "string" ? s : s.session_id ?? s.id ?? "");
const sessionLabel = s => {
  if (typeof s === "string") return s;
  const st = s.status ? ` · ${s.status}` : "";
  const app = s.app_id ? ` (${s.app_id})` : "";
  return `${sessionId(s)}${app}${st}`;
};

function renderSessionSelect() {
  const sel = $("#sessionSelect");
  const cur = state.sid;
  if (state.sessions.length === 0) {
    sel.innerHTML = `<option value="">(无会话)</option>`;
    sel.disabled = true;
    return;
  }
  sel.disabled = false;
  sel.innerHTML = state.sessions
    .map(s => `<option value="${esc(sessionId(s))}">${esc(sessionLabel(s))}</option>`)
    .join("");
  if (cur && state.sessions.some(s => sessionId(s) === cur)) sel.value = cur;
}

function updateSessionBar() {
  const cur = state.sessions.find(s => sessionId(s) === state.sid);
  const archived = !!cur && typeof cur === "object" && cur.status === "archived";
  const has = !!state.sid && !archived;  // 归档会话只读,禁变更操作
  $("#btnReset").disabled = !has;
  $("#btnClose").disabled = !has;
  $("#btnReplay").disabled = !has;
}

/* ---------- 新建会话 ---------- */
async function openNewSessionModal() {
  let apps = [];
  try {
    const res = await api.getApps();
    apps = Array.isArray(res) ? res : (res.apps ?? []);
  } catch (e) {
    showError(e.message);
  }
  const appOpts = apps.map(a => {
    const id = typeof a === "string" ? a : a.id ?? a.app_id;
    const desc = typeof a === "object" && a.description ? ` — ${a.description}` : "";
    return `<option value="${esc(id)}">${esc(id)}${esc(desc)}</option>`;
  }).join("");

  const modal = openModal(`
    <h3>新建会话</h3>
    <div class="form-row">
      <label>App(来自 GET /api/apps)</label>
      ${apps.length
        ? `<select id="nsApp">${appOpts}</select>`
        : `<input id="nsApp" placeholder="app_id,如 ecommerce_demo" value="ecommerce_demo">`}
    </div>
    <div class="form-cols">
      <div class="form-row"><label>max_actions</label><input id="nsActions" type="number" placeholder="500"></div>
      <div class="form-row"><label>max_duration_s</label><input id="nsDuration" type="number" placeholder="1800"></div>
      <div class="form-row"><label>max_observations</label><input id="nsObs" type="number" placeholder="1000"></div>
    </div>
    <div class="form-row"><label>seed(可选)</label><input id="nsSeed" type="number" placeholder="留空则随机"></div>
    <div class="modal-actions">
      <button class="btn" id="nsCancel">取消</button>
      <button class="btn primary" id="nsOk">创建</button>
    </div>`);

  modal.querySelector("#nsCancel").onclick = closeModal;
  modal.querySelector("#nsOk").onclick = async () => {
    const appId = modal.querySelector("#nsApp").value.trim();
    if (!appId) { showError("app_id 不能为空"); return; }
    const body = { app_id: appId };
    const seed = modal.querySelector("#nsSeed").value;
    if (seed !== "") body.seed = Number(seed);
    const budget = {};
    const ma = modal.querySelector("#nsActions").value;
    const md = modal.querySelector("#nsDuration").value;
    const mo = modal.querySelector("#nsObs").value;
    if (ma !== "") budget.max_actions = Number(ma);
    if (md !== "") budget.max_duration_s = Number(md);
    if (mo !== "") budget.max_observations = Number(mo);
    if (Object.keys(budget).length) body.budget = budget;
    try {
      const res = await api.createSession(body);
      closeModal();
      await pollSessions();
      if (res.session_id) setSession(res.session_id);
    } catch (e) {
      showError(e.message);
    }
  };
}

/* ---------- 确定性重放报告 ---------- */
async function runDeterministicReplay() {
  if (!state.sid) return;
  if (!confirm(`对 ${state.sid} 执行确定性重放?\n(将 reset 环境并重放全部 action)`)) return;
  let report;
  try {
    report = await api.replaySession(state.sid, "deterministic");
  } catch (e) {
    showError(e.message);
    return;
  }
  const rows = Object.entries(report).map(([k, v]) => {
    let cls = "", val = v;
    if (typeof v === "boolean") { cls = v ? "ok" : "bad"; val = v ? "✔ 一致" : "✘ 不一致"; }
    else if (v == null) val = "—";
    else if (typeof v === "object") val = JSON.stringify(v);
    return `<div class="report-kv"><span>${esc(k)}</span><b class="${cls}">${esc(val)}</b></div>`;
  }).join("");
  openModal(`
    <h3>确定性重放报告 <span class="mono dim">${esc(state.sid)}</span></h3>
    ${rows}
    <div class="modal-actions"><button class="btn primary" onclick="this.closest('#modalRoot').hidden=true;this.closest('#modalRoot').innerHTML=''">关闭</button></div>`);
}

/* ---------- 事件绑定 ---------- */
$("#viewTabs").addEventListener("click", e => {
  const btn = e.target.closest(".tab");
  if (btn) setView(btn.dataset.view);
});
$("#sessionSelect").addEventListener("change", e => setSession(e.target.value));
$("#btnNew").onclick = openNewSessionModal;
$("#btnNewEmpty").onclick = openNewSessionModal;
$("#btnReset").onclick = async () => {
  if (!state.sid || !confirm(`重置会话 ${state.sid}?(环境回到 S0)`)) return;
  try { await api.resetSession(state.sid); } catch (e) { showError(e.message); }
};
$("#btnClose").onclick = async () => {
  if (!state.sid || !confirm(`关闭会话 ${state.sid}?该操作不可撤销。`)) return;
  try { await api.closeSession(state.sid); } catch (e) { showError(e.message); }
};
$("#btnReplay").onclick = runDeterministicReplay;
$("#errorClose").onclick = () => { $("#errorBanner").hidden = true; };

/* ---------- 启动 ---------- */
const hashView = location.hash.replace(/^#\//, "");
if (views[hashView]) {
  state.view = hashView;
  document.querySelectorAll("#viewTabs .tab").forEach(b =>
    b.classList.toggle("active", b.dataset.view === hashView));
}
pollSessions();
setInterval(pollSessions, 3000);
mountView();
