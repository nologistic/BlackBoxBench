import time, subprocess, os, json, urllib.request
from PIL import Image
from pyzbar.pyzbar import decode
import segno

DISP = os.environ.get("DISPLAY", ":99")
OUT = "/app/zhihu_qr.txt"; OUTBIG = "/app/zhihu_qr_big.txt"; STAT = "/app/_login_status.txt"
CUR = "/tmp/_qr_cur.png"
UI = "http://127.0.0.1:8400/input"

def grab():
    subprocess.run(["scrot","-o",CUR], env={"DISPLAY":DISP,"PATH":"/usr/bin:/bin"},
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=8, check=False)
    return Image.open(CUR).convert("RGB")

def find_qr(img):
    for b in [None,(400,440,660,670),(430,470,610,650),(380,300,620,640)]:
        c = img if b is None else img.crop(b)
        c = c.resize((c.width*2, c.height*2))
        r = decode(c)
        if r: return r[0].data.decode("utf-8","ignore")
    return None

def write_qr(data):
    qr = segno.make(data, error="m")
    with open(OUT,"w",encoding="utf-8") as fh: qr.terminal(out=fh, compact=True)
    with open(OUTBIG,"w",encoding="utf-8") as fh:
        for row in qr.matrix: fh.write("      "+"".join("██" if v else "  " for v in row)+"\n")

def reload_page():
    try:
        req = urllib.request.Request(UI, data=json.dumps({"kind":"navigation","command":"reload"}).encode(),
                                     headers={"content-type":"application/json"}, method="POST")
        urllib.request.urlopen(req, timeout=8)
    except Exception:
        pass

last = None; last_change = time.time()
while True:
    try:
        data = find_qr(grab())
        now = time.time()
        if data:
            if data != last:
                last = data; last_change = now
            write_qr(data)
            age = int(now - last_change)
            open(STAT,"w").write("QR_FRESH %s age=%ds len=%d" % (time.strftime("%H:%M:%S"), age, len(data)))
            # 同一个码超过 70s 未变 -> 视为将过期，刷新页面强制换新码
            if age > 70:
                reload_page(); last_change = now + 6  # 给页面加载留缓冲
        else:
            open(STAT,"w").write("NO_QR %s (logged in / loading)" % time.strftime("%H:%M:%S"))
    except Exception as e:
        open(STAT,"w").write("ERR %s %s" % (time.strftime("%H:%M:%S"), str(e)[:80]))
    time.sleep(8)
