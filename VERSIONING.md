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
