package com.godofthings.wt;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;
import net.minecraft.world.inventory.MenuType;

import appeng.core.definitions.AEItems;
import appeng.menu.locator.ItemMenuHostLocator;

import com.godofthings.Godofthings;
import com.godofthings.wt.core.WtTerminalDefinition;
import com.godofthings.wt.terminals.WtAccessMenu;
import com.godofthings.wt.terminals.WtAccessMenuHost;
import com.godofthings.wt.terminals.WtCraftingMenu;
import com.godofthings.wt.terminals.WtCraftingMenuHost;
import com.godofthings.wt.terminals.WtEncodingMenu;
import com.godofthings.wt.terminals.WtEncodingMenuHost;

/**
 * 无线终端装配入口（照抄上游 AE2wtlib.onAe2Initialized 的注册段，[本地改动]
 * crafting 终端复用 AE2 原版无线合成终端的物品（不 mixin 替换），pattern_encoding /
 * pattern_access 用本模组的独立终端物品；热键动作名带 godofthings_ 前缀避免同装冲突）。
 * 在 commonSetup 的 enqueueWork 里调用——三台终端定义就绪后配方与升级卡注册才安全。
 */
public final class WtRegistration
{
    private WtRegistration()
    {
    }

    private static final Style STYLE_GRAY = Style.EMPTY.withColor(ChatFormatting.GRAY);

    private static WtTerminalDefinition CRAFTING_DEF;
    private static WtTerminalDefinition PATTERN_ENCODING_DEF;
    private static WtTerminalDefinition PATTERN_ACCESS_DEF;

    /** 三台终端定义注册（enqueueWork 内调用一次）。 */
    public static void registerTerminals()
    {
        CRAFTING_DEF = new WtTerminalDefinition("crafting",
                (player, locator, returningFromSubmenu) -> WtRegistration.openMenu(
                        WtCraftingMenu.TYPE, player, locator, returningFromSubmenu),
                (item, player, locator, returnToMainMenu) -> new WtCraftingMenuHost(item, player, locator,
                        returnToMainMenu),
                WtCraftingMenu.TYPE,
                AEItems.WIRELESS_CRAFTING_TERMINAL.get(),
                format("block.ae2.crafting_terminal"),
                "block.ae2.crafting_terminal",
                // 上游 crafting 终端复用 AE2 的无线终端热键名（HotkeyActions.init 注册的名字）
                "wireless_terminal",
                new WtTerminalDefinition.WtComponentKey("crafting"),
                2);

        PATTERN_ENCODING_DEF = new WtTerminalDefinition("pattern_encoding",
                (player, locator, returningFromSubmenu) -> WtRegistration.openMenu(
                        WtEncodingMenu.TYPE, player, locator, returningFromSubmenu),
                (item, player, locator, returnToMainMenu) -> new WtEncodingMenuHost(item, player, locator,
                        returnToMainMenu),
                WtEncodingMenu.TYPE,
                Godofthings.WIRELESS_PATTERN_ENCODING_TERMINAL.get(),
                format("item.godofthings.wireless_pattern_encoding_terminal"),
                "item.godofthings.wireless_pattern_encoding_terminal",
                "godofthings_wireless_pattern_encoding_terminal",
                new WtTerminalDefinition.WtComponentKey("pattern_encoding"),
                2);

        PATTERN_ACCESS_DEF = new WtTerminalDefinition("pattern_access",
                (player, locator, returningFromSubmenu) -> WtRegistration.openMenu(
                        WtAccessMenu.TYPE, player, locator, returningFromSubmenu),
                (item, player, locator, returnToMainMenu) -> new WtAccessMenuHost(item, player, locator,
                        returnToMainMenu),
                WtAccessMenu.TYPE,
                Godofthings.WIRELESS_PATTERN_ACCESS_TERMINAL.get(),
                format("item.godofthings.wireless_pattern_access_terminal"),
                "item.godofthings.wireless_pattern_access_terminal",
                "godofthings_wireless_pattern_access_terminal",
                new WtTerminalDefinition.WtComponentKey("pattern_access"),
                2);

        WtTerminalDefinition.add(CRAFTING_DEF);
        WtTerminalDefinition.add(PATTERN_ENCODING_DEF);
        WtTerminalDefinition.add(PATTERN_ACCESS_DEF);
    }

    public static WtTerminalDefinition CRAFTING()
    {
        return CRAFTING_DEF;
    }

    public static WtTerminalDefinition PATTERN_ENCODING()
    {
        return PATTERN_ENCODING_DEF;
    }

    public static WtTerminalDefinition PATTERN_ACCESS()
    {
        return PATTERN_ACCESS_DEF;
    }

    /** 打开指定菜单类型（照抄 ItemWT.open 的 MenuOpener 路径）。 */
    public static boolean openMenu(MenuType<?> type, net.minecraft.world.entity.player.Player player,
            ItemMenuHostLocator locator, boolean returningFromSubmenu)
    {
        return appeng.menu.MenuOpener.open(type, player, locator, returningFromSubmenu);
    }

    private static MutableComponent format(String key)
    {
        return Component.translatable(key).withStyle(STYLE_GRAY);
    }
}
