import json,sys,os
def j(p):
    return json.load(open(p,encoding="utf-8")) if p and os.path.exists(p) else {}
metrics, vissim, cov, tgt, sid = j(sys.argv[1]), j(sys.argv[2]), None, sys.argv[4], sys.argv[5]
covp = sys.argv[3]
if os.path.exists(covp):
    c=json.load(open(covp)); cov=f"{sum(1 for x in c if x.get('reached'))}/{len(c)}"
gc=metrics.get("grade_counts",{})
tb=metrics.get("tokens_by_stage",{})
def g(k,d=""): return metrics.get(k,d)
row=[tgt,sid,gc.get("full",0),gc.get("partial",0),gc.get("placeholder",0),gc.get("broken",0),
     g("wfs"),g("persistence_wfs"),cov or "",vissim.get("visual_similarity",""),
     g("effective_action_rate"),g("invalid_action_rate"),g("platform_failures"),
     g("repro_revisions"),g("illusion_rate"),
     tb.get("explore",""),tb.get("reproduce",""),tb.get("evaluate",""),
     g("tokens_total"),g("tokens_per_fidelity_point")]
print(",".join(str(x) for x in row))
