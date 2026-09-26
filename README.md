# God of Things（万物之神）

一个面向 **Minecraft 1.21.1 + NeoForge** 的集合型模组，全部内容为原创。

> 神之熔炉、神之矿机、天神附魔、神之工具…… 一整套"神之"工具与机器。

## 内容一览

| 模块 | 说明 |
|------|------|
| 神之熔炉 God Furnace | 超级熔炉，支持 JEI / EMI 配方查看 |
| 神之矿机 God Miner | 范围挖掘机器，效率/时运/精准采集可调；六面输入输出面配置（输入抽入神之加速 / 输出推出产物与液体） |
| 神之资源机 God Resource | 资源复制机器（9 输入槽） |
| 神之掉落机 God Drop | 掉落物收集机器（9 输入槽） |
| 神之附魔 God Enchant | 最高 **255 级**天神附魔 + 天神附魔台 God Heaven Enchant |
| 神之剑 God Sword | 秒杀 + 斩首 / 捕捉 / 抢劫 / 吸星 / 吸魂 / 杀戮光环（可单独或组合开启） |
| 神之炮 God Cannon | 电磁炮：左键贯穿光束（128 格）、右键三层蓄力范围炮（半径 4/7/12 格） |
| 神之加速 God Accelerator | 放入神之系列机器提升并行数：每 1 个 16 倍，一组 64 个 = **1024 倍** |
| 神之工具 God Favor Wand | 多模式工具杖：连锁挖掘 / 强制破坏 / 时运 / 精准采集 / 一击必杀 / 无敌 / 捕捉；模式转盘；AE2 存储优先；扳手 / 螺丝刀 / 木槌 / 撬棍 / 锤 五种工具形态 |
| 神之改造 God Change | 时间 / 天气调控 |
| 神之合成 God Craft | 高级合成台（配置菜单 + 模板菜单 + 均分输入） |
| 神之护甲 God Armor | 全套神之护甲（飞行 / 无敌 / 免疫负面 / 永不饥饿 / 火焰免疫 / 水下呼吸 / 夜视 / 熔岩可视 / Ad Astra 无限氧气），**12 项功能各有独立开关**，O 键打开开关界面，按玩家保存 |
| 神之不毁 God Unbreakable | 特殊合成配方 |
| 神之请神 God Invite | 右键生物赋予无限血量（保留受击反馈，可切换） |
| 神之吞噬 God Devourer | 虚空垃圾桶（方块 + 背包内快捷按钮，退出界面销毁） |
| 神之记录 God Record | 传送点系统（/setpoint、/point、U 键打开，可编辑/删除二次确认） |
| 时空永恒 Space Time Eternity | 放下后锁定世界时间与天气 |
| 生物覆灭 Creature Annihilation | 放下后半径 512 格内禁止生物自然生成 |
| 神之黑盒 God Black Box | 拾取过滤存储：白名单 / 黑名单切换，无堆叠上限，滚轮快捷开关 |
| 神之传输 God Transmitter | 无线 FE 充能（机器 + 玩家），四标签页 UI，跨维度连接 |
| 神之绑定器 God Binder | 右键机器绑定到神之传输充能（副手放置自动绑定） |
| 神之砍杀 God Slaughter | 范围击杀（开关/范围 0-1600/抢夺 0-1600/秒杀），掉落物直接进内部 27 格无限存储 |
| 神之吸收 God Absorber | 大范围吸收掉落物+经验（开关/范围 0-1600/存储经验面板/面配置/AE 并网） |
| 维度 | 超平坦维度 + 虚空维度 + 双向传送门方块 |
| 能量系统 | **创造能量立方 Creative Energy Cube**：无限 FE 能量源——六个面均以最大速率（∞ FE/t）向相邻机器输出；右键打开 GUI 充电 |
| 神之套装技能树 | 穿齐全套后按 **K** 或 **O** 打开配置界面（**8 个标签页：基础属性 / 特殊增幅 / 终极节点 / 特殊被动 / 光环 / 机械共鸣 / 魔法增幅 / 套装功能**，会记住你上次停留的那一页）。圆角贴片 + 分类色条 + 等级进度条 + 滚动列表。共 **118 个技能**：基础属性 15 + 特殊增幅 15 + **魔法增幅 26** + **终极节点 21** + **特殊被动 22** + **光环 11** + **机械共鸣 8**。统一公式：**最终属性 = 基础固定值总和 × (1 + 增幅百分比总和)**；奥术防护用 `减伤 = 防/(防+k)` 渐进公式（永不封顶）。**无技能点、无前置**：左键点行＝开/关（**关闭会保留等级**），等级条上拖动＝直接设定等级，滚轮悬停等级条＝±1 级，右键＝+1 级（Shift+右键 +10）。**光环（11 个）**：杀戮领域（范围脉动伤害，半径 20）/ 修罗杀域（伤害增幅）/ 疾攻之势（缩短脉动间隔）/ 回春妙手（范围治疗）/ 汲灵之环（给经验）/ 吸星大法（吸取掉落物与经验球）/ 定身神域（免击退免传送）/ 虚空诛灭（50 格内处决低血目标）/ 挪移术 + 搬运术（潜行+右键容器绑定，掉落物与背包物品直入容器）/ 净化领域（范围清负面）。**魔法增幅（26 个）**：给**铁魔法 / 新生魔艺 / Goety** 的属性加增幅（魔力上限、魔力回复、咏唱速度、冷却缩减、九系法术强度等）。采用**可选依赖**设计——按属性 ID 字符串从原版注册表解析，**零编译依赖**：装了对应 mod 才生效，没装自动跳过且绝不影响启动（加减 mod 需重启游戏）。**机械共鸣**：八个「共鸣」开关让**模拟玩家机器**继承对应效果（真玩家不受影响）。仅在穿齐全套时生效 |
| 血量数字显示 | 最大生命超过原版 20 点后，血条自动压缩为**最多 10 颗心**（只压缩显示，真实生命值不变），并在血条左侧显示**真实血量数字**（`当前 / 上限`，伤害吸收另起一行）；其他血条 mod 不受影响，未超阈值时零干预 |
| AE2 兼容 | 熔炉/矿机/资源机/掉落机/砍杀/合成台/吸收 **7 台**会生产资源的机器可作为 AE 网格节点直接并网（线缆直连、占一个频道），产物自动输出进 AE 网络；每台 UI 有「AE」接入开关（只控制是否把产物推进 AE，不改变机器本身的并网状态） |

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
- `J` 神之剑功能面板
- `O` 神之套装功能开关界面（穿齐全套后按开关生效）

## 目录结构

```
src/main/java/com/godofthings/
  ├─ Godofthings.java        # 主类：全部 DeferredRegister 注册 + 配置 + 网络注册
  ├─ block/ block/entity/    # 方块与方块实体（熔炉/矿机/资源机/掉落机/附魔/合成/传送门…）
  ├─ item/                   # 物品（神之剑/神之炮/神之加速/神之工具/护甲…）
  ├─ menu/ client/screen/    # 容器菜单与屏幕
  ├─ modes/                  # 神之工具模式系统
  ├─ network/                # 网络包（模式转轮 / 神之工具 / 神之炮光束）
  ├─ recipe/ config/         # 配方与机器参数配置
  ├─ dimension/ energy/      # 超平坦 / 虚空维度、创造能量立方
  ├─ handler/ emi/ jei/      # 集成（Ad Astra / EMI / JEI / AE2）
  └─ utils/mining/           # 挖掘策略（连锁 / 强制破坏…）
src/main/resources/
  ├─ assets/godofthings/     # blockstates / models / textures / lang
  ├─ data/godofthings/       # recipe / loot_table / advancement / dimension / worldgen…
  └─ META-INF/neoforge.mods.toml
```

## 许可

**All Rights Reserved**（见 `src/main/resources/META-INF/neoforge.mods.toml`）。

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