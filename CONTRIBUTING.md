# 贡献指南

感谢关注 **God of Things（万物之神）**！本项目目前以作者自主开发为主，但欢迎 Issue 与 PR。

## 提交前检查

1. 分支：一律基于 **`1.21.1`**（仓库默认分支）；
2. `./check-lang.ps1` 必须全绿（zh/en 键集一致、注册覆盖、标签、手册条目）；
3. `gradlew runGameTestServer` 全部 GameTest 通过；
4. `gradlew build` 成功。

## 约定

- 提交信息：**中文一句话**，说清「做了什么 / 为什么」；
- 版本号遵循 [VERSIONING.md](VERSIONING.md)（修复末位 +1、新增小物品第二位 +1、系统性新增首位 +1）；
- 新增物品 / 方块必须补手册条目与双语语言键（`check-lang.ps1` 会挡下遗漏）；
- 新功能独立成包 / 文件，`Godofthings.java` 只做注册；
- 环境与内部规范详见 [AGENTS.md](AGENTS.md)。

## Issue

- Bug 请用模板（版本 / 环境 / 复现步骤 / 日志）；
- 功能建议欢迎描述使用场景。
