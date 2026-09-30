import json,re,sys
raw=open(sys.argv[1],encoding="utf-8").read()
# Find all JSON-array blocks; the prompt may embed a literal example, so the
# LAST block (the model's actual output, at the tail) is the authoritative one.
blocks=re.findall(r'\[\s*\{.*?\}\s*\]', raw, re.S)
if not blocks: print("NO_JSON_FOUND"); sys.exit(2)
data=json.loads(blocks[-1])
json.dump(data, open(sys.argv[2],"w"), ensure_ascii=False, indent=2)
reached=sum(1 for d in data if d.get("reached"))
print(f"COVERAGE {reached}/{len(data)} reached ({reached/len(data):.0%})")
