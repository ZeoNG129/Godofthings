<div align="center">

<img src="banner.png" alt="God of Things Banner" width="100%" />

<br/><br/>

# 🌌 God of Things（万物之神）

**面向 Minecraft 1.21.1 + NeoForge 的全能型终极功能模组**

*炽神圣火熔炉 · 撼地穿霄掘机 · 天神附魔 · ME 无限元件 · 随身神包 · 原创全能机器与神明武装*

<br/>

[![CI](https://img.shields.io/github/actions/workflow/status/ZeoNG129/Godofthings/build.yml?branch=1.21.1&label=CI&logo=github)](https://github.com/ZeoNG129/Godofthings/actions/workflows/build.yml)
![MC](https://img.shields.io/badge/Minecraft-1.21.1-50586d?logo=minecraft&logoColor=white)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.249-F16436)
![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)
![Version](https://img.shields.io/badge/Release-v7.18.2-7c3aed)
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
| 🔥 **炽神圣火熔炉** <br/>`God Furnace` | 超级熔炉：**17 输入 + 51 输出网格**（照通用机械无限离心机工厂布局）、内置 64 并发、六面 I/O 独立可视化配置，原生深度并网 **AE2**（冶炼产物自动汇入 ME 网络） |
| ⛏️ **撼地穿霄掘机** <br/>`God Miner` | 领域挖掘机：效率 / 时运 / 精准采集全自由调节，六面 I/O 配置，挖掘掉落物完美享受神之共鸣增幅 |
| 💎 **点金圣灵秘柜** <br/>`God Ore Machine` | 矿石倍化专精：原矿 / 金属锭 / 宝石 / 矿石方块（全模组矿石智能兼容）→ 对应高纯产物 **×64 / 轮** |
| 🌱 **丰饶母神温床** <br/>`God Crop Machine` | 农耕繁衍专精：一切作物按成熟收获掉落生成或原样复制，极速产出 **×64 / 轮** |
| ✨ **创生摹刻圣坛** <br/>`God Duplicate` | 万物复制中枢：放入任意物品直接复制全新堆叠，单次吞吐 **×64 / 轮** |
| 🪓 **净木剥相祭台** <br/>`God Peeler` | 自动化去皮工坊：原木 → 去皮原木（原版 11 组木材 + 模组惯例智能识别），**17 输入 + 17 输出网格**、内置 64 并发，可作 AE2 网格节点 |
| 🎁 **万魂余响鉴录仪** <br/>`God Drop` | 实体战利品池：严格按**原版战利品表**概率生成生物全部掉落物；无缝兼容所有模组刷怪蛋 |
| 🥚 **灵胚孳生圣卵** <br/>`God Spawn Egg` | 生物克隆：同种刷怪蛋以 **×64 / 轮** 批量复制，实体 NBT 数据百分百保真 |
| 📜 **至高律法铭刻座** <br/>`God Enchant` | 创世附魔台：任意装备/工具随心自选全附魔词条，附魔等级突破原版限制最高可达 **255 级** |
| ⚡ **普朗克超频催化组件** <br/>`God Accelerator` | 机器并行倍频插件：单个加速提升 **×16**，单槽插满一组 64 个极速倍乘至 **×1024** |
| 🌦️ **神之改造** <br/>`God Change` | 创世天象仪：主世界时间流逝步长、晴雨雷雪天气一键随心调控 |
| 📏 **量天裁度金尺** <br/>`God Measurement` | 激光卷尺：两点选区高亮显示三轴坐标跨度与包围盒体积（纯客户端轻量零开销） |
| ♾️ **原初万象圣晶** <br/>`Universal Cell` | 一件元件同时无限供应 **5 种资源**：水 / 熔岩 / 圆石 / 空白样板 / **福禄伊克斯色 ME 玻璃线缆**；容量 **9.2E**（9,223,372,036,854,775,807，AE2 的 long 上限与显示上限）；多元件叠加靠 `KeyCounter` 钳制 + BigInteger 精确记账，不会溢出成负数；闲置耗电 8 AE/t。取代旧版 4 个单资源元件 |
 | 🌌 **吞噬万籁的永寂** <br/>`Devouring Silence` | 终极 ME 存储元件（整包移植自 [ExtendedAE+](https://github.com/GLodBlock/ExtendedAE-Plus)，LGPL-3.0）：**种类不限 + 容量 BigInteger 无限**，任意物品 / 流体 / AE2 附属资源都能存；终端里单个物品最多显示 **9.2E**，但继续插入不丢、超出仍精确记账；闲置耗电 512 AE/t，4 个升级卡槽 + 分区过滤；真实数据存于世界存档（`world/data/infinity_biginteger_cells.dat`），元件只带 UUID，**丢了可用 `/godofthings infinity recover` 找回**；名字与 tooltip 用 `§` 格式码做彩色渐变 |
| 💳 **高级内存卡** <br/>`Advanced Memory Card` | AE2 内存卡增强版（移植自 suntide_20210418 的 Advanced Memory Card）：**复制模式** 潜行右键偷配置 → 右键两个角选长方体区域 → 对空气右键批量粘贴（<kbd>V</kbd> 切模式）；**配置模式** 右键 P2P 隧道打开通道管理器（P2P 类型→频段→设备三级树、改名 / 绑定 / 待绑定 / 高亮 / 定位传送 / 分配新频段 / 一键自动输入输出） |
| 🛠️ **天工造化神机** <br/>`God Craft` | 高级自动化合成台：直观参数配置 / 8 组模板预设槽 / 智能均分投料 |
| 🛡️ **众神之铠** <br/>`God Armor` | 诸神四件套：集成 **8 大被动功能开关** 与天神技能树（7 项增幅 + 7 项共鸣特权） |
| ❤️ **神之免死敕** <br/>`God Invite` | 生物赐福法杖：右键生物赋予超限无限生命值（支持随时切换反转） |
| 🗑️ **永夜归虚深井** <br/>`God Devourer` | 虚空垃圾箱：清空杂物方块 + 背包 GUI 快捷一键吞噬清空按钮 |
| 🌀 **传送点** <br/>`Waypoints` | 维度地标导航：按下快捷键呼出高颜值 GUI，支持快速跃迁 / 编辑 / 置顶 / 跨端导出备份，上限 256 处 |
| 🌍 **无用维度** <br/>`Useless Dimension` | 奇景探险：自带三大特殊维度世界，配备可视化参数控制面板 |
| ⏳ **定晷封时神碑** | 法则方块：放置后永久锁定当前世界的时间流逝与天气演变 |
| 💥 **生灵止步界石** | 领域压制：以方块为中心半径 512 格范围内强制禁绝任何敌对生物自然生成 |
| 📦 **微型黑洞** <br/>`God Black Box` | 智能拾取黑洞：支持白名单/黑名单精准过滤，无堆叠上限，滚轮一键启闭 |
| 📡 **极光灵脉引渡枢** <br/>`God Transmitter` | 跨维无线输电：为指定机器与玩家无线注能，支持跨维度传输，四大分类标签 |
| 🔗 **同心契** <br/>`God Binding` | 快速配对工具：右键机器即刻绑定至极光灵脉引渡枢（副手手持放置方块时自动配对） |
| ⚙️ **摹律印** <br/>`God Config` | 机器配置模板：**潜行右键**机器保存六面 I/O 模式 / AE 开关 / 加速数量，**普通右键**同类机器一键粘贴；加速按存下的数量从物品栏扣（创造不扣），不够会提示 |
| 🏗️ **筑界杖** <br/>`God Construction` | 建筑棒移植：**右键**按当前模式批量施工（建造 / 销毁 / 替换，**潜行右键**循环切换）；建造可平铺 1024 格、右键空气可悬空放置（天使），销毁一次 81 格且不掉落，替换需副手拿方块；**自带不毁**；**潜行右键空气**开设置界面（撤销上次操作最多 **10 步**、**预览线框**、**相似方块匹配**）；**副手方块优先**（材料也从副手扣）；会开界面的方块默认开界面、**按住 Ctrl 右键**强制施工；在 AE2 无线接入点链接后可**直接从 ME 网络取料** |
| 👑 **开天权** <br/>`Heavenly Construction` | 筑界杖的天神版：操作完全一致，但**放置数 / 破坏数 / 悬空距离全部 = int 上限**，耐久同样无限；**副手方块优先**、会开界面的方块**按住 Ctrl 右键**强制施工；超大操作按 **每 tick 8192 格**分批执行不会卡服；配方 = 筑界杖 ×1 + 万物原石 ×8 |
 | 🧭 **寻境司** <br/>`Nature's Compass` | 生物群系定位器（整包移植自 [Nature's Compass](https://www.curseforge.com/minecraft/mc-mods/natures-compass)，CC BY-NC-SA 4.0）：右键开界面列出全世界所有生物群系（按名称 / 来源模组 / 标签 / 维度 / 降水量 / 温度 **六种方式环形切换排序**），选中即按螺旋采样 + 连通性判定搜索最近的落点，实时显示搜索半径与样本数，支持**搜索下一个**、**传送**、耐久条与损坏修复；两份配置（`godofthings-natures-compass.toml` / `-client.toml`）可调最大半径、样本上限、耐久消耗、传送权限、黑名单与 HUD 位置；**搜索框支持拼音**（装了 [通用拼音搜索](https://github.com/Towdium/JustEnoughCharacters) 时按全拼 / 首字母匹配，没装自动降级为普通匹配）；**群系名已内置 67 条官方中文翻译**，不依赖中文语言包 |
 | 🗺️ **探古司** <br/>`Explorer's Compass` | 结构定位器（整包移植自 [Explorer's Compass](https://github.com/MattCzyr/ExplorersCompass)，CC BY-NC-SA 4.0）：右键开界面列出**本存档真实会生成**的全部结构，按名称 / 来源模组 / 分组 / 维度环形切换排序；按结构的放置策略（随机散布 / 同心环 / 通用）自动分派对应搜索算法，支持**搜索下一个**、**传送**、**清除结果**；已删除上游的 Xaero 自动路径点联动；**搜索框支持拼音**（同上）；**内置 34 条原版结构中文名**（远古城市 / 堡垒遗迹 / 试炼密室 …），原版 1.21.1 本身没有任何 `structure.*` 语言键，所以这是本模组补的；配置在 `godofthings-explorers-compass.toml` / `-client.toml` |
| 📜 **万物原石** <br/>`God Recipe` | 一切神之造物的**合成底料**：铁锭 / 铜锭 / 金锭 / 钻石 / 下界合金锭 / 绿宝石 / 红石 / 青金石 / 煤炭 各一个摆满 3×3（有序合成）；**神之套装四件 + 18 台机器 / 创造方块 + 普朗克超频催化组件 + 全套工具（免死敕 / 更改 / 测量 / 手册 / 便签 / 背包 / 腰带 / 配置 / 不毁 / 黑盒 / 绑定）已全部改用它当原料** |
| 📝 **神思夙愿天书** <br/>`God Note` | 沉浸任务清单：多层子任务 / 鼠标拖拽重排 / 桌面屏幕悬浮窗 HUD，数据绑定玩家独立存档 |
| 🎒 **须弥囊** <br/>`God Backpack` | **120 格** 超大随身便携仓库（单档独占，右键直接打开）。内置**一键整理**（按名称 / 模组 / 数量 / 标签）、**记忆格锁定**（记忆物品、整理不位移、快捷存入优先填入）、**忽略整理锁**、**模组精准搜索**（支持 `@模组名` 筛选）、**一键存入 / 快速取出**、**忽略耐久 / 忽略 NBT** 过滤开关；支持鼠标滚轮平滑翻页（9 列 × 6 行可视区域）。支持绑定快捷键直接呼出（存放于主背包或 **Curios 饰品栏背部槽位**均可生效，模组自动解锁槽位） |
| ⏳ **荒辰移晷之杖** <br/>`Wondrous Staff` | 全能加速之杖（完整移植自 [UselessStretcher](https://github.com/sorrowmist/UselessStretcher)，并**继承原版造化杖全部能力**：永远正确工具等级 / 不可损坏 / 五种工具形态（G 轮盘切换扳手等）/ Tab 连锁挖掘 / R 强制挖掘 / 顺手收菜 / 催熟 / 剪刀 / 打火石 / 短距传送 / 强制击杀 / 范围伤害 / 杀戮光环 / 保护名单 / 击杀磁力 / 无敌 / 高级隐身 / 连点 / AE2 并网）：**Shift+右键**机器加速（**Shift+滚轮**换挡 x2~x1024，**永久模式**不消失）、生物计时器加速、仰望天体加速时间、**3D 范围加速场**（放置历史 / 编辑 / 回收）、**批量召唤与一键收回**、**P 键时停 12 秒**、**相位穿墙**、战利品箱刷新、Mekanism 辐射防护；打树叶有概率掉杖与紬紬玩偶（**B 键远程收回紬紬**）；X 开配置界面（Apotheosis 附魔台兼容，未装时隐藏页签） |
| 🧰 **神之皮带** <br/>`God Belt` | 9 格工具腰带（整包移植自 [ToolBelt](https://github.com/gigaherz/ToolBelt)，BSD-3-Clause）：右键打开皮带界面，**按 <kbd>R</kbd> 呼出轮盘**与手持物品互换，**按 <kbd>V</kbd> 打开皮带槽位界面**（装备皮带 + 2×2 合成 + 背包）。**默认满级**（无需皮带包 / 升级），神之工具（免死敕 / 翻云令 / 量天裁度金尺 / 微型黑洞 / 同心契）默认在白名单里，支持染色与穿戴渲染 |
| ⚔️ **诸神黄昏斩刑机** <br/>`God Slaughter` | 领域斩杀阵：自由调节作用范围 / 抢夺等级 / 真实秒杀判定，内置 27 格无上限战利品缓存 |
| 🧲 **万象归引天核** <br/>`God Absorber` | 超距引力场：超广范围瞬间强力吸纳地面掉落物与散落经验球，支持六面独立配置 |
| 🕳️ **虚空维度** | 双向跨维中枢：自带虚空世界与双向跨维传送门方块 |
| 🧸 **紬紬玩偶** <br/>`Two_i Doll` | 用作者皮肤当外观的互动玩偶：右键打开互动界面（摸头 / 送礼 / 好感度），可放出实体陪伴冒险、放进头盔槽坐头顶、佩戴 Curios 饰品槽或摆放为装饰摆件，支持远程收回 |
| 🔋 **无限能阶发生源** | 概念级能源：无限 FE 能量源（全六面满功率持续输出 ∞ FE/t） |
| ♾️ **ME 无限样板供应器** <br/>`Infinity Pattern Provider` | AE2 样板供应器四档强化（1 / 32 / 1024 / 32723 槽，反编译移植自 Infinity Pattern Provider）：1024 槽起翻页浏览（8×4 一页），**每档都有方块与线缆部件两种形态**，挂在 AE2 线缆侧面即可用；与 AE2 原版样板供应器之间可**双向互转**（切石机 / 处理器升级 / 降级），配方与成就同步收录 |
| 🌐 **AE2 并网** | 9 台产资源机器可作 AE2 网格节点（产物自动进入 AE2 网络，GUI 内配备 AE 启闭总闸） |
| 🪄 **ME 放置工具三件套** <br/>ME Placement Tools | 从 ME 网络直接施工的三大神器（移植自 moakiee 的 ME-Placement-Tool，不耗能源版）：**ME 放置工具**对空气右键开 18 槽幽灵配置、G 键轮盘选目标、潜行右键无线访问点绑定；**ME 多方块放置工具**批量放置选中物品（1/8/64/256/1024，三层轮盘选方向/数量/物品，Ctrl+左键撤销）；**ME 线缆放置工具**三模式（直线/平面填充/平面分支）批量铺线缆，内置光谱钥匙功能可直接选全部 17 色，副手持染料自动补染 |
| 📡 **无线连接器** <br/>Wireless Connector | 把任意位置的 AE 设备无线接入指定网络（连接器方块 + 连接卡，卡功能移植自 AE2LT 过载频率卡）：潜行右键**无线连接器方块**入网并作为网络入口；**无线连接器卡**潜行右键连接器绑定、右键 AE 设备接入/再点断开、按键开关自动连接（放置即接入）、快捷键连接注视目标，可插进 AE2 无线终端升级槽随身携带 |
| 🖥️ **无耗能无线通用终端** <br/>`Wireless Universal Terminal (No Power)` | 无线通用终端的完全独立移植（v7.15.1 起零 ae2wtlib 依赖）且**不消耗任何能量**（能量条恒满）：与无线合成 / 无线样板 / 无线样板管理终端在工作台合成即可装载（可多台叠加），右键打开、按键或 Shift+滚轮切换当前终端；与原版 ae2wtlib 通用终端互相兼容 |
| 🧲 **磁卡** <br/>`Magnet Card` | 移植自 ae2wtlib 的磁卡升级：装进支持磁卡的终端后自动吸取附近掉落物（吸进背包或经终端绑定的 AE 网络直接入网），过滤列表与模式沿用 ae2wtlib 磁卡界面，与原版 ae2wtlib 磁卡互通 |
| ⚗️ **超级融合卡** <br/>Super Fusion Card | 通用机械机器终极配置器（移植自 Mekanism-Card，软依赖 Mekanism）：等级安装 / 模块升级 / 内存复制 / 配置粘贴四模式 + 按键批量选择 |
| 🏆 **成就树** | 沉浸式成就体系：包含 **33 个成就**，横跨机器线、套装线、便签线与探索维度线 |
| 📖 **众神全知启示录** <br/>`God Manual` | 游戏内维基百科：每一个方块和物品均收录图文词条，支持随时按键查阅 |
| 🩺 **诊断与备份** | 运行 `/godofthings doctor` 智能诊断；支持通过指令无损导出与恢复便签及传送点 |

> 💡 **版本历史**：每个版本详细变更见 [CHANGELOG.md](CHANGELOG.md)；游戏中可随时按下 <kbd>P</kbd> 键调出《众神全知启示录》查阅全物品条目。

---

<a id="快速开始"></a>
## 🚀 快速开始

1. **环境准备**：安装 **Minecraft 1.21.1 + NeoForge 21.1.249** 与 **Java 21**。
2. **下载模组**：从 [Releases](https://github.com/ZeoNG129/Godofthings/releases) 下载 `godofthings-<版本>.jar`（最新 5.x 版本的 jar 文件均可在对应 Release 资产列表中获取）。
3. **安装依赖**：将 jar 放入 `.minecraft/mods/` 目录。
   > ⚠️ **运行依赖说明**：运行期**必须安装 Applied Energistics 2 (AE2)**（模组内多台机器直接实现了 AE2 接口），缺少它将在加载阶段报错退出。无耗能无线通用终端自 v7.15.1 起为完全独立实现，不再需要 ae2wtlib。
4. **探索体验**：进入世界后按下 <kbd>P</kbd> 键即可打开《众神全知启示录》，内置详细图文指导与配方解析。

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
                              #   刷怪蛋、资源三机过滤、去皮机、神之共鸣、神之皮带（10 个测试类）
```

 `check-lang.ps1` 一次查完这些：zh/en 键集双向一致、无空值、代码里所有 `translatable("字面量")` 都有键、运行时拼接的 25 个键前缀仍能解析、**注册的 110 个方块 / 111 个物品 / 5 个实体 ↔ blockstate · item 模型 · 语言键全覆盖**、**每个物品 / 方块都有手册条目**（`manual.godofthings.<注册名>`，系统条目另验标题与正文）、反向的孤儿 blockstate，以及**仓库内所有 `.ps1` 必须纯 ASCII**（UTF-8 无 BOM 又含中文的脚本被 PowerShell 5.1 读错编码时，注释尾字节会被当成续行符、把下一行代码吞掉 —— 这个坑已经踩过两次）；**文档计数与源码一致**（回归测试条数 / AE2 并网机数 / 成就数 / 方块物品实体与前缀数，直接对着源码树数）与**仓库根无解包污染目录**（防解包事故复发）。

 `runGameTestServer` 需要运行期有 AE2（本模组有 9 个方块实体直接 implements AE2 接口），`prepareGameTestMods` 任务会自动从 `libs/` 与 `run-server/mods/` 把 AE2 与 guideme 拷进 `run-gametest/mods/`。CI（`.github/workflows/build.yml`）跑的是 fetch-libs → check-lang → build → game tests。

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
| `/godofthings manual` | **调出众神全知启示录**：随时打开手册界面（与按键 <kbd>P</kbd> 及手册物品右键等价） |

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
| 未绑定 | **众神全知启示录** | 随时随地查阅全模组方块与物品维基（手持手册右键同效） |
| 未绑定 | **须弥囊** | 快速呼出 120 格专属随身仓库（佩戴于饰品栏或放入物品栏均可） |
| 未绑定 | **神思夙愿天书** | 打开便签记事本管理面板，规划建设任务与目标进度 |
| 未绑定 | **便签悬浮调整** | 直接进入便签桌面悬浮窗（HUD）的「拖拽摆放与尺寸调节」模式 |
| 未绑定 | **传送点中枢** | 打开维度坐标管理器，支持一键精准跃迁与坐标编辑 |
| 未绑定 | **众神之铠开关** | 快速切换神之套装的 8 大全能被动功能（飞行、夜视、无敌等） |
| 未绑定 | **神之增幅技能** | 打开天神属性技能加成面板，自选激活属性特权 |

**以上功能键一律默认不绑定**（避免和其它模组撞键）：到「选项 → 按键控制 → 神之」里自己设；**巧匠神巡束带的 4 个键同样默认未绑定**。

---

<a id="目录结构"></a>

## 🗂️ 目录结构

代码全部位于 `src/main/java/com/godofthings/`，按「一个功能子系统一个包」组织：

- **`Godofthings.java`** — 主类：全部 `DeferredRegister` 注册 + 模组配置 + 网络数据包注册（只做注册，不写逻辑）。
- **根级包**（`block/` `block/entity/` `item/` `menu/` `client/` `network/` `handler/` `armor/` `note/` `waypoint/` `manual/` `dimension/` `energy/` `config/` `recipe/` 等）— 首批原创核心功能：神之机器、套装、便签、传送点、手册、虚空维度等。
- **移植子系统包**（各包 `package-info.java` 注明上游与许可）— `beef/`（useless_mod）、`compass/explorers`+`compass/natures`（双罗盘）、`doll/`+`wondrous/`（useless_stretcher）、`belt/`（ToolBelt）、`advancedmemorycard/`、`wt/`（无线通用终端，自 ae2wtlib 独立移植）、`aalink/`（作者自有项目并入）、`mept/`（ME 放置工具）、`superfusion/`（Mekanism-Card）、`omni/`（Omni Upgrades）、`measurement/`、`infinitecell/`（ExtendedAE / ExtendedAE+）、`building/`+`overloadedinterface/`（AE2LT）、`construction/`（ConstructionSticks）、`armor/skill/`（Zifeng Skill Tree）。
- **支撑包** — `gametest/`（数据层回归测试）、`jei/` `emi/`（配方查看器集成）。

资源在 `src/main/resources/`：`assets/godofthings/`（blockstates / models / textures / lang）、
`data/godofthings/`（recipe / loot_table / advancement / dimension / worldgen…，GameTest 专用结构在
`data/godofthings/structure/gametest/`）、`META-INF/`（mods.toml、访问转换器、第三方许可声明）。

---

<a id="许可"></a>

## 📜 许可

本模组核心代码基于 **[MIT 许可证](LICENSE.txt)** 开源 —— 允许自由使用、修改、学习与分发（包括直接收录至第三方整合包），仅需保留原项目版权与许可声明。

⚠️ **部分引入的开源子模块遵循其原始许可协议**：
- **MIT**：useless_mod、AE2WTLib、Zifeng Skill Tree、Omni Upgrades、Measurements、ConstructionSticks
- **LGPL-3.0**：GT New Horizons / PersonalSpace、ExtendedAE / ExtendedAE+、AE2 Lightning Tech Reborn（源码）、ME-Placement-Tool
- **BSD-3-Clause**：ToolBelt
- **CC BY-NC-SA（禁止商用）**：Nature's / Explorer's Compass（4.0）、AE2 Lightning Tech Reborn 素材（3.0）
- **GPL-3.0（传染性）**：Advanced Memory Card —— 与本模组同库分发时整体需满足 GPL-3.0 义务
- ⚠️ **待决项**：Useless Stretcher（doll/wondrous 两包的上游）为 All Rights Reserved、Mekanism Card（superfusion）许可待验证，详见声明文件

完整第三方项目清单与版权声明请见 [`THIRD_PARTY_NOTICES.md`](src/main/resources/META-INF/THIRD_PARTY_NOTICES.md)，开源协议全文见 [`LICENSES/`](LICENSES/)。
