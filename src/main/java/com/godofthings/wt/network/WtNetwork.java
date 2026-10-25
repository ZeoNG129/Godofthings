package com.godofthings.wt.network;

import java.util.HashMap;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import appeng.menu.locator.ItemMenuHostLocator;

import com.godofthings.Godofthings;
import com.godofthings.wt.WtEvents;
import com.godofthings.wt.core.WtComponents;
import com.godofthings.wt.core.WtCraftingTerminalHandler;
import com.godofthings.wt.core.WtMagnetHandler;
import com.godofthings.wt.core.WtExtraTerminals;
import com.godofthings.wt.core.WtMagnetMode;
import com.godofthings.wt.core.WtTerminalDefinition;
import com.godofthings.wt.core.WtUniversalTerminalItem;

/**
 * 无线终端的自建网络包（[本地改动] 对应上游 CycleTerminalPacket / SelectTerminalPacket /
 * TerminalSettingsPacket / PickBlockPacket / UpdateWUTPackage / RestockAmountPacket
 * 六合一；全部注册在本模组命名空间下）。
 */
public final class WtNetwork
{
    private WtNetwork()
    {
    }

    // ---- C2S: 循环切换当前终端（照抄 CycleTerminalPacket）----
    public record CycleTerminal(boolean reverse) implements CustomPacketPayload
    {
        public static final Type<CycleTerminal> ID = new Type<>(Godofthings.godofthingsId("wt_cycle_terminal"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CycleTerminal> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, CycleTerminal::reverse, CycleTerminal::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return ID;
        }

        public void handle(Player player)
        {
            var containerMenu = player.containerMenu;
            ItemStack item;
            ItemMenuHostLocator locator;

            if (containerMenu instanceof appeng.menu.AEBaseMenu aeMenu
                    && aeMenu.getLocator() instanceof ItemMenuHostLocator l
                    && (item = l.locateItem(player)).getItem() instanceof WtUniversalTerminalItem)
            {
                locator = l;
            }
            else
            {
                // 手持场景（照抄 cycleHeldUniversalTerminal）
                if (player.getMainHandItem().getItem() instanceof WtUniversalTerminalItem)
                {
                    locator = appeng.menu.locator.MenuLocators.forInventorySlot(player.getInventory().selected);
                }
                else if (player.getOffhandItem().getItem() instanceof WtUniversalTerminalItem)
                {
                    locator = appeng.menu.locator.MenuLocators
                            .forInventorySlot(net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND);
                }
                else
                {
                    return;
                }
                item = locator.locateItem(player);
                WtTerminalDefinition.cycle(player, locator, item, reverse());
                // [本地改动] v7.15.6：附属终端也会被循环选中——内建选择为空且游标指向
                // 附属终端时，显示该附属终端的名字（原逻辑只查内建定义，附属切换无提示）。
                var current = WtTerminalDefinition.ofOrNull(item);
                if (current != null)
                {
                    player.displayClientMessage(Component.translatable("gui.godofthings.wut.current_terminal",
                            Component.translatable(current.translationKey())), true);
                }
                else
                {
                    var extras = WtExtraTerminals.list(item);
                    int cursor = WtExtraTerminals.current(item);
                    if (!extras.isEmpty() && cursor >= 0 && cursor < extras.size())
                    {
                        player.displayClientMessage(Component.translatable("gui.godofthings.wut.current_terminal",
                                WtExtraTerminals.displayName(extras.get(cursor))), true);
                    }
                }
                return;
            }

            WtTerminalDefinition.cycle(player, locator, item, reverse());
            WtTerminalDefinition.open(player, locator, true);
        }
    }

    // ---- C2S: 选中指定终端（照抄 SelectTerminalPacket）----
    public record SelectTerminal(String terminalName) implements CustomPacketPayload
    {
        public static final Type<SelectTerminal> ID = new Type<>(Godofthings.godofthingsId("wt_select_terminal"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectTerminal> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, SelectTerminal::terminalName, SelectTerminal::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return ID;
        }

        public void handle(Player player)
        {
            var containerMenu = player.containerMenu;
            if (!(containerMenu instanceof appeng.menu.AEBaseMenu aeMenu)
                    || !(aeMenu.getLocator() instanceof ItemMenuHostLocator locator))
            {
                return;
            }
            ItemStack item = locator.locateItem(player);
            if (!(item.getItem() instanceof WtUniversalTerminalItem))
            {
                return;
            }
            var terminal = WtTerminalDefinition.of(terminalName());
            if (!WtTerminalDefinition.hasTerminal(item, terminal))
            {
                return;
            }
            WtTerminalDefinition.setCurrentTerminal(player, locator, item, terminal);
            WtTerminalDefinition.open(player, locator, true);
        }
    }

    /** 客户端请求选中指定终端（照抄 AE2wtlibAPI.selectTerminal）。 */
    public static void sendSelectTerminal(String terminalName)
    {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new SelectTerminal(terminalName));
    }

    // ---- C2S: 选中指定附属终端（[本地改动] v7.15.6：内建终端走 SelectTerminal，
    // 附属终端按 EXTRA_TERMINALS 列表下标选择——选择按钮对附属终端的落点）----
    public record SelectExtra(int index) implements CustomPacketPayload
    {
        public static final Type<SelectExtra> ID = new Type<>(Godofthings.godofthingsId("wt_select_extra"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SelectExtra> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SelectExtra::index, SelectExtra::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return ID;
        }

        public void handle(Player player)
        {
            var containerMenu = player.containerMenu;
            if (!(containerMenu instanceof appeng.menu.AEBaseMenu aeMenu)
                    || !(aeMenu.getLocator() instanceof ItemMenuHostLocator locator))
            {
                return;
            }
            ItemStack item = locator.locateItem(player);
            if (!(item.getItem() instanceof WtUniversalTerminalItem))
            {
                return;
            }
            var extras = WtExtraTerminals.list(item);
            if (index() < 0 || index() >= extras.size())
            {
                return;
            }
            // 清掉内建选择、游标指向附属终端（open 靠游标走附属分支）
            item.remove(com.godofthings.Godofthings.WT_CURRENT_TERMINAL);
            WtExtraTerminals.setCurrent(item, index());
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            {
                com.godofthings.wt.network.WtNetwork.updateClientTerminal(serverPlayer, locator, item);
            }
            WtTerminalDefinition.open(player, locator, true);
        }
    }

    // ---- C2S: 循环切换磁卡模式（照抄 MagnetHotkeyAction：关→背包→ME→只入网→关）----
    public record CycleMagnetMode() implements CustomPacketPayload
    {
        public static final Type<CycleMagnetMode> ID = new Type<>(Godofthings.godofthingsId("wt_cycle_magnet_mode"));
        public static final CycleMagnetMode INSTANCE = new CycleMagnetMode();
        public static final StreamCodec<RegistryFriendlyByteBuf, CycleMagnetMode> STREAM_CODEC = StreamCodec.unit(
                INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return ID;
        }

        public void handle(Player player)
        {
            if (!(player instanceof ServerPlayer serverPlayer))
            {
                return;
            }
            var terminal = WtCraftingTerminalHandler.get(serverPlayer).getCraftingTerminal();
            if (terminal.isEmpty())
            {
                return;
            }
            var mode = WtMagnetHandler.getMagnetMode(terminal);
            var next = switch (mode)
            {
                case OFF ->
                {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "gui.godofthings.magnetcard.hotkey",
                            Component.translatable("gui.godofthings.magnetcard.desc.inv")
                                    .withStyle(net.minecraft.ChatFormatting.GREEN)), true);
                    yield WtMagnetMode.PICKUP_INVENTORY;
                }
                case PICKUP_INVENTORY ->
                {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "gui.godofthings.magnetcard.hotkey",
                            Component.translatable("gui.godofthings.magnetcard.desc.me")
                                    .withStyle(net.minecraft.ChatFormatting.GREEN)), true);
                    yield WtMagnetMode.PICKUP_ME;
                }
                case PICKUP_ME ->
                {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "gui.godofthings.magnetcard.hotkey",
                            Component.translatable("gui.godofthings.magnetcard.desc.me_no_magnet")
                                    .withStyle(net.minecraft.ChatFormatting.GREEN)), true);
                    yield WtMagnetMode.PICKUP_ME_NO_MAGNET;
                }
                case PICKUP_ME_NO_MAGNET ->
                {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "gui.godofthings.magnetcard.hotkey",
                            Component.translatable("gui.godofthings.magnetcard.desc.off")
                                    .withStyle(net.minecraft.ChatFormatting.RED)), true);
                    yield WtMagnetMode.OFF;
                }
                default -> mode;
            };
            WtMagnetHandler.saveMagnetMode(terminal, next);
        }
    }

    // ---- C2S: 终端设置（照抄 TerminalSettingsPacket）----
    public record TerminalSettings(ItemMenuHostLocator terminal, boolean pickBlock, boolean restock,
            boolean magnet, boolean pickupToME, boolean craftIfMissing) implements CustomPacketPayload
    {
        public static final Type<TerminalSettings> ID = new Type<>(Godofthings.godofthingsId("wt_terminal_settings"));
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSettings> STREAM_CODEC = StreamCodec
                .composite(
                        WtComponents.MENU_HOST_LOCATOR_STREAM_CODEC, TerminalSettings::terminal,
                        ByteBufCodecs.BOOL, TerminalSettings::pickBlock,
                        ByteBufCodecs.BOOL, TerminalSettings::restock,
                        ByteBufCodecs.BOOL, TerminalSettings::magnet,
                        ByteBufCodecs.BOOL, TerminalSettings::pickupToME,
                        ByteBufCodecs.BOOL, TerminalSettings::craftIfMissing,
                        TerminalSettings::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return ID;
        }

        public void handle(Player player)
        {
            var stack = terminal.locateItem(player);
            stack.set(WtComponents.PICK_BLOCK, pickBlock);
            stack.set(WtComponents.CRAFT_IF_MISSING, craftIfMissing);
            stack.set(WtComponents.RESTOCK, restock);
            var magnetSettings = stack.getOrDefault(Godofthings.WT_MAGNET_SETTINGS, WtMagnetMode.OFF);
            magnetSettings = magnetSettings.set(magnet, pickupToME);
            stack.set(Godofthings.WT_MAGNET_SETTINGS, magnetSettings);
        }
    }

    // ---- C2S: 中键选取方块（照抄 PickBlockPacket）----
    public record PickBlock(ItemStack itemStack) implements CustomPacketPayload
    {
        public static final Type<PickBlock> ID = new Type<>(Godofthings.godofthingsId("wt_pick_block"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PickBlock> STREAM_CODEC = ItemStack.STREAM_CODEC
                .map(PickBlock::new, PickBlock::itemStack);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return ID;
        }

        public void handle(Player player)
        {
            if (player instanceof ServerPlayer serverPlayer)
            {
                WtEvents.pickBlock(serverPlayer, itemStack);
            }
        }
    }

    // ---- S2C: 同步终端组件补丁（照抄 UpdateWUTPackage）----
    public record UpdateTerminal(ItemMenuHostLocator locator, net.minecraft.core.component.DataComponentPatch patch)
            implements CustomPacketPayload
    {
        public static final Type<UpdateTerminal> ID = new Type<>(Godofthings.godofthingsId("wt_update_terminal"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateTerminal> STREAM_CODEC = StreamCodec
                .composite(
                        WtComponents.MENU_HOST_LOCATOR_STREAM_CODEC, UpdateTerminal::locator,
                        net.minecraft.core.component.DataComponentPatch.STREAM_CODEC, UpdateTerminal::patch,
                        UpdateTerminal::new);

        public UpdateTerminal(ItemMenuHostLocator locator, ItemStack stack)
        {
            this(locator, stack.getComponentsPatch());
        }

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return ID;
        }

        public void handle(Player player)
        {
            // 客户端：把服务端的组件补丁应用回本地终端物品（通用终端的宿主类）
            var host = locator.locate(player, com.godofthings.wt.core.WtMenuHost.class);
            if (host != null)
            {
                host.getItemStack().applyComponents(patch());
            }
        }
    }

    // ---- S2C: 可补货数量（照抄 RestockAmountPacket）----
    public record RestockAmount(HashMap<Holder<Item>, Long> amounts) implements CustomPacketPayload
    {
        public static final Type<RestockAmount> ID = new Type<>(Godofthings.godofthingsId("wt_restock_amount"));

        @SuppressWarnings({"unchecked", "rawtypes"})
        public static final StreamCodec<RegistryFriendlyByteBuf, RestockAmount> STREAM_CODEC = new StreamCodec<>()
        {
            @Override
            public RestockAmount decode(RegistryFriendlyByteBuf buf)
            {
                int size = buf.readVarInt();
                HashMap<Holder<Item>, Long> map = new HashMap<>();
                for (int i = 0; i < size; i++)
                {
                    int id = ByteBufCodecs.VAR_INT.decode(buf);
                    Holder<Item> item = net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getHolder(id).orElse(null);
                    long count = buf.readLong();
                    if (item != null)
                    {
                        map.put(item, count);
                    }
                }
                return new RestockAmount(map);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, RestockAmount value)
            {
                buf.writeVarInt(value.amounts.size());
                value.amounts.forEach((holder, count) -> {
                    ByteBufCodecs.VAR_INT.encode(buf, net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(holder.value()));
                    buf.writeLong(count);
                });
            }
        };

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return ID;
        }

        public void handle(Player player)
        {
            if (player instanceof net.minecraft.client.player.LocalPlayer)
            {
                com.godofthings.wt.client.WtClientState.setRestockAmounts(amounts());
            }
        }
    }

    // ---- S2C: 补货槽位同步（照抄 UpdateRestockPacket）----
    public record UpdateRestock(int slot, ItemStack stack) implements CustomPacketPayload
    {
        public static final Type<UpdateRestock> ID = new Type<>(Godofthings.godofthingsId("wt_update_restock"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateRestock> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, UpdateRestock::slot,
                ItemStack.STREAM_CODEC, UpdateRestock::stack,
                UpdateRestock::new);

        @Override
        public Type<? extends CustomPacketPayload> type()
        {
            return ID;
        }

        public void handle(Player player)
        {
            if (player instanceof net.minecraft.client.player.LocalPlayer localPlayer)
            {
                var inv = localPlayer.getInventory();
                if (slot == Inventory.INVENTORY_SIZE)
                {
                    inv.offhand.set(0, stack());
                }
                else if (slot >= 0 && slot < inv.items.size())
                {
                    inv.items.set(slot, stack());
                }
            }
        }
    }

    /** 服务端把组件同步给客户端（照抄 WUTHandler.updateClientTerminal）。 */
    public static void updateClientTerminal(ServerPlayer player, ItemMenuHostLocator locator, ItemStack stack)
    {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new UpdateTerminal(locator, stack));
    }

    /** 注册全部包（RegisterPayloadHandlersEvent 里调用）。 */
    public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event)
    {
        var registrar = event.registrar(Godofthings.MODID);
        registrar.playToServer(CycleTerminal.ID, CycleTerminal.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle(context.player())));
        registrar.playToServer(CycleMagnetMode.ID, CycleMagnetMode.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle(context.player())));
        registrar.playToServer(SelectTerminal.ID, SelectTerminal.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle(context.player())));
        registrar.playToServer(SelectExtra.ID, SelectExtra.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle(context.player())));
        registrar.playToServer(TerminalSettings.ID, TerminalSettings.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle(context.player())));
        registrar.playToServer(PickBlock.ID, PickBlock.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle(context.player())));
        registrar.playToClient(UpdateTerminal.ID, UpdateTerminal.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle(context.player())));
        registrar.playToClient(RestockAmount.ID, RestockAmount.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle(context.player())));
        registrar.playToClient(UpdateRestock.ID, UpdateRestock.STREAM_CODEC,
                (packet, context) -> context.enqueueWork(() -> packet.handle(context.player())));
    }
}
