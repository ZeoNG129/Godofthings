package com.godofthings.compat.aeind;

/**
 * Applied Industrialization（modId {@code aeind}）「样板输入仓」槽位扩充补丁的参数。
 *
 * <h2>为什么需要这个补丁</h2>
 * <p>aeind 的「样板输入仓」是一套给 Modern Industrialization 多方块用的 AE2 样板供应器。
 * 它的样板槽位数被写成了**编译期常量**（{@code public static final int PATTERN_SLOTS = 9}），
 * javac 会把这个值**内联进每一个使用点**，因此运行时改字段是无效的；
 * 而且该模组**完全没有配置文件**（源码里没有任何 {@code ModConfigSpec}／config 类），
 * 所以在配置里也找不到开关。</p>
 *
 * <h2>真正的可打点</h2>
 * <p>槽位数的**唯一来源**是两处把常量传给 AE2 的构造器调用
 * （{@code appeng.helpers.patternprovider.PatternProviderLogic(IManagedGridNode, PatternProviderLogicHost, int)}，
 * 第三个参数就是样板库存容量）：</p>
 * <ul>
 *   <li>{@code HatchPatternProviderLogic.<init>} → {@code super(node, host, 9)}
 *       —— 服务「样板输入仓」与「高级样板输入仓」（后者的 logic 继承前者，也走这一处）；</li>
 *   <li>{@code ExtendedHatchPatternProviderLogic.<init>} → {@code super(node, host, 36)}
 *       —— 服务「高级扩展样板输入仓」。</li>
 * </ul>
 * <p>两个 Mixin 分别在 {@code <init>} 里对 AE2 super 构造器的第 3 个实参做 {@code @ModifyArg}，
 * 只动这一个参数，不碰同类里其它数字（例如扩展类里 {@code new List[36]} 的房间数组）。</p>
 *
 * <h2>为什么不用改 jar</h2>
 * <p>这样不需要修改 aeind 的 jar（它声明 All Rights Reserved），补丁只在本模组的 Mixin 里生效，
 * 且由 {@link AeindMixinPlugin} 保证**未安装 aeind 时完全不加载**；想关掉只需移除本模组或
 * 去掉 mixin 配置。</p>
 *
 * <h2>界面</h2>
 * <p>aeind 的这两个菜单继承 AE2 自己的 {@code PatternProviderMenu}，槽位是**按库存容量动态创建**的
 * （菜单里没有硬编码 9），而 AE2 的 {@code pattern_provider.json} 界面样式把样板槽声明为
 * {@code "grid": "HORIZONTAL"} 的**横向滚动槽面** —— 因此槽位变多之后是可以滚动显示的。</p>
 */
public final class AeindPatternSlots {

    /** 本补丁的日志器（主类 Godofthings 的 LOGGER 是 private，这里独立取一个）。 */
    public static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** 目标模组 id。 */
    public static final String AEIND_MODID = "aeind";

    /** 「样板输入仓」/「高级样板输入仓」的槽位倍数：9 → 45。 */
    public static final int PROVIDER_MULTIPLIER = 5;

    /** 「高级扩展样板输入仓」的槽位倍数：36 → 180。 */
    public static final int EXTENDED_MULTIPLIER = 5;

    private AeindPatternSlots() {}
}
