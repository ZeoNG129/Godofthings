# AGENTS.md — God of Things (Forge 1.20.1)

> 本文件供任何 AI agent / 新接手的开发者快速上手：读到它就等于知道本项目的构建、部署与约定。
> 详细开发文档见 **docs/DEVELOPMENT.md**，项目介绍见 **README.md**。

## 环境硬约定
- MC 1.20.1 + Forge 47.4.20，Java 17（本机 `E:\MC\java\java17`，**不要下载 JDK**）
- 工作目录 `E:\MC\Mod\1.20.1\Godofthings`；jar 产物在 `build\libs\godofthings-<版本>.jar`
- 版本号唯一来源：`gradle.properties` 的 `mod_version`；`mods.toml` 用 `${mod_version}` 占位符（由 processResources 展开）
- **版本号更替规范（x.x.x）**：修复/优化 → 末位 +1，到 10 自动进位（1.3.9 → 1.4.0）；新增小物品 → 第二位 +1（末位归零）；系统性新增 → 首位 +1（→ 2.0.0）

## 构建 → 自动部署（重要约定）
- **`gradlew build` 成功后自动同步 jar 到两个本机测试目录**（由 build.gradle 的 `deployJars` 任务实现，build 依赖它，编译失败不会部署）：
  1. `E:\MC\modpacks\PCL\versions\1.20.1测试\mods` — PCL 测试实例
  2. `E:\MC\ALL\mods\自制\1.20.1` — ALL 模组仓库
- 部署时自动清理目标目录里旧版本号的 `godofthings-*.jar`，防止双 jar 重复模组崩溃
- 所以：**改完代码 → `gradlew build` → 直接开这两个实例的游戏测试**，无需手动拷贝

## 构建环境注意
- 用户级 `C:\Users\zuoh1\.gradle\gradle.properties` 配置了代理 127.0.0.1:7890；**代理没开时构建会 Connection refused**
- 处理方案（按优先级）：
  1. 先试 `gradlew build --offline`（依赖缓存通常已完整）
  2. 仍失败：临时注释该文件里 4 行 `systemProp.*` 代理 → 直连构建 → 构建完**字节级恢复**（模板见 docs/DEVELOPMENT.md「构建环境」）
  3. 或用仓库内 `fix.init.gradle` 注入腾讯镜像：`gradlew build -I fix.init.gradle`

## 项目约定
- **只保留原创内容**：勿再引入第三方模组移植包
- 语言文件：`zh_cn.json` 与 `en_us.json` 键集必须双向一致（校验命令见 docs/DEVELOPMENT.md）
- 新方块/物品走 DeferredRegister + 标准 assets/data 结构；能量类内容在 `com.godofthings.energy`（当前为创造能量立方：六面无限 FE 输出 + 4 充能格 GUI）
- git 已启用（此前无版本控制）；改动请保持 worktree 干净、提交信息用中文一句话
