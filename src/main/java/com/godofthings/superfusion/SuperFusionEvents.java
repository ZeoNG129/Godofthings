package com.godofthings.superfusion;

import mekanism.api.IConfigCardAccess;
import mekanism.common.attachments.FrequencyAware;
import mekanism.common.content.qio.IQIOFrequencyHolder;
import mekanism.common.content.qio.QIOFrequency;
import mekanism.common.lib.frequency.Frequency.FrequencyIdentity;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.registries.MekanismDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 超级融合卡的服务端事件（软依赖 Mekanism）。
 * <p>
 * 三件事：
 * <ul>
 *   <li><b>右键机器拦截</b>：手持融合卡右键时取消原版交互（机器 GUI 不会打开），转交融合卡逻辑。
 *       照抄上游 {@code ModEvents.onRightClickBlock}——Mekanism 机器的 {@code useWithoutItem}
 *       优先级高于物品 {@code useOn}，不拦截的话右键永远轮不到融合卡；</li>
 *   <li><b>QIO 频率绑定</b>：潜行右键 QIO 仪表盘，把卡绑到该频率（材料可从 QIO 取）；</li>
 *   <li><b>网络载荷与能力注册</b>。</li>
 * </ul>
 */
@EventBusSubscriber(modid = com.godofthings.Godofthings.MODID)
public final class SuperFusionEvents
{
    private SuperFusionEvents()
    {
    }

    public static boolean isMekanismLoaded()
    {
        return net.neoforged.fml.ModList.get().isLoaded("mekanism");
    }

    /** 手持融合卡右键：拦截并执行融合卡当前模式（潜行则复制配置或绑 QIO） */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event)
    {
        if (!isMekanismLoaded())
        {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof SuperFusionItem))
        {
            return;
        }

        // 潜行 + QIO 仪表盘：绑定 QIO 频率（优先级最高，与上游一致）
        BlockPos pos = event.getPos();
        if (player.isShiftKeyDown()
                && event.getLevel().getBlockEntity(pos) instanceof IQIOFrequencyHolder)
        {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            if (event.getLevel().isClientSide)
            {
                return;
            }
            handleQioBinding(event, player, stack, pos);
            return;
        }

        // 潜行右键：复制机器配置
        if (player.isShiftKeyDown())
        {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            if (!event.getLevel().isClientSide)
            {
                MemoryCardLogic.handleCopy(event.getLevel(), pos, player, stack);
            }
            return;
        }

        // 普通右键：执行当前模式。客户端直接取消（防抖动），服务端也取消并执行
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
        if (event.getLevel().isClientSide)
        {
            return;
        }
        net.minecraft.core.Direction face = event.getFace() == null
                ? net.minecraft.core.Direction.UP : event.getFace();
        switch (SuperFusionItem.getMode(stack))
        {
            case TIER_INSTALL -> SuperFusionItem.handleTierOperation(event.getLevel(), pos, face, player, stack);
            case MODULE_UPGRADE -> SuperFusionItem.handleModuleOperation(event.getLevel(), pos, face, player, stack);
            case MEMORY_COPY -> MemoryCardLogic.handlePaste(event.getLevel(), pos, player, stack);
            case FULL_PASTE -> MemoryCardLogic.handleFullPaste(event.getLevel(), pos, player, stack);
        }
    }

    /** 把卡绑到目标 QIO 仪表盘的频率上（之后材料可从 QIO 里扣） */
    private static void handleQioBinding(PlayerInteractEvent.RightClickBlock event, Player player,
                                         ItemStack stack, BlockPos pos)
    {
        if (!(event.getLevel().getBlockEntity(pos) instanceof IQIOFrequencyHolder holder))
        {
            return;
        }
        QIOFrequency freq = holder.getQIOFrequency();
        if (freq == null || !freq.isValid())
        {
            player.displayClientMessage(Component.translatable(
                    "message.godofthings.super_fusion.qio_no_frequency")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        FrequencyIdentity identity = freq.getIdentity();
        stack.set(MekanismDataComponents.QIO_FREQUENCY.get(),
                FrequencyAware.create(FrequencyType.QIO, identity, player.getUUID()));
        player.displayClientMessage(Component.translatable(
                "message.godofthings.super_fusion.qio_bound", freq.getName())
                .withStyle(ChatFormatting.GREEN), true);
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event)
    {
        if (!isMekanismLoaded())
        {
            return;
        }
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(com.godofthings.superfusion.network.FusionActionPayload.TYPE,
                com.godofthings.superfusion.network.FusionActionPayload.CODEC,
                com.godofthings.superfusion.network.FusionActionPayload::handle);
        registrar.playToServer(com.godofthings.superfusion.network.MiddleClickPayload.TYPE,
                com.godofthings.superfusion.network.MiddleClickPayload.CODEC,
                com.godofthings.superfusion.network.MiddleClickPayload::handle);
        registrar.playToServer(com.godofthings.superfusion.network.BatchSelectionPayload.TYPE,
                com.godofthings.superfusion.network.BatchSelectionPayload.CODEC,
                com.godofthings.superfusion.network.BatchSelectionPayload::handle);
    }
}
