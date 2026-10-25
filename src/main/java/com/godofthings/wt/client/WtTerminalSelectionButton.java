package com.godofthings.wt.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import appeng.client.gui.style.BackgroundGenerator;

import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtTerminalDefinition;
import com.godofthings.wt.network.WtNetwork;

/**
 * 终端切换选择按钮（照抄 ae2wtlib 的 {@code TerminalSelectionButton} 全文，[本地改动]
 * 选中动作发本模组的 {@code WtNetwork.SelectTerminal} 包；图标走本模组 {@link WtIcon}）。
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
        if (!alwaysVisible() && !terminals.isEmpty())
            menuOpen = !menuOpen;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        boolean alwaysVisible = alwaysVisible();
        if ((menuOpen || alwaysVisible) && button == 0)
        {
            WtTerminalDefinition terminal = terminalAt(mouseX, mouseY);
            if (terminal != null)
            {
                if (!alwaysVisible)
                    menuOpen = false;
                storeState.run();
                WtNetwork.sendSelectTerminal(terminal.terminalName());
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
        return com.godofthings.wt.WtConfig.alwaysShowTerminalSelector() && !terminals.isEmpty();
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

        BackgroundGenerator.draw(width, height, guiGraphics, panelX, panelY);

        for (int i = 0; i < terminals.size(); i++)
        {
            WtTerminalDefinition terminal = terminals.get(i);
            int x = buttonX(i);
            int y = buttonY(i);
            boolean selected = terminal.equals(currentTerminal);
            boolean hovered = mouseX >= x && mouseY >= y && mouseX < x + BUTTON_WIDTH
                    && mouseY < y + BUTTON_HEIGHT;
            int yOffset = hovered ? 1 : 0;
            WtIcon background = hovered ? ae2ToolbarBackgroundHovered()
                    : selected ? ae2ToolbarBackgroundFocused() : ae2ToolbarBackground();

            background.getBlitter().dest(x, y + yOffset, background.width(), background.height()).zOffset(2)
                    .blit(guiGraphics);
            iconOf(terminal).getBlitter().dest(x + ICON_OFFSET, y + ICON_OFFSET + yOffset).zOffset(3)
                    .blit(guiGraphics);

            if (hovered)
                tooltip = List.of(terminal.formattedName());
        }
    }

    private WtTerminalDefinition terminalAt(double mouseX, double mouseY)
    {
        for (int i = 0; i < terminals.size(); i++)
        {
            int x = buttonX(i);
            int y = buttonY(i);
            if (mouseX >= x && mouseY >= y && mouseX < x + BUTTON_WIDTH && mouseY < y + BUTTON_HEIGHT)
                return terminals.get(i);
        }
        return null;
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
        return Math.max(1, (terminals.size() + rows() - 1) / rows());
    }

    private int rows()
    {
        return Math.max(1, Math.min(MAX_ROWS, terminals.size()));
    }

    private int lastColumnYOffset(int index)
    {
        int entries = terminals.size() % rows();
        if (entries == 0 || index / rows() != columns() - 1)
            return 0;
        return (rows() - entries) * (BUTTON_HEIGHT + GAP) / 2;
    }
}
