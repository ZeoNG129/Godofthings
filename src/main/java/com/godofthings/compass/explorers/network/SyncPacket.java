package com.godofthings.compass.explorers.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.godofthings.Godofthings;
import com.godofthings.compass.explorers.ExplorersCompassData;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端 → 客户端：把「可搜结构 / 结构所属维度 / 结构 → 组 / 组 → 结构 / 能否传送」一次性推下去。
 *
 * <p>移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0。
 * 上游是 {@code SimpleChannel.registerMessage(i, SyncPacket.class, ...)}；这里换成
 * NeoForge 1.21 的 {@link CustomPacketPayload} + {@link StreamCodec}，字段顺序与原
 * {@code toBytes} / 构造器逐字段一致（先结构表、再组表）。</p>
 */
public record SyncPacket(boolean canTeleport, List<ResourceLocation> allowedStructureKeys,
        ListMultimap<ResourceLocation, ResourceLocation> dimensionKeysForAllowedStructureKeys,
        Map<ResourceLocation, ResourceLocation> structureKeysToTypeKeys,
        ListMultimap<ResourceLocation, ResourceLocation> typeKeysToStructureKeys) implements CustomPacketPayload
{
    public static final Type<SyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "explorers_compass_sync"));

    public static final StreamCodec<FriendlyByteBuf, SyncPacket> CODEC =
            StreamCodec.ofMember(SyncPacket::write, SyncPacket::read);

    public static SyncPacket read(FriendlyByteBuf buf)
    {
        boolean canTeleport = buf.readBoolean();
        List<ResourceLocation> allowedStructureKeys = new ArrayList<>();
        ListMultimap<ResourceLocation, ResourceLocation> dimensionKeys = ArrayListMultimap.create();
        Map<ResourceLocation, ResourceLocation> structureKeysToTypeKeys = new HashMap<>();
        ListMultimap<ResourceLocation, ResourceLocation> typeKeysToStructureKeys = ArrayListMultimap.create();

        int numStructures = buf.readInt();
        for (int i = 0; i < numStructures; i++)
        {
            ResourceLocation structureKey = buf.readResourceLocation();
            int numDimensions = buf.readInt();
            List<ResourceLocation> dimensions = new ArrayList<>(Math.max(0, numDimensions));
            for (int j = 0; j < numDimensions; j++)
            {
                dimensions.add(buf.readResourceLocation());
            }
            ResourceLocation typeKey = buf.readResourceLocation();
            allowedStructureKeys.add(structureKey);
            dimensionKeys.putAll(structureKey, dimensions);
            structureKeysToTypeKeys.put(structureKey, typeKey);
        }

        int numTypes = buf.readInt();
        for (int i = 0; i < numTypes; i++)
        {
            ResourceLocation typeKey = buf.readResourceLocation();
            int numStructuresToAdd = buf.readInt();
            for (int j = 0; j < numStructuresToAdd; j++)
            {
                typeKeysToStructureKeys.put(typeKey, buf.readResourceLocation());
            }
        }

        return new SyncPacket(canTeleport, allowedStructureKeys, dimensionKeys, structureKeysToTypeKeys,
                typeKeysToStructureKeys);
    }

    public void write(FriendlyByteBuf buf)
    {
        buf.writeBoolean(canTeleport);
        buf.writeInt(allowedStructureKeys.size());
        for (ResourceLocation structureKey : allowedStructureKeys)
        {
            buf.writeResourceLocation(structureKey);
            List<ResourceLocation> dimensions = dimensionKeysForAllowedStructureKeys.get(structureKey);
            buf.writeInt(dimensions.size());
            for (ResourceLocation dimensionKey : dimensions)
            {
                buf.writeResourceLocation(dimensionKey);
            }
            ResourceLocation typeKey = structureKeysToTypeKeys.get(structureKey);
            buf.writeResourceLocation(typeKey == null ? ExplorersCompassData.NONE : typeKey);
        }

        buf.writeInt(typeKeysToStructureKeys.keySet().size());
        for (ResourceLocation typeKey : typeKeysToStructureKeys.keySet())
        {
            buf.writeResourceLocation(typeKey);
            List<ResourceLocation> keys = typeKeysToStructureKeys.get(typeKey);
            buf.writeInt(keys.size());
            for (ResourceLocation structureKey : keys)
            {
                buf.writeResourceLocation(structureKey);
            }
        }
    }

    public static void handle(SyncPacket packet, IPayloadContext context)
    {
        if (context.flow().isClientbound())
        {
            context.enqueueWork(() ->
            {
                ExplorersCompassData.canTeleport = packet.canTeleport();
                ExplorersCompassData.allowedStructureKeys = packet.allowedStructureKeys();
                ExplorersCompassData.dimensionKeysForAllowedStructureKeys =
                        packet.dimensionKeysForAllowedStructureKeys();
                ExplorersCompassData.structureKeysToTypeKeys = packet.structureKeysToTypeKeys();
                ExplorersCompassData.typeKeysToStructureKeys = packet.typeKeysToStructureKeys();
            });
        }
    }

    @Override
    public Type<SyncPacket> type()
    {
        return TYPE;
    }
}
