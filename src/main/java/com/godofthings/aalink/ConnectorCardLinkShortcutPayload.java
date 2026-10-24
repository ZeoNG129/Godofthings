package com.godofthings.aalink;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.godofthings.Godofthings;

/**
 * 快捷键「连接注视目标」包（照抄上游 FrequencyCardLinkShortcutPacket）：
 * 用玩家身上最合适的一张卡连接正在注视的方块。
 */
public record ConnectorCardLinkShortcutPayload(BlockPos pos, Direction face, Vec3 hitVec)
        implements CustomPacketPayload
{
    public static final Type<ConnectorCardLinkShortcutPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "connector_card_link_shortcut"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConnectorCardLinkShortcutPayload> STREAM_CODEC =
            StreamCodec.ofMember(ConnectorCardLinkShortcutPayload::write, ConnectorCardLinkShortcutPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static ConnectorCardLinkShortcutPayload decode(RegistryFriendlyByteBuf buf)
    {
        return new ConnectorCardLinkShortcutPayload(
                buf.readBlockPos(), buf.readEnum(Direction.class),
                new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    public void write(RegistryFriendlyByteBuf buf)
    {
        buf.writeBlockPos(pos);
        buf.writeEnum(face);
        buf.writeDouble(hitVec.x);
        buf.writeDouble(hitVec.y);
        buf.writeDouble(hitVec.z);
    }

    public static void handle(ConnectorCardLinkShortcutPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player)
            {
                payload.handleOnServer(player);
            }
        });
    }

    private void handleOnServer(ServerPlayer player)
    {
        if (!(player.level() instanceof ServerLevel level)
                || !level.isLoaded(pos)
                || !player.canInteractWithBlock(pos, 1.0D)
                || !hitInsideBlock())
        {
            return;
        }

        var selection = ConnectorCardItem.selectLinkCard(player);
        if (selection.ambiguous())
        {
            message(player, "message.godofthings.aalink.card_link_ambiguous");
            return;
        }
        ItemStack card = selection.selected().orElse(ItemStack.EMPTY);
        if (card.isEmpty())
        {
            message(player, "message.godofthings.aalink.card_no_candidate");
            return;
        }

        ConnectorCardUsePayload.tryLinkWithCard(player, card, level, pos);
    }

    private boolean hitInsideBlock()
    {
        final double tolerance = 0.01D;
        return hitVec.x >= pos.getX() - tolerance && hitVec.x <= pos.getX() + 1 + tolerance
                && hitVec.y >= pos.getY() - tolerance && hitVec.y <= pos.getY() + 1 + tolerance
                && hitVec.z >= pos.getZ() - tolerance && hitVec.z <= pos.getZ() + 1 + tolerance;
    }

    private static void message(ServerPlayer player, String key)
    {
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
    }
}
