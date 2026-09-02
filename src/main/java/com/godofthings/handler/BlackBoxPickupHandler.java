package com.godofthings.handler;

import com.godofthings.Godofthings;
import com.godofthings.item.BlackBoxData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * 神之黑盒拾取拦截：玩家物品栏/背包中有开启的黑盒时，
 * 拾取物先入黑盒（白名单外销毁、白名单内无堆叠上限保留），不再进入普通物品栏。
 */
@EventBusSubscriber(modid = Godofthings.MODID)
public class BlackBoxPickupHandler
{
    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event)
    {
        Player player = event.getPlayer();
        if (player.level().isClientSide)
        {
            return;
        }

        ItemStack box = BlackBoxData.findEnabledBox(player);
        if (box.isEmpty())
        {
            return;
        }

        ItemEntity itemEntity = event.getItemEntity();
        ItemStack picked = itemEntity.getItem();
        if (picked.isEmpty())
        {
            return;
        }

        // 白名单外销毁，白名单内（或未启用白名单）存入黑盒无堆叠上限
        if (BlackBoxData.shouldKeep(box, picked, player.level().registryAccess()))
        {
            BlackBoxData.addToStorage(box, picked, player.level().registryAccess());
        }

        // 阻止默认拾取并移除掉落物实体（物品已处置：入库或销毁）
        event.setCanPickup(TriState.FALSE);
        picked.setCount(0);
        itemEntity.discard();
    }
}
