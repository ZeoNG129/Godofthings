package com.godofthings.mept.network;

import com.godofthings.Godofthings;
import com.godofthings.mept.ItemMECablePlacementTool;
import com.godofthings.mept.MeptItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端请求清空线缆工具的选点（左键空击事件只在客户端触发；照抄上游 ClearCableToolPointsPayload）。
 */
public record ClearCableToolPointsPayload(int slot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ClearCableToolPointsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_clear_cable_points"));

    public static final StreamCodec<FriendlyByteBuf, ClearCableToolPointsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, ClearCableToolPointsPayload::slot,
            ClearCableToolPointsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearCableToolPointsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // 校验客户端提供的槽位下标（负数会让 getItem 抛异常）
            if (payload.slot < 0 || payload.slot >= player.getInventory().getContainerSize()) {
                return;
            }

            ItemStack stack = player.getInventory().getItem(payload.slot);
            if (stack.getItem() != MeptItems.ME_CABLE_PLACEMENT_TOOL.get()) {
                return;
            }

            // 检查是否有已设置的选点
            var p1 = ItemMECablePlacementTool.getPoint1(stack);
            var p2 = ItemMECablePlacementTool.getPoint2(stack);
            var p3 = ItemMECablePlacementTool.getPoint3(stack);

            if (p1 != null || p2 != null || p3 != null) {
                // 清空全部选点
                ItemMECablePlacementTool.clearAllPoints(stack);

                player.displayClientMessage(Component.translatable("message.godofthings.mept.points_cleared"), true);
            }
        });
    }
}
