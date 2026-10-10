<div align="center">

<img src="logo.png" width="140" alt="God of Things Logo" style="filter: drop-shadow(0px 8px 16px rgba(0,0,0,0.25));" />

# God of Things（万物之神）

**全能、优雅、高自动化的 Minecraft 1.21.1 现代化「神之」集合模组**

[![CI Status](https://img.shields.io/github/actions/workflow/status/ZeoNG129/Godofthings/build.yml?branch=1.21.1&label=CI&style=flat-square&logo=github)](https://github.com/ZeoNG129/Godofthings/actions)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-50586d?style=flat-square&logo=minecraft)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.249-F16436?style=flat-square)](https://neoforged.net/)
[![Java](https://img.shields.io/badge/Java-21-007396?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/Mod-v5.17.4-7c3aed?style=flat-square)](https://github.com/ZeoNG129/Godofthings/releases)
[![GameTests](https://img.shields.io/badge/GameTests-45%20Passed-2ea44f?style=flat-square)](https://github.com/ZeoNG129/Godofthings)
[![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE.txt)

<br/>

[ 📥 下载安装 ](#-快速开始) &nbsp;•&nbsp;
[ 🌸 核心特性 ](#-核心特性一览) &nbsp;•&nbsp;
[ 🎮 快捷键位 ](#-快捷键位) &nbsp;•&nbsp;
[ 🧪 工程与测试 ](#-工程与测试) &nbsp;•&nbsp;
[ 📜 开源协议 ](#-许可与致谢)

</div>

---

## 🌟 王牌特性速览

<table>
<tr>
<td width="50%">

### 🎒 神之背包 (God Backpack)
- **120 格海量容量**：告别杂乱背包，随身超大仓库。
- **智能记忆格**：锁定特定格子，空槽显示半透明影子，同类物品优先自动存入。
- **即时搜索与过滤**：支持物品名模糊匹配与 `@模组名` 命名空间过滤。
- **饰品栏直通**：自动开启 **Curios 背部饰品槽**，按 <kbd>B</kbd> 随时随地呼出。
- **防套娃与防串台**：菜单级锁定源槽位，杜绝递归与多背包覆盖。

</td>
<td width="50%">

### 📝 神之便签 (God Note)
- **多级任务清单**：支持无限级子任务嵌套、展开与折叠。
- **所见即所得**：任务按住任意拖拽排序，支持快速升降级。
- **屏幕 HUD 悬浮窗**：按 <kbd>M</kbd> 自由定位悬浮挂载，边看边玩。
- **数据保真**：完全绑定玩家存档，支持 SNBT 一键导出与备份恢复。

</td>
</tr>
<tr>
<td width="50%">

### 🏭 工业与 AE2 并网
- **9 台自动化重工直连 AE2**：熔炉、矿机、去皮机、资源三机等直接作为网络节点，产物毫秒级并网。
- **千倍并行加速**：神之加速单体 ×16，一组叠乘实现最高 ×1024 倍速。
- **免管道损耗**：原生六面 I/O 自定义配置，自动推出与抽入。

</td>
<td width="50%">

### 🌌 规则与世界重塑
- **时空永恒**：一键锁定当前世界的白天/黑夜与天气。
- **生物覆灭**：半径 512 格全局阻断自然生物生成，净化卡顿。
- **智能砍杀机**：0~480 格秒杀，原生保护村民、流浪商人、已驯服宠物与命名生物。
- **卷尺三维测量**：两点划线，零开销实时渲染三轴物理线框与尺寸。

</td>
</tr>
</table>

---

<a id="核心特性一览"></a>
## 🌸 核心特性一览

<details open>
<summary><b>🏭 工业与自动化机器（11 种神级方块）</b></summary>

| 机器名称 | 核心功能与机制说明 |
|---|---|
| 🔥 **神之熔炉** God Furnace | 超级熔炉：**9 输入 + 9 输出**、六面 I/O 自定义配置，原生并网 **AE2**（产物自动进网络） |
| ⛏️ **神之矿机** God Miner | 方形范围挖掘机（1~1600 半径），效率/时运/精准采集自选，内置无限液体罐，掉落吃神之共鸣 |
| 🪓 **神之去皮** God Peeler | 原木自动去皮：原生兼容原版 11 种木料及所有模组木质，9+9 槽位独立并行，可并网 AE2 |
| 💎 **神之矿物** God Ore Machine | 只收矿物：原矿 / 锭 / 宝石 / 矿石块（含任意模组矿） → 对应产物 **×64 / 轮** 极速批量产出 |
| 🌱 **神之作物** God Crop Machine | 只收作物：一切植物按收获掉落或复制，**×64 / 轮** 批量丰收 |
| ✨ **神之复制** God Duplicate | 收一切物品：任何东西稳定复制 **×64 / 轮**（全新堆叠） |
| 🎁 **神之掉落机** God Drop | 严格按**原版战利品表**产出生物全部掉落物；全模组刷怪蛋无缝兼容 |
| 🥚 **神之怪蛋** God Spawn Egg | 刷怪蛋复制器：同种蛋 **×64 / 轮**，实体 NBT 数据 100% 原样保真 |
| 🛠️ **神之合成** God Craft | 高级矩阵工作台：8 组配方模板槽、均分原料输入、支持 JEI / EMI 一键传配方 |
| ⚔️ **神之砍杀** God Slaughter | 范围击杀（0~480 格可调）：抢夺最高 255 级，掉落物直入 27 格无限存储；**内置保护名单（不杀宠物/村民/展架）** |
| 🧲 **神之吸收** God Absorber | 大范围吸纳掉落物与经验球，六面 I/O 配置，经验点数可视化存取 |

</details>

<details open>
<summary><b>🎒 便携神物与智能存储</b></summary>

| 物品 / 功能 | 核心功能与机制说明 |
|---|---|
| 🎒 **神之背包** God Backpack | **120 格随身仓库**：记忆格、忽略整理、智能搜索、饰品栏联动，按 <kbd>B</kbd> 即开，防套娃保护 |
| 📦 **神之黑盒** God Black Box | 拾取过滤存储：白名单/黑名单双模切换，无单组堆叠上限，手持滚轮极速开关 |
| 🗑️ **神之吞噬** God Devourer | 虚空垃圾桶：方块形态 + 背包内嵌一键清除快捷按钮 |
| 📏 **神之测量** God Measurement | 便携卷尺：两点选区拉出三维物理线框并显三轴尺寸，纯客户端 0 服务端开销 |
| ♾️ **ME 无限存储元件** Infinity Cell | 水 / 熔岩 / 圆石 / 空白样板 ×4：**21.4 亿** 极限资源无限存取 |

</details>

<details open>
<summary><b>⚡ 能源、时空与世界神权</b></summary>

| 功能系统 | 核心功能与机制说明 |
|---|---|
| 🔋 **创造能量立方** Creative Cube | 无限 FE 能源源泉（∞ FE/t 六面全速输出） |
| ⚡ **神之加速** God Accelerator | 机器硬件加速：单体 ×16，一组 64 个提供高达 **×1024** 倍并行运算倍速 |
| 📡 **神之传输 / 绑定** Transmitter | 跨维度无线 FE 充能网络：使用神之绑定器副手放置机器即刻自动入网 |
| ⏳ **时空永恒** Space-Time Eternity | 放置即刻锁定世界当前时间戳与晴雨雷暴状态，时间永不流逝 |
| 💥 **生物覆灭** Creature Annihilation | 半径 512 格立体空间全局阻断自然生物生成，大幅提升服务器 TPS |
| 🌀 **传送点系统** Waypoints | 按 <kbd>U</kbd> 唤出独立传送管理面板：编辑、置顶、坐标直传与 SNBT 备份导出 |
| 🛡️ **神之护甲** God Armor | 全套 8 大被动神能开关 + 7 大神之增幅技能树节点 |
| 📜 **天神附魔** God Enchant | 自选附魔属性与等级注入，最高支持突破原版上限的 **255 级** |
| 🧸 **皮肤玩偶 HoYooG Fumo** | 12 部件精细玩家模型玩偶：右键可自转，支持佩戴在头上作为饰品 |

</details>

<details open>
<summary><b>🌐 模组联动与生态集成</b></summary>

| 模组 | 集成效果 |
|---|---|
| ⚡ **Applied Energistics 2** | **9 台产资源机器可作 AE2 网格节点**（产物自动进网络，AE 开关可控） |
| 💍 **Curios API** | 自动为玩家解锁「背部」饰品槽，神之背包可直接穿戴并按 <kbd>B</kbd> 触发 |
| 🔍 **JEI / EMI** | 神之合成、神之熔炉配方全量支持查看与一键传输转移 |
| 🎒 **ToolBelt** | 模组核心五大随身神物自动注入腰带白名单，开箱即用 |
| 🏆 **原生进度树** | 包含机器、套装、便签、维度 4 大分支共 **33 个成就** |

</details>

---

<a id="快速开始"></a>
## 🚀 快速开始

### 1. 环境前置

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

### 2. 下载安装

1. 前往 **[Releases 页面](https://github.com/ZeoNG129/Godofthings/releases)**；
2. 下载最新的 `godofthings-<版本>.jar` 并放入游戏 `.minecraft/mods/` 文件夹；
3. 启动游戏后按 <kbd>P</kbd> 即可随时查阅内置的「神之手册」。

> [!TIP]
> **版本号下载指引**：本仓库遵循大版本合并归档规范。5.x 系列的最新 jar 文件均持续上传至 **`v5.1.7` Release 的 Assets 附件列表** 中，请直接在 Assets 内下载最新编号的 jar（如 `godofthings-5.17.4.jar`）。

---

<a id="快捷键位"></a>
## 🎮 快捷键位

默认按键分类：`key.category.godofthings.wand`，可在游戏内「选项 → 控制 → 按键绑定 → 神之」自由更改：

| 按键 | 触发功能 | 说明 |
|:---:|---|---|
| <kbd>P</kbd> | **神之手册 (Manual)** | 游戏内百科全书，所有方块/物品全覆盖 |
| <kbd>B</kbd> | **神之背包 (Backpack)** | 随身仓库直开（支持快捷栏、副手与 Curios 背部槽） |
| <kbd>N</kbd> | **神之便签 (Note)** | 打开任务记事本，编辑/勾选/管理清单 |
| <kbd>M</kbd> | **便签悬浮模式 (HUD)** | 快速进入屏幕任务清单悬浮窗定位模式 |
| <kbd>U</kbd> | **传送面板 (Waypoints)** | 唤出全局传送点管理与瞬移界面 |
| <kbd>O</kbd> | **神之套装开关 (Armor)** | 开启或关闭神之护甲的 8 项被动神能 |
| <kbd>K</kbd> | **神之增幅 (Skills)** | 打开神之增幅技能树界面 |

---

<a id="工程与测试"></a>
## 🧪 工程与测试

本项目拥有严苛的自动化静态扫描与回归测试卡点，保障模组极端场景下的数据安全：

```powershell
# 静态代码与资源覆盖度全量校验
.\check-lang.ps1

# 启动专用服务端执行自动化游戏内测试
.\gradlew runGameTestServer   # 回归测试（共 45 项）（10 个测试类）
