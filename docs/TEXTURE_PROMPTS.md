# 物品材质重生成提示词包（喂给图像模型用）

> 目标：重绘本 mod 全部 11 个物品图标 + 机器方块贴图，风格统一、贴合名称、好看大气。
> 用法：每条英文提示词直接复制给图像模型 → 生成 512×512（或更大）透明底 PNG →
> 用 `tools/tex/downscale16.ps1` 最近邻降采样到 16×16 → 同名替换
> `assets/godofthings/textures/item/`（或 `textures/block/`）→ 游戏内 F3+T 验证。

---

## 一、统一风格系统「神金 · 虚空奥术」(Divine Gold & Void Arcana)

风格三要素（所有提示词共享，保证成套）：

1. **固定调色板**（写死色号，模型不跑色）：

| 角色 | 色号 |
|---|---|
| 神圣金（主） | 底 `#E8B23A` / 高光 `#FFD966` / 暗部 `#8A5A18` |
| 虚空紫（衬底/描边） | 中 `#4A2E73` / 深 `#2B1B45` / 外描边 `#1A0F2E` |
| 奥术青（能量/发光） | `#4DE8E8` / 高光 `#9FF8F8` |
| 能量品红（点缀，克制使用） | `#E34FD0` |
| 合金灰（盔甲/机体） | 中 `#46424A` / 高光 `#6A6570` / 暗 `#312E36` |
| 羊皮纸（卷轴） | `#E8D9A8` / 暗 `#B09A62` |
| 白高光 | `#FFFFFF` |

2. **固定轮廓语言**：1px 深紫黑外描边 `#1A0F2E`，剪影干净完整，任何色块 ≥2px。
3. **固定能量语言**：全 mod 的"神性能量"统一为 **白心 → 青 → 细品红边** 的径向发光
   （与创造能量立方核心同款）。凡涉及"能量/神焰/核心"的图案都用它。

### 通用正向后缀（拼在每条提示词末尾）

```
Minecraft item icon, 16x16 pixel art, single item centered on transparent background,
clean readable silhouette with a thin dark purple outline (#1A0F2E), flat cel shading
2-3 tones per color, limited palette: holy gold (#E8B23A #FFD966 #8A5A18), void purple
(#4A2E73 #2B1B45), arcane cyan glow (#4DE8E8), white highlights, vanilla Minecraft
texture style, crisp pixels, no anti-aliasing
```

### 通用负向提示词（每张都带）

```
text, letters, numbers, watermark, signature, background, environment, ground,
drop shadow, reflection, photo, photorealistic, 3D render, smooth gradient, blur,
motion blur, multiple items, cropped, frame, border, vignette, oversaturated
```

### 系列一致性技巧

- 先生成一张定调（推荐先做 **神之甲** 或 **造化垂青之杖**），选定后把它作为**垫图/参考图**
  喂给后续生成（支持垫图的模型：MJ `--sref`/垫图、SD img2img 低重绘、即梦/可灵参考图），并固定 seed；
- 盔甲四件必须明说 "part of the same divine armor set"；
- 传送门两枚是孪生设计，一条里写 "same ring shape as its twin"。

---

## 二、Part A —— 物品图标（textures/item/，11 枚）

### A1 `god_favor_wand` 造化垂青之杖（手持贴图，左下→右上斜置）

```
A diagonal divine scepter from bottom-left to top-right, dark amethyst wooden shaft
wrapped in golden holy rune rings, golden pommel and collar fittings, the head cradles
a floating orb glowing white at the core fading to arcane cyan with a thin magenta rim
(radial energy core), three golden prongs holding the orb,
```

### A2 `god_helmet` 神之头

```
Front-view Minecraft helmet icon, dark netherite-gray dome with layered plates,
ornate golden trim along the brow and cheek guards, a horizontal glowing arcane-cyan
eye slit, small golden crest fin on top, golden rivets, part of a divine armor set,
```

### A3 `god_chestplate` 神之甲（先做这张定调整套）

```
Front-view Minecraft chestplate icon, dark netherite-gray cuirass with layered
golden-edged plates, a round arcane-cyan gem set at the center chest with a white
glowing core, void-purple cloth under-layer at the shoulders and waist, golden rivets,
part of a divine armor set,
```

### A4 `god_leggings` 神之腿

```
Front-view Minecraft leggings icon, dark netherite-gray waist skirt and thigh plates
with golden edges, void-purple cloth layer, small cyan rune stitch marks, golden knee
caps, part of a divine armor set,
```

### A5 `god_boots` 神之鞋

```
Front-view pair of Minecraft boots icon, dark netherite-gray boots with golden trim
and toe caps, a glowing arcane-cyan sole line, void-purple cuffs,
part of a divine armor set,
```

### A6 `god_unbreakable` 神之不毁

```
A divine eight-pointed golden star emblem, layered gold rays with #FFD966 highlights,
round core with a white center glowing to arcane cyan and a thin magenta ring,
void-purple halo outline, symbol of indestructibility,
```

### A7 `enchant_scroll` 附魔卷轴

```
A half-opened parchment scroll with rolled ends tied by a golden band, warm cream
parchment (#E8D9A8), a purple wax seal, faint arcane-cyan glowing runes written across
the surface, one edge slightly curled,
```

### A8 `god_change` 神之更改（时间与天气图腾）

```
A round golden amulet badge, split design: the top half shows a small golden sun and
crescent moon, the bottom half shows a rain cloud with arcane-cyan drops, a tiny white
hourglass at the center, void-purple inner disc background, symbol of time and weather
control,
```

### A9 `superflat_teleporter` 神之平坦（传送门·草原）

```
An oval golden portal ring seen slightly from above, inside it a miniature flat
grassland: bright green grass plane with a straight horizon line and pale blue sky,
a soft cyan portal sparkle at the center,
```

### A10 `void_teleporter` 神之虚空（传送门·虚空，与 A9 孪生）

```
An oval golden portal ring seen slightly from above, same ring shape as its twin,
inside it deep void space: dark purple nebula swirl with tiny white stars and
arcane-cyan sparks, near-black center,
```

### A11 `god_craft` 神之合成（物品图标）

```
A majestic golden crafting altar icon, dark stone body with an ornate golden frame,
the front face shows a 3x3 grid of glowing arcane-cyan squares, a white-gold star
emblem at the center, void-purple base trim,
```

---

## 三、Part B —— 机器方块贴图（textures/block/，背包内图标=方块贴图）

**家族模板**（统一机器观感，像原版熔炉/附魔台家族）——每条 = 模板 + 各自图案：

```
Minecraft block texture, 16x16 pixel art, obsidian-dark machine body (#2B2B31 with
subtle darker speckles), ornate golden trim border with golden corner rivets,
<MOTIF>, one small arcane-cyan indicator light at a corner, flat shading, straight edges
```

| 贴图 | `<MOTIF>` 填入（英文） |
|---|---|
| `god_furnace_side` 神之熔炉 | `a central furnace mouth with divine fire: white core fading to arcane cyan then a thin magenta flame rim` |
| `god_furnace_top` | `a golden gear emblem centered on the dark top face` |
| `god_miner_side` 神之矿机 | `a golden drill bit pointing downward in the center, two small cyan progress bars on both sides` |
| `god_miner_top` | `a golden mining derrick emblem centered on the dark top face` |
| `god_resource_side` 神之资源 | `a golden seedling with two leaves growing from a small soil strip, centered` |
| `god_resource_top` | `a golden leaf emblem centered on the dark top face` |
| `god_drop_side` 神之掉落 | `a golden mob spawn egg silhouette resting on a small pedestal, centered` |
| `god_drop_top` | `a golden egg emblem centered on the dark top face` |
| `god_enchant_side` 神之附魔 | `a floating open book above a small pedestal with purple-cyan rune sparkles around it` |
| `god_enchant_top` | `a golden five-pointed star emblem centered on the dark top face` |
| `god_heaven_enchant_side` 天神附魔 | `the whole body is brighter gold-plated, a floating open book with a white halo ring above it, purple-cyan rune sparkles` |
| `god_heaven_enchant_top` | `a golden star emblem with a thin white halo ring, centered` |
| `god_craft` / `god_craft_side` 神之合成 | `a golden 3x3 crafting grid inlay with a white-gold star at the center` |
| `god_craft_top` | `a golden 3x3 grid surface on the dark top face` |
| `superflat_teleporter` | `obsidian-dark portal frame with golden corner ornaments, the inner surface a flat green grass field with a pale horizon` |
| `void_teleporter` | `obsidian-dark portal frame with golden corner ornaments, the inner surface a dark purple starry void swirl` |
| `creative_energy_cube` | （风格基准，**建议保留现有**；若重生成：`dark amethyst cube face with a large radial core: white center to arcane cyan to magenta rim, four cyan energy ports at the edge midpoints`） |

---

## 四、Part C —— 缺失项修复：`god_favor_wand_fortune` 造化垂青之杖·时运

> 现状：`models/item/god_favor_wand.json` 的 override 指向 `godofthings:item/god_favor_wand_fortune`，
> 且代码在时运模式会设置 `CustomModelData=1`（`WandItemUtils.java:132`），但该模型+贴图已被清理 →
> 时运模式显示紫黑棋盘格。生成贴图后需补一个 model json（交给 agent 一句话即可）。

```
Same divine scepter as its twin (dark amethyst shaft, golden rune rings), but the orb
glows emerald-green at the core with golden sparks and a tiny four-leaf clover glint,
fortune theme,
```

---

## 五、技术要点

1. **尺寸**：让模型出 512×512 或 1024×1024、透明背景 PNG；务必用最近邻（Nearest Neighbor）
   降到 16×16 —— `powershell -File tools/tex/downscale16.ps1 -Path <生成的图或文件夹> -OutDir build\textures16`
2. **16px 可读性自检**：外轮廓 1px 连续闭合；每个色块 ≥2px；主色 ≤5 种；发光部分只占图案 20% 左右。
   不达标就改提示词重生成（优先简化图案，而不是加细节）。
3. **光影**：光源统一左上；禁止柔和渐变（16px 下会糊），要"色块阶"。
4. **不要动**：GUI 背景板贴图（已程序化生成、风格统一）；创造能量立方（基准）。
5. **不适合图像模型**：盔甲穿戴层 `textures/models/armor/god_armor_layer_1/2.png`（64×32 UV 展开，
   目前缺失——穿戴在身上时不可见），需要手绘或后续用工具处理，别让图像模型碰。
