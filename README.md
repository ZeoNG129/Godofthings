<div align="center">

<img src="banner.png" alt="God of Things Banner" width="100%" />

<br/><br/>

# 🌌 God of Things（万物之神）

**面向 Minecraft 1.21.1 + NeoForge 的全能型终极功能模组**

*神之熔炉 · 神之矿机 · 天神附魔 · ME 无限元件 · 随身神包 · 原创全能机器与神明武装*

<br/>

[![CI](https://img.shields.io/github/actions/workflow/status/ZeoNG129/Godofthings/build.yml?branch=1.21.1&label=CI&logo=github)](https://github.com/ZeoNG129/Godofthings/actions/workflows/build.yml)
![MC](https://img.shields.io/badge/Minecraft-1.21.1-50586d?logo=minecraft&logoColor=white)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.249-F16436)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Version](https://img.shields.io/badge/Release-v5.17.4-7c3aed)
![Tests](https://img.shields.io/badge/回归测试-45%20Passed-success)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE.txt)

<br/>

**[📥 立即下载](https://github.com/ZeoNG129/Godofthings/releases)** ·
[✨ 核心特性](#-核心特性一览) ·
[🚀 快速开始](#-快速开始) ·
[🧭 环境要求](#-环境要求) ·
[🛠️ 构建指南](#️-构建指南) ·
[🧪 自动化测试](#-自动化测试) ·
[⌨️ 指令速查](#️-指令速查) ·
[🎮 默认键位](#-默认键位) ·
[🗂️ 目录结构](#️-目录结构) ·
[📜 许可协议](#-许可)

</div>

---

<a id="内容一览"></a>
<a id="核心特性一览"></a>

## ✨ 核心特性一览

| 模块 | 说明 |
|---|---|
| 🔥 **神之熔炉** <br/>`God Furnace` | 超级熔炉：**9 输入 + 9 输出**、六面 I/O 独立可视化配置，原生深度并网 **AE2**（冶炼产物自动汇入 ME 网络） |
| ⛏️ **神之矿机** <br/>`God Miner` | 领域挖掘机：效率 / 时运 / 精准采集全自由调节，六面 I/O 配置，挖掘掉落物完美享受神之共鸣增幅 |
| 💎 **神之矿物** <br/>`God Ore Machine` | 矿石倍化专精：原矿 / 金属锭 / 宝石 / 矿石方块（全模组矿石智能兼容）→ 对应高纯产物 **×64 / 轮** |
| 🌱 **神之作物** <br/>`God Crop Machine` | 农耕繁衍专精：一切作物按成熟收获掉落生成或原样复制，极速产出 **×64 / 轮** |
| ✨ **神之复制** <br/>`God Duplicate` | 万物复制中枢：放入任意物品直接复制全新堆叠，单次吞吐 **×64 / 轮** |
| 🪓 **神之去皮** <br/>`God Peeler` | 自动化去皮工坊：原木 → 去皮原木（原版 11 组木材 + 模组惯例智能识别），9 输入 + 9 输出槽，可作 AE2 网格节点 |
| 🎁 **神之掉落机** <br/>`God Drop` | 实体战利品池：严格按**原版战利品表**概率生成生物全部掉落物；无缝兼容所有模组刷怪蛋 |
| 🥚 **神之怪蛋** <br/>`God Spawn Egg` | 生物克隆：同种刷怪蛋以 **×64 / 轮** 批量复制，实体 NBT 数据百分百保真 |
| 📜 **神之附魔** <br/>`God Enchant` | 创世附魔台：任意装备/工具随心自选全附魔词条，附魔等级突破原版限制最高可达 **255 级** |
| ⚡ **神之加速** <br/>`God Accelerator` | 机器并行倍频插件：单个加速提升 **×16**，单槽插满一组 64 个极速倍乘至 **×1024** |
| 🌦️ **神之改造** <br/>`God Change` | 创世天象仪：主世界时间流逝步长、晴雨雷雪天气一键随心调控 |
| 📏 **神之测量** <br/>`God Measurement` | 激光卷尺：两点选区高亮显示三轴坐标跨度与包围盒体积（纯客户端轻量零开销） |
| ♾️ **ME 无限存储元件** <br/>`Infinite Cells` | 4 种无限存储体（无限水 / 熔岩 / 圆石 / 空白样板）：单一元件吞吐 **21.4 亿**资源无限存取 |
| 🛠️ **神之合成** <br/>`God Craft` | 高级自动化合成台：直观参数配置 / 8 组模板预设槽 / 智能均分投料 |
| 🛡️ **神之护甲** <br/>`God Armor` | 诸神四件套：集成 **8 大被动功能开关** 与天神技能树（7 项增幅 + 7 项共鸣特权） |
| ❤️ **神之请神** <br/>`God Invite` | 生物赐福法杖：右键生物赋予超限无限生命值（支持随时切换反转） |
| 🗑️ **神之吞噬** <br/>`God Devourer` | 虚空垃圾箱：清空杂物方块 + 背包 GUI 快捷一键吞噬清空按钮 |
| 🌀 **传送点** <br/>`Waypoints` | 维度地标导航：按下快捷键呼出高颜值 GUI，支持快速跃迁 / 编辑 / 置顶 / 跨端导出备份，上限 256 处 |
| 🌍 **无用维度** <br/>`Useless Dimension` | 奇景探险：自带三大特殊维度世界，配备可视化参数控制面板 |
| ⏳ **时空永恒** | 法则方块：放置后永久锁定当前世界的时间流逝与天气演变 |
| 💥 **生物覆灭** | 领域压制：以方块为中心半径 512 格范围内强制禁绝任何敌对生物自然生成 |
| 📦 **神之黑盒** <br/>`God Black Box` | 智能拾取黑洞：支持白名单/黑名单精准过滤，无堆叠上限，滚轮一键启闭 |
| 📡 **神之传输** <br/>`God Transmitter` | 跨维无线输电：为指定机器与玩家无线注能，支持跨维度传输，四大分类标签 |
| 🔗 **神之绑定** <br/>`God Binding` | 快速配对工具：右键机器即刻绑定至神之传输（副手手持放置方块时自动配对） |
| 📝 **神之便签** <br/>`God Note` | 沉浸任务清单：多层子任务 / 鼠标拖拽重排 / 桌面屏幕悬浮窗 HUD，数据绑定玩家独立存档 |
| 🎒 **神之背包** <br/>`God Backpack` | **120 格** 超大随身便携仓库（单档独占，右键直接打开）。内置**一键整理**（按名称 / 模组 / 数量 / 标签）、**记忆格锁定**（记忆物品、整理不位移、快捷存入优先填入）、**忽略整理锁**、**模组精准搜索**（支持 `@模组名` 筛选）、**一键存入 / 快速取出**、**忽略耐久 / 忽略 NBT** 过滤开关；支持鼠标滚轮平滑翻页（9 列 × 6 行可视区域）。支持绑定快捷键直接呼出（存放于主背包或 **Curios 饰品栏背部槽位**均可生效，模组自动解锁槽位） |
| ⚔️ **神之砍杀** <br/>`God Slaughter` | 领域斩杀阵：自由调节作用范围 / 抢夺等级 / 真实秒杀判定，内置 27 格无上限战利品缓存 |
| 🧲 **神之吸收** <br/>`God Absorber` | 超距引力场：超广范围瞬间强力吸纳地面掉落物与散落经验球，支持六面独立配置 |
| 🕳️ **虚空维度** | 双向跨维中枢：自带虚空世界与双向跨维传送门方块 |
| 🧸 **皮肤玩偶** <br/>`HoYooG Fumo` | 趣味二次元周边：12 部件精细化玩家模型玩偶，支持右键原地自转、直接佩戴在头顶 |
| 🔋 **创造能量立方** | 概念级能源：无限 FE 能量源（全六面满功率持续输出 ∞ FE/t） |
| 🌐 **AE2 并网** | 9 台产资源机器可作 AE2 网格节点（产物自动进入 AE2 网络，GUI 内配备 AE 启闭总闸） |
| 🏆 **成就树** | 沉浸式成就体系：包含 **33 个成就**，横跨机器线、套装线、便签线与探索维度线 |
| 📖 **神之手册** <br/>`God Manual` | 游戏内维基百科：每一个方块和物品均收录图文词条，支持随时按键查阅 |
| 🩺 **诊断与备份** | 运行 `/godofthings doctor` 智能诊断；支持通过指令无损导出与恢复便签及传送点 |

> 💡 **版本历史**：每个版本详细变更见 [VERSIONING.md](VERSIONING.md)；游戏中可随时按下 <kbd>P</kbd> 键调出《神之手册》查阅全物品条目。

---

<a id="快速开始"></a>
## 🚀 快速开始

1. **环境准备**：安装 **Minecraft 1.21.1 + NeoForge 21.1.249** 与 **Java 21**。
2. **下载模组**：从 [Releases](https://github.com/ZeoNG129/Godofthings/releases) 下载 `godofthings-<版本>.jar`（最新 5.x 版本的 jar 文件均可在对应 Release 资产列表中获取）。
3. **安装依赖**：将 jar 放入 `.minecraft/mods/` 目录。
   > ⚠️ **运行依赖说明**：运行期**必须安装 Applied Energistics 2 (AE2)**（模组内多台机器直接实现了 AE2 接口，缺少依赖将在加载阶段报错退出）。
4. **探索体验**：进入世界后按下 <kbd>P</kbd> 键即可打开《神之手册》，内置详细图文指导与配方解析。

---

| 组件 | 版本要求 | 属性 |
|---|---|---|
| **Minecraft** | `1.21.1` | 基础平台 |
| **NeoForge** | `21.1.249+` | 模组加载器 |
| **Java Runtime** | `Java 21` (推荐 Temurin / GraalVM) | 运行环境 |
| **Applied Energistics 2 (AE2)** | 最新 1.21.1 NeoForge 版 | 🔴 **必选前置**（硬依赖） |
| **Curios API** | 最新 1.21.1 版 | 🟢 **推荐安装**（解锁饰品栏背包槽） |
| **EMI / JEI** | 最新 1.21.1 版 | 🟢 **推荐安装**（配方查看） |

> [!IMPORTANT]
> **关于运行依赖**：由于模组内 9 台自动化机器底层实现了 AE2 网格节点接口，**运行期必须安装 AE2**，否则游戏将在类加载阶段终止并提示缺少依赖。

- **JDK 21**（Minecraft 1.21.1 / NeoForge 21.1 必需标准运行环境）
- **NeoForge** `21.1.249` + **ModDevGradle 2.0.x**（由 Gradle Wrapper 自动调度）
- 构建期初次构建需联网拉取 NeoForge 基础依赖；JEI / EMI / AE2 本地集成库均已预置于 `libs/` 目录，日常构建无需依赖外部网络。

---

<a id="构建"></a>
<a id="构建指南"></a>

## 🛠️ 构建指南

```powershell
# JDK 21 自动由 gradle.properties 的 org.gradle.java.home 加载，直接执行构建即可：
.\gradlew build
# 构建产物输出路径：build\libs\godofthings-<mod_version>.jar
```

**自动同步部署机制**：`build` 构建任务完成后，`deployJars` 会将构建出的 jar 文件自动同步复制到本机测试客户端目录，并自动清理旧版本构建产物，实现即改即测。

常用开发调试指令：

```powershell
.\gradlew runClient   # 启动开发客户端进行实时调试
.\gradlew runServer   # 启动开发专用独立服务端
.\gradlew runData     # 运行数据生成器（产物输出至 src/generated/resources）
```

> 📦 **首次克隆仓库**：运行 `.\fetch-libs.ps1` 脚本从 release 缓存拉取 4 个第三方模组 jar（JEI ×2 / EMI / AE2，约 9MB）到 `libs/`，即可离线构建。

---

<a id="测试"></a>
<a id="自动化测试"></a>

## 🧪 自动化测试

```powershell
.\check-lang.ps1              # 静态校验：语言键 + 资源一致性 + 手册覆盖 + 脚本纯 ASCII（见下）
.\gradlew runGameTestServer   # 回归测试（共 45 项）：便签 / 传送点、掉落机战利品表、
                              #   刷怪蛋、资源三机过滤、去皮机、神之共鸣、ToolBelt 兼容（7 个测试类）
```

`check-lang.ps1` 一次查完这些：zh/en 键集双向一致、无空值、代码里所有 `translatable("字面量")` 都有键、运行时拼接的 7 个键前缀仍能解析、**注册的 22 个方块 / 23 个物品 / 0 个实体 ↔ blockstate · item 模型 · 语言键全覆盖**、**每个物品 / 方块都有手册条目**（`manual.godofthings.<注册名>`，系统条目另验标题与正文）、反向的孤儿 blockstate，以及**仓库内所有 `.ps1` 必须纯 ASCII**（UTF-8 无 BOM 又含中文的脚本被 PowerShell 5.1 读错编码时，注释尾字节会被当成续行符、把下一行代码吞掉 —— 这个坑已经踩过两次）；**文档计数与源码一致**（回归测试条数 / AE2 并网机数 / 成就数 / 方块物品实体与前缀数，直接对着源码树数）与**仓库根无解包污染目录**（防解包事故复发）。

`runGameTestServer` 需要运行期有 AE2（本模组有 7 个方块实体直接 implements AE2 接口），`prepareGameTestMods` 任务会自动从 `libs/` 与 `run-server/mods/` 把 AE2 与 guideme 拷进 `run-gametest/mods/`。CI（`.github/workflows/build.yml`）跑的是 fetch-libs → check-lang → build → game tests。

---

<a id="指令"></a>
<a id="指令速查"></a>

## ⌨️ 指令速查

| 指令 | 作用与说明 |
|---|---|
| `/godofthings doctor` | **模组综合诊断**：扫描已启用的联动 mod、统计技能增幅解析状态、检测是否有第三方 Mixin 冲突覆盖 |
| `/godofthings export note [名称]` | **导出便签**：将当前玩家便签清单导出为 SNBT 结构化文件（存放在 `<存档>/godofthings/exports/`） |
| `/godofthings export points [名称]` | **导出传送点**：将玩家标记的维度传送坐标点全量导出备份 |
| `/godofthings import note <名称>` | **导入便签**：从 SNBT 备份中恢复便签（导入前自动创建安全备份） |
| `/godofthings import points <名称>` | **导入传送点**：从备份恢复坐标数据，支持跨存档迁移 |
| `/godofthings manual` | **调出神之手册**：随时打开手册界面（与按键 <kbd>P</kbd> 及手册物品右键等价） |

---

<a id="版本号"></a>

## 🔢 版本号命名规范

项目严格遵循语义化三段式版本命名规范 `x.y.z`（详情请参阅 [VERSIONING.md](VERSIONING.md)）：

| 变更类型 | 升级规则 | 说明示例 |
|---|:---:|---|
| **修复 / 细节优化** | 末位 `+1` | 例如 `5.17.3` → `5.17.4` |
| **小功能 / 新物品添加** | 第二位 `+1`，末位归零 | 例如 `5.17.9` → `5.18.0`（末位满 10 自动进位） |
| **大型架构调整 / 系统性新增** | 首位大版本 `+1`，后两位归零 | 例如 `4.x.x` → `5.0.0` |

---

<a id="键位"></a>
<a id="默认键位"></a>

## 🎮 默认键位

可在游戏内「选项 → 控制 → 按键绑定」的 `key.category.godofthings.wand` 分类中自由改建：

| 快捷键 | 功能 | 说明 |
|:---:|---|---|
| <kbd>P</kbd> | **神之手册** | 随时随地查阅全模组方块与物品维基（手持手册右键同效） |
| <kbd>B</kbd> | **神之背包** | 快速呼出 120 格专属随身仓库（佩戴于饰品栏或放入物品栏均可） |
| <kbd>N</kbd> | **神之便签** | 打开便签记事本管理面板，规划建设任务与目标进度 |
| <kbd>M</kbd> | **便签悬浮调整** | 直接进入便签桌面悬浮窗（HUD）的「拖拽摆放与尺寸调节」模式 |
| <kbd>U</kbd> | **传送点中枢** | 打开维度坐标管理器，支持一键精准跃迁与坐标编辑 |
| <kbd>O</kbd> | **神之护甲开关** | 快速切换神之套装的 8 大全能被动功能（飞行、夜视、无敌等） |
| <kbd>K</kbd> | **神之增幅技能** | 打开天神属性技能加成面板，自选激活属性特权 |

---

<a id="目录结构"></a>

## 🗂️ 目录结构

```
src/main/java/com/godofthings/
  ├─ Godofthings.java        # 主类：全部 DeferredRegister 注册 + 模组配置 + 网络数据包注册
  ├─ block/ block/entity/    # 方块与方块实体（熔炉/矿机/资源机/去皮机/掉落机/附魔/合成/传送门…）
  ├─ item/ menu/ client/     # 物品、容器菜单交互、网络包与渲染窗口
  ├─ armor/ skill/           # 神之套装：8 个功能开关 + 神之增幅 7 节点（含老存档无损迁移）
  ├─ note/ waypoint/         # 神之便签（支持 HUD 悬浮窗）、维度传送点
  ├─ measurement/            # 神之测量（激光卷尺，自 Measurements 移植精简）
  ├─ infinitecell/           # ME 无限存储元件（水/熔岩/圆石/空白样板，自 ExtendedAE 移植优化）
  ├─ manual/                 # 神之手册（基于游戏内注册表自动生成图文词条）
  ├─ network/                # 网络数据包通信（套装开关/技能、传送点、便签、维度配置）
  ├─ recipe/ config/         # 自定义配方与机器核心运行参数配置
  ├─ dimension/ energy/      # 虚空维度系统、创造能量立方
  ├─ handler/ emi/ jei/      # 模组联动集成（Ad Astra / EMI / JEI / AE2）
  └─ beef/                   # 移植 useless_mod 部分（无用维度、传送方块、合金炉、AE 链接等）
src/main/resources/
  ├─ assets/godofthings/     # blockstates / models / textures / lang
  ├─ data/godofthings/       # recipe / loot_table / advancement / dimension / worldgen…
  └─ META-INF/neoforge.mods.toml
```

---

<a id="许可"></a>

## 📜 许可

本模组核心代码基于 **[MIT 许可证](LICENSE.txt)** 开源 —— 允许自由使用、修改、学习与分发（包括直接收录至第三方整合包），仅需保留原项目版权与许可声明。

⚠️ **部分引入的开源子模块遵循其原始许可协议**：
- **useless_mod**：MIT 许可证
- **GT New Horizons / PersonalSpace**：LGPL-3.0 许可证
- **AE2 Lightning Tech Reborn**：源码基于 LGPL-3.0，模型与材质素材遵循 CC BY-NC-SA 3.0（**该模型素材严禁用于商业用途**）

完整第三方项目清单与版权声明请见 [`THIRD_PARTY_NOTICES.md`](src/main/resources/META-INF/THIRD_PARTY_NOTICES.md)，开源协议全文见 [`LICENSES/`](LICENSES/)。

### 📎 第三方代码与开源署名

本模组的**神之套装技能树**（数值算法、效果体系与界面布局设计）移植自开源项目 **[Zifeng Skill Tree / 子枫的百宝箱](https://github.com/ZeoNG129)**（原作者 **zifeng**），遵循 **MIT License**：

```text
MIT License
Copyright (c) 2026 zifeng

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

移植部分已根据本模组定位深度重构：移除了复杂的技能点消耗与前置限制，深度融合至神之套装被动体系；**v5.9.0 起只保留了 7 项核心「神之增幅」开关节点**，并根据规约去除了原作者个人命名前缀。其余引入模块（无用维度、Fumo 玩偶等）的完整许可说明均已收录于 [`THIRD_PARTY_NOTICES.md`](src/main/resources/META-INF/THIRD_PARTY_NOTICES.md)。
