package com.godofthings.godgrid;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * 神之网格机器对外契约：六台机器（熔炉 / 矿物 / 复制 / 去皮 / 掉落 / 怪蛋）的方块实体实现它，
 * 共用的 {@link AbstractGodGridMenu} 与 {@link AbstractGodGridScreen} 只认这个接口，
 * 因此界面代码一份即可服务全部机器。
 */
public interface GodGridHost
{
    /** 机器槽位处理器：前 {@link GodGridLayout#INPUT_COUNT} 格输入，其后 {@link GodGridLayout#OUTPUT_COUNT} 格输出 */
    IItemHandler gridItems();

    /** 神之加速槽（单格，只接受神之加速） */
    IItemHandler gridAccel();

    /** AE 并网开关（界面「AE」按钮渲染用） */
    boolean gridAeEnabled();

    /** 切换 AE 并网开关 */
    void gridToggleAe();

    /** 当前排序模式序号（0=名称 1=模组 2=数量），用于按钮文案与同步 */
    int gridSortMode();

    /** 轮换排序模式并整理输出槽，返回切换后的模式序号 */
    int gridSortOutputs();

    /** 打开该机器的六面 I/O 配置界面（服务端执行） */
    void gridOpenFaceConfig(ServerPlayer player);

    /** 是否支持六面配置页签（没有面配置的机器返回 false，界面就不会画这个按钮） */
    default boolean gridSupportsFaceConfig()
    {
        return true;
    }

    /**
     * 某个机器槽的真实存量（自管 long 槽的机器才有意义）。
     * 返回 -1 表示「不显示」（默认）；界面只对 &gt; 99 的槽在右下角标紧凑数字。
     */
    default long gridAmount(int machineSlot)
    {
        return -1L;
    }

    /** 是否有自管 long 槽（true 才会为该机器开启数量同步与界面大数字，其余机器零变化） */
    default boolean gridHasLongAmounts()
    {
        return false;
    }
    /** 单列每 tick 的并行操作数（内置 64 × 加速倍率），仅用于界面提示 */
    int gridOperationsPerColumn();
}
