# God of Things —— 贴图重绘提示词包（给生图 AI）

> 用法：先投喂【第 0 节 全局硬规则】建立风格共识，再按【第 2 节】逐族投喂，
> 单个资产用【第 3 节 模板】填空。同一族资产**尽量在同一次会话里连续生成**，
> 这样 AI 能保持批次内配色与光影一致。

---

## 第 0 节 · 全局硬规则（每次必带，先投这段）

```
You are redrawing Minecraft mod textures for "God of Things" (Chinese: 万物之神),
a Minecraft 1.21.1 NeoForge endgame "god-powers" mod.

HARD TECHNICAL RULES — never violate:
1. PIXEL ART ONLY. Hard edges, no anti-aliasing, no blur, no airbrush, no soft gradients
   except where I explicitly allow them. Every color transition is a sharp step.
2. LIMITED PALETTE. 16-24 colors maximum per texture. Flat color blocks with crisp
   highlights and shadows, not photographic rendering.
3. OUTPUT EXACT PIXEL SIZE, no upscaling artifacts, no added borders, no watermarks,
   no text, no drop shadows outside the canvas.
4. TRANSPARENT BACKGROUND for item icons (alpha channel, PNG). Opaque full-coverage
   for block textures (they wrap a cube).
5. NEAREST-NEIGHBOR look. If you work at a larger canvas, downscale with nearest
   neighbor so pixels stay perfectly square and crisp.
6. Minecraft scale is 16x16 texture pixels per block face. A 64x64 texture = 4x supersample.
   So design as if the smallest detail unit is 4x4 pixels in the 64px file.
7. Do NOT add any lettering, numbers, logos or UI text.

ART DIRECTION — the "Divine Machinery" look:
- Theme: divine/celestial technology. Gold sacred mechanisms wrapped in white marble-like
  plating, sitting against deep space blue-black. Think "a cathedral built by a machine god".
- Lighting: top-left key light. Highlights on upper-left edges, shadows on lower-right.
- Materials to suggest: polished gold, white ceramic/marble plating, deep blue-black alloy,
  glowing cyan or amber runes as accent (used sparingly — 5-10% of pixels only).
- Line work: dark navy (NOT pure black) outlines, 1 texture-pixel wide.
- Silhouette rule: every icon must read clearly at 16x16 when downscaled. Big simple
  shape first, detail second. If it turns to mush at 16px, simplify.

CANONICAL PALETTE (sample from the mod's existing textures, match these):
  Gold          #E6AA14   (primary metal, largest metal area)
  Bright gold   #FFDC32   (rim light / rune glow)
  Deep gold     #784B05   (gold shadow)
  Off-white     #EEF2F8   (plating / marble body)
  Pure white    #FAFCFF   (specular highlight)
  Warm cream    #FFFAD2   (soft glow bloom)
  Deep space    #141923   (recesses, panel gaps)
  Space blue    #0F1428   (darkest body, energy cube)
  Navy outline  #1E1E2D   (outlines)
  Ember orange  #FF3C00   (furnace / heat, sparingly)
  Cyan rune     #00E6FF   (energy / AE2 accents, sparingly)
  Arcane purple #160F23   (law blocks, deep shadow)
```

---

## 第 1 节 · 输出规格表（投喂时指明你要哪几项）

| 类别 | 张数 | 尺寸 | 背景 | 模型用法 |
|---|---|---|---|---|
| 机器方块 side | 11 | 64×64 | 不透明 | `cube`，**需可左右无缝平铺** |
| 机器方块 top | 11 | 64×64 | 不透明 | `cube`，顶面独立设计 |
| 法则/功能方块 | 25 | 64×64 | 不透明 | `cube_all`，六面同一张 |
| 物品图标 | 20 | 128×128 | 透明 | 平面图标 |
| 皮带图标 | 4 | 16×16 | 透明 | 平面图标（保持 16×16） |
| 护甲外层 | 2 | 64×32 | 透明 | `layer_1` / `layer_2` |
| GUI 背景 | 17 | 256×256 | 不透明 | **必须 256×256，否则渲染拉伸** |

---

## 第 2 节 · 分族提示词

### 族 A · 神之机器（11 台，最重要）

```
Redraw a Minecraft BLOCK SIDE texture, 64x64, opaque, pixel art.

Machine identity: [见下表]

Design the side face as a "sacred machine panel":
- Outer frame: 4px gold border (#E6AA14) with navy outline, like a shrine's gilded casing.
- Body: off-white plating (#EEF2F8 / #FAFCFF) with subtle panel seams — straight
  horizontal/vertical lines only, no curves.
- A recessed central slot or vent in deep space (#141923) — this is the machine's "mouth".
- Inside or beside the slot, ONE recognizable themed motif rendered as a simple glowing
  rune or relief (see table). Gold or cyan, small, centered-ish, not full-bleed.
- Bottom 8px: a darker plinth so blocks read as sitting on the ground.
- Keep left and right edges TILEABLE: whatever crosses the left edge must continue on the
  right edge at the same y. Top/bottom do not need to tile.
- 16-22 colors. Hard edges. Top-left lighting.
```

| 文件（side） | 主题纹样（放进中央凹槽） |
|---|---|
| `god_furnace_side` | 火焰符文 + 熔炉炉口，ember orange `#FF3C00` 微光 |
| `god_miner_side` | 钻头 / 镐尖浮雕，向下指向 |
| `god_ore_machine_side` | 矿石晶体簇（紫晶 + 金粒） |
| `god_crop_machine_side` | 麦穗 + 叶片纹样 |
| `god_duplicate_machine_side` | 双箭头镜像符（两个相对的三角） |
| `god_peeler_side` | 原木年轮 + 剥离的树皮片 |
| `god_drop_side` | 掉落物漏斗 + 向下箭头 |
| `god_spawn_egg_side` | 蛋形轮廓 + 斑点纹 |
| `god_craft_side` | 合成网格 3×3 点阵 |
| `god_enchant_side` | 附魔符文环 + 星芒 |
| `god_transmitter` | 天线塔 / 信号波纹（**cube_all，六面同一张**） |

顶面配对提示词：

```
Redraw the matching BLOCK TOP texture, 64x64, opaque, pixel art — must look like the
same machine family as the side face I just described.
- Top face reads as a "sealed shrine lid": gold frame, off-white plate, and a CENTERED
  symmetrical emblem (the same motif as the side, but front-facing and mirrored on both
  axes — mandala-like, not directional).
- Add a thin inset ring 6-8px inside the border for a machined look.
- No tileable requirement — this is a standalone centered composition.
- Same palette, same lighting, same 16-22 color budget.
```

---

### 族 B · 法则方块 / 功能方块（cube_all，25 张）

```
Redraw a Minecraft BLOCK texture, 64x64, opaque, pixel art, used on ALL SIX faces
(same image every side), so the composition must be CENTERED and non-directional.

Identity: [见下表]

Style: "law of reality" blocks. Deep arcane purple-black body (#160F23 / #141923)
with gold or cyan sigils floating on the surface. Feel ancient and authoritative —
like a tablet that rewrites the rules of the world.
- Base: dark textured stone/obsidian-like with hard-edged speckle noise (not smooth).
- Center: one large symmetrical sigil, glowing, 40-55% of canvas height.
- Corner accents: small gold ticks or runes in the four corners.
- No ground plinth — this face repeats on the bottom too.
- 16-20 colors.
```

| 文件 | 意象 | 强调色 |
|---|---|---|
| `space_time_eternity` | 沙漏 + 环绕星轨 | 金 `#E6AA14` |
| `space_time_eternity_on` | 同上，符文**点亮发光** | 亮金 `#FFDC32` |
| `space_time_eternity_off` | 同上，符文**熄灭变灰** | 灰蓝 |
| `creature_annihilation` | 压制领域：破碎的生物剪影 + 禁行圈 | 暗红 |
| `creature_annihilation_on` | 同上点亮 | 红亮 |
| `creature_annihilation_off` | 同上熄灭 | 灰 |
| `teleport_block` | 传送门漩涡 1 | 青 `#00E6FF` |
| `teleport_block_2` | 传送门漩涡 2（不同色相） | 紫 |
| `teleport_block_3` | 传送门漩涡 3 | 琥珀 |
| `void_teleporter` | 虚空裂隙，纯黑核心 | 紫黑 + 青边 |
| `creative_energy_cube` | 无限能源：发光能量核心 `#0F1428` 底 | 青 `#00E6FF` |
| `god_absorber` | 引力场：向内汇聚的同心箭头 | 金 |
| `god_slaughter` | 斩杀阵：交叉刃 + 血色环 | 暗红 |
| `god_devourer` | 虚空吞噬：黑洞 / 深渊开口 | 纯黑 + 金边 |
| `hoyoog_fumo` | 玩偶方块（唯一可爱向）：Q 版角色 | 柔彩色 |

---

### 族 C · 物品图标（20 张，128×128，透明背景）

```
Redraw a Minecraft ITEM ICON, 128x128, TRANSPARENT background, pixel art.

Item: [见下表]

Rules for all icons:
- Centered composition, occupying ~85% of the canvas, with a slender transparent margin.
- Flat vector-like pixel shapes with 1-2px navy outline (#1E1E2D).
- Palette budget: 4-8 flat colors for the body + 1 accent glow color. Do NOT use
  gradients or soft shading except on the few items I mark "detailed".
- Add a subtle 2px darker bottom edge so the item reads as a solid object, not a sticker.
- Must remain legible when displayed at 16x16 in the inventory — test mentally by
  squinting: one dominant silhouette, one distinguishing detail.
```

| 文件 | 造型 | 备注 |
|---|---|---|
| `god_recipe` | 神之配方：9 种矿物锭/宝石环绕排布的圆形徽章 | **detailed**，允许渐变，现 894 色 |
| `god_config` | 机器配置模板：齿轮 + 六面方块示意 | **detailed**，允许渐变，现 228 色 |
| `god_construction` | 建筑棒：法杖 + 方块投影线框 | 中等细节，现 86 色 |
| `god_backpack` | 神之背包：120 格背包，金扣 + 白帆布 | 剪影 |
| `god_belt` / `god_belt_buckle` / `god_belt_dyed` | 皮带与带扣 | **保持 16×16，勿改尺寸** |
| `god_black_box` | 神之黑盒：纯黑立方 + 金边 + 吸取口 | 剪影 |
| `god_note` | 神之便签：卷起的羊皮纸 + 金羽毛笔 | 剪影 |
| `god_manual` | 神之手册：精装书 + 金色书签带 | 剪影 |
| `god_accelerator` | 神之加速：闪电 + 齿轮叠加 | 剪影 |
| `god_measure` | 神之测量：激光卷尺 + 红色测量线 | 剪影 |
| `god_invite` | 神之请神：法杖 + 生命光环 | 剪影 |
| `god_change` | 神之改造：天象仪 / 天气拨盘 | 剪影 |
| `god_binder` | 神之绑定：锁链环 + 链接箭头 | 剪影 |
| `god_unbreakable` | 不毁：钻石被光环包裹 | 剪影 |
| `god_helmet` / `god_chestplate` / `god_leggings` / `god_boots` | 神之护甲四件 | 金甲 + 青色能量点缀，四件风格必须成套 |

---

### 族 D · 无限存储元件（4 张，128×128，透明）

```
Redraw an AE2-style storage CELL item icon, 128x128, transparent, pixel art.
These must still visibly belong to Applied Energistics 2's visual family:
- Housing: angled rectangular ME drive casing, dark navy body, with the characteristic
  colored status bar / indicator strip along one edge.
- Center: a clear circular or diamond "core lens" showing the stored substance.
- Keep the AE2 silhouette (people must recognize it as a storage cell) but upgrade the
  surface finish to polished gold trim + white plating to match this mod's palette.
- 8-14 colors. Flat shading with one specular highlight on the lens.
```

| 文件 | 核心内容物 | 强调色 |
|---|---|---|
| `infinity_cell_water` | 水，带流动感 | 蓝 |
| `infinity_cell_lava` | 熔岩，带气泡 | 橙红 |
| `infinity_cell_cobblestone` | 圆石纹理 | 灰 |
| `infinity_cell_blank_pattern` | 空白样板，空镜面 | 浅青 |

---

### 族 E · 护甲层（models/armor，2 张 64×32）

```
Redraw a Minecraft armor layer texture, 64x32, transparent, pixel art.
This is the classic Minecraft armor UV layout (helmet/chest/legs/boots mapped across
the 64x32 sheet) — keep the standard part placement, only restyle the material.

Material: divine gold armor. Polished gold plate (#E6AA14 / #784B05) with off-white
under-layer (#EEF2F8), navy outlines, and thin cyan (#00E6FF) energy lines tracing the
plate seams. Ornate but readable — add small shoulder pauldrons and a chest emblem.
Do NOT move or resize body parts; the layout is fixed by Minecraft.
```

---

### 族 F · GUI 背景（17 张，必须 256×256）

```
Redraw a Minecraft GUI background, EXACTLY 256x256 pixels, opaque, pixel art.
CRITICAL: the output must be precisely 256x256 — the renderer samples this on a
256x256 UV space and any other size gets silently stretched, breaking slot alignment.

Style: divine machine interface panel.
- Base: smooth dark panel — deep space navy (#141923) with a 2px gold (#E6AA14) inner
  bevel border and a subtle vertical panel gradient (allowed here: 3-4 discrete steps,
  still hard-edged, no smooth blur).
- Corners: small gold bracket ornaments.
- Keep the CENTER AREA relatively plain and dark — that's where inventory slots and
  progress arrows are drawn by code on top. Do not paint busy patterns in the middle.
- The functional furniture (slot frames, arrows, buttons) is drawn by the mod's code;
  your job is the BACKGROUND PLATE only. Leave it clean.
- 12-20 colors.
```

---

## 第 3 节 · 单资产填空模板

```
Redraw ONE texture for the Minecraft mod "God of Things".

FILE:        [例如 god_furnace_side.png]
TYPE:        [block side / block top / cube_all block / item icon / armor layer / gui]
SIZE:        [64x64 / 128x128 / 256x256 / 64x32]
BACKGROUND:  [opaque / transparent]
TILEABLE:    [yes — left-right seamless / no]
SUBJECT:     [一句话描述物体]
MOTIF:       [中心纹样]
PALETTE:     gold #E6AA14, bright gold #FFDC32, deep gold #784B05, off-white #EEF2F8,
             white #FAFCFF, deep space #141923, navy outline #1E1E2D
             + accent: [cyan #00E6FF / ember #FF3C00 / purple #160F23]
COLOR BUDGET: [16-22] colors

Style: Minecraft pixel art, hard edges, no anti-aliasing, no blur, top-left key light,
navy outlines, 16-24 colors, legible at 16x16, no text or logos.
Output exactly [SIZE] pixels, PNG.
```

---

## 第 4 节 · 交付后自检（每张图都要过）

- [ ] 尺寸与规格表一致（GUI 尤其必须是 256×256）
- [ ] 放大看边缘是硬台阶，没有羽化/模糊
- [ ] 缩到 16×16 还能认出是什么
- [ ] 调色板数量在预算内（不是照片级渐变）
- [ ] 方块 side 图左右边缘接得上（无缝）
- [ ] 物品图标背景是真透明（不是白底）
- [ ] 主色命中 `#E6AA14 / #EEF2F8 / #FAFCFF / #141923`
- [ ] 同一族的多张图摆在一起看，风格一致

> ⚠️ 替换进工程后：方块贴图直接覆盖 `src/main/resources/assets/godofthings/textures/block/`；
> 若改了文件名或新增贴图，需要同步 `models/block/*.json` 与 `blockstates`，
> 并跑 `.\check-lang.ps1` 做资源一致性校验（它会查模型与语言键覆盖）。
