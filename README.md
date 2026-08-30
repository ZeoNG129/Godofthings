# God of Things（万物之神）

一个面向 **Minecraft 1.20.1 + Forge** 的集合型模组，全部内容为原创。

> 神之熔炉、神之矿机、天神附魔、造化垂青之杖…… 一整套"神之"工具与机器。

## 内容一览

| 模块 | 说明 |
|------|------|
| 神之熔炉 God Furnace | 超级熔炉，支持 JEI / EMI 配方查看 |
| 神之矿机 God Miner | 范围挖掘机器，效率/时运/精准采集可调（`godofthings-machines.toml`） |
| 神之资源机 God Resource | 资源复制机器 |
| 神之掉落机 God Drop | 掉落物收集机器 |
| 神之附魔 God Enchant | 最高 **255 级**天神附魔 + 天神附魔台 God Heaven Enchant |
| 附魔卷轴 Enchant Scroll | 附魔抽取 / 转移道具 |
| 造化垂青之杖 God Favor Wand | 多模式工具杖：连锁挖掘 / 强制破坏 / 时运 / 精准采集 / 一击必杀 / 无敌 / 捕捉；模式转盘（默认切换键见下）；AE2 存储优先（GridLinkable）；扳手 / 螺丝刀 / 木槌 / 撬棍 / 锤 五种变体形态 |
| 神之改造 God Change | 时间 / 天气调控 |
| 神之合成 God Craft | 高级合成台（配置菜单 + 模板菜单） |
| 神之护甲 God Armor | 全套神之护甲 |
| 神之不毁 God Unbreakable | 特殊合成配方 |
| 维度 | 超平坦维度 + 虚空维度 + 双向传送门方块 |
| 能量系统 | **创造能量立方 Creative Energy Cube**：无限 FE 能量源——六个面均以最大速率（∞ FE/t）向相邻机器输出；右键打开 GUI，单个充能格，可充电物品放入即以最大速率充能。创造物品栏获取（无合成配方） |

## 环境要求

- **JDK 17**（Minecraft 1.20.1 / Forge 47.x 要求；本机路径示例：`E:\MC\java\java17`）
- Gradle 8.8（由 wrapper 自动分发，无需单独安装）
- 构建期需要网络：首次构建会解析 ForgeGradle / Minecraft 依赖（几百 MB，已缓存则无需重复下载）

## 构建

```powershell
# 设置 JDK 17 后构建
$env:JAVA_HOME = 'E:\MC\java\java17'
.\gradlew build
# 产物：build\libs\godofthings-2.0.0.jar
```

**构建成功后自动同步**：`build` 完成后 `deployJars` 任务会把 jar 自动复制到两个本机测试目录
（`E:\MC\modpacks\PCL\versions\1.20.1测试\mods` 与 `E:\MC\ALL\mods\自制\1.20.1`），
并清理旧版本 jar——改完代码 `gradlew build` 即可开游戏测试，无需手动拷贝。

开发运行：

```powershell
.\gradlew runClient   # 启动开发客户端
.\gradlew runServer   # 启动开发服务端
.\gradlew runData     # 数据生成器（输出到 src/generated/resources）
```

### 网络问题（代理 / 镜像）

构建依赖 Maven 仓库。若本机代理（`127.0.0.1:7890`）未启动，可用仓库内置的腾讯镜像 init 脚本绕过：

```powershell
.\gradlew build -I fix.init.gradle
```

`fix.init.gradle` 会追加 `mirrors.cloud.tencent.com` 公共镜像（直连，不经过代理）。

## 键位（造化垂青之杖）

默认分类：`key.category.godofthings.wand`，可在"选项 → 控制"中修改。

- 切换模式转盘 / 连锁挖掘 / 增强连锁 / 强制破坏 / 时运 / 精准采集
- 捕捉开关 / 一击必杀开关 / 无敌开关 / 触发强制挖掘

## 目录结构

```
src/main/java/com/godofthings/
  ├─ Godofthings.java        # 主类：全部 DeferredRegister 注册 + 配置 + 网络注册
  ├─ block/ block/entity/    # 方块与方块实体（熔炉/矿机/资源机/掉落机/传送门…）
  ├─ item/                   # 物品（造化垂青之杖、护甲、卷轴…）
  ├─ menu/ client/screen/    # 容器菜单与屏幕
  ├─ modes/                  # 造化垂青之杖模式系统
  ├─ network/                # 网络包（模式转盘 / 造化垂青之杖）
  ├─ recipe/ config/         # 配方（神之不毁等）与机器参数配置
  ├─ dimension/ energy/      # 超平坦 / 虚空维度与地形、创造能量立方
  ├─ handler/ emi/ jei/      # 集成（Ad Astra / EMI / JEI / AE2）
  └─ utils/
src/main/resources/
  ├─ assets/godofthings/     # blockstates / models / textures / lang
  └─ data/godofthings/       # recipes / loot_tables / advancements / dimension…
```

## 许可

**All Rights Reserved**（见 `src/main/resources/META-INF/mods.toml`）。

开发约定与架构细节见 [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md)；AI agent / 新维护者快速上手见 [AGENTS.md](AGENTS.md)。
