package com.godofthings.overloadedinterface;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.helpers.InterfaceLogic;

/**
 * 过载接口逻辑（移植自 AE2LT 的 OverloadedInterfaceLogic，裁掉反射改配置/存储那套）。
 *
 * <p>上游用反射把 AE2 {@code InterfaceLogic} 的私有 config/storage/upgrades
 * 换成自己的实现（无限槽、代理 ME 网络等）。这里改成直接继承、只加 ticker：
 * AE2 原生的 {@code InterfaceLogic} 已经自带 36 槽配置与存储的行为，
 * 我们只需要一个每 1~5 tick 的 ticker 去驱动导出 / 导入。</p>
 *
 * <p>注意：AE2 的 {@code InterfaceLogic} 构造函数本身会注册自己的 stocking ticker；
 * 这里用 {@code mainNode.addService} 覆盖成我们的 ProxyTicker（同一 ClassMap 的
 * 最后写入生效）。</p>
 */
public class OverloadedInterfaceLogic extends InterfaceLogic
{
    /** AE2 默认 Interface tick 是 5~120，空转太慢；这里覆盖成 1~5 保证响应。 */
    private static final int MIN_TICKS = 1;
    private static final int MAX_TICKS = 5;

    private final OverloadedInterfaceBlockEntity owner;

    public OverloadedInterfaceLogic(IManagedGridNode gridNode, OverloadedInterfaceBlockEntity host,
            net.minecraft.world.item.Item item, int slots)
    {
        super(gridNode, host, item, slots);
        this.owner = host;
        // 覆盖 AE2 原生注册的 stocking ticker（ClassToInstanceMap 后写生效）
        gridNode.addService(IGridTickable.class, new ProxyTicker());
    }

    public OverloadedInterfaceBlockEntity getOwner()
    {
        return this.owner;
    }

    /** 过载接口专用的 ticker：只驱动导出 / 导入，不做原版的补货。 */
    private class ProxyTicker implements IGridTickable
    {
        @Override
        public TickingRequest getTickingRequest(IGridNode node)
        {
            return new TickingRequest(MIN_TICKS, MAX_TICKS, false);
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall)
        {
            // 节点不活跃时保持最大间隔轮询：通道/供电恢复不会稳定触发 alertDevice，
            // 直接 SLEEP 会让接口卡死
            if (!mainNode.isActive())
            {
                return TickRateModulation.IDLE;
            }
            boolean hasWork = owner.hasGridItemIoWork();
            if (hasWork)
            {
                owner.tickGridItemIo();
            }
            return hasWork ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
        }
    }
}
