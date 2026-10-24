package com.godofthings.superfusion.network;

import com.godofthings.superfusion.SuperFusionItem;
import com.godofthings.superfusion.ModuleConfigLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 中键点击包：把手上融合卡指向的机器的升级一次装满。
 * <p>
 * 中键在原版里没有服务端交互入口，只能客户端拦截鼠标事件后自己发包。
 */
public record MiddleClickPayload(BlockPos pos, Direction face) implements CustomPacketPayload
{
    public static final Type<MiddleClickPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(com.godofthings.Godofthings.MODID, "super_fusion_middle"));

    public static final StreamCodec<FriendlyByteBuf, MiddleClickPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, MiddleClickPayload::pos,
            ByteBufCodecs.idMapper(Direction::from3DDataValue, Direction::get3DDataValue), MiddleClickPayload::face,
            MiddleClickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(MiddleClickPayload payload, IPayloadContext context)
    {
        Player player = context.player();
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SuperFusionItem))
        {
            return;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && com.godofthings.superfusion.UltimineBridge.isPressed(serverPlayer))
        {
            var positions = com.godofthings.superfusion.UltimineBridge.getCachedPositions(
                    serverPlayer, payload.pos(), payload.face());
            if (positions.size() > 1)
            {
                ModuleConfigLogic.handleMiddleClickPositions(player.level(), positions, player, stack);
                return;
            }
        }
        SuperFusionItem.handleMiddleClick(player.level(), payload.pos(), player, stack);
    }
}
