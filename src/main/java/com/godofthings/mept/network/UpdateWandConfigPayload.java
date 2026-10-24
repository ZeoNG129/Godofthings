package com.godofthings.mept.network;

import com.godofthings.Godofthings;
import com.godofthings.mept.BasePlacementToolItem;
import com.godofthings.mept.MeptDataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 -> 服务端更新法杖配置（照抄上游 UpdateWandConfigPayload）。
 */
public record UpdateWandConfigPayload(CompoundTag tag) implements CustomPacketPayload {
    private static final int TOTAL_SLOTS = 18;

    public static final Type<UpdateWandConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_update_wand_config"));

    public static final StreamCodec<FriendlyByteBuf, UpdateWandConfigPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.COMPOUND_TAG,
                    UpdateWandConfigPayload::tag,
                    UpdateWandConfigPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateWandConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // 法杖配置适用于任何放置工具子类（ME 或多方块）；双手都允许
            ItemStack stack = BasePlacementToolItem.findHeldTool(player, BasePlacementToolItem.class);
            if (stack.isEmpty()) return;

            // 绝不信任客户端原始 NBT：按白名单键重建，
            // 防止 NBT 炸弹被持久化并广播
            stack.set(MeptDataComponents.PLACEMENT_CONFIG.get(), sanitize(payload.tag, player));
        });
    }

    /**
     * 只接受白名单键、内容有界地重建放置配置。
     */
    static CompoundTag sanitize(CompoundTag raw, ServerPlayer player) {
        CompoundTag clean = new CompoundTag();
        if (raw == null) {
            return clean;
        }

        var registries = player.level().registryAccess();

        // items：经固定大小的 ItemStackHandler 往返，每槽一件
        if (raw.contains("items", CompoundTag.TAG_COMPOUND)) {
            ItemStackHandler handler = new ItemStackHandler(TOTAL_SLOTS);
            try {
                // ItemStackHandler 信任序列化里的 Size 并会重排后备列表。
                // 绝不能放行客户端控制的值：超大尺寸会 OOM，
                // 小于 TOTAL_SLOTS 的尺寸会让归一化循环越界。
                CompoundTag rawItems = raw.getCompound("items").copy();
                rawItems.putInt("Size", TOTAL_SLOTS);
                handler.deserializeNBT(registries, rawItems);
            } catch (Exception ignored) {
                handler = new ItemStackHandler(TOTAL_SLOTS);
            }
            ItemStackHandler normalized = new ItemStackHandler(TOTAL_SLOTS);
            for (int i = 0; i < TOTAL_SLOTS; i++) {
                ItemStack s = handler.getStackInSlot(i);
                if (!s.isEmpty()) {
                    normalized.setStackInSlot(i, s.copyWithCount(1));
                }
            }
            clean.put("items", normalized.serializeNBT(registries));
        }

        // fluids：槽位下标 -> 流体 id 字符串（必须是合法资源位置）
        if (raw.contains("fluids", CompoundTag.TAG_COMPOUND)) {
            CompoundTag rawFluids = raw.getCompound("fluids");
            CompoundTag cleanFluids = new CompoundTag();
            for (String key : rawFluids.getAllKeys()) {
                int slot;
                try {
                    slot = Integer.parseInt(key);
                } catch (NumberFormatException e) {
                    continue;
                }
                if (slot < 0 || slot >= TOTAL_SLOTS) continue;
                String fluidId = rawFluids.getString(key);
                if (!fluidId.isEmpty() && ResourceLocation.tryParse(fluidId) != null) {
                    cleanFluids.putString(key, fluidId);
                }
            }
            clean.put("fluids", cleanFluids);
        }

        if (raw.contains("SelectedSlot")) {
            clean.putInt("SelectedSlot", Math.max(0, Math.min(TOTAL_SLOTS - 1, raw.getInt("SelectedSlot"))));
        }
        if (raw.contains("PlacementCount")) {
            // 读的时候还会再按白名单过滤；这里只做边界约束
            clean.putInt("PlacementCount", Math.max(1, Math.min(1024, raw.getInt("PlacementCount"))));
        }
        if (raw.contains("DirectionMode")) {
            clean.putInt("DirectionMode", Math.max(0, raw.getInt("DirectionMode")));
        }

        return clean;
    }
}
