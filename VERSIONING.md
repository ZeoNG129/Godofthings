# 版本号更替规范

God of Things 模组版本号采用 `x.y.z` 三段式，由 `gradle.properties` 的 `mod_version` 决定。

## 规则

| 变更类型 | 版本号变化 | 示例 |
|---|---|---|
| 修复 / 优化 | 末位 +1 | 1.3.8 → 1.3.9 |
| 末位到 10 自动进位 | 末位归零、第二位 +1 | 1.3.9 → 1.4.0 |
| 新增小物品 | 第二位 +1、末位归零 | 1.3.0 → 1.4.0 |
| **删除功能 / 删除物品 / 回退** | **末位 +1**（与「修复 / 优化」同类；整体回退到旧版本时可直接用旧版本号，非递增） | 2.12.2 → 2.12.3；2.7.5 → 2.7.0 |
| 系统性新增（大系统 / 大改） | 首位 +1、后两位归零 | 1.4.0 → 2.0.0 |

## 执行方式

1. 按上表确定新版本号，修改 `gradle.properties` 中的 `mod_version`。
2. `gradlew build` 会生成 `build/libs/godofthings-<版本>.jar` 并自动部署到
   `E:/MC/modpacks/PCL/versions/1.21.1测试/mods` 与 `E:/MC/ALL/mods/自制/1.21.1`
   （`deployJars` 任务自动清理旧版本号 jar，`keepOldVersions=false`）。
3. 提交代码并同步 GitHub：
   `git add -A && git commit -m "版本说明" && git push origin 1.21.1`
4. 发布 Release（打 tag + 创建 GitHub Release + 上传 jar）：
   `powershell -ExecutionPolicy Bypass -File .\release.ps1 -Notes "更新说明"`
   （版本号缺省读 `gradle.properties`；本机 PowerShell 默认禁止运行脚本，故加 `-ExecutionPolicy Bypass`）

## 版本历史
- 1.0.0 → 1.3.0：神之剑新增可切换功能（斩首 / 捕捉 / 抢劫，J 键打开面板，可单独或同时生效）。
- 1.3.0 → 1.4.0：神之剑新增吸星 / 吸魂功能；修复神之工具连锁挖掘 Tab 键状态残留导致默认连锁且关不掉的问题。
- 1.4.0 → 1.5.0：新增请神物品（右键生物赋予无限血量、保留受击反馈）。
- 1.5.0 → 1.5.1：请神可切换（再次右键取消）并加提示消息。
- 1.5.1 → 1.6.0：新增神之吞噬方块（垃圾销毁，含背包按钮便携版）。
- 1.6.0 → 1.6.1：吞噬改为退出界面销毁暂存物品；背包按钮位置修复。
- 1.6.1 → 1.7.0：神之套装飞行时挖掘速度不再受空中惩罚（×5）；吞噬按钮移到快捷栏正下方。
- 1.7.0 → 1.7.1：修复神之资源 / 神之掉落内部储存满后在机器上方生成掉落物（无限储存同类恒单堆、数量无上限）；神之合成自动输入改为均分到每一格。
- 1.7.1 → 1.7.2：修复神之合成模板界面物品栏无槽位无法操作（补玩家物品栏槽位）；自动输入均分改为低水位优先（锁定配方时同样生效，避免供给慢时退化成顺序填充）；合成改为每 tick 循环多次提高吞吐。
- 1.7.2 → 1.7.3：性能优化——神之合成每 tick 合成次数 256→8 并锁定配方改用 byKey 直查（修复掉 TPS）；吸星/吸魂改为每 5 tick 扫描一次、吸魂已在玩家面前的目标不再重复传送（消除生物抖动与海量移动事件）；神之矿机缓存附魔假镐（避免每挖一块重建附魔）。
- 1.7.3 → 1.7.4：传送点系统新增编辑功能（改名 / 改坐标，含「设为当前位置」）；删除点位增加二次确认。
- 1.7.4 → 1.8.0：新增神之黑盒物品（右键打开配置界面，含开关与白名单过滤槽位；开启后拾取物品先入黑盒，白名单外销毁、白名单内无堆叠上限保留）。
- 1.8.0 → 1.8.1：神之黑盒新增白名单 / 黑名单模式切换；修复同类物品无法合并累加导致的堆叠上限 64（改按物品类型合并同类恒单堆、数量无上限）。
- 1.8.1 → 1.8.2：神之剑吸星与神之黑盒兼容（吸星大范围吸收掉落物时按黑盒白名单 / 黑名单判定）；吸星 / 吸魂半径改为可调节（3~300，面板内 [-] / [+] 按钮）。
- 1.8.2 → 1.8.3：修复神之黑盒白名单模式物品看似消失（白名单槽位与存储合一，吸收物品直接堆叠进白名单槽位、数量无上限且可见）；新增数值增减鼠标手势（Shift=±10、Shift+Ctrl=±50，应用于神之剑吸星 / 吸魂半径调节）。
- 1.8.3 → 1.9.0：新增神之剑「杀戮光环」（可切换目标：敌对 / 友好 / 全部，范围 0~300，与斩首 / 捕捉 / 抢劫兼容）；抢劫强度改为可调节（0~300，指数映射到掠夺附魔等级 0~255）；神之黑盒蹲下右键可存储容器时把盒内物品转移进容器（装满剩余保留盒内）。
- 1.9.0 → 2.0.0：系统性新增——神之黑盒蹲下滚轮切换开关；大数字 K/M/G/T/P 单位显示；神之矿机新增时运 3 / 精准采集切换按钮（运 / 精）；修复神之附魔台不兼容神之剑附魔（神之剑加入 minecraft:swords 标签）；神之黑盒卸货对神之吞噬兼容（蹲下右键吞噬直接销毁盒内物品）。
- 2.0.0 → 2.0.1：神之黑盒滚轮快捷开关加提示且仅支持手持黑盒；槽位大数字显示去小数点缩短到不超单槽位；修复玩家丢出（Q 键）物品被黑盒吞掉不落地（丢出物品直接按白名单/黑名单规则入盒或销毁）；黑盒吸收大量掉落物性能优化（吸星批量一次深拷贝入库，替代逐条 copyTag）。
- 2.0.1 → 2.1.0：新增神之绑定器 + 神之传输（能量传输器，绑定器放入后给绑定玩家物品栏可充能物品充能、范围内 FE 机器充能；跨维度 / 玩家 / 机器独立开关，作用范围 64~160 随神之加速数量线性扩大）；修复神之黑盒界面里按 Q 丢出物品不吸收（菜单层直接拦截丢出）。
- 2.1.0 → 2.2.0：神之传输 UI 大改（三分区：无线连接 / 玩家充能 / 权限，无玩家物品栏，界面更宽松）；修复机器无线充能未生效（充能改 side=null 先查、六面兜底，兼容 Mekanism 等只在特定面暴露能力）；无线传输速率滑块（100/1K/5K/10K/100K）+ 手动输入 FE/T（1-9999999）+ 速率限制/无上限模式 + 跨维度连接开关 + 绑定设备数显示 + 清除全部绑定；玩家充能独立开关/跨维度/速率；新增绑定器右键机器绑定充能（再次右键取消），副手持有绑定器放置可充能机器自动绑定。
- 2.2.0 → 2.3.0：玩家绑定改为权限界面选择在线玩家绑定/解绑（绑定器不再放入神之传输，绑定器仅保留右键机器绑定/取消绑定）；删除神之加速兼容（移除加速槽，范围固定 64）；神之传输 UI 改为四标签页（无线连接 / 玩家充能 / 权限 / 已绑定机器，顶部按钮切换，已绑定机器页显示坐标列表）。
- 2.3.0 → 2.3.1：神之传输七项修复——①移除 UI 残留的「物品栏」标题；②速率输入框改为可正常输入（EditBox 注册进 widget 列表，此前未注册导致无法聚焦输入）；③权限页在线玩家列表改为客户端打开屏幕后主动请求（修 openScreen 时序竞态导致列表被丢弃）；④移除 64×64×5 范围每 10 tick 自动扫描绑定（绑定设备数不再虚高、大幅降低 TPS 开销）；⑤绑定器改为 onItemUseFirst 在方块交互前拦截（修复右键机器只绑定不打开机器 UI、手动绑定后机器充能生效）。
- 2.3.1 → 2.3.2：神之传输 Mek 兼容 + 失效绑定清理——①机器充能改探测式：遍历 side=null 与六面用 simulate 探测实际可接收量选接收量最大的 storage（替代「第一个 canReceive 直接充」，兼容 Mekanism 等 canReceive 行为特殊 / 特定面暴露能力的机器）；②绑定机器时记录方块 ID，每 20 tick 校验方块 ID 是否仍一致，被打掉自动从绑定列表清理（UI 不再残留已拆除机器）。
- 2.3.2 → 2.4.0：新增神之砍杀机器——功能板块（开关/范围 0-300/抢夺开关与强度 0-300/秒杀开关）+ 存储板块（27 格、无堆叠上限）；击杀范围内生物掉落物直接进内部存储不在世界生成（FakePlayer + 抢夺附魔剑作攻击者 + LivingDropsEvent 拦截），六面 FaceMode 输入输出配置（功能板块内）。
- 2.4.0 → 2.4.1：修复神之砍杀右键踢出游戏（StorageView 未实现 IItemHandlerModifiable，SlotItemHandler 在菜单加载 ContainerSetContentPacket 时 cast 失败；改为 implements IItemHandlerModifiable 并委托 setStackInSlot）。
- 2.4.1 → 2.4.2：神之砍杀 UI 重构——①功能/存储/经验三板块独立（renderSlot 只在存储板块渲染存储槽）；②玩家物品栏固定底部 y=84 起、各板块内容 y=24-78 不重合；③面配置改为单独按钮打开独立界面（GodSlaughterConfigMenu/Screen，参考神之熔炉）；④击杀生物经验点吸收进内部存储不生成经验球（LivingExperienceDropEvent setDroppedExperience(0)），新增经验板块（显示经验点数 + 取1/10/100级/全部按钮）。
- 2.4.2 → 2.4.3：神之砍杀修复——①三板块真正独立（存储槽 isActive 覆写，非存储板块不渲染/不 hover/不可点击，替代仅 renderSlot 控制）；②击杀范围改为球形半径（AABB 初筛后按 distanceToSqr ≤ range² 过滤，避免立方体对角超半径）；③经验显示等级（原版累计经验公式 xpToReach/xpToLevel 换算），取出 N 级按「升 N 级所需累计点数」扣除（随玩家当前等级递增与原版一致）；④神之吞噬背包按钮移到物品栏边框左下角。
- 2.4.3 → 2.4.4：性能优化——①神之砍杀扫描间隔 10→20 tick（降低大范围 AABB 扫描频率）；②神之传输机器充能加 storage 缓存（每 20 tick 随清理重建一次，替代每 tick 对每个绑定机器做 7 次 side=null+六面探测）。
- 2.4.4 → 2.5.0：AE2 兼容（初版，后被 2.5.1 修正）——暴露 AECapabilities.ME_STORAGE 当被动箱子给存储总线读。
- 2.5.0 → 2.5.1：AE2 兼容修正——改为机器作为 AE 网格节点直接并网（线缆直连、占一个频道、产物主动输出进 AE 网络，而非「存储总线当箱子读」）；熔炉/矿机/资源机/掉落机/砍杀/合成台 6 台会生产资源的机器实现 IInWorldGridNodeHost + AeGridNode（GridHelper.createManagedNode + REQUIRE_CHANNEL + IGrid.getStorageService().getInventory().insert），产物每 20 tick 自动推入 AE 网络；吞噬/附魔台不自动生产资源已回滚 AE 改动；删除 ItemHandlerMEStorage 适配器。
- 2.5.1 → 2.5.2：修复 AE 线缆无法连接——AeGridNode 补 `setInWorldNode(true)`（AE ManagedGridNode 的 InitData.inWorldNode 为 false 时创建的是普通 GridNode 而非 InWorldGridNode，线缆连不上；置 true 才创建世界内节点供线缆连接）。
- 2.5.2 → 2.5.3：AE 与机器多项优化——①Jade 兼容：机器并网时显示「设备在线/离线」（GodofthingsJadePlugin + @WailaPlugin + IBlockComponentProvider，libs 加 jade compileOnly）；②AE 网络工具/控制器显示机器方块图标（AeGridNode setVisualRepresentation(方块 Item) 替代默认线缆）；③神之合成台 AE 按钮移到产物槽上方，且锁定模板时开启 AE 自动合成（每 20 tick 从 AE 网络拉原料补齐合成格）；④时空永恒/生物覆灭加右键开关（默认开启）；⑤神之黑盒过滤槽 9→15 格（5×3 向右扩）；⑥神之砍杀移除每次击杀上限，扫描时范围内生物一次性全部死亡。
- 2.5.3 → 2.6.0：新增神之吸收（大范围掉落物+经验吸收器，开关/范围 0-1600/功能存储经验三面板/面配置/AE 并网，参考神之砍杀）；修复 Jade 离线误报（改为服务端 IServerDataProvider 收集在线状态经 NBT 同步，客户端不创建 grid node 直接判断恒离线）；合成台 AE 按钮再上移去重合；时空永恒/生物覆灭开关材质变化（enabled 布尔 blockstate，开启绿/关闭红）；黑盒过滤槽 5×3 统一槽位框风格（弃 dispenser 贴图）；所有可调范围上限 300→1600（神之剑吸星/吸魂/光环、神之砍杀）。
- 2.6.0 → 2.6.1：三项修复——①AE 机器在线显示改用 AE 原本方法：7 台机器 BE 从 `implements IInWorldGridNodeHost, IActionHost` 改为 `implements IGridConnectedBlockEntity`（AE 自带 Jade 集成识别的接口，自动显示在线/离线/缺频道），删除自建 GodofthingsJadePlugin 与 jade compileOnly 依赖；②时空永恒/生物覆灭开关材质改为「原版图案 + 绿(开)/红(关)边框」而非纯色替换，两个方块材质不再一样；③无痕夜视失效修复：由客户端改伽马值改为服务端药水夜视 `MobEffectInstance(NIGHT_VISION, 400, 0, ambient=true, visible=false, showIcon=false)`，更可靠。
- 2.6.1 → 2.6.2：修复客户端无法连接服务端（「服务端缺少此客户端需要的网络通道 godofthings:transmitter_list」）——S2C 通道 `TransmitterListPayload` 此前只在客户端侧 ClientTransmitterHandler 用 playToClient 注册，服务端未声明该通道导致握手失败；修复为在 TransmitterMessages（两端都执行的 MOD 总线）里 playToClient 注册并把 handle 移入 TransmitterListPayload，删除 ClientTransmitterHandler。
- 2.6.2 → 2.6.3：传送点 UI 新增「新建」按钮——点开可输入名字创建记录点（坐标默认玩家当前位置），复用编辑界面新增「新建传送点」模式；`WaypointData.create(ServerPlayer,name,x,y,z)`（维度/面对方向取玩家当前状态）+ C2S `WaypointCreatePayload`。
- 2.6.3 → 2.6.4：神之矿机补上面配置（此前六面固定全输出）——新增 `GodMinerConfigMenu`/`GodMinerConfigScreen`（六面磁贴与神之熔炉/神之吸收同构，矿机语义为「输入＝抽入神之加速、输出＝推出产物与液体」），主界面 AE 开关右侧加「面配置」按钮（按钮 10）；`GodMinerBlockEntity` 加 `faceModes[6]` + 按面受限的物品能力（槽 0＝加速槽只收神之加速、槽 1..N＝产物区只出不进）+ 只出不进的液体能力 + 面感知 `autoTransfer()`（每 tick 每 INPUT 面最多搬 1 个神之加速）并写入 NBT（旧存档无 FaceModes 字段时默认六面全 OUTPUT，行为不变）。同时修复 4 项资源缺失与 3 项工程问题：①神之传输/神之砍杀/神之吸收/神之吞噬 4 个方块补 loot table（此前打掉不掉落、连同内部存储一起蒸发）并补进 `minecraft:mineable/pickaxe` 标签（此前挖得极慢）；②修复神之黑盒与神之吞噬合成配方完全相同导致其中一个永远合不出来（神之吞噬改为 8 黑曜石 + 岩浆桶）；③17 个方块 loot table 补 `minecraft:survives_explosion` 条件（与原生方块一致）；④补 31 个 `advancement/recipes/*` 配方书解锁条目（此前配方能摆但不进配方书）；⑤修复 `gradle.properties` 的 16 处编码损坏（非法 UTF-8，中文注释丢字节）；⑥README 神之砍杀范围同步为 0-1600（2.6.0 已改但 README 未同步）；⑦`.gitignore` 补 `run-server/`（此前 `gradlew runServer` 会把服务端存档一起提交）。
- 2.6.4 → 2.7.0：**AE2 改为真正的软依赖**（此前是隐藏硬依赖，未装 AE2 时 7 台机器全部 `NoClassDefFoundError` 崩在注册阶段——dev 环境实测复现，AE2 只在 compileOnly 里、dev 运行时根本没有）。做法：7 个基础方块实体（熔炉/矿机/资源机/掉落机/合成台/砍杀/吸收）去掉 `implements IGridConnectedBlockEntity` 与全部 appeng 引用，改为空实现的 `protected pushOutputToAe()` / `protected aeAutoCraft()` 钩子；AE 逻辑移入新包 `com.godofthings.ae2` 的 7 个 `*AeBlockEntity` 子类，由 `AeSoftDepend.blockEntity()`（不引用 appeng 的入口类）在 `ModList.isLoaded("ae2")` 为真时反射构造，失败自动回退基础版；AE 能力注册集中到 `AeRegistration`，由 MOD 总线上的 `AeCapabilityDispatch` 守卫后反射调用；`Godofthings` 的 7 个 `BlockEntityType` 注册改用反射供应器。**新增神之套装功能开关系统**：穿齐全套神之护甲的 12 项功能（创造飞行 / 飞行无惯性 / 飞行挖掘不减速 / 免疫所有伤害 / 不会死亡 / 免疫负面状态 / 永不饥饿 / 火焰熔岩免疫 / 水下呼吸 / 无痕夜视 / 熔岩可视 / Ad Astra 无限氧气）各有独立开关，O 键打开 `GodArmorConfigScreen`（2×6 开关 + 全部开启/全部关闭/返回），开关按玩家存在新数据附件 `godofthings:armor_features`（`Codec.INT` + `copyOnDeath`，默认全开＝旧行为不变），客户端经 `ArmorMessages` 三个包（C2S mask/request + S2C sync）与服务端收敛；`GodArmorHandler`/`GodArmorClientHandler`/`AdAstraCompat` 全部改为按开关门控（飞行开关关闭时会正确收回飞行能力）。
- 2.7.0 → 2.7.1：修复「AE 接入开关」名不符实——此前该开关**只停产物输出，网格节点在 `onLoad` 里无条件创建**，所以机器始终并网、始终占着一个频道，关掉开关也不断开。现改为开关真正控制节点生命周期（新增基础类钩子 `onAeEnabledChanged()`，由 AE 子类的 `applyAeConnection()` 覆写）：**开启＝`ManagedGridNode.create()` 并入网络，关闭＝`destroy()` 断开连接并释放频道**（Jade 与网络工具随即显示离线）；`onLoad()` 也改为按 NBT 里的开关状态决定是否建节点（`onLoad` 由 `Level.tickBlockEntities` 在下一 tick 触发，晚于 `loadAdditional`，所以首帧即正确），旧存档开关为开者行为不变。`AeGridNode` 相应改造：`create()` 加幂等守卫、`destroy()` 销毁后必须丢弃实例（AE2 的 `ManagedGridNode` 一旦 `create` 过，`InitData` 即被清空，再次 `create` 会抛 `IllegalStateException`，只能重建新实例——已用 javap 核对 AE2 19.2.17 字节码确认）、`isActive()`/`getStorage()` 改为空安全查询不再顺手创建节点。顺带把 README 的 AE2 兼容条目从「6 台」更正为 **7 台**（漏了神之吸收）并写明开关语义。**⚠ 本版本的实现方式有误（destroy + 重建节点），实测导致 AE 产物不再输出，已在 2.7.2 回退重做。**
- 2.7.1 → 2.7.2：**回退 2.7.1 的「销毁 + 重建节点」方案，AE 产物输出恢复正常**。教训：AE2 的 `ManagedGridNode` 生命周期不能来回切——`create()` 开头就调 `getInitData()`，而 `create()` 自己会把 `InitData` 置空，所以同一实例第二次 `create()` 必抛 `IllegalStateException`；加上 AE2 自身（线缆扫描、Jade、能力查询）随时会调 `getMainNode()`，`destroy` 后再重建的时序极难保证，最终表现为节点建不起来 → 机器不并网 → 产物不输出。**正确做法（本版）**：节点生命周期完全恢复成引入开关之前的样子（`onLoad` 调一次 `create`、`setRemoved` 调一次 `destroy`，不再由开关触碰），「AE 接入」开关改为调 AE2 官方的运行时接口 `IManagedGridNode.setExposedOnSides()`——**开启＝暴露六个面（线缆可连、占一个频道），关闭＝不暴露任何面（线缆连不上 → 节点脱离 ME 网络 → 释放频道，Jade/网络工具显示离线）**。该接口是 AE2 自己 `ToggleBusPart`（切换总线）等部件用来运行时接入/断开的同一入口，`InWorldGridNode.setExposedOnSides` 内部会 `updateState()` 重算连接；`isExposedOnSide` 正是 `GridHelper` 建立连接时的判定依据（均以 javap 核对 19.2.17 字节码确认）。`AeGridNode` 另用自己的 `created` 布尔记录建节点状态（不再依赖 AE2 的 `isReady()` 语义），并新增诊断日志：每种方块实体选用了 AE 版还是基础版各打一条 INFO（`AeSoftDepend`），能力注册入口调用成功也打一条，便于日后一眼定位「AE 功能没生效」是装配问题还是逻辑问题。
- 2.7.2 → 2.7.3：**修机器始终并不上 AE 网络**（2.7.2 实测：能力注册成功、AE 版方块实体也确实被实例化，但线缆始终连不上）。根因是**建节点时机不是 AE2 的时机**：本项目一直在方块实体的 `onLoad()` 里调 `getMainNode().create(...)`，而 AE2 自己的联网机器 `AENetworkedBlockEntity` 用的是 `clearRemoved()` → `scheduleInit()` → `GridHelper.onFirstTick(...)` → `onReady()` 里才建节点，并在 `onChunkUnloaded()` + `setRemoved()` 两处销毁（MC 卸载区块不会走 `setRemoved`）。本版照抄该时序：7 个 AE 子类改为 `clearRemoved()` 里 `GridHelper.onFirstTick(this, ::initAeNode)` 排队建节点、`onChunkUnloaded()`/`setRemoved()` 销毁，`onLoad()` 仅作兜底（`create` 幂等）；`initAeNode()` 加 try/catch 只记日志，绝不让 BE 初始化挂掉。同时把节点状态（created/connected/node/ready/active/online/grid）打进 INFO 日志（`AeGridNode.logState`，建节点后与每次切换开关后各一条），下次若仍连不上，日志可直接区分「节点没建起来」还是「节点在但没并进网络（旁边无网络/无电/缺频道）」。
- 2.7.3 → 2.7.4：**真正的根因：方块实体没覆写 `getCableConnectionType`，被 AE2 当成「玻璃线缆级主机」**。2.7.3 的状态日志证明节点确实建起来了（`node=true ready=true grid=true`），于是问题只可能在「线缆为什么不认这台主机」。查 AE2 19.2.17 字节码发现 `IInWorldGridNodeHost.getCableConnectionType(Direction)` 是**默认方法且返回 `AECableType.GLASS`**；而 AE2 自家所有机器（Drive / Interface / PatternProvider / SpatialAnchor / AENetworkedBlockEntity / AENetworkedPoweredBlockEntity …，逐个 javap 核对过）**无一例外显式返回 `SMART`**（能量接收器返回 `COVERED`），没有任何一台用默认值。本项目此前只在辅助类 `AeGridNode` 里写 `return AECableType.SMART`，**但那个类永远不会被 AE2 调用**——AE2 拿到的 host 是能力返回的方块实体本身，走的是接口默认 GLASS。现改为 7 个 AE 子类各自覆写 `getCableConnectionType` 返回 `SMART`，与 AE2 自家机器一致。同时给 `getGridNode(side)` 加了前 4 次查询的诊断日志（打印返回是否为 null + `AeGridNode.describe()` 的 active/online/powered/booted/gridSize），用于确认「线缆到底有没有来问」以及「节点是否落在真正的网络里（gridSize=1 表示还孤零零一个节点）」「网络是否供电」。**实测确认修复生效**：放置后约 3 秒日志从 `gridSize=1 powered=false` 变为 `gridSize=10 active=true online=true powered=true`（已并入 10 节点的网络并供电在线）；关掉 AE 开关立即 `connected=false active=false online=false`，再打开立即恢复 `online=true`。
- 2.7.4 → 2.7.5：修「挖掉机器再重新放置后连不上」。节点创建加**自愈**：`AeGridNode.ensureCreated()` 在 AE2 每次查询 `getGridNode(side)` 时检查，若节点尚未建立就地补建（幂等，仅服务端）——这样即使排队的初始化没跑到（破坏后重放、区块重载、各种加载顺序差异）也不会出现「方块在但永远不并网」。另外 `create()` 建完节点后主动 `level.updateNeighborsAt(...)` 踢一次邻居，让旁边线缆立刻重新扫描本主机，而不是等下一次偶然的方块更新。
- 2.7.5 → **2.7.0（按用户要求回退，非递增）**：用户实测后决定回退到 2.7.0 的 AE 行为。本次回退**只撤销 2.7.1～2.7.5 的 AE 接入改动**，保留 2.6.4 与 2.7.0 的全部成果（资源缺失修复、矿机面配置、配方冲突修复、AE2 软依赖重构、神之套装功能开关界面）。具体撤销内容：①删除基础方块实体的 `onAeEnabledChanged()` 钩子（开关恢复为「只挡产物输出」，不再断网/并网）；②`AeGridNode` 恢复为 `create()`（onLoad 里无条件建节点）/`destroy()` 原样；③7 个 AE 子类恢复为「`onLoad` 建节点 + `setRemoved` 销毁」，移除 `clearRemoved`/`onChunkUnloaded`/`initAeNode`/`setConnected`/`getCableConnectionType`/`getGridNode` 等全部改动。**但下面这些结论已用实测日志确认，回退后仍然有效，将来若再修 AE 连接可直接照做**：Ⅰ. 建节点时机应照 AE2 自己的 `AENetworkedBlockEntity`——`clearRemoved()` → `GridHelper.onFirstTick()` → 建节点，并在 `onChunkUnloaded()` + `setRemoved()` 两处销毁；Ⅱ. **最关键的一条：方块实体必须覆写 `getCableConnectionType(Direction)` 返回 `AECableType.SMART`** —— 该接口默认实现返回 `GLASS`，而 AE2 自家所有机器都显式返回 `SMART`（`AeGridNode` 里写 SMART 没用，AE2 拿到的 host 是方块实体本身）；只加这一条，实测日志即从 `gridSize=1 powered=false` 变为 `gridSize=10 active=true online=true powered=true`，机器成功并入网络并供电在线。另外实测确认 `IManagedGridNode.setExposedOnSides()` 可做运行时接入/断开（关闭＝不暴露任何面→线缆连不上→脱离网络并释放频道）。
- **回退到 2.6.3 的 AE 形态 → 2.6.4（按用户要求，非递增）**：用户实测 2.7.0 后反馈「很多机器还是不能连接进 AE 网络」，决定放弃全部 AE 接入改动，以 **2.6.3** 为基础。本次**只保留**这 5 项（其余全部回到 2.6.3）：①4 个方块（神之传输/砍杀/吸收/吞噬）的掉落表 + `mineable/pickaxe` 标签 + 全部 17 个掉落表的 `survives_explosion` 条件；②神之黑盒 / 神之吞噬配方冲突修复（吞噬改为 8 黑曜石 + 岩浆桶）；③31 个配方书解锁 advancement；④神之矿机六面配置（`GodMinerConfigMenu`/`GodMinerConfigScreen` + 主界面按钮 10）；⑤神之套装功能开关界面（O 键 + `godofthings:armor_features` 附件）。**已撤销**：AE2 软依赖重构（7 个基础 BE 恢复为 `implements IGridConnectedBlockEntity` 并把 AE 逻辑内联回去，删除 `ae2` 包下的 `AeSoftDepend`/`AeRegistration`/`AeCapabilityDispatch` 与 7 个 `*AeBlockEntity`，`Godofthings` 的 BE 注册恢复直接 `::new`）、「AE 接入开关控制并网/断网」的行为、以及 2.7.1～2.7.5 的全部 AE 改动。**另保留两项纯工程改动（无任何游戏行为影响）**：`gradle.properties` 的 16 处编码损坏修复（不修则文件不是合法 UTF-8）、`.gitignore` 补 `run-server/`。注：本版 AE 形态与 2.6.3 完全一致——即「AE 接入开关只挡产物输出」，机器并网与否取决于 AE2 对该方块实体的识别；若要真正修好并网，见上一条中的 Ⅱ（`getCableConnectionType` → `SMART`）。
- 2.6.4 → **2.8.0（新增神之套装技能树，阶段 1）**：把 **Zifeng Skill Tree（子枫的百宝箱）** 的「配置界面 + 基础属性 + 特殊增幅」移植到神之套装上，并按需求**删除技能点解锁需求与全部前置需求**（点击即解锁）。本次交付：①**技能树界面**（K 键打开，列式布局，鼠标左键开/关、右键 +1 级、Shift+右键 +10 级，每列可一键全开/全关，悬停显示每级效果与当前等级）；②**属性引擎**——移植原 mod 的统一公式「最终属性 = Σ基础固定值 × (1 + Σ增幅百分比)」，用原版 `ADD_VALUE` + `ADD_MULTIPLIED_TOTAL` 天然实现，旧修饰符按技能独立 id 重挂（关闭即回收，不残留）；③**基础属性 15 项**（血魄淬炼 +20 生命/级、磐石之躯 +20 护甲 +10 韧性/级、剑心通明 +10 攻击/级、疾风连击 +2.0 攻速/级、破岩神工 +3.0 挖掘/级、健步如飞 +0.05 移速/级、生生不息 +2 生命/秒/级、鸿运当头 +1.0 幸运/级、一蹦三尺 +0.1 跳跃/级、御空翱翔 +0.05 飞行/级、如鱼得水 +0.05 泳速/级、暴击要害 +1% 暴击率/级、噬血之刃 +1% 吸血/级、荆棘护体 +0.5 反伤/级、破甲利刃 +1.5% 破甲/级）；④**特殊增幅 15 项**（各 +100%/级，破岩真解 +120%、暴击真解 +50% 暴击伤害、噬血/荆棘/破甲真解各 +40%）；⑤**事件型机制**：回血（每秒）、暴击（含倍率）、吸血（伤害结算后）、荆棘反伤、破甲增伤，均在服务端事件里按开关实时判定。数据用玩家附件 `godofthings:armor_skills`（`Map<技能id,等级>`，`copyOnDeath`），客户端用静态镜像，进出世界/重生/跨维度/穿脱套装自动重挂；**技能只在穿齐全套神之护甲时生效**。
  - **单位换算（关键）**：原 mod 有 10 倍等级压缩（1000 级 → 100 级、500 级 → 50 级），效果计算时 `有效等级 = UI等级 × 10`。本移植把该 ×10 **直接烘进每级数值**（即上表数值 = 原 `perPoint × 10`），所以界面上显示几级就按几级算，无需再乘。原 mod 的 1 级 ≈ 本表 1 级，数值一一对应。
  - **后续阶段（未做）**：终极节点 21 / 特殊被动 24（纯事件型，阶段 2）；光环 11 / 机械共鸣 13（需常驻 tick 与选区交互，阶段 3）；魔法增幅 29（**其中 26 项是给新生魔艺/铁魔法/Goety 加属性的兼容技能，未装对应模组不生效**，阶段 4）。
  - **许可**：技能数值与机制移植自 Zifeng Skill Tree（Copyright (c) 2026 zifeng，MIT License），署名见 README「第三方代码与许可」。
- 2.8.0 → **2.8.1（技能树界面重做 + 血量数字显示）**：①**界面改成直接搬参考模组的设计**（原来手搓的版本太丑，用户反馈）——圆角贴片 + 左侧分类色条 + 等级进度条 + 分类标签行 + 可滚动列表与滚动条，配色沿用参考模组按分类区分的深色贴片方案（基础列 `0xFF203A55/0xFF2B4B6D` + 天蓝强调色 `0xFF87CEEB`，增幅列 `0xFF503A27/0xFF674A30` + 橙强调色 `0xFFFFAA55`），圆角半径沿用其 `R_BIG=12 / R_SUB=8 / R_ROW=6`，进度条沿用其「双轨底 + 填充 + 黄色手柄（`0xFFFFDD44`、高 6px）」。②**O 键合并进本界面**：技能树与套装功能（原 12 项开关）现在是同一个界面的两个标签页，**K** 打开停在「基础属性」页、**O** 打开停在「套装功能」页（原先 O 是另一个独立界面，用户反馈没合并）。③**升级语义修正**：到上限后再升级**不再回绕到 1 级**（服务端 clamp 到 `[0, 上限]`）。④**新增拖动改等级**（参考模组同款）：按住左键在等级进度条上拖动＝按位置直接设定等级，滚轮悬停进度条＝等级 ±1、滚轮在别处＝滚动列表；右键点行仍为 +1 级（Shift+右键 +10）。⑤**移植血量数字显示**（`ArmorHealthHelper` + `ArmorHealthHud` + `GuiHeartsCompressMixin`，均移植自参考模组，MIT）：血量堆到十万量级时原版会画数万颗心——既铺满屏幕挡视野，又让每帧 blit 次数爆炸掉帧。做法是**只压缩「喂给原版渲染器的数值」**（`renderHealthLevel` 的当前生命 / 最大生命 / 伤害吸收三个取值点各一个 `@Redirect`），原版渲染代码一行未改、真实生命值完全不受影响（属性/伤害/存档不变），心条永远最多 10 颗；真实数值以数字显示在血条左侧（`%s / %s`，吸收另起一行）。本项目首次引入 Mixin：`src/main/resources/godofthings.mixins.json` + `neoforge.mods.toml` 的 `[[mixins]]` 声明；全部注入点 `require = 0`，注入失败只静默跳过、绝不崩游戏；真实最大生命 ≤ 20（原版）时完全不干预。
- 2.8.1 → **2.9.0（修 3 个问题 + 技能树阶段 2 第一批：终极节点 9 + 特殊被动 6）**
  - **修复①「功能开关重置等级」**：原来关闭技能 = 把该技能从表里删掉，再打开就只能给默认 1 级。现改为<b>带符号等级</b>编码——<b>正数 = 已开启</b>、<b>负数 = 已关闭但记住等级</b>（绝对值），不存在 = 从未解锁。于是：关掉再打开恢复原等级；「全部关闭」只关不清等级；「重置」才是真清除。界面在关闭状态显示「关 (Lv.N)」，让"关掉不丢等级"一目了然。
  - **修复②「每次打开界面都回到第一页」**：新增客户端配置 `config/godofthings-client.toml`（`ClientConfig`，`ModConfig.Type.CLIENT`）记住上次停留的标签页，关闭界面时落盘、关游戏再进也保留。K / O 两键现在都回到上次离开的那一页（不再强制跳第一页）。
  - **修复③（关键，否则阶段 1 的属性被原版悄悄砍掉）**：新增 `RangedAttributeMixin`（移植自参考模组，MIT）解除原版 `RangedAttribute.sanitizeValue` 的属性上限。原版上限对本 mod 来说低到离谱：`MAX_HEALTH` 只有 1024（血魄淬炼 100 级 × +20 = +2000 会被砍到 1024）、`ARMOR` 只有 **30**（磐石之躯 2 级就顶满）、`JUMP_STRENGTH` 只有 32。白名单 10 个属性（生命/攻击/攻速/挖掘/移速/幸运/跳跃/飞行 + 护甲/护甲韧性），按<b>属性实例引用</b>比较、用<b>编译期字段引用</b>取值（参考模组踩过"反射字符串被重映射→白名单恒为空"的坑）；其余属性保持原版 clamp，多模组零误伤。`require = 0` 注入失败只降级不崩。
  - **阶段 2 第一批（15 个技能）**：终极节点 9 个 —— 浴血奋战（常驻 +50% 攻击与生命）、全能精通（全属性 +25%）、稳如泰山（每级 +10% 击退抗性，满级免疫）、横扫千军（每级 +1 格攻击距离）、财源滚滚（每级掉落翻倍）、猎魂丰收（每级生物掉落 +1 倍）、点石成金（每级方块掉落 +1 倍）、经验飞涨（每级经验 +2 倍）、宇宙的青睐（真创造飞行）；特殊被动 6 个 —— 长臂善舞（每级 +1 格触摸/攻击距离）、星瞳夜视、饱食无忧、鲛人之息、破暗之瞳（持续清除黑暗）、万民敬仰（村庄英雄）。收益型挂 `LivingDropsEvent` / `BlockDropsEvent` / `LivingExperienceDropEvent`，常驻型每 40 tick 刷新一次药水效果（持续 100 tick，不闪烁）。界面相应扩为 **5 个标签页**（基础属性 / 特殊增幅 / 终极节点 / 特殊被动 / 套装功能），配色沿用参考模组对应分类的贴片色（终极 `0xFF512B34`、特殊 `0xFF4C3B27`）。技能总数 **45**。
  - **阶段 2 剩余（未做）**：金身真解（需自注册物理减伤属性）、不坏金身、凤凰涅槃、死神凝视、奥术神体、虚空神体、万物可掘（需方块 Mixin）、不朽铭文（铁砧）、自动熔炼、斩首夺颅、妖魂凝卵、万载不磨（需物品 Mixin）；特殊被动剩余 18 个（附魔系列 / 村民交易 / 寻宝 / 闪现 / 暴食等）。
- 2.9.0 → **2.10.0（技能树阶段 2 第二批：终极节点做满 21/21，新增 12 个）**
  - **战斗大招**：**金身真解**（每级 +1% 物理减伤，上限 80 级；为此新增自定义属性 `godofthings:damage_reduction` + `ModAttributes`，并用 NeoForge 的 `EntityAttributeModificationEvent` 挂到玩家身上——因为原版护甲减伤公式本身封顶 80%，护甲再高也无用，故用独立乘算层承接）、**不坏金身**（常驻 抗性提升 X / 伤害吸收 C / 抗火 V，无限时长不倒数）、**凤凰涅槃**（死亡原地复活：回 50% 生命、清空状态、5 秒吸收盾、播放不死图腾动画、冷却 60 秒）、**死神凝视**（非玩家目标生命低于 15% 时 30% 概率直接处决，伤害 99999）、**奥术神体**（魔法伤害 −35%）、**虚空神体**（免疫击退 + 免死兜底，冷却 60 秒）。
  - **掉落生产**：**万载不磨**（每 20 tick 把背包与装备的耐久修满 → 工具护甲永不损耗；不引入物品 Mixin）、**妖魂凝卵**（每级 10% 概率额外掉刷怪蛋，用原版 `SpawnEggItem.byId` 查表）、**斩首夺颅**（每级 20% 概率额外掉头颅，内置 8 种生物→头颅映射）、**自动熔炼**（方块掉落查原版熔炼配方表替换成产物）、**万物可掘**（手持镐子可挖基岩等不可破坏方块——新增 `BlockStateMixin`，Mixin 目标是 `BlockBehaviour.BlockStateBase.getDestroyProgress` 而不是 `BlockState`，因为该方法定义在父类、Mixin 到 BlockState 会静默找不到；不可破坏时改用黑曜石的挖掘进度，客户端动画与服务端流程都能跑通）、**不朽铭文**（铁砧中放两个相同物品 → 合成带原版 `Unbreakable` 组件的工具）。
  - 界面无需改动（终极节点标签页自动多出 12 行）。技能总数 **57**（基础 15 + 增幅 15 + 终极 **21** + 特殊 6），语言键 410×2、差异 0。
  - **阶段 2 剩余**：特殊被动 18 个（附魔三件套 / 村民交易 / 寻宝大师 / 暴食 / 闪现 / 御风止步 / 凌空采掘 / 烈焰不侵 / 发光 / 奥术防护 5 个）。**魔法增幅 29 个**需先决定是否引入新生魔艺 / 铁魔法 / Goety。
  - **踩坑记录**：`SpawnEggItem` 在 1.21.1 没有无参 `getType()`，要用静态 `byId(EntityType)`；`Unbreakable` 没有 `FALSE` 常量，判断"是否已无法破坏"用 `stack.has(DataComponents.UNBREAKABLE)`。
- 2.10.0 → **2.10.1（修用户反馈的两个 bug）**
  - **修复①「配置界面右上角的"是否穿齐全套"不更新」**：原来错用了 `GodArmorState.getClientMask() != 0` —— 那是"12 项功能开关的位图"，跟穿没穿全套毫无关系（只有把 12 项全关掉才会显示"未穿齐"）。改为**每帧实时读玩家背包**（`GodArmorHandler.isFullSetWorn(Minecraft.getInstance().player)`，纯装备槽检查、双端可用），穿脱护甲立刻反映。
  - **修复②「血条永远只显示半颗心，即使血量几万」**：根因是**生命上限涨了但当前血量不会自动跟着涨**——装备后上限从 20 涨到几万（本模组属性加成），而原版只在"上限下降"时才 clamp 当前血量，于是当前血仍是 20；血条压缩按 `当前血 × 20/上限` 计算 → `20 × 20/20000 = 0.02` → 不到 1 点血 → 半颗心。又因为神之套装自带「免疫所有伤害 / 不死」，血量既不掉也不补，症状就永久固定。修法：在属性重挂（`ArmorSkillHandler.refresh`）前后记录生命状况，**上限变高时按原生命百分比同步当前血量**（满血→满血、半血→半血）；上限变低时不缩放，交回原版 clamp（脱下套装即为满血）。这样也顺带修掉了"穿上套装后残血但血条几乎为空"的观感问题。
- 2.10.1 → **2.11.0（技能树阶段 2 第三批：特殊被动 15 个）**
  - **生存便利**：发光（附近 35 格生物发光，每 40 tick 刷一次）、烈焰不侵（常驻抗火）、暴食（进食瞬间完成——把 `LivingEntityUseItemEvent.Start` 的时长压到 1 tick）。
  - **奥术防护（6 个，公式移植自参考模组）**：奥术壁垒（每级 +6 魔法防御）、奥术真解（每级再 +4）、法术抑制（弹射物/法术额外减伤，每级 +5 防御）、适应之躯（每次受魔法伤害 +2% 减伤，最多 60%，8 秒不受伤衰减）、法术反射（30% 概率把魔法伤害原样反弹给施法者）、驱法破咒（每 5 秒清除自己与附近友方各一个负面）。核心公式 `减伤 = 防/(防+k)`（k：魔法 1200 / 法术抑制 800）——**渐进逼近 1、永不封顶，且有效生命随防御线性增长**，因此每一级价值恒定，不会出现"第 5 级就封顶"；并按参考模组的做法与「奥术神体」的 −35% **乘算**叠加（不是相加）。另加「破法之刃」：目标身上每个增益 → 对其伤害 +15%（最多 +60%）。
  - **附魔三件套（铁砧）**：随机附魔（右槽 4 青金石 + 1 级 → 随机一条可附魔的正面附魔）、附魔突破（右槽 2 青金石块 + 4 级 → 已有附魔各 +1，上限 20）、超限附魔（右槽 2 下界之星 + 10 级 → 已有附魔各 +1，上限 100）。用 `EnchantmentHelper.updateEnchantments` + `ItemEnchantments.Mutable` 实现，按 `EnchantmentTags.CURSE` 排除诅咒。
  - **交易两件套**：无限交易（`TradeWithVillagerEvent` → `MerchantOffer.resetUses()`）、村民大师（右键村民提升为大师级 5 级）。
  - 技能总数 **72**（基础 15 / 增幅 15 / 终极 21 / 特殊 **21**），语言键 470×2、差异 0。
  - **特殊被动未做的 3 个（说明原因）**：**寻宝大师**（需要客户端方块发光轮廓渲染）、**御风止步**（无飞行惯性，需改客户端移动输入）、**凌空采掘**（本模组的"穿齐全套"本身已提供飞行挖掘无惩罚，做技能会重复）。**闪现**按用户要求不做。**法力虹吸**依赖 Ars Nouveau / Iron's Spells 的魔力系统，与魔法增幅一起留到最后一批。
  - **额外修复（用户反馈）**：配置界面右上角"是否穿齐全套"改为**缓存 + 每 5 tick 刷新，且只在界面打开期间计算**（原先每帧扫一次背包——实测 4 次数组读取 + 4 次 instanceof，开销可忽略，但按用户要求改成更省的方案）。
- 2.11.0 → **2.11.1（补齐遗漏的「碧波清眸」）**：用户指出特殊被动漏了**碧波清眸（`underwater_vision`，水下视野）**。已补：水下与岩浆中**雾效完全消除**，视野与空气中一致。
  - **为什么必须用 Mixin 而不是 ViewportEvent**（参考模组踩过的坑）：`ViewportEvent.RenderFog` 在 `setupFog` 内部设置完 shader 之后才触发，而**水下是球体指数雾**——只改 start/end 玩家仍只能看一两格。故新增客户端 `FogRendererMixin`，在 `setupFog` **入口直接拦截**：等价于原版 `setupNoFog`（`RenderSystem.setShaderFogStart/End(Float.MAX_VALUE)` + `setShaderFogShape(CYLINDER)` + `ci.cancel()`）。
  - 生效条件（全满足才拦截，零误伤）：①相机浸没在水或岩浆 ②相机实体是**本地玩家**（多人下不影响别人视角）③碧波清眸已开启**且穿齐全套**。`require = 0`，注入失败只降级（雾照旧）不崩。
  - 技能总数 **73**（基础 15 / 增幅 15 / 终极 21 / 特殊 **22**），语言键 442×2、差异 0。Mixin 总数 4 个（RangedAttribute / BlockState / GuiHeartsCompress / **FogRenderer**）。
- 2.11.1 → **2.12.0（技能树阶段 3：机械共鸣 12 个）**
  - **八个「共鸣」开关**（战利品爆炸 / 工具不毁 / 生物掉落 / 方块掉落 / 经验获取 / 刷怪蛋掉落 / 头颅掉落 / 自动熔炼）：移植参考模组的 `SkillEffects.isEffectAllowedFor` 语义——**真玩家穿齐全套即生效、不需要共鸣技能；模拟玩家机器（`FakePlayer`）必须以主人 UUID 解析出在线主人、主人穿齐全套、并开启对应共鸣技能，才允许继承该效果**。关闭共鸣立即回收（每次事件实时判定，无持久状态）。已接入：`LivingDropsEvent`（生物掉落/刷怪蛋/头颅/战利品爆炸）、`BlockDropsEvent`（方块掉落/自动熔炼/战利品爆炸）、`LivingExperienceDropEvent`（经验）、以及万载不磨的定期修耐久。
  - **四个选区技能**：选区攻击（半径 8 格内敌对生物每 20 tick 受一次你的攻击伤害）、防护选区（半径 8 格内友好生物免疫你的伤害）、**选区挖掘**（按 **N** 键一键挖掉半径 8 格内可破坏方块）、**选区放置**（按 **B** 键用主手方块填满半径 8 格内空位，消耗手中方块、创造模式不消耗）。
    - 适配说明：参考模组的选区技能依赖它的「木棍工具」系统（木棍框选区域 + 6 种模式循环 + 线框渲染，共约 1500 行）；本项目没有该系统，故改为**以玩家为中心、固定半径 8 格的方形区域 + 两个触发键**，效果等价（同为区域批量操作），且**分批执行**（每 tick 最多 256 格）避免一次性改动上万方块导致卡顿。
  - **技能总数 85**（基础 15 / 增幅 15 / 终极 21 / 特殊 22 / **机械共鸣 12**），语言键 476×2、差异 0。界面扩为 **6 个标签页**（新增「机械共鸣」，配色沿用参考模组 `0xFF3E4248`/`0xFF515760`）。
  - **未做 1 个（说明原因）**：**机械之星 `machine_star`** —— 在参考模组里它是「前置核心」，**自身没有任何效果**（只作为其它共鸣技能的前置条件）。按用户「删除前置需求」的要求，做出来也只是一个点了没反应的节点，故不做。
- 2.12.0 → **2.12.1（选区技能改成原版那套木棍框选）**：用户指出选区技能要"原版那样的"，即不能只用固定半径。已按参考模组的木棍选区系统重做：
  - **手持木棍激活**（同时要求穿齐全套 + 已开启至少一个选区技能）；**左键点两次**取两个角成区；**潜行 + 左键**清除当前模式的选区；**选区期间取消原版攻击/挖掘**（否则木棍会把方块敲掉，用 `InputEvent.InteractionKeyMappingTriggered` + `setCanceled/setSwingHand`）。
  - **每个模式各记一个选区**（放置 / 挖掘 / 攻击 / 防护互不覆盖），服务端按玩家 UUID + 模式索引保存；**体积上限 131072 格**（与参考模组一致），超出只提示不执行。
  - **线框渲染**：`RenderLevelStageEvent`（AFTER_TRANSLUCENT_BLOCKS）+ `LevelRenderer.renderLineBox(PoseStack, VertexConsumer, AABB, r, g, b, a)` 绘制选区框，颜色按模式区分（放置=金黄 `0xFFFFD24A` / 挖掘=青 `0xFF55FFFF` / 攻击=红 `0xFFFF5555` / 防护=绿 `0xFF55FF55`，沿用参考模组）；第一角用白色小框提示。
  - **快捷键改为**：**H** = 切换选区模式（放置→挖掘→攻击→防护，actionbar 提示）；**N** = 执行当前模式的选区操作。原 2.12.0 的 N/B 双快捷键取消（B 不再需要）。
  - **选区操作**：放置/挖掘把选区内方块入队，之后每 tick 最多 256 格分批处理；攻击模式 = 选区内敌对生物每 20 tick 受一次你的攻击伤害；防护模式 = 选区内友好生物免疫你的伤害（均为持续生效，按 N 只提示）。
  - 新增/改写 19 组语言键（模式名、提示语、按键名、技能说明），并把两条选区技能的说明从"半径 8 格"改写为"木棍框选 + N 执行"。
  - **踩坑记录**：① `ShapeRenderer` 在 1.21.1 <b>不存在</b>，画线框要用 `net.minecraft.client.renderer.LevelRenderer.renderLineBox(...)`（静态方法）；`MultiBufferSource` 在 `net.minecraft.client.renderer` 包下，不是 `com.mojang.blaze3d.vertex`。② **批量改语言文件时必须保留原行尾逗号**——用脚本重建整行时若丢掉逗号，非末行就缺分隔符，整个 JSON 非法（本次已踩，回滚重做并在写入前先做 JSON 校验）。
- 2.12.1 → **2.12.2（选区可视化与交互补齐到参考模组的水平）**：用户反馈"选区做得不好"。对照参考模组的 `ZoneSkillRenderer` / `StickToolHudRenderer`，补齐了此前缺的四样：
  - **双层线框**：外层压暗色并外扩 0.002，内层亮色（与参考模组"外金 + 内亮金"同一手法），颜色仍按模式区分。
  - **半透明填充**：`LevelRenderer.renderVoxelShape(pose, fill, shape, 0,0,0, r,g,b, 0.35F, false)` + `RenderType.debugFilledBox()`，参考模组的填充 alpha 同为 0.35。
  - **第二角预览**：选完第一角后，**视线所指处实时以青框预览**将要生成的选区（参考模组预览色 `0.15/0.95/1.0`），配合第一角的白色小框。
  - **右下角功能面板**（移植 `StickToolHudRenderer`）：彩色圆点 + 当前模式名 + 一行操作提示 + **选区尺寸（体积与 X×Y×Z）**；黑底 `0xAA000000`、距底部 35px、右缘 4px，与参考模组一致。
  - **切换模式自动跳过未解锁的模式**（参考模组的 `nextUnlockedStickMode` 行为）：H 键只在已开启的选区技能之间循环，不会停在点了也没用的模式上。
  - **木棍判定放宽为主手或副手**（参考模组同款：副手只做状态查看/选区，不抢交互）。
- 2.12.2 → **2.12.3（按用户要求：整体删除选区功能）**：
  - **删除的功能**：机械共鸣里的 4 个选区技能（选区放置 / 选区挖掘 / 选区攻击 / 防护选区）、木棍框选系统、选区线框渲染、右下角选区面板、`H`（切模式）与 `N`（执行）快捷键、全部选区网络包与语言键。
  - **删除的文件**：`client/ArmorZoneClient.java`、`armor/skill/ArmorZoneData.java`；从 `ArmorSkillMessages` / `ArmorSkillHandler` / `ArmorSkills` / `ArmorSkillDef` / `WandKeyBindings` / `ArmorKeyHandler` 中移除全部选区代码。
  - **技能总数 85 → 81**（基础 15 / 增幅 15 / 终极 21 / 特殊 22 / **机械共鸣 8**）；语言键 490 → **458**（zh/en 双向一致，差异 0）。
  - **机械共鸣的 8 个「共鸣」开关保留**（生物掉落 / 方块掉落 / 战利品爆炸 / 经验 / 刷怪蛋 / 头颅 / 自动熔炼 / 工具不毁），机器继承判定 `effectAllowed` 不受影响。
  - **为什么删**：2.12.0 的固定半径版不好用；2.12.1/2.12.2 改成木棍框选后又出现两次闪退（`IllegalStateException: Not building!`——在按区块层派发的 `AFTER_TRANSLUCENT_BLOCKS` 阶段调用 `endBatch` 污染了帧内 BufferBuilder 状态）。用户判定这个功能不值得继续投入，故整体移除。
- 2.12.3 → **2.13.0（技能树阶段 4：光环 11 个）**：最后一个大类补齐，`AURA` 分类实际启用（此前枚举里有 AURA 但没用过）。
  - **杀戮领域** `aura_damage`（50 级）：每 200 tick 对半径 20 格内敌对生物造成 `攻击力 × (1+10%/级)` 伤害。
  - **修罗杀域** `aura_empower`（50 级）：光环伤害再乘 `(1+10%/级)`（与杀戮领域相乘，不叠加饱和）。
  - **疾攻之势** `aura_speed`（10 级）：每级让脉动间隔 **-10%**（200 → 最低 20 tick）。
  - **回春妙手** `aura_heal`（50 级）：每次脉动治疗自己 + 半径 10 格内友方，治疗量 = 最大生命 × 2%/级。
  - **汲灵之环** `aura_xp`（50 级）：每次脉动给 `1×等级` 点经验。
  - **吸星大法** `aura_magnet`（1 级）：每 tick 吸附半径 20 格内的掉落物与经验球，**每 tick 最多 64 个**（与参考模组 `magnetMaxPerTick` 一致），拉向玩家而非瞬移。
  - **定身神域** `aura_lock`（1 级）：免疫击退（`LivingKnockBackEvent`）+ 免疫一切传送（`EntityTeleportEvent`，覆盖末影珍珠/末影人/指令）。
  - **虚空诛灭** `aura_void`（50 级）：半径 **50 格**内血量低于 `5%+1%/级` 的生物直接处决。
  - **挪移术** `aura_loot_vacuum` + **搬运术** `container_haul`：**潜行 + 右键容器**绑定搬运目标（存在玩家持久数据 `godofthings_haul_pos`，随存档保存，无需额外附件注册）；挪移术让吸附到的掉落物直接进容器，搬运术每 20 tick 把背包物品送进容器。容器填充为手写槽位逻辑（`canPlaceItem` + 同类堆叠），不依赖版本间签名变动的 `ContainerHelper`。
  - **净化领域** `purify_field`（1 级）：每 60 tick 清除自己 + 半径 8 格内玩家各一个负面效果。
  - **脉动错峰**：用 `(gameTime + player.getId()) % interval == 0` 按玩家错峰（参考模组的多人优化手法），避免同 tick 集体做球扫描造成周期性尖峰。
  - **技能总数 81 → 92**（基础 15 / 增幅 15 / 终极 21 / 特殊 22 / **光环 11** / 机械共鸣 8）；界面扩为 **7 个标签页**（新增「光环」，紫色 `0xFF3A2B4A`/`0xFF4E3A63`，强调色 `0xFFC79BFF`）；语言键 458 → **481**（zh/en 差异 0）。
  - **踩坑**：1.21.1 里 `ContainerHelper.insertItemStacked(...)` 签名与旧版不同（编译报"找不到符号"）→ 改为手写槽位填充（`getContainerSize`/`canPlaceItem`/`getItem`/`setItem`/`isSameItemSameComponents`），完全不依赖易变的辅助类。
- 2.13.0 → **2.14.0（技能树阶段 5：魔法增幅 26 个 —— 技能树全类别收官）**：依赖铁魔法 / 新生魔艺 / Goety 的 26 个魔法增幅技能落地。三个 mod 均已安装（Iron's Spells 3.16.3、Ars Nouveau 5.13.1、Goety 3.1.5.1）。
  - **核心设计：零编译依赖的属性桥接**（`MagicAttributeBridge`）。NeoForge 中所有属性（含其它 mod 注册的）最终都在原版属性注册表里，因此只要知道 **属性 ID 字符串**就能取到：
    `BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.tryParse("irons_spellbooks:max_mana"))`
    → **完全不需要 import 三个 mod 的任何类**，也就不存在 `NoClassDefFoundError`（这正是本项目 AE2 那次崩溃的根因），天然满足**可选依赖**要求：**装了生效、没装自动跳过、绝不影响启动**。
  - **两级解析**：先按候选 ID 精确匹配，全部失败则**在该 mod 命名空间内按关键词扫描属性**（如 `irons_spellbooks` + `["fire","power"]`），即使 mod 更新改名也大概率仍能命中；实在找不到就**静默跳过**。
  - **独立事件处理器** `MagicAttributeHandler`：穿齐全套 → 每 20 tick 幂等挂/更新属性修正（先移除再添加，修正器 ID `godofthings:magic_<技能>`）；脱下套装 → 立刻全部移除。**完全不动已有技能 tick 逻辑**，异常一律吞掉不外抛。
  - **26 个技能与其属性**：铁魔法 13 个（`max_mana` +10/级、`mana_regen` +0.05/级、`cast_time_reduction` +0.5%/级、`cooldown_reduction` +0.5%/级、九系 `*_spell_power` 各 +5%/级）；Goety 11 个（焦点强度 +5%/级、灵魂节约 -2%/级、九系法术强度各 +5%/级，按 `goety:*` 候选 + 关键词兜底）；新生魔艺 + 通用 2 个（魔力上限 / 魔力回复，按 `ars_nouveau:*` 候选 + 兜底）。
  - **技能总数 92 → 118**（基础 15 / 增幅 15 / **魔法增幅 26** / 终极 21 / 特殊 22 / 光环 11 / 机械 8）；界面扩为 **8 个标签页**（新增「魔法增幅」，蓝紫 `0xFF2A2E4A`/`0xFF3B4066`，强调色 `0xFF8AA8FF`）；语言键 481 → **533**（zh/en 差异 0）。
  - 说明：这是"**可选依赖**"而不是"热插拔" —— 装了才启用、没装自动隐藏，但**加减 mod 必须重启游戏**（Minecraft 模组在启动时一次性加载）。
  - **踩坑**：`player.getAttribute(...)` 在 1.21.1 需要 **`Holder<Attribute>`** 而不是 `Attribute` → 用 `BuiltInRegistries.ATTRIBUTE.getHolder(rl)` 与 `wrapAsHolder(...)`；`List.of(keywords)` 会把已是的 `List<String>` 再包一层导致"推论变量 E 具有不兼容的上限"，应直接传 `keywords`。
- 2.14.0 → **2.15.0（大清理：删光环 / 精简特殊被动 / 删 5 个终极节点 / 修岩浆火焰覆盖层）**
  1. **完全删除光环（AURA 11 个技能）**：`ArmorSkillHandler` 中的整段光环运行时（脉动/磁吸/定身/净化/容器绑定）、`ArmorSkillEngine` 中的光环数值 API、界面「光环」标签页、全部光环语言键一并移除。
  2. **特殊被动栏删除**：仅保留 6 项并**移入套装功能**（无等级、只有开关，占用 bit 12–17）：**万民敬仰 / 发光 / 暴食 / 无限交易 / 村民大师 / 碧波清眸**（原技能等级一律按最高级生效，因为改成开关后恒为"满级"）。其余 16 个特殊被动（长臂善舞 / 夜视 / 饱食 / 水下呼吸 / 黑暗视觉 / 烈焰不侵 / 奥术防护 6 件套 / 附魔三件套 / 破法之刃 / 驱法破咒）全部移除。实现落在新的 `ArmorExtraFeatures`（服务端 5 项）+ `FogRendererMixin`（碧波清眸，客户端读同一个开关位）。
  3. **终极节点删除 5 个**：不朽铭文 / 万物可掘 / 万载不磨 / 虚空神体 / 凤凰涅槃。随之删除 `BlockStateMixin`（万物可掘的方块 Mixin）与 `mixins.json` 中的注册；`machine_unbreakable`（工具不毁·共鸣）因对应技能已删而一并移除，避免出现点了没反应的死节点。
  4. **修复岩浆中的火焰覆盖层**：新增客户端 `ScreenEffectRendererMixin`，注入 `ScreenEffectRenderer.renderScreenEffect` HEAD —— 当**开着「火焰熔岩免疫」且穿齐全套**时，若玩家着火 / 浸在岩浆里（`isOnFire` / `isInLava` / `getRemainingFireTicks() > 0`）则**取消整层屏幕覆盖贴图**。此前只做了伤害与雾效免疫，那层橙色火焰纹理会照旧绘制挡视野。
  - **技能总数 118 → 79**（基础 15 / 增幅 15 / 魔法增幅 26 / 终极 16 / 机械共鸣 7；光环与特殊被动两栏清空）；**套装功能 12 → 18 项**；界面标签页 **8 → 6**（基础属性 / 特殊增幅 / 终极节点 / 机械共鸣 / 魔法增幅 / 套装功能）；语言键 533 → **461**（zh/en 差异 0）。
  - **实现手法（可复用）**：删除技能时，**把对已删技能常量的判定统一替换为常量**（`ArmorSkillData.isEnabled(..., ArmorSkills.X)` → `false`、`effectiveLevel(...)` → `0`、`ArmorSkillEngine.isOn(...)` → `false`），既保证编译通过、又让对应功能立即失效，风险远低于逐块删代码。
  - **踩坑**：正则里用 `$` 锚点匹配行尾在 CRLF 文件上会失败（行尾是 `\r`），要用 `[ \t]*\r?$`；另外 **PowerShell 的 `-match` 默认不区分大小写**，用 `village_hero` 去检查 `VILLAGE_HERO` 会得到假阳性，必须用 `-cmatch` 或 `[regex]::Matches`。
- 2.15.0 → **2.16.0（神之工具获得「时间加速」—— 移植自万象担架的荒辰移晷之杖）**
  - **来源与判断**：`D:\下载\UselessStretcher-main.zip`（万象担架 1.4.8，130 个 Java 文件 / 12,419 行）。荒辰移晷之杖 `WondrousStaffItem` **继承上游「造化垂青之杖」`EndlessBeafItem`**（来自 Useless Mod 本体，**本机并未安装**），它自己的增量是**时间加速 / 生物加速 / 天地加速 / 召唤 / 战利品箱刷新**。因此可移植且值得移植的是**加速子系统**；"多工具"部分我们神之工具本来就有（模式轮盘 / 精准采集 / 连锁挖掘 / AE 联动）。
  - **移植的核心机制**（`WandAcceleration`）：对一个方块连续驱动 N 次"它一 tick 该做的事"——
    · **避雷针**：直接生成真实落雷（不依赖天气，等价 `/summon lightning_bolt`），**单次上限 8 道**防 1024 倍刷爆实体；
    · **AE 机器**：走 AE 自己的网格刻 —— `GridHelper.getNodeHost(level,pos)` → `IGridNode.getService(IGridTickable.class)` → `tickingRequest(node, 1)`，返回 `SLEEP` 的端点当次摘除；同一节点多面重复要按**身份去重**（`IdentityHashMap`）。**我们本来就是 AE2 硬依赖，这条直接可用**；
    · **有方块实体的方块**：取 `BlockState.getTicker(...)` 反复调用；方块被替换/移除立即停手；
    · **随机刻方块**（作物/树苗）：反复 `randomTick`。**一次虚拟刻 = 一次 randomTick**——上游注释特别强调：再乘一次原版 1/1365 的区块抽样概率会让作物慢 1365 倍，这个坑必须避开。
  - **与上游的实现差异（刻意）**：上游为每个被加速目标生成一个自定义实体 `WondrousStaffAccelerationEntity` 托管计时（25KB + 22KB）；这里改为**纯服务端坐标标记表**（`Map<维度, Map<坐标, {速度,剩余tick}>>`）——不需要自定义实体、渲染器与额外网络包，语义等价而实现更轻。
  - **时间预算**：每个服务器 tick 加速总耗时上限 **3ms**（`BUDGET_NANOS`），超预算立即停止本轮、余下下个 tick 继续 —— **1024 倍也不会拖崩服务器**。
  - **操作**：潜行 + 右键方块挂加速（默认 **2×**，持续 **600 tick = 30 秒**）；**对同一方块再按一次倍率翻倍**（2→4→8→…→1024→回到 2）；潜行 + 右键避雷针 = 引雷；**看向天空**（`getXRot() < -40°`）潜行 + 右键空气 = 推进时间 100 tick。仅在**主手或副手拿着神之工具**时生效。
  - 语言键 461 → **463**（zh/en 差异 0）。
  - **尚未移植（需确认是否要）**：召唤模式 GUI（`WondrousStaffSummonScreen` 选生物召唤）、战利品箱刷新（`WondrousStaffLootRefresh`）、加速配置 GUI（倍率/持续模式/附加功能面板）、生物加速（需反复驱动实体 tick，风险较高）、范围加速存档（`RangeAccelerationSavedData` 37KB）。
- 2.16.0 → **2.17.0（神之工具完整移植：直接继承上游「造化垂青之杖」）**
  - **做法**：把 `[无用之物] useless_mod-1.21.1-2.4.0.jar` 与 `useless_stretcher-1.21.1-1.4.8.jar` 以 `compileOnly files(...)` 接入（与本项目既有的 JEI/EMI/AE2 同一模式），然后 **`GodFavorWandItem` 的父类从 `DiggerItem` 换成上游的 `EndlessBeafItem`** —— 一个继承即取得上游全部多工具能力（传送 / 耕地 / 作物收割 / 剪刀 / 打火石 / 催熟 / 强制生长 / 自动点击 / 手杖链接 / 附魔刷新 / 工具模式轮盘 …）。
  - **6 个模式变体一一对应上游 `ToolTypeMode`**：`god_favor_wand`→`OMNITOOL_MODE`、`_wrench`→`WRENCH_MODE`、`_screwdriver`→`SCREWDRIVER_MODE`、`_mallet`→`MALLET_MODE`、`_crowbar`→`CROWBAR_MODE`、`_hammer`→`HAMMER_MODE`；注册由 `registerItem(name, GodFavorWandItem::new)` 改为 `register(name, () -> new GodFavorWandItem(模式))`（因为上游构造不接收 `Item.Properties`）。
  - **名字保持「神之工具」**：上游 `getName` 会返回「造化垂青之杖」，本类原有的 `getName` 覆写继续生效并优先返回我们自己的语言键，因此 6 个变体仍显示「神之工具」系列。
  - **必需依赖**：`neoforge.mods.toml` 新增 `[[dependencies.godofthings]] modId = "useless_mod" type = "required"` —— 我们继承了它的类，缺上游会 `NoClassDefFoundError`。
  - **代价（如实说明）**：上游构造内部用 `createProperties` 生成属性，**不再接收 `Item.Properties`**，因此原先的 `stacksTo(1)` / `EPIC` / `UNBREAKABLE(false)` 改由上游决定。
  - **坑**：PowerShell 里带 `[ ]` 的文件名必须用 **`-LiteralPath`**（`Copy-Item -Path` 会把 `[无用之物]` 当通配符字符组 → **静默不复制**，而且后续 `gradlew compileJava` 因为没有代码引用它依然 BUILD SUCCESSFUL，形成"三重假成功"）。
- 2.17.0 → **2.18.0（荒辰移晷之杖附加功能收尾：战利品箱刷新 / 生物加速 / 树叶掉落 / 倍率配置）**
  - **战利品箱刷新**（移植万象担架 `WondrousStaffLootRefresh` 190 行）：潜行 + 右键战利品容器 → `clearContent()` → `setLootTable(源表, 随机种子)` → `unpackLootTable(player)`（上游同款三件套），**双联箱两半一起刷**。
    - ⚠️ **与上游的差异**：上游用一个 Mixin 在"容器首次打开、原版即将丢弃战利品表之前"把表记进持久数据；我们**没有加那个 Mixin**，改为**在玩家手拿神之工具右键容器的瞬间记账**（那时表还在，存进方块实体的持久数据 `godofthings:wand_source_loot_table`）。因此行为是：**第一次右键 = 记住并刷新；之后每次右键 = 按记住的表重刷**。
  - **生物加速**：潜行 + 右键生物（非玩家）→ 该生物被反复驱动 `entity.tick()`（倍率-1 次，**单实体每 tick 上限 8 次**），受同一个 3ms 时间预算约束；实体消失/异常自动摘除标记。
  - **树叶掉落**：破坏**树叶**（`BlockTags.LEAVES`）时，若手持神之工具则有概率掉出神之工具本体。上游默认概率是 `0.00001`（十万分之一，实际几乎见不到），这里**调到 1/2000** 让它真的能被玩家见到。
  - **倍率配置**：倍率存在物品自身的 `CUSTOM_DATA`（键 `godofthings:wand_speed`）；**潜行 + 右键同一方块会刷新并翻倍**（2→4→…→1024→回到 2）。
  - 新增语言键 461 → **469**（zh/en 差异 0）。
  - **未做（如实说明）**：上游的三个**图形界面**没有移植 —— 召唤模式 GUI（`WondrousStaffSummonScreen` 选生物召唤）、加速配置 GUI（`WondrousStaffConfigScreen`）、范围加速存档与预览（`RangeAccelerationSavedData` 37KB + `RangeAccelerationPreview`）。功能层面已由上面的交互覆盖；这三个是纯 UI/存档层。
- 2.18.0 → **2.19.0（神之工具去掉前置 mod 依赖，改为独立实现）**
  - **用户要求**：不要前置 mod。原 v2.17.0 让 `GodFavorWandItem extends` 上游 `EndlessBeafItem`，等于把「无用之物」变成**必需依赖**（缺了 `NoClassDefFoundError`）。
  - **撤销继承**：类声明还原为 `extends DiggerItem`，构造还原为 `GodFavorWandItem(Item.Properties)`（恢复 `stacksTo(1)` / `EPIC` / `UNBREAKABLE(false)`）；6 个变体注册还原为 `ITEMS.registerItem(name, GodFavorWandItem::new)`。
  - **依赖清理**：`neoforge.mods.toml` 的 `[[dependencies.godofthings]] modId = "useless_mod"` 整块删除；`build.gradle` 的两个 `compileOnly files(libs/useless*)` 删除；`libs/` 下两个上游 jar 删除。
  - **多工具便利功能改为自实现**（不再借用上游）：
    · **成熟作物** → 右键收割并自动补种（`Block.getDrops` + `CropBlock.getStateForAge(0)`）
    · **未成熟作物** → 右键催熟到满级
    · **泥土 / 草方块**（上方为空）→ 右键开垦为耕地
    · 工具挖掘能力（镐/斧/铲/锄/剪/打火石）仍由本类原有的 `canPerformAction` 提供 —— 那本来就是自实现的。
  - 自研功能全部保留（互不依赖上游）：**时间加速**（方块 / AE 机器 / 避雷针 / 随机刻 / 时间）、**战利品箱刷新**、**生物加速**、**树叶掉落**、**倍率配置**。
  - 语言键 469 → **472**（zh/en 差异 0）。
  - **与上游的差异（如实说明）**：上游 `EndlessBeafItem` 里的「传送 / 手杖链接 / 自动点击 / 附魔刷新 / 剪刀 / 打火石开关」等模块**没有逐个重写**；需要哪个再单独补。
- 2.19.0 → **2.19.1（让神之工具的能力"看得见"：物品提示能力表 + 持握 HUD）**
  - **用户反馈**："怎么神之工具没有任何变化"。
  - **核实**：`godofthings-2.19.0.jar`（637 条目）内 `WandAcceleration` / `WandAccelerationHandler` / `WandFeatureHandler` / `WandLootRefresh` **均已在包内**，`@EventBusSubscriber` 与 `@SubscribeEvent` 注解正确 —— **代码是生效的**。
  - **真正的问题是我的设计失误**：为了满足"不要前置 mod"，v2.19.0 撤销了上游继承，**物品本身必然回到原样**；而新增能力全部藏在"潜行 + 右键"的隐藏交互里，**既没有物品提示也没有任何界面**，玩家无从察觉。
  - **本轮修法（对齐上游的 `WondrousStaffHud`）**：
    · **物品提示**：`appendHoverText` 追加「荒辰移晷」能力表（加速/刷新/生物/农务四行 + 当前倍率），**悬停即见**；
    · **持握 HUD**：新增 `com.godofthings.client.WandHud`，`RenderGuiEvent.Post` 绘制右下角常驻面板（距底 30px、右缘 4px、行高 10、底色 `0xAA000000`，与参考模组同款布局），显示能力与当前加速倍率；界面打开或隐藏 HUD 时不绘制。
  - 新增语言键 470 → **480**（zh/en 差异 0）。
- 2.19.1 → **2.19.2（神之工具加速配置界面：H 键打开，可调倍率与持续模式）**
  - **对照上游** {@code WondrousStaffConfigScreen}：新增客户端界面 `WandConfigScreen`，**H 键打开**。
  - **可调项**：加速倍率 10 档（2 / 4 / 8 / 16 / 32 / 64 / 128 / 256 / 512 / 1024×，◀ ▶ 切换）、**持续模式**（关 = 只加速 30 秒；开 = 永久加速）。
  - **数据落地**：界面改动经新增的 C2S 包 `WandConfigPayload` 发到服务端，写入手持神之工具的 `CUSTOM_DATA`（键 `godofthings:wand_speed` / `godofthings:wand_permanent`）。
    **为什么必须发包**：`ItemStack` 的修改是服务端权威的，客户端改不会同步；而加速标记建立在服务端，必须由客户端把配置发上来。
  - `WandFeatureHandler` 的方块加速已读取「持续模式」：开启时标记为永久（不自动结束）。
  - 新增按键 **H**（`key.godofthings.wand_config`，默认 H，可在原版按键设置里改）。
  - 新增语言键 480 → **488**（zh/en 差异 0）。
  - **踩坑**：以现有按键常量为模板批量生成新按键时，`event.register(X)` 需要写成 `event.register(X.get())`（`Lazy<KeyMapping>` 要解包），漏了 `.get()` 会报 `Lazy<KeyMapping> 无法转换为 KeyMapping`。
- 2.19.2 → **2.20.0（照抄万象担架的荒辰移晷之杖子系统，共 65 个文件 / 7,038 行）**
  - **用户要求**：不要自创，完整照抄别人的代码。
  - **前期误判的纠正**：我先前判断"抄不了"是因为只看了**本体物品**（`EndlessBeafItem` 依赖上游 25 个类）。实测荒辰移晷相关 **32 个文件里 24 个完全不依赖上游 mod**（`✅ 0` import），只有 8 个用到极少数符号。因此**加速/配置界面/召唤界面/树叶掉落/HUD/范围加速存档等约 4,500 行可以逐字照抄**。
  - **照抄方式**（保持原代码不动，只做最小适配）：
    1. 从 `.ref/useless/UselessStretcher-main` 复制 75 个零上游依赖文件到 `com/godofthings/wand/`，**仅改包名** `com.sorrowmist.useless.stretcher` → `com.godofthings.wand`；
    2. 从上游源码照抄 2 个小枚举 `ToolTypeMode`(24 行) / `EnchantMode`(15 行) 到 `com.godofthings.wand.api.enums.tool`；
    3. 为个别上游符号写**垫片**，让照抄的代码一个字不改：
       · `WandComponents` —— 顶替 `UComponents.BeefTimeAccelerationEnabledComponent`（读 CUSTOM_DATA）；
       · `Network` / `com.godofthings.wand.network.Network` —— 顶替原 mod 的网络类，按**原签名**留空实现；
       · `ModItems` —— 顶替原物品注册表，字段**全部指向本模组的「神之工具」**；
       · `UselessStretcherMod` —— 顶替原主类，`MODID` 指向本项目；
    4. 手杖本体的引用 `WondrousStaffItem` → 本项目的 `GodFavorWandItem`；
    5. 与手杖无关的子系统（万象方块 / 模具 / 图案装配 / 维度传送方块 / 9 连区块维度）**不引入**，相关文件删除（14+5+3 个）。
  - **结果**：`com.godofthings.wand` 包 **65 个文件 / 7,038 行**，`gradlew compileJava` **BUILD SUCCESSFUL**，且**完全不依赖无用之物 mod**。
  - **尚待接线**（下一步）：注册照抄来的按键（`StretcherKeyBindings`）、网络包（`RangeNetwork`）、客户端事件（`WondrousStaffHud` / `RangeAccelerationPreview` / `WondrousStaffClient`），并把神之工具的交互改为调用照抄来的 `WondrousStaffAcceleration.tryUse(...)`；同时**退役我先前自创的 `WandAcceleration` / `WandConfigScreen` / `WandHud` / `WandFeatureHandler`**（已被照抄版本取代）。
- 2.20.0 → **2.20.1（把照抄的荒辰移晷子系统接上电，并退役我自创的版本）**
  - **接线**（4 处）：
    1. `Godofthings` 构造里调用 `StretcherComponents.init(modEventBus)` —— 注册照抄来的实体类型与数据组件；
    2. `WandMessages.onRegisterPayloads` 里调用 `RangeNetwork.register(registrar)` —— 注册照抄来的全部网络包（范围加速 C2S/S2C）；
    3. `WandKeyBindings.register` 里注册照抄来的按键 `StretcherKeyBindings.WONDROUS_STAFF_MODE`（**X 键** = 打开加速配置界面）；
    4. `godofthings.mixins.json` 注册照抄来的 4 个 Mixin：`RandomizableContainerLootMemoryMixin`（战利品表记忆，刷新战利品的关键）、`MinecartContainerLootMemoryMixin`、`BlockEntityChangedMixin`、`LevelRendererCloudMixin`（客户端）。
  - **自动生效、无需接线的部分**（因为垫片 `UselessStretcherMod.MODID = "godofthings"`）：
    · `WondrousStaffRightClickHandler` —— 潜行右键方块/生物即走照抄的 `WondrousStaffAcceleration.tryUse/tryUseEntity`（它判定 `ModItems.WONDROUS_STAFF.get()`，经垫片正好是**神之工具**）；
    · `WondrousStaffClient` / `RangeAccelerationInteraction` / `RangeAccelerationPreview` / `WondrousStaffHighlight` / `WondrousStaffHud` / `RangeAccelerationTicker` / `WondrousStaffFeatureEvents` / `MyriadItemMigration` —— 客户端渲染、输入、HUD、tick 全部自动订阅；
    · `UselessStretcherClient`（`@Mod(dist=CLIENT)`）—— 自动加载并注册配置界面扩展点。
  - **退役我自创的 7 个类**：`WandAcceleration`、`WandLootRefresh`、`WandFeatureHandler`、`WandAccelerationHandler`、`WandConfigScreen`、`WandHud`、`WandConfigPayload`；同时移除我加的 H 键、物品提示能力表、`getAccelSpeed`。
    → 现在**加速/战利品刷新/生物加速/HUD/配置界面/召唤界面/树叶掉落/范围加速**全部由照抄来的原版代码承担。
  - `gradlew compileJava` **BUILD SUCCESSFUL**。
  - **操作方式（与原 mod 一致）**：**潜行 + 右键方块** = 时间加速；**潜行 + 右键生物** = 生物加速；**潜行 + 右键战利品箱** = 刷新战利品；**X 键** = 打开加速配置界面。
- 2.20.1 → **2.20.2（修闪退：照抄代码的配置文件没注册）**
  - **崩溃现场**（`crash-2026-09-27_15.00.59-client.txt`）：
    ```
    java.lang.IllegalStateException: Cannot get config value before config is loaded.
      at com.godofthings.wand.config.StretcherConfig.highlightSeeThrough(StretcherConfig.java:122)
      at com.godofthings.wand.client.WondrousStaffHighlight.onRender(WondrousStaffHighlight.java:53)
      at net.neoforged.neoforge.client.ClientHooks.dispatchRenderStage
    ```
  - **根因**：原 mod 是在它自己的主类构造里注册配置的
    `container.registerConfig(ModConfig.Type.COMMON, StretcherConfig.COMMON_SPEC);`
    —— 而我们的垫片 `UselessStretcherMod` 只是个提供 `MODID` 的空壳，**配置文件从未注册**，
    于是照抄的客户端渲染代码一读配置就抛异常。**编译期完全看不出来，只有运行到那一行才炸。**
  - **修法**：把原版主类构造里的初始化清单**照抄**到本模组主类（只取我们抄了的那些）：
    ```java
    com.godofthings.wand.init.StretcherComponents.init(modEventBus);
    com.godofthings.wand.init.ModEntities.ENTITIES.register(modEventBus);
    com.godofthings.wand.init.ModCreativeTabs.CREATIVE_TAB.register(modEventBus);
    modContainer.registerConfig(ModConfig.Type.COMMON, StretcherConfig.COMMON_SPEC);
    ```
    原版还有 `ModBlocks`/`ModBlockEntities`/`ModMenuTypes`/`DimensionCompat`/`Network::register`/`ModItemDefaults`
    这 6 项属于**未引入的万象方块与维度子系统**，不需要。
  - **教训（重要）**：照抄一个子系统时，**必须把它主类构造里的初始化清单一起照抄** ——
    配置注册、DeferredRegister 挂载、事件监听器注册都在那里，漏掉任何一项都是"编译通过、运行崩溃"。
  - 日志里另有两条**无害**残留（来自之前装过真 mod 的存档）：`useless_stretcher:wondrous_staff` 旧物品失效、
    `useless_stretcher:*`/`useless_mod:*` 维度条目解码失败 —— 都不影响游戏。
- 2.20.2 → **2.20.3（G 键改为打开照抄的手杖配置界面；模式轮盘让位到 X）**
  - **用户反馈**："人家原版按 G 能打开配置界面"。
  - **实测结论（关键数据）**：按 G 打开的界面是**上游本体的** `ModeWheelScreen`（「模式配置」）——
    **1512 行**，而它只是冰山一角：
    ```
    以 ModeWheelScreen + ModeWheelHandler 为根做传递闭包（BFS 逐级解析上游 import）：
      需要照抄的上游类 = 339 个
      总行数          = 62,802 行
      还牵扯 5 个外部 mod 的深层 API：AE2 / 工业先驱 Industrial Foregoing /
                                       新生魔艺 / JEI / AdvancedAE
    ```
    → **照抄这个界面在物理上不成立**：它不是"一个屏幕"，而是整个无用之物本体的
    **工具模块系统 / 机器系统 / 配方系统**的总入口（`EndlessBeafItem` 1469 行 +
    `BeefToolLayout` 475 + `BeefToolModuleRegistry` 389 + `UComponents` 403 +
    熔炉/合成/被动合成仓等 6 万行）。这正是当初判定"抄上游本体不可行"的同一个问题。
  - **本轮改动**（让"按 G 打开配置界面"立刻成立）：
    · **G 键 = 打开照抄来的手杖配置界面**（`WondrousStaffConfigScreen`，260 行，上一轮已照抄）
      —— 改 `StretcherKeyBindings.WONDROUS_STAFF_MODE` 与 `WondrousStaffClient` 里硬编码的
      `GLFW_KEY_X` 为 `GLFW_KEY_G`（共 3 处）；
    · **模式轮盘（我们自己的 370 行 `ModeWheelScreen`）键位 G → X**，功能不丢。
  - 最终键位：**G** = 手杖加速配置界面；**X** = 工具模式轮盘；其余不变。
- 2.20.3 → **2.20.4（补齐照抄代码的 70 个语言键，否则界面显示原始键名）**
  - **问题**：照抄进来的 65 个文件用了 **69 个语言键**（`gui.useless_stretcher.*` 等），而我们一个都没有 ——
    打开界面看到的全是 `gui.useless_stretcher.staff_config.title` 这种**原始键名**，这也是"看起来还是不一样"的一部分。
  - **修法**：从两个来源按 key 取值合并进本模组语言文件（addon 1.4.8 + 上游 2.4.0 的 `assets/*/lang/`，
    两边合计 1328 个键可查）：
    · `gui.useless_stretcher.*` / `tooltip.useless_stretcher.*` / `entity.*` / `itemGroup.*` / `key.*` → addon 语言文件；
    · `tooltip.useless_mod.*`（9 个，上游工具键位提示）→ 上游语言文件；
    · 唯一两边都没有的 `gui.useless_stretcher.staff_summon.mode` 手工补（召唤模式 / Summon mode）。
  - 语言键 **487 → 557**（zh/en 差异 0）。
  - **教训**：照抄一个模块时，**必须连它的语言文件一起抄** —— 代码里的 `Component.translatable("...")` 只是键名，
    值在 `assets/<modid>/lang/*.json` 里；不搬值 = 界面全是键名。
- 2.20.4 → **2.20.5（按用户要求删除「神之工具」）** —— 删除属于「修复/优化」，按规则**末位 +1**（先例：2.12.2 → 2.12.3 整体删除选区功能）。此前误按「新增小物品」写成 2.21.0，本版改正。
  - **删除内容**：
    · **物品本体** `GodFavorWandItem`（922 行）与其 **6 个变体**注册（`god_favor_wand` / `_wrench` / `_screwdriver` / `_mallet` / `_crowbar` / `_hammer`）；
    · **照抄来的荒辰移晷子系统** `com.godofthings/wand/` **65 个文件 / 7,038 行**（上一版刚抄进来的那套）；
    · 神之工具专属类：`GodFavorWandAe2Helper`、`ModeWheelHandler`、`ModeWheelScreen`、`WandMessages`、`WandClientHooks`、`WandKeyInputHandler`、`WandDamageTypes`；
    · 主类里 27 行（6 变体注册 + 上一版加的 4 行照抄子系统接线）；
    · `godofthings.mixins.json` 里照抄的 4 个 Mixin 注册（现为 mixins=1 / client=3）；
    · **资源**：7 个物品模型 + 材质 + 配方 + 配方进度 + 语言键 16 个（zh/en 各 549，差异 0）；
    · **标签**：22 个标签文件里的 `god_favor_wand*` 条目（`c:tools/*`、`gtceu:tools/*`、`c:shears` 等）。
  - **刻意保留（重要）**：`WandItemUtils` / `WandConfig` / `WandModes` / `MiningUtils` / `ModeManager` / `ToolMode` / `WandKeyBindings` ——
    这些**被神之采矿机（`GodMinerBlock`）与连锁挖掘策略（`ChainMiningStrategy` 等）共用**，删掉会连带弄坏机器；
    其中 `WandItemUtils` 里两处对神之工具的引用已**中性化**（捕捉刷怪蛋功能停用、AE 存储优先交回采矿机自身处理）。
  - 全仓扫描 `GodFavorWandItem|god_favor_wand|com.godofthings.wand`：**零残留**，`compileJava` SUCCESS。
- 2.20.5 → **3.0.0（照抄 useless_mod 的「太初洞见之杖」＝造化杖整体子系统）** —— 属于「系统性新增（大系统）」，按规则**首位 +1、后两位归零**。
  - **目标澄清**：「太初洞见之杖」不是独立物品，而是 useless_mod 里 `endless_beaf_item`（造化杖）在**精准采集附魔形态下的显示名**（时运形态显示为「造化垂青之杖」）。上游共注册 7 个形态物品（本体 / `_no_wrench` / `_wrench` / `_screwdriver` / `_mallet` / `_crowbar` / `_hammer`），约 30 个模式开关与 3 套界面。因此本次移植的是**整个造化杖子系统**。
  - **移植方式：逐字照抄 + 最小接线**（用户要求「可以复制粘贴、不要过多修改、和原版一模一样」）。落入新包 `com.godofthings.beef`，全量 **286 个 class**（约 8,300 行上游源码）。
    · 唯一的机械改写只有两处：包名前缀 `com.sorrowmist.useless` → `com.godofthings.beef`，命名空间 `useless_mod` → `godofthings`（含字符串字面量里的反射类名）。
    · 为不动任何一行照抄代码，新增垫片 `com.godofthings.beef.UselessMod`（只提供上游代码引用的 `MODID` / `id()` / `LOGGER`）。
  - **为什么不能真「全量粘贴」**：该物品的 import 闭包是 **408 个文件 / 68,454 行**（含万象合金炉机器树、40+ 个外部 mod 编译依赖）。实测剔除机器子系统后仍 242 文件，故按「照抄工具本体 + 局部裁剪」执行。
  - **局部裁剪清单（全部有注释标注）**：
    · `UComponents`：去掉 10 个机器专用物品组件（熔炉数据 / 全能样板 / 多方块恢复等）；
    · `EventHandler`：去掉维度配置、合金炉自动搭建、配方索引重建（4 处）；
    · `ClientEventBusSubscriber`：去掉维度传送方块 / 合金炉核心的交互让位判定；
    · `EndlessBeafItem`：去掉「按万象炉配方数放大攻击力」（改为基础值）与「荧光塑料 / 无用玻璃潜行让位」；
    · `ClientPacketHandlers`：去掉 AE 任务进度（机器）处理；
    · `compat/jei/JEIPlugin`：上游 321 行里与造化杖有关的只有 JEI 运行时持有器，故只保留该部分（连锁等价组界面要用它查物品）；
    · 未照抄：`ExternalInventoryStore`（仅机器引用）、`compat/mekanism` 里 4 个机器耦合类、`ArsSourceCompat` / `MiEnergyCompat` / `AeSourceCompat`（需要 19.8MB 新生魔艺 / 6.8MB 现代工业化 / arseng 三个 mod 才能编译，其**反射加载器与桥接口已照抄**，这些 mod 在场时对应端点会走「加载失败」日志分支，其余功能不受影响）。
  - **配套接线**（我们自己的注册处，对应上游 `UselessMod` 的 5 行）：`beef/init/ModItems`（7 个物品）、`ModEntities`、`ModMenuType`、`ModNetwork`（30 个网络包）、`UComponents.init`、3 个配置规格（`godofthings-beef-{common,client,server}.toml`，避开已有的 `godofthings-client.toml`）、`StaffLinkScreen` 菜单界面注册、创造标签新增 7 项。
    · 网络通道版本沿用本模组既有的 `event.registrar("1")`（上游是 `.versioned("13")`；通道版本是模组级设置，非工具逻辑）。
    · 访问转换器 `META-INF/accesstransformer.cfg`（14 条）随源码带入并在 `build.gradle` 声明——造化杖的强制击杀要直接读写 `LivingEntity.dead/deathScore/dropAllDeathLoot` 与 `ServerLevel.entityManager` 等成员。
  - **资源**：8 个物品模型 + 2 张材质（命名空间改写）；语言键从上游合并 **197 条新增 / 20 条覆盖**，并清理 96 条旧工具（已删除的「神之工具」与 useless_stretcher）遗留死键；**zh/en 各 651 键，双向差异 0**。
  - **旧遗留清理（推翻 2.20.5 条目里「刻意保留」的结论）**：全仓核查确认 `ToolMode` / `ModeManager` / `WandModes` / `WandItemUtils` / `WandConfig` / `utils/mining/*` **只被彼此引用**，与采矿机共用一说不成立（`GodMinerBlock` 只用到 `WandItemUtils.enchantHolder` 一个 3 行工具方法，已内联）。故整包删除 14 个文件，`WandKeyBindings` 收窄为只留 4 个非工具按键。
  - **代价与依赖**：`libs/` 新增 14 个 compile-only jar（约 **42 MB**），仅为让上游 `compat/*` 类**逐字编译**；它们全部是可选集成，运行时缺失即自动跳过，不影响造化杖本体。若不需要这些可选集成，可删除对应 jar 与 compat 源码。
  - `gradlew build` SUCCESS，jar 含 885 条目（其中 `com/godofthings/beef` 286 个 class），已自动部署到两个测试实例。
- 3.0.0 → **3.0.1（补上整层漏抄的 Mixin：背包带杖创造飞行等）** —— 修复，按规则**末位 +1**。
  - **用户反馈**：3.0.0 里「背包里放着造化杖就有创造飞行」失效（原版有）。
  - **根因**：3.0.0 只照抄了 Java 代码，**漏掉了上游 `mixin/` 整层**。飞行不是纯事件驱动的——服务端每 tick 授予 `mayfly` 的那部分（`EventHandler.updateBeefToolFlight`）确实抄到了，但让飞行「挂得住」的 5 个 Mixin 全缺：
    · `ServerGamePacketListenerImplFlightMixin`（通用段）：记录客户端自己上报的飞行意图，区分「玩家主动关飞行」与「别的 mod 偷偷清飞行」；
    · `ClientPacketListenerMixin`（客户端）：记录服务端确实授予过飞行，作为拦截闸门；
    · `LocalPlayerMixin`（客户端）：拦掉外部 mod（实测 Re-Avaritia）伪装成玩家操作的 `mayfly` 清除上报；
    · `MultiPlayerGameModeMixin`（客户端）：切游戏模式时原版会本地重置 abilities，这里在重置前后保住飞行；
    · `ServerPlayerGameModeMixin`（通用段）：切换游戏模式后重新授予造化杖飞行。
  - **同时补抄的另外 7 个**（同属造化杖，一并补齐）：`EntityMixin` / `EntityGetterMixin` / `LivingEntityMixin` / `PlayerMixin` / `ServerLevelMixin` / `LevelMixin` / `ClientLevelMixin`（无敌模式与高级隐身的玩家保护：不可选取/不可攻击/不被投射物锁定/不被 `/kill` 清掉/从实体查询与渲染列表里隐藏），以及 `ServerGamePacketListenerImplMixin`（独立服务端段：高级隐身时拦下他人对自己的交互包）。
  - **裁剪**：`LevelMixin` 里让「无用维度」永昼无雨的 3 个注入（`isDay` / `isRaining` / `isThundering`）去掉——那套维度属上游维度子系统，未移植；保留会被改成 godofthings 命名空间，反过来把本模组自己的超平坦/虚空维度变成永昼无雨。
  - **接线**：新增独立配置 `godofthings.beef.mixins.json`（`package: com.godofthings.beef.mixin`），并在 `neoforge.mods.toml` 追加 `[[mixins]]` 块。`injectors.defaultRequire` 沿用本仓库既有的 `0`（注入失败退化为功能缺失而不错杀启动；失败仍会在日志里报 Mixin 错误）。
  - **创造物品栏收窄**：按用户要求，7 个形态只保留本体 `endless_beaf_item` 一格（其余 6 个仍已注册，由模式轮盘运行时切换生成）。
  - **新增配方**（用户要求按下界合金锭自制）：`data/godofthings/recipe/endless_beaf_item.json`，沿用上游原版的 3×3 布局 `ABC/DEF/IGH`，只把上游那 5 个「无用锭（1~5 阶，属机器子系统）」换成**下界合金锭**——即 6 个下界合金锭 + 钻石镐 + 下界合金镐 + 金胡萝卜 + 恶魂之泪；配套 `advancement/recipes/misc/endless_beaf_item.json` 解锁进度（判据改为持有下界合金锭）。
  - `gradlew build` SUCCESS，jar 含 13 个 `com/godofthings/beef/mixin` class 与两个 mixin 配置。
- 3.0.1 → **4.0.0（照抄 useless_stretcher 扩展模组的「荒辰移晷之杖」）** —— 属于「系统性新增（大系统）」，按规则**首位 +1、后两位归零**。
  - **目标**：`D:\下载` 新放入的 `UselessStretcher-main.zip`（扩展模组源码，`mod_id=useless_stretcher`，包 `com.sorrowmist.useless.stretcher`，130 文件 / 12,419 行）+ `useless_stretcher-1.21.1-1.4.8.jar`。该扩展模组里的「杖」是 **`WondrousStaffItem`＝荒辰移晷之杖**（`useless_stretcher:wondrous_staff`），一个**时间加速杖**，且 `extends EndlessBeafItem` —— 即它继承的正是上一版照抄进来的造化杖。
  - **移植方法沿用上一版**：落到 `com.godofthings.beef.stretcher`（因为扩展模组的包本身就是 `com.sorrowmist.useless` 的子包，同一套前缀改写即可让它对核心类的引用**自动指向本模组已照抄的造化杖**）。机械改写仍只有命名空间/包名：`com.sorrowmist.useless` → `com.godofthings.beef`、`useless_stretcher` → `godofthings`、`useless_mod` → `godofthings`。垫片 `UselessStretcherMod` 保留同名类型提供 `MODID`（去掉 `@Mod` 与构造器——同一 modid 不允许两个 `@Mod` 类），注册由新增的 `stretcher/init/StretcherRegistration` 接线。
  - **照抄范围（杖及其全部配套）53 个源文件**：物品 6（杖本体 / 右键处理 / 战利品刷新 / 连锁挖矿上下文 / 教学数据 / 范围回收器）、实体 6（加速实体 + 加速逻辑 + 召唤 + 时间流逝实体 + 树叶奖励实体 + ChangedTick 访问器）、树叶掉落数据、加速预算、范围设置与存档数据（751 行）、客户端 17（HUD / 高亮 / 云层时钟 / 加速配置界面 / 召唤界面 / 范围配置·历史·编辑三界面 / 范围预览 / 交互 / 快捷键 / 界面样式与 AE2 风格控件 / 3 个实体渲染器）、配置 1、事件 3、网络 3（含裁剪版 `Network` 与 `ClientStateReceiver`）、Mixin 8。
  - **Mixin 8 个**（上一版教训：不能再漏）：`StaffMiningDispatcherMixin` / `StaffMiningDropsMixin`（杖的连锁挖掘与掉落）、`BeefTimeAccelerationMixin` / `BeefToolVariantsMixin`（杖接入造化杖的时间加速与工具变体系统）、`ItemStackToolTagsMixin`（杖跟随扳手标签开关，含 Mekanism 配置器）、`RandomizableContainerLootMemoryMixin` / `MinecartContainerLootMemoryMixin`（战利品箱/矿车战利品表记忆，战利品刷新用）、`LevelRendererCloudMixin`（客户端云层时间加速）。独立配置 `godofthings.beef.stretcher.mixins.json` + `mods.toml` 第三段 `[[mixins]]`。
  - **裁剪清单（全部有注释标注）**：
    · 未照抄「万象模具」整条线：`OmniversalMyriadBlock/BlockEntity/Menu/Screen`、`content/mold/*`（5 个）、`event/MyriadWorkQueue`、`MyriadSelectionBatches` 及其 5 个 Mixin（`MoldHubMultiMold` / `MoldMatcherPreparedMolds` / `AdapterUtils*` / `EmptyMyriadMold` / `MyriadContainerMigration`）；
    · 未照抄维度子系统：`dimension/*`（12 文件）+ 4 个维度 Mixin + 维度方块物品；
    · 未照抄「万象担架」本体物品与 `content/ae/AeBindingStore`、`AeMaterialContext`（前者仅被模具方块实体与担架物品使用，后者被模具 `PatternFetcher` 与机器 Mixin 使用）；
    · `client/StretcherHighlight` 照抄后发现它其实是**机器方块高亮**（引用 `MePatternAssemblyBlockEntity` / `PassiveCraftingHatchBlockEntity` / `AdvancedAlloyFurnaceBlockEntity` / 担架物品），与杖无关 → 删除；
    · `Network.java` 裁掉模具三方包（`MyriadStatePayload` / `MyriadActionPayload` / `FullSlotsPayload` 及其处理器），保留杖与范围加速的全部包；`ClientStateReceiver` 同理裁掉 `accept(MyriadStatePayload)` 与 `handleFullSlots`；
    · `UselessModLightningRodMixin` 未照抄：它 `@Mixin(UselessMod.class)` 注入的是**上游主类**的 `onRightClickBlock`（用于抢回被上游最高优先级监听器吃掉的避雷针点击）。本模组没有那个监听器（照抄后 `EventHandler.onBlockInteract` 对避雷针会直接 return，不抢交互），故该 Mixin 无对象可注入。
  - **依赖**：只新增 AE2（时间加速可作用于 AE 机器与 ME 网络端点；AE2 jar 之前已在 `libs/`）。**无需任何新 jar**。
  - **资源与文案**：3 个物品模型（杖本体 / 精准采集变体 / 范围回收器）+ 2 张材质（杖用的是 `endless_beaf_time_item.png`，模型里引用的就是它）；`data/godofthings/advancement/staff_leaf_drop.json`（树叶掉落彩蛋进度）；`c:tools/wrench` 标签并入 `godofthings:wondrous_staff`（共 3 项）。语言键新增 **117 条**（含 59 个配置项），**zh/en 各 914 键，双向差异 0**。
  - **配方（唯一自主设计处）**：上游是 `无序合成(useless_stretcher:useless_stretcher + #c:tools/tools)`，而「万象担架」本体属未移植子系统 → 保持上游的**无序合成结构**不变，只把那个拿不到的原料换成本模组已有的**造化杖**：`godofthings:endless_beaf_item` + `#c:tools/tools` → `godofthings:wondrous_staff`。语义上正好对应「造化杖升级成荒辰移晷之杖」。范围回收器沿用上游设定：无配方，仅 /give。
  - **创造物品栏**：按上一版要求保持精简，只加杖本体与范围回收器各一格。
  - **未做**：AE2 手册（`assets/ae2/ae2guide/useless_stretcher/*`，index + 6 页）未照抄——它是扩展模组的整本手册，其中 `myriad.md` / `stretcher.md` 讲的是未移植的模具与担架，只抄 `staff.md` / `range.md` 会让索引指向不存在的页面。需要的话可以单独做一份只含杖与范围加速的精简手册。
  - `gradlew build` SUCCESS，jar 含 105 个 `com/godofthings/beef/stretcher` class 与 8 个 stretcher Mixin，已自动部署到两个测试实例。
- 4.0.0 → **4.0.1（修复 Mixin 方法描述符里的旧包名）** —— 修复，按规则**末位 +1**。
  - **问题**：`BeefToolVariantsMixin` 里 `@Inject(method = "createForToolMode(...)")` 用的是 **JVM 描述符字符串**，其中类型是**斜杠形式** `Lcom/sorrowmist/useless/api/enums/tool/ToolTypeMode;`。照抄时的机械改写只覆盖了点号形式 `com.sorrowmist.useless`，**没有覆盖斜杠形式** → 该注入的目标方法签名对不上，静默失效（`injectors.defaultRequire=0`），症状是杖无法接入造化杖的工具变体系统（模式轮盘里切形态时不会保持杖的身份）。
  - **修复**：把该描述符改成 `Lcom/godofthings/beef/api/enums/tool/ToolTypeMode;`。
  - **全仓复查**：`grep com/sorrowmist` 与 `grep com\.sorrowmist` 覆盖整个 `src/`，确认**仅此一处**功能性残留（其余全部是 shim 与接线类里的说明性注释）。同时复核了另外 3 个带 `method = "..."` 的 stretcher Mixin（`StaffMiningDispatcherMixin` / `StaffMiningDropsMixin` / `BeefTimeAccelerationMixin`），它们的目标类型都由 import 解析，描述符里只有原版/NeoForge 类型，无遗留。
  - **教训**：照抄含 Mixin 的模组时，包名改写必须同时覆盖**点号形式**（`a.b.C`）与 **JVM 斜杠描述符形式**（`La/b/C;`）——后者只在 `@Inject/@Redirect/@WrapMethod` 的 `method = "..."` 字面量里出现，grep 点号形式查不到。
- 4.0.1 → **4.0.2（与 useless_mod / useless_stretcher 同时安装会启动崩溃：共享 Mixin 冲突）** —— 修复，按规则**末位 +1**。
  - **用户反馈**：同时装 godofthings + useless_mod + useless_stretcher 三个模组时游戏报错崩溃（错误报告见 `D:\下载\错误报告-2026-9-27_17.02.41.zip`）。
  - **崩溃现场**（错误报告 `游戏崩溃前的输出.txt`）：
    ```
    @Redirect conflict. Skipping useless_mod.mixins.json:EntityGetterMixin from mod useless_mod
      ->@Redirect::useless_mod$filterProtectedPlayers(...) with priority 1000,
      already redirected by godofthings.beef.mixins.json:EntityGetterMixin from mod godofthings
      ->@Redirect::godofthings$filterProtectedPlayers(...) with priority 1000
    ...
    Caused by: org.spongepowered.asm.mixin.injection.throwables.InjectionError:
      Critical injection failure: Redirector useless_mod$filterProtectedPlayers(...) in
      useless_mod.mixins.json:EntityGetterMixin from mod useless_mod failed injection check, (0/1) succeeded.
    ```
  - **根因**：本模组的造化杖移植是逐字照抄 useless_mod 的，两边的 `EntityGetterMixin` 都是 `@Redirect` 打在同一批 `EntityGetter#players()` 调用点上。一个指令上只能留一个 `@Redirect`，优先级相同（都是默认 1000）时**先注册的先赢** —— 本模组赢了，于是 useless_mod 的重定向被跳过。而 **useless_mod 的 mixin 配置是 `injectors.defaultRequire = 1`**，被跳过的注入算致命失败 → 直接崩在启动阶段。本模组自己的配置是 `defaultRequire = 0`，所以「被跳过」对本模组无害，对原版是致命的。
  - **修复（确定性让位）**：给 `com.godofthings.beef.mixin.EntityGetterMixin` 加 **`priority = 500`**（低于默认 1000）。这样原版 useless_mod 在场时它的重定向**必胜**、本模组被跳过（只记一条警告）；原版不在场时本模组照常生效。同类问题一并处理：`com.godofthings.beef.stretcher.mixin.LevelRendererCloudMixin` 也加 `priority = 500` —— 扩展模组自带一个打在 `LevelRenderer.ticks` 上的同款 `@Redirect`，两边都写了 `require = 0`（不会崩），压低优先级只是把「谁赢看加载顺序」变成「原版稳赢」，避免云层加速时有时无。
  - **为什么不干脆在检测到原版时整体禁用本模组的 Mixin**：那样确实更「干净」，但会让本模组自己的那把杖**重新失去背包飞行与无敌保护**（3.0.0 的原始 bug，见上一条）—— 因为 useless_mod 的 Mixin 只保护它自己命名空间下的物品。压优先级的方案两头都保住：原版完好，本模组独立安装时功能完整。
  - **全仓排查确认只此一处硬冲突**：扫描本模组全部 21 个 Mixin 的注入类型，只有 `EntityGetterMixin` 用 `@Redirect`（`LevelRendererCloudMixin` 那个两边都 `require=0`）；upstream useless_mod 的 mixin 里同样只有它一个用 `@Redirect`、没有 `@Overwrite`；扩展模组那边用 `@Redirect/@Overwrite/@WrapMethod` 的三个 Mixin 目标分别是 `LevelRenderer`（上面已处理）、`ConfigManager`（上游的类，与本模组同名类不同，且被扩展模组自己的插件在 2.3.7+ 上禁用）、上游的 `MiningDispatcher`（本模组的同名类不同）——均无硬冲突。
  - **运行时实测（关键）**：起了专用服务器，`run-server/mods` 放入 **useless_mod 2.4.4 + useless_stretcher 1.4.8 + AE2 19.2.17 + guideme**，与本模组一起加载（`gradlew runServer`，Gradle 需要代理时用 `JAVA_TOOL_OPTIONS` 传 `-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890` 走 NBVPN，**不改任何配置文件**）。结果：
    · 冲突方向已反转：`Skipping godofthings.beef.mixins.json:EntityGetterMixin from mod godofthings ... with priority 500, already redirected by useless_mod... with priority 1000`；
    · **无 `InjectionError` / `Critical injection failure`**；
    · 四个模组全部加载（God of Things 4.0.2 / Useless Mod 1.21.1-2.4.4 / Useless Stretcher 1.21.1-1.4.7 / AE2 19.2.17），服务器 `Done (6.931s)! For help, type "help"` 正常开服；
    · 日志里与本模组相关的 ERROR/FATAL：**0 条**（仅剩 useless_mod 自己那 3 条 `useless_compact_l9/f9/c9` 战利品表解析错误，与本次无关）。
  - **同时安装时仍存在的无害干扰（已知，不崩）**：
    · `Method overwrite conflict for shouldProtectFromRemoval / isUnsafePosition in useless_mod.mixins.json:EntityMixin ... previously written by com.godofthings.beef.mixin.EntityMixin` —— 两个 `EntityMixin` 的私有辅助方法同名，Mixin 跳过后者；因为两边是同一份逐字代码，跳过的那个调用点仍会解析到本模组合并进去的同名方法，行为一致。只是警告。
    · 两边都有 `@Inject` 的钩子（飞行/隐身/存活保护等）会**各自都执行**；但若本模组先取消（`cancellable`），原版同点的处理器可能不再运行 —— 各自只认自己命名空间下的物品，实际互不影响各自的杖。
    · **X 键被绑三次**（本模组造化杖、本模组荒辰移晷之杖、扩展模组自己的杖各注册一次默认 X），按键设置里会标红冲突，需要玩家手动改键；不影响启动。
    · 三个模组各注册一份同名物品（`godofthings:endless_beaf_item` 与 `useless_mod:endless_beaf_item` 等），创造栏里会看到重复的杖 —— 这是命名空间不同导致的正常现象。
  - `gradlew build` SUCCESS，已自动部署到两个测试实例。
- 4.0.2 → **5.0.0（照抄 useless_mod 的「无用维度」子系统：奇数 / 偶数 / 三维度 + 传送方块）** —— 属于「系统性新增（大系统）」，按规则**首位 +1、后两位归零**。
  - **用户需求**：把「奇数维度方块也移植进来」。先澄清了歧义——两个上游模组都有名字相近的方块：useless_mod 的 `block.useless_mod.teleport_block`＝**「奇数维度传送方块」**（维度名为「奇数维度」），扩展模组的是「四联/九连区块·奇数中心维度传送方块」。用户确认要 **useless_mod 那个**（名字对得上，且本模组的 `ConfigManager` 是整份照抄的，`godofthings.configuration.useless_dimension*`（地板方块黑白名单）等配置键早已存在、但维度本体在 3.0.0 时被我裁掉了，属于悬空配置）。
  - **照抄范围（23 个源文件 / 约 3,400 行，全部逐字照抄，仅改包名与命名空间）**：
    · `world/dimension/` 10 个：`UselessDimensions`（3 个维度键 + 3 个区块生成器注册）、`UselessDimGen{,2,3}`、`AbstractPlasticPlatformGenerator`（179）、`DimensionGenerationConfig`（586，平台/道路/边界/中心标记的完整数据结构 + JSON 序列化）、`PlatformLayout`（233）、`PlatformStyle`（144）、`UselessDimensionConfigManager`（44）、`UselessDimensionConfigSavedData`（73，SavedData 持久化）；
    · `world/teleport/` 4 个：`AbstractDimensionTeleporter`（168，POI 找最近传送点 → 螺旋搜索找安全落点 → 落地自动铺一块传送方块 → 传送）、`UselessDimTeleporter{,2,3}`；
    · `content/blocks/TeleportPadBlock`（58，右键传送 / 潜行右键开配置界面）、`content/menus/DimensionConfigMenu`（479）、`client/gui/DimensionConfigScreen`（754）、`client/gui/PlatformPreview`（588，顶视/剖面预览渲染）、`content/blocks/IColoredBlock`、`api/enums/EnumColor`；
    · `network/DimensionConfigGhostSlotPacket` + `DimensionConfigSubmitPacket`、`init/ModPOIs`（3 个传送方块 POI）。
  - **之前裁掉的维度相关代码随本次一并补回**（这次维度本体进来了，那些裁剪就不再成立）：
    · `EventHandler`：恢复 `onLevelLoad`（维度载入时把已保存的地形配置灌进区块生成器）与 `onServerStarted` 里的 `UselessDimensionConfigManager.applyAll(server)`；
    · `Mixin/LevelMixin`：恢复 `isDay` / `isRaining` / `isThundering` 三个注入（无用维度永远晴天）。`isUselessDimension()` 改为调用 `UselessDimensions.isUselessDimension(level.dimension())`，只认那三个维度键，**不会误伤本模组自己的超平坦 / 虚空维度**（这正是 3.0.0 当初裁掉它的理由，现在用精确判定解决）。
  - **接线**：新建 `beef/init/ModBlocks`（3 个 `TeleportPadBlock`，属性照抄 `.strength(2.0f, 65536.0f).requiresCorrectToolForDrops()`）、`beef/init/ModItems` 追加 3 个方块物品、`ModMenuType` 追加 `DIMENSION_CONFIG_MENU`、`ModNetwork` 追加 2 个包、`ClientModEvents` 注册 `DimensionConfigScreen`、主类注册 `ModBlocks.BLOCKS` / `ModPOIs.POI_TYPES` / `UselessDimensions.init`，创造栏加 3 个传送方块。
  - **资源与数据（全部照抄并改写命名空间）**：3 个 `dimension` JSON + 3 个 `dimension_type` JSON（奇数/偶数/三维度的区别在传送方块与生成器，维度类型同为无天气、固定时间 5000、384 高、overworld 效果）+ 自定义生物群系 `useless_biome`（无特征、无刷怪）+ 3 个方块 state/model/材质 + 3 个战利品表 + 3 个配方 + 3 个配方解锁进度。
  - **配方（用户要求，且上游本来就有）**：上游自带配方，故**照抄上游**而非自行设计——`teleport_block`（奇数）= `AAA/ABA/AAA`（A=木板标签、B=泥土）；`teleport_block_2`（偶数）= `BBB/BAB/BBB`（B=泥土、A=木板）；`teleport_block_3`（三维度）= `BBB/AAA/BBB`（B=木板、A=泥土）。三者用料相同、排布不同，很便宜——与上游一致。
  - **语言键**：新增 69 条（含 62 条维度配置界面键 + 3 个方块名 + 3 个维度名 + 菜单标题），**zh/en 各 983 键，双向差异 0**。名称：奇数维度传送方块 / 偶数维度传送方块 / 三维度传送方块；维度名：奇数维度 / 偶数维度 / 三维度。
  - **运行时实测**：再次起专用服务器（`run-server/mods` 仍放 useless_mod 2.4.4 + useless_stretcher 1.4.8 + AE2 19.2.17 + guideme），结果 `Done (6.708s)!`；**维度 / 维度类型 / 生物群系 / 区块生成器全部零报错**（`dimension_type` 或 `dimension` JSON 若不合法，Minecraft 会像 useless_mod 那 3 条战利品表那样刷 ERROR —— 这里一条都没有）；Mixin 冲突仍是「本模组让位」的正确方向。**未验证**：实际进入维度后的地形生成与传送流程（需要真人玩家操作）。
  - `gradlew build` SUCCESS，jar 含 19 个 `world/dimension` class + 5 个 `world/teleport` class 与全部维度数据/资源，已自动部署到两个测试实例。
- 5.0.0 → **5.1.0（移植 ae2lt 的 fumo 玩偶系统，做成作者自己的皮肤玩偶「HoYooG Fumo」）** —— 新增小物品，按规则**第二位 +1、末位归零**。
  - **用户需求**：研究 `D:\下载\AE2-Lightning-Tech-Reborn-main.zip` 里的「moakiee fumo」，给自己的模组加一个**自己皮肤的玩偶**（皮肤在 `E:\MC\ALL\皮肤\kon_mugi_skin.png`）。
  - **可行性结论（侦察得出）**：上游 `moakiee_fumo` 的贴图就是 **64×64**（标准 Minecraft 皮肤尺寸），模型 `models/block/moakiee_fumo.json`（25 KB）是 **12 个部件**的完整原版玩家模型 —— `Head / Hat Layer / Left Arm (+Layer) / RightArm (+Layer) / Left Leg (+Layer) / Right Leg (+Layer) / Body (+Layer)` —— 且 UV 用的是**标准皮肤布局**（例：头部正面 `uv [2,2,4,4]` 在 64×64 贴图里正好对应像素 8..16，即原版头部正面；帽层 `uv [10,2,12,4]` 对应像素 40..48，即原版帽层正面）。**所以"换一张皮肤贴图 = 换一个玩偶"**，而用户的皮肤同样是 64×64 → 直接可用。
  - **⚠️ 许可问题（动手前已与用户确认）**：ae2lt 与之前照抄的两个模组**许可完全不同**——源码是 **GNU LGPL 3.0**（copyleft），素材是 **CC BY-NC-SA 3.0**（署名 + **禁止商用** + 相同方式共享）；而本模组声明的是 `All Rights Reserved`。已向用户说明后果并给出「照抄 + 保留许可署名」与「自建等效实现、零许可义务」两个方案，**用户选择照抄并保留许可**。因此：那 5 个类**保持 LGPL-3.0**（不得按本模组 ARR 再许可），模型 `hoyoog_fumo.json` **保持 CC BY-NC-SA 3.0（禁止商用）**，均已在文件头与 `THIRD_PARTY_NOTICES.md` 里写明并署名 6 位作者（MOAKIEE、CystrySU、gjmhmm8、_leng、TedXenon、MHanHanBing）。贴图是用户自己的皮肤，不属上游素材，按 ARR 分发。
  - **照抄内容**：`FumoBlock`（facing + waterlogged 的小方块、`ENTITYBLOCK_ANIMATED`、按朝向的 4 个 VoxelShape、羊毛音效、右键切换自转）、`FumoBlockEntity`（自转标记 + `yRot/prevYRot`，6°/tick 客户端推进，存档持久 + 方块更新包同步）、`FumoBlockItem`（`BlockItem implements Equipable`，**可戴在头上**，可选 tooltip）、`FumoBlockRenderer`（BER：自转时先绕 Y 轴旋到中心再转，世界内走 `tesselateBlock`，无 level 时走手写 quad 回退）、`SpinningFumoBakedModel`（`BakedModelWrapper`，仅在 `ItemDisplayContext.HEAD` 时按游戏时间自转 —— **戴头上也会转**）。
  - **裁剪**：上游的「超维猪咪」分支（`HyperdimensionalPigmeePortalLayer` / `HyperdimensionalPigmeeTextureLayer` / 物品侧 `BlockEntityWithoutLevelRenderer`）与另外 4 个玩偶未移植；上游的 `ModBlocks`/`ModItems`/`ModBlockEntities` 三个注册器合并为 `fumo/registry/ModFumos`（注册名与方块实体 ID 与上游同名 `fumo`）。**本 mod 没有 Curios 依赖，故未带上游的 `curios:head` 标签**；头部穿戴走原版 `Equipable`。
  - **命名与配方（用户指定）**：物品显示名 **HoYooG Fumo**（en/zh 同名，与上游「Moakiee Fumo」同样不翻译），id `godofthings:hoyoog_fumo`；配方 **8 个 `#minecraft:wool` + 中心 1 个铁锭**（结构照上游猪咪的 8 围 1），并配了配方解锁进度。
  - **资源**：方块状态（4 朝向）、方块模型（25 KB，玩家 12 部件）、物品模型（带 `display.head` 的 `translation [0,14,0]`）、贴图 = 用户的 `kon_mugi_skin.png`（校验过：**1320 字节、64×64、与源文件逐字节一致**，且在 jar 内复核一致）、战利品表（挖掉掉自己）。
  - **顺手补齐的合规缺口（重要）**：前几轮照抄 useless_mod / useless_stretcher 时**没有把它们的 MIT 许可与署名带进来**；而 useless_mod 的 README 还声明它适配了 **GT New Horizons/PersonalSpace（LGPL-3.0）的边界/道路/中心标记生成模型**（正是 5.0.0 移植的无用维度地形代码），扩展模组则声明其时间加速设计参考了 **JDTE（MIT）**、且荒辰移晷之杖与区块维度方块的**美术素材由「虾比」制作**。本次一并补齐：
    · 新增 `THIRD_PARTY_NOTICES.md`（仓库根目录 + `src/main/resources/META-INF/`，随 jar 分发），逐项写明照抄范围、署名与适用范围；
    · 新增 `LICENSES/`：`useless-mod-MIT.txt`、`useless-stretcher-MIT.txt`、`JDTE-MIT.txt`、`PersonalSpace-LGPL-3.0.txt`、`AE2LT-LGPL-3.0.txt`、`AE2LT-ASSETS-CC-BY-NC-SA-3.0.md`，并镜像到 `META-INF/LICENSE-*` 一起打进 jar（LGPL/MIT/CC 都要求随副本提供许可文本）；
    · README 的「许可」一节加了显式提示：ARR 之外还有这些独立许可的部分，并指向 `THIRD_PARTY_NOTICES.md`。
  - **语言键**：新增 2 条（`block.` 与 `item.godofthings.hoyoog_fumo` = "HoYooG Fumo"），**zh/en 各 985 键，双向差异 0**；README 内容一览加一行。
  - **顺手修掉一处跨模组干扰（4.0.2 那次只做了「让位」，这次根治）**：专用服务器日志显示本模组的 `LevelMixin` 与 `EntityMixin` 里有三个**私有辅助方法**与 useless_mod 的同名副本同名同签名——
    `isUselessDimension`、`shouldProtectFromRemoval`、`isUnsafePosition`。Mixin 只能保留一个，被跳过后**两个模组的注入处理器会共用剩下的那一个**：由于两者实现不同（各自查自己命名空间的物品/维度），等于有一方在用错逻辑。修复方式是把**本模组这三个辅助方法**按 Mixin 的私有成员命名惯例加 `godofthings$` 前缀（上游的注入处理器本来就带 `useless_mod$` 前缀，只有这三个辅助方法漏了），两边即可各自保留、互不干扰。
    · 实测（专用服务器 + useless_mod 2.4.4 + useless_stretcher 1.4.8 + AE2）：`Method overwrite conflict` 警告**从 3 条降到 0 条**；仅剩 6 条**刻意为之**的 `@Redirect conflict. Skipping godofthings...EntityGetterMixin with priority 500`（4.0.2 的让位设计，保持不变）。
  - `gradlew build` SUCCESS，jar 内含 7 个 `com/godofthings/fumo` class 与全部玩偶资源，已自动部署到两个测试实例。
- 5.1.0 → **5.1.1（维度配置界面两处缺失：运行时拼出来的语言键、JEI 拖拽）** —— 修复，按规则**末位 +1**。
  - **用户反馈**：无用维度传送方块的配置界面里 ①「马路 / 多联」显示成英文（实际是原始键名）②没有和原版一样「从 JEI 把物品拖进配置界面」的能力。
  - **问题 ① 根因**：这两个名字的键是**运行时拼出来的** —— `DimensionConfigScreen` 里写的是
    `Component.translatable("gui.godofthings.dimension_config.mode." + key)`，
    真正的键是 `...mode.road` / `...mode.multi`。而我在 5.0.0 合并语言键时，是按「**代码里出现的字面量**」提取键名的，
    这种 `前缀 + 变量` 的键只会提取到那个**带尾点的前缀**，真实键名一个都没提 → 缺键 → 界面回落显示原始键名。
  - **一次性审计（顺手把同类问题全找出来）**：写脚本扫遍全部照抄代码，抓出所有 `"...前缀." + 变量` 形式的键前缀
    （共 5 个：`godofthings.configuration.`、`gui.godofthings.dimension_config.mode.`、
    `gui.godofthings.dimension_config.preview.role.`、`gui.godofthings.wireless_logistics.`、
    `gui.godofthings.wireless_logistics.side.`），再把上游语言文件里**命中这些前缀的全部键**取出来比对，
    结果**共缺 303 条**，分三组：
    · **维度配置界面 10 条**（`mode.road` / `mode.multi` + `preview.role.*` 8 条）——用户报的这条；
    · **无线物流界面 35 条**（`side.*` 六面 + `medium.*` 九种介质 + `flow.*` + `trigger.*` + 各种 `*_label`）——
      **同一个 bug，只是荒辰移晷之杖那套界面用户还没点到**；
    · **配置界面 258 条**（`godofthings.configuration.*`：catalyst/coil/furnace 各阶并行与耗时、各 mod 配方转换开关等）——
      因为 `ConfigManager` 在 v3.0.0 是**整份照抄**的，这些配置项在本模组的配置文件里真实存在，
      界面上就会显示原始键名（此前只补了 144 条，漏了动态拼接与 `.comment`/`.tooltip` 变体）。
    三组共 303 条全部从上游补齐，**zh/en 各 1288 键，双向差异 0**。抽查：`mode.road`=马路、`mode.multi`=多联、
    `preview.role.road`=道路主体、`wireless_logistics.side.north`=北面、`medium.ae_item`=AE 物品。
  - **问题 ② 根因（判断失误，非疏漏）**：v3.0.0 我把上游 `compat/jei/JEIPlugin`（363 行）裁剪成「只有运行时持有器」，
    理由写的是「上游该类里与造化杖有关的只有这个运行时持有器」。**那个判断漏看了 `registerGuiHandlers`** ——
    它注册的**三个** JEI 拖拽处理器（`IGhostIngredientHandler`）分别服务于：
    · `DimensionConfigScreen`（无用维度配置界面，用户报的这条）、
    · `StaffLinkScreen`（无线物流过滤槽）、
    · `ChainGroupScreen`（连锁等价组拖方块）。
    三者都属于已移植子系统，因此是**三个一起丢**。现按上游逐字补回三个处理器（连注释一起照抄），
    挂到现有的 `com.godofthings.beef.compat.jei.JEIPlugin`（保留运行时持有器）上。
    所需的面板公开方法经核对全部已随界面照抄存在（`getMenu().isGhostSlotActive/getGhostSlot/setGhostSlotFromClient`、
    `filterSlotCount/filterSlotScreenX/filterSlotScreenY/filterSlotSize`、`chainGroupCount/isGroupRowVisible/groupDropZoneScreenX/groupDropZoneScreenY/dropZoneSize/addEntryFromBlock`），
    服务端收包链路（`DimensionConfigGhostSlotPacket`）在 5.0.0 已注册。
  - **教训**：① 合并语言键时，**不能只按代码里的完整字面量提取** —— 凡 `前缀 + 变量` 拼出来的键都要单独审计
    （做法：抓 `"...前缀." +` 形态的前缀，再把上游语言文件里命中该前缀的键全量取回比对）；
    ② 裁掉上游某个类之前，要**通读该类的每个 @Override 方法**再判断「哪部分与本次移植有关」，
    否则会像这次一样把整块功能连同理由一起误删。
  - `gradlew build` SUCCESS，已自动部署到两个测试实例。
  - **未验证**：JEI 拖拽是纯客户端交互，需要真人进游戏拖一次；已确认的是编译通过、处理器已注册、面板侧方法齐全、收包链路已就位。
- 5.1.1 → **5.1.2（只保留荒辰移晷之杖 / 删除神之平坦与超平坦维度 / 神之剑三项功能移到杖上并删除神之剑与神之炮）** —— 以删除功能为主，按规则**末位 +1**。
  - **用户需求**：① 两个杖只保留荒辰移晷之杖 ② 删除神之平坦 ③ 神之剑的「吸星 / 吸魂 / 杀戮光环」移到荒辰移晷之杖上，然后删除神之剑与神之炮。
  - **动手前发现的结构性问题（已与用户确认）**：`WondrousStaffItem extends EndlessBeafItem` —— 荒辰移晷之杖是**继承**造化杖基类的，它的采集 / 时运 / 无敌 / 连锁能力全部来自基类。所以「删掉造化杖」只能是删掉那 7 个**物品注册**，基类必须留作内部父类。真正需要决策的是**5 种工具形态**（扳手 / 螺丝刀 / 软锤 / 撬棍 / 铁锤）：上游靠「换成另一个物品」实现。
  - **三个决策点与用户选择**：
    · 工具形态 → **保留 5 种形态能力，都作用在同一个杖上**；
    · 三项功能的界面 → **保留 J 键面板（改名「手杖功能面板」），只留这 3 项**；
    · 神之平坦 → **方块 + 超平坦维度一起删**。
    · 另外告知并确认过：神之剑的**斩首 / 捕捉 / 抢劫 / 秒杀**随剑一起消失（用户点名只移 3 项）。其中「抢劫强度」（`GodSwordItem.applyLooting`）被**神之砍杀**内部调用，故迁为独立工具类保留，否则砍杀会坏。
  - **① 只保留荒辰移晷之杖**：
    · `BeefToolVariants` 改为**不换物品**：`createForToolMode` / `withWrenchTag` 改成 `source.copy()` + 写 `CurrentToolTypeComponent` / `WrenchTagEnabledComponent` 组件（工具形态的能力本来就由该组件决定），`isBaseVariant` 改成 `instanceof EndlessBeafItem`；`ToolTypeModeSwitchPacket` 的「全能工具缺席」回退同步改为「保持原物品」。
    · 删除 7 个物品注册（`endless_beaf_item`、`_no_wrench`、扳手 / 螺丝刀 / 软锤 / 撬棍 / 铁锤）+ 创造栏条目 + 8 个物品模型 + 2 张贴图 + 配方 + 配方解锁进度。
    · **贴图 `endless_beaf_time_item.png` 保留** —— 它正是荒辰移晷之杖自己的贴图（模型 `wondrous_staff.json` 引用它）。
    · 荒辰移晷之杖的**合成配方**：上游是「万象担架 + 工具标签」，上次移植时代入的是造化杖；造化杖没了，锚点改为**下界之星 + `#c:tools/tools`**（无序）—— 下界之星是本模组所有「神之X」机器的招牌材料（8 个配方在用）。
    · **工具标签整批改写**：`c:tools/*`（13 个）、`gtceu:crafting_tools/*`（10 个）、`minecraft:{axes,hoes,pickaxes,shovels,swords,breaks_decorated_pots,cluster_max_harvestables}`、`malum:soul_shatter_capable_weapon` 里原本列着那 7 个变体 → 全部替换为 **`godofthings:wondrous_staff`**（上游是把 7 个变体都列进每个标签，换成单一杖即行为等价）。
    · 打草彩蛋（`GrassWandDropHandler`）改为发放荒辰移晷之杖；其进度 `grass_wand_drop` 图标同步。
    · `EndlessBeafItem.getName` 原本用 `item.godofthings.endless_beaf_item.{fortune,silk_touch}` 两个**旧物品名**（造化垂青之杖 / 太初洞见之杖）——那两个键随物品一起删了，若不改会显示原始键名。改为 `item.godofthings.wondrous_staff.{fortune,silk_touch}` = 「荒辰移晷之杖（时运）」「荒辰移晷之杖（精准采集）」。
  - **② 删除神之平坦 + 超平坦维度**：方块与物品注册、创造栏、`SUPERFLAT_DIMENSION` 维度键、`DimensionSetup` 里超平坦的出生点逻辑、区块生成器 `GodFlatDimGen` 与 `SUPERFLAT_GEN_CODEC`（`GodFlatDimension` 整个文件）、`data/godofthings/dimension|dimension_type/superflat.json`、`worldgen/biome/superflat_biome.json`、方块状态 / 模型 / 两张贴图 / 配方 / 配方解锁进度 / 战利品表、`minecraft:mineable/pickaxe` 标签里那一项。**神之虚空不受影响**（它用原版 `minecraft:flat` 生成器，与 superflat_gen 无关）。
    · 进度 `advancement/dimension.json` 原本专为「进入超平坦维度」而写 → **改为进入虚空维度**（图标换成神之虚空传送器、条件 `to: godofthings:void`、`enter_superflat` → `enter_void`），保住这个成就而不是删掉。
  - **③ 三项功能移到杖上 + 删除神之剑与神之炮**：
    · `SwordEffectHandler`：`findSword` 的判定从 `GodSwordItem` 改为 `WondrousStaffItem`（主手优先，副手兜底）；杀戮光环里原本复用 `GodSwordItem.killEntity`（为兼容斩首 / 捕捉 / 抢劫），现改为**直接用带玩家攻击者的伤害源一击必杀**（`playerAttack`，掉落与经验照常记在玩家头上，掉落表也能读到手持物的抢夺附魔）。
    · J 键面板 `SwordModeScreen` 裁到 3 行（吸星 / 吸魂 / 杀戮光环），行号位移、面板高度 176 → 116、点击命中判定同步；`SwordMessages.SwordMode` 枚举删掉 `BEHEAD/CAPTURE/LOOTING`，两个 switch 与 `findSword` 同步。
    · **保留**私下仍在服务的 `SwordModes` 存储类（含 3 项功能的开关与半径 / 目标类型；另外 3 项功能的存取器已成为惰性代码，未删以免牵动更多文件）。
    · `GodSwordItem` 的 `lootingLevel` / `applyLooting` 迁到新类 **`com.godofthings.item.LootingHelper`**（逐字搬运），神之砍杀的调用改为它 —— 否则砍杀会编译不过。
    · 删除：`GodSwordItem`、`GodCannonItem`、`CannonMessages`、`CannonClientHandler` 四个类 + 两个物品注册 + 创造栏 + 模型 / 贴图 / 配方 / 配方解锁进度 + `minecraft:swords` 里的神之剑 + 其语言键（含 `message.godofthings.cannon.charge_*`）。
  - **语言键**：删 15 条（3 个旧杖名、神之剑 / 神之炮 / 神之平坦 / 超平坦维度 / 蓄力提示 / 超平坦传送提示、斩首·捕捉·抢劫三项），并把**全部文案里残留的「造化杖」批量改成「荒辰移晷之杖」**（19 处，主要是 `godofthings.configuration.beef_*` 配置项说明与工具提示），`gui./key.godofthings.sword_mode` 改为「手杖功能面板」，新增 2 条杖名变体键。**zh/en 各 1275 键，双向差异 0**。
  - README「内容一览」按约定同步：删掉神之剑 / 神之炮 / 造化杖三行，把荒辰移晷之杖改写为唯一杖并写清三项新功能与工具形态的新实现方式，维度行与 J 键说明、目录结构注释一并更新。
  - `gradlew build` SUCCESS，已自动部署到两个测试实例。
  - **未验证**：工具形态切换、J 面板三项功能的实际生效、打草彩蛋——都需要真人进游戏点；已确认编译通过、注册项与标签无悬挂引用（全仓扫描 `endless_beaf_*` / `god_sword` / `god_cannon` / `superflat*` 仅剩说明性注释）。

> **v5.2.0 已作废并整体回滚（不并入版本历史）**：Applied Industrialization（aeind）「样板输入仓」
> 槽位 ×5 曾作为 v5.2.0 **做在本模组里**（一个 Mixin 兼容补丁）。用户的真实要求是
> **直接修改那个第三方模组本身、重新打包后交给他自己去替换**，而不是改动本模组。
> 因此该提交已 `git revert`（9 个文件、-265 行），本模组回到 **5.1.2**，不含任何 aeind 相关代码；
> 用户测试整合包里被误换上的 5.2.0 也已撤销、还原回他原来的 2.15.0。
> 该项需求改为**离线改写 `Applied-Industrialization-v1.2.7.jar` 的字节码后重新打包**交付（见下）。
>
> **教训**：「修改某个模组的功能」有两种完全不同的落点 —— ① 在自己的模组里写兼容补丁（需要双方都装），
> ② 直接改写对方产物并重新打包（对方可原样替换）。动手前必须先问清是哪一种，不要自行假定。
>
> **实际改法（改对方 jar，未纳入本仓库版本控制）**：aeind 没有公开源码、也没有任何配置文件，
> 槽位数是编译期常量 `PATTERN_SLOTS`（javac 会内联），唯一可改点是它调用 AE2 的
> `PatternProviderLogic(节点, 宿主, 槽位数)` 构造器的那一个实参。做法：
> · 工具 `AeindSlotPatcher`（ASM 9.10.1，两遍扫描）：定位「AE2 构造器调用之前的那一条整数压栈指令」
>   再改写，**只动这一个实参**；扩展类构造器里那个无关的 `new List[36]`（隔离房间数组）保持 36 不动。
> · 结果：`HatchPatternProviderLogic` 9 → 45；`ExtendedHatchPatternProviderLogic` 36 → 180。
>   整包 182 个条目中**只有这 2 个 class 内容变化**，其余逐字节一致，modId/版本号/文件名不变 → 可直接覆盖替换。
> · **运行验证**：改后的 jar 放进测试专用服务器（同时装 MI 2.5.8 / ExtendedAE / AE2 19.2.17），
>   开服 `Done (5.9s)`；再用数据包 `forceload add 0 0` + `schedule` 延后 3 秒 `setblock`，
>   在世界上**真正放下这两种方块并回读确认**，两个方块实体的构造器都成功执行 ——
>   日志 `[aeind-test] base-hatch OK (slots 9->45)` / `extended-hatch OK (slots 36->180)`，
>   **零报错、零 VerifyError/ClassFormatError**。（坑：加载函数在区块加载前执行，`setblock` 会静默失败；
>   且该存档出生点不在 (0,0)，必须先 `forceload` 才能放。）
> · 交付物：`.ref/aeind/delivery/`（改好的同名 jar + `原始备份/` + `说明.txt` + `补丁工具/`），
>   同一份已复制到 `D:\下载\aeind-样板槽位5倍\`。用户两个包（客户端整合包 / 服务端全量包）里的
>   原件经哈希核对**仍是未改的原版**，等他自行替换。
> · **未验证**：45 槽的界面滚动手感（aeind 的界面继承 AE2 的 `PatternProviderMenu`，槽位按库存容量
>   动态创建，AE2 样式里样板槽是 `"grid": "HORIZONTAL"` 横向滚动面，理论上可滚动显示）。
- 5.1.2 → **5.1.3（删除神之熔炉的 AE 功能，且让它连不上 AE 网络）** —— 以删除功能为主，按规则**末位 +1**。
  - **用户需求**：「删除神之熔炉的 ae 功能，且不能连接 ae 网络。」
  - **先查清「能连上 AE」到底有几条路**（这是本次的关键，只删一半会留下漏洞）：
    · **线缆直连**：AE2 靠能力 `AECapabilities.IN_WORLD_GRID_NODE_HOST` 找网格节点宿主；
    · **无线并网**：本模组荒辰移晷之杖的「AE 网络连接」模式（`beef/compat/ae/AeDeviceLinker`）——
      它在第 110-116 行**同样先取这个能力**，取不到就提示「该方块还没有可用的 AE 网络节点」并直接返回。
    结论：**撤掉这一个能力注册，两条路同时失效** —— 不需要额外改 linker。
  - **删除清单（三处，共 3 个文件）**：
    · `GodFurnaceBlockEntity`：不再 `implements IGridConnectedBlockEntity`；删掉 `aeEnabled` / `aeNode(AeGridNode)` /
      `aeTick` 三个字段，`isAeEnabled` / `toggleAeEnabled` / `getMainNode` / `saveChanges` / `pushOutputToAe` 五个方法，
      `onLoad`/`setRemoved` 里 `aeNode.create/destroy`（两个覆写因此整段删除）、tick 里每 20 tick 的产物推送、
      NBT 的 `AeEnabled` 读写、以及 **`AECapabilities.IN_WORLD_GRID_NODE_HOST` 的注册**（只留 `ItemHandler.BLOCK`）；
      清理 9 个 `appeng.*` 与 `AeGridNode` 的 import，类注释里写明本机为何与另外 6 台不同。
    · `GodFurnaceMenu`：删掉 AE 同步 `DataSlot`、`cachedAeEnabled`、`isAeEnabled()`、`clickMenuButton` 里的 `buttonId == 7` 分支。
    · `GodFurnaceScreen`：删掉 AE 按钮的常量、绘制与点击分支（buttonId 7）。
  - **保留不动**：其余 6 台（矿机 / 资源机 / 掉落机 / 砍杀 / 合成台 / 吸收）的 AE 并网、`AeGridNode` 类、
    `AeDeviceLinker` 与杖的「AE 网络连接」模式全部照旧。
  - **旧存档兼容**：`loadAdditional` 里不再读 `AeEnabled` 键（旧数据留着不读，不报错）。
    · 已知小尾巴：若某台熔炉**在本次改动前**已被无线并网过，`AeConnectLinkSavedData` 里那条链接记录仍在，
    会变成无效残留（不会有任何功能，只是高亮线可能还画着）；需要的话可以下次顺手清。
  - README「内容一览」的 AE2 兼容一行按约定同步：**7 台 → 6 台**，并标注熔炉已删除 AE。
  - **验证**：编译 0 错误；全仓 grep `appeng|AECapabilities|AeGridNode|IGridConnected|aeEnabled|AeEnabled|aeNode|aeTick`
    在熔炉三个文件里**只剩说明性注释**；熔炉的能力注册只剩 `Capabilities.ItemHandler.BLOCK` ✓；
    专用服务器实跑（含数据包在工作世界放下熔炉并让 tick 正常跑）确认无异常。
- 5.1.3 → **5.1.4（按键设置里显示的是上游模组的名字）** —— 修复，按规则**末位 +1**。
  - **用户反馈**：「杖的配置按键界面为什么用的是无用之物的名字。」
  - **根因（照抄的必然副作用）**：按键的「分类键」也是照抄的字符串，只改了命名空间、**没改它的显示名**：
    · 荒辰移晷之杖的 X 键（`key.godofthings.wondrous_staff_mode` = 打开手杖加速配置）由
      `StretcherKeyBindings.CATEGORY = "key.categories.godofthings"` 归类 —— 这个键来自上游
      `key.categories.useless_stretcher`，语言文件里的值就是上游的模组名「**万象担架** / Useless Stretcher」；
    · 造化杖那 15 个按键（连锁挖掘 / 时运 / 模式轮盘 / 无线物流…）由
      `beef/core/common/KeyBindings.CATEGORY = "key.category.godofthings.useless"` 归类 —— 来自上游
      `key.category.useless_mod.useless`，值是「**无用模组** / Useless Mod」。
    合并上游语言文件时我按「命名空间替换」处理，**键名换了、值里的上游模组名跟着一起搬了过来**，
    于是按键设置里就出现了别人的名字。同类漏网还有 3 处：创造栏 `itemGroup.godofthings`（万象担架，实际未被使用）、
    配置界面标题 `godofthings.configuration.title`（万象担架配置）、范围回收器提示里的「万象担架创造标签」，
    以及一条日志前缀 `Useless Mod optional CPU reflection failed…`。
  - **修法**：
    · **把两个分类合并成一个** —— 造化杖那 15 个键改用 `key.categories.godofthings`（与杖的 X 键同一分类），
      按键设置里本模组移植来的 16 个键只出现**一个分组**，不再分成两半；
    · 该分类的显示名改为本模组自己的名字：**神之物 / God of Things**（与本模组 `mod_name` 一致）；
      删除已无引用的 `key.category.godofthings.useless`；
    · 另外三处一并改：删掉未使用的 `itemGroup.godofthings`、`godofthings.configuration.title` → 「神之物配置」、
      范围回收器提示 → 「…可在 JEI 或神之物创造标签中找到」；日志前缀改成 `God of Things:`。
    · **保留不动**：`key.category.godofthings.wand` =「神之工具」（本模组自己的 4 个按键，不是漏网）；
      以及语言文件里「**无用维度**」系列（`godofthings.configuration.useless_dimension*`、
      `menu.godofthings.dimension_config` = 无用维度配置）—— 那是被移植的那个**维度子系统本身的名字**
      （奇数 / 偶数 / 三维度），属于内容命名而非模组名泄漏，用户未提出异议故不动。
    · **代码注释里保留**上游项目名（`Useless Stretcher` 等）—— 那是移植来源与署名的记录，必须留着。
  - **验证**：编译 0 错误；代码里出现的 2 个分类键逐个在 zh/en 语言文件里核对**全部解析成功**
    （`key.categories.godofthings` = 神之物 / God of Things、`key.category.godofthings.wand` = 神之工具 / God Tool，
    旧的无用模组分类键已不存在）；全仓 grep 确认代码中残留的 `无用/Useless` 只剩注释与「无用维度」内容命名；
    **zh/en 各 1273 键，双向差异 0**。
    · 按键界面是纯客户端显示，需要真人进游戏看：**按键设置 → 神之物** 分组下应能看到 16 个键。
- 5.1.4 → **5.1.5（手杖右键避雷针引雷失效）** —— 修复，按规则**末位 +1**。
  - **用户反馈**：「手杖的右键避雷针引雷功能失效了，这个原版手杖右键避雷针引雷是自然雷。」
  - **排查过程（逐层排除，最后定位到缺了一段事件监听）**：
    · 先把整条链路与上游**逐行对比**：`BeefTimeAcceleration`（141 行）、`WondrousStaffRightClickHandler`（107 行）、
      `WondrousStaffItem`（222 行）、`EndlessBeafItem.useOn` **全部与上游一致**，只差包名与语言键命名空间
      —— 所以不是这几处被改坏，而是**某一段根本没移植**。
    · 上游这条链的入口有两个：① `UselessMod.onRightClickBlock`（`EventPriority.HIGHEST` + `receiveCanceled = true`
      的**事件监听器**，末尾对任何 `EndlessBeafItem` 调 `trySummonLightningForCollector` 并取消事件）；
      ② `EndlessBeafItem.useOn` 里的同名调用（物品链回退）。**我们只移植了 ②，① 没有。**
    · 上游扩展模组的 `UselessModLightningRodMixin` 恰好说明了这条分界线（照抄时被我跳过，且跳过的判断是对的
      —— 它拦的是 useless_mod 自己的监听器，我们的杖不继承它的物品类，不冲突）：其注释写明
      「加速关闭时保留上游手杖的一次性引雷」，即**加速开启时由加速路径接管、标记/放置模式下由手杖处理器接管**，
      只有**加速关闭**时才走这次性引雷。
    · 另核对过：`LightningRodBlock` **没有** `useItemOn`（不会在方块层吃掉右键）、
      `WondrousStaffAcceleration` 的 `isValidTarget` 对避雷针显式返回 true 且 `tickTargetWithinBudget`
      对避雷针调 `tickLightningRod`（`setVisualOnly(false)`，即自然雷）—— 加速那条路本身是好的。
  - **修复**：在 `WondrousStaffRightClickHandler.onRightClickBlock`（HIGHEST + receiveCanceled）里，
    把原来「加速关闭 → 直接 return」改成**加速关闭就地在事件层做一次性引雷**（`placementMode` 时让位）：
    `EndlessBeafItem.trySummonLightningForCollector(...)` → 取消事件并回填结果。
    这样右键避雷针不再依赖物品链（物品链那一步会被更早的 `onItemUseFirst` / 其它右键处理器抢先消费），
    与上游「事件层保证」一致。引雷本体仍走上游原样代码：真实 `LightningBolt`、`setCause(玩家)`、
    并打上 `ae2lt.natural_weather_lightning` 标记（上游自己就有这个 ae2lt 互操作标记，不是我们加的）。
  - **行为对照（修复后）**：加速**关闭** → 右键避雷针 = 一次性自然雷（1 道）；
    加速**开启** → 右键避雷针 = 进入加速，`tickLightningRod` 每 tick 按档位产出自然雷（与上游一致，不叠加一次性引雷）；
    标记 / 放置模式 → 由手杖自己的处理器接管，不引雷（与上游 mixin 一致）。
  - **验证**：编译 0 错误；链路四方逐行对比已确认与上游一致；修改点落在事件层且复用上游原函数。
    · **待用户实机确认**：右键避雷针是否出雷（这是纯玩家交互，无头环境无法自动点击）。
      若仍不出，需回报「加速开关是开还是关、档位数值」——`WondrousStaffAcceleration.tryUse` 在档位为 **×0** 时
      会主动丢弃加速标记（等于什么都不做），那是上游设计而非 bug。
- 5.1.5 → **5.1.6（删除套装的村民交易两项功能 + 神之掉落改为与原版生物掉落物同步）** —— 修复，按规则**末位 +1**。
  - **用户需求原文**：
    · 「神之套装的与村民相关功能：可能是村民大师（不管是否开启）在和村民交易都是直接大师，然后只有两个交易选项
      还不会刷新。你删除无限交易和村民大师这两个功能吧。」
    · 「神之掉落的资源生成机制不对，比如：我放入鸡刷怪蛋就只会生成羽毛，应该是和原版生物掉落物同步生成羽毛和生鸡肉。
      其他坚守者刷怪蛋也只会生成一种物资，你看一下所有的生成资源匹不匹配。」
  - **一、删除套装的两项村民功能**：
    · 现象成因（顺带查清，即使删掉也值得记录）：`ArmorExtraFeatures.onInteractVillager` 在**右键村民**时
      直接 `setVillagerData(level=5)` 并把 `setVillagerXp(0)` —— 等级硬拉满 + 经验清零，交易表因此异常
      （选项变少、且不再按经验刷新）；`onTrade` 则在每次交易后 `resetUses()`（无限交易）。
      两项都受 `GodArmorState.active(...)` 开关判定，但**开关默认全开**（`GodArmorFeatures.ALL`），
      所以「不管是否开启」实际是「没在套装功能界面里手动关过」。
    · 删除清单（4 处，共 4 个文件）：
      `handler/ArmorExtraFeatures.java` —— 删掉 `onTrade` 与 `onInteractVillager` 两个监听器及随之无用的 import
      （`TradeWithVillagerEvent` / `Villager` / `PlayerInteractEvent`），类注释改为「4 项」并写明删除原因；
      `armor/GodArmorFeatures.java` —— 删掉 `UNLIMITED_TRADES = 15` / `VILLAGER_MASTER = 16` 两个开关位与
      `LANG_KEYS` 里对应两项，`COUNT` 18 → 16，原 `UNDERWATER_VISION` 17 → 15；
      `armor/skill/ArmorSkillDef.java` —— 删掉 `EffectKind` 里的 `UNLIMITED_TRADES` / `VILLAGER_MASTER`
      （这两个只是效果类型枚举的遗留项：**没有任何技能注册它们、也没有引擎引用**，删掉不影响技能树；
      技能等级是按**字符串 id** 存在数据附件里的，所以枚举顺序变化不会串档）；
      两个语言文件 —— 删 `gui.godofthings.armor.unlimited_trades` / `...villager_master`（各 1 对）。
    · **注意（存档位）**：开关位是 bitmask 存在玩家数据附件里，重编号后旧存档里 bit 15/16 的残留含义会变
      （变成 UNDERWATER_VISION 的位、另一个被忽略）。因为**默认就是全开**，最坏情况只是某项"看起来是开着的"，
      不会导致功能错乱。
    · README：技能数 **79 个不受影响**（这两项从来不在技能表里，只在套装功能开关表里）；占位无需改。
  - **二、神之掉落改为与原版同步**：
    · 根因：`GodDropBlockEntity` 里有一张**手写的 `EGG_DROPS`（实体 → 单个物品）**表，命中就只产那一种物品；
      未命中的才走战利品表，而那条路也只 `findFirst()` 取一种 —— **两条路都只产一种**，所以鸡只出羽毛。
    · 修法：**整张手写表删除**，一律走**原版战利品表**，并把「只取第一种」改成「**全部产物都产出**」：
      同种物品先按物品合并（战利品表会把羽毛拆成 0-2 份），每种产物每周期 64 个（沿用旧产量口径），
      产出循环本来就有 `for (ItemStack out : outputs)`，所以多产物无需改消费端。
      仍保留 `isBannedDrop`（武器/工具/盔甲不进产出）—— 刻意的平衡取舍，已在代码注释里写明。
    · 附带说明：**坚守者（Warden）原版只掉 1 个幽匿催发体**，所以它「只生成一种」其实是**与原版一致**的，
      不是 bug；而旧表把凋灵骷髅强行改成只掉头颅、烈焰人只掉烈焰棒之类才是真正的"不匹配"，现已一并纠正。
  - **验证**：编译 0 错误；语言文件 zh/en 各 1271 键、双向差异 0；手写表与 `produceByMapping` 全仓已无引用。
    · **专用服务器实机验证**（数据包在真实世界放置 3 台神之掉落，用 `data modify block ... InputSlot` 塞入刷怪蛋，
      跑过工作周期后用 `execute if data block ... Inventory.Items[...] run say` 把结果打到日志里）：
      **鸡 → 羽毛 = YES + 生鸡肉 = YES**（用户报的那个 case）、牛 → 牛肉 + 皮革 = YES、
      凋灵骷髅 → 煤炭 + 骨头 = YES；反向对照「牛机里不该有羽毛」= CORRECT。
      期间还临时打过产出函数的诊断，实测 `roll -> 2 stacks: [minecraft:feather x2, minecraft:chicken x1]`、
      `insert ... leftover=0`，确认多产物都落进内部存储、无丢失（诊断代码验证后已全部删除）。
    · **验证过程中的两个坑（记下来省下次时间）**：
      ① `forceload add 0 0` 只覆盖区块 (0,0)（x/z 0-15），把机器放在 x=20+ 会 **静默不放置**（setblock 在未加载区块
         里失败且函数不广播失败信息）；数据包里的 `setblock` 必须落在已加载区块内。
      ② 函数里执行 `data get block ...` **不会有任何输出**（函数的命令反馈被抑制，`say` 才有）；
         而且这个机器的「无限储存」序列化格式**不是**原版的 `{Slot,id,count}`，而是
         `{Items:[{Count:9728,Item:{count:1,id:"minecraft:leather"}}]}` —— 查询路径必须写成
         `Inventory.Items[{Item:{id:"..."}}]`。这两种写法我都先写错过一次。
- 5.1.6 → **5.1.7（全项目体检：补 5 个缺失语言键 + gitignore）** —— 修复，按规则**末位 +1**。
  - **背景**：用户要求「检查当前项目」，做了一次完整体检（构建 / 版本一致性 / 语言键 / 悬空引用 / 仓库整洁度 /
    远程发布状态）。**体检结论：项目健康**——构建 0 错误、版本三处一致（gradle.properties = jar 内 mods.toml = 部署 jar）、
    语言文件 zh/en 双向零差异、79 个技能的名称与说明键**一个不缺**、8 个运行时拼接前缀**全部有对应键**、
    代码里零调试残留、已删功能零悬空引用、README 声称的技能数（79）与 `ArmorSkills.reg(...)` 实际登记数一致。
  - **本次修的问题（体检发现 5 个真缺失语言键，界面会直接显示原始键名）**：
    · `menu.godofthings.wireless_logistics` —— **无线物流界面的标题**。它在 `StaffLinkOpenPacket` 里作为
      `MenuProvider.getDisplayName()` 返回，而 `StaffLinkScreen` 是 `AbstractContainerScreen`，且在重写的
      `renderLabels`（第 697-698 行）里**直接画了 `title`** —— 所以标题栏会显示成 `menu.godofthings.wireless_logistics`
      这串原始键名。→ 补「无线物流网络 / Wireless Logistics Network」。
    · `gui.godofthings.wireless_logistics.batch_bound` / `.batch_unbound` / `.batch_none_bound` / `.batch_none_unbound`
      —— `StaffLinkBinding` 批量绑定/解绑的 4 条提示（前两条带 `%s` 台数参数）→ 按现有 `.bound` / `.unbound` 的措辞补全。
    · 体检还报出 `tooltip.godofthings.enhanced_chain_description`，但它出现在 `EndlessBeafItem:1620` 的**注释行**里，
      不是真引用，故未补。
  - **顺带**：`.gitignore` 增加 `.workbuddy/`（本机 AI 工作目录，此前一直以"未跟踪"出现在 `git status` 里）。
  - README：本次没有新增/删除内容，表格无需改动。
  - **验证**：语言文件 zh/en 各 **1276** 键、双向差异 0；5 个键的 zh/en 文案均已回读确认；构建 0 错误并已部署。
  - **随后一并处理（同一轮，已完成）**：把本地累积的 **19 个提交**推送到远程（`6fbe6d6..ae8a0b5`，约 45MB，含 `libs/` 下 14 个此前未推的预下载依赖 jar），
    并按「每个首位大版本一个 release」的规则**新建 3.x / 4.x / 5.x 三个 release**、把各自大版本的全部小版本 jar 挂上去：
    · **v3.0.1**（3.x）挂 `3.0.0`、`3.0.1`；
    · **v4.0.2**（4.x）挂 `4.0.0`、`4.0.1`、`4.0.2`；
    · **v5.1.7**（5.x）挂 `5.0.0`、`5.1.0`…`5.1.7` 共 9 个；
    · 顺手把本地仅存却漏挂的 `2.20.5` 补进已有的 2.x release（现 20 个 asset）。
    至此远程 release 共 **6 个**（v1.9.0 / v2.0.5 / v2.4.1 / v3.0.1 / v4.0.2 / v5.1.7），tag 与分支均与本地一致；
    下载链路实测 `HTTP 200`（`godofthings-5.1.7.jar` = 1,746,580 字节）。发版脚本落在 `.ref/release/publish-majors.ps1`（ASCII-only，
    走 `git credential fill` 取 token、自动探测 7890 代理），AGENTS.md 的同步状态段已同步更新。
- 5.1.7 → **5.1.8（全项目体检后修复：补 1 个缺失语言键 + 把 26 处写死的中文文案改成语言键）** —— 修复，按规则**末位 +1**。
  - **背景**：用户要求「检查一下这个模组」，做了一轮更彻底的体检（构建 / 真实加载 / 资源完整性 / 语言键 / 数据引用 / jar 打包 / 版本一致性）。
    **体检结论：项目健康**——`gradlew build` 0 错误；开发专用服务端实机加载 `Done (7.440s)!`（注册表 / Mixin / 数据包 / 5061 条配方全部就绪）；
    zh/en 各 1276 键双向零差异；模型引用的 79 处贴图全部存在、所有注册物品与方块都有 model + blockstate；
    jar 内 `mods.toml` 版本号替换正确、`zh_cn` 是合法 UTF-8 无乱码、6 份第三方许可都打进了 `META-INF/`；
    数据文件没有引用不存在的 `godofthings:` 物品 ID；代码零 `System.out` / `printStackTrace` / `TODO` 残留；
    3 个 mixin 配置里列的 25 个类全部存在；`gradle.properties` / `VERSIONING.md` / README 三处版本号一致。
  - **问题 ①（真 bug）：缺 `gui.godofthings.staff_summon.mode`**。它在 `WondrousStaffSummonScreen:253` 的
    `enabledMessage()` 里被用作**召唤界面的开关按钮文字**（77 / 207 / 221 行都调用它），而 zh_cn / en_us 两个文件里都**没有这个键**
    —— 玩家看到的是原始键名拼参数（`gui.godofthings.staff_summon.mode开`）。→ 补「召唤模式：%s / Summon mode: %s」。
    · 上次体检（5.1.7）没抓到这个键，原因是键集比对只比 zh 与 en 是否一致，**两边同时缺的键那种检查天然看不见**；
      这次改成「从 Java 里抽出全部 `translatable("字面量")` 再逐个回查语言文件」才暴露出来。
  - **问题 ②：26 处写死的界面文案绕过了语言文件**，英文环境下照样显示中文（`Component.literal(...)`）：
    · `GodCraftScreen`：模板 / 锁 / 开·停 / 配 / 模 / 神之合成；
    · `GodCraftTemplateScreen`：模板 / 单击预览 · Shift+单击保存 / 产物 / 无 / 模板为空 / 加载 / 返；
    · `GodAbsorberScreen`：`AE: 开` · `AE: 关`；`DevourerButtonHandler`：背包左上角按钮「吞」；
    · `WondrousStaffHud`：HUD 上的「时间加速 x%s ∞」「时间加速 x%s · %ss」（源码里写成 `\uXXXX` 转义，
      第一遍按中文正则扫没扫到，第二遍放宽规则才发现）；
    · `AbstractWaypointScreen`：置顶前缀「[顶] 」；`RangeAccelerationHistoryScreen`：历史条目详情的「偏 x,y,z」；
    · `PointCommands`：`/setpoint`、`/point tp`、`/point del` 的 5 条命令反馈。
    → 全部改成 `Component.translatable(...)`，共**新增 23 个键**（zh/en 同步），另有 3 处直接复用已有键
      （`gui.godofthings.back`、`container.godofthings.god_craft`、`gui.godofthings.range.offset_short`）。
    · 顺带把神之合成界面那排小按钮加宽（`BTN_W` / `BACK_W` 22 → 30，`BTN_X` / `BACK_X` 146+SHIFT → 140+SHIFT），
      否则英文短标签（Lock / On / Off / Face / Tpl）放不进 22px；改后左右都不与 AE 按钮（170+SHIFT 起、宽 16）
      和结果槽（124+SHIFT、宽 16）重叠，右边缘仍停在 222 之前没压到原版贴图边框。
  - **两处刻意不动**：① 配置注释（`ConfigManager` / `StretcherConfig` / `MachinesConfig` / `ClientConfig`）——NeoForge 的配置注释
    不支持语言键，只能写死；② 日志文案（`AeLogisticsCompat` / `AeEnergyCompat` / `AdAstraCompat` / `MagicAttributeHandler` /
    `StaffLinkManager` / `StaffLinkTargets` 等）是给开发者看的，不面向玩家。另外发现 `GodMinerBlockEntity.fluidName()`
    是**无人调用的死代码**（返回「水 / 岩浆 / 液体」），本次未动，留待后续清理。
  - **验证**：语言文件 zh/en 各 **1299** 键（1276 + 23）、双向差异 0、与仓库原有的排序规则一致；
    `gradlew build` 0 错误并自动部署（清理 5.1.7、落 5.1.8 到两个测试实例）；jar 内 `version="5.1.8"`、
    zh/en 各 1299 键且新增键文案回读正确；改完后再跑一次开发服务端加载自检通过。
  - README：本次没有新增/删除内容，表格无需改动。
  - **发版（已完成）**：本地提交 `af788fb` 已推送到 `origin/1.21.1`（走 NBVPN 的 7890 代理）；
    `godofthings-5.1.8.jar`（1,747,018 字节）按「每个首位大版本一个 release」的规则**追加到 5.x 那个 release**
    （tag `v5.1.7`，release id `400888984`），**没有新建 release、也没有新建 tag**；该 release 的 notes 同步追加了
    v5.1.8 一行并把「包含 jar」清单改成 5.0.0…5.1.8（现共 **10 个 asset**），远端 notes 与本地文案逐字一致。
    · AGENTS.md 的同步状态段已同步更新（5.x 由 9 个 jar 改为 10 个，核对日期改 2026-10-02）。
    · 上传脚本这次是临时脚本（`git credential fill` 取 token + 自动探测 7890 + `PATCH /releases/{id}` 改 notes +
      `POST uploads.github.com/.../assets` 传 jar），未入库；`.ref/release/publish-majors.ps1` 目前只管「建大版本 release」，
      小版本的「追加 asset + 改 notes」还没有可复用脚本，下次可以考虑补一个 `publish-patch.ps1`。
- 5.1.8 → **5.2.0（新增物品：神之便签 / God Note —— 记事本 + 屏幕悬浮便签）** —— 新增小物品，按规则**第二位 +1、末位归零**。
  - **需求**：用户要一个「类似便签的东西」，快捷键 / 物品 / 指令三种方式都能打开记事本，任务前带方框勾选；
    界面里可以开关悬浮窗，悬浮窗的位置、大小、背景也都能调。
  - **数据归属（本次的设计决定）**：便签属于**玩家个人资料**，不是物品自带 —— 一本便签对应一个玩家，存在存档里
    （`GodNoteData extends SavedData`，挂主世界维度存储、跨维度共享，与传送点同一套做法）。这样「N 键」「`/godnote`」
    这两条入口不需要玩家拿着物品也能用，悬浮窗也有一份稳定的数据源；物品只是第三个入口，把便签丢给别人不会把内容带走。
  - **网络（`GodNoteMessages`，4+1 条包）**：`note_sync`（服务端→客户端，整本下发）/ `note_update`（客户端→服务端，整本替换）/
    `note_request`（进世界后拉一次）/ `note_open`（快捷键请求开界面）+ `note_open_screen`（服务端→客户端，让客户端开界面）。
    · 同步策略用「整本替换」而不是增量：数据量很小（上限 64 条 × 64 字），换来的是「客户端怎么改、服务端就怎么存」这种
      最不容易出错的形态；**服务端始终先 `clamp()` 再落库**，客户端塞越界数据也写不坏存档（自检里专门验了这条）。
    · 发的是**副本**而不是引用：`PacketDistributor` 到网络线程才编码，直接发主线程那份对象会有并发改写风险。
    · 进世界后的首次同步由**客户端主动请求**（`ClientNoteCache.requestOnce()`），而不是服务端在登录事件里推送 ——
      纯原版客户端连进来时，服务端就不会往它发它不认识的包。
  - **界面**：`GodNoteScreen`（纯 Screen：`renderBackground` 留空、自己在 `render` 里画底色与面板，与 `RangeAccelerationConfigScreen`
    同一套写法）分两页：
    · **任务页**：一行一条 —— 左侧方框点击勾选、点文字改名（底部输入框变成「保存修改」）、右侧 `x` 删单条、`清空已完成`、
      `悬浮窗：开/关`、`悬浮窗设置 >`；列表滚轮翻页并显示 `起始-结束 / 总数`；Enter 提交，正在改名时 Esc 只取消改名不关界面。
    · **悬浮窗页**：开关、位置 X / Y（±2%）、大小（±0.25x，范围 0.5~2.5）、透明度（±0.1）、背景（`<` `>` 循环 5 档）、
      `已完成条目` 开关、`拖动摆放`、`< 返回任务列表`。
    · 换页不直接 `rebuildWidgets()`（按钮回调还在事件分发里，中途清空控件列表会让本轮点击分发提前结束），
      改成置 `pendingPage`、下一帧 `render` 里再切。
  - **悬浮窗**：`GodNoteHud` 注册为独立 GUI 层（`registerAbove(VanillaGuiLayers.EXPERIENCE_LEVEL, …)`，在血条之上、聊天栏之下，
    不做任何 Mixin，与其他 HUD mod 零冲突）。
    · 位置按**屏幕比例**存（0~1），绘制时换算成像素并夹在屏幕内 —— 换分辨率、换 GUI 缩放都不会跑偏；大小用 `pose().scale()`
      缩放，文字随之放大。
    · 每条任务按像素宽度折行（英文优先在空格断行、中文硬断；单条最多 6 行、整块最多 24 行），已完成条目画灰 + 删除线，
      勾选框用 4 个小方块拼出「✓」（不依赖字体里有没有 U+2713）。
    · 背景 5 档：无 / 半透明黑 / 羊皮纸（不透明 + 深棕文字）/ 深空蓝 / 自定义（颜色 + 透明度）；`mc.screen != null` 时不画，
      免得盖住别的界面。
    · `GodNoteHudEditScreen`：「拖动摆放」用的透明界面 —— 没有界面打开时鼠标事件根本不会送到模组手里（点一下就是攻击），
      所以用一层透明界面接管输入：拖动移动、滚轮缩放、Esc 保存并回到记事本。
  - **三入口**：`GodNoteItem`（右键 → 服务端先 `sendSync` 再 `sendOpenScreen`）/ `WandKeyBindings.OPEN_NOTE_KEY`（**N 键**，
    客户端 `NoteKeyHandler` 每帧 `consumeClick` → `note_open`）/ `GodNoteCommands`（`/godnote`〔开界面〕、`add <文字>`、`list`、
    `clear`、`hud on|off`；不设权限等级，谁都能管自己那一本）。
  - **新增文件**：`note/{NoteTask, NoteHud, NoteBook, GodNoteData}.java`、`network/GodNoteMessages.java`、`item/GodNoteItem.java`、
    `client/{ClientNoteCache, GodNoteHud, NoteKeyHandler}.java`、`client/screen/{GodNoteScreen, GodNoteHudEditScreen}.java`、
    `command/GodNoteCommands.java`；资源 `models/item/god_note.json`、`textures/item/god_note.png`（16×16 像素画）、
    `recipe/god_note.json`；`Godofthings.java` 注册物品与创造栏、`WandKeyBindings` 加 N 键。
  - **验证**：`gradlew build` 0 错误并自动部署（清理 5.1.8、落 5.2.0）；开发专用服务端实机加载 `Done (8.776s)!`；
    临时自检（验证后已删除，jar 内已确认无残留）在真实服务端运行时里跑通 4 项 —— **NBT 往返 = PASS、SavedData 整份存 / 取 = PASS、
    网络封包往返（且缓冲区正好读完）= PASS、越界数据 clamp = PASS**（日志实测 `nbt=true savedData=true packet=true clamp=true`）。
  - 语言文件 zh/en 各 **1338** 键（1299 + 39）、双向差异 0；README「内容一览」与键位表已同步（新增神之便签一行 + `N` 键）。
  - **发版（已完成）**：本地提交 `9049cc8` 已推送到 `origin/1.21.1`（走 NBVPN 的 7890 代理）；
    `godofthings-5.2.0.jar`（1,794,963 字节）按「每个首位大版本一个 release」的规则**追加到 5.x 那个 release**
    （tag `v5.1.7`，release id `400888984`）—— **没有新建 release、也没有新建 tag**。
    · 记一笔规矩上的冲突：AGENTS.md「发版标准流程」第 4 步写的是「大版本（第二位/首位变化）才新建 Release」，
      而「项目约定」写的是「按首位大版本归类，新小版本 jar 追加为 asset、不删旧 asset」。5.2.0 属于第二位变化，
      两者会给出不同答案；本次按后者执行（与该 release 现有内容一致：里面本来就放着 5.0.0…5.1.8 十个小版本）。
      若以后想改成「第二位变化就开新 release」，需要把 5.x 现有的 11 个 jar 一并迁到新 release 下。
    · release notes 追加了 v5.2.0 一行，「整合 5.0.0 → 5.1.8」改成「→ 5.2.0」，jar 清单补到 5.2.0（现共 **11 个 asset**）；
      下载链路实测 `HTTP 200`、1,794,963 字节，与本地 jar 完全一致。
    · AGENTS.md 的同步状态段已同步更新（5.x 由 10 个 jar 改为 11 个）。
- 5.2.0 → **5.2.1（神之便签：悬浮窗标题可自定义 + 自动更名开关）** —— 在刚发布的 5.2.0 物品上追加功能，按「修复 / 优化」类**末位 +1**
  （依据：与 1.5.1「请神可切换（再次右键取消）并加提示消息」同类 —— 给已有物品加一个开关功能，不走「新增小物品」的第二位进位）。
  - **需求**：悬浮窗的名字（默认显示「神之便签」）要能改；这个名字再加一个**自动更名**开关，打开后按当日日期命名（今天 = `10.02`），
    等到 10 月 3 号再进游戏、且开关还开着时，名字自动变成 `10.03`。
  - **数据**：`NoteBook` 增加 `name`（上限 24 字，空串 = 用默认「神之便签」）与 `autoName` 两个字段，NBT 存档与网络封包都带上；
    服务端 `clamp()` 里对新名称去换行 + 截断。
  - **服务端驱动**（新增 `NoteAutoName`，游戏总线）三处兜底：
    · **登录时**刷一次 —— 今天才进游戏，名字还停在昨天就换成今天（10.02 → 10.03）；
    · **每 1200 tick（60 秒）**给在线玩家刷一次 —— 游戏一直开着跨零点也会换；
    · **`GodNoteData.put()`** 落库前再强制一次 —— 客户端上传带 `autoName` 的副本时立即按服务端日期归一。
  - **日期格式** `MM.dd`（`DateTimeFormatter`），取**服务端本机日期**；单人 / 自己开服即玩家本机日期。实机自检里 `today=10.02` 与需求给的例子一致。
  - **幂等**：自动更名的名字本身就是日期串，所以不需要另存一份「上次自动命名的日期」，直接比字符串即可，重复调用无副作用。
  - **手动改名的语义**：界面里改名字输入框、或 `/godnote name <文字>`，都会**顺手关掉自动更名**（否则下一 tick 就被日期覆盖）；
    名字清空 = 恢复默认名。
  - **界面**：悬浮窗页重排成一屏 7 行（名称输入框 / 自动更名开关 / 悬浮窗开关 + 拖动摆放 / 位置 X·Y 合并一行 / 大小·透明度合并一行 /
    背景·已完成条目合并一行 / 返回），面板仍是 300×200；行内坐标按**英文标签宽度**留了余量（英文 Position X / Completed 比中文宽）。
    名字输入框在失焦 / 回车 / 切页 / 关界面时提交；服务端回包时若输入框没聚焦会跟着回读，所以跨天自动换名后界面也能同步显示。
  - **新增指令**：`/godnote name <文字>`、`/godnote auto on|off`。
  - **验证**：`gradlew build` 0 错误并自动部署（清理 5.2.0、落 5.2.1）；开发专用服务端实机加载 `Done (8.654s)!`；
    临时自检（验证后已删除，jar 内确认无残留）在真实服务端跑通 **7 项** —— NBT 往返 / 封包往返 / 自动更名生效 / 重复调用幂等 /
    手动模式不被动改 / `put()` 落库即当日日期 / 名称超长换行兜底，日志实测
    `nbt=true packet=true auto=true idempotent=true manual=true put=true clamp=true today=10.02`。
  - 语言文件 zh/en 各 **1348** 键（+10）、双向差异 0；README「内容一览」的神之便签一行与指令清单已同步
    （顺手修掉那一行里 `hud on|off` 没转义的竖线 —— 它会把 Markdown 表格多切出一列）。
  - **发版（已完成）**：本地提交 `05b16b8` 已推送到 `origin/1.21.1`（走 NBVPN 的 7890 代理）；
    `godofthings-5.2.1.jar`（1,799,372 字节）继续**追加到 5.x 那个 release**（tag `v5.1.7`，release id `400888984`），
    没有新建 release / tag；release notes 追加了 v5.2.1 一行，「整合 5.0.0 → 5.2.0」改成「→ 5.2.1」，jar 清单补到 5.2.1
    （现共 **12 个 asset**）；下载链路实测 `HTTP 200`、1,799,372 字节与本地 jar 一致。AGENTS.md 的同步状态段同步改为 12 个 jar。
- 5.2.1 → **5.3.0（体检后的完善批次：诊断指令 / 数据导出导入 / 成就补全 / 悬浮窗吸附 / GameTest + CI + 仓库瘦身）** —— 一批**新增功能 + 工程完善**，按规则**第二位 +1、末位归零**（不是单个物品的小改动，走「新增」那一档）。
  - 本轮是用户逐条确认的清单（体检报告里的风险 4/5/6/8/9/10 + 补充 2/3/4/5 + 完善 1~5），下面按条目记。
  - **① 渲染路径实体扫描改缓存（风险 5）**：上游 useless_stretcher 有三处在**渲染路径**里每帧 `getEntitiesOfClass(..., inflate(96/64))`，
    其中 HUD 那条还不看手里拿着什么。新增 `RenderEntityScan`（8 tick 快照，三处共用，最多 0.4 秒延迟）；三处调用点都标了 `[本地改动]`。
  - **② 删死代码（风险 9）**：删掉无人调用的 `GodMinerBlockEntity.fluidName()`（连带 `Fluid`/`Fluids` import），
    以及 `EndlessBeafItem` 里那行只存在于注释中的悬空 lang 键引用。
  - **③ AGENTS.md 规则整理（风险 8 + 6）**：把与「项目约定」冲突的发版流程第 4 步改成
    「**只有首位大版本变化才新建 release / tag，其余一律追加 asset**」；新增两条约定 —— **新功能一律独立成包**
    （别再往 1000+ 行的老类里塞）、**对上游抄来的代码做本地改动要标 `[本地改动]`**（上游更新后继续移植是既定做法）。
  - **④ 模组元数据（风险 10）**：`neoforge.mods.toml` 补 `logoFile` / `displayURL` / `issueTrackerURL` / `credits`；
    新增 `logo.png`（256×256，用本模组自己的贴图拼的）。
  - **⑤ 悬浮窗吸附 / 对齐 + 快捷键（补充 5）**：`GodNoteHudEditScreen` 松手时先贴屏幕四边与两条中线（6px 内吸附），
    否则对齐 4px 网格；新增 **M 键**（`key.godofthings.edit_note_hud`）直接进摆放模式，不必先开记事本。
  - **⑥ `/godofthings doctor`（补充 2）**：一条指令打印 5 组信息 —— 版本 / 装了哪些可选 mod（26 个 id 的清单）/
    **魔法增幅属性解析成功几项、哪些没解析** / **是否有上游 mod 覆盖本模组 Mixin**（直接点名 useless_mod→EntityGetterMixin、
    useless_stretcher→LevelRendererCloudMixin）/ 便签与传送点数据统计。控制台也能跑。
  - **⑦ 数据导出 / 导入（补充 3）**：新增 `SaveFileIO`（SNBT 文本落盘到 `<存档>/godofthings/exports/`）+
    `/godofthings export|import note|points`。用 SNBT 而不是自写 JSON，是为了直接复用两边已有的 save/load，不会出现两套格式跑偏；
    文件名做了防目录穿越清洗。传送点导入按名字**合并**（同名覆盖、新名新增、不删旧条目）。
  - **⑧ 成就补全（补充 4）**：从 6 个（root / 套装 / 矿机 / 维度 + 2 个彩蛋）补到 **33 个** —— 机器线、无线能量、便签线、
    杖 / 回收器、玩偶、虚空、无用维度、创造能量立方、时空永恒、生物覆灭、吞噬，外加「神之机器全家桶」；
    其中 **3 个是行为成就**（打开记事本 / 开悬浮窗 / 开自动更名），JSON 用 `minecraft:impossible`，由新增的 `NoteAdvancements`
    在服务端对应位置授予。实机日志实测 `Loaded 2093 advancements`（比 5.2.1 的 2066 正好多 27）。
  - **⑨ 数据层 GameTest 入库（风险 4 / 完善 1）**：新增 `com.godofthings.gametest.NoteDataGameTest`（5 个测试：便签 NBT 往返 /
    封包往返 / 自动更名规则 / 兜底 clamp / 传送点 NBT 往返）+ 模板 `data/godofthings/structure/note_data.nbt`（1×1×1 空结构，
    脚本生成的 gzip NBT）；build.gradle 补 `gameTestServer` run 配置，`gradlew runGameTestServer` 实测
    **All 5 required tests passed**。跑测试需要运行期有 AE2 —— `prepareGameTestMods` 任务会自动把 `libs/ae2-*.jar` 与
    `run-server/mods/guideme-*.jar` 拷进 `run-gametest/mods/`。
  - **⑩ 语言键校验脚本（完善 2）**：新增根目录 `check-lang.ps1`（ASCII-only，PS 5.1 / pwsh 都能跑，用 exit code 给 CI）——
    校验 zh/en 键集双向一致、无空值、代码里所有 `translatable("字面量")` 都有键、6 个运行时拼接前缀仍能解析到真实键。
    **做了正反两次验证**：正常跑 OK / exit 0；临时塞一个不存在的键进去，脚本抓到并 exit 1（验证后已还原）。
  - **⑪ GitHub Action（完善 3）**：新增 `.github/workflows/build.yml` —— fetch-libs → check-lang → `gradlew build`
    （用 `-Dorg.gradle.java.home="$JAVA_HOME"` 覆盖本机 JDK 路径，这个覆盖行为本地做过对照实验确认有效）。
    **不跑 gametest**（runner 上没有 AE2 / guideme）。libs 用 actions/cache 缓存。
  - **⑫ libs 移出版本库（风险 2 / 完善 4）**：19 个第三方模组 jar（52MB）从版本库移除（`.gitignore` + `git rm --cached`，
    本地文件保留），改挂到新 release **`libs-1.21.1`**（pre-release，不属于模组版本线，19 个 asset）；新增根目录
    `fetch-libs.ps1`（自动探测 7890/7897 代理、已存在则跳过、并处理 EMI 那个 `+` 被 GitHub 资产名规则换成 `.` 的改名映射），
    **实测 19 个文件下载后 SHA256 与本地逐一相同**。
    · 诚实说明：这**只瘦身了工作树与后续提交**，历史里那 52MB 仍然在 —— 要真正缩小 clone 体积得重写历史
      （filter-repo + force push + 重建 tag/release），风险大，本次没做。
  - **⑬ 顺带发现的真问题（记下来）**：`GodMiner` / `GodResource` / `GodDrop` / `GodCraft` / `GodSlaughter` / `GodAbsorber`
    这 6 个方块实体**类声明上直接 `implements` 了 AE2 的 `IGridConnectedBlockEntity`**，所以运行期**必须有 AE2**，
    否则模组在**加载阶段**就 `NoClassDefFoundError`（这次第一次跑 gametest 的环境里没有 AE2，就是这么崩的）——
    也就是说 README/AGENTS 里「没装对应 mod 自动跳过且绝不影响启动」只对「魔法增幅」那类字符串解析成立，对 AE2 不成立。
    **用户明确表示不主动修**（自己的整合包始终带 AE2），已写进 AGENTS.md 的「已知运行期硬依赖」一节。
  - **验证**：`gradlew build` 0 错误并自动部署（清理 5.2.1、落 5.3.0）；`gradlew runGameTestServer` **5/5 通过**；
    开发专用服务端实机加载 `Done (9.727s)!`、`Loaded 2093 advancements`、无成就解析报错；
    `check-lang.ps1` OK（1422 键 × 2）；jar 内确认含 `logo.png`、结构模板、64 个成就 json（33 个内容成就 + 31 个配方解锁）与 6 个新类。
  - 语言文件 zh/en 各 **1422** 键（+75：成就 54 / 指令与导出 18 / 吸附与快捷键 3）；README 已同步
    （内容一览新增「成就树」「诊断与备份」两行、神之便签行补 M 键与吸附，并新增「测试」「指令」两节）。
  - **发版（已完成）**：本地提交 `4f74290` 已推送到 `origin/1.21.1`（75 个文件、2418 行改动；走 NBVPN 的 7890 代理）；
    `godofthings-5.3.0.jar`（1,835,589 字节）继续**追加到 5.x 那个 release**（tag `v5.1.7`，release id `400888984`）——
    没有新建 release / tag；notes 追加了 v5.3.0 一行、「整合 5.0.0 → 5.2.1」改成「→ 5.3.0」、jar 清单补到 5.3.0
    （现共 **13 个 asset**）；下载链路实测 `HTTP 200`、1,835,589 字节与本地 jar 一致。
    · **新增依赖 release `libs-1.21.1`**（pre-release、19 个 asset、不属于版本线）：`libs/` 里那 19 个第三方模组 jar 从版本树移除后，
      由根目录 `fetch-libs.ps1` 从这里拉回；实测 19 个文件 SHA256 与本地逐一相同。
    · AGENTS.md 的同步状态段已同步更新（5.x 由 12 个 jar 改为 13 个，并注明多了一个依赖 release）。
  - **CI 首跑失败 → 已修复（记一笔，省下次时间）**：第一个 Action 运行（run #2/#3）在 `fetch-libs` 与 `build` 两步先后失败，都是**本机脚本里的 Windows 独有写法**：
    · `fetch-libs.ps1` 用了 `Test-NetConnection` 探测本机代理 —— **Linux 上的 pwsh 没有这个 cmdlet**（它属于 Windows 的 NetTCPIP 模块），直接 `CommandNotFound` 让脚本退出。
      改成 `System.Net.Sockets.TcpClient` 的跨平台探测，并按平台选 `curl.exe` / `curl`。
    · `build.gradle` 的 `deployJars` 用 `file('E:/MC/...')` —— 在 Linux 上这段字符串会被当成 URL，抛 `Cannot convert URL 'E:/...' to a file`，把整个 build 拖失败（**编译本身是成功的**）。
      改成先判 `os.name` 含 windows，否则打印一行「非 Windows 环境，跳过本机部署」直接返回。
    · 两处修完后 run #4 **全部步骤通过**（fetch-libs / check-lang / build / upload jar）。本机也复验过：Windows 上照常部署，把 `os.name` 伪装成 Linux 则走跳过分支。
    · 教训：**本机用的 .ps1 / build.gradle 只要会在 CI 跑到，就不能用 Windows 独有的 cmdlet 或盘符路径**。
    · 备注：release 里那个 `godofthings-5.3.0.jar` 是从 `4f74290`（功能提交）构建的；之后两个提交（`653a624`、`f5b616b`）只动了 CI 与构建脚本，**不影响 jar 内容**，所以没有重新上传。
- 5.3.0 → **5.4.0（便签多本 + 拖拽排序；体检发现的掉落 bug 修复；CI 跑起 gametest；校验脚本扩到资源一致性）** —— 一批**新增功能 + 修复 + 工程完善**，按规则**第二位 +1、末位归零**。
  - **① 便签一册多本（补充 6）**：新增 `NoteShelf` —— 一个玩家一册，**最多 8 本**，每本有自己的任务 / 名字 / 自动更名开关；
    悬浮窗的显示设置（位置 / 大小 / 背景 / 透明度 / 是否显示已完成）移到册一级共享，窗口里画的是**当前选中那本**。
    界面任务页顶部加了一条便签册栏：`<` `>` 切换、`新建`、`删除`（**点两次确认**，且只剩一本时不允许删）。
    自动更名改为「对每一本开着的那本生效」；doctor 会报「当前第 i/n 本」。
    · **旧存档兼容**：v5.3.0 及以前顶层是 `Tasks/Name/Hud`，读取时整份当成「一册里的一本」，悬浮窗设置一并接上
      （GameTest 里专门有一条迁移测试）。
  - **② 任务拖拽排序（补充 6）**：按住任务行上下拖拽即可重排（拖到列表上下边缘会自动滚动），松手才发包；
    没移动过就还是「点文字改名」。
  - **③ 修掉一个真 bug：3 个无用维度传送方块挖掉不掉落（优化①带出来的内容审计）**。
    `teleport_block` / `_2` / `_3` 带 `requiresCorrectToolForDrops()`，但**没进任何 `mineable` 标签**
    —— 原版判定链里没有等级规则命中时 `Tool#isCorrectForDrops` 直接返回 false，于是 `canHarvestBlock` 为假、
    `playerDestroy` 不执行，**掉落表写了也永远不触发**（钻石 / 下界合金镐一样）。查上游 useless_mod 自己的
    `data/minecraft/tags/block/mineable/pickaxe.json` 确实含这 3 个 id，属于照抄时漏了标签 → 按上游补进 pickaxe.json。
  - **④ 补 3 个实体语言键 + 删 4 张孤儿贴图（同一轮内容审计的产物）**：`beef_time_acceleration` 原本没有
    `entity.godofthings.*` 键（名称会回落成原始键），另两个辅助实体也一并补上；删掉 4 张经核对无任何模型 /
    blockstate 引用的贴图（`item/god_craft`、`item/void_teleporter`、`item/wondrous_staff`、`block/god_craft`）。
  - **⑤ 静态校验脚本扩到「语言 + 资源一致性」（补充 2）**：`check-lang.ps1` 现在一次查完 —— 键集双向一致 / 无空值 /
    代码字面量回查 / 6 个拼接前缀能解析 / **注册的方块→blockstate、非方块物品→item 模型、物品方块→名称键、
    实体→实体名键 全覆盖** / 反向「有 blockstate 但没有注册方块」 / **仓库内 .ps1 必须纯 ASCII**。
    规则在 HEAD 上零误报（20 方块 / 18 物品 / 4 实体），并做了正反两次验证（临时拿掉一个 blockstate → exit 1，还原 → exit 0）。
    · 顺带把 `release.ps1` 里唯一一处中文注释换成英文 —— 它本来就是 AGENTS.md 里点名的「PS 5.1 解析风险」来源。
  - **⑥ GameTest 进 CI（补充 1）**：`guideme` 作为第 20 个 asset 传到 `libs-1.21.1`，`fetch-libs.ps1` 加
    `-IncludeTestMods`；CI 现在是 fetch-libs → check-lang → build → **runGameTestServer** → 上传 jar。
    测试从 5 条扩到 **7 条**（新增多本 / 旧存档迁移；拖拽排序、自动更名、clamp 都加进了断言）。
  - **⑦ 导入前自动备份 + 存档体积体检（补充 3）**：`/godofthings import note` 覆盖前先写一份
    `note-<名字>-backup-<yyyyMMdd-HHmmss>.snbt`（备份失败只提示、不阻断导入）；`/godofthings doctor` 新增一行
    报告便签与传送点两份 SavedData 的 NBT 字节数，方便排查存档膨胀。
  - **⑧ mods.toml 依赖声明补全（补充 4）**：把 **AE2 如实声明为 `required`**（6 个方块实体类声明上直接 implements 它的接口，
    缺了就是加载期崩 —— 声明后缺依赖时会给出「缺 ae2」的明确提示而不是崩溃堆栈），另加 19 个 `optional` + `ordering="AFTER"`
    的可选兼容条目（JEI / EMI / AE2WTLib / ExtendedAE / AppliedFlux / AppMek / Ad Astra / Occultism / Malum /
    神秘农业 / Goety / 铁魔法 / 新生魔艺 / EnderIO / 通用机械 / MI / 资源蜜蜂 / 建筑手杖 / FTB Teams / GTCEu）。
  - **⑨ 配置注释双语化（补充 9）**：4 个配置类共 **117 处 `.comment(...)` / 117 个字符串字面量**后面追加英文
    （中文原文一字未改）：ConfigManager 83、StretcherConfig 28、MachinesConfig 5、ClientConfig 1；
    另有 15 个本来就是英文的注释保持不动。NeoForge 的配置注释不支持语言键，只能这样中英并排。
  - **⑩ 修一个会「静默吞代码」的脚本坑（顺带发现）**：给 `fetch-libs.ps1` 加 `-IncludeTestMods` 时我在里面写了中文注释，
    结果 **UTF-8 无 BOM + 中文** 被 PowerShell 读错编码后，注释尾字节被当成续行符，把下一行的 `$files += ...` 吞掉了 ——
    表现为「开关值为 True、if 也进了，但数组就是没变」（靠临时插调试行才定位到）。已改成英文注释，
    并把「仓库内 .ps1 必须纯 ASCII」做成了校验项（见 ⑤）。
  - **⑪ 文档订正（优化①）**：README 里「神之护甲 **12 项**功能」与代码不符 —— `GodArmorFeatures.COUNT = 16`
    （多出来的是水下视觉 / 村庄英雄 / 发光 / 暴食），已订正并补进功能列表；顺带更新了「测试」一节与便签一行（多本 / 拖拽排序）。
  - **发版（5.4.0，已完成）**：本地提交 `5add2a0` 已推送到 `origin/1.21.1`；`godofthings-5.4.0.jar`（1,846,170 字节）
    继续**追加到 5.x 那个 release**（tag `v5.1.7`，release id `400888984`）—— 没有新建 release / tag；notes 追加了 v5.4.0 一行、
    「整合 5.0.0 → 5.3.0」改成「→ 5.4.0」、jar 清单补到 5.4.0（现共 **14 个 asset**）；下载实测 `HTTP 200`、字节与本地一致。
    · **CI 首次跑通含 game test 的完整流程**（run #6 全绿）：fetch-libs -IncludeTestMods → 静态校验 → build →
      **Game tests（7 项）** → 上传 jar。为此把 `guideme` 作为第 20 个 asset 传到了 `libs-1.21.1`。
- 5.4.0 → **5.4.1（优化落地：悬浮窗排版缓存 + 发包去重）** —— 纯优化，按规则**末位 +1**。
  - **① 悬浮窗排版缓存（优化③的落地，是我自己在 5.3.0 引入的真热点）**：`GodNoteHud` 是**每帧**画的，
    而排版要**逐字符**量 `font.width` 才能折行 —— 一册 8 本 × 64 条任务时等于每帧几千次字形查询（比之前修掉的
    「每帧扫 192 格实体」还重）。改成按内容签名缓存排版结果：签名只是拼字符串，比重排便宜两个数量级；
    `rect()`（编辑模式每帧命中判定）也吃同一份缓存。
  - **② 发包去重（优化④的落地部分）**：整册替换的包里存的就是内容本身，所以内容没变时重发纯属浪费
    （设置页连点、界面 tick 兜底提交都会走到 `push()`）。新增 `ClientNoteCache.shouldPush()`：按
    「书名/自动更名/任务文字与勾选/当前选中/悬浮窗五项设置」算签名，与上次发出的相同就直接不发。
    · **为什么没做完整增量协议**：诚实结论 —— 数据量根本不值得。整册上限 8 本 × 64 条 × 64 字 ≈ 8KB（现实里通常 1~3KB），
      传送点一条约 40 字节。改成「只发改动的那几本 + 服务端合并」要么引入合并语义的一致性 bug，
      要么为几 KB 的带宽赌博式重构；**收益远小于风险**，所以只做了安全的去重那一半，并在这里把结论记下来。
  - **③ God class 拆分（优化②）的现状**：本轮所有新东西都是独立文件 —— `NoteShelf` / `RenderEntityScan` /
    `SaveFileIO` / `GodofthingsCommand` / `NoteAdvancements` / `NoteDataGameTest` / `check-lang.ps1` / `fetch-libs.ps1`，
    没有往 1000+ 行的老类里塞任何新逻辑。**存量那几个大类的拆分没有在本轮做**：`ModeWheelScreen`(1512) /
    `EndlessBeafItem`(1464) / `StaffLinkScreen`(1381) / `ConfigManager`(1385) 大多是逐字照抄上游的，
    拆之前得先确认「上游更新时怎么合并」，而且拆分属于纯重构、必须进游戏逐屏验证 —— 属于该单独开一轮的活。
- 5.4.1 → **5.4.2（God class 拆分第一步 + 掉落逻辑首次有测试）** —— 重构 + 测试，按规则**末位 +1**。
  - **背景**：优化②「God class 逐步拆分」在 5.4.1 里只做到了「新代码一律独立成文件」，存量大类没动。
    本轮挑了一个**真正安全**的落点：`GodDropBlockEntity` 里那段「按原版战利品表产出掉落物」的算法。
  - **做了什么**：把 `produce` / `produceFromLootTable` / `nearestPlayer` / `isBannedDrop` + 那 16 项
    `BANNED_ITEM_CLASSES` 常量（共 **110 行**）搬到新文件 `com.godofthings.block.entity.machine.DropLootRoller`，
    方块实体只留一行委派：**514 行 → 396 行**，并自动清掉了 31 个因此变成未使用的 import。
    选它的理由是「自成一体的纯算法」——只依赖 `(ServerLevel, BlockPos, EntityType/ItemStack)`，不碰 BE 的字段。
  - **顺带补上了一个真空地带**：这条链路（掉落机到底会不会按原版表产出全部掉落物、装备过滤对不对）
    **以前一条测试都没有**，只能人工进游戏摆机器试。抽成静态方法后新增
    `DropLootRollerGameTest` **4 条测试**（用真实服务端的原版战利品表跑）：
    · 鸡 → 必含生鸡肉、每种产物都是 64 个、产物里不出现被过滤的装备；
    · 刷怪蛋入口能产出 / 非刷怪蛋物品产出空表；
    · 非生物实体（船）安静返回空表而不是抛异常；
    · 装备过滤：剑 / 盔甲 / 弓 / 剪刀 / 打火石 → 过滤，羽毛 / 生鸡肉 → 放行，空物品按过滤处理。
    → 测试总数 **7 → 11**，`gradlew runGameTestServer` 实测 `All 11 required tests passed`。
  - **没做的部分（仍然只做了一小步）**：`ModeWheelScreen`(1512) / `EndlessBeafItem`(1464) / `StaffLinkScreen`(1381) /
    `ConfigManager`(1385) / `GodCraftBlockEntity`(987) 这些大类的拆分**还是没动**，原因不变：
    它们多是逐字照抄上游的，拆之前要先定「上游更新时怎么合并」，而且属于纯重构、必须逐屏实机验证 ——
    该在单独一轮里挑一个类、配一次完整的手动回归。这轮先把「可测的小块」拆出来做样板。
  - 开发专用服务端实机加载通过（含 AE2 required 声明、`note_sync` 协议版本 1→2 之后）。
- 5.4.2 → **5.5.0（新增「神之手册」：游戏内查每个物品 / 方块是干嘛的）** —— 新增功能，按规则**第二位 +1、末位归零**。
  - **起因**：用户提出「有些物品我都不知道作用是什么了」—— 之前那轮我曾建议做手册、当时按用户意思跳过了，现在补上。
  - **① 条目直接来自注册表，结构上不可能漏**：`ManualCatalog` 遍历 `BuiltInRegistries.ITEM` 里 `godofthings` 命名空间的
    所有物品，方块物品归「方块」页、其余归「物品」页，**标题直接用该物品自己的显示名**（不用另写标题键），
    正文取语言键 `manual.godofthings.<注册名>`。再加 10 条固定「系统」条目（入门 / 按键与指令 / 杖 / 套装与技能树 /
    便签 / 传送点 / 维度 / AE2 并网 / 兼容与共存 / 常见问题）。
    当前规模：**34 条物品 / 方块条目 + 10 条系统条目**，中英双语共 68+68+13 个新语言键。
  - **② 界面**（`ManualScreen`，380×220 面板）：左侧四个分类页签 + 条目列表（带图标、可滚动、显示「共 N 条」），
    右侧图标 + 标题 + 正文（可滚动，带细滚动条）；顶部搜索框**输入即筛**（匹配标题、正文与注册名）；
    滚轮按指针所在的那一列决定滚列表还是滚正文；方向键上下翻条目、Esc 关闭。
  - **③ 三种入口**：快捷键 **P**（`WandKeyBindings.OPEN_MANUAL_KEY`，可在「选项 → 按键控制 → 神之」里改）、
    可合成物品 **神之手册**（书 ×1 + 金锭 ×4）、指令 **`/godofthings manual`**。
    快捷键与物品是客户端直接开界面（内容全在客户端，不用等服务端往返）；指令走新增的一条
    `manual_open` 包（服务端 → 客户端，`ManualMessages`）。
  - **④ 校验防漏**：`check-lang.ps1` 新增「manual coverage」一节 —— **每个注册物品 / 方块都必须有
    `manual.godofthings.<id>`**，且 `ManualCatalog.SYSTEM_IDS` 里每条系统条目都必须有正文与标题键
    （系统 id 直接从 Java 源码里解析出来，所以以后加系统条目也会被自动纳入校验）。
    做了负向测试：中英两边同时删掉 `god_drop` 的手册条目 → 报 `[FAIL] registered items/blocks without a
    manual entry ... god_drop` 并 exit 1；还原后 OK。
  - **⑤ 顺手又踩了一次那个脚本编码坑（值得记）**：我在 `check-lang.ps1` 的 `$prefixAllow` 里写了三行中文注释，
    结果 PowerShell 读错编码后**注释把下一行的 `'gui.godofthings.manual.tab.'` 吞掉了** —— 表现为「前缀明明登记了，
    校验却还报缺这个键」。改成英文注释后立刻正常，同时脚本自身的「必须纯 ASCII」校验也由红转绿。
    这条规则（AGENTS.md 已记）现在有两个实例了。
  - **⑥ 物品贴图**：`textures/item/god_manual.png`（16×16，程序生成：深棕书壳 + 金书脊 + 金徽记 + 书页边），
    模型走 `minecraft:item/generated`，与既有物品一致。
  - 验证：`check-lang.ps1` OK（1501×2 键）；`runGameTestServer` **11/11 通过**；开发服务端加载通过；
    `gradlew build` 自动部署到两个实例。
- 5.5.0 → **5.5.1（把神之手册物品补进创造标签）** —— 修复，按规则**末位 +1**。
  - **问题**：本模组的创造标签（「神之物」）是**手动列举**物品的（`Godofthings.java` 里一长串 `output.accept(...)`），
    5.5.0 加手册物品时漏了这一行 —— 结果是**创造模式里找不到神之手册**（生存能合成、快捷键也能用，所以只影响创造取用）。
  - **顺带说明为什么校验脚本没抓到**：`check-lang.ps1` 查的是「注册内容 ↔ 资源/语言键/手册条目」，
    而「创造标签里有没有列出这个物品」不属于它能静态判定的类别（要判定得解析那段 lambda 的逻辑）。
    这类「新增物品容易漏的清单」目前有两处：**创造标签**与**手册条目**（后者已加校验）。
  - 验证：`gradlew build` 自动部署；创造标签里现在 `GOD_NOTE` 与 `GOD_MANUAL` 相邻两行。
- 5.5.1 → **5.5.2（修神之手册界面布局：面板内偏移被当成绝对坐标用）** —— 修复，按规则**末位 +1**。
  - **症状**（用户截图）：分类页签与搜索框看着「跑到屏幕中间」，条目列表与正文却贴在屏幕最上方，两边还对不上、互相压字。
  - **根因**：`ManualScreen` 里 `LIST_TOP`（=46）是**面板内偏移**常量，但列表与正文的**绘制**和**命中判定**直接拿它当**绝对 Y** 用了；
    而面板底色、页签、搜索框用的是 `top + …`（居中后的位置）。于是在「面板顶边恰好≈44」的 GUI 尺寸下，
    列表/正文落到了屏幕顶部、页签留在面板里 —— 看起来就是分类跑到中间。同一处错误还波及点击命中区与滚轮区域。
  - **修法**：
    · 所有坐标统一走 `listTop() / listBottom() / bodyTop() / bodyBottom()` 这些 `top + 偏移` 的辅助方法，绘制与命中判定共用同一套；
      文件头加了「坐标纪律」注释把这个坑写清楚，避免以后再犯。
    · 面板从固定 380×220 改成**自适应**：`min(420, 宽-32) × min(250, 高-32)`，最小 240×160；
      左列占面板宽 34%、搜索框宽度与四个页签宽度都按面板宽度算 —— 用户那台 GUI 只有约 383×310（界面缩放 3），
      原来的 380 宽面板左右各剩 1 像素，怎么摆都挤。
  - **验证**：`gradlew build` 通过；另用**与 Java 完全相同的公式**在目标尺寸 383×310 与常见尺寸 854×480 下渲染布局示意图，
    逐个元素核对（面板边距、头行搜索框与 X 不重叠、四个页签等宽铺满、列表/正文两列不越界、底部「共 N 条」在面板内）——
    全部通过，示意图在本轮对话里给用户看过。
- 5.5.2 → **5.6.0（神之便签支持子任务：主任务下挂子任务，像论文大标题 / 小标题）** —— 新增功能，按规则**第二位 +1、末位归零**。
  - **数据层**（`NoteTask` / `NoteBook`）：任务从「文字 + 勾选」变成一棵**只开放两级**的树 ——
    每条主任务最多挂 `MAX_CHILDREN = 16` 条子任务，单本总条数兜底 `MAX_TOTAL = 256`（兜同步包大小），
    超过两级的嵌套在 NBT / 网络读取时**按深度安静截掉**（不崩、不显示乱缩进）。
    NBT 写 `Children` 列表、网络包递归读写（读的时候不管深度够不够都把字节读完，保证不错位）。
    **旧平铺存档完全兼容**：没有 `Children` 字段就读成无子任务的主任务。
  - **结构操作集中在 NoteBook**（方便 GameTest 直接测）：`addChild` / `removeChild` / `moveChild`（同一父任务下重排）、
    `promoteChild`（子任务升级为主任务，插在父任务后面）、`demoteTop`（主任务降级为上一条的子任务）；
    `clearDone` 现在同时清已完成的主任务与子任务；删除主任务**连子任务一起删**。
  - **界面**（任务页）：列表改成「扁平行模型」（主任务 + 缩进的子任务按显示顺序摊平），子任务缩进 10px、文字更淡；
    带子任务的主任务显示 `(n/m)` 进度；**行尾 `+` 号给这条加子任务**（输入框进入「子任务模式」，Esc 取消）；
    **编辑文字时 `Tab` 降级 / `Shift+Tab` 升级**（像写大纲）；拖拽仍可用但限定**同级**（主任务排主任务、子任务在其父任务内排），
    拖拽语义不变：松手没动 = 点文字改名；删除带子任务的主任务要点两次确认（行变红）。
    顶栏计数改为「含子任务的完成 / 总数」。
  - **悬浮窗**：子任务缩进显示（颜色更淡）、`showDone` 关闭时完成的子任务也隐藏、主任务带 `(n/m)` 进度、
    标题统计改为含子任务；排版签名字段加入子任务（缓存不失效的问题不存在了）。
  - **指令**：`/godnote list` 改为树形缩进并带序号（`1.` / `1.1`）；新增 **`/godnote sub <主任务序号> <文字>`**；
    顶栏 / list 的统计都含子任务。
  - **协议**：`note_sync` / `note_update` 载荷变为递归结构，registrar 版本 `2` → `3`（新旧两端握手时直接拒掉，不会静默错位）。
  - **测试**：GameTest 11 → **13 条**（新增 `noteSubTaskStructure`：NBT/封包往返 + 旧平铺兼容 + 深度/数量夹取；
    `noteSubTaskMove`：降级/升级/子任务重排/主任务拖拽带子任务/删除连子任务/clearDone 语义）。
    过程中抓到一次**测试自身断言写错**（clearDone 后剩余子任务数期望值算错），修正断言后全过。
  - **文档**：README 便签行与指令表更新；手册「神之便签」条目与「系统 · 神之便签」重写（含子任务用法与 Tab 操作）。
    任务页布局用与 Java 相同的公式渲了示意图自查（缩进、行尾 +/× 热区、无重叠）。
- 5.6.0 → **5.6.1（便签子任务首批体验修复，用户实测反馈 4 条）** —— 修复 + 小改进，按规则**末位 +1**。
  - **① 提示文字压进输入框**（用户截图：输入框背景透出黄字）。根因：任务区 8 行排到 y=148，而拖拽/子任务模式的
    黄色提示画在 y=150、输入框也在 y=152 —— 三层叠在一起。修法：任务区改成 **7 行**，腾出一条**独立提示行**（y=139），
    拖拽 / 删除确认 / 子任务模式三种提示统一画在这行，与输入框不再重叠。
  - **② 删除点一下就没了，容易误触**。原来只有「带子任务的主任务」要点两次，普通行一击就删。
    现在**所有行的 × 都要点两次**：第一下行变红 + 提示行出现「再点一次 × 确认删除」，第二下才真删；Esc 或点别处取消。
  - **③ 缺任务展开按钮**。新增**行首三角折叠 / 展开**（`v` = 展开、`>` = 折起，只有带子任务的主任务才有）：
    折叠状态存进任务数据（`NoteTask.collapsed`），随任务保存与同步 —— 这样改完别的内容折叠不会丢（纯客户端状态会被
    每次整册同步冲掉），悬浮窗也遵循折叠（折起后只显示主任务与 (n/m) 进度）。协议版本 3 → **4**。
  - **④ 「一个任务只能加一个子任务」**。其实上限是 16，真正的坑是：**加完一条就退出了子任务模式**，
    用户接着输入的第二条被当成了新主任务。修法：回车加完**保持子任务模式**，可以连着加一串（这正是
    「1.热核 2.工业先锋」的用法），Esc 或点到别处才退出；空回车不再误退出。已加回归测试（连加 3 条 + 折叠状态
    NBT / 封包往返），GameTest 13 条全过。
  - 布局用与 Java 相同的公式渲了示意图自查（提示行独立、三角位置、折叠效果），图在对话里给用户看过。
- 5.6.1 → **5.6.2（用户实测两个 bug：子任务保存丢失 + 折叠按钮无效 —— 同一个根因）** —— 修复，按规则**末位 +1**。
  - **症状**：①加完几条子任务退出便签，重进只剩一条（甚至没有）；②行首折叠三角点了没反应。
  - **根因（两症状同源）**：客户端推送去重用的整册签名 `ClientNoteCache.signature()` **只覆盖主任务的文字与勾选，漏了子任务与折叠状态**——
    这是 5.4.x 做推送去重优化时写的，5.6.0 加子任务时忘了同步扩展。于是：
    · 加子任务 / 折叠后 `push()` 被签名判定为「无变化」**整册推送被吞**，服务端从未收到 —— 界面开着看到的是本地镜像（所以当时看得到），
      重开界面 `requestSync` 拉回服务端版本 → 子任务消失；
    · 折叠同样没有回执 → 客户端书对象不变 → 界面按「书对象身份」缓存的扁平行列表**不重建** → 画面纹丝不动，按钮像死的。
  - **修法**：
    · 签名挪到公共层 **`NoteShelf.signature()`**，覆盖全部可变内容（选中本 / 悬浮窗七项 / 书名 / 自动更名 / 每条任务的文字·勾选·折叠·**全部子任务**）；
      `ClientNoteCache.shouldPush` 改用它。挪到公共层的原因：这类 bug 只有测试能兜住，而签名测试必须在非客户端代码里写。
    · `GodNoteScreen` 的扁平行列表**去掉按对象身份的缓存**，每次调用重建（界面只在打开时跑，量级可忽略）——
      原地修改任务后旧缓存不失效的问题从结构上消除。
  - **回归测试**：新增 `noteSignatureCoversSubTasks`（加子任务 / 勾子任务 / 折叠 / 改名 / 再加一条，每步断言签名变化）——
    GameTest 13 → **14 条**全过。
- 5.6.2 → **5.7.0（新方块「神之怪蛋」：任意刷怪蛋复制机；神之掉落机改为兼容所有模组的刷怪蛋）** —— 新增方块 + 兼容性修复，按规则**第二位 +1、末位归零**。
  - **① 新方块「神之怪蛋」**（`god_spawn_egg`）：放入刷怪蛋就产出**同种**刷怪蛋（每周期 64 个 × 神之加速并行倍率），
    其余功能与神之资源机 / 掉落机一致 —— 9 个输入槽、内部无限存储、神之加速槽、AE2 并网、工作间隔可配、产物自动向下输出。
    配方：8 绿宝石 + 1 鸡蛋。配套资源齐全：blockstate、方块与物品模型、侧面/顶面贴图（掉落机贴图色相偏移，同族但可区分）、
    GUI 贴图、掉落表、成就「蛋生蛋」+ 配方成就、**mineable/pickaxe 标签**（上次传送方块的教训已内化）、中英语言键、
    **手册条目**（check-lang 强制）；新增配置项 `spawnEggMachine.workInterval`（默认 20 tick）。
  - **② 神之掉落机不再只认原版刷怪蛋**：原来输入槽校验（`instanceof SpawnEggItem`）与掷表器都只认原版类，
    自己实现刷怪蛋物品的模组连蛋都放不进去。现在统一走新的 `SpawnEggHelper`：
    先按 `SpawnEggItem.getType`（覆盖原版 / NeoForge `DeferredSpawnEggItem` 及各种子类），
    再回退到按原版约定读 `ENTITY_DATA` 组件里的实体 id（模组自建物品只要写了这个组件就能用）。
  - **③ 测试期间抓到一个真坑**：`BuiltInRegistries.ENTITY_TYPE` 是**带默认值的注册表（默认 pig）**，
    直接 `get()` 一个不存在的 id 会返回「猪」而不是 null —— 意味着任何写了垃圾 id 的物品都会被当成猪刷怪蛋。
    已改为先 `containsKey` 再取。这是 GameTest 直接红出来的（`nonEggIsRejected`），不是人工想到的。
  - **④ 回归测试 14 → 19 条**：新增 `SpawnEggHelperGameTest`（原版蛋识别 / 模组蛋识别 / 非蛋与坏数据拒绝 /
    复制产物保真（同种物品，实体数据与自定义名等组件一起带走）/ 掉落机接受模组蛋的端到端）。
  - 文档：README 内容一览新增「神之怪蛋」行、掉落机行补「v5.7.0 起兼容所有模组刷怪蛋」、AE2 行 6 台 → **7 台**；
    手册新增 `manual.godofthings.god_spawn_egg` 条目，并同步更新掉落机与 AE2 两条表述。
- 5.7.0 → **5.8.0（按用户要求删除三块内容：神之记录方块 / 神之便签指令 / 荒辰移晷之杖整套；手册同步）** —— 内容删除，按规则**第二位 +1、末位归零**。
  - **① 神之记录方块**（`god_record`）：方块 / 方块实体 / 菜单 / 界面 / blockstate / 模型 / 贴图 / 掉落表 / 配方 / 成就 / mineable 标签条目 / 中英语言键 / 手册条目全部删除。
    **传送点系统本身保留**：`/setpoint`、`/point`、`/delpoint` 与 U 键界面照常可用（README 那一行从「神之记录」改题为「传送点」并注明方块已删除）。
  - **② 神之便签指令**：整个 `GodNoteCommands` 删除（`/godnote`、`add`、`list`、`clear`、`name`、`auto`、`hud`、`sub`），连带 10 个只有它使用的 `message.godofthings.note.*` 回执键。
    **便签功能本身完整保留**：物品右键、N 键界面、悬浮窗（M 键摆放）、多本与子任务、导出导入（`/godofthings export|import note`）都不受影响；手册与 README 的入口说明改为「两种打开方式」。
  - **③ 荒辰移晷之杖（权杖）彻底清除**：`beef/stretcher` 整套 **56 个文件**（杖物品、时间加速、范围加速及其预览/历史/配置界面、召唤、战利品刷新、树叶掉落彩蛋、时流范围回收器、四套配置、网络包、HUD、3 个实体与渲染器、mixins 配置），
    再加「神之剑」系统 **5 个文件**（J 键功能面板 —— 它的 `findSword` 找的就是权杖，README 里也写作「杖的功能面板」，属权杖专属）、打草彩蛋处理器，
    以及相关资源（3 个模型 / 2 个成就 / 1 个配方 / `neoforge.mods.toml` 的 mixins 声明 / 源码内与 `LICENSES/` 的 MIT 许可副本）、**61 个语言键**、
    配置段 `godofthings-staff-*.toml`、手册条目与系统条目（`ManualCatalog.SYSTEM_IDS` 里的 `staff`）。
    · 连带清理：**31 个标签文件**里对权杖的引用（`c:tools/*`、`gtceu:crafting_tools/*`、`malum:soul_shatter_capable_weapon`、原版剑/镐/斧/锹/锄与可采掘标签等）——
      用 **JSON 解析**方式删条目（第一版正则把 JSON 删坏过，被脚本自带的 `json.loads` 校验当场拦下，没有写坏文件）；两个以已删物品为图标 / 父级的成就（`grass_wand_drop`、`wonder_collector`）一并删除。
    · **保留**：无线物流（`StaffLink`，15 个文件 —— 查证过它不引用权杖，属 beef 核心自有）、无用维度、皮肤玩偶。
    · 如实说明一个边界：**牛排工具那套基类框架现在成了没有物品可达的代码**（`EndlessBeafItem` 1464 行、`ConfigManager` 1385 行、模式轮盘、工具按键与网络包；牛排工具物品本体早在 v5.1.2 就已按用户要求移除，基类此前只被权杖继承）。
      它来自上游 useless_mod 的移植，与本模组自有的无线物流 / 维度共处同一批包，彻底拆掉要先重构共享部分 —— 本轮按「权杖」这个边界处理，没有动它；要一起清掉再单独提。
  - **④ 手册同步**：删除 `system.staff`、`wondrous_staff`、`range_reclaimer` 与神之记录条目；改写「按键与指令」「兼容与共存」「神之便签」等条目（去掉 x/X 与 J 杖按键、`/godnote` 指令、useless_stretcher 提法）；README 成就数按实际文件重算为 **29 个**（原文档的 33 已过时）。
  - 验证：`check-lang.ps1` OK（**381 个 java 文件 / 20 方块 / 17 物品 / 1 实体 / 32 条手册条目 + 9 条系统条目**）；GameTest **19/19 通过**；
    残留引用全量复查（java + 资源 + 标签 + 成就）为 **0**；开发服务端加载通过。过程中修掉自己造成的一处 `mods.toml` 孤儿 `[[mixins]]` 空块（会让模组加载失败，被 GameTest 报出来）。
- 5.8.0 → **5.9.0（按用户要求：删无线物流 / 删传送点指令 / 神之套装技能树与套装功能大合并）** —— 内容删除与重构，按规则**第二位 +1、末位归零**。
  - **① 无线物流（StaffLink，照抄 useless_mod）整体删除**
    · 删 **36 个文件**：`beef/content/stafflink/`（12）+ `beef/world/stafflink/`（3）+ `beef/network/StaffLink*Packet`（12）+ `beef/client/gui/StaffLinkScreen`、`beef/client/render/StaffLinkHighlightRenderer`、`beef/client/StaffLinkClientHooks`、`beef/content/menus/StaffLinkMenu`（4）+ 4 个只为它存在的兼容桥（`compat/ars/SourceBridge`、`compat/ars/ArsSourceCompatLoader`、`compat/ae/AeSourceBridge`、`compat/ae/AeSourceCompatLoader`）+ `command/PointCommands.java`（见 ②）。
    · 改 15 个文件：网络包注册、菜单注册、界面注册、B 键（`KeyBindings` + 客户端事件 + 只为它服务的 Shift+滚轮处理器）、`UComponents` 的三个属主组件、`ClientPacketHandlers` 的三个处理函数、模式轮盘的物流条目（`ModeWheelScreen` / `ModeTypeEnum` / `BeefToolModuleRegistry`）、`ModeTogglePacket`、`EventHandler` 的三个物流事件、`EndlessBeafItem` 的物流开关、JEI 拖拽处理器、注释。
    · 连带清理**已无调用方的物流支撑死代码 23 个文件**：`beef/api/logistics/`（5）、`beef/compat/teams/`（4，FTB Teams 团队归属只为物流网络服务）、`beef/compat/modernindustrialization/`（2）、`beef/compat/mekanism/`（1）、`beef/compat/ae/` 里只被物流簇引用的 11 个（Chemical/Energy/Logistics 三对 Bridge+Compat+Loader、`AeNetworks`、`DynamicReflectionSupport`）。
      **保留** `beef/compat/ae/AeDeviceLinker` 与 `AeLinkChannelBypass` —— 查证它们有活调用方（`EventHandler`、`AeLinkPreviewPacket`、`AeConnectLinkSavedData`、`GodFurnaceBlockEntity`），是神之机器并网/链接预览在用的。
    · **保留**：无用维度、皮肤玩偶、神之传输（无线 FE 充能，与物流无关）、`gui.godofthings.transmitter.*` 等键。
    · 语言键删除 **91 个**（83 个 `gui.godofthings.wireless_logistics.*` + `key/menu/tooltip` 各 1 + 5 个 `message.godofthings.point.*`）。
  - **② 传送点指令删除**：`/setpoint`、`/point`、`/delpoint`（`command/PointCommands.java`）整个删除，**只保留 U 键图形界面**（传送点数据、`WaypointMenu`、界面编辑/删除/置顶一律照旧）；手册与 README 的指令表同步去掉这三条。
  - **③ 神之套装技能树大改（按用户要求）**
    · 删「基础属性（15）」「特殊增幅（15）」「机械共鸣」「魔法增幅（26）」四页与其余终极节点；`ArmorSkillCategory` 现在只剩 `ULTIMATE` 一个分类，`ArmorSkills` 只剩 **7 个开关式节点**，`ArmorSkillEngine` / `ArmorSkillHandler` / `ArmorExtraFeatures` 里对应的属性、暴击、吸血、荆棘、破甲、金身、涅槃、死神、奥术、光环、铁砧附魔、机械继承等实现一并删除；整个 `MagicAttributeBridge`（魔法增幅属性桥）删除。
    · 「终极节点」改名 **神之增幅**，7 个节点**改为开关**（打开即给到该节点的最大等级，关闭只关不清等级）：**神之掉落**（原财源滚滚，满级 100）/ **神之生物**（原猎魂丰收，×10）/ **神之方块**（原点石成金，×10）/ **神之经验**（原经验飞涨，×10）/ **神之怪蛋**（原妖魂凝卵，满级 100%）/ **神之头颅**（原斩首夺颅，满级 100%）/ **神之熔炼**（原自动熔炼）。id 沿用上游 skillId，改名只动语言键。
  - **④ 套装功能（神之套装）合并**：16 个开关 → **8 个**，`GodArmorFeatures.COUNT` 16 → 8
    · 神之飞行 = 创造飞行 + 飞行无惯性 + 飞行挖掘不减速；神之无敌 = 免疫所有伤害 + 不会死亡 + 免疫负面 + 无限氧气（含 Ad Astra 氧气）；神之视觉 = 无痕夜视 + 熔岩夜视 + 碧波清眸（水下清晰）；
      永不饥饿 → **神之饱和**；火焰熔岩免疫 → **神之抗火**；水下呼吸 → **神之呼吸**；发光 → **神之透视**；暴食 → **神之贪吃**。
    · **万民敬仰（村庄英雄）改为常驻**：穿齐套装即生效，不再占开关位（代码里用 `GodArmorState.alwaysOn`）。
    · **老存档迁移**：新增附件 `godofthings:armor_features_v2`（默认 -1 = 未写过）；读到 -1 就把旧的 16 位掩码按合并规则迁移过来（`GodArmorFeatures.migrateLegacy`：被合并的旧开关里任一为开，合并后的新开关即为开）并落库，之后只读写新附件。
    · 界面 `GodArmorSkillScreen` **整个重写**：两页（K = 神之增幅 / O = 神之套装）+ 开关式行 + 悬停说明 + 滚轮 + 「全部开启 / 全部关闭」+ 「未穿齐」提示 + 记住上次页面；旧界面里的等级进度条、拖动调级、整列操作等一并去掉（等级概念只保留在「开启 = 满级」）。
  - 语言键：套装相关删除 178 个（72 个已删节点 × 名称+说明、6 个分类键、14 个旧功能键及其说明），改写 19 个，新增 18 个（8 个新功能名 + 说明、界面提示、技能状态文案）。合并后两个语言文件各 **1194 → 最终以实际为准** 条。
  - 文档：README 的护甲行（8 开关 + 常驻万民敬仰）、技能树行（两页 7 节点）、第三方署名段；手册套装条目；AGENTS 同步。
  - 协作说明：本轮由一名 teammate 执行无线物流/传送点指令的删除（含隔离验证树自证删除不破坏编译），主 agent 执行技能树与套装功能改造，双方写集不重叠（teammate 只碰 beef/client/command，主 agent 只碰 armor/screen/lang/docs），最后由主 agent 统一编译、校验、测试与发版。
- 5.9.0 → **5.10.0（按用户要求清除「牛排工具」框架残留）** —— 大规模死代码清除，按规则**第二位 +1、末位归零**。
  - 背景：早期照抄上游 `useless_mod` 移植的「牛排工具」（`EndlessBeafItem` 那一套）。它的**物品本体**早在 v5.1.2 就被用户删除，v5.8.0 又删掉了唯一还继承它的权杖，于是整套框架成了**没有任何入口的死代码**（`beef\init\ModItems` 只注册 3 个传送方块物品，全仓库无一个类 `extends EndlessBeafItem`）。
  - **删除 73 个文件（≈1.2 万行）**：`EndlessBeafItem` / `BeefToolVariants` / 各类工具物品（`BeefRipen` `BeefCropHarvest` `BeefFlintAndSteel` `BeefMagnetHandler` `BeefTeleportHandler` `BeefTimeAcceleration` + 时间加速实体）/ 模式轮盘（`ModeWheelScreen` 1507 行、`BeefToolModuleRegistry`、`ModeTypeEnum`、`ModeWheelHandler`、`ChainGroupScreen`）/ 工具按键（beef `KeyBindings` 整类）/ 19 个工具网络包 / 工具 HUD 与挖掘辅助（`MiningStatusGui`、`utils\mining\` 全包 9 个、`BeefAutoClicker`）/ 工具 Mixin 5 个（含飞行）/ `ModEntities`、`ModDamageTypes` / 只服务工具的 6 个 compat（建筑手杖、神秘仪式挎包、EnderIO 传送、资源蜜蜂捕捉、AE2 存储优先、龙之进化）。beef 包 **146 → 73 个文件**。
  - **裁剪 13 个保留文件（−1,607 行）**：`EventHandler`（768→507，删掉全部「手持工具才可能触发」的事件与整套飞行维护）、`ClientEventBusSubscriber`（279→38）、`UComponents`（30 个组件删到 5 个，活组件保留）、`ConfigManager`（1385→953，只删 `beef_tool` / 自定义药水 / 连锁等工具段，**维度生成与合金炉等级等活配置一行未动**）、`ModNetwork`（19 个工具包解注册）、`AeLinkHighlightRenderer`（拆掉工具门）、`ModTags`/`ModItems`/`JEIPlugin`/`ClientPacketHandlers` 等。
  - **严格保留**：无用维度（10 文件）、3 个传送方块物品、AE 无线接入点连接子系统、高级合金炉 + `AlloyFurnaceTierRules`、能量系统、以及本模组自己的便签 / 传送点 / 手册 / 套装 / 机器代码。
  - 资源与语言：删掉 `data\godofthings\damage_type\beef_tool.json`（`ModDamageTypes` 已删 → 孤儿资源）与 **174 个孤儿语言键**（工具配置 32、工具 GUI 60、工具提示 64、工具按键 14、牛排工具名 2、注册表派生 2）；`godofthings.beef.mixins.json` 去掉 5 条飞行 Mixin 登记（删类的必要配套）。语言文件 1190 → **1016 条**。README 的「键位（神之工具）」段、目录结构、指令表，以及手册的「按键与指令」「传送点」条目同步更新（顺手删掉遗留的 `manual.godofthings.system.staff.title` 孤儿键）。
  - 验证：`check-lang.ps1` **OK**（**247 个 java 文件**、20 方块、17 物品、**0 实体**）；GameTest **19/19 通过**；`gradlew build` 成功并自动部署；顺带清掉 `run-gametest` / `run-server` 里带已删 `[beef_tool]` 段的旧配置实例（避免启动时做一次无意义的自动纠正）。
  - 协作方式：由 teammate 执行删除并自证（强制全量 `compileJava` 0 error、全仓库引用图与悬空 `{@link}` 清零、复跑 `check-lang.ps1`），主 agent 负责语言键、文档、验收与发版；删语言键时一度把 JSON 末尾逗号删坏，被 `json.load` + `check-lang.ps1` 双重兜底当场发现并修好。
  - **遗留待定（下一轮）**：①「神之共鸣」（原机械共鸣）按用户要求恢复并改名 —— 方案已交用户审阅；②AE 无线接入点连接子系统（≈1,100 行）与「无敌 / 高级隐身」玩家保护层 + 8 个保护 Mixin —— 两者都已查实「本模组内没有任何入口能触发」，等用户决定是否一并删除。
- 5.10.0 → **5.11.0（按用户要求：恢复并改名「神之共鸣」、界面文案精简、删高级合金炉模块、再清两块死代码）** —— 内容增删，按规则**第二位 +1、末位归零**。
  - **① 神之共鸣**（原「机械共鸣」，用户要求恢复并改名）：`ArmorSkillCategory.MACHINE` 恢复，7 个开关（`machine_loot_bomb` 等，id 沿用上游便于对照）重挂回技能表，界面加第三页。
    · **默认关**：机器默认**不**继承增幅；要让机器继承，需要「主人**在线** + **穿齐全套** + **对应共鸣开关打开**」三条同时满足 —— `ArmorSkillHandler.effectAllowed(玩家, 共鸣id)`，7 处判定分别挂在生物掉落/方块掉落/经验（「生物」「方块」「经验」各管一个入口）、刷怪蛋、头颅、自动熔炼、以及战利品爆炸（「掉落」共鸣，生物与方块两处都会叠加）上。
    · 界面：**三页**（K = 神之增幅 / O = 神之套装 / 中间页「神之共鸣」用页签或 `Tab` 切），行尾只显示开 / 关，页底一行说明「默认关：打开后对应的神之系列机器才继承这个增幅」。
    · 老存档：以前点开过的共鸣开关若还在 `armor_skills` 里就照旧生效（等级记忆机制未变）。
  - **② 界面文案按用户要求精简**：行尾只显示「开 / 关」（不再带「满级 N」）；神之增幅的技能说明**只保留破折号后的内容**；「神之套装」页去掉「功能」二字（页签与标题一致）；功能说明**只保留主要介绍**、不再出现「原 N 个开关合并」字样、句尾不留句号。
  - **③ 删除高级合金炉模块**：`beef/content/machines/advanced_alloy_furnace/**`（含 `chemical` 子包 7 个文件）+ `AlloyFurnaceTierRules` + `ConfigManager` 的服务端 `advanced_alloy_furnace` 段（10 个字段 + 定义块 + 11 个零调用 getter + 校验器，953 → 825 行）。
    · 查证：本仓库里它**从来没有方块 / 方块实体 / 菜单 / 屏幕 / 配方类型 / POI 注册**，JEI / EMI 也没有它的类别，`resources` 里一个合金炉文件都没有 —— 只剩配置段。启动日志那条 `Built alloy-furnace recipe catalog` 是**上游 useless_mod 自己打的**（`com.sorrowmist.useless.content.recipe.AlloyFurnaceRecipeCatalog`），本仓库无此类，未动。
    · 保留：多方块万象合金炉（`omniversal_multiblock_alloy_furnace`）配置段、神之熔炉全套、`advanced_alloy_furnace.recipe_conversion`（见「遗留」）。
  - **④ 删除 AE 无线接入点连接子系统**（≈1,100 行，用户批准）：`AeDeviceLinker` / `AeLinkChannelBypass` / `AeConnectLinkSavedData` / `AeLinkPreview{,Request}Packet` / `AeLinkHighlightRenderer` + `EventHandler` 四处 + `ModNetwork` 注册 + 2 个数据组件。
    · **保留**：神之系列机器自己的 AE2 并网（7 个方块实体仍 `implements IGridConnectedBlockEntity`、`AeGridNode`、`registerGridLinkables`）。
  - **⑤ 删除「无敌 / 高级隐身」玩家保护层 + 7 个保护 Mixin**（用户批准）：`BeefInvulnerabilityOwnership`、2 个状态包、`ClientPacketHandlers`、`ClientEventBusSubscriber`、`UselessItemUtils`、`ToolTypeMode`、`ModTags`；`UComponents` 组件删光后**整类删除**（`Godofthings` 里的 `UComponents.init` 一并去掉）；`EventHandler` 重置为 53 行（只剩无用维度的地形灌注）。
    · Mixin：删 `EntityGetterMixin` / `EntityMixin` / `LivingEntityMixin` / `PlayerMixin` / `ServerLevelMixin` / `ClientLevelMixin` / `ServerGamePacketListenerImplMixin`；**`LevelMixin` 保留**（`isDay` / `isRaining` / `isThundering` 三条「无用维度永远晴天」注入属于要保留的维度子系统），`godofthings.beef.mixins.json` 因此保留、`mods.toml` 那行不动。
    · **意外收获**：删掉 priority=500 的 `EntityGetterMixin` 后，useless_mod 那个 `defaultRequire=1` 的同名重定向不会再被挤掉 —— 历史注释里记的 `Critical injection failure` 启动崩溃风险顺手消除。
    · `GodofthingsCommand`：`MIXIN_OVERRIDERS` 去掉 `{useless_mod, EntityGetterMixin}`；`OPTIONAL_MODS` 26 → **5**（`ae2` / `jei` / `emi` / `ad_astra` / `gtceu`；jei 与 emi 是真插件但代码里没有字面量，特意保留）。
  - 清理量：删 **30 个 java 文件 + 12 个空目录**，改 7 个文件；beef 包 **73 → 43 个文件**；语言键 1030 → **960**（删 70 个孤儿键：AE 链接/访问点 10、合金炉配置展示 18、档位数组展示 41、`tooltip.godofthings.omnitool_mode` 1）；两处失效 javadoc 顺手改掉。
  - 验证：`check-lang.ps1` **OK**（**217 个 java 文件**、20 方块、17 物品、0 实体）；GameTest **19/19**；`gradlew build` 成功并部署；开发服务端加载通过。
  - 遗留（既有死代码，用户可按需再清）：`beef/compat/AppFluxCompat`、`beef/core/component/ExternalInventoryKind|Reference`、`beef/core/constants/NBTConstants`、`beef/energy/{EnergyManager,IEnergyManager}`、`beef/client/gui/{ScaledEnergyAmount,PinyinSearch}`、`beef/api/enums/EnumColor` + `content/blocks/IColoredBlock`、`ConfigManager` 里未接线的 `recipe_conversion` 那套（~60 个开关）与另外约 40 个零调用 getter。
- 5.11.0 → **5.12.0（按用户要求清掉最后一批既有死代码）** —— 死代码清理，按规则**第二位 +1、末位归零**。
  - 删除 8 个文件（674 行）：`beef/compat/AppFluxCompat`、`beef/core/component/{ExternalInventoryKind,ExternalInventoryReference}`、`beef/core/constants/NBTConstants`、`beef/energy/{EnergyManager,IEnergyManager}`（整包；本模组自己的 `com/godofthings/energy/` 未动）、`beef/client/gui/{ScaledEnergyAmount,PinyinSearch}`。
  - `ConfigManager` **825 → 155 行（−670）**：删掉整套未接线的「配方转换」子系统（`RECIPE_CONVERSION_OPTIONS` 60 条 + `isRecipeConversionEnabled` + 约 60 个 `ENABLE_*_RECIPE_CONVERSION` 开关 + 5 个 `is*RecipeConversionEnabled` + `defineRecipeConversionOption`）、万象炉多方块段、矿物生成器段、AE2 礼物包段、机械升级段、植物盆/矩阵段，以及上列的全部 getter 与 `normalizeInventorySlots`。
    · **保留**：三个 `*_SPEC` 与 Builder（项目约定）、无用维度地板黑白名单整套（`DimensionGenerationConfig` 在用）。
    · `COMMON_SPEC` / `CLIENT_SPEC` 现在是空表 —— 已核对 NeoForge 21.1.249 源码：`Builder#build()` 只做 `ensureEmpty()` 校验（检查有没有挂着的 comment/翻译键/range/restart 标记），空表在 `validateSpec` / `correct` / `acceptConfig` 里空转，**安全**；对应 toml 会生成空文件，旧键由 `correct()` 清掉。
  - 语言键：删掉 **331 个孤儿键**（本轮 config 段 253 个 + v5.10.0 等更早清理遗留的 78 个），保留 `godofthings.configuration.{title, section.*, useless_dimension*}`；语言文件 960 → **629**。
  - **纠正一处误判（重要）**：`EnumColor` / `IColoredBlock` / `PlatformPreview` **不是**死代码 —— `PlatformPreview` 被活的 `DimensionConfigScreen`（`DimensionConfigMenu`）使用，`IColoredBlock` 又是 `PlatformPreview` 里 `instanceof` 判定用的接口，三者全部保留（`IColoredBlock` 在本仓库无实现类，是给别的模组染色方块用的判定接口）。
  - 验证：`check-lang.ps1` **OK**（**209 个 java 文件**、20 方块、17 物品、0 实体）；GameTest **19/19**；`gradlew build` 成功并部署；开发服务端加载通过。
  - 顺带说明：删掉 `PinyinSearch` 意味着上游那条「jecharacters 拼音搜索」可选集成彻底消失（此前已无调用方，属清死代码而非砍功能）；日后便签 / 手册的搜索框若要拼音，需要重写。
- 5.12.0 → **5.12.1（按用户要求 9 项修复 + 死代码清理）** —— 修复/优化 + 死代码清理，按规则**末位 +1**。
  - **① 神之护甲材质重置**：新增自定义 `ArmorMaterial`（`item/GodArmorMaterials`，注册名 `god`）—— 数值与下界合金一致，但穿戴图层指向本模组的 `textures/models/armor/godofthings_layer_1/2.png`（由原版下界合金图层按本模组「金/紫/青」色板重着色，形状/明暗结构逐像素保留，穿戴贴合度不变）；此前穿戴时用 `ArmorMaterials.NETHERITE` 的原版黑紫图层，与物品图标风格脱节。
  - **② 删 15 个死依赖 jar（libs 52.2 → 9.1 MB）**：造化杖的 14 个可选集成（Draconic / BrandonsCore / CodeChickenLib / enderio / Mekanism / Applied-Mekanistics / AppliedFlux / FluxNetworks / productivebees / occultism / modonomicon / geckolib / SmartBrainLib / ftb-teams，v5.8.0 随功能已删）+ 未被 build.gradle 引用的 `jade`。`build.gradle` 依赖块与 `fetch-libs.ps1` 下载清单同步精简到实际引用的 4 个（JEI ×2 / EMI / AE2）；`neoforge.mods.toml` 的 12 个幽灵 optional 依赖声明一并移除（与 `GodofthingsCommand.OPTIONAL_MODS` 的 5 个对齐）。
  - **③ 加速倍率 16× 抽公共常量**：此前 `count * 16` 字面量复制粘贴在 GodFurnace:146 / GodResource:241 / GodDrop:188 / GodSpawnEgg:188 四处（`GodAcceleratorItem` 注释里写着 16 倍但常量不在自己类里）。现收敛为 `GodAcceleratorItem.PARALLEL_PER_ITEM` + `multiplierFor(count)`，4 台机器统一委托；改倍率只动一处。
  - **④ 神之砍杀拆分范围 / 抢夺上限**：`setLooting` 此前误用 `MAX_RANGE`（1600）clamp 抢夺强度——两个语义不同的量共用一个常量，改范围上限会连带改抢夺上限。现拆出独立常量 `MAX_LOOTING = 1600`；类注释「范围（0-300）/抢夺（0-300）」更正为实际值 1600。
  - **⑤ 技能树属性侧死链路删除**：v5.9.0 删属性类节点后，14 个现役节点 `attrs()` 恒为 `List.of()`，`ArmorSkillEngine.applyAll/removeAll` 每次调用都空转（属性修饰符从未生效过，属「界面能开关、掉落侧生效、属性侧静默失效」的半残）。现删除整条空转链路：`ArmorSkillEngine` 的 `applyAll/removeAll/apply`、`ArmorSkillDef` 的 `attrs` 组件 / `modifierId()` / `EffectKind.ATTR`、`ModAttributes.DAMAGE_REDUCTION`（注册后无任何使用方，整类删除）、`RangedAttributeMixin`（原版属性解上限失去全部受益者，删除 + mixins.json 移除条目）、`ArmorSkillHandler.refresh` 与签名巡检（唯一作用是重挂属性；`ArmorSkillMessages` 两处调用移除，登录/重生/跨维度保留纯同步）。掉落侧 7 个倍率函数与事件逻辑不变。
  - **⑥ 其余死代码清理**：删除 `META-INF/accesstransformer.cfg`（13 条 AT 全部服务于已删的玩家保护层/造化杖，0 处引用）+ `build.gradle` 的 `accessTransformers` 行；删除 53 个空标签文件（`data/{c,gtceu,malum}` 的牛排工具工具标签，`{"values":[]}`）；删除 `Godofthings.registerGridLinkables()`（空 try 块）及调用；删除 3 处空 `renderLabels` 覆写（AbstractWaypointScreen / GodCraftConfigScreen / GodSlaughterConfigScreen）；删除孤儿纹理 `textures/item/endless_beaf_time_item.png`；清理 run 目录 15 个残留（2 个 `godofthings-staff-common.toml` + 13 个 `.bak`）。
  - **⑦ 传送点数量上限**：`WaypointData` 新增 `MAX_WAYPOINTS = 256`（此前完全无上限、可无限新建撑大存档，与便签 8 本×64 条的严格上限不对称）；超限 / 重名 / 空名新建失败时给玩家 actionbar 提示（新增语言键 `message.godofthings.waypoint.limit` / `.create_failed`，zh+en 同步）。
  - **⑧ 配置快照改实时读**：5 个 `static final int X = MachinesConfig.Y.get()`（GodDrop/GodSpawnEgg 的 `WORK_INTERVAL`、GodResource 的 `WORK_INTERVAL`、GodMiner 的 `MAX_RADIUS` / `MAX_BLOCKS_PER_TICK`）改为运行时 `.get()`——此前类加载时快照，改 toml 必须重启；现在重进世界即生效（`ticksPerColumnBase` 原本就是实时读，不变）。
  - **⑨ 文档与许可修正**：补 `LICENSES/useless-stretcher-MIT.txt`（THIRD_PARTY_NOTICES.md 引用了它但文件缺失；从 `.ref/uselessstretcher/LICENSE` 复制，Copyright (c) 2026 2i学习）；README 两处 `THIRD_PARTY_NOTICES.md` 失效链接改指 `src/main/resources/META-INF/`；README/AGENTS「6 个方块实体 implements AE2」→ **7 个**（漏怪蛋机）；AGENTS「ConfigManager 950+ 行」→ 155 行（多轮清理后）；README/AGENTS 依赖清单 19 jar/52MB → 4 jar/9MB。
  - 验证：`check-lang.ps1`、`gradlew build`、GameTest 19/19。
  - **v5.12.1 追加（同版本未发布，按用户要求继续 6 项）**：
    · **修复神之吸收抽入吞吐 bug**：`pullFrom` 原先每 tick 每面只抽 1 个就 `break`（`extractItem(s, 1)`），大容器补货时吸收速度锁死 1 个/tick/面；现改为整槽扫描、按堆叠抽取，余量塞回原槽（与 `pushTo` 全量语义对称）。
    · **README「内容一览」同步**：护甲行补专属穿戴图层说明；传送点行补「上限 256 个」；神之传输行补 `transmitter.range` 配置（0=无限距离）；技能树行补「共鸣页不再显示技能介绍」。
    · **清除指令残留文案**：`gui.godofthings.waypoint.empty` 原文「使用 /setpoint 设置」——该指令 v5.9.0 已删，改为「点击『新建』添加」（zh+en 同步）。
    · **神之传输范围配置化**：`MachinesConfig` 新增 `transmitter.range`（默认 64，范围 0-1000000，实时读）；**0 = 无限距离**（同维度内不限距离；跨维度仍由机器界面上的跨维度开关独立控制）。`GodTransmitterBlockEntity` 的 `RANGE` 常量改为 `range()` 方法 + `inRange(distSqr)` 判定，两个距离检查点统一走它。
    · **清理尾巴**：删 8 个零调用 public static 成员（`EnumColor.valuesInOrder/byName/byIndex`、`UselessDimensions.teleporterFor`、`ArmorSkillData.clientEnabled`、`ManualCatalog.invalidate/countIn`、`BlackBoxData.addToBoxBatch`、`ArmorSkillMessages.sendBulk`、`ConfigManager.isUselessDimensionFloorBlockBlacklisted`）；`GodCraftTemplateMenu` 的 3 个 `BUTTON_*` 常量改为 private 并让 `clickMenuButton` 真正引用（消除字面量 10/20/30 漂移风险）；`LootingHelper.lootingLevel` 降 private；`ClientConfig.lastSkillTab` 范围收紧 0-2（`getLastTab`/`setLastTab` 用 `floorMod` 兜底旧存档大值）；删 92 条孤儿语言键（杖 ~66 + 扳手模式 ~19 + 维度显示名 2 + 零散 5，zh+en 同步删，语言文件 631 → 540）；另删 7 条 `machine_*.desc`（共鸣页不再显示技能介绍，见下条）。
    · **神之共鸣页不再显示技能介绍**（按用户要求）：`GodArmorSkillScreen` 悬停 tooltip 仅在非共鸣页显示；那 7 个开关的名字（「神之共鸣·掉落」等）本身就是全部信息，7 条 `machine_*.desc` 语言键删除。
- 5.12.1 → **5.13.0（按用户要求：神之资源拆分为矿物 / 作物 / 方块三台）** —— 系统性新增，按规则**首位 +1、后两位归零**。
  - **拆分**：原单一「神之资源」（`god_resource`）删除，替换为三台机器，输入过滤互斥、其余行为与原机完全一致（9 输入槽不消耗模板、快配方每 tick / 慢配方每 20 tick、神之加速并行、向下自动输出、无限存储、AE2 并网、打掉不掉落）：
    · **神之矿物**（`god_ore_machine`，8 钻石块 + 下界星）：只收原矿 / 锭 / 宝石（原 `ORE_ITEM_DROPS` 15 项表）与一切矿石块（注册名含 "ore"，兼容格雷科技等模组），产出对应锭 / 成品或按矿石时运 III 掉落表复制。
    · **神之作物**（`god_crop_machine`，8 干草块 + 下界星）：只收种子 / 作物 / 树苗 / 海带 / 甘蔗 / 仙人掌 / 浆果 / 地狱疣及一切模组植物（原植物逻辑整体搬入：`SAPLING_LOG`、满熟作物按原版概率掷表、`specificPlantDrop` 专属产物、GTCEu 橡胶树苗、`_sapling→_log` 启发式）。
    · **神之方块**（`god_block_machine`，8 石英块 + 下界星）：收任意**纯方块物品**复制 64 个（原 `DUPLICATES` 名单每 tick 快配方，名单外方块 20 tick）。
  - **实现**：新增 `GodResourceVariant` 枚举（ORE/CROP/BLOCK，含 `accepts` 输入过滤 / `isFastRecipe` 快慢配方资格 / `produce` 产出计算，全部从原 `GodResourceBlockEntity` 迁出）；三台共用 `GodResourceBlock` + `GodResourceBlockEntity` + BE 类型 + 菜单/屏幕（变体由注册工厂闭包传入，BE 反序列化时按 `state.getBlock()` 回填 `setVariant`）。
  - **边界判定**：方块机的「任意方块」用 `isPureBlockItem`（BlockItem 但排除 `ItemNameBlockItem` / `StandingAndWallBlockItem`——种子/红石/花/告示牌这类「放下去变成方块」的关联物品不是方块本身；种子归作物机）。此边界由 GameTest 实测发现（种子是 BlockItem 子类）并回归覆盖。
  - **资源**：三台各有 blockstate / block+item 模型 / 方块贴图（原机身染绿 / 橙 / 保持金）/ GUI 贴图 / 配方 / 掉落表 / 成就 + 配方书解锁；`all_machines` 成就的 `has_resource` 替换为三台各自 criteria；`mineable/pickaxe` 标签同步；旧 `god_resource` 的 10 个资源文件与 5 组语言键删除，新增 15 键/语言（显示名 / 成就 / 手册）。
  - **迁移**：旧存档已放置的 `god_resource` 方块会随资源删除而消失（内部存储一并丢失）——本模组机器本就「打掉不掉落」，风险与玩家手动拆除一致；输入槽内的模板物品可换用对应新机器重新放入。
  - 验证：`check-lang.ps1` OK（542 键、22 方块、17 物品、手册 34+9 条）；GameTest **23/23**（新增 `GodResourceVariantGameTest` 4 条：矿物机只收矿 / 作物机只收植物 / 方块机收纯方块拒种子 / 三类产出计算）；`gradlew build` 成功部署。
  - **5.13.0 追加（同版本未发布，按用户要求再改两项）**：
    · **神之附魔合并天神附魔**：删除 `god_heaven_enchant` 方块（`GodHeavenEnchantBlock` 类 + 注册 + 资源 6 文件 + 贴图 2 张 + 语言键 3 组/语言 + `god_enchant` 成就的 `has_heaven` criterion）；`GodEnchantMenu` 移除 `heavenly` 字段与全部分支——统一按 `HEAVENLY_ENCHANT_MAX_LEVEL`（255）处理，**选中附魔即默认 255 级**（原天神行为成为唯一行为）；`GodEnchantBlock.useWithoutItem` 不再写 boolean、BE `createMenu` 不再按方块判型；BE 类型 valid blocks 收窄到 `god_enchant`；`GodEnchantMenu.stillValid` 只查神之附魔。语言文件 542 → 539，手册条目更新为 255 级语义。
    · **神之熔炉输入输出 6+6 → 9+9**：`INPUT_SLOT_COUNT` / `OUTPUT_SLOT_COUNT` 6 → 9（TOTAL_SLOTS 12 → 18）；GUI 贴图补 5 个槽框（输入行 (116,17)(134,17)、输出行 (116,53)(134,53)、加速槽新位 (152,35)），清除原右侧深色面板；加速槽坐标 116 → 152（Screen 的 hover 判定与标签同步）；「输入/输出」文字标签删除（9+9 满行后无空白区，槽位排列本身已表达分区）。旧存档迁移逻辑（`oldSize/2` 推断旧输出区 → 搬到新输出区）不变，6 槽存档读回自动迁移到 18 槽。
    · 验证：`check-lang.ps1` OK（539 键、21 方块、手册 33+9）；GameTest **23/23**；`gradlew build` 成功部署。
  - **5.13.0 追加 2（同版本未发布，按用户要求）：新增「神之测量」** —— 卷尺物品，自 Mrbysco 的 **Measurements**（MIT License）移植并改名，行为原封不动：**右击方块**定起点，再右击定终点，画线框并显示 X / Y / Z 三轴长度数字（可继续开下一个框）；**潜行右击**撤销上一个框；**不手持 / 换维度 / 进出存档自动清空**。测量纯客户端（服务端零参与、无网络包），右击已被测量吸收（不会误触方块功能）。
    · **实现**：新增 `measurement/` 包 6 文件 —— `GodMeasureItem`（卷尺物品，useOn 只在客户端真人时接 `MeasureBoxHandler`）、`MeasureBoxHandler`（测量框列表）、`MeasureBox`（线框 + 三轴数字渲染，含最近可见边挑选与 0.5 格边距 clamp；原作致谢 MadeBaruna/BlockMeter）、`MeasureRenderType`（无深度测试线框 RenderType，画到 outline 目标层）、`MeasureLineColor` / `MeasureTextColor`（颜色配置枚举，XYZRGB = 按轴红绿蓝）；原作的跨平台注册抽象（RegistrationProvider / Services / platform）按本模组惯例收编：注册走 `Godofthings.ITEMS`，事件走 `@EventBusSubscriber(Dist.CLIENT)` 游戏总线（`MeasureClientHandler`：tick 跟随准星 / 世界渲染后画框 / 进出存档清空）。
    · **配置**：`ClientConfig`（godofthings-client.toml）新增 `[measurement]` 段 5 项 —— `lineColor` / `textColor`（枚举）/ `textSize`（0.01-0.10）/ `lineWidth` / `lineWidthMax`（距离 >48 格用后者）。
    · **资源**：16×16 贴图（原 tape_measure2）与物品模型（含第一/三人称显示变换）按本模组命名改为 `god_measure`；配方照搬原作（灰羊毛 + 铁锭 + 黄羊毛）；创造标签加在「神之更改」后；手册条目 zh/en 双语；README 内容一览与包结构同步。
    · 验证：`check-lang.ps1` OK（541 键、21 方块、18 物品、手册 34+9）；GameTest **23/23**；`gradlew build` 成功部署。
  - **5.13.0 追加 3（同版本未发布，按用户要求）：「神之方块」改名「神之复制」并放开收一切物品**：
    · **改名**：注册名 `god_block_machine` → `god_duplicate_machine`（`GodResourceVariant.BLOCK` → `DUPLICATE`；注册处 / BE 占位变体与显示名 / `GodResourceMenu.stillValid` / 成就 icon 与 title / criterion `has_resource_block` → `has_duplicate` / 掉落表 / `mineable/pickaxe` 标签 / blockstate / 方块+物品模型 / 方块贴图 ×2 / GUI 贴图 / 配方文件 / 语言键 6 组 ×2 语言全部同步改名）。v5.13.0 未发布故无旧存档迁移负担（此前放置的 `god_block_machine` 会随改名消失，与 v5.13.0 删 `god_resource` 的处理口径一致）。
    · **放开输入**：`accepts` 的 DUPLICATE 分支由 `isPureBlockItem`（纯方块物品，排除 `ItemNameBlockItem` / `StandingAndWallBlockItem`）改为 **true（一切物品）**——原版 / 模组任意物品都复制 64 个；三台机器的输入过滤从此不再互斥（同一物品可进多台）。产出仍为**全新堆叠**（输入上的附魔 / 耐久 / 容器内容不带入产物）；快配方资格不变（原 DUPLICATES 名单方块 + AE2 天陨石每 tick，其余物品每 20 tick）。`isPureBlockItem` 随之零调用删除。
    · **测试**：`GodResourceVariantGameTest` 方块机过滤测试改为复制机「收一切」（方块 / 种子 / 木棍 / 钻石剑 / 成书均接受），产出测试补「木棍（非方块物品）→ 64 木棍」。
    · ⚠ **有意未动**：套装技能树 / 神之共鸣里的「神之方块」节点（`block_drop`，原「点石成金」）——它是按玩家保存的技能 id 且属套装技能体系，是否随机器改名（如改「神之复制」）待用户定。
    · 验证：`check-lang.ps1` OK（541 键、21 方块、18 物品、手册 34+9）；GameTest **23/23**；`gradlew build` 成功部署。
  - **5.13.0 追加 4（同版本未发布）：体检修复 `mineable/pickaxe` 残留引用** —— 天神附魔方块已在本批次删除，但标签文件里的 `godofthings:god_heaven_enchant` 漏删：运行期整个 `minecraft:mineable/pickaxe` 标签加载失败（TagLoader ERROR），**全部 21 个模组方块静默丢失镐类挖掘属性**（GameTest 不覆盖标签加载、check-lang 不查标签，故一直没发现）。已删该条目（重跑 gametest 确认零 ERROR / 警告消失）；`check-lang.ps1` 新增「`data/**/tags/**` 的 godofthings: 条目 ↔ 注册表」校验（负向测试已验证能抓此类残留，防复发）。
  - **5.13.0 追加 5（同版本未发布，按用户要求清理遗留）**：删除仓库根 `LICENSES/useless-stretcher-MIT.txt`（权杖 v5.8.0 已整体删除，磁盘残留孤儿文件）与 `.tmp/`、`.tmp_furnace_new.png`（熔炉 GUI 制作期的工作图）；同步清第三方声明 —— `THIRD_PARTY_NOTICES.md` 删「Useless Stretcher」整节与只为其而设的「JDTE」节（连带删两处 `JDTE-MIT.txt`，代码 0 引用），「Useless Mod」照抄范围收窄到存活的无用维度（造化杖已删），「本模组原创部分」清单修剪已删条目（剑 / 炮 / 记录 / 超平坦）；`neoforge.mods.toml` credits 去掉 `useless_stretcher`。
  - **5.13.0 追加 5（同版本未发布，按用户要求清理遗留）**：删除仓库根 `LICENSES/useless-stretcher-MIT.txt`（权杖 v5.8.0 已整体删除，磁盘残留孤儿文件）与 `.tmp/`、`.tmp_furnace_new.png`（熔炉 GUI 制作期的工作图）；同步清第三方声明 —— `THIRD_PARTY_NOTICES.md` 删「Useless Stretcher」整节与只为其而设的「JDTE」节（连带删两处 `JDTE-MIT.txt`，代码 0 引用），「Useless Mod」照抄范围收窄到存活的无用维度（造化杖已删），「本模组原创部分」清单修剪已删条目（剑 / 炮 / 记录 / 超平坦）；`neoforge.mods.toml` credits 去掉 `useless_stretcher`。
- 5.13.0 → **5.14.0（按用户要求：新增 ME 无限存储元件四件套）** —— 新增小物品，按规则**第二位 +1、末位归零**。
  - **功能**：4 个 ME 无限存储元件（物品 id `infinity_water_cell` / `infinity_lava_cell` / `infinity_cobblestone_cell` / `infinity_blank_pattern_cell`），放入 ME 驱动器后网络内**永远有 21.4 亿个（`Integer.MAX_VALUE × amountPerUnit`）绑定资源**可无限存取：不占字节、不写 NBT（persist 空操作，拆下重放不变）、驱动器 LED 常亮黄（NOT_EMPTY）、闲置耗电 8 AE/t（上游默认值）。名字走 `ME无限%s元件` 模板键（%s = 资源显示名），悬浮提示带内容预览图。
  - **实现**：新增 `infinitecell/` 包 2 文件 —— `InfinityCellItem`（AEBaseItem 子类，AEKey 延迟到首次使用解析，避免注册期取 AE2 物品）与 `InfinityCellInventory`（StorageCell：insert/extract 对绑定 Key 全量放行，静态 ICellHandler）。`commonSetup` 的 `enqueueWork` 里 `StorageCells.addCellHandler` + 4 次 `StorageCellModels.registerModel`（驱动器槽位模型）。自 **ExtendedAE**（glodblock，LGPL-3.0）的 `ItemInfinityCell` / `InfinityCellInventory` 移植改名，行为原封不动；水 / 圆石两件为上游既有，熔岩 / 空白样板按同一模式补齐。
  - **资源**：物品模型（layer0 自贴图 + layer1 AE2 LED 覆盖层）×4、驱动器槽位模型 ×4（上游同一 Blockbench 模型，贴图改 godofthings 命名空间）、驱动器贴图与水/圆石/熔岩物品贴图分别取自 ExtendedAE 与整合包 kubejs 素材（infinitycell 命名空间）、空白样板贴图以水元件为底按空白样板配色重染 34 像素；配方 ×4（水 / 圆石逐字照抄上游仅改结果 id，熔岩 / 空白样板按同构补齐：石英玻璃 ×2 + 16k 存储组件 + 钻石 ×3 + 水桶×4 / 水桶×2+熔岩桶×2 / 熔岩桶×4 / 空白样板×4）；创造标签加在神之测量后。
  - **声明**：`THIRD_PARTY_NOTICES.md` 增「ExtendedAE —— LGPL-3.0」节（两个移植类保持 LGPL-3.0），`LICENSES/` 与 jar 内 `META-INF/` 各补一份全文，credits 同步；README 内容一览与包结构同步。
  - 验证：`check-lang.ps1` OK（551 键、21 方块、22 物品、手册 38+9）；GameTest **23/23**；`gradlew build` 成功部署。
- 5.14.0 → **5.15.0（按用户要求：新增神之去皮机器）** —— 新增一台机器，按规则**第二位 +1、末位归零**。
  - **功能**：神之去皮（方块 id `god_peeler`）—— 自动去皮机：原木放进输入槽，自动变成对应的去皮原木（1 进 1 出，1:1 转化）。原版 11 组全覆盖（8 种原木/木头 + 绯红/诡异菌柄/蕈柄 + 竹块）；模组原木按惯例命名（`xxx_log / xxx_wood / xxx_stem / xxx_hyphae` 且存在 `stripped_` 孪生）自动支持；已去皮的原木不再收（机器天然拒绝二次去皮）。无需能源；漏斗 / 管道可从任意面塞原木、取成品（输入只进不出、输出只出不进）。每 5 tick 去皮 1 个，`godofthings-machines.toml` 的 `[peeler].workInterval` 可调（1~100000）。
  - **实现**：`block/entity/machine/LogStripper.java`（原木→去皮映射：懒构建的注册表启发式扫描——路径像原木且 `stripped_ + 路径` 孪生真实存在才收录；独立成类供 GameTest 静态测，同 DropLootRoller / SpawnEggHelper 做法）+ `GodPeelerBlock` / `GodPeelerBlockEntity`（1 输入 + 1 输出 ItemStackHandler，输出槽 insert 恒拒绝；组合 IItemHandler 暴露给任意面：槽 0 只进、槽 1 只出；NBT 存双槽与计数器；无 AE2 依赖）+ `GodPeelerMenu` / `GodPeelerScreen`（vanilla 菜单同步，无自定义网络包）。`MachinesConfig` 加 `peeler.workInterval`。
  - **资源**：方块贴图 side/top 复用资源系列贴图底子、仅强调色像素重染为浅去皮木色（与神之复制的饱和橙区分）；GUI `god_peeler.png`（176×166：双槽 + 转化箭头 + 玩家物品栏，原版风格程序生成，输入/输出标签复用 `gui.godofthings.furnace.input/output`）；blockstate / 方块模型 / 物品模型 / 战利品表 / 配方（8 任意原木 `#minecraft:logs` + 下界星）/ 配方进度 + 成就 `god_peeler` / mineable-pickaxe 标签；语言键 5 组 ×2 语言（含手册条目与成就「褪去树皮 / Bark Be Gone」）；创造标签加在神之复制后。
  - 验证：`check-lang.ps1` OK（556 键、22 方块、22 物品、手册 39+9）；GameTest **27/27**（新增 `GodPeelerGameTest` 4 条：原版 11 组映射、木头/蕈柄、已去皮与非原木拒绝、convertOne 进出与满槽停）；`gradlew build` 成功部署。
- 5.15.0 → **5.15.1（按用户要求：神之去皮改为神之熔炉同款 9+9 布局）** —— 修复 / 优化，按规则**末位 +1**。
  - **功能**：输入 / 输出槽 1+1 → **9+9**（3×3 两行，输入 i 一一对应输出 i，槽位坐标与神之熔炉完全一致），右侧加**神之加速槽**：并行倍率 = 每周期每个输入槽去皮「倍率」个原木（每个加速 ×16，一组 64 个 = ×1024），槽位容量随倍率提升（单堆上限 99，同熔炉）；5.15.0 的单槽旧存档自动迁移（旧 InputSlot / OutputSlot → 新布局槽 0 / 槽 9）。
  - **实现**：GodPeelerBlockEntity 改为单个 18 格合并 ItemStackHandler（输入槽 isItemValid = LogStripper.isStrippable、输出槽对外插入恒拒绝、getSlotLimit = min(99, 64×倍率)，照抄熔炉）；convertOne(handler, 输入槽, 输出槽, 倍率) 返回实际去皮数；自动化视图 19 槽（0-8 只进、9-17 只出、18 加速槽对外封闭）；菜单 / 界面照抄熔炉坐标（212×166 画布、加速槽 178,35、悬浮显示并行倍率；无齿轮 / 面配置——六面通用进出）。
  - **资源**：GUI 贴图重做（212×166：9+9 槽框 + 中间向下转化箭头 + 右侧加速槽框）；手册条目 zh / en 更新（9+9 布局与并行说明）。
  - 验证：`check-lang.ps1` OK（556 键、22 方块、手册 39+9）；GameTest **28/28**（GodPeelerGameTest 4→5 条：新增并行倍率四轮耗尽与 9 槽独立去皮）；`gradlew build` 成功部署。
- 5.15.1 → **5.15.2（修复：去皮机界面贴图完全错乱）** —— 修复 / 优化，按规则**末位 +1**。
  - **根因**：v5.15.1 重做的 GUI 贴图存成 212×166，而全套 house GUI 惯例是 **256×256 画布**（熔炉 / 资源三机等均为 256×256，实测确认）；1.21.1 六参 `GuiGraphics.blit` 默认按 256×256 采样 —— 采样区 (0,0)-(212,166) 映射到贴图实际像素只有左上约 176×108，整个界面被拉伸裁切成完全错乱的画面，槽位位置全错（看起来像机器「停了」）。
  - **修复**：贴图按惯例重制为 256×256 画布（内容仍画在左上 212×166，9+9 槽 / 向下箭头 / 加速槽不变）；机器逻辑零改动。
  - 验证：`check-lang.ps1` OK（556 键）；GameTest **29/29** 复跑全过；`gradlew build` 成功部署。
- 5.15.2 → **5.15.3（按用户要求：GUI 直接复制神之熔炉 + 面配置照搬）** —— 优化 / 小新增，按规则**末位 +1**。
  - **功能**：去皮机 GUI **直接复制神之熔炉的贴图**（`god_peeler.png` / `god_peeler_config.png` = 熔炉对应贴图的逐字节复制——两者槽位坐标完全一致，齿轮图标在备用区 UV (8,170) 一并带来）；并加入**与神之熔炉同一套的六面输入 / 输出配置**：主界面右上角齿轮按钮 → 面配置界面（3×2 方向磁贴：无 / 输入 / 输出 / 输入和输出，循环切换），**默认全部无**（先配置面，漏斗 / 管道才能进出——与熔炉行为一致）；INPUT 面自动从相邻容器抽入可去皮原木（同物品优先、否则空槽），OUTPUT 面自动把成品推给相邻容器，BOTH 面两者都做。
  - **实现**：GodPeelerBlockEntity 加 `faceModes[6]` + 每面 `SideHandler`（照抄熔炉：insert 只进输入槽 / extract 只出输出槽 / isItemValid 按 LogStripper 过滤），`getSideCapability(side)`（NONE 面返回 null）替换原「任意面通用」的 19 槽组合视图；tick 先 autoTransfer（每 tick 抽推）再去皮（每间隔一轮）；NBT 存 `FaceModes`。新增 `GodPeelerConfigMenu`（6 个面模式 DataSlot + 按钮 0-5 循环 / 6 返回）与 `GodPeelerConfigScreen`（照抄熔炉面配置界面，语言键全部复用 face_config / mode.* / dir.*，仅新增 `container.godofthings.god_peeler` 标题键）。
  - 验证：`check-lang.ps1` OK（557 键）；GameTest **30/30**（新增 FaceMode 往返 / 越界折回测试，与熔炉取模语义一致）；`gradlew build` 成功部署。
- 5.15.3 → **5.15.4（修复：神之共鸣对神之机器不起效果）** —— 修复，按规则**末位 +1**。
  - **根因**：共鸣 7 开关（machine_*）只有技能定义与事件侧消费（ArmorSkillHandler 按 FakePlayer 归属判定），但机器侧三处断链——①所有机器不记录主人（无 owner 字段）；②砍杀机假玩家用共享假 UUID（`FakePlayerFactory.getMinecraft`），事件侧 `ownerOf` 按 UUID 反查在线主人永远查不到；③矿机不走事件（`Block.getDrops` 直取）、资源三机 / 掉落机 / 怪蛋机 / 吸收器也没有任何共鸣钩子——共鸣对机器恒不生效。
  - **修复**：①新增 `MachineOwner` 接口（owner UUID + NBT 持久化），6 台 BE（砍杀 / 吸收 / 矿机 / 资源三机 / 掉落机 / 怪蛋机）实现，6 个方块类 `setPlacedBy` 记录放置者；②砍杀机假玩家改用主人 GameProfile（`FakePlayerFactory.get(level, new GameProfile(owner, "GodofThings"))`），生物掉落 / 头颅 / 怪蛋 / 经验四条共鸣经既有事件链自动生效；③`ArmorSkillHandler` 新增直查辅助（`resonantLevels` + `lootBombBoost` / `blockDropBoost` / `xpBoost` / `autoSmeltOn`，主人在线 + 穿齐全套 + 共鸣开才生效），矿机（点石成金 × 财源滚滚 + 自动熔炼）、资源三机 / 掉落机 / 怪蛋机（战利品爆炸倍率）、吸收器（经验飞涨）直挂共鸣；`smeltResult` 拆出 MinecraftServer 重载供机器用。
  - 验证：`check-lang.ps1` OK（557 键）；GameTest **33/33**（新增共鸣判定 3 条：离线 / 未记录主人不生效、倍率回退中性、开关存储往返）；`gradlew build` 成功部署。
- 5.15.4 → **5.15.5（按用户要求：神之熔炉恢复 AE 并网）** —— 优化 / 小新增，按规则**末位 +1**。
  - **功能**：熔炉重新接回 AE 网络（v5.1.3 时按当时要求拆除，本次照抄资源三机现成模式恢复）——线缆直连、**占一个频道**、`REQUIRE_CHANNEL`；熔炼产物自动输出进 ME 网络（**只推输出槽 9-17**，输入槽是熔炼队列不推），每 20 tick 节流推一次；界面加速槽左侧「AE」开关（绿=推送 / 灰=关闭，与资源系列同一画法与按钮位），开关状态随 NBT 持久化（旧存档缺键默认开）。荒辰移晷之杖已在 v5.8.0 删除，本次只恢复线缆直连一条路。
  - **实现**：GodFurnaceBlockEntity `implements IGridConnectedBlockEntity`（appeng.me.helpers）+ `AeGridNode`（`setInWorldNode(true)` + `setVisualRepresentation` 方块图标）；`getMainNode`/`saveChanges`/`onLoad`/`setRemoved` 四钩子 + `pushOutputToAe`（MEStorage.insert + extractItem 回收）+ `pushOutputToAeThrottled`；能力注册恢复 `AECapabilities.IN_WORLD_GRID_NODE_HOST`（替换 v5.1.3 注释）；Menu 加 DataSlot 同步 + `clickMenuButton(10)`（与面配置按钮 6 并存）；Screen 加 AE 按钮（150,6，齿轮 179,9 旁不重叠）。
  - 验证：`check-lang.ps1` OK（557 键，手册 god_furnace 与 system.ae2 双语 7→8 台同步）；GameTest **33/33** 复跑全过（AE 网格行为与其余机器一致，无新增测试——AE2 并网无法在纯 GameTest 里架设网格）；`gradlew build` 成功部署。
- 5.15.5 → **5.15.6（按用户要求：五件物品可放进工具皮带 + 神之绑定器改名神之绑定）** —— 改名 / 澄清，按规则**末位 +1**。
  - **考证**：解包用户提供的 `D:\下载\ToolBelt-1.21.1-2.2.10.jar`（dev.gigaherz.toolbelt）全量反汇编——腰带收纳格（`BeltAttachment` 的匿名 `ItemStackHandler$1`）**只覆写 `onContentsChanged`（同步通知），整个模组没有任何 isItemValid / mayPlace / 标签准入**：工具皮带收纳格不挑物品，任何物品都能直接放进去快捷切换；它唯一自带的标签是 `data/curios/tags/item/belt.json = ["toolbelt:belt"]`（把自己的腰带物品装备进 Curios 默认 belt 槽）。
  - **修正**：删除上一轮误加的 `curios:belt` 标签文件（那是「把物品**当腰带穿**」的准入标签，会让五件物品与真正的腰带竞争装备槽，并非「放进腰带收纳」）——**五件物品（请神 / 神之更改 / 神之测量 / 神之黑盒 / 神之绑定）无需任何改动即可放进工具皮带**；手册条目（zh/en ×5）与 README 绑定行改为如实说明「可直接放进 ToolBelt 快捷切换、不限物品类型」。神之绑定器 → **神之绑定**改名保留（仅显示名，物品 id `god_binder` 不变，旧存档无损）。
  - 验证：`check-lang.ps1` OK（557 键、标签文件回至 4 个）；GameTest **33/33**；`gradlew build` 成功部署。
- 5.15.6 → **5.15.7（修正工具皮带考证结论：准入是配置白名单，不是「不挑物品」）** —— 澄清 / 文档更正，按规则**末位 +1**。
  - **上轮结论有误**：v5.15.6 只反汇编了 `BeltAttachment$1`（它确实只覆写 onContentsChanged），但漏了真正的闸门——**`common/BeltSlot.mayPlace → ConfigData.isItemStackAllowed`**：白名单命中→放行、黑名单命中→拒绝、腰带本身→拒绝、`allowAllNonStackableItems=true` 且不可堆叠（maxStackSize==1）→放行、**其余一律拒绝**。用户实测五件物品（四件可堆叠 64）全部放不进去，即被此闸门拦下。
  - **准入来源**：`ConfigData$ServerConfig` 的 `whitelist` / `blacklist`（TOML 字符串列表）+ `allowAllNonStackableItems` 开关；解析方法 `parseItemStack` = `BuiltInRegistries.ITEM.get(ResourceLocation.parse(str))`——**仅支持物品 id、不支持标签**（ToolBelt.class 里的 `itemTag` 工厂为零调用的死代码）。
  - **修正**：①用户实例 `config/toolbelt-server.toml` 的 whitelist 加入五件物品 id（`godofthings:god_invite / god_change / god_measure / god_black_box / god_binder`），重启后即可放入；②手册条目（zh/en ×5）与 README 绑定行改为如实说明「在 toolbelt-server.toml 白名单加物品 id」（默认仅收不可堆叠物品，不支持标签）；③本模组代码零改动。
  - 验证：`check-lang.ps1` OK（557 键）；GameTest **33/33**；`gradlew build` 成功部署。
- 5.15.7 → **5.15.8（按用户要求：换新包不改配置也能放进工具皮带）** —— 功能 / 体验优化，按规则**末位 +1**。
  - **需求**：上一版的「改实例配置白名单」方案要求每个整合包都手动加 id——用户要的是**开箱即用**：换任何新包、不动 ToolBelt 配置，五件物品照样能放进腰带。
  - **实现**：新增 `handler/ToolBeltCompat.java`——在 ToolBelt 配置重建之后的时机（`ServerStartingEvent` + `PlayerLoggedInEvent` 兜底）反射把五件物品（请神 / 神之更改 / 神之测量 / 神之黑盒 / 神之绑定）注入其 `ConfigData.whiteList` 私有静态集（`modId=toolbelt`、字段名均经用户实例里的真实 jar 复核）。**零配置、零硬依赖**：未装 ToolBelt 时直接跳过；注入幂等（`ItemStack.isSameItem` 查重），配置重载重建集合后由下一次事件再注入；手动改配置的条目与注入并存（白名单优先于黑名单）。
  - **同步**：手册条目（zh/en ×5）与 README 绑定行改为「模组自动注入白名单，换包无需改配置」；测试实例 toml 的 whitelist 回滚为空（由自动注入接管）。
  - 验证：`check-lang.ps1` OK（557 键）；GameTest **34/34**（新增 ToolBelt 兼容测试：未装 ToolBelt 时注入为无操作不抛错）；`gradlew build` 成功部署。
