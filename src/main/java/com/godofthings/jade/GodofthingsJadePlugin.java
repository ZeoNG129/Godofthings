package com.godofthings.jade;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import com.godofthings.Godofthings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 兼容：神之机器接入 AE 网络时，在 Jade 提示里显示「设备在线 / 设备离线」
 * （与 AE 设备一致的在线状态显示）。
 */
@WailaPlugin(Godofthings.MODID)
public class GodofthingsJadePlugin implements IWailaPlugin
{
    @Override
    public void registerClient(IWailaClientRegistration registration)
    {
        // 注册到所有方块，仅对 implements IInWorldGridNodeHost 的机器生效
        registration.registerBlockComponent(AeStatusProvider.INSTANCE, Block.class);
    }

    public enum AeStatusProvider implements IBlockComponentProvider
    {
        INSTANCE;

        @Override
        public ResourceLocation getUid()
        {
            return ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "ae_status");
        }

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config)
        {
            BlockEntity be = accessor.getBlockEntity();
            if (be instanceof IInWorldGridNodeHost host)
            {
                IGridNode node = host.getGridNode(null);
                boolean online = node != null && node.isActive();
                tooltip.add(Component.translatable(online
                        ? "jade.godofthings.ae_online"
                        : "jade.godofthings.ae_offline"));
            }
        }
    }
}
