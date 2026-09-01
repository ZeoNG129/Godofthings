# 版本号更替规范

God of Things 模组版本号采用 `x.y.z` 三段式，由 `gradle.properties` 的 `mod_version` 决定。

## 规则

| 变更类型 | 版本号变化 | 示例 |
|---|---|---|
| 修复 / 优化 | 末位 +1 | 1.3.8 → 1.3.9 |
| 末位到 10 自动进位 | 末位归零、第二位 +1 | 1.3.9 → 1.4.0 |
| 新增小物品 | 第二位 +1、末位归零 | 1.3.0 → 1.4.0 |
| 系统性新增（大系统 / 大改） | 首位 +1、后两位归零 | 1.4.0 → 2.0.0 |

## 执行方式

1. 按上表确定新版本号，修改 `gradle.properties` 中的 `mod_version`。
2. `gradlew build` 会生成 `build/libs/godofthings-<版本>.jar` 并自动部署到
   `E:/MC/modpacks/PCL/versions/1.21.1测试/mods` 与 `E:/MC/ALL/mods/自制/1.21.1`
   （`deployJars` 任务自动清理旧版本号 jar，`keepOldVersions=false`）。
3. 提交代码并同步 GitHub：
   `git add -A && git commit -m "版本说明" && git push origin 1.21.1`
4. 发布 Release（打 tag + 创建 GitHub Release + 上传 jar）：
   `.\release.ps1 -Notes "更新说明"`（版本号缺省读 `gradle.properties`）

## 版本历史

- 2.0.5 → 2.0.6：神之资源 / 神之掉落可放置输入槽 1 → 9（优化）。
- 2.0.6 → 2.0.7：修复旧世界（2.0.5 保存的 9 槽前 NBT Size=1）加载时输入槽被缩回 1 槽、tickServer 遍历越界闪退（修复）。
- 2.0.7 → 1.0.0：正式发布。此前 2.0.x 为移植 / 测试期版本号，用户确认稳定后重新从 1.0 起算。
