# -*- coding: utf-8 -*-
"""分析现有神系列材质：尺寸 + 像素网格打印，用于精确复刻统一风格。"""
from PIL import Image
import os

# 以脚本自身位置定位项目根（tools/ 的上一级），避免项目移动后硬编码路径失效
BASE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "godofthings", "textures")

FILES = [
    "block/god_furnace_side.png",
    "block/god_furnace_top.png",
    "block/god_drop_side.png",
    "block/god_drop_top.png",
    "block/god_enchant_side.png",
    "item/god_craft.png",
    "item/enchant_scroll.png",
    "block/energy_relay_side.png",
    "block/energy_relay_top.png",
    "block/energy_relay_btm.png",
]

# 颜色 -> 字符映射（动态生成）
CHARS = ".:;+*oO#@%&$!?~=^-_,/\\|<>()[]"

for rel in FILES:
    path = os.path.join(BASE, rel.replace("/", os.sep))
    img = Image.open(path).convert("RGBA")
    w, h = img.size
    px = img.load()
    print("=" * 70)
    print(f"{rel}  size={w}x{h}")
    # 统计颜色
    colors = {}
    for y in range(h):
        for x in range(w):
            c = px[x, y]
            colors[c] = colors.get(c, 0) + 1
    ranked = sorted(colors.items(), key=lambda kv: -kv[1])
    cmap = {}
    for i, (c, _) in enumerate(ranked):
        cmap[c] = CHARS[i % len(CHARS)]
    for c, n in ranked[:20]:
        print(f"  {cmap[c]}  rgba{c}  x{n}")
    print("  grid:")
    for y in range(h):
        row = "".join(cmap[px[x, y]] for x in range(w))
        print("  " + row)
