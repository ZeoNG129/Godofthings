package com.godofthings.torcherino.network;

import com.mojang.datafixers.util.Pair;
import com.godofthings.torcherino.Torcherino;
import com.godofthings.torcherino.api.Tier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步加速等级表。
 */
public final class S2CTierSyncMessage
{
    private final Map<ResourceLocation, Tier> tiers;

    public S2CTierSyncMessage(Map<ResourceLocation, Tier> tiers)
    {
        this.tiers = tiers;
    }

    public static void encode(S2CTierSyncMessage message, FriendlyByteBuf buffer)
    {
        buffer.writeInt(message.tiers.size());
        message.tiers.forEach((name, tier) -> writeTier(name, tier, buffer));
    }

    public static S2CTierSyncMessage decode(FriendlyByteBuf buffer)
    {
        Map<ResourceLocation, Tier> localTiers = new HashMap<>();
        int count = buffer.readInt();
        for (int i = 0; i < count; i++)
        {
            Pair<ResourceLocation, Tier> entry = readTier(buffer);
            localTiers.put(entry.getFirst(), entry.getSecond());
        }
        return new S2CTierSyncMessage(localTiers);
    }

    public static void handle(S2CTierSyncMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getOriginationSide() == LogicalSide.SERVER)
        {
            context.enqueueWork(() -> Torcherino.setRemoteTiers(message.tiers));
            context.setPacketHandled(true);
        }
    }

    private static Pair<ResourceLocation, Tier> readTier(FriendlyByteBuf buffer)
    {
        return new Pair<>(buffer.readResourceLocation(), new Tier(buffer.readInt(), buffer.readInt(), buffer.readInt()));
    }

    private static void writeTier(ResourceLocation name, Tier tier, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(name).writeInt(tier.maxSpeed()).writeInt(tier.xzRange()).writeInt(tier.yRange());
    }
}
