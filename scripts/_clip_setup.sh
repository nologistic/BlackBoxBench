#!/bin/bash
# One-time CLIP environment build (CPU torch + transformers + a CLIP checkpoint
# fetched from ModelScope, since HF/hf-mirror are unreachable on this host).
set -x
PIP="pip install --no-cache-dir --retries 10 --timeout 60"
# CPU torch from the reachable pytorch CPU wheel index; rest from tuna.
$PIP --index-url https://download.pytorch.org/whl/cpu torch || echo TORCH_FAIL
$PIP -i https://pypi.tuna.tsinghua.edu.cn/simple/ transformers pillow modelscope numpy || echo DEPS_FAIL
python3 - <<'PY'
import os
os.environ.setdefault("MODELSCOPE_CACHE","/models/ms")
try:
    from modelscope import snapshot_download
    d = snapshot_download("AI-ModelScope/clip-vit-base-patch32")
    print("WEIGHTS_DIR", d)
    from transformers import CLIPModel, CLIPProcessor
    import torch
    m = CLIPModel.from_pretrained(d); p = CLIPProcessor.from_pretrained(d)
    from PIL import Image
    img = Image.new("RGB",(224,224),(128,128,128))
    inp = p(images=img, return_tensors="pt")
    with torch.no_grad():
        f = m.get_image_features(**inp)
    print("CLIP_OK embed_dim", f.shape[-1])
except Exception as e:
    import traceback; traceback.print_exc(); print("CLIP_SETUP_FAIL", str(e)[:120])
PY
echo CLIP_BUILD_DONE
