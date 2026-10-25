package com.godofthings.wt.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import appeng.menu.locator.ItemMenuHostLocator;

import com.godofthings.Godofthings;

/**
 * 无线终端定义注册表（照抄 ae2wtlib 的 {@code WTDefinition} + {@code WTDefinitionBuilder}
 * + {@code WUTHandler} 三合一，[本地改动] 只服务本模组的通用终端）。
 *
 * <p>本模组把三台终端注册进这里：crafting（AE2 无线合成终端）、pattern_encoding
 * （本模组移植的无线样板终端）、pattern_access（本模组移植的无线样板管理终端）。
 * 通用终端按组件键 {@code godofthings:wt_has_*_terminal} 判断装了哪几台，
 * {@code wt_current_terminal}（字符串 = terminalName）保存当前选择。</p>
 */
public record WtTerminalDefinition(String terminalName, ContainerOpener containerOpener,
        WtMenuHostFactory wTMenuHostFactory, MenuType<?> menuType, Item item,
        MutableComponent formattedName, String translationKey,
        String hotkeyName, WtComponentKey componentType, int upgradeCount)
{
    public interface ContainerOpener
    {
        boolean tryOpen(Player player, ItemMenuHostLocator locator, boolean returningFromSubmenu);
    }

    public interface WtMenuHostFactory
    {
        WtMenuHost create(WtTerminalItem item, Player player, ItemMenuHostLocator locator,
                java.util.function.BiConsumer<Player, appeng.menu.ISubMenu> returnToMainMenu);
    }

    /** 终端装载组件键的持有者（真实 DataComponentType 由 Godofthings 注册后回填）。 */
    public record WtComponentKey(String name)
    {
        @SuppressWarnings("unchecked")
        public net.minecraft.core.component.DataComponentType<com.mojang.datafixers.util.Unit> type()
        {
            return (net.minecraft.core.component.DataComponentType<com.mojang.datafixers.util.Unit>)
                    Godofthings.WT_HAS_TERMINALS.get(name).value();
        }
    }

    // ---- 注册表 ----

    private static final Map<String, WtTerminalDefinition> TERMINALS = new LinkedHashMap<>();
    private static final List<WtTerminalDefinition> TERMINAL_LIST = new ArrayList<>();

    public static void add(WtTerminalDefinition definition)
    {
        TERMINALS.put(definition.terminalName(), definition);
        TERMINAL_LIST.add(definition);
    }

    public static List<WtTerminalDefinition> wirelessTerminalList()
    {
        return TERMINAL_LIST;
    }

    public static WtTerminalDefinition of(String name)
    {
        var def = TERMINALS.get(name);
        if (def == null)
        {
            throw new IllegalArgumentException("WtTerminalDefinition " + name + " does not exist");
        }
        return def;
    }

    private static final Style STYLE_GRAY = Style.EMPTY.withColor(ChatFormatting.GRAY);

    // ---- 当前终端 / 循环 / 打开（照抄 WUTHandler，作用对象换成本地 WUT）----

    public static boolean hasTerminal(ItemStack stack, WtTerminalDefinition terminal)
    {
        if (stack.isEmpty())
        {
            return false;
        }
        if (stack.getItem() instanceof WtUniversalTerminalItem)
        {
            return stack.get(terminal.componentType().type()) != null;
        }
        return stack.getItem() == terminal.item();
    }

    /**
     * 取物品栈对应的终端定义：通用终端读当前选择（照抄 WTDefinition.ofOrNull(ItemStack)），
     * 独立终端物品按注册表里登记的 item 实例反查（照抄 ofOrNull(ItemWT)）。
     */
    public static WtTerminalDefinition ofOrNull(ItemStack stack)
    {
        if (stack.isEmpty())
        {
            return null;
        }
        if (!(stack.getItem() instanceof WtTerminalItem))
        {
            return null;
        }
        if (stack.getItem() instanceof WtUniversalTerminalItem)
        {
            String current = stack.get(com.godofthings.Godofthings.WT_CURRENT_TERMINAL);
            if (current != null && TERMINALS.containsKey(current))
            {
                var def = TERMINALS.get(current);
                if (stack.get(def.componentType().type()) != null)
                {
                    return def;
                }
            }
            // [本地改动] v7.15.5：内建选择无效时，若游标指向附属终端则维持附属选择
            // （不能自动写回内建终端，否则循环切到附属终端后会被立即拽回）。
            if (WtExtraTerminals.current(stack) >= 0 && !WtExtraTerminals.list(stack).isEmpty())
            {
                return null;
            }
            for (var def : TERMINAL_LIST)
            {
                if (stack.get(def.componentType().type()) != null)
                {
                    stack.set(com.godofthings.Godofthings.WT_CURRENT_TERMINAL, def.terminalName());
                    return def;
                }
            }
            return null;
        }
        // 独立终端物品：按物品实例反查定义（照抄 WTDefinition.ofOrNull(ItemWT)）
        for (var def : TERMINAL_LIST)
        {
            if (stack.getItem() == def.item())
            {
                return def;
            }
        }
        return null;
    }

    /** 非空版本（照抄 WTDefinition.of(ItemStack)，找不到抛 NPE）。 */
    public static WtTerminalDefinition of(ItemStack stack)
    {
        return java.util.Objects.requireNonNull(ofOrNull(stack));
    }

    public static void setCurrentTerminal(Player player, ItemMenuHostLocator locator, ItemStack stack,
            WtTerminalDefinition terminal)
    {
        if (!(stack.getItem() instanceof WtUniversalTerminalItem))
        {
            return;
        }
        if (!hasTerminal(stack, terminal))
        {
            return;
        }
        stack.set(com.godofthings.Godofthings.WT_CURRENT_TERMINAL, terminal.terminalName());
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
        {
            com.godofthings.wt.network.WtNetwork.updateClientTerminal(serverPlayer, locator, stack);
        }
    }

    /** 循环到下一个已安装终端（照抄 nextTerminal）。 */
    public static WtTerminalDefinition nextTerminal(ItemStack stack, boolean reverse)
    {
        WtTerminalDefinition next = ofOrNull(stack);
        if (next == null)
        {
            return null;
        }
        var terminals = TERMINAL_LIST;
        int guard = 0;
        do
        {
            int i;
            if (reverse)
            {
                i = terminals.indexOf(next) - 1;
                if (i == -1)
                {
                    i = terminals.size() - 1;
                }
            }
            else
            {
                i = terminals.indexOf(next) + 1;
                if (i == terminals.size())
                {
                    i = 0;
                }
            }
            next = terminals.get(i);
            guard++;
        } while (stack.get(next.componentType().type()) == null && guard <= terminals.size());
        return next;
    }

    public static void cycle(Player player, ItemMenuHostLocator locator, ItemStack stack,
            boolean isHandlingRightClick)
    {
        // [本地改动] v7.15.5：循环序列 = 已装内建终端 + 已合并附属终端（平铺后环形移动）
        var builtins = new ArrayList<WtTerminalDefinition>();
        for (var def : TERMINAL_LIST)
        {
            if (stack.get(def.componentType().type()) != null)
            {
                builtins.add(def);
            }
        }
        var extras = WtExtraTerminals.list(stack);
        int total = builtins.size() + extras.size();
        if (total == 0)
        {
            return;
        }

        int cur;
        var currentDef = ofOrNull(stack);
        if (currentDef != null)
        {
            cur = builtins.indexOf(currentDef);
            if (cur < 0)
            {
                return;
            }
        }
        else
        {
            int extraCur = WtExtraTerminals.current(stack);
            if (extraCur < 0 || extraCur >= extras.size())
            {
                return;
            }
            cur = builtins.size() + extraCur;
        }

        int next = ((cur + (isHandlingRightClick ? -1 : 1)) % total + total) % total;
        if (next < builtins.size())
        {
            // 切到内建终端
            stack.set(com.godofthings.Godofthings.WT_CURRENT_TERMINAL, builtins.get(next).terminalName());
            WtExtraTerminals.setCurrent(stack, -1);
        }
        else
        {
            // 切到附属终端：清掉内建选择，游标指向附属列表
            stack.remove(com.godofthings.Godofthings.WT_CURRENT_TERMINAL);
            WtExtraTerminals.setCurrent(stack, next - builtins.size());
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
        {
            com.godofthings.wt.network.WtNetwork.updateClientTerminal(serverPlayer, locator, stack);
        }
    }

    /**
     * 打开当前终端菜单（照抄 WUTHandler.open；耗电检查已被无耗能五连覆盖）。
     * [本地改动] v7.15.5：当前游标指向附属终端时，直接打开该终端物品自己的 AE2 原生菜单
     * （借它的 getMenuType / checkPreconditions，菜单宿主由 AE2 原生 WirelessTerminalItem
     * 路径创建，本模组组件仍存在通用终端上）。
     */
    public static boolean open(Player player, ItemMenuHostLocator locator, boolean returningFromSubmenu)
    {
        var stack = locator.locateItem(player);
        var current = ofOrNull(stack);
        if (current == null)
        {
            // 内建三台都没有：有附属终端时打开游标指向的附属终端
            var extras = WtExtraTerminals.list(stack);
            int cursor = WtExtraTerminals.current(stack);
            if (!extras.isEmpty() && cursor >= 0 && cursor < extras.size())
            {
                return openExtra(player, locator, stack, extras.get(cursor), returningFromSubmenu);
            }
            player.displayClientMessage(Component.translatable("msg.godofthings.wut.empty"), false);
            return false;
        }
        return current.containerOpener().tryOpen(player, locator, returningFromSubmenu);
    }

    /**
     * 打开一台合并进来的附属终端（[本地改动] 走 AE2 原生 MenuOpener：用附属终端物品
     * 自己的菜单类型 + 栈定位器（AE2 会按栈定位器调 getMenuHost 创建该终端原生宿主）。
     * 临时副本在打开前把电量充满——通用终端本身无耗能，附属终端的电量检查也一并免除，
     * 且副本不回写不会污染存档数据。
     */
    private static boolean openExtra(Player player, ItemMenuHostLocator locator, ItemStack stack,
            ItemStack extra, boolean returningFromSubmenu)
    {
        if (extra.getItem() instanceof appeng.items.tools.powered.WirelessTerminalItem terminal)
        {
            var copy = extra.copyWithCount(1);
            copy.set(appeng.api.ids.AEComponents.STORED_ENERGY, copy
                    .getOrDefault(appeng.api.ids.AEComponents.ENERGY_CAPACITY, 0.0) + 1.0);
            // [本地改动] v7.15.6：副本升级槽预置通用终端升级槽里的卡（上限内）——用户在
            // 通用终端上装的磁卡/连接器卡随之在附属终端菜单里显示生效；磁卡设置也带上
            // （磁卡系统读的是 WUT 上的 WT_MAGNET_SETTINGS，打开菜单本身不改它）。
            var wutUpgrades = ((WtUniversalTerminalItem) stack.getItem()).getUpgrades(stack);
            var copyUpgrades = ((appeng.api.upgrades.IUpgradeableItem) terminal).getUpgrades(copy);
            for (int i = 0; i < wutUpgrades.size(); i++)
            {
                var card = wutUpgrades.getStackInSlot(i);
                if (!card.isEmpty())
                {
                    copyUpgrades.addItems(card.copyWithCount(1));
                }
            }
            var magnetMode = stack.getOrDefault(com.godofthings.Godofthings.WT_MAGNET_SETTINGS,
                    WtMagnetMode.OFF);
            copy.set(com.godofthings.Godofthings.WT_MAGNET_SETTINGS, magnetMode);
            return appeng.menu.MenuOpener.open(terminal.getMenuType(), player,
                    appeng.menu.locator.MenuLocators.forStack(copy), returningFromSubmenu);
        }
        return false;
    }

    /** 找到玩家身上装有指定终端的槽位定位器（照抄 WUTHandler.findTerminal，含 Curios）。 */
    public static ItemMenuHostLocator findTerminal(Player player, WtTerminalDefinition terminal)
    {
        ItemMenuHostLocator locator = null;

        var cap = player.getCapability(appeng.integration.modules.curios.CuriosIntegration.ITEM_HANDLER);
        if (cap != null)
        {
            for (int i = 0; i < cap.getSlots(); i++)
            {
                var stack = cap.getStackInSlot(i);
                if (!hasTerminal(stack, terminal))
                {
                    continue;
                }
                if (stack.getItem() instanceof WtUniversalTerminalItem)
                {
                    return appeng.menu.locator.MenuLocators.forCurioSlot(i);
                }
                else if (locator == null)
                {
                    locator = appeng.menu.locator.MenuLocators.forCurioSlot(i);
                }
            }
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++)
        {
            var stack = player.getInventory().getItem(i);
            if (!hasTerminal(stack, terminal))
            {
                continue;
            }
            if (stack.getItem() instanceof WtUniversalTerminalItem)
            {
                return appeng.menu.locator.MenuLocators.forInventorySlot(i);
            }
            else if (locator == null)
            {
                locator = appeng.menu.locator.MenuLocators.forInventorySlot(i);
            }
        }
        return locator;
    }

    /** 终端格式化名（照抄 TextConstants.formatTerminalName）。 */
    public static MutableComponent formatTerminalName(String key)
    {
        return Component.translatable(key).withStyle(STYLE_GRAY);
    }
}
