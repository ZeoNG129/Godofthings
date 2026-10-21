package com.godofthings.construction;

import appeng.api.config.Actionable;
import appeng.api.ids.AEComponents;
import appeng.api.implementations.blockentities.IWirelessAccessPoint;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 神之建筑的 AE 取物入口：材料在背包里凑不够时，从「物品上绑定的那张 ME 网络」里抽。
 *
 * <p>链路照 Applied Construction Sticks（作者 benbenlaw）的 AE2Util / HandlerAE2 精简而来：
 * 物品数据组件 {@code ae2:wireless_link_target}（{@link GlobalPos}）指向一个无线访问点，
 * 访问点所在网络的 {@link MEStorage} 才是真正的库存。任何一环断掉都返回 0，
 * 调用方按「材料不够」处理，不会抛异常、也不会误扣。</p>
 */
public final class ConstructionAeSource
{
    private ConstructionAeSource()
    {
    }

    /**
     * 从无线连接的网络里抽出最多 {@code want} 个 {@code wanted}。
     *
     * @return 实际抽出的数量；0 = 没绑网络 / 绑的东西不是无线访问点 / 区块没加载 / 网络里没有这种物品
     */
    public static long extract(ServerPlayer player, ItemStack wand, ItemStack wanted, int want)
    {
        if (want <= 0 || wanted.isEmpty())
        {
            return 0;
        }

        MEStorage storage = getStorage(player, wand);
        if (storage == null)
        {
            return 0;
        }

        // 空栈的 of 会返回 null，必须先判空
        AEItemKey key = AEItemKey.of(wanted);
        if (key == null)
        {
            return 0;
        }

        return storage.extract(key, want, Actionable.MODULATE, IActionSource.ofPlayer(player));
    }

    /** 物品上的无线连接 → 目标网络的存储；任一步不成立返回 null */
    private static MEStorage getStorage(ServerPlayer player, ItemStack wand)
    {
        GlobalPos linked = wand.get(AEComponents.WIRELESS_LINK_TARGET);
        if (linked == null)
        {
            return null;
        }

        MinecraftServer server = player.getServer();
        if (server == null)
        {
            return null;
        }

        ServerLevel linkedLevel = server.getLevel(linked.dimension());
        if (linkedLevel == null || !linkedLevel.isLoaded(linked.pos()))
        {
            return null;
        }

        BlockEntity blockEntity = linkedLevel.getBlockEntity(linked.pos());
        if (!(blockEntity instanceof IWirelessAccessPoint accessPoint))
        {
            return null;
        }

        IGrid grid = accessPoint.getGrid();
        if (grid == null)
        {
            return null; // 访问点没供电 / 没连上网络
        }

        return grid.getStorageService().getInventory();
    }
}
