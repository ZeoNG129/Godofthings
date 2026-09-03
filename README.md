# God of Things（万物之神）

一个面向 **Minecraft 1.21.1 + NeoForge** 的集合型模组，全部内容为原创。

> 神之熔炉、神之矿机、天神附魔、神之工具…… 一整套"神之"工具与机器。

## 内容一览

| 模块 | 说明 |
|------|------|
| 神之熔炉 God Furnace | 超级熔炉，支持 JEI / EMI 配方查看 |
| 神之矿机 God Miner | 范围挖掘机器，效率/时运/精准采集可调 |
| 神之资源机 God Resource | 资源复制机器（9 输入槽） |
| 神之掉落机 God Drop | 掉落物收集机器（9 输入槽） |
| 神之附魔 God Enchant | 最高 **255 级**天神附魔 + 天神附魔台 God Heaven Enchant |
| 神之剑 God Sword | 秒杀 + 斩首 / 捕捉 / 抢劫 / 吸星 / 吸魂 / 杀戮光环（可单独或组合开启） |
| 神之炮 God Cannon | 电磁炮：左键贯穿光束（128 格）、右键三层蓄力范围炮（半径 4/7/12 格） |
| 神之加速 God Accelerator | 放入神之系列机器提升并行数：每 1 个 16 倍，一组 64 个 = **1024 倍** |
| 神之工具 God Favor Wand | 多模式工具杖：连锁挖掘 / 强制破坏 / 时运 / 精准采集 / 一击必杀 / 无敌 / 捕捉；模式转盘；AE2 存储优先；扳手 / 螺丝刀 / 木槌 / 撬棍 / 锤 五种工具形态 |
| 神之改造 God Change | 时间 / 天气调控 |
| 神之合成 God Craft | 高级合成台（配置菜单 + 模板菜单 + 均分输入） |
| 神之护甲 God Armor | 全套神之护甲（飞行 + 挖掘加速） |
| 神之不毁 God Unbreakable | 特殊合成配方 |
| 神之请神 God Invite | 右键生物赋予无限血量（保留受击反馈，可切换） |
| 神之吞噬 God Devourer | 虚空垃圾桶（方块 + 背包内快捷按钮，退出界面销毁） |
| 神之记录 God Record | 传送点系统（/setpoint、/point、U 键打开，可编辑/删除二次确认） |
| 时空永恒 Space Time Eternity | 放下后锁定世界时间与天气 |
| 生物覆灭 Creature Annihilation | 放下后半径 512 格内禁止生物自然生成 |
| 神之黑盒 God Black Box | 拾取过滤存储：白名单 / 黑名单切换，无堆叠上限，滚轮快捷开关 |
| 神之传输 God Transmitter | 无线 FE 充能（机器 + 玩家），四标签页 UI，跨维度连接 |
| 神之绑定器 God Binder | 右键机器绑定到神之传输充能（副手放置自动绑定） |
| 神之砍杀 God Slaughter | 范围击杀（开关/范围 0-300/抢夺 0-300/秒杀），掉落物直接进内部 27 格无限存储 |
| 维度 | 超平坦维度 + 虚空维度 + 双向传送门方块 |
| 能量系统 | **创造能量立方 Creative Energy Cube**：无限 FE 能量源——六个面均以最大速率（∞ FE/t）向相邻机器输出；右键打开 GUI 充电 |
| AE2 兼容 | 熔炉/矿机/资源机/掉落机/砍杀/合成台 6 台会生产资源的机器可作为 AE 网格节点直接并网（线缆直连、占一个频道），产物自动输出进 AE 网络，每台 UI 有「AE」接入开关 |

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
