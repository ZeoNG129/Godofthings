package com.godofthings.aalink;

import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.godofthings.Godofthings;

/**
 * 切换无线连接器卡自动连接的包（照抄上游 ToggleFrequencyCardAutoConnectPacket）。
 * hand 存在时直接切那张卡（Shift+滚轮路径）；否则按优先级选卡（按键路径）。
 */
public record ConnectorCardToggleAutoPayload(Optional<InteractionHand> hand) implements CustomPacketPayload
{
    public static final Type<ConnectorCardToggleAutoPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "connector_card_toggle_auto"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConnectorCardToggleAutoPayload> STREAM_CODEC =
            StreamCodec.ofMember(ConnectorCardToggleAutoPayload::write, ConnectorCardToggleAutoPayload::decode);

    /** Shift+滚轮：切指定手上那张卡。 */
    public static ConnectorCardToggleAutoPayload forHand(InteractionHand hand)
    {
        return new ConnectorCardToggleAutoPayload(Optional.of(hand));
    }

    /** 按键：按优先级自动选卡。 */
    public static ConnectorCardToggleAutoPayload forPreferredCard()
    {
        return new ConnectorCardToggleAutoPayload(Optional.empty());
    }

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static ConnectorCardToggleAutoPayload decode(RegistryFriendlyByteBuf buf)
    {
        return new ConnectorCardToggleAutoPayload(
                buf.readBoolean() ? Optional.of(buf.readEnum(InteractionHand.class)) : Optional.empty());
    }

    public void write(RegistryFriendlyByteBuf buf)
    {
        buf.writeBoolean(hand.isPresent());
        hand.ifPresent(buf::writeEnum);
    }

    public static void handle(ConnectorCardToggleAutoPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
            {
                return;
            }

            ItemStack stack;
            if (payload.hand.isPresent())
            {
                stack = player.getItemInHand(payload.hand.get());
            }
            else
            {
                var selection = ConnectorCardItem.selectToggleCard(player);
                if (selection.ambiguous())
                {
                    player.displayClientMessage(
                            Component.translatable("message.godofthings.aalink.card_auto_ambiguous")
                                    .withStyle(ChatFormatting.RED),
                            true);
                    return;
                }
                stack = selection.selected().orElse(ItemStack.EMPTY);
            }
            if (stack.isEmpty() || !(stack.getItem() instanceof ConnectorCardItem))
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
