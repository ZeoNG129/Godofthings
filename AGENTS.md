# AGENTS.md — God of Things (NeoForge 1.21.1)

> 本文件供任何 AI agent / 新接手的开发者快速上手：读到它就等于知道本项目的构建、部署与约定。
> 版本号规范见 **VERSIONING.md**，项目介绍见 **README.md**。

## 环境硬约定
- MC 1.21.1 + NeoForge 21.1.249，Java 21（本机 `E:\MC\java\java21`，**不要下载 JDK**）
- 工作目录 `E:\MC\Mod\1.21.1\godofthings`；jar 产物在 `build\libs\godofthings-<版本>.jar`
- 版本号唯一来源：`gradle.properties` 的 `mod_version`；`META-INF/neoforge.mods.toml` 用 `${mod_version}` 占位符（processResources 已固定 `filteringCharset='UTF-8'`，防中文注释被 GBK 破坏）
- **版本号更替规范（x.x.x）**：修复/优化 → 末位 +1（到 10 自动进位，1.3.9 → 1.4.0）；新增小物品 → 第二位 +1（末位归零）；系统性新增 → 首位 +1（→ 2.0.0）。详见 VERSIONING.md。

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
4. **大版本（第二位/首位变化）才新建 GitHub Release**；小版本（末位变化，修复/优化）build 后把 `godofthings-<版本>.jar` 作为**额外 asset 上传到归属大版本 release 下**（用 REST API：`POST https://uploads.github.com/repos/ZeoNG129/Godofthings/releases/{大版本release_id}/assets?name=godofthings-<版本>.jar`，不新建 release），并在 release notes 里追加该小版本一行说明——即每个大版本一个 release，其下能下到该大版本所有小版本 jar

## 项目约定
- 语言文件 `zh_cn.json` 与 `en_us.json` 键集必须双向一致
- **README「内容一览」提交更新时就要同步更新**（新增/删除物品、方块、功能都要改那张表格）
- git 分支：本项目用 `1.21.1` 分支（GitHub 仓库默认分支已设为 `1.21.1`）；1.20.1 Forge 版在 `main` 分支（本地 `E:\MC\Mod\1.20.1\Godofthings`），两仓库 remote 指向同一 GitHub 仓库 `ZeoNG129/Godofthings`
- GitHub Release 按**首位大版本**归类（1.x / 2.x / 3.x / 4.x / 5.x …各一个 release，tag 取该大版本下的一个具体版本，如 v2.6.0）；1.20.1 保留 v2.0.5；每个大版本 release 下挂该大版本**所有小版本 jar**（新小版本 jar 追加为 asset，不删除旧 asset）
- **当前同步状态（2026-10-02 核对，已全部补齐）**：远程 `origin/1.21.1` 与本地**一致**，tag **v3.0.1 / v4.0.2 / v5.1.7** 已推送；GitHub Releases 共 **6 个**：
  v1.9.0（1.x，24 个 jar）、v2.0.5（1.20.1 Forge）、v2.4.1（2.x，20 个 jar，已补 2.20.5）、**v3.0.1（3.x，2 个 jar）**、**v4.0.2（4.x，3 个 jar）**、**v5.1.7（5.x，12 个 jar，已补 5.1.8 / 5.2.0 / 5.2.1）**。
  · **发小版本**（末位变化）：build → commit → `git push origin 1.21.1` → 建 tag 并把 jar **追加到该首位大版本已有的 release** 下（不新建 release）；例如 5.1.8 传到 v5.1.7 那个 release 里。新增首位大版本才建新 release。
  · 现成脚本：`.ref/release/publish-majors.ps1`（`git credential fill` 取 token + 自动探测 7890 代理 + 建 tag/release/上传资产，ASCII-only 免受 PS 5.1 中文解析问题影响）。注意 `release.ps1` 是**无 BOM 的 UTF-8 且含中文注释**，PS 5.1 直接执行有解析风险，建议用 pwsh 或上面的脚本。
  · `libs/` 下 19 个预下载依赖 jar 在版本库里（约 45MB，供离线构建），首次推送时有一次较大的上传。
- 提交信息用中文一句话
- 已知非阻塞警告：约 20-30 条 `@EventBusSubscriber bus()` [removal] 警告（`RegisterCapabilitiesEvent`/`RegisterPayloadHandlersEvent` 是 IModBusEvent 必须保留 `bus=Bus.MOD`，NeoForge 21.1 过渡标记，无替代 API）
