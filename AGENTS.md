# AGENTS.md — God of Things (NeoForge 1.21.1)

> 本文件供任何 AI agent / 新接手的开发者快速上手：读到它就等于知道本项目的构建、部署与约定。
> 版本号规范见 **VERSIONING.md**，项目介绍见 **README.md**。

## 环境硬约定
- MC 1.21.1 + NeoForge 21.1.249，Java 21（本机 `E:\MC\java\java21`，**不要下载 JDK**）
- 工作目录 `E:\MC\Mod\1.21.1\godofthings`；jar 产物在 `build\libs\godofthings-<版本>.jar`
- 版本号唯一来源：`gradle.properties` 的 `mod_version`；`META-INF/neoforge.mods.toml` 用 `${mod_version}` 占位符（processResources 已固定 `filteringCharset='UTF-8'`，防中文注释被 GBK 破坏）
- **版本号更替规范（x.x.x）**：修复/优化 → 末位 +1（到 10 自动进位，1.3.9 → 1.4.0）；新增小物品 → 第二位 +1（末位归零）；系统性新增 → 首位 +1（→ 2.0.0）。详见 VERSIONING.md。

## 测试 / CI
- **回归测试（共 19 条）**：`gradlew runGameTestServer`，三个测试类共用 `data/godofthings/structure/note_data.nbt` 那个 1×1×1 空结构：
  · `com.godofthings.gametest.NoteDataGameTest`（10 条）：便签整册的 NBT / 封包往返、多本与旧存档迁移、拖拽排序、自动更名、兜底 clamp、**子任务结构（连加 / 深度数量夹取 / 折叠状态往返）与升降级重排**，以及传送点 NBT；
  · `com.godofthings.gametest.DropLootRollerGameTest`（4 条）：神之掉落机按原版战利品表产出（鸡必掉生鸡肉、每种 64 个）、刷怪蛋入口、非生物实体返回空、装备过滤名单。
  · `com.godofthings.gametest.SpawnEggHelperGameTest`（5 条）：刷怪蛋识别（原版 / 模组自建 ENTITY_DATA 蛋 / 非蛋与坏数据拒绝）、神之怪蛋的复制产物保真、掉落机接受模组蛋的端到端。
  · **跑之前运行期必须有 AE2**（本模组有 7 个方块实体直接 implements AE2 接口）：`prepareGameTestMods` 任务会自动把 `libs/ae2-*.jar` 与 `guideme-*.jar` 拷进 `run-gametest/mods/`；本机 `fetch-libs.ps1 -IncludeTestMods` 会把 guideme 一起拉下来。
- **静态校验**：`./check-lang.ps1`，一次查全部 —— zh/en 键集双向一致、无空值、代码里所有 `translatable("字面量")` 都有键、运行时拼接前缀能解析、**注册的方块/物品/实体 ↔ blockstate / item 模型 / 语言键 全覆盖**、**每个物品/方块都有手册条目**（`manual.godofthings.<id>`，系统条目另验标题+正文）、反向的孤儿 blockstate、以及**仓库内 .ps1 必须纯 ASCII**。CI 跑的就是它。
- **CI**：`.github/workflows/build.yml`（fetch-libs -IncludeTestMods → check-lang → `gradlew build` → `gradlew runGameTestServer` → 上传 jar）。
- **已知运行期硬依赖（不是"没装就跳过"）**：`GodMiner` / `GodResource` / `GodDrop` / `GodSpawnEgg` / `GodCraft` / `GodSlaughter` / `GodAbsorber` 这几个方块实体**类声明上直接 `implements` 了 AE2 的 `IGridConnectedBlockEntity`**，所以运行期必须有 AE2，否则模组在**加载阶段**就 `NoClassDefFoundError` 崩掉（`neoforge.mods.toml` 里已如实把 AE2 声明为 required，缺依赖时会给出明确提示而不是崩溃堆栈）。要真做成可选，得把 AE2 那段抽到单独的类里（用接口桥接），现在没做。

## 构建 → 自动部署（重要约定）
- **`gradlew build` 成功后自动同步 jar 到两个本机测试目录**（build.gradle 的 `deployJars` 任务，build 依赖它，编译失败不会部署）：
  1. `E:\MC\modpacks\PCL\versions\1.21.1测试\mods` — PCL 测试实例
  2. `E:\MC\ALL\mods\自制\1.21.1` — ALL 模组仓库
- 部署时自动清理目标目录里旧版本号的 `godofthings-*.jar`，防止双 jar 重复模组崩溃
- 改完代码 → `gradlew build` → 直接开这两个实例的游戏测试

## 网络 / 代理（重要约定）
- **代理软件统一用 `D:\AAA\NBVPN`（主程序 `牛逼.exe`，mihomo 内核 VPN 客户端，XBoard 面板）**。需要代理时（如 `git push` / GitHub release 连不上）先启动它，**不要再改用 Clash Verge**。
- **启动 ≠ 连接**：只启动 NBVPN 时监听 `127.0.0.1:9589`（非代理，不能用来 push）。必须在 NBVPN 界面里**点击「连接」按钮**，连接成功后才会监听 **`127.0.0.1:7890`（mihomo mixed-port 代理端口，实测已确认）**。AI 启动 NBVPN 后若发现 7890 未监听，应提示用户点击连接按钮（无法自动化点击 GUI）。
- 判断是否已连接（7890 是否监听）：
  ```powershell
  (Test-NetConnection 127.0.0.1 -Port 7890 -WarningAction SilentlyContinue).TcpTestSucceeded
  ```
- push 走 7890 代理：
  ```powershell
  git -c http.proxy=http://127.0.0.1:7890 -c https.proxy=http://127.0.0.1:7890 push origin 1.21.1
  ```
- `release.ps1` 已自动探测 `@(7890, 7897)`（7890 命中 NBVPN 连接后端口）。
- git 全局代理历史值是 `http://127.0.0.1:7890`（与 NBVPN 连接后端口一致）。

## 发版标准流程（4 步）
1. 改 `gradle.properties` 的 `mod_version` + 在 `VERSIONING.md` 加历史
2. `gradlew build`（自动部署）
3. `git add -A && git commit -m "中文一句话" && git push origin 1.21.1`
4. **只有首位大版本变化（1.x → 2.x）才新建 GitHub Release（含新 tag）**；其余所有版本一律把 `godofthings-<版本>.jar` 作为**额外 asset 追加到该首位大版本已有的 release** 下（REST API：`POST https://uploads.github.com/repos/ZeoNG129/Godofthings/releases/{release_id}/assets?name=godofthings-<版本>.jar`，**不新建 release、不新建 tag**），并在 release notes 里追加该版本一行说明、更新末尾的「包含 jar」清单——即每个首位大版本一个 release，其下能下到该大版本所有小版本 jar

## 项目约定
- 语言文件 `zh_cn.json` 与 `en_us.json` 键集必须双向一致
- **README「内容一览」提交更新时就要同步更新**（新增/删除物品、方块、功能都要改那张表格）
- **新功能一律独立成包 / 独立文件**：`Godofthings.java` 只做注册；不要再往已经很大的类里塞新东西（`ModeWheelScreen` 1500+ 行、`EndlessBeafItem` 1400+、`ConfigManager` 1100+、`GodMinerBlockEntity` / `GodCraftBlockEntity` 1000 行左右，且大多是照抄上游的）。参考 `note/` 包（神之便签）的做法：数据 / 网络 / 界面 / 指令各自成文件
- **对「照抄上游」代码的本地改动，用 `[本地改动]` 注释标出来**，并在 VERSIONING.md 记一笔：上游更新后继续移植是既定做法（用户明确会选择继续移植），标注了才知道哪些是「移植时要带上」的。当前代码里暂无此类标注（原举例的 `RenderEntityScan` 已随权杖在 v5.8.0 一并删除）。
- **语言键改动后用脚本自检**：`check-lang.ps1`（根目录）会校验 zh/en 键集双向一致、且代码里所有 `translatable("字面量")` 都能在语言文件里找到
- **新增物品 / 方块必须补一条手册条目**：`manual.godofthings.<注册名>`（`check-lang.ps1` 会校验，漏了直接报错）。
  手册条目是**从注册表自动生成**的（`manual/ManualCatalog.java`），所以只要补语言键就自动出现在手册里；
  新增「系统」条目则往 `ManualCatalog.SYSTEM_IDS` 里加 id，并补 `manual.godofthings.system.<id>` 与 `.title` 两个键。
  手册入口：`P` 键（`WandKeyBindings.OPEN_MANUAL_KEY`）/ 神之手册物品 / `/godofthings manual`。
- git 分支：本项目用 `1.21.1` 分支（GitHub 仓库默认分支已设为 `1.21.1`）；1.20.1 Forge 版在 `main` 分支（本地 `E:\MC\Mod\1.20.1\Godofthings`），两仓库 remote 指向同一 GitHub 仓库 `ZeoNG129/Godofthings`
- GitHub Release 按**首位大版本**归类（1.x / 2.x / 3.x / 4.x / 5.x …各一个 release，tag 取该大版本下的一个具体版本，如 v2.6.0）；1.20.1 保留 v2.0.5；每个大版本 release 下挂该大版本**所有小版本 jar**（新小版本 jar 追加为 asset，不删除旧 asset）
- **当前同步状态（2026-10-02 核对，已全部补齐）**：远程 `origin/1.21.1` 与本地**一致**，tag **v3.0.1 / v4.0.2 / v5.1.7** 已推送；**模组 release 6 个**（另有一个 `libs-1.21.1` 依赖 release，标为 pre-release、不属于版本线）：
  v1.9.0（1.x，24 个 jar）、v2.0.5（1.20.1 Forge）、v2.4.1（2.x，20 个 jar，已补 2.20.5）、**v3.0.1（3.x，2 个 jar）**、**v4.0.2（4.x，3 个 jar）**、**v5.1.7（5.x，25 个 jar，已补 5.1.8 / 5.2.0 / 5.2.1 / 5.3.0 / 5.4.0 / 5.4.1 / 5.4.2 / 5.5.0 / 5.5.1 / 5.5.2 / 5.6.0 / 5.6.1 / 5.6.2 / 5.7.0 / 5.8.0 / 5.9.0）**。
  · **发小版本 / 第二位变化**：build → commit → `git push origin 1.21.1` → **不新建 release、不新建 tag**，只把 jar 追加到该首位大版本已有的 release 下并补 release notes（例如 5.1.8 / 5.2.0 / 5.2.1 都进 v5.1.7 那个 release）。
  · 现成脚本：`.ref/release/publish-majors.ps1`（`git credential fill` 取 token + 自动探测 7890 代理 + 建 tag/release/上传资产）。仓库内所有 `.ps1` **一律保持纯 ASCII**（`check-lang.ps1` 会校验这条）：UTF-8 无 BOM 且含中文的脚本被 PowerShell 5.1 读错编码时，注释的尾字节会被当成续行符、**把下一行代码吞掉**（`fetch-libs.ps1` 真踩过一次，表现为某个分支静默失效）。
  · **离线构建依赖**：`libs/` 下 19 个第三方模组 jar（约 52MB）**不进版本库**（仓库瘦身 + 第三方 jar 不再随源码分发）；克隆后跑根目录的 `fetch-libs.ps1`，从本仓库的 `libs-1.21.1` release 一次性拉回来（该 release 标为 pre-release，不属于模组版本线）。
- 提交信息用中文一句话
- 已知非阻塞警告：约 20-30 条 `@EventBusSubscriber bus()` [removal] 警告（`RegisterCapabilitiesEvent`/`RegisterPayloadHandlersEvent` 是 IModBusEvent 必须保留 `bus=Bus.MOD`，NeoForge 21.1 过渡标记，无替代 API）
