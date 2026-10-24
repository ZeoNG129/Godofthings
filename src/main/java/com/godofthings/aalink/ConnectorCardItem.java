package com.godofthings.aalink;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 无线连接器（卡）（照抄上游 OverloadedFrequencyCardItem，绑定对象按需求换成无线连接器）：
 * 把任意 AE 设备接入卡上绑定的无线连接器所在的 ME 网络。
 *
 * <ul>
 *   <li>潜行右键无线连接器 → 绑定（记录连接器维度 + 坐标 + 主人 UUID）</li>
 *   <li>潜行对空气右键 → 清除绑定</li>
 *   <li>右键任意 AE 设备 → 把该设备接入绑定连接器所在网络（并阻止方块 UI 打开）</li>
 *   <li>自动连接开启时，放置的方块 / 部件会自动接入（见 {@link ConnectorCardAutoConnectHandler}）</li>
 * </ul>
 *
 * <p>继承 AE2 的 {@code UpgradeCardItem}（与上游一致）：AE2 的升级槽用
 * {@code Upgrades.isUpgradeCardItem}（instanceof 判定）过滤可放入物品，
 * 不继承它就永远进不了任何升级槽——这是 v7.11.1 之前卡放不进终端的根因。</p>
 */
public class ConnectorCardItem extends appeng.items.materials.UpgradeCardItem
{
    private static final String TAG_AUTO_CONNECT = "AutoConnect";
    private static final String TAG_BOUND_CONNECTOR_DIM = "BoundConnectorDim";
    private static final String TAG_BOUND_CONNECTOR_POS = "BoundConnectorPos";
    private static final String TAG_OWNER_UUID = "OwnerUuid";

    public ConnectorCardItem(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    {
        return handleBlockUse(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        return handleBlockUse(context);
    }

    private InteractionResult handleBlockUse(UseOnContext context)
    {
        var player = context.getPlayer();
        if (player == null)
        {
            return InteractionResult.PASS;
        }

        var level = context.getLevel();
        if (level.isClientSide())
        {
            // 照抄上游：客户端发交互包，服务端统一处理（绑定 / 接入 / 解绑都走同一条链）
            PacketDistributor.sendToServer(new ConnectorCardUsePayload(
                    context.getHand(),
                    context.getClickedPos(),
                    context.getClickedFace(),
                    net.minecraft.client.gui.screens.Screen.hasShiftDown()));
            return InteractionResult.SUCCESS_NO_ITEM_USED;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        var stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown())
        {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide())
        {
            var data = getData(stack);
            if (data.isBound() && !data.canBeUsedBy(player.getUUID()))
            {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.aalink.card_owner_mismatch")
                                .withStyle(ChatFormatting.RED),
                        true);
                return InteractionResultHolder.fail(stack);
            }
            setData(stack, data.clearBinding());
            player.displayClientMessage(
                    Component.translatable("message.godofthings.aalink.card_cleared").withStyle(ChatFormatting.GREEN),
                    true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag tooltipFlag)
    {
        var data = getData(stack);
        if (data.isBound())
        {
            tooltip.add(Component.translatable("tooltip.godofthings.aalink.card_bound",
                            data.boundConnectorDimension().orElse("?"),
                            shortPos(data.boundConnectorPos().orElse(0L)))
                    .withStyle(ChatFormatting.AQUA));
        }
        else
        {
            tooltip.add(Component.translatable("tooltip.godofthings.aalink.card_unbound")
                    .withStyle(ChatFormatting.GRAY));
        }

        tooltip.add(Component.translatable(
                        data.autoConnect()
                                ? "tooltip.godofthings.aalink.card_auto_on"
                                : "tooltip.godofthings.aalink.card_auto_off")
                .withStyle(data.autoConnect() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));

        if (data.isBound())
        {
            tooltip.add(Component.translatable("tooltip.godofthings.aalink.card_bound_hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        else
        {
            tooltip.add(Component.translatable("tooltip.godofthings.aalink.card_bind_hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.godofthings.aalink.card_controls")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.godofthings.aalink.card_scroll_toggle")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable(
                        "tooltip.godofthings.aalink.card_link_shortcut",
                        Component.keybind("key.godofthings.aalink.card_link"))
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static String shortPos(long packed)
    {
        BlockPos pos = BlockPos.of(packed);
        return pos.toShortString();
    }

    public static ConnectorCardData getData(ItemStack stack)
    {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return fromTag(tag);
    }

    public static void setData(ItemStack stack, ConnectorCardData data)
    {
        var tag = toTag(data);
        if (tag.isEmpty())
        {
            stack.remove(DataComponents.CUSTOM_DATA);
        }
        else
        {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    /** 绑定到玩家指定的无线连接器（服务端）。 */
    public static void bindConnector(ItemStack stack, ServerLevel level, BlockPos connectorPos, UUID ownerUuid)
    {
        setData(stack, getData(stack).bind(
                level.dimension().location().toString(),
                connectorPos.asLong(),
                ownerUuid));
    }

    public static boolean toggleAutoConnect(ItemStack stack)
    {
        var data = getData(stack).toggleAutoConnect();
        setData(stack, data);
        return data.autoConnect();
    }

    /** 找到玩家身上可用于自动连接的卡（主手 > 副手 > 快捷栏 > 背包，照抄上游优先级）。 */
    public static Optional<ItemStack> findAutoConnectCard(Player player)
    {
        return selectCard(player, true).selected();
    }

    public static boolean hasMultipleAutoConnectCandidates(Player player)
    {
        return selectCard(player, true).ambiguous();
    }

    /** 快捷键「连接注视目标」的选卡（主手/副手/快捷栏/背包，要求未绑定或本人可用）。 */
    public static CardSelection selectLinkCard(Player player)
    {
        return selectCard(player, false);
    }

    /** 快捷键「切换自动连接」的选卡。 */
    public static CardSelection selectToggleCard(Player player)
    {
        return selectCard(player, false);
    }

    private static CardSelection selectCard(Player player, boolean requireAutoConnect)
    {
        UUID playerUuid = player.getUUID();

        var main = player.getMainHandItem();
        if (isCandidate(main, playerUuid, requireAutoConnect))
        {
            return CardSelection.selected(main);
        }

        var offhand = player.getOffhandItem();
        if (isCandidate(offhand, playerUuid, requireAutoConnect))
        {
            return CardSelection.selected(offhand);
        }

        // Curios 饰品槽里的卡（照抄上游 CuriosFrequencyCardFinder 的优先级位置）；
        // Curios 是可选依赖，没装时 findFirstInCurios 返回空
        var curiosCard = com.godofthings.backpack.CuriosCompat.findFirstInCurios(
                player, stack -> isCandidate(stack, playerUuid, requireAutoConnect));
        if (!curiosCard.isEmpty())
        {
            return CardSelection.selected(curiosCard);
        }

        // 插在无线终端升级槽里的卡（照抄上游 TerminalFrequencyCardFinder；只用于
        // 自动连接这种「只读」路径——升级槽返回的是反序列化快照，改了不落盘，
        // 所以 requireAutoConnect=false 的切换/连接动作跳过终端内卡，与上游一致）
        if (requireAutoConnect)
        {
            ItemStack terminalCard = findCardInTerminals(player, playerUuid);
            if (!terminalCard.isEmpty())
            {
                return CardSelection.selected(terminalCard);
            }
        }

        var inventory = player.getInventory();
        ItemStack best = ItemStack.EMPTY;
        int bestCount = 0;
        for (int slot = 0; slot < inventory.items.size(); slot++)
        {
            var stack = inventory.items.get(slot);
            if (isCandidate(stack, playerUuid, requireAutoConnect))
            {
                best = stack;
                bestCount++;
            }
        }

        if (bestCount == 0)
        {
            return CardSelection.empty();
        }
        // 与上游一致：同优先级多张卡时报告歧义，让玩家明确选一张
        if (bestCount > 1)
        {
            return CardSelection.ofAmbiguous();
        }
        return CardSelection.selected(best);
    }

    private static boolean isCandidate(ItemStack stack, UUID playerUuid, boolean requireAutoConnect)
    {
        if (!(stack.getItem() instanceof ConnectorCardItem))
        {
            return false;
        }
        var data = getData(stack);
        if (requireAutoConnect)
        {
            return data.isBound() && data.autoConnect() && data.canBeUsedBy(playerUuid);
        }
        return !data.isBound() || data.canBeUsedBy(playerUuid);
    }

    /**
     * 找插在玩家携带的无线终端（任何 {@code IUpgradeableItem}）升级槽里的连接器卡。
     * 只读快照（照抄上游 TerminalFrequencyCardFinder 的口径）。
     */
    private static ItemStack findCardInTerminals(Player player, UUID playerUuid)
    {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++)
        {
            var found = findCardInTerminalStack(inventory.items.get(i), playerUuid);
            if (!found.isEmpty())
            {
                return found;
            }
        }
        return findCardInTerminalStack(player.getOffhandItem(), playerUuid);
    }

    private static ItemStack findCardInTerminalStack(ItemStack terminalStack, UUID playerUuid)
    {
        if (terminalStack.isEmpty()
                || !(terminalStack.getItem() instanceof appeng.api.upgrades.IUpgradeableItem upgradeable))
        {
            return ItemStack.EMPTY;
        }
        var upgrades = upgradeable.getUpgrades(terminalStack);
        for (int slot = 0; slot < upgrades.size(); slot++)
        {
            var card = upgrades.getStackInSlot(slot);
            if (isCandidate(card, playerUuid, true))
            {
                return card;
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 终端联网重定向（照抄上游 WTMenuHostMixin 的 resolveFrequencyNode 语义）：
     * 终端升级槽里插着已绑定的连接器卡时，返回绑定连接器所在的 ME 网络；
     * 没插卡 / 未绑定 / 连接器不可用返回 null（调用方走原版无线访问点逻辑）。
     * 仅服务端（level 为 ServerLevel 时才有意义）。
     */
    public static appeng.api.networking.IGrid resolveBoundGridForTerminal(
            ItemStack terminalStack, net.minecraft.world.level.Level level)
    {
        if (terminalStack.isEmpty() || level == null || level.isClientSide())
        {
            return null;
        }
        if (!(terminalStack.getItem() instanceof appeng.api.upgrades.IUpgradeableItem upgradeable))
        {
            return null;
        }
        var upgrades = upgradeable.getUpgrades(terminalStack);
        for (int slot = 0; slot < upgrades.size(); slot++)
        {
            var card = upgrades.getStackInSlot(slot);
            if (!(card.getItem() instanceof ConnectorCardItem))
            {
                continue;
            }
            var data = getData(card);
            if (!data.isBound())
            {
                continue;
            }
            var dimId = net.minecraft.resources.ResourceLocation.tryParse(
                    data.boundConnectorDimension().orElse(""));
            if (dimId == null || !(level instanceof net.minecraft.server.level.ServerLevel serverLevel))
            {
                continue;
            }
            var hubLevel = serverLevel.getServer().getLevel(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION, dimId));
            if (hubLevel == null)
            {
                continue;
            }
            var hubPos = net.minecraft.core.BlockPos.of(data.boundConnectorPos().orElse(0L));
            if (!(hubLevel.getBlockEntity(hubPos) instanceof AaLinkConnectorBlockEntity hub))
            {
                continue;
            }
            var hubNode = hub.getHubNode();
            if (hubNode != null && hubNode.getGrid() != null)
            {
                return hubNode.getGrid();
            }
        }
        return null;
    }

    /** 选卡结果（对应上游 FrequencyCardCandidateSelector.Selection）。 */
    public record CardSelection(Optional<ItemStack> chosen, boolean isAmbiguous)
    {
        public boolean ambiguous()
        {
            return isAmbiguous;
        }

        public Optional<ItemStack> selected()
        {
            return chosen;
        }

        public static CardSelection empty()
        {
            return new CardSelection(Optional.empty(), false);
        }

        public static CardSelection ofAmbiguous()
        {
            return new CardSelection(Optional.empty(), true);
        }

        public static CardSelection selected(ItemStack value)
        {
            return new CardSelection(Optional.of(value), false);
        }
    }

    private static ConnectorCardData fromTag(CompoundTag tag)
    {
        boolean autoConnect = tag.getBoolean(TAG_AUTO_CONNECT);
        Optional<String> dim = tag.contains(TAG_BOUND_CONNECTOR_DIM)
                ? Optional.of(tag.getString(TAG_BOUND_CONNECTOR_DIM))
                : Optional.empty();
        Optional<Long> pos = tag.contains(TAG_BOUND_CONNECTOR_POS)
                ? Optional.of(tag.getLong(TAG_BOUND_CONNECTOR_POS))
                : Optional.empty();
        Optional<UUID> owner = Optional.empty();
        if (tag.contains(TAG_OWNER_UUID))
        {
            try
            {
                owner = Optional.of(UUID.fromString(tag.getString(TAG_OWNER_UUID)));
            }
            catch (IllegalArgumentException ignored)
            {
                owner = Optional.empty();
            }
        }
        return new ConnectorCardData(autoConnect, dim, pos, owner);
    }

    private static CompoundTag toTag(ConnectorCardData data)
    {
        var tag = new CompoundTag();
        if (data.autoConnect())
        {
            tag.putBoolean(TAG_AUTO_CONNECT, true);
        }
        data.boundConnectorDimension().ifPresent(value -> tag.putString(TAG_BOUND_CONNECTOR_DIM, value));
        data.boundConnectorPos().ifPresent(value -> tag.putLong(TAG_BOUND_CONNECTOR_POS, value));
        data.ownerUuid().ifPresent(value -> tag.putString(TAG_OWNER_UUID, value.toString()));
        return tag;
    }
}
