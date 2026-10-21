package com.godofthings.compass.natures.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.godofthings.Godofthings;
import com.godofthings.compass.natures.NaturesCompassData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 → 客户端：把「能不能传送 / 最多搜几次 / 允许搜的群系 / 每个群系扣几级经验 / 群系在哪些维度生成」
 * 一次性推给客户端（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>客户端收到后写进 {@link NaturesCompassData} 的静态字段，界面与 HUD 都从那里读。</p>
 */
public record SyncPacket(boolean canTeleport, int maxNextSearches, boolean infiniteXp, List<ResourceLocation> allowedBiomes,
        Map<ResourceLocation, Integer> xpLevelsForAllowedBiomes,
        ListMultimap<ResourceLocation, ResourceLocation> dimensionKeysForAllowedBiomeKeys) implements CustomPacketPayload
{
    public static final Type<SyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "natures_compass_sync"));

    public static final StreamCodec<FriendlyByteBuf, SyncPacket> CODEC =
            StreamCodec.ofMember(SyncPacket::write, SyncPacket::read);

    public static SyncPacket read(FriendlyByteBuf buf)
    {
        boolean canTeleport = buf.readBoolean();
        int maxNextSearches = buf.readInt();
        boolean infiniteXp = buf.readBoolean();

        List<ResourceLocation> allowedBiomes = new ArrayList<>();
        Map<ResourceLocation, Integer> xpLevelsForAllowedBiomes = new HashMap<>();
        ListMultimap<ResourceLocation, ResourceLocation> dimensionKeysForAllowedBiomeKeys = ArrayListMultimap.create();

        int listSize = buf.readInt();
        for (int i = 0; i < listSize; i++)
        {
            ResourceLocation biomeKey = buf.readResourceLocation();
            int numDimensions = buf.readInt();
            List<ResourceLocation> dimensionKeys = new ArrayList<>();
            for (int j = 0; j < numDimensions; j++)
            {
                dimensionKeys.add(buf.readResourceLocation());
            }

            int xpLevels = buf.readInt();

            if (biomeKey != null)
            {
                allowedBiomes.add(biomeKey);
                xpLevelsForAllowedBiomes.put(biomeKey, xpLevels);
                dimensionKeysForAllowedBiomeKeys.putAll(biomeKey, dimensionKeys);
            }
        }

        return new SyncPacket(canTeleport, maxNextSearches, infiniteXp, allowedBiomes, xpLevelsForAllowedBiomes,
                dimensionKeysForAllowedBiomeKeys);
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeBoolean(canTeleport);
        buf.writeInt(maxNextSearches);
        buf.writeBoolean(infiniteXp);

        buf.writeInt(allowedBiomes.size());
        for (ResourceLocation biomeKey : allowedBiomes)
        {
            buf.writeResourceLocation(biomeKey);
            List<ResourceLocation> dimensionKeys = dimensionKeysForAllowedBiomeKeys.get(biomeKey);
            buf.writeInt(dimensionKeys.size());
            for (ResourceLocation dimensionKey : dimensionKeys)
            {
                buf.writeResourceLocation(dimensionKey);
            }
            int xpLevels = xpLevelsForAllowedBiomes.getOrDefault(biomeKey, 0);
            buf.writeInt(xpLevels);
        }
    }

    public static void handle(SyncPacket packet, IPayloadContext context)
    {
        if (context.flow().isClientbound())
        {
            context.enqueueWork(() ->
            {
                NaturesCompassData.synced = true;
                NaturesCompassData.canTeleport = packet.canTeleport;
                NaturesCompassData.maxNextSearches = packet.maxNextSearches;
                NaturesCompassData.infiniteXp = packet.infiniteXp;
                NaturesCompassData.allowedBiomes = packet.allowedBiomes;
                NaturesCompassData.xpLevelsForAllowedBiomes = packet.xpLevelsForAllowedBiomes;
                NaturesCompassData.dimensionKeysForAllowedBiomeKeys = packet.dimensionKeysForAllowedBiomeKeys;
            });
        }
    }

    @Override
    public Type<SyncPacket> type()
    {
        return TYPE;
    }
}
