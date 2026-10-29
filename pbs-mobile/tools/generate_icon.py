"""Generate stylized PBS icon using Pillow. Reference artwork is a reconstruction, not an official brand master."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
S = 512
im = Image.new("RGB", (S, S), "#0c1e38")
d = ImageDraw.Draw(im)
d.arc((66,115,449,386), 202, 338, fill="#ff9019", width=8)
d.arc((75,111,436,398), 23, 157, fill="#16b9d9", width=10)
d.arc((60,124,453,443), 15, 164, fill="#17b7da", width=7)
for a,b in [((213,135),(183,82)),((240,123),(223,61)),((278,120),(282,61)),((307,122),(335,76))]:
    d.line((a,b), fill="#19b9d7" if a[0]<270 else "#ff8819",width=8)
fontpaths=["/usr/share/fonts/truetype/dejavu/DejaVuSans-BoldOblique.ttf",
"/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"]
font=ImageFont.truetype(next(p for p in fontpaths if Path(p).exists()),136)
for letter,x,color in [("P",107,"#198ed6"),("B",205,"#14c7ce"),("S",316,"#ff931b")]:
    d.text((x,186),letter,font=font,fill=color,stroke_width=0)
out=Path(__file__).resolve().parents[1]/"app/src/main/res/drawable-nodpi/ic_pbs.png"
out.parent.mkdir(parents=True,exist_ok=True)
im.save(out,optimize=True)
print(f"Icon generated: {out}")
