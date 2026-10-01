# God of Things（万物之神）

一个面向 **Minecraft 1.21.1 + NeoForge** 的集合型模组，全部内容为原创。

> 神之熔炉、神之矿机、天神附魔、神之工具…… 一整套"神之"工具与机器。

## 内容一览

| 模块 | 说明 |
|------|------|
| 神之熔炉 God Furnace | 超级熔炉，支持 JEI / EMI 配方查看 |
| 神之矿机 God Miner | 范围挖掘机器，效率/时运/精准采集可调；六面输入输出面配置（输入抽入神之加速 / 输出推出产物与液体） |
| 神之资源机 God Resource | 资源复制机器（9 输入槽） |
| 神之掉落机 God Drop | 掉落物收集机器（9 输入槽）；同时是**刷怪蛋生产机**：不消耗刷怪蛋，按**原版战利品表**产出该生物被击杀时的**全部掉落物**（v5.1.6 起与原版同步：鸡 = 羽毛 + 生鸡肉，凋灵骷髅 = 煤炭 + 骨头（+头颅，概率同原版），不再是固定的一种物品），每种产物每周期 64 个，神之加速按并行倍率乘 |
| 神之附魔 God Enchant | 最高 **255 级**天神附魔 + 天神附魔台 God Heaven Enchant |
| 神之加速 God Accelerator | 放入神之系列机器提升并行数：每 1 个 16 倍，一组 64 个 = **1024 倍** |
| 荒辰移晷之杖 Wondrous Staff | **本模组唯一的杖**（v5.1.2 起：造化杖 / 太初洞见之杖已按用户要求移除，只剩这一个物品）。**逐字照抄 useless_stretcher（万象担架）扩展模组的杖**，它 `extends` 造化杖的基类，因此**完整保留造化杖的全部采集 / 时运 / 无敌 / 连锁能力**，另加自己的那套：**时间加速**（Shift+右键方块、AE 机器、避雷针、实体、日月天气，倍率 ×0/2/4/16…1024 循环）、**范围加速**（放置加速范围 + 白/黑名单标记 + 历史记录与回收）、**召唤**（勾选生物批量召唤）、战利品箱刷新、树叶掉落与打草彩蛋。界面：X 键加速配置面板、召唤选择界面、范围加速配置 / 历史 / 编辑三界面、右下角 HUD、范围预览与高亮渲染。<br>**工具形态**：☰ 模式轮盘里可切 扳手 / 螺丝刀 / 软锤 / 撬棍 / 铁锤（v5.1.2 起改为**写在同一个物品的数据组件上**，不再换物品）。**杖功能面板（J 键）**：吸星（吸取附近掉落物与经验，可调半径）/ 吸魂（把附近生物吸到面前）/ 杀戮光环（自动杀戮范围内敌对·友好·全部，可调半径）—— 这三项原属神之剑，v5.1.2 移到杖上
| 神之改造 God Change | 时间 / 天气调控 |
| 神之合成 God Craft | 高级合成台（配置菜单 + 模板菜单 + 均分输入） |
| 神之护甲 God Armor | 全套神之护甲（飞行 / 无敌 / 免疫负面 / 永不饥饿 / 火焰免疫 / 水下呼吸 / 夜视 / 熔岩可视 / Ad Astra 无限氧气），**12 项功能各有独立开关**，O 键打开开关界面，按玩家保存 |
| 神之不毁 God Unbreakable | 特殊合成配方 |
| 神之请神 God Invite | 右键生物赋予无限血量（保留受击反馈，可切换） |
| 神之吞噬 God Devourer | 虚空垃圾桶（方块 + 背包内快捷按钮，退出界面销毁） |
| 神之记录 God Record | 传送点系统（/setpoint、/point、U 键打开，可编辑/删除二次确认） |
| 无用维度 Useless Dimension | **逐字照抄 useless_mod 的维度子系统**：3 个世界 —— **奇数维度**（`teleport_block` 奇数维度传送方块）、**偶数维度**（偶数维度传送方块）、**三维度**（三维度传送方块）。方块潜行右键打开**维度配置界面**（层数 / 起始 Y / 填充·边框·中心方块 / 马路·多联两种平台模式 / 道路主体·边缘·中心线 / 边界间隔 / 基岩层开关与置底 / 顶视与剖面预览 / 配置导出导入），右键直接传送（POI 找已有传送点，找不到就在落点自动铺一块）。传送方块用木板 + 泥土合成，无用维度内永远晴天 |
| 时空永恒 Space Time Eternity | 放下后锁定世界时间与天气 |
| 生物覆灭 Creature Annihilation | 放下后半径 512 格内禁止生物自然生成 |
| 神之黑盒 God Black Box | 拾取过滤存储：白名单 / 黑名单切换，无堆叠上限，滚轮快捷开关 |
| 神之传输 God Transmitter | 无线 FE 充能（机器 + 玩家），四标签页 UI，跨维度连接 |
| 神之绑定器 God Binder | 右键机器绑定到神之传输充能（副手放置自动绑定） |
| 神之砍杀 God Slaughter | 范围击杀（开关/范围 0-1600/抢夺 0-1600/秒杀），掉落物直接进内部 27 格无限存储 |
| 神之吸收 God Absorber | 大范围吸收掉落物+经验（开关/范围 0-1600/存储经验面板/面配置/AE 并网） |
| 维度 | 虚空维度 + 双向传送门方块（**超平坦维度与「神之平坦」已在 v5.1.2 按用户要求整体删除**）|
| 皮肤玩偶 HoYooG Fumo | **照抄 ae2lt（AE2 闪电科技：重生）的 fumo 玩偶系统**：12 部件完整玩家模型 + 标准 64×64 皮肤 UV，贴图用的是本模组作者自己的皮肤。**右键**切换自转（6°/tick，状态写入方块实体存档）；**可戴在头上**（`Equipable`，头部槽），戴头上时同样绕 Y 轴自转；小碰撞箱、羊毛音效、可含水。合成：**8 个羊毛 + 中心 1 个铁锭** |
| 能量系统 | **创造能量立方 Creative Energy Cube**：无限 FE 能量源——六个面均以最大速率（∞ FE/t）向相邻机器输出；右键打开 GUI 充电 |
| 神之套装技能树 | 穿齐全套后按 **K** 或 **O** 打开配置界面（**6 个标签页：基础属性 / 特殊增幅 / 终极节点 / 机械共鸣 / 魔法增幅 / 套装功能**，会记住你上次停留的那一页）。圆角贴片 + 分类色条 + 等级进度条 + 滚动列表。共 **79 个技能**：基础属性 15 + 特殊增幅 15 + **魔法增幅 26** + **终极节点 16** + **机械共鸣 7**。统一公式：**最终属性 = 基础固定值总和 × (1 + 增幅百分比总和)**；奥术防护用 `减伤 = 防/(防+k)` 渐进公式（永不封顶）。**无技能点、无前置**：左键点行＝开/关（**关闭会保留等级**），等级条上拖动＝直接设定等级，滚轮悬停等级条＝±1 级，右键＝+1 级（Shift+右键 +10）。**光环（11 个）**：杀戮领域（范围脉动伤害，半径 20）/ 修罗杀域（伤害增幅）/ 疾攻之势（缩短脉动间隔）/ 回春妙手（范围治疗）/ 汲灵之环（给经验）/ 吸星大法（吸取掉落物与经验球）/ 定身神域（免击退免传送）/ 虚空诛灭（50 格内处决低血目标）/ 挪移术 + 搬运术（潜行+右键容器绑定，掉落物与背包物品直入容器）/ 净化领域（范围清负面）。**魔法增幅（26 个）**：给**铁魔法 / 新生魔艺 / Goety** 的属性加增幅（魔力上限、魔力回复、咏唱速度、冷却缩减、九系法术强度等）。采用**可选依赖**设计——按属性 ID 字符串从原版注册表解析，**零编译依赖**：装了对应 mod 才生效，没装自动跳过且绝不影响启动（加减 mod 需重启游戏）。**机械共鸣**：八个「共鸣」开关让**模拟玩家机器**继承对应效果（真玩家不受影响）。仅在穿齐全套时生效 |
| 血量数字显示 | 最大生命超过原版 20 点后，血条自动压缩为**最多 10 颗心**（只压缩显示，真实生命值不变），并在血条左侧显示**真实血量数字**（`当前 / 上限`，伤害吸收另起一行）；其他血条 mod 不受影响，未超阈值时零干预 |
| AE2 兼容 | 矿机/资源机/掉落机/砍杀/合成台/吸收 **6 台**会生产资源的机器可作为 AE 网格节点直接并网（线缆直连、占一个频道），产物自动输出进 AE 网络；每台 UI 有「AE」接入开关（只控制是否把产物推进 AE，不改变机器本身的并网状态）。**神之熔炉已在 v5.1.3 按用户要求删除全部 AE 功能：既不接线缆、也不能被无线并网（它连 `IN_WORLD_GRID_NODE_HOST` 能力都不再注册）** |

## 环境要求

- **JDK 21**（Minecraft 1.21.1 / NeoForge 21.1 要求；本机路径示例：`E:\MC\java\java21`）
- NeoForge `21.1.249` + NeoGradle（ModDevGradle 2.0.x，由 wrapper 自动分发）
- 构建期首次需联网解析 Minecraft / NeoForge 依赖（已缓存则无需重复下载）；JEI / EMI / AE2 集成依赖已预下载到 `libs/`，构建无需联网

## 构建

```powershell
# JDK 21 由 gradle.properties 的 org.gradle.java.home 指定，直接构建即可
.\gradlew build
# 产物：build\libs\godofthings-<mod_version>.jar（版本见 gradle.properties）
```

**构建成功后自动同步**：`build` 完成后 `deployJars` 任务会把 jar 自动复制到两个本机测试目录
（`E:\MC\modpacks\PCL\versions\1.21.1测试\mods` 与 `E:\MC\ALL\mods\自制\1.21.1`），
并清理旧版本号 jar（`keepOldVersions=false`）——改完代码 `gradlew build` 即可开游戏测试。

开发运行：

```powershell
.\gradlew runClient   # 启动开发客户端
.\gradlew runServer   # 启动开发服务端
.\gradlew runData     # 数据生成器（输出到 src/generated/resources）
```

## 版本号

版本号采用 `x.y.z` 三段式，规范见 [VERSIONING.md](VERSIONING.md)：

| 变更类型 | 版本号变化 |
|---|---|
| 修复 / 优化 | 末位 +1 |
| 末位到 10 自动进位 | 第二位 +1、末位归零 |
| 新增小物品 | 第二位 +1、末位归零 |
| 系统性新增 | 首位 +1、后两位归零 |

## 键位（神之工具）

默认分类：`key.category.godofthings.wand`，可在"选项 → 控制"中修改。

- 切换模式转轮 / 连锁挖掘 / 增强连锁 / 强制破坏 / 时运 / 精准采集
- 捕捉开关 / 一击必杀开关 / 无敌开关 / 触发强制挖掘

## 键位（其他）

- `U` 传送点界面
- `J` 手杖功能面板（吸星 / 吸魂 / 杀戮光环）
- `O` 神之套装功能开关界面（穿齐全套后按开关生效）

## 目录结构

```
src/main/java/com/godofthings/
  ├─ Godofthings.java        # 主类：全部 DeferredRegister 注册 + 配置 + 网络注册
  ├─ block/ block/entity/    # 方块与方块实体（熔炉/矿机/资源机/掉落机/附魔/合成/传送门…）
  ├─ item/                   # 物品（神之加速/神之工具/护甲…；杖相关在 beef/ 与 beef/stretcher/ 下）
  ├─ menu/ client/screen/    # 容器菜单与屏幕
  ├─ modes/                  # 神之工具模式系统
  ├─ network/                # 网络包（模式转轮 / 神之工具 / 手杖功能开关）
  ├─ recipe/ config/         # 配方与机器参数配置
  ├─ dimension/ energy/      # 虚空维度、创造能量立方
  ├─ handler/ emi/ jei/      # 集成（Ad Astra / EMI / JEI / AE2）
  └─ utils/mining/           # 挖掘策略（连锁 / 强制破坏…）
src/main/resources/
  ├─ assets/godofthings/     # blockstates / models / textures / lang
  ├─ data/godofthings/       # recipe / loot_table / advancement / dimension / worldgen…
  └─ META-INF/neoforge.mods.toml
```

## 许可

**All Rights Reserved**（见 `src/main/resources/META-INF/neoforge.mods.toml`）。

⚠️ **但本模组自 v3.0.0 起逐字照抄了若干开源项目的代码与素材，这些部分的许可独立于上面的 ARR**：
useless_mod（MIT）、GT New Horizons/PersonalSpace（LGPL-3.0）、useless_stretcher（MIT）、
JDTE（MIT）、AE2 Lightning Tech Reborn（源码 LGPL-3.0 / 模型素材 CC BY-NC-SA 3.0 ——
**该模型禁止商用**）。完整清单、署名与各自适用范围见仓库根目录的
[`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)；各许可全文在 `LICENSES/`，
并随 jar 一起打包在 `META-INF/` 下，再分发时请一并保留。

## 第三方代码与许可

本模组的**神之套装技能树**（技能数值、效果公式与界面布局）移植自
**[Zifeng Skill Tree / 子枫的百宝箱](https://github.com/ZeoNG129)**（作者 **zifeng**），
按 **MIT License** 使用：

> MIT License
> Copyright (c) 2026 zifeng
>
> Permission is hereby granted, free of charge, to any person obtaining a copy
> of this software and associated documentation files (the "Software"), to deal
> in the Software without restriction, including without limitation the rights
> to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
> copies of the Software, and to permit persons to whom the Software is
> furnished to do so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in all
> copies or substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
> IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
> FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
> AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
> LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
> SOFTWARE.

移植部分已按本项目需要重做（无技能点、无前置、挂载到神之套装而非全局技能树），
技能显示名中的原作者个人前缀（「子枫的」）已按要求去除。

> 其余逐字照抄的子系统（造化杖 / 无用维度 / 荒辰移晷之杖 / fumo 玩偶）的署名与许可条款，
> 见 [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)。