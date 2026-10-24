package com.godofthings.mept.network;

import com.godofthings.Godofthings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.godofthings.mept.BasePlacementToolItem;
import com.godofthings.mept.MeptDataComponents;
import com.godofthings.mept.WandMenu;

public record UpdateWandSlotPayload(int slotIndex, ItemStack stack) implements CustomPacketPayload {
    private static final int TOTAL_SLOTS = 18;

    public static final CustomPacketPayload.Type<UpdateWandSlotPayload> TYPE =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_update_wand_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateWandSlotPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        UpdateWandSlotPayload::slotIndex,
        ItemStack.OPTIONAL_STREAM_CODEC,
        UpdateWandSlotPayload::stack,
        UpdateWandSlotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateWandSlotPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // 幽灵槽永远只代表单个物品；把客户端发来的堆归一化成数量 1
            ItemStack sanitized = payload.stack().isEmpty() ? ItemStack.EMPTY : payload.stack().copyWithCount(1);

            // WandMenu 开着就走它的 handler
            if (player.containerMenu instanceof WandMenu wandMenu) {
                wandMenu.handleUpdateSlot(player, payload.slotIndex(), sanitized);
            } else {
                // 兜底：直接写物品数据组件。
                // 只允许写到真正的放置工具上，绝不能写任意物品。
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.getItem() instanceof BasePlacementToolItem
                        && payload.slotIndex() >= 0 && payload.slotIndex() < TOTAL_SLOTS) {
                    updateItemDirectly(mainHand, payload.slotIndex(), sanitized, player);
                }
            }
        });
    }

    private static void updateItemDirectly(ItemStack wandStack, int slotIndex, ItemStack newStack, ServerPlayer player) {
        CompoundTag existingConfig = wandStack.getOrDefault(MeptDataComponents.PLACEMENT_CONFIG.get(), new CompoundTag());

        ItemStackHandler handler = new ItemStackHandler(TOTAL_SLOTS);
        if (existingConfig.contains("items")) {
            handler.deserializeNBT(player.level().registryAccess(), existingConfig.getCompound("items"));
        }

        handler.setStackInSlot(slotIndex, newStack);

        CompoundTag newConfig = new CompoundTag();
        newConfig.put("items", handler.serializeNBT(player.level().registryAccess()));

        if (existingConfig.contains("fluids")) {
            newConfig.put("fluids", existingConfig.getCompound("fluids"));
        }
        if (existingConfig.contains("SelectedSlot")) {
            newConfig.putInt("SelectedSlot", existingConfig.getInt("SelectedSlot"));
        }
        if (existingConfig.contains("PlacementCount")) {
            newConfig.putInt("PlacementCount", existingConfig.getInt("PlacementCount"));
        }
        if (existingConfig.contains("DirectionMode")) {
            newConfig.putInt("DirectionMode", existingConfig.getInt("DirectionMode"));
        }

        wandStack.set(MeptDataComponents.PLACEMENT_CONFIG.get(), newConfig);
    }
}
