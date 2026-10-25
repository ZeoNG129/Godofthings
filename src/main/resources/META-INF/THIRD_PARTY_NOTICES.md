# Third-Party Notices / 第三方代码与素材声明

本模组（God of Things）移植/照抄了下列开源项目的代码与素材（除注明外只改包名与命名空间）。
**这些部分的许可独立于本模组自身的 `mod_license`（MIT，见仓库根 LICENSE.txt）** ——
取用、再分发本模组时，请一并遵守下列条款与署名要求。各许可全文见仓库 `LICENSES/`，
并随 jar 一起打包在 `META-INF/LICENSE-*`。本文件为唯一权威清单；代码侧各 vendored 包的
`package-info.java` 注有对应摘要（2026-10 仓库整理时建立，二者以本文件为准）。

> ⚠️ **再分发前必读的三处硬约束**
> 1. **GPL-3.0 传染**：Advanced Memory Card 段（第 9 节）的代码为 GPL-3.0，与其同库分发时，
>    整个作品事实上需满足 GPL-3.0 的开源义务，与 MIT 再授权不兼容，需作者决策（见该节）。
> 2. **All Rights Reserved**：Useless Stretcher 段（第 11 节）的上游未开源、保留全部权利，
>    在取得作者书面授权前，其衍生代码（`doll/`、`wondrous/`）不满足公开再分发条件。
> 3. **禁商用素材**：AE2 Lightning Tech Reborn 的素材为 CC BY-NC-SA 3.0（第 3 节），严禁商用。

---

## 1. Useless Mod（无用之物）—— MIT License

- **照抄内容**：`beef` 包整体（无用维度：奇数 / 偶数 / 三维度，传送方块、合金炉、AE 链接辅助等；
  其原「造化杖」部分已在 v5.8.0 删除、后以「太初洞见之杖」形式回归）。
- **署名**：作者 `C-H716`、`水幕忘忧`；美术 `虾比`、`麦淇淋`。
- **许可**：MIT（见 `LICENSES/useless-mod-MIT.txt`）。
- 说明：上游仓库未随源码附独立 `LICENSE` 文件，其 MIT 许可在 `README.md` 的 License 一节中声明。

## 2. GT New Horizons / PersonalSpace —— GNU LGPL v3.0

- **关联内容**：Useless Mod 的「可配置边界 / 道路 / 中心标记生成模型」适配自该项目
  （<https://github.com/GTNewHorizons/PersonalSpace>）。Useless Mod 的接口、持久化、网络与平台适配为其原创。
- **对本模组的影响**：本模组无用维度的地形生成部分
  （`beef/world/dimension/DimensionGenerationConfig`、`PlatformLayout`、`AbstractPlasticPlatformGenerator`）
  沿用了这套被适配过的生成模型，因此**该部分同样适用 LGPL-3.0**。
- **许可**：见 `LICENSES/PersonalSpace-LGPL-3.0.txt`。

## 3. AE2 Lightning Tech Reborn —— 源码 LGPL-3.0 / 素材 CC BY-NC-SA 3.0

- **照抄内容**：
  - `building` 包的 CTM 连接材质几何（`connected_texture` 几何加载器及配套 baked model，
    注释自述 ported from AE2 Lightning Tech / AE2 RenderHelper / makeBitmask）；
  - `building` 包的猪咪建材方块（素板 / 框板 / 台阶，原样移植自 AE2LT，贴图取自猪咪 fumo
    的素色装饰方块，含 `textures/block/pigmee_fumo.png`）；
  - `overloadedinterface` 包的过载接口整套（方块 / 方块实体 / 升级 / 槽位语义，
    移植自 AE2LT 的 OverloadedInterfaceBlockEntity / OverloadedSlotSemantics 等）。
  - 历史：早期照抄的 fumo 玩偶五个类（FumoBlock 等）已随版本更迭删除，仅存上列素材与代码。
- **署名**：作者 `MOAKIEE`、`CystrySU`、`gjmhmm8`、`_leng`、`TedXenon`、`MHanHanBing`。
- **源码许可**：GNU LGPL 3.0 —— 见 `LICENSES/AE2LT-LGPL-3.0.txt`。
- **素材许可**：**CC BY-NC-SA 3.0** —— 见 `LICENSES/AE2LT-ASSETS-CC-BY-NC-SA-3.0.md`。
  即上列猪咪 fumo 相关贴图要求：**署名、禁止商用、相同方式共享**；
  任何再分发必须保留同样许可，且**不得用于商业用途**。

## 4. ExtendedAE / ExtendedAE+ —— GNU LGPL v3.0

- **照抄内容**：`infinitecell/big` 包的 ME 无限存储元件（吞噬万籁的永寂）——
  `InfinityCellItem` / `InfinityCellInventory` / `InfinityConstants` / `InfinityDataStorage` /
  `InfinityStorageManager` / `InfinityCellHandler` 等，类注释自述移植自
  ExtendedAE+ 1.6.3（<https://github.com/GaLicn/ExtendedAE_Plus>，上游类
  `com.extendedae_plus.items.InfinityBigIntegerCellItem` 等）。
  其中 `infinitecell` 包根的「原初万象圣晶」Universal Cell 为本模组在同期技术上的原创重写，
  不适用本节；早期照抄自 ExtendedAE（glodblock）的四元件类已并入上述 ExtendedAE+ 移植。
- **署名**：ExtendedAE+ 作者 `GaLicn` 与 contributors；ExtendedAE 作者 `glodblock` 与 contributors。
- **许可**：GNU LGPL 3.0 —— 见 `LICENSES/ExtendedAE-LGPL-3.0.txt` 与
  `LICENSES/ExtendedAEPlus-LGPL-3.0.txt`（相关类保持 LGPL-3.0，不得按本模组的 MIT 条款再许可）。

## 5. ToolBelt —— BSD-3-Clause

- **移植内容**：神之皮带整套（`com.godofthings.belt` 包）—— 皮带物品 / 皮带槽附件 / 皮带界面 /
  工具轮盘 / 网络包 / 配置，移植自 [ToolBelt](https://github.com/gigaherz/ToolBelt) 2.2.10
  （上游包名 `dev.gigaherz.toolbelt.*`，BSD-3-Clause）。
  本地改动：包名与注册名统一改为 `godofthings:god_belt*`、**默认满级（9 格）**、
  删除皮带包（pouch）与缝纫升级整套、Curios 饰品栏查找器改用物品栏保底、
  自定义 `ItemDisplayContext` 枚举扩展改用原版展示方式、槽位空图标不再写 `minecraft` 命名空间资源。
- **署名**：作者 David Quintana（gigaherz@gmail.com）。
- **源码许可**：BSD 3-Clause —— 见 `LICENSES/ToolBelt-BSD-3-Clause.txt`（版权声明原样保留，未改动）。

## 6. Nature's Compass —— CC BY-NC-SA 4.0

- **移植内容**：`compass/natures` 包（寻境司，生物群系定位器），移植自
  [MattCzyr/NaturesCompass](https://github.com/MattCzyr/NaturesCompass)。
  本地改动：包名改为 `com.godofthings.compass.natures`、注册名加前缀、删除 Xaero 联动、
  搜索框拼音匹配适配（装通用拼音搜索时按全拼/首字母，未装降级）、群系名内置中文翻译。
- **署名**：作者 MattCzyr。
- **许可**：**CC BY-NC-SA 4.0**（署名—非商业性—相同方式共享 4.0）—— 见
  `LICENSES/NaturesCompass-CC-BY-NC-SA-4.0.txt`。**严禁商用；衍生作品须以同许可共享。**

## 7. Explorer's Compass —— CC BY-NC-SA 4.0

- **移植内容**：`compass/explorers` 包（探古司，结构定位器），移植自
  [MattCzyr/ExplorersCompass](https://github.com/MattCzyr/ExplorersCompass)。
  本地改动：同第 6 节口径（包名 / 注册名 / 删除 Xaero 联动 / 拼音搜索 / 结构名内置中文翻译）。
- **署名**：作者 MattCzyr。
- **许可**：**CC BY-NC-SA 4.0** —— 见 `LICENSES/ExplorersCompass-CC-BY-NC-SA-4.0.txt`。**严禁商用。**

## 8. 高级内存卡 —— 见第 9 节（本节号曾用于 AE2WTLib，2026-10 整理时重排；保留节号防外部引用错位）

## 9. Advanced Memory Card —— **GNU GPL v3.0（传染性许可，注意）**

- **移植内容**：`advancedmemorycard` 包（高级内存卡：批量复制模式 / P2P 通道管理器），
  移植自 [suntide-20210418/AdvancedMemoryCard](https://github.com/suntide-20210418/AdvancedMemoryCard)。
  本地改动：包名与命名空间、复制模式选区交互、P2P 频段树界面接 AE2 screens 系统。
- **署名**：作者 suntide（suntide-20210418）。
- **许可**：**GNU GPL v3.0** —— 见 `LICENSES/AdvancedMemoryCard-GPL-3.0.txt`（上游 LICENSE.txt 全文）。
- ⚠️ **传染性说明**：GPL-3.0 代码与本模组其余部分同库分发时，整个作品按 GPL-3.0 的要求
  须以兼容许可开源，**不能按 MIT 单独声明该部分**。本模组选择继续捆绑分发即代表接受该义务；
  若要维持「整体 MIT」，需以原创实现替换本子系统或取得上游作者的双许可授权。

## 10. AE2WTLib（AE2 Wireless Terminals / Wireless Terminal Library）—— MIT License

- **移植内容**：`wt` 包（无耗能无线通用终端），自 v7.15.1 起**逐行照抄 ae2wtlib 1.21.1 源码
  独立移植**（终端定义注册表 / 组件键 / 菜单 / 宿主 / 屏幕 / 热键 / 磁卡全链路，本地改动以
  `[本地改动]` 标注，[本地改动] 清单见 CHANGELOG.md 的 7.15.0 → 7.15.1 节）。
  历史：v7.15.0 曾以「继承上游 api ItemWUT + mixin」方式硬依赖 ae2wtlib 19.2.17+，
  v7.15.1 起撤销硬依赖，编译与运行期零 ae2wtlib 引用。
- **署名**：作者 Mari_023（mari_023）。
- **许可**：MIT —— 依据上游 CurseForge 页 License 栏与其仓库谱系
  [Mari023/AE2WirelessTerminalLibrary](https://github.com/Mari023/AE2WirelessTerminalLibrary)
  的 LICENSE（版权 (c) 2021 mari_023），见 `LICENSES/AE2WTLib-MIT.txt`。
  （说明：上游现代版本主仓库当前无法直接访问，许可依据以上游发布页标注为准。）

## 11. Useless Stretcher —— **All Rights Reserved（未授权，待决项）**

- **移植内容**：`doll` 包（绯绯玩偶）与 `wondrous` 包（荒辰移晷之杖等，v7.12.0），
  移植自 Useless Stretcher（作者 2ixuexi，CurseForge
  <https://www.curseforge.com/minecraft/mc-mods/useless-stretcher>）。
- **许可状态**：上游 CurseForge 许可页标注 **"All Rights Reserved unless otherwise explicitly
  stated"**，且未公开源码仓库。**未取得书面授权前，这两包的衍生代码不满足公开再分发条件。**
- 详细状态与待办见 `LICENSES/useless-stretcher-ALL-RIGHTS-RESERVED.md`（随 jar 打包于
  `META-INF/LICENSE-useless-stretcher-ALL-RIGHTS-RESERVED.md`）。
  **待办：取得 2ixuexi 书面授权，或以原创实现替换这两个子系统。**

## 12. Zifeng Skill Tree（子枫的自定义技能树）—— MIT License

- **移植内容**：`armor/skill` 包（神之套装技能树：7 项增幅 + 7 项共鸣特权），移植自
  [aszqb1125763/zifeng_s_custom_skill_tree-](https://github.com/aszqb1125763/zifeng_s_custom_skill_tree-)。
- **署名**：作者 子枫（不无聊的子枫哥）。
- **许可**：MIT —— 见 `LICENSES/ZifengSkillTree-MIT.txt`（版权 (c) 2026 zifeng）。

## 13. Omni Upgrades（全能升级）—— MIT License

- **移植内容**：`omni` 包（全能升级：批量安装升级、黑名单同步、高亮渲染等，
  原样移植 OmniBlacklistPayload / OmniEnabledPayload / ServerBlacklistPayload /
  OmniSyncEvents / CompatManager / NeighborOffsets / AE2Compat 等）。
- **署名**：作者 雨下的樱花。
- **许可**：MIT —— 依据其 CurseForge 页 License 标注（上游未公开源码仓库），
  注记见 `LICENSES/OmniUpgrades-MIT.txt`。

## 14. Measurements —— MIT License

- **移植内容**：`measurement` 包（量天裁度金尺：激光卷尺选区测量），自 Measurements 移植精简。
- **署名**：源码仓库 [Mrbysco/Measurements](https://github.com/Mrbysco/Measurements)
  （Modrinth/发布署名 jozufozu，其 source_url 指向上述仓库）。
- **许可**：MIT —— 见 `LICENSES/Measurements-MIT.txt`（版权 (c) 2021 Mrbysco）。

## 15. ConstructionSticks —— MIT License

- **移植内容**：`construction` 包（筑界杖/开天权：建筑棒建造/销毁/替换、撤销、AE2 取料适配），
  为上游「下界合金棒」的精简移植。
- **署名**：作者 Mrbysco，上游
  [Mrbysco/ConstructionSticks](https://github.com/Mrbysco/ConstructionSticks)
  （ConstructionWand 的分支，同 MIT）。
- **许可**：MIT —— 见 `LICENSES/ConstructionSticks-MIT.txt`（版权 (c) 2024 Mrbysco）。

## 16. ME-Placement-Tool —— GNU LGPL v3.0

- **移植内容**：`mept` 包（ME 放置工具：方块 / 多方块 / 线缆三类放置工具、幽灵槽界面、
  MEPT 配置与数据组件），照抄上游 ItemMEPlacementTool 等类。
- **署名**：作者 MOAKIEE，上游
  [MOAKIEE/ME-Placement-Tool](https://github.com/MOAKIEE/ME-Placement-Tool)。
- **许可**：GNU LGPL 3.0 —— 见 `LICENSES/ME-Placement-Tool-LGPL-3.0.txt`
  （相关类保持 LGPL-3.0，不得按本模组的 MIT 条款再许可）。

## 17. Mekanism Card —— **许可未验证（待决项）**

- **移植内容**：`superfusion` 包（超级融合卡：AE2 桥接、FTB Ultimine 兼容、配置粘贴等，
  类注释自述移植自 Mekanism-Card 的 AE2Integration / UltimineCompat 等）。
- **许可状态**：上游仅经 CurseForge 分发、未公开源码仓库；其许可页无法自动读取，
  **截至 2026-10 本仓库未能确认上游许可证文本**。详见
  `LICENSES/MekanismCard-UNVERIFIED.md`。**待办：向原作者确认许可并补齐声明。**

---

## 非第三方：本模组自有 / 作者自有项目并入的部分

- `aalink` 包（AE 无线连接器卡与全局桥接）：并入自作者（ZeoNG129）本人的 AALink 项目
  （CurseForge <https://www.curseforge.com/minecraft/mc-mods/aalink>，作者 ZeoNG）。
  该包随本模组按 `mod_license`（MIT）分发，不属于第三方代码。
- 除上述移植部分外，本模组的其余代码与素材（神之熔炉 / 矿机 / 资源机 / 掉落机 / 附魔 /
  加速 / 改造 / 合成 / 护甲本体 / 不毁 / 请神 / 吞噬 / 虚空维度 / 便签 / 传送点 /
  手册 / 背包 / 黑盒 / 万物原石等）由本模组作者创作，按 `gradle.properties` 的
  `mod_license`（MIT，见仓库根 LICENSE.txt）分发。
