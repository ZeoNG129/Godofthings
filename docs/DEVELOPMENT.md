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

版本命名规范（见 `gradle.properties` 注释）：**小版本 1.0.x、中版本 1.x.0、大版本 x.0.0**。

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
- 伤害类型等自定义注册用数据包：`data/godofthings/damage_type/beef_tool.json`（造化垂青之杖伤害，配套 `WandDamageTypes.BEEF_TOOL`）。

## 4. 版本号与 mods.toml

- `src/main/resources/META-INF/mods.toml` 的 `version` 字段写 **`${mod_version}` 占位符**，
  由 `build.gradle` 的 `processResources`（`filesMatching(['META-INF/mods.toml','pack.mcmeta'])`）从
  `gradle.properties` 的 `mod_version` 展开成实际版本；
- **升级版本只改 `gradle.properties` 一处**，不要把硬编码版本写回 mods.toml。

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

- **v1.3.0 起本模组为纯原创内容**：曾临时内置的 5 个第三方模组移植包
  （ConstructionWand / Torcherino / Torchmaster / ToolBelt / Just Dire Things，共 239 个类）
  已整体移除，连同其资源、配方、语言键与 9 个跨模组联动依赖（Botania/Curios/L2/Tom's Storage/
  Refined Storage/Beyond Dimensions/ProjectE）一并清理。相关方块/物品不复存在，
  存档中已放置的旧方块会显示为虚空方块（贴图缺失），属预期行为。
- **能量系统重构（继 1.3.0 之后）**：删除 `generator/` 包（能量发电机 EnergyGenerator +
  能量中继 EnergyRelay，12 个类）及其全部资源/配方/语言键，替换为 `energy/` 包的
  **创造能量立方**（`CreativeEnergyCubeBlock/Entity/Menu/Screen`）：
  - 六面 FE 能量源，`extractEnergy` 无条件满足（创造行为），输出恒为最大速率；
  - 每 tick 向相邻接收端推送 `Integer.MAX_VALUE` FE/面，并为 GUI 内 4 个充能格物品充满电；
  - 无合成配方（创造物品栏获取），`MachinesConfig` 无能量配置项；
  - 注意：`AdAstraCompat` 是氧气事件兼容，与能量系统无关，未受影响。
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
