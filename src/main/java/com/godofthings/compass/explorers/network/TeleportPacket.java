package com.godofthings.compass.explorers.network;

import com.godofthings.Godofthings;
import com.godofthings.compass.explorers.config.ConfigHandler;
import com.godofthings.compass.explorers.items.ExplorersCompassItem;
import com.godofthings.compass.explorers.util.CompassState;
import com.godofthings.compass.explorers.util.ItemUtils;
import com.godofthings.compass.explorers.util.PlayerUtils;

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
 * 客户端 → 服务端：传送到已找到的结构（无载荷）。
 *
 * <p>移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0。
 * 上游 {@code TeleportPacket} 的 {@code toBytes} 是空的；这里用 {@link StreamCodec#unit}。</p>
 */
public record TeleportPacket() implements CustomPacketPayload
{
    public static final Type<TeleportPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "explorers_compass_teleport"));

    public static final StreamCodec<FriendlyByteBuf, TeleportPacket> CODEC =
            StreamCodec.unit(new TeleportPacket());

    public static void handle(TeleportPacket packet, IPayloadContext context)
    {
        if (context.flow().isServerbound())
        {
            context.enqueueWork(() ->
            {
                if (!(context.player() instanceof ServerPlayer player))
                {
                    return;
                }
                ItemStack stack = ItemUtils.getHeldExplorersCompass(player);
                if (stack.isEmpty())
                {
                    return;
                }
                ExplorersCompassItem compass = (ExplorersCompassItem) stack.getItem();
                if (ConfigHandler.GENERAL.allowTeleport.get() && PlayerUtils.canTeleport(player.getServer(), player))
                {
                    if (compass.getState(stack) == CompassState.FOUND)
                    {
                        int x = compass.getFoundStructureX(stack);
                        int z = compass.getFoundStructureZ(stack);
                        int y = findValidTeleportHeight(player.level(), x, z);

                        player.stopRiding();
                        player.teleportTo(x, y, z);
                        if (!player.isFallFlying())
                        {
                            player.setDeltaMovement(player.getDeltaMovement().x(), 0,
                                    player.getDeltaMovement().z());
                            player.setOnGround(true);
                        }
                    }
                }
            });
        }
    }

    /** 从海平面上下对找第一个能站人的高度（上游算法，一行未改） */
    private static int findValidTeleportHeight(Level level, int x, int z)
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

    private static boolean isValidTeleportPosition(Level level, BlockPos pos)
    {
        return isFree(level, pos) && isFree(level, pos.above()) && !isFree(level, pos.below());
    }

    private static boolean isFree(Level level, BlockPos pos)
    {
        return level.getBlockState(pos).isAir() || level.getBlockState(pos).is(BlockTags.FIRE)
                || level.getBlockState(pos).liquid() || level.getBlockState(pos).canBeReplaced();
    }

    @Override
    public Type<TeleportPacket> type()
    {
        return TYPE;
    }
}
