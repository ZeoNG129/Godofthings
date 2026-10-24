package com.godofthings.mept.network;

import com.godofthings.Godofthings;
import com.godofthings.mept.BasePlacementToolItem;
import com.godofthings.mept.ItemMultiblockPlacementTool;
import com.godofthings.mept.MeptDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 -> 服务端更新多方块放置工具的方向模式（照抄上游 UpdateDirectionModePayload）。
 */
public record UpdateDirectionModePayload(int modeId) implements CustomPacketPayload {
    public static final Type<UpdateDirectionModePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_update_direction_mode"));

    public static final StreamCodec<FriendlyByteBuf, UpdateDirectionModePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    UpdateDirectionModePayload::modeId,
                    UpdateDirectionModePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateDirectionModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // 双手找多方块工具（先主手后副手）
            ItemStack stack = BasePlacementToolItem.findHeldTool(player, ItemMultiblockPlacementTool.class);
            if (stack.isEmpty()) return;

            CompoundTag cfg = stack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
            cfg = (cfg == null) ? new CompoundTag() : cfg.copy();
            cfg.putInt("DirectionMode", ItemMultiblockPlacementTool.DirectionMode.fromId(payload.modeId()).ordinal());
            stack.set(MeptDataComponents.PLACEMENT_CONFIG.get(), cfg);
        });
    }
}
