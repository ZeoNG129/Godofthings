package com.godofthings.aalink;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import com.godofthings.Godofthings;

/**
 * 无线连接器卡的右键交互包（照抄上游 FrequencyCardUsePacket）：
 * 服务端统一处理「潜行绑定无线连接器 / 非潜行接入目标设备」。
 */
public record ConnectorCardUsePayload(
        InteractionHand hand,
        BlockPos pos,
        Direction face,
        boolean shiftDown
) implements CustomPacketPayload
{
    public static final Type<ConnectorCardUsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "connector_card_use"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConnectorCardUsePayload> STREAM_CODEC =
            StreamCodec.ofMember(ConnectorCardUsePayload::write, ConnectorCardUsePayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static ConnectorCardUsePayload decode(RegistryFriendlyByteBuf buf)
    {
        return new ConnectorCardUsePayload(
                buf.readEnum(InteractionHand.class),
                buf.readBlockPos(),
                buf.readEnum(Direction.class),
                buf.readBoolean());
    }

    public void write(RegistryFriendlyByteBuf buf)
    {
        buf.writeEnum(hand);
        buf.writeBlockPos(pos);
        buf.writeEnum(face);
        buf.writeBoolean(shiftDown);
    }

    public static void handle(ConnectorCardUsePayload payload, IPayloadContext context)
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
        if (!(player.level() instanceof ServerLevel level)) return;
        if (!level.isLoaded(pos)) return;
        if (!player.canInteractWithBlock(pos, 1.0D)) return;

        var stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof ConnectorCardItem)) return;

        if (shiftDown || player.isShiftKeyDown())
        {
            var data = ConnectorCardItem.getData(stack);
            if (data.isBound() && !data.canBeUsedBy(player.getUUID()))
            {
                message(player, "message.godofthings.aalink.card_owner_mismatch", ChatFormatting.RED);
                return;
            }
            var be = level.getBlockEntity(pos);
            if (be instanceof AaLinkConnectorBlockEntity)
            {
                bindConnector(player, stack, level, pos);
            }
            else
            {
                message(player, "message.godofthings.aalink.card_not_connector", ChatFormatting.RED);
            }
            return;
        }

        tryLinkWithCard(player, stack, level, pos);
    }

    /**
     * 非潜行「连接/断开目标」动作：把目标设备接入卡上绑定的连接器网络。
     * 手持卡右键与快捷键共用这一条链。
     */
    public static void tryLinkWithCard(ServerPlayer player, ItemStack card, ServerLevel level, BlockPos pos)
    {
        var data = ConnectorCardItem.getData(card);
        if (!data.isBound())
        {
            message(player, "message.godofthings.aalink.card_unbound", ChatFormatting.RED);
            return;
        }
        if (!data.canBeUsedBy(player.getUUID()))
        {
            message(player, "message.godofthings.aalink.card_owner_mismatch", ChatFormatting.RED);
            return;
        }

        ResourceLocation dimId = ResourceLocation.tryParse(data.boundConnectorDimension().orElse(""));
        BlockPos hubPos = BlockPos.of(data.boundConnectorPos().orElse(0L));
        ServerLevel hubLevel = dimId == null
                ? null
                : level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimId));
        if (hubLevel == null)
        {
            message(player, "message.godofthings.aalink.card_hub_dim_missing", ChatFormatting.RED);
            return;
        }
        if (!(hubLevel.getBlockEntity(hubPos) instanceof AaLinkConnectorBlockEntity hub))
        {
            message(player, "message.godofthings.aalink.card_hub_missing", ChatFormatting.RED);
            return;
        }
        var hubNode = hub.getHubNode();
        if (hubNode == null || hubNode.getGrid() == null)
        {
            message(player, "message.godofthings.aalink.card_hub_not_ready", ChatFormatting.RED);
            return;
        }

        // 绑定的连接器自己不算目标
        if (level == hubLevel && pos.equals(hubPos))
        {
            message(player, "message.godofthings.aalink.card_target_is_hub", ChatFormatting.YELLOW);
            return;
        }

        var deviceNode = AaLinkBridge.resolveDeviceNode(level, pos);
        if (deviceNode == null)
        {
            message(player, "message.godofthings.aalink.card_not_ae_device", ChatFormatting.RED);
            return;
        }
        if (deviceNode.getGrid() == hubNode.getGrid())
        {
            // 已同网 → 断开（与上游「再点一次断开」语义一致）
            try
            {
                AaLinkBridge.disconnect(deviceNode);
                AaLinkSavedData.get(level).unlink(pos);
                message(player, "message.godofthings.aalink.card_unlinked", ChatFormatting.YELLOW);
            }
            catch (Exception e)
            {
                message(player, "message.godofthings.aalink.card_failed", ChatFormatting.RED);
            }
            return;
        }
        try
        {
            appeng.api.networking.GridHelper.createConnection(deviceNode, hubNode);
            AaLinkSavedData.get(level).link(pos); // 持久化：重进存档后保持连接
            message(player, "message.godofthings.aalink.card_linked", ChatFormatting.GREEN);
        }
        catch (Exception e)
        {
            message(player, "message.godofthings.aalink.card_failed", ChatFormatting.RED);
        }
    }

    private static void bindConnector(ServerPlayer player, ItemStack stack, ServerLevel level, BlockPos connectorPos)
    {
        var be = level.getBlockEntity(connectorPos);
        if (!(be instanceof AaLinkConnectorBlockEntity hub)) return;
        if (hub.getHubNode() == null || hub.getHubNode().getGrid() == null)
        {
            message(player, "message.godofthings.aalink.card_hub_not_ready", ChatFormatting.RED);
            return;
        }

        ConnectorCardItem.bindConnector(stack, level, connectorPos, player.getUUID());
        player.displayClientMessage(
                Component.translatable(
                                "message.godofthings.aalink.card_bound",
                                level.dimension().location().toString(),
                                connectorPos.toShortString())
                        .withStyle(ChatFormatting.GREEN),
                true);
    }

    private static void message(ServerPlayer player, String key, ChatFormatting style)
    {
        player.displayClientMessage(Component.translatable(key).withStyle(style), true);
    }
}
