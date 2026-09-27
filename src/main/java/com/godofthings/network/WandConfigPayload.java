package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.item.WandAcceleration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 神之工具配置界面 → 服务端：把倍率与「持续模式」写进手持工具的物品数据。
 *
 * <p>为什么要发包：{@code ItemStack} 的修改是服务端权威的，客户端改动不会同步；
 * 而加速标记是在服务端建立的，所以必须由客户端把配置发上来落盘。
 */
public record WandConfigPayload(int speed, boolean permanent) implements CustomPacketPayload
{
    public static final Type<WandConfigPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "wand_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WandConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, WandConfigPayload::speed,
            ByteBufCodecs.BOOL, WandConfigPayload::permanent,
            WandConfigPayload::new);

    /** 物品数据键（与 WandFeatureHandler 共用） */
    public static final String TAG_SPEED = "godofthings:wand_speed";
    public static final String TAG_PERMANENT = "godofthings:wand_permanent";

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    /** 把配置写进玩家手持（主手优先）的神之工具 */
    public static void apply(ServerPlayer player, int speed, boolean permanent)
    {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof com.godofthings.item.GodFavorWandItem))
        {
            stack = player.getOffhandItem();
        }
        if (!(stack.getItem() instanceof com.godofthings.item.GodFavorWandItem))
        {
            return;
        }
        int s = Math.max(1, Math.min(speed, WandAcceleration.MAX_SPEED));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_SPEED, s);
        tag.putBoolean(TAG_PERMANENT, permanent);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        player.containerMenu.broadcastChanges();
    }

    public static void handle(WandConfigPayload msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx)
    {
        ctx.enqueueWork(() ->
        {
            if (ctx.player() instanceof ServerPlayer sender)
            {
                apply(sender, msg.speed(), msg.permanent());
            }
        });
    }
}