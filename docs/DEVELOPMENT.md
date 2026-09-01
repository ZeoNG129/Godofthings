# 开发文档（DEVELOPMENT）

本文面向后续维护者，描述本模组的架构约定、注册模式与协作规范。

## 1. 技术栈

| 项 | 值 |
|----|----|
| Minecraft | 1.20.1 |
| Forge | 47.4.20（`forge_version_range [47,)`） |
| 映射 | official（`mapping_channel=official`, `mapping_version=1.20.1`） |
| Java | 17（toolchain 固定） |
| Gradle | 8.8（wrapper） |
| ForgeGradle | [6.0,6.2) |

版本号更替规范（`x.x.x`，唯一来源 `gradle.properties` 的 `mod_version`）：

| 变更类型 | 版本动作 | 示例 |
|----------|----------|------|
| 修复 / 优化 | 末位 +1，**到 10 自动进位到第二位**（末位归零） | 1.3.0 → 1.3.1 … 1.3.9 → 1.4.0 |
| 新增小物品 | 第二位 +1，末位归零 | 1.3.0 → 1.4.0 |
| 系统性新增（新能量系统、新维度等大系统/大改） | 首位大版本 +1，后两位归零 | 1.x.x → 2.0.0 |

## 2. 注册模式

所有注册集中在主类 `src/main/java/com/godofthings/Godofthings.java`，使用 `DeferredRegister`：

- 方块 / 物品 / 方块实体 / 菜单 / 配方序列化器 / 创造模式标签页均以 `public static final RegistryObject<T>` 暴露；
- `modEventBus` 传入构造器完成 `register()`；
- 客户端屏幕绑定在 `client/ClientModEvents.java`（`MenuScreens.register`）；
- 网络通道 `network/WandMessages.java` 在 `commonSetup` 中 `enqueueWork` 注册；
- AE2 GridLinkable 在 `commonSetup` 的 `registerGridLinkables()` 注册（AE2 缺席时由类加载隔离自动跳过）；
- Ad Astra 兼容走 `handler/AdAstraCompat.java`：**ModList 判断 + 未安装直接 return**，这是本模组唯一允许的可选联动模式。

新增内容清单（checklist）：

1. `Godofthings.java` 注册方块/物品/BE/Menu；
2. `blockstates/<id>.json` + `models/block|item/<id>.json` + 对应贴图；
3. `data/godofthings/recipes/<id>.json` 与 `loot_tables/blocks/<id>.json`；
4. **双语** `lang/en_us.json` 与 `lang/zh_cn.json` 同步补键（见 §5）；
5. 若有 GUI：`menu/` + `client/screen/` + `textures/gui/<name>.png`。

## 3. 资源约定

- 所有资源位于 `assets/godofthings/` 与 `data/godofthings/`，**命名空间一律 `godofthings`**；
- `pack.mcmeta` 的 `pack_format=15`（MC 1.20.1）由 processResources 展开，勿手改版本号字段；
- **GUI 贴图画布一律 256×256**，内容画在左上角（如 176×166 区域）：`GuiGraphics.blit(tex, x, y, u, v, w, h)`
  的 6 参重载**硬编码按 256×256 归一化 UV**——贴图文件实际尺寸若是 176×166，游戏只会采样其左上角
  约 121×108 区域并拉伸铺满整个 GUI（表现为"槽位偏右下、整体错位"）。新 GUI 贴图必须开 256×256 画布；
- **GUI 贴图几何规范（与 `creative_energy_cube_gui.png` 一致，2.0.4 起全部机器统一）**：
  ① 面板 `#C6C6C6`，2px 白倒角（上/左）+ 2px `#555555`（下/右）；
  ② 槽位格框 18×18 画在 **(sx-1, sy-1)**（sx/sy 为 Menu 槽位坐标）：2px `#373737` 上/左 +
     2px `#FFFFFF` 下/右 + 14×14 `#8B8B8B` 内芯——旧贴图 1px 边框/17×17 格导致物品压边、格子观感粘连；
  ③ 「物品栏」标签 y = 物品格网格顶行 y − 12（原版标准），标题与标签颜色 `0x404040`（原版深灰，勿用白色）；
  ④ 物品格网格顶行 → 快捷栏标准间距 **+58**（如 84→142）；面板高 = 快捷栏格底 +2px 倒角 + 间隙；
  ⑤ 生成/校验脚本：`tools/gui/ui-regen-gui.ps1`（重生成 7 张机器贴图）、`tools/gui/ui-assert-gui.ps1`
     （像素级断言：画布/边界/每个槽位内芯位置）、`tools/gui/ui-scan-gui.ps1`（扫描诊断）。
- 伤害类型等自定义注册用数据包：`data/godofthings/damage_type/beef_tool.json`（造化垂青之杖伤害，配套 `WandDamageTypes.BEEF_TOOL`）。

## 4. 版本号与 mods.toml

- `src/main/resources/META-INF/mods.toml` 的 `version` 字段写 **`${mod_version}` 占位符**，
  由 `build.gradle` 的 `processResources`（`filesMatching(['META-INF/mods.toml','pack.mcmeta'])`）从
  `gradle.properties` 的 `mod_version` 展开成实际版本；
- **升级版本只改 `gradle.properties` 一处**，不要把硬编码版本写回 mods.toml；
- 何时升哪一位：按 §1「版本号更替规范」——修复/优化末位 +1（到 10 进位）、新增小物品第二位 +1、系统性新增首位 +1。

## 5. 语言文件（双语强制对齐）

- `lang/en_us.json` 与 `lang/zh_cn.json` 必须**键集合完全一致**（当前各 195 键）；
- 任何新功能提交必须同时补齐两个文件；英文值放英文，中文值放中文，**不要在 en_us 里放中文文案**；
- 不要覆盖 `enchantment.minecraft.*`、`block.minecraft.*` 等原版命名空间键——原版语言文件自带；
- 校验方法（PowerShell + node 皆可）：

```powershell
$en = (gc src\main\resources\assets\godofthings\lang\en_us.json -Raw | ConvertFrom-Json).PSObject.Properties.Name
$zh = (gc src\main\resources\assets\godofthings\lang\zh_cn.json -Raw | ConvertFrom-Json).PSObject.Properties.Name
"missing in zh: $(($en | ? { $zh -notcontains $_ }).Count) / missing in en: $(($zh | ? { $en -notcontains $_ }).Count)"
```

## 6. 配置文件

| 文件 | 注册点 | 内容 |
|------|--------|------|
| `godofthings-machines.toml` | `Godofthings.java`（`ModConfig.Type.SERVER`，显式文件名） | 矿机/资源机/掉落机参数（`config/MachinesConfig.SPEC`） |

> 注意：注册配置时显式指定文件名，不依赖 Forge 默认命名，避免未来新增配置时互相覆盖。

## 7. 依赖

全部为 `compileOnly`（不打包、运行时按 ModList 可选）：

| 依赖 | 来源 | 用途 |
|------|------|------|
| EMI 1.1.24 | `libs/emi-1.1.24+1.20.1+forge.jar` | 神之熔炉配方展示 |
| JEI 15.20.0.133 | `libs/jei-1.20.1-forge-15.20.0.133.jar` | 神之熔炉配方展示 |
| AE2 | `libs/ae2-1.20.1.jar` | 造化垂青之杖 AE 存储优先模式 |

`libs/` 下 jar 随仓库保留（flatDir 仓库 `libs`）。

## 8. 历史注记

- **2.0.1 配方重平衡 + 能量立方 GUI 重制**：全部合成配方按进度阶梯重排
  （入门：神之合成/熔炉 → 物流：掉落机/传送门 → 生产：矿机/资源机 → 附魔线：附魔台→天神附魔台 →
  神装/神器：护甲/不毁/造化垂青之杖）。修复强弱倒挂（资源复制机原 8 绿宝石、掉落机原 72 绿宝石）；
  护甲从 24 个下界之星改为 下界合金件+星+钻石块；造化垂青之杖改为 星/烈焰棒/合金锭 竖排；
  神之不毁降为 星+4 黑曜石（消耗型催化剂）；创造能量立方 GUI 收敛为单充能格（水平居中 x=79），
  贴图按标准 176x166 槽位网格重制（充能格 (78,29)、背包 (7,83)、快捷栏 (7,141)，槽位=格心-1px）。
- **2.0.2 能量立方标签布局修正**：充能格/输出标签改为紧贴居中单槽的上/下沿居中排布
  （`renderLabels` 用 `font.width` 计算居中 x），修复标签堆在左侧与居中槽位脱节的观感问题。
- **2.0.3 GUI 错位根因修复（重要教训）**：用户截图实测证实，错位真因是**贴图画布非 256×256**——
  `blit` 6 参重载按 256 归一化 UV，176×166 的贴图被"采样左上 121×108 再拉伸"。修复：
  ① `creative_energy_cube_gui.png` 以 256×256 画布重生成；② 顺带修复原作者遗留同款 bug
  `god_change.png`（176×120→256×256 内容原样搬运）；③ 能量立方布局按用户选择改为**方案C 极简**
  （无"充能格"标签，仅居中槽位 + 下方输出行），删除失效 lang 键 `gui.godofthings.creative_energy_cube.charge`。
  经验：**文件尺寸 ≠ 画布尺寸**，任何新 GUI 贴图先开 256×256 画布（见 §3）。
- **2.0.4 机器 GUI 全面标准化**：用户反馈 5 项问题（整体偏下、熔炉/矿机"物品栏"标签与首行重叠、
  资源/掉落间距过大、附魔物品栏格子粘连）。根因：旧机器贴图槽位格是 1px 边框/17×17 且画在 (sx,sy)，
  与物品渲染几何不符，也与 2.0.3 立方贴图的 2px/18×18/(sx-1,sy-1) 标准不一致。修复：
  ① 7 张机器贴图（furnace/furnace_config/miner/resource/drop/enchant/change）按立方几何重生成
  （脚本 `tools/gui/`，断言全 PASS）；② 标签统一 `inv_y-12` + `0x404040`（熔炉 78→72、矿机 150→144）；
  ③ 资源/掉落物品栏 118/176→标准 84/142（高度 196→166）；④ 附魔重排：LEVEL_Y 144→142、
  行2按钮 162→158、快捷栏 232→236、高度 250→**256**（260 会超出 256 画布——教训：
  **面板高度 ≤256 必须先于布局确定**，FillRectangle 越界会被静默裁掉）；⑤ 矿机快捷栏 210→214、
  高度 230→234（网格与快捷栏原来仅差 2px 相贴）。
- **2.0.5 物品/方块贴图全面重绘（AI 生成管线）**：按 `docs/TEXTURE_PROMPTS.md` 提示词包由图像模型
  生成 28 张 1024px 原图（12 物品 + 16 方块），`tools/tex/convert-downloads.ps1` 完成
  背景移除（边框泛洪 + 容差 30）→ alpha 感知盒式降采样 1024→16 → 阈值清理（a<10 归零）→
  替换资产；预览拼图 `ui-preview/textures-2.0.5.png`。顺带修复**时运权杖紫黑棋盘格**：
  `god_favor_wand_fortune.json` 模型早已存在但其贴图在去 vendored 时被清理，本版补齐
  `textures/item/god_favor_wand_fortune.png`。★教训：图像模型输出的 PNG 常是
  **Format24bppRgb（无 alpha 通道）**——GDI+ 对这种位图 `LockBits(Format32bppArgb)` 的写回会丢失，
  必须先 `Clone` 成 32bppArgb 再做像素操作；另 PS 5.1 读**无 BOM UTF-8 脚本按 ANSI**，
  脚本里写中文路径会乱码，用 `[char]0x4E0B` 码点拼接。
- 构建网络故障排查：本机代理未启动时 `gradlew` 会因 `C:\Users\<user>\.gradle\gradle.properties`
  里的 `systemProp.http(s).proxyHost=127.0.0.1:7890` 全部连接失败；此时加
  `-I fix.init.gradle`（腾讯公共镜像直连）即可完成解析，详见 README。

## 9. 常用命令与自动部署

```powershell
$env:JAVA_HOME = 'E:\MC\java\java17'
.\gradlew compileJava        # 快速编译检查
.\gradlew build              # 完整构建（含 reobfJar + deployJars 自动同步）
.\gradlew runClient          # 开发客户端
.\gradlew runData            # 数据生成
```

**构建产物自动同步（deployJars）**：`build` 成功后自动把 `build/libs/godofthings-<版本>.jar`
复制到两个本机测试 mods 目录，改完代码无需手动拷 jar：

| 目标目录 | 用途 |
|----------|------|
| `E:\MC\modpacks\PCL\versions\1.20.1测试\mods` | PCL 测试实例 |
| `E:\MC\ALL\mods\自制\1.20.1` | ALL 模组仓库 |

- 实现位置：`build.gradle` 末尾的 `deployJars` 任务，`build` 依赖它——**编译失败不会部署**；
- 部署前自动清理目标目录中旧版本号的 `godofthings-*.jar`（防止版本变更后双 jar 重复模组崩溃）；
- 目标目录不存在时跳过并 WARN，不阻断构建；
- 增删部署目标只改 `build.gradle` 顶部的 `deployTargets` 列表；
- 跨 agent 约定入口：仓库根 `AGENTS.md`（新 agent 先读它）。
