package com.direwolf20.justdirethings.common.network;

import com.direwolf20.justdirethings.JustDireThings;
import com.direwolf20.justdirethings.common.network.data.AreaAffectingPayload;
import com.direwolf20.justdirethings.common.network.data.BreakerPayload;
import com.direwolf20.justdirethings.common.network.data.ClickerPayload;
import com.direwolf20.justdirethings.common.network.data.DirectionSettingPayload;
import com.direwolf20.justdirethings.common.network.data.FilterSettingPayload;
import com.direwolf20.justdirethings.common.network.data.GhostSlotPayload;
import com.direwolf20.justdirethings.common.network.data.ItemCollectorSettingsPayload;
import com.direwolf20.justdirethings.common.network.data.RedstoneSettingPayload;
import com.direwolf20.justdirethings.common.network.data.SwapperPayload;
import com.direwolf20.justdirethings.common.network.data.TickSpeedPayload;
import com.direwolf20.justdirethings.common.network.handler.AreaAffectingPacket;
import com.direwolf20.justdirethings.common.network.handler.BreakerPacket;
import com.direwolf20.justdirethings.common.network.handler.ClickerPacket;
import com.direwolf20.justdirethings.common.network.handler.DirectionSettingPacket;
import com.direwolf20.justdirethings.common.network.handler.FilterSettingPacket;
import com.direwolf20.justdirethings.common.network.handler.GhostSlotPacket;
import com.direwolf20.justdirethings.common.network.handler.ItemCollectorSettingsPacket;
import com.direwolf20.justdirethings.common.network.handler.RedstoneSettingPacket;
import com.direwolf20.justdirethings.common.network.handler.SwapperPacket;
import com.direwolf20.justdirethings.common.network.handler.TickSpeedPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 精简网络通道：只注册 5 个机器（含 T1/T2）界面实际用到的 10 个服务端包。
 */
@SuppressWarnings("removal")
public class PacketHandler {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;
    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void registerNetworking(final FMLCommonSetupEvent event) {
        CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(JustDireThings.MODID, "main"), () -> PROTOCOL,
                PROTOCOL::equals, PROTOCOL::equals);

        CHANNEL.registerMessage(nextId(), AreaAffectingPayload.class, AreaAffectingPayload::write,
                AreaAffectingPayload::new, AreaAffectingPacket::handle);
        CHANNEL.registerMessage(nextId(), BreakerPayload.class, BreakerPayload::write, BreakerPayload::new,
                BreakerPacket::handle);
        CHANNEL.registerMessage(nextId(), ClickerPayload.class, ClickerPayload::write, ClickerPayload::new,
                ClickerPacket::handle);
        CHANNEL.registerMessage(nextId(), DirectionSettingPayload.class, DirectionSettingPayload::write,
                DirectionSettingPayload::new, DirectionSettingPacket::handle);
        CHANNEL.registerMessage(nextId(), ItemCollectorSettingsPayload.class, ItemCollectorSettingsPayload::write,
                ItemCollectorSettingsPayload::new, ItemCollectorSettingsPacket::handle);
        CHANNEL.registerMessage(nextId(), FilterSettingPayload.class, FilterSettingPayload::write,
                FilterSettingPayload::new, FilterSettingPacket::handle);
        CHANNEL.registerMessage(nextId(), GhostSlotPayload.class, GhostSlotPayload::write, GhostSlotPayload::new,
                GhostSlotPacket::handle);
        CHANNEL.registerMessage(nextId(), RedstoneSettingPayload.class, RedstoneSettingPayload::write,
                RedstoneSettingPayload::new, RedstoneSettingPacket::handle);
        CHANNEL.registerMessage(nextId(), SwapperPayload.class, SwapperPayload::write, SwapperPayload::new,
                SwapperPacket::handle);
        CHANNEL.registerMessage(nextId(), TickSpeedPayload.class, TickSpeedPayload::write, TickSpeedPayload::new,
                TickSpeedPacket::handle);
    }
}
