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
 * 客户端 -> 服务端更新放置数量（照抄上游 UpdatePlacementCountPayload）。
 */
public record UpdatePlacementCountPayload(int count) implements CustomPacketPayload {
    public static final Type<UpdatePlacementCountPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_update_placement_count"));

    public static final StreamCodec<FriendlyByteBuf, UpdatePlacementCountPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    UpdatePlacementCountPayload::count,
                    UpdatePlacementCountPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdatePlacementCountPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // PlacementCount 只对多方块工具有意义；双手都查
            ItemStack stack = BasePlacementToolItem.findHeldTool(player, ItemMultiblockPlacementTool.class);
            if (stack.isEmpty()) return;

            // 更新配置里的放置数量
            CompoundTag cfg = stack.get(MeptDataComponents.PLACEMENT_CONFIG.get());
            if (cfg == null) cfg = new CompoundTag();
            else cfg = cfg.copy(); // 复制一份，避免直接改不可变组件数据

            // 用 "PlacementCount" 与 ItemMultiblockPlacementTool.getPlacementCount() 对齐
            cfg.putInt("PlacementCount", payload.count());
            stack.set(MeptDataComponents.PLACEMENT_CONFIG.get(), cfg);
        });
    }
}
