package com.godofthings.aalink;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.godofthings.Godofthings;

/**
 * 切换无线连接器卡自动连接的包（照抄上游 ToggleFrequencyCardAutoConnectPacket，去掉终端内卡路径）。
 */
public record ConnectorCardToggleAutoPayload() implements CustomPacketPayload
{
    public static final Type<ConnectorCardToggleAutoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "connector_card_toggle_auto"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConnectorCardToggleAutoPayload> STREAM_CODEC =
            StreamCodec.unit(new ConnectorCardToggleAutoPayload());

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(ConnectorCardToggleAutoPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
            {
                return;
            }

            var selection = ConnectorCardItem.selectToggleCard(player);
            if (selection.ambiguous())
            {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.aalink.card_auto_ambiguous")
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }
            ItemStack stack = selection.selected().orElse(ItemStack.EMPTY);
            if (stack.isEmpty())
            {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.aalink.card_no_candidate")
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }

            var data = ConnectorCardItem.getData(stack);
            if (data.isBound() && !data.canBeUsedBy(player.getUUID()))
            {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.aalink.card_owner_mismatch")
                                .withStyle(ChatFormatting.RED),
                        true);
                return;
            }

            boolean enabled = ConnectorCardItem.toggleAutoConnect(stack);
            player.displayClientMessage(
                    Component.translatable(enabled
                                    ? "message.godofthings.aalink.card_auto_enabled"
                                    : "message.godofthings.aalink.card_auto_disabled")
                            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW),
                    true);
        });
    }
}
