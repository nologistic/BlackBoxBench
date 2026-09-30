import json,sys
s=json.load(open(f"review_specs/{sys.argv[1]}.json"))
for f in s.get("features",[]):
    steps=" | ".join(f.get("steps",[]))[:180]
    print(f'- {f["id"]}: {f.get("name","")} :: {steps}')
