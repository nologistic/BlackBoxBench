import json,re,sys
raw=open(sys.argv[1],encoding="utf-8").read()
m=re.search(r'\[\s*\{.*\}\s*\]', raw, re.S)
if not m: print("NO_JSON_FOUND"); sys.exit(2)
data=json.loads(m.group(0))
json.dump(data, open(sys.argv[2],"w"), ensure_ascii=False, indent=2)
reached=sum(1 for d in data if d.get("reached"))
print(f"COVERAGE {reached}/{len(data)} reached ({reached/len(data):.0%})")
