package com.godofthings.mept.network;

import com.godofthings.Godofthings;
import com.godofthings.mept.ItemMECablePlacementTool;
import com.godofthings.mept.MeptItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;

/**
 * 放置后服务端 -> 客户端同步线缆工具的选点（照抄上游 SyncCableToolPointsPayload）。
 */
public record SyncCableToolPointsPayload(
    int slot,
    boolean hasPoint1, int p1x, int p1y, int p1z,
    boolean hasPoint2, int p2x, int p2y, int p2z,
    boolean hasPoint3, int p3x, int p3y, int p3z
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncCableToolPointsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_sync_cable_points"));

    public static final StreamCodec<FriendlyByteBuf, SyncCableToolPointsPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SyncCableToolPointsPayload decode(FriendlyByteBuf buf) {
            int slot = buf.readInt();
            boolean hasP1 = buf.readBoolean();
            int p1x = hasP1 ? buf.readInt() : 0;
            int p1y = hasP1 ? buf.readInt() : 0;
            int p1z = hasP1 ? buf.readInt() : 0;
            boolean hasP2 = buf.readBoolean();
            int p2x = hasP2 ? buf.readInt() : 0;
            int p2y = hasP2 ? buf.readInt() : 0;
            int p2z = hasP2 ? buf.readInt() : 0;
            boolean hasP3 = buf.readBoolean();
            int p3x = hasP3 ? buf.readInt() : 0;
            int p3y = hasP3 ? buf.readInt() : 0;
            int p3z = hasP3 ? buf.readInt() : 0;
            return new SyncCableToolPointsPayload(slot, hasP1, p1x, p1y, p1z, hasP2, p2x, p2y, p2z, hasP3, p3x, p3y, p3z);
        }

        @Override
        public void encode(FriendlyByteBuf buf, SyncCableToolPointsPayload payload) {
            buf.writeInt(payload.slot);
            buf.writeBoolean(payload.hasPoint1);
            if (payload.hasPoint1) {
                buf.writeInt(payload.p1x);
                buf.writeInt(payload.p1y);
                buf.writeInt(payload.p1z);
            }
            buf.writeBoolean(payload.hasPoint2);
            if (payload.hasPoint2) {
                buf.writeInt(payload.p2x);
                buf.writeInt(payload.p2y);
                buf.writeInt(payload.p2z);
            }
            buf.writeBoolean(payload.hasPoint3);
            if (payload.hasPoint3) {
                buf.writeInt(payload.p3x);
                buf.writeInt(payload.p3y);
                buf.writeInt(payload.p3z);
            }
        }
    };

    /**
     * 从工具当前状态构造载荷。
     */
    public static SyncCableToolPointsPayload create(int slot, @Nullable BlockPos p1, @Nullable BlockPos p2, @Nullable BlockPos p3) {
        return new SyncCableToolPointsPayload(
            slot,
            p1 != null, p1 != null ? p1.getX() : 0, p1 != null ? p1.getY() : 0, p1 != null ? p1.getZ() : 0,
            p2 != null, p2 != null ? p2.getX() : 0, p2 != null ? p2.getY() : 0, p2 != null ? p2.getZ() : 0,
            p3 != null, p3 != null ? p3.getX() : 0, p3 != null ? p3.getY() : 0, p3 != null ? p3.getZ() : 0
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncCableToolPointsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return;

            ItemStack stack = player.getInventory().getItem(payload.slot);
            if (stack.getItem() != MeptItems.ME_CABLE_PLACEMENT_TOOL.get()) {
                return;
            }

            // 在客户端更新选点
            if (payload.hasPoint1) {
                ItemMECablePlacementTool.setPoint1(stack, new BlockPos(payload.p1x, payload.p1y, payload.p1z));
            } else {
                ItemMECablePlacementTool.setPoint1(stack, null);
            }

            if (payload.hasPoint2) {
                ItemMECablePlacementTool.setPoint2(stack, new BlockPos(payload.p2x, payload.p2y, payload.p2z));
            } else {
                ItemMECablePlacementTool.setPoint2(stack, null);
            }

            if (payload.hasPoint3) {
                ItemMECablePlacementTool.setPoint3(stack, new BlockPos(payload.p3x, payload.p3y, payload.p3z));
            } else {
                ItemMECablePlacementTool.setPoint3(stack, null);
            }
        });
    }
}
