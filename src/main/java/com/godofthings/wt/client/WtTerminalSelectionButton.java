package com.godofthings.wt.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.renderer.Rect2i;

import appeng.client.gui.style.BackgroundGenerator;

import com.godofthings.wt.core.WtExtraTerminals;
import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtTerminalDefinition;
import com.godofthings.wt.network.WtNetwork;

/**
 * 终端切换选择按钮（照抄 ae2wtlib 的 {@code TerminalSelectionButton} 全文，[本地改动]
 * 选中动作发本模组的 {@code WtNetwork.SelectTerminal} 包；图标走本模组 {@link WtIcon}；
 * v7.15.6：附属终端也进选择面板，点击发 {@code WtNetwork.SelectExtra}）。
 */
final class WtTerminalSelectionButton extends WtIconButton
{
    private static final int BUTTON_WIDTH = 18;
    private static final int BUTTON_HEIGHT = 20;
    private static final int MAX_ROWS = 3;
    private static final int GAP = 2;
    private static final int PANEL_GAP = 5;
    private static final int ALWAYS_VISIBLE_PANEL_X_OFFSET = -15;
    private static final int ALWAYS_VISIBLE_PANEL_Y_OFFSET = 3;
    private static final int PANEL_PADDING = 3;
    private static final int ICON_OFFSET = 1;
    private static final int TOOLBAR_BUTTON_SPACING = 6;

    private final WtMenuHost host;
    private final List<WtTerminalDefinition> terminals;
    /** [本地改动] v7.15.6：已合并的附属终端（面板里排在内建终端之后）。 */
    private final List<ItemStack> extras;
    private final Runnable storeState;
    private boolean menuOpen;
    private List<Component> tooltip = List.of();

    WtTerminalSelectionButton(WtMenuHost host, Runnable storeState)
    {
        super(btn ->
        {
        }, WtIcon.CRAFTING, ae2ToolbarBackground(), ae2ToolbarBackgroundHovered(),
                ae2ToolbarBackgroundFocused());
        this.host = host;
        this.storeState = storeState;
        this.terminals = installedTerminals(host);
        this.extras = WtExtraTerminals.list(host.getItemStack());
    }

    private static WtIcon ae2ToolbarBackground()
    {
        return new WtIcon(176, 128, 18, 20, new WtIcon.Texture(appeng.client.gui.Icon.TEXTURE,
                appeng.client.gui.Icon.TEXTURE_WIDTH, appeng.client.gui.Icon.TEXTURE_HEIGHT));
    }

    private static WtIcon ae2ToolbarBackgroundHovered()
    {
        return new WtIcon(212, 128, 18, 19, new WtIcon.Texture(appeng.client.gui.Icon.TEXTURE,
                appeng.client.gui.Icon.TEXTURE_WIDTH, appeng.client.gui.Icon.TEXTURE_HEIGHT));
    }

    private static WtIcon ae2ToolbarBackgroundFocused()
    {
        return new WtIcon(194, 128, 18, 20, new WtIcon.Texture(appeng.client.gui.Icon.TEXTURE,
                appeng.client.gui.Icon.TEXTURE_WIDTH, appeng.client.gui.Icon.TEXTURE_HEIGHT));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial)
    {
        if (!visible)
            return;

        boolean alwaysVisible = alwaysVisible();
        tooltip = List.of();
        if (!alwaysVisible)
        {
            super.renderWidget(guiGraphics, mouseX, mouseY, partial);
            if (isHovered())
                tooltip = currentTooltip();
        }

        if (menuOpen || alwaysVisible)
            renderMenu(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected WtIcon getIcon()
    {
        WtTerminalDefinition terminal = WtTerminalDefinition.ofOrNull(host.getItemStack());
        return terminal == null ? WtIcon.CRAFTING : iconOf(terminal);
    }

    private static WtIcon iconOf(WtTerminalDefinition terminal)
    {
        return switch (terminal.terminalName())
        {
            case "pattern_access" -> WtIcon.PATTERN_ACCESS;
            case "pattern_encoding" -> WtIcon.PATTERN_ENCODING;
            default -> WtIcon.CRAFTING;
        };
    }

    @Override
    public void onPress()
    {
        if (!alwaysVisible() && entryCount() > 0)
            menuOpen = !menuOpen;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        boolean alwaysVisible = alwaysVisible();
        if ((menuOpen || alwaysVisible) && button == 0)
        {
            int entry = entryAt(mouseX, mouseY);
            if (entry >= 0)
            {
                if (!alwaysVisible)
                    menuOpen = false;
                storeState.run();
                if (entry < terminals.size())
                {
                    WtNetwork.sendSelectTerminal(terminals.get(entry).terminalName());
                }
                else
                {
                    // [本地改动] v7.15.6：附属终端按 EXTRA_TERMINALS 下标选择
                    PacketDistributor.sendToServer(
                            new WtNetwork.SelectExtra(entry - terminals.size()));
                }
                return true;
            }
        }

        if (alwaysVisible)
            return false;

        if (super.mouseClicked(mouseX, mouseY, button))
            return true;

        if (menuOpen)
        {
            menuOpen = false;
        }
        return false;
    }

    @Override
    public int getWidth()
    {
        return alwaysVisible() ? 0 : super.getWidth();
    }

    @Override
    public int getHeight()
    {
        return alwaysVisible() ? -TOOLBAR_BUTTON_SPACING : super.getHeight();
    }

    @Override
    public List<Component> getTooltipMessage()
    {
        return tooltip;
    }

    @Override
    public Rect2i getTooltipArea()
    {
        if (!menuOpen && !alwaysVisible())
            return super.getTooltipArea();

        int x = panelX() - 1;
        int y = panelY() - 1;
        int right = Math.max(getX() + getWidth(), panelX() + panelWidth() + 1);
        int bottom = Math.max(getY() + getHeight(), panelY() + panelHeight() + 1);
        return new Rect2i(x, y, right - x, bottom - y);
    }

    private static List<WtTerminalDefinition> installedTerminals(WtMenuHost host)
    {
        List<WtTerminalDefinition> terminals = new ArrayList<>();
        for (var terminal : WtTerminalDefinition.wirelessTerminalList())
        {
            if (WtTerminalDefinition.hasTerminal(host.getItemStack(), terminal))
                terminals.add(terminal);
        }
        return terminals;
    }

    private boolean alwaysVisible()
    {
        return com.godofthings.wt.WtConfig.alwaysShowTerminalSelector() && entryCount() > 0;
    }

    /** 面板总条目数 = 内建终端 + 附属终端。 */
    private int entryCount()
    {
        return terminals.size() + extras.size();
    }

    private List<Component> currentTooltip()
    {
        WtTerminalDefinition terminal = WtTerminalDefinition.ofOrNull(host.getItemStack());
        if (terminal == null)
            return List.of(Component.translatable("msg.godofthings.wut.empty"));
        return List.of(Component.translatable("gui.godofthings.wut.current_terminal",
                Component.translatable(terminal.translationKey())));
    }

    private void renderMenu(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        int panelX = panelX();
        int panelY = panelY();
        int width = panelWidth();
        int height = panelHeight();
        WtTerminalDefinition currentTerminal = WtTerminalDefinition.ofOrNull(host.getItemStack());
        int extraCurrent = WtExtraTerminals.current(host.getItemStack());

        BackgroundGenerator.draw(width, height, guiGraphics, panelX, panelY);

        for (int i = 0; i < terminals.size(); i++)
        {
            WtTerminalDefinition terminal = terminals.get(i);
            int x = buttonX(i);
            int y = buttonY(i);
            boolean selected = terminal.equals(currentTerminal);
            renderEntry(guiGraphics, mouseX, mouseY, x, y, selected,
                    List.of(terminal.formattedName()), iconOf(terminal), null);
        }
        // [本地改动] v7.15.6：附属终端条目——直接渲染终端物品本身（附属模组各有贴图），
        // tooltip 用它的显示名
        for (int j = 0; j < extras.size(); j++)
        {
            ItemStack extra = extras.get(j);
            int i = terminals.size() + j;
            int x = buttonX(i);
            int y = buttonY(i);
            boolean selected = extraCurrent == j
                    && currentTerminal == null;
            Component name = WtExtraTerminals.displayName(extra);
            renderEntry(guiGraphics, mouseX, mouseY, x, y, selected,
                    List.of(name instanceof MutableComponent m ? m : name.copy()),
                    null, extra);
        }
    }

    /** 渲染单个面板条目（icon 与 itemStack 二选一）。 */
    private void renderEntry(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y,
            boolean selected, List<Component> entryTooltip, WtIcon icon, ItemStack itemStack)
    {
        boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + BUTTON_WIDTH
                && mouseY < y + BUTTON_HEIGHT;
        int yOffset = hovered ? 1 : 0;
        WtIcon background = hovered ? ae2ToolbarBackgroundHovered()
                : selected ? ae2ToolbarBackgroundFocused() : ae2ToolbarBackground();

        background.getBlitter().dest(x, y + yOffset, background.width(), background.height()).zOffset(2)
                .blit(guiGraphics);
        if (icon != null)
        {
            icon.getBlitter().dest(x + ICON_OFFSET, y + ICON_OFFSET + yOffset).zOffset(3)
                    .blit(guiGraphics);
        }
        else if (itemStack != null)
        {
            guiGraphics.renderItem(itemStack, x + ICON_OFFSET, y + ICON_OFFSET + yOffset, 0);
        }

        if (hovered)
            tooltip = entryTooltip;
    }

    /** 命中的面板条目（0..内建数+附属数-1），无命中返回 -1。 */
    private int entryAt(double mouseX, double mouseY)
    {
        int count = entryCount();
        for (int i = 0; i < count; i++)
        {
            int x = buttonX(i);
            int y = buttonY(i);
            if (mouseX >= x && mouseY >= y && mouseX < x + BUTTON_WIDTH && mouseY < y + BUTTON_HEIGHT)
                return i;
        }
        return -1;
    }

    private int panelX()
    {
        int x = getX();
        int gap = PANEL_GAP;
        if (alwaysVisible())
        {
            x -= super.getWidth();
            gap = ALWAYS_VISIBLE_PANEL_X_OFFSET;
        }
        return x - gap - panelWidth();
    }

    private int panelY()
    {
        return getY() + (alwaysVisible() ? ALWAYS_VISIBLE_PANEL_Y_OFFSET : 0);
    }

    private int panelWidth()
    {
        return PANEL_PADDING * 2 + columns() * BUTTON_WIDTH + Math.max(0, columns() - 1) * GAP;
    }

    private int panelHeight()
    {
        return PANEL_PADDING * 2 + 2 + rows() * BUTTON_HEIGHT + Math.max(0, rows() - 1) * GAP;
    }

    private int buttonX(int index)
    {
        return panelX() + PANEL_PADDING + (index / rows()) * (BUTTON_WIDTH + GAP);
    }

    private int buttonY(int index)
    {
        return panelY() + PANEL_PADDING + (index % rows()) * (BUTTON_HEIGHT + GAP) + lastColumnYOffset(index);
    }

    private int columns()
    {
        return Math.max(1, (entryCount() + rows() - 1) / rows());
    }

    private int rows()
    {
        return Math.max(1, Math.min(MAX_ROWS, entryCount()));
    }

    private int lastColumnYOffset(int index)
    {
        int entries = entryCount() % rows();
        if (entries == 0 || index / rows() != columns() - 1)
            return 0;
        return (rows() - entries) * (BUTTON_HEIGHT + GAP) / 2;
    }
}
