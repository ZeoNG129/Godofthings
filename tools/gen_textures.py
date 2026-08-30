# -*- coding: utf-8 -*-
"""生成统一风格的材质 PNG（16x16）。

1. god_craft 方块侧面/顶面 + 物品图标 —— 神系列面板风（琥珀色调 + 3x3 合成网格图标）
2. enchant_scroll 物品 —— 暗紫魔法卷轴（金色绑带 + 封印）
3. energy_relay 侧面/顶面/底面 —— 科技感线圈风（深色金属 + 青色发光线圈）
"""
from PIL import Image
import os

# 以脚本自身位置定位项目根（tools/ 的上一级），避免项目移动后硬编码路径失效
BASE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    "src", "main", "resources", "assets", "godofthings", "textures")

TRANSPARENT = (0, 0, 0, 0)


def save(name, rows, cmap):
    img = Image.new("RGBA", (16, 16), TRANSPARENT)
    px = img.load()
    for y, row in enumerate(rows):
        assert len(row) == 16, f"{name} row{y} width={len(row)}: {row}"
        for x, ch in enumerate(row):
            if ch != '.':
                px[x, y] = cmap[ch]
    path = os.path.join(BASE, name.replace("/", os.sep))
    img.save(path)
    print(f"saved {path}")


# ============================================================
# 1. 神之合成 —— 神系列面板风（复刻熔炉/掉落机的面板布局，琥珀色调）
# ============================================================
# 神系列统一布局（与 god_furnace / god_drop 完全一致）
CRAFT_BASE = [
    "....::::::::....",
    "....::::::::....",
    "..;;;;;;;;;;;;..",
    "..;..........;..",
    "::;..........;::",
    "::;..........;::",
    "::;..........;::",
    "::;..........;::",
    "::;..........;::",
    "::;..........;::",
    "::;..........;::",
    "::;..........;::",
    "..;..........;..",
    "..;;;;;;;;;;;;..",
    "....::::::::....",
    "....::::::::....",
]
# 3x3 合成网格图标（10x10，覆盖 rows3-12 / cols3-12）
CRAFT_ICON = [
    "++++++++++",
    "+**+**+**+",
    "+**+**+**+",
    "++++++++++",
    "+**+**+**+",
    "+**+**+**+",
    "++++++++++",
    "+**+**+**+",
    "+**+**+**+",
    "++++++++++",
]
craft_map = {
    '.': (238, 230, 214, 255),  # 暖白面板
    ':': (82, 60, 38, 255),     # 深棕描边
    ';': (170, 144, 100, 255),  # 中间调边框
    '+': (122, 82, 46, 255),    # 网格线（深棕）
    '*': (216, 174, 104, 255),  # 网格格（暖金）
}
craft_side = [list(r) for r in CRAFT_BASE]
for dy in range(10):
    for dx in range(10):
        craft_side[3 + dy][3 + dx] = CRAFT_ICON[dy][dx]
craft_side = ["".join(r) for r in craft_side]
save("block/god_craft_side.png", craft_side, craft_map)
save("block/god_craft_top.png", craft_side, craft_map)  # 顶面与侧面一致（与熔炉/掉落机惯例相同）

# 物品图标：取方块面 rows2-13 / cols2-13 的 12x12 区域，居中放置
item_rows = ["." * 16 for _ in range(16)]
for dy in range(12):
    for dx in range(12):
        item_rows[2 + dy] = (item_rows[2 + dy][:2 + dx]
                             + craft_side[2 + dy][2 + dx]
                             + item_rows[2 + dy][3 + dx:])
save("item/god_craft.png", item_rows, craft_map)

# ============================================================
# 2. 附魔卷轴 —— 暗紫魔法卷轴（卷起的卷轴 + 金色绑带 + 封印）
# ============================================================
scroll_map = {
    'D': (48, 26, 78, 255),    # 深紫描边
    'H': (160, 118, 205, 255), # 亮紫高光
    'L': (122, 82, 168, 255),  # 浅紫
    'P': (82, 48, 124, 255),   # 主体紫
    'Q': (58, 32, 92, 255),    # 右侧阴影
    'K': (30, 16, 50, 255),    # 卷轴内芯暗部
    'Y': (255, 232, 130, 255), # 金高光
    'G': (240, 200, 70, 255),  # 金
    'g': (196, 148, 40, 255),  # 金暗部
}
scroll = [
    "................",
    ".....DDDDDD.....",
    "....DHLKKLHD....",
    "....DLKHKLKD....",
    "....DHPPPPQD....",
    "....DHPPPPQD....",
    "....DHPPPPQD....",
    "....DYGGGGYD....",
    "....DgGKKGGD....",
    "....DgGKKGGD....",
    "....DHPPPPQD....",
    "....DHPPPPQD....",
    "....DLKHKLKD....",
    "....DHLKKLHD....",
    ".....DDDDDD.....",
    "................",
]
save("item/enchant_scroll.png", scroll, scroll_map)

# ============================================================
# 3. 能量传输器 —— 科技感线圈风（深色金属 + 青色发光线圈）
# ============================================================
relay_map = {
    'A': (24, 26, 32, 255),      # 近黑描边
    'B': (44, 48, 58, 255),      # 暗金属
    'C': (70, 76, 88, 255),      # 中间调金属
    'E': (104, 110, 124, 255),   # 亮金属高光
    'c': (16, 96, 108, 255),     # 暗青
    'T': (48, 208, 220, 255),    # 青色发光
    'W': (190, 250, 255, 255),   # 亮青/白核心
}
relay_side = [
    "....AAAAAAAA....",
    "..AABBBBBBBBAA..",
    ".ABBBBBBBBBBBBA.",
    ".ABCEEEEEEEECBA.",
    ".ABcTTTTTTTTcBA.",
    ".ABcTWWWWWWTcBA.",
    ".ABCCBCCCCBCCBA.",
    ".ABBEBBBBBBEBBA.",
    ".ABCCBCCCCBCCBA.",
    ".ABcTTTTTTTTcBA.",
    ".ABcTWWWWWWTcBA.",
    ".ABBBBBBBBBBBBA.",
    ".ABBCBCCCCBCBBA.",
    ".ABBBBBBBBBBBBA.",
    "..AABBBBBBBBAA..",
    "....AAAAAAAA....",
]
save("block/energy_relay_side.png", relay_side, relay_map)

relay_top = [
    "....AAAAAAAA....",
    "..AABBBBBBBBAA..",
    ".ABBBBBBBBBBBBA.",
    ".ABCEEEEEEEECBA.",
    ".ABCCCccccCCCBA.",
    ".ABCcTWWWWTcCBA.",
    ".ABCTWWWWWWTCA.",  # 占位，下面覆盖为对称版
    ".ABCTWWWWWWTCA.",
    ".ABCcTWWWWTcCBA.",
    ".ABCCCccccCCCBA.",
    ".ABCCCCCCCCCCBA.",
    ".ABBCCCCCCCCBBA.",
    ".ABBBBBBBBBBBBA.",
    ".ABBBBBBBBBBBBA.",
    "..AABBBBBBBBAA..",
    "....AAAAAAAA....",
]
# 修正 r6/r7 为对称 16 字符行
relay_top[6] = ".ABCTWWWWWWTCBA."
relay_top[7] = ".ABCTWWWWWWTCBA."
save("block/energy_relay_top.png", relay_top, relay_map)

relay_btm = [
    "....AAAAAAAA....",
    "..AABBBBBBBBAA..",
    ".ABBBBBBBBBBBBA.",
    ".ABBBBBBBBBBBBA.",
    ".ABBBCCCCCCBBBA.",
    ".ABBCBCCCCBCBBA.",
    ".ABBCBAAAABCBBA.",
    ".ABBCBAAAABCBBA.",
    ".ABBCBAAAABCBBA.",
    ".ABBCBAAAABCBBA.",
    ".ABBCBCCCCBCBBA.",
    ".ABBBCCCCCCBBBA.",
    ".ABBBBBBBBBBBBA.",
    ".ABBBBBBBBBBBBA.",
    "..AABBBBBBBBAA..",
    "....AAAAAAAA....",
]
save("block/energy_relay_btm.png", relay_btm, relay_map)

print("done")
