package com.godofthings.jade;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import com.godofthings.Godofthings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 兼容：神之机器接入 AE 网络时，在 Jade 提示里显示「设备在线 / 设备离线」。
 * <p>
 * 在线状态在服务端收集（grid node isActive），经 Jade 服务端数据机制同步到客户端显示——
 * 因为客户端 BE 不创建 grid node（getGridNode 返回 null），直接客户端判断会恒离线。
 */
@WailaPlugin(Godofthings.MODID)
public class GodofthingsJadePlugin implements IWailaPlugin
{
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "ae_status");

    @Override
    public void register(IWailaCommonRegistration registration)
    {
        registration.registerBlockDataProvider(AeServerDataProvider.INSTANCE, Block.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration)
    {
        registration.registerBlockComponent(AeStatusProvider.INSTANCE, Block.class);
    }

    /** 服务端：收集 AE 在线状态写入 NBT。 */
    public enum AeServerDataProvider implements IServerDataProvider<BlockAccessor>
    {
        INSTANCE;

        @Override
        public ResourceLocation getUid()
        {
            return UID;
        }

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor)
        {
            BlockEntity be = accessor.getBlockEntity();
            if (be instanceof IInWorldGridNodeHost host)
            {
                IGridNode node = host.getGridNode(null);
                data.putBoolean("AeOnline", node != null && node.isActive());
            }
        }
    }

    /** 客户端：显示在线状态（读服务端同步的数据）。 */
    public enum AeStatusProvider implements IBlockComponentProvider
    {
        INSTANCE;

        @Override
        public ResourceLocation getUid()
        {
            return UID;
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
        {
            boolean online = accessor.getServerData().getBoolean("AeOnline");
            tooltip.add(Component.translatable(online
                    ? "jade.godofthings.ae_online"
                    : "jade.godofthings.ae_offline"));
        }
    }
}
