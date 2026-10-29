"""Create PBS Balap launcher/header icon using the PBS logo supplied by the user.

The repository keeps a compact WEBP-encoded rendition of the actual supplied logo;
no generated/reconstructed PBS artwork is used.
"""
from pathlib import Path
import base64, io
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
source = ROOT / "assets" / "pbs-official-logo.webp.b64"
raw = base64.b64decode(source.read_text().strip(), validate=True)
logo = Image.open(io.BytesIO(raw)).convert("RGBA")
if logo.width < 128 or logo.height < 128:
    raise RuntimeError("Official icon source is too small")

out = ROOT / "app/src/main/res/drawable-nodpi/ic_pbs.png"
out.parent.mkdir(parents=True, exist_ok=True)
logo.resize((512,512), Image.Resampling.LANCZOS).save(out, format="PNG", optimize=True)

for dpi, size in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)]:
    dest = ROOT / f"app/src/main/res/mipmap-{dpi}/ic_launcher.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    logo.resize((size,size), Image.Resampling.LANCZOS).save(dest,format="PNG",optimize=True)
    (dest.parent/"ic_launcher_round.png").write_bytes(dest.read_bytes())

print(f"PBS official icon generated from {source}, main={out}, density variants=5")
