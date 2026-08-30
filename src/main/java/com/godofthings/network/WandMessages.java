package com.godofthings.network;

import com.godofthings.Godofthings;
import com.godofthings.item.GodFavorWandItem;
import com.godofthings.item.WandItemUtils;
import com.godofthings.item.WandModes;
import com.godofthings.modes.ModeManager;
import com.godofthings.modes.ToolMode;
import com.godofthings.utils.mining.MiningDispatcher;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

/**
 * 造化垂青之杖的网络通道。
 */
public class WandMessages
{
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.tryBuild(Godofthings.MODID, "wand"),
            () -> VERSION, VERSION::equals, VERSION::equals);

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

    public static void register()
    {
        CHANNEL.registerMessage(0, WandActionMessage.class,
                WandActionMessage::encode, WandActionMessage::decode, WandActionMessage::handle);
        CHANNEL.registerMessage(1, ModeSwitchMessage.class,
                ModeSwitchMessage::encode, ModeSwitchMessage::decode, ModeSwitchMessage::handle);
        CHANNEL.registerMessage(2, MiningControlMessage.class,
                MiningControlMessage::encode, MiningControlMessage::decode, MiningControlMessage::handle);
    }

    public static void send(WandAction action)
    {
        CHANNEL.sendToServer(new WandActionMessage(action));
    }

    public static void sendModeSwitch(ToolMode mode)
    {
        CHANNEL.sendToServer(new ModeSwitchMessage(mode));
    }

    public static void sendMiningControl(MiningControl control, boolean value)
    {
        CHANNEL.sendToServer(new MiningControlMessage(control, value));
    }

    // ==================== 简单动作包 ====================

    public static class WandActionMessage
    {
        private final WandAction action;

        public WandActionMessage(WandAction action)
        {
            this.action = action;
        }

        public static void encode(WandActionMessage msg, FriendlyByteBuf buf)
        {
            buf.writeEnum(msg.action);
        }

        public static WandActionMessage decode(FriendlyByteBuf buf)
        {
            return new WandActionMessage(buf.readEnum(WandAction.class));
        }

        public static void handle(WandActionMessage msg, Supplier<NetworkEvent.Context> ctxSupplier)
        {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() ->
            {
                ServerPlayer sender = ctx.getSender();
                if (sender == null)
                {
                    return;
                }
                ItemStack stack = findWand(sender);
                if (stack == null)
                {
                    return;
                }

                switch (msg.action)
                {
                    case SILK_TOUCH -> switchEnchant(sender, stack, false);
                    case FORTUNE -> switchEnchant(sender, stack, true);
                    case TOGGLE_FORCE_KILL ->
                            WandModes.setForceKillEnabled(stack, !WandModes.isForceKillEnabled(stack));
                    case TOGGLE_CAPTURE ->
                            WandModes.setBeefCaptureEnabled(stack, !WandModes.isBeefCaptureEnabled(stack));
                    case TOGGLE_INVULNERABILITY ->
                            WandModes.setBeefInvulnerabilityEnabled(stack, !WandModes.isBeefInvulnerabilityEnabled(stack));
                    case TOGGLE_CHAIN_MINING ->
                            WandModes.setChainMiningEnabled(stack, !WandModes.isChainMiningEnabled(stack));
                    case TOGGLE_ENHANCED_CHAIN ->
                            toggleMode(sender, stack, ToolMode.ENHANCED_CHAIN_MINING);
                    case TOGGLE_FORCE_MINING ->
                            toggleMode(sender, stack, ToolMode.FORCE_MINING);
                    case TOGGLE_AE_STORAGE_PRIORITY ->
                            toggleMode(sender, stack, ToolMode.AE_STORAGE_PRIORITY);
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    // ==================== 模式轮盘包 ====================

    public static class ModeSwitchMessage
    {
        private final ToolMode mode;

        public ModeSwitchMessage(ToolMode mode)
        {
            this.mode = mode;
        }

        public static void encode(ModeSwitchMessage msg, FriendlyByteBuf buf)
        {
            buf.writeEnum(msg.mode);
        }

        public static ModeSwitchMessage decode(FriendlyByteBuf buf)
        {
            return new ModeSwitchMessage(buf.readEnum(ToolMode.class));
        }

        public static void handle(ModeSwitchMessage msg, Supplier<NetworkEvent.Context> ctxSupplier)
        {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() ->
            {
                ServerPlayer sender = ctx.getSender();
                if (sender == null)
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
                modeManager.toggleMode(msg.mode);
                modeManager.saveToStack(target);

                if (target.getItem() instanceof GodFavorWandItem wand)
                {
                    ItemStack finalStack = wand.switchToolModeItem(target, modeManager);
                    if (!finalStack.isEmpty())
                    {
                        replaceInHand(sender, finalStack);
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    // ==================== 挖掘控制包 ====================

    public static class MiningControlMessage
    {
        private final MiningControl control;
        private final boolean value;

        public MiningControlMessage(MiningControl control, boolean value)
        {
            this.control = control;
            this.value = value;
        }

        public static void encode(MiningControlMessage msg, FriendlyByteBuf buf)
        {
            buf.writeEnum(msg.control);
            buf.writeBoolean(msg.value);
        }

        public static MiningControlMessage decode(FriendlyByteBuf buf)
        {
            return new MiningControlMessage(buf.readEnum(MiningControl.class), buf.readBoolean());
        }

        public static void handle(MiningControlMessage msg, Supplier<NetworkEvent.Context> ctxSupplier)
        {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() ->
            {
                ServerPlayer sender = ctx.getSender();
                if (sender == null)
                {
                    return;
                }
                switch (msg.control)
                {
                    case TAB_PRESSED -> MiningDispatcher.setTabPressed(sender, msg.value);
                    case TRIGGER_FORCE -> MiningDispatcher.dispatchForceBreak(sender, msg.value);
                }
            });
            ctx.setPacketHandled(true);
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
            wand.switchEnchantmentMode(stack, !fortune);
        }
        else
        {
            WandItemUtils.switchEnchant(stack, fortune);
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
            wand.updateEnchantments(stack);
        }
    }

    private static void replaceInHand(ServerPlayer player, ItemStack newStack)
    {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof GodFavorWandItem)
        {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, newStack);
        }
        else
        {
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, newStack);
        }
    }
}
