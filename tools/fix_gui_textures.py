# -*- coding: utf-8 -*-
"""一次性修补 GUI 贴图（2026-08）：
1. god_resource/god_drop/god_enchant：输入槽框从 x=56 移到 x=79（槽位居中）。
2. god_enchant：物品栏槽框区（y=162..233）整体下移 16px 到 y=178..249，
   与 GodEnchantMenu 中玩家物品栏 y=178/快捷栏 y=232 对齐，消除按钮与物品栏视觉重叠。
"""
from PIL import Image

BASE = 'src/main/resources/assets/godofthings/textures/gui/'
BG = (198, 198, 198, 255)
DARK = (55, 55, 55, 255)

def move_input_frame(im):
    """把输入槽 18x18 框块从 (56,35) 搬到 (79,35)，原处填面板底色。"""
    block = im.crop((56, 35, 74, 53))
    px = im.load()
    for y in range(35, 53):
        for x in range(56, 74):
            px[x, y] = BG
    im.paste(block, (79, 35))

def shift_enchant_inventory(im):
    """物品栏 4 行槽框块 y=162..233 下移到 y=178..249；y=162..177 填面板底色。"""
    block = im.crop((0, 162, 176, 234))
    px = im.load()
    for y in range(162, 178):
        for x in range(0, 174):
            px[x, y] = BG
        px[174, y] = DARK
        px[175, y] = BG
    im.paste(block, (0, 178))

for name in ['god_resource.png', 'god_drop.png', 'god_enchant.png']:
    path = BASE + name
    im = Image.open(path).convert('RGBA')
    move_input_frame(im)
    if name == 'god_enchant.png':
        shift_enchant_inventory(im)
    im.save(path)
    print('patched', name)
