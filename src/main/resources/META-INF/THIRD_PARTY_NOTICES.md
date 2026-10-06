# Third-Party Notices / 第三方代码与素材声明

本模组（God of Things）自 v3.0.0 起**逐字照抄**了下列开源项目的代码与素材（只改包名与命名空间）。
**这些部分的许可独立于本模组自身的 `mod_license`（All Rights Reserved）** —— 取用、再分发本模组时，
请一并遵守下列条款与署名要求。各许可全文见仓库 `LICENSES/`，并随 jar 一起打包在 `META-INF/` 下。

---

## 1. Useless Mod（无用之物）—— MIT License

- **照抄内容**：无用维度（奇数维度 / 偶数维度 / 三维度）子系统（其原「造化杖」部分已在 v5.8.0 删除）。
- **署名**：作者 `C-H716`、`水幕忘忧`；美术 `虾比`、`麦淇淋`。
- **许可**：MIT（见 `LICENSES/useless-mod-MIT.txt`）。
- 说明：上游仓库未随源码附独立 `LICENSE` 文件，其 MIT 许可在 `README.md` 的 License 一节中声明。

## 2. GT New Horizons / PersonalSpace —— GNU LGPL v3.0

- **关联内容**：Useless Mod 的「可配置边界 / 道路 / 中心标记生成模型」适配自该项目
  （<https://github.com/GTNewHorizons/PersonalSpace>）。Useless Mod 的接口、持久化、网络与平台适配为其原创。
- **对本模组的影响**：本模组无用维度的地形生成部分
  （`world/dimension/DimensionGenerationConfig`、`PlatformLayout`、`AbstractPlasticPlatformGenerator`）
  沿用了这套被适配过的生成模型，因此**该部分同样适用 LGPL-3.0**。
- **许可**：见 `LICENSES/PersonalSpace-LGPL-3.0.txt`。

## 3. AE2 Lightning Tech Reborn —— 源码 LGPL-3.0 / 素材 CC BY-NC-SA 3.0

- **照抄内容**：fumo 玩偶系统 —— `block/FumoBlock`、`blockentity/FumoBlockEntity`、
  `item/FumoBlockItem`、`client/FumoBlockRenderer`、`client/SpinningFumoBakedModel`，
  以及方块模型 `assets/godofthings/models/block/hoyoog_fumo.json`。
- **署名**：作者 `MOAKIEE`、`CystrySU`、`gjmhmm8`、`_leng`、`TedXenon`、`MHanHanBing`。
- **源码许可**：GNU LGPL 3.0 —— 见 `LICENSES/AE2LT-LGPL-3.0.txt`
  （上述五个类保持 LGPL-3.0，不得按本模组的 ARR 条款再许可）。
- **素材许可**：**CC BY-NC-SA 3.0** —— 见 `LICENSES/AE2LT-ASSETS-CC-BY-NC-SA-3.0.md`。
  即 `hoyoog_fumo.json` 那份玩家模型要求：**署名、禁止商用、相同方式共享**；
  任何再分发必须保留同样许可，且**不得用于商业用途**。
- **例外**：`assets/godofthings/textures/block/hoyoog_fumo.png` **不是**上游素材，
  它是本模组作者自己的 Minecraft 皮肤（64×64），随本模组按 `mod_license` 分发。

---

## 本模组自身原创部分

除上述逐字照抄的部分以外，本模组的其余代码与素材（神之熔炉 / 矿机 / 资源机 / 掉落机 / 附魔 /
加速 / 改造 / 合成 / 护甲 / 不毁 / 请神 / 吞噬 / 时空永恒 / 生物覆灭 / 虚空维度 / 玩偶贴图等）由本模组作者创作，
按 `gradle.properties` 的 `mod_license`（All Rights Reserved）分发。
