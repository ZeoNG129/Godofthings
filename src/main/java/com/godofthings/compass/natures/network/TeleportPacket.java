package com.godofthings.compass.natures.network;

import com.godofthings.Godofthings;
import com.godofthings.compass.natures.NaturesCompassData;
import com.godofthings.compass.natures.config.ConfigHandler;
import com.godofthings.compass.natures.items.NaturesCompassItem;
import com.godofthings.compass.natures.util.CompassState;
import com.godofthings.compass.natures.util.ItemUtils;
import com.godofthings.compass.natures.util.PlayerUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端 → 服务端：传送到已找到的群系（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>落点从海平面开始上下交替找第一个「脚下有支撑、上面两格是空的」位置（火 / 液体 / 可替换方块都算空），
 * 一路找不到就退到 y=256。</p>
 */
public record TeleportPacket() implements CustomPacketPayload
{
    public static final Type<TeleportPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "natures_compass_teleport"));

    public static final StreamCodec<FriendlyByteBuf, TeleportPacket> CODEC =
            StreamCodec.ofMember(TeleportPacket::write, TeleportPacket::read);

    public static TeleportPacket read(FriendlyByteBuf buf)
    {
        return new TeleportPacket();
    }

    public void write(FriendlyByteBuf buf)
    {
    }

    public static void handle(TeleportPacket packet, IPayloadContext context)
    {
        if (context.flow().isServerbound())
        {
            context.enqueueWork(() ->
            {
                ItemStack stack = ItemUtils.getHeldNatureCompass(context.player());
                if (stack.isEmpty())
                {
                    return;
                }
                NaturesCompassItem natureCompass = (NaturesCompassItem) stack.getItem();
                ServerPlayer player = (ServerPlayer) context.player();
                if (ConfigHandler.GENERAL.allowTeleport.get() && PlayerUtils.canTeleport(player.getServer(), player))
                {
                    if (natureCompass.getState(stack) == CompassState.FOUND)
                    {
                        int x = natureCompass.getFoundBiomeX(stack);
                        int z = natureCompass.getFoundBiomeZ(stack);
                        int y = packet.findValidTeleportHeight(context.player().level(), x, z);

                        player.stopRiding();
                        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());

                        if (!player.isFallFlying())
                        {
                            player.setDeltaMovement(player.getDeltaMovement().x(), 0, player.getDeltaMovement().z());
                            player.setOnGround(true);
                        }
                    }
                }
                else
                {
                    NaturesCompassData.LOGGER.warn("Player " + player.getDisplayName().getString()
                            + " tried to teleport but does not have permission.");
                }
            });
        }
    }

    private int findValidTeleportHeight(Level level, int x, int z)
    {
        int upY = level.getSeaLevel();
        int downY = level.getSeaLevel();
        while ((!level.isOutsideBuildHeight(upY) || !level.isOutsideBuildHeight(downY))
                && !(isValidTeleportPosition(level, new BlockPos(x, upY, z))
                        || isValidTeleportPosition(level, new BlockPos(x, downY, z))))
        {
            upY++;
            downY--;
        }
        BlockPos upPos = new BlockPos(x, upY, z);
        BlockPos downPos = new BlockPos(x, downY, z);
        if (isValidTeleportPosition(level, upPos))
        {
            return upY;
        }
        if (isValidTeleportPosition(level, downPos))
        {
            return downY;
        }
        return 256;
    }

    private boolean isValidTeleportPosition(Level level, BlockPos pos)
    {
        return isFree(level, pos) && isFree(level, pos.above()) && !isFree(level, pos.below());
    }

    private boolean isFree(Level level, BlockPos pos)
    {
        return level.getBlockState(pos).isAir()
                || level.getBlockState(pos).is(BlockTags.FIRE)
                || level.getBlockState(pos).liquid()
                || level.getBlockState(pos).canBeReplaced();
    }

    @Override
    public Type<TeleportPacket> type()
    {
        return TYPE;
    }
}
