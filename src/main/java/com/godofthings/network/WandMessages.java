package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.item.GodFavorWandItem;
import com.godofthings.item.WandItemUtils;
import com.godofthings.modes.ModeManager;
import com.godofthings.modes.ToolMode;
import com.godofthings.utils.mining.MiningDispatcher;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 造化垂青之杖的网络通道（NeoForge 1.21.1：CustomPacketPayload + PayloadRegistrar）。
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public class WandMessages
{
    @SubscribeEvent
    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(WandActionPayload.TYPE, WandActionPayload.STREAM_CODEC, WandActionPayload::handle);
        registrar.playToServer(ModeSwitchPayload.TYPE, ModeSwitchPayload.STREAM_CODEC, ModeSwitchPayload::handle);
        registrar.playToServer(MiningControlPayload.TYPE, MiningControlPayload.STREAM_CODEC, MiningControlPayload::handle);
    }

    public enum WandAction
    {
        SILK_TOUCH,
        FORTUNE,
        TOGGLE_FORCE_KILL,
        TOGGLE_CAPTURE,
        TOGGLE_INVULNERABILITY,
        TOGGLE_CHAIN_MINING,
        TOGGLE_ENHANCED_CHAIN,
        TOGGLE_FORCE_MINING,
        TOGGLE_AE_STORAGE_PRIORITY
    }

    public enum MiningControl
    {
        TAB_PRESSED,
        TRIGGER_FORCE
    }

    public static void send(WandAction action)
    {
        PacketDistributor.sendToServer(new WandActionPayload(action));
    }

    public static void sendModeSwitch(ToolMode mode)
    {
        PacketDistributor.sendToServer(new ModeSwitchPayload(mode));
    }

    public static void sendMiningControl(MiningControl control, boolean value)
    {
        PacketDistributor.sendToServer(new MiningControlPayload(control, value));
    }

    // ==================== 简单动作包 ====================

    public record WandActionPayload(WandAction action) implements CustomPacketPayload
    {
        public static final Type<WandActionPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "wand_action"));
        public static final StreamCodec<ByteBuf, WandActionPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT.map(i -> WandAction.values()[i], WandAction::ordinal), WandActionPayload::action,
                WandActionPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(WandActionPayload msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer sender))
                {
                    return;
                }
                ItemStack stack = findWand(sender);
                if (stack == null)
                {
                    return;
                }

                switch (msg.action())
                {
                    case SILK_TOUCH -> switchEnchant(sender, stack, false);
                    case FORTUNE -> switchEnchant(sender, stack, true);
                    case TOGGLE_FORCE_KILL ->
                            com.godofthings.item.WandModes.setForceKillEnabled(stack, !com.godofthings.item.WandModes.isForceKillEnabled(stack));
                    case TOGGLE_CAPTURE ->
                            com.godofthings.item.WandModes.setBeefCaptureEnabled(stack, !com.godofthings.item.WandModes.isBeefCaptureEnabled(stack));
                    case TOGGLE_INVULNERABILITY ->
                            com.godofthings.item.WandModes.setBeefInvulnerabilityEnabled(stack, !com.godofthings.item.WandModes.isBeefInvulnerabilityEnabled(stack));
                    case TOGGLE_CHAIN_MINING ->
                            com.godofthings.item.WandModes.setChainMiningEnabled(stack, !com.godofthings.item.WandModes.isChainMiningEnabled(stack));
                    case TOGGLE_ENHANCED_CHAIN ->
                            toggleMode(sender, stack, ToolMode.ENHANCED_CHAIN_MINING);
                    case TOGGLE_FORCE_MINING ->
                            toggleMode(sender, stack, ToolMode.FORCE_MINING);
                    case TOGGLE_AE_STORAGE_PRIORITY ->
                            toggleMode(sender, stack, ToolMode.AE_STORAGE_PRIORITY);
                }
            });
        }
    }

    // ==================== 模式轮盘包 ====================

    public record ModeSwitchPayload(ToolMode mode) implements CustomPacketPayload
    {
        public static final Type<ModeSwitchPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "wand_mode_switch"));
        public static final StreamCodec<ByteBuf, ModeSwitchPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT.map(i -> ToolMode.values()[i], ToolMode::ordinal), ModeSwitchPayload::mode,
                ModeSwitchPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(ModeSwitchPayload msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer sender))
                {
                    return;
                }
                ItemStack target = findWand(sender);
                if (target == null)
                {
                    return;
                }

                ModeManager modeManager = new ModeManager();
                modeManager.loadFromStack(target);
                modeManager.toggleMode(msg.mode());
                modeManager.saveToStack(target);

                if (target.getItem() instanceof GodFavorWandItem wand)
                {
                    ItemStack finalStack = wand.switchToolModeItem(target, modeManager, sender.level().registryAccess());
                    if (!finalStack.isEmpty())
                    {
                        replaceInHand(sender, finalStack);
                    }
                }
            });
        }
    }

    // ==================== 挖掘控制包 ====================

    public record MiningControlPayload(MiningControl control, boolean value) implements CustomPacketPayload
    {
        public static final Type<MiningControlPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "wand_mining_control"));
        public static final StreamCodec<ByteBuf, MiningControlPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.INT.map(i -> MiningControl.values()[i], MiningControl::ordinal), MiningControlPayload::control,
                ByteBufCodecs.BOOL, MiningControlPayload::value,
                MiningControlPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return TYPE;
        }

        public static void handle(MiningControlPayload msg, net.neoforged.neoforge.network.handling.IPayloadContext ctx)
        {
            ctx.enqueueWork(() ->
            {
                if (!(ctx.player() instanceof ServerPlayer sender))
                {
                    return;
                }
                switch (msg.control())
                {
                    case TAB_PRESSED -> MiningDispatcher.setTabPressed(sender, msg.value());
                    case TRIGGER_FORCE -> MiningDispatcher.dispatchForceBreak(sender, msg.value());
                }
            });
        }
    }

    // ==================== 工具方法 ====================

    private static ItemStack findWand(ServerPlayer player)
    {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GodFavorWandItem)
        {
            return stack;
        }
        stack = player.getOffhandItem();
        if (stack.getItem() instanceof GodFavorWandItem)
        {
            return stack;
        }
        return null;
    }

    private static void switchEnchant(ServerPlayer player, ItemStack stack, boolean fortune)
    {
        if (stack.getItem() instanceof GodFavorWandItem wand)
        {
            wand.switchEnchantmentMode(stack, !fortune, player.level().registryAccess());
        }
        else
        {
            WandItemUtils.switchEnchant(stack, fortune, player.level().registryAccess());
        }
    }

    private static void toggleMode(ServerPlayer player, ItemStack stack, ToolMode mode)
    {
        if (stack.getItem() instanceof GodFavorWandItem wand)
        {
            ModeManager mm = new ModeManager();
            mm.loadFromStack(stack);
            mm.toggleMode(mode);
            mm.saveToStack(stack);
            wand.updateEnchantments(stack, player.level().registryAccess());
        }
    }

    private static void replaceInHand(ServerPlayer player, ItemStack newStack)
    {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof GodFavorWandItem)
        {
            player.setItemInHand(InteractionHand.MAIN_HAND, newStack);
        }
        else
        {
            player.setItemInHand(InteractionHand.OFF_HAND, newStack);
        }
    }
}
