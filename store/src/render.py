#!/usr/bin/env python3
"""Renders Play Store graphics (screenshots, feature graphic, icon) from HTML with headless Chrome.

Usage: python3 store/src/render.py   (run from the repo root)
Raw app screenshots come from store/raw/ (captured from a debug build with demo data).
"""
import base64, pathlib, subprocess, tempfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
RAW, OUT = ROOT / "raw", ROOT / "play"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
OUT.mkdir(exist_ok=True)

FONT = "<link href='https://fonts.googleapis.com/css2?family=Inter:wght@500;700;800&display=swap' rel='stylesheet'>"
BASE_CSS = """
*{margin:0;box-sizing:border-box} body{font-family:Inter,Helvetica,Arial,sans-serif;overflow:hidden}
"""

ICON_SVG = """<svg viewBox='0 0 108 108' xmlns='http://www.w3.org/2000/svg'>
<rect width='108' height='108' fill='#0B6E4F'/>
<path fill='#fff' d='M34,36h40a6,6 0 0 1 6,6v22a6,6 0 0 1 -6,6h-26l-10,9v-9h-4a6,6 0 0 1 -6,-6v-22a6,6 0 0 1 6,-6z'/>
<path fill='#0B6E4F' d='M42,51h18v-6l11,8.5l-11,8.5v-6h-18z'/></svg>"""


def img(name):
    return "data:image/png;base64," + base64.b64encode((RAW / name).read_bytes()).decode()


def render(html, out, w, h):
    with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False) as f:
        f.write(f"<!doctype html><html><head><meta charset='utf-8'>{FONT}<style>{BASE_CSS}</style></head><body>{html}</body></html>")
    subprocess.run([CHROME, "--headless=new", "--hide-scrollbars", "--force-device-scale-factor=1",
                    f"--window-size={w},{h}", "--virtual-time-budget=4000",
                    f"--screenshot={out}", f"file://{f.name}"], check=True, capture_output=True)
    print("wrote", out.relative_to(ROOT.parent))


SHOTS = [
    ("home.png", "Forward the texts<br>that matter", "Simple rules. Runs quietly in the background.", "#E3F4EC", "#0B3D2C"),
    ("edit_bank.png", "Pick senders,<br>or forward everything", "Choose from your contacts or type any number or sender ID.", "#E8F0FB", "#13315C"),
    ("edit_all.png", "Built for<br>dual-SIM phones", "Forward from SIM 1, SIM 2 or both — and pick which SIM sends.", "#FFF1DC", "#5C3B0B"),
    ("activity.png", "See every<br>forward", "Failed sends retry automatically when signal returns.", "#EFE8FB", "#3A1F66"),
    ("setup.png", "Private by<br>design", "No internet access, no account, no tracking. Open source.", "#E3F4EC", "#0B3D2C"),
]

for i, (raw, title, sub, bg, ink) in enumerate(SHOTS, 1):
    html = f"""
<div style='width:1080px;height:1920px;background:{bg};display:flex;flex-direction:column;align-items:center;padding-top:110px'>
  <h1 style='font-size:84px;line-height:1.05;font-weight:800;color:{ink};text-align:center;letter-spacing:-2px'>{title}</h1>
  <p style='font-size:36px;line-height:1.35;font-weight:500;color:{ink};opacity:.75;text-align:center;max-width:820px;margin-top:28px'>{sub}</p>
  <div style='margin-top:64px;width:700px;height:1540px;border-radius:64px;background:#111;padding:14px;box-shadow:0 40px 90px rgba(0,0,0,.22)'>
    <img src='{img(raw)}' style='width:100%;height:100%;border-radius:52px;object-fit:cover;object-position:top;display:block'>
  </div>
</div>"""
    render(html, OUT / f"screenshot-{i}.png", 1080, 1920)

# Feature graphic 1024x500
feature = f"""
<div style='width:1024px;height:500px;background:linear-gradient(135deg,#0B6E4F 0%,#0A5A41 60%,#08402F 100%);position:relative;overflow:hidden;display:flex;align-items:center;padding-left:72px'>
  <div style='width:132px;height:132px;border-radius:32px;overflow:hidden;box-shadow:0 12px 30px rgba(0,0,0,.25);flex:none'>{ICON_SVG}</div>
  <div style='margin-left:40px;color:#fff;max-width:420px'>
    <div style='font-size:64px;font-weight:800;letter-spacing:-1.5px'>SMS Relay</div>
    <div style='font-size:28px;font-weight:500;opacity:.9;margin-top:10px;line-height:1.3'>Forward texts to another phone.<br>Dual-SIM aware. Private &amp; open source.</div>
  </div>
  <img src='{img("home.png")}' style='position:absolute;right:56px;top:46px;width:250px;border-radius:30px;border:8px solid #111;box-shadow:0 20px 50px rgba(0,0,0,.35)'>
</div>"""
render(feature, OUT / "feature-graphic.png", 1024, 500)

# App icon 512x512 (Play applies its own rounding/mask)
render(f"<div style='width:512px;height:512px'>{ICON_SVG.replace('<svg ', '<svg width=512 height=512 ')}</div>", OUT / "icon-512.png", 512, 512)
