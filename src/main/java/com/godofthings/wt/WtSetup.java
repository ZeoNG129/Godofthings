package com.godofthings.wt;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import com.godofthings.wt.terminals.WtAccessMenu;
import com.godofthings.wt.terminals.WtCraftingMenu;
import com.godofthings.wt.terminals.WtEncodingMenu;
import com.godofthings.wt.terminals.WtMagnetMenu;
import com.godofthings.wt.terminals.WtTrashMenu;

/**
 * 无线终端的 mod-bus 接线（照抄 ae2wtlib 的 {@code AE2wtlibForge} 构造器段：
 * 菜单注册在 RegisterEvent（MENU 键）、事件订阅在 NeoForge 总线）。
 */
public final class WtSetup
{
    private WtSetup()
    {
    }

    public static void init(IEventBus modEventBus)
    {
        modEventBus.addListener((RegisterEvent e) -> {
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.MENU))
            {
                Registry.register(BuiltInRegistries.MENU, WtCraftingMenu.ID, WtCraftingMenu.TYPE);
                Registry.register(BuiltInRegistries.MENU, WtEncodingMenu.ID, WtEncodingMenu.TYPE);
                Registry.register(BuiltInRegistries.MENU, WtAccessMenu.ID, WtAccessMenu.TYPE);
                Registry.register(BuiltInRegistries.MENU, WtMagnetMenu.ID, WtMagnetMenu.TYPE);
                Registry.register(BuiltInRegistries.MENU, WtTrashMenu.ID, WtTrashMenu.TYPE);
            }
            // WtComponents 静态构造的组件键在此入表（v7.15.3 修：漏注册导致终端物品
            // 带上未注册组件后存盘 / 同步直接崩 "Unregistered component"）。
            // 注意：wt_current_terminal / wt_magnet_settings 已由 Godofthings.DATA_COMPONENTS
            // 注册，跳过 BUILT 里的同 id 键防止重复注册崩溃。
            if (e.getRegistryKey().equals(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE))
            {
                com.godofthings.wt.core.WtComponents.BUILT.forEach((id, type) ->
                {
                    if (BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(id))
                    {
                        return;
                    }
                    Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, type);
                });
            }
        });

        // 磁力 / 补货 / ME 直接入网（NeoForge 总线）
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) -> {
                    if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp
                            && !sp.level().isClientSide())
                    {
                        // 磁力扫描（每 tick 一台终端一次）：从身上找装了磁卡的本地通用终端
                        var terminal = com.godofthings.wt.core.WtMagnetTickHelper.findMagnetTerminal(sp);
                        if (!terminal.isEmpty())
                        {
                            com.godofthings.wt.core.WtMagnetHandler.handle(sp, terminal);
                        }
                        // 补货状态刷新（对应上游 AE2wtlibClient.clientTick 的服务端镜像）
                        com.godofthings.wt.core.WtCraftingTerminalHandler.get(sp).checkTerminal();
                    }
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (ItemEntityPickupEvent.Pre e) -> {
                    if (e.canPickup().isFalse())
                    {
                        return;
                    }
                    var entity = e.getItemEntity();
                    var player = e.getPlayer();
                    if (e.canPickup().isDefault())
                    {
                        if (entity.hasPickUpDelay())
                        {
                            return;
                        }
                        if (entity.target != null && !entity.target.equals(player.getUUID()))
                        {
                            return;
                        }
                    }
                    com.godofthings.wt.WtEvents.insertStackInME(entity, player);
                });
        // 补货（照抄上游三个事件：用完物品 / 右键方块 / 右键实体 / 弓拉弦）
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent.Finish e) -> {
                    if (!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player))
                    {
                        return;
                    }
                    com.godofthings.wt.WtEvents.restock(player, e.getItem(), e.getResultStack(),
                            e::setResultStack);
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
                    if (!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
                            || e.isCanceled())
                    {
                        return;
                    }
                    var item = e.getItemStack();
                    com.godofthings.wt.WtEvents.restock(player, item, item,
                            stack -> player.setItemInHand(e.getHand(), stack));
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific e) -> {
                    if (!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)
                            || e.isCanceled())
                    {
                        return;
                    }
                    var item = e.getItemStack();
                    com.godofthings.wt.WtEvents.restock(player, item, item,
                            stack -> player.setItemInHand(e.getHand(), stack));
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.ArrowNockEvent e) -> {
                    if (!e.hasAmmo())
                    {
                        return;
                    }
                    if (!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player))
                    {
                        return;
                    }
                    ItemStack projectile = player.getProjectile(e.getBow());
                    com.godofthings.wt.WtEvents.restock(player, projectile, projectile, stack -> {
                    });
                });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.ArrowLooseEvent e) -> {
                    if (!e.hasAmmo())
                    {
                        return;
                    }
                    if (!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player))
                    {
                        return;
                    }
                    ItemStack projectile = player.getProjectile(e.getBow());
                    com.godofthings.wt.WtEvents.restock(player, projectile, projectile, stack -> {
                    });
                });
        // 玩家退出时清缓存
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) ->
                        com.godofthings.wt.core.WtCraftingTerminalHandler.invalidate(e.getEntity()));
    }
}
