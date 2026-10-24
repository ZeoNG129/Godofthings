package com.godofthings.mept.network;

import com.godofthings.Godofthings;
import com.godofthings.mept.CableToolMenu;
import com.godofthings.mept.ItemMECablePlacementTool;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.chat.Component;

/**
 * 客户端请求打开线缆工具配置界面（照抄上游 OpenCableToolGuiPayload）。
 */
public record OpenCableToolGuiPayload() implements CustomPacketPayload {
    public static final Type<OpenCableToolGuiPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_open_cable_tool_gui"));

    public static final StreamCodec<FriendlyByteBuf, OpenCableToolGuiPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenCableToolGuiPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenCableToolGuiPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ItemStack main = player.getMainHandItem();
                ItemStack off = player.getOffhandItem();

                ItemStack tool = null;
                int slot = -1;

                if (main.getItem() instanceof ItemMECablePlacementTool) {
                    tool = main;
                    slot = player.getInventory().selected;
                } else if (off.getItem() instanceof ItemMECablePlacementTool) {
                    tool = off;
                    slot = 40; // 副手槽
                }

                if (tool != null) {
                    final ItemStack finalTool = tool;
                    final int finalSlot = slot;
                    player.openMenu(new SimpleMenuProvider(
                            (id, inv, p) -> new CableToolMenu(id, inv, finalTool, finalSlot),
                            Component.translatable("gui.godofthings.mept.cable_tool")
                    ), buf -> buf.writeInt(finalSlot));
                }
            }
        });
    }
}
