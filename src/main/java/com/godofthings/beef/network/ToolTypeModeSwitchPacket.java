package com.godofthings.beef.network;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.api.enums.tool.ToolTypeMode;
import com.godofthings.beef.content.items.BeefToolVariants;
import com.godofthings.beef.core.component.UComponents;
import com.godofthings.beef.utils.UselessItemUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ToolTypeModeSwitchPacket(ToolTypeMode mode) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, ToolTypeModeSwitchPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> buf.writeEnum(packet.mode),
                    buf -> new ToolTypeModeSwitchPacket(buf.readEnum(ToolTypeMode.class))
            );

    public static final Type<ToolTypeModeSwitchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UselessMod.MODID, "tool_type_mode_switch"));

    public static void handle(ToolTypeModeSwitchPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            var toolEntry = UselessItemUtils.findTargetToolInHands(player);
            if (toolEntry.isEmpty()) return; // 没找到工具直接返回

            var entry = toolEntry.get();
            ItemStack targetItem = entry.getKey();
            InteractionHand targetHand = entry.getValue();

            // 1. 创建新物品实例
            ItemStack newStack = switch (msg.mode) {
                case NONE_MODE, WRENCH_MODE, SCREWDRIVER_MODE, MALLET_MODE, CROWBAR_MODE, HAMMER_MODE ->
                        BeefToolVariants.createForToolMode(targetItem, msg.mode);
                case OMNITOOL_MODE -> {
                    ResourceLocation omnitoolId = ResourceLocation.fromNamespaceAndPath("omnitools", "omni_wrench");
                    Item toolItem = BuiltInRegistries.ITEM.get(omnitoolId);
                    if (toolItem != Items.AIR) {
                        yield new ItemStack(toolItem);
                    } else {
                        // 本项目只保留荒辰移晷之杖一个物品：未装「全能工具」时保持原物品不变
                        // （上游此处会换成 endless_beaf_wrench，该物品已随造化杖一并移除）
                        yield BeefToolVariants.createForToolMode(targetItem, ToolTypeMode.NONE_MODE);
                    }
                }
            };

            // 2. 复制原有物品的所有NBT数据到新实例
            newStack.applyComponents(targetItem.getComponents());

            // 3. 覆盖状态：最后设置当前的工具模式组件
            if (!newStack.isEmpty()) {
                newStack.set(UComponents.CurrentToolTypeComponent.get(), msg.mode);
                player.setItemInHand(targetHand, newStack);

                // 显式同步物品到客户端
                player.containerMenu.broadcastChanges();
            }
        });
    }
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
