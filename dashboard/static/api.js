// /api/* 契约封装(全部相对路径,由 controller 同端口挂载)
async function req(path, opts = {}) {
  let res;
  try {
    res = await fetch(path, {
      headers: { "Content-Type": "application/json" },
      ...opts,
    });
  } catch (e) {
    throw new Error(`网络错误: ${path} · ${e.message}`);
  }
  if (!res.ok) {
    let detail = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      if (body.detail) detail = body.detail + (body.message ? `: ${body.message}` : "");
    } catch { /* 非 JSON 错误体 */ }
    throw new Error(`${path} → ${detail}`);
  }
  const ct = res.headers.get("content-type") || "";
  return ct.includes("json") ? res.json() : res;
}

export const getApps = () => req("/api/apps");
export const getSessions = () => req("/api/sessions");
export const createSession = body =>
  req("/api/sessions", { method: "POST", body: JSON.stringify(body) });
export const getStatus = sid => req(`/api/sessions/${encodeURIComponent(sid)}`);
export const resetSession = sid =>
  req(`/api/sessions/${encodeURIComponent(sid)}/reset`, { method: "POST", body: "{}" });
export const closeSession = sid =>
  req(`/api/sessions/${encodeURIComponent(sid)}/close`, { method: "POST", body: "{}" });
export const replaySession = (sid, mode = "deterministic") =>
  req(`/api/sessions/${encodeURIComponent(sid)}/replay?mode=${mode}`, { method: "POST", body: "{}" });
export const getTrace = sid => req(`/api/sessions/${encodeURIComponent(sid)}/trace`);
export const getTopology = sid => req(`/api/sessions/${encodeURIComponent(sid)}/topology`);
export const getHypotheses = sid => req(`/api/sessions/${encodeURIComponent(sid)}/hypotheses`);

export const frameUrl = (sid, id) =>
  `/api/sessions/${encodeURIComponent(sid)}/frames/${id}.png`;
export const liveUrl = sid =>
  `/api/sessions/${encodeURIComponent(sid)}/live.png?t=${Date.now()}`;
