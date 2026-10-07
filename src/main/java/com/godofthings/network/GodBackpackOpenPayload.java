package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.backpack.CuriosCompat;
import com.godofthings.backpack.GodBackpackItem;
import com.godofthings.backpack.GodBackpackMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 神之背包：客户端 → 服务端的「快捷键打开」空包（默认键 B）。
 *
 * <p>客户端只负责按键发这个包，<b>找背包与开界面全在服务端做</b>——背包可能装备在 Curios 背部槽、
 * 躺在主物品栏、或拿在副手，只有服务端才有权威位置。查找顺序：
 * ① Curios 背部槽（没装 Curios 时反射工具直接返回空）→ ② 主物品栏 0..35 → ③ 副手；
 * 三处都没有就发一条 actionbar 提示，不打开任何界面。</p>
 *
 * <p>空包（无字段），沿用通道版本 {@code "1"}，注册见 {@code beef/init/ModNetwork}。</p>
 */
public record GodBackpackOpenPayload() implements CustomPacketPayload
{
    public static final Type<GodBackpackOpenPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "god_backpack_open"));

    public static final StreamCodec<ByteBuf, GodBackpackOpenPayload> STREAM_CODEC =
            StreamCodec.unit(new GodBackpackOpenPayload());

    @Override
    public Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    public static void handle(GodBackpackOpenPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
            {
                return;
            }
            // ① Curios 背部槽
            if (!CuriosCompat.findBackpackInBackSlot(player).isEmpty())
            {
                GodBackpackItem.openMenu(player, GodBackpackMenu.SLOT_CURIOS);
                return;
            }
            // ② 主物品栏 0..35（与右键打开时用的槽位编号一致）
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.items.size(); slot++)
            {
                if (inventory.items.get(slot).getItem() instanceof GodBackpackItem)
                {
                    GodBackpackItem.openMenu(player, slot);
                    return;
                }
            }
            // ③ 副手
            if (inventory.offhand.get(0).getItem() instanceof GodBackpackItem)
            {
                GodBackpackItem.openMenu(player, GodBackpackItem.OFFHAND_SLOT);
                return;
            }
            // 身上没有背包：actionbar 提示（不弹界面）
            player.displayClientMessage(Component.translatable("gui.godofthings.backpack.not_found"), true);
        });
    }
}
