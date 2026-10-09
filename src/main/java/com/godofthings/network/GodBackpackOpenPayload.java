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
 * <b>① 手上拿着的（主手 → 副手）→ ② Curios 背部槽 → ③ 主物品栏 0..35 里第一个</b>；
 * 三处都没有就发一条 actionbar 提示，不打开任何界面。</p>
 *
 * <p><b>为什么手持优先</b>：原先的顺序是「Curios → 物品栏第一个」，于是玩家有多个背包时，
 * 按 B 永远打开物品栏里那一个（用户实测报过「不管我有多少个背包，打开显示的都是一个背包」）。
 * 现在手上拿着哪个就开哪个，符合直觉；没拿在手上时才退回「饰品栏 / 物品栏第一个」。</p>
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
            Inventory inventory = player.getInventory();
            // ① 手上拿着的：主手（= 快捷栏当前选中格）优先，其次副手
            int selected = inventory.selected;
            if (inventory.items.get(selected).getItem() instanceof GodBackpackItem)
            {
                GodBackpackItem.openMenu(player, selected);
                return;
            }
            if (inventory.offhand.get(0).getItem() instanceof GodBackpackItem)
            {
                GodBackpackItem.openMenu(player, GodBackpackItem.OFFHAND_SLOT);
                return;
            }
            // ② Curios 背部槽（没装 Curios 时反射工具直接返回空）
            if (!CuriosCompat.findBackpackInBackSlot(player).isEmpty())
            {
                GodBackpackItem.openMenu(player, GodBackpackMenu.SLOT_CURIOS);
                return;
            }
            // ③ 主物品栏 0..35（与右键打开时用的槽位编号一致）
            for (int slot = 0; slot < inventory.items.size(); slot++)
            {
                if (inventory.items.get(slot).getItem() instanceof GodBackpackItem)
                {
                    GodBackpackItem.openMenu(player, slot);
                    return;
                }
            }
            // 身上没有背包：actionbar 提示（不弹界面）
            player.displayClientMessage(Component.translatable("gui.godofthings.backpack.not_found"), true);
        });
    }
}
