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
