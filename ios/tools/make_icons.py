"""Create iOS PBS Balap icons from the official PBS logo rendition saved by the Android build."""
from pathlib import Path
import base64, io
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
asset = ROOT / "pbs-mobile/assets/pbs-official-logo.webp.b64"
if not asset.exists():
    raise SystemExit("Missing official PBS artwork: " + str(asset))
logo = Image.open(io.BytesIO(base64.b64decode(asset.read_text().strip()))).convert("RGB")
if logo.width < 128:
    raise SystemExit("Logo source too small")
out = ROOT / "ios/PBSBalap/Assets.xcassets"
for folder, name, size in [
    ("AppIcon.appiconset", "AppIcon-1024.png", 1024),
    ("BrandLogo.imageset", "brand.png", 512),
]:
    file = out / folder / name
    file.parent.mkdir(parents=True, exist_ok=True)
    logo.resize((size, size), Image.Resampling.LANCZOS).save(file, optimize=True)
    print(file)
