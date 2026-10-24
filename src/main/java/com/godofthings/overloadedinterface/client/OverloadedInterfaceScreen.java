package com.godofthings.overloadedinterface.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.IconButton;

import com.godofthings.infinityprovider.client.InfToggleButton;
import com.godofthings.overloadedinterface.OverloadedInterfaceMenu;

/**
 * 过载接口屏幕（重写版：继承 UpgradeableScreen，自行管理分页与设置按钮）。
 *
 * <ul>
 *   <li>每页 18 个配置槽（两行 × 9），滚轮 / 左工具栏页码按钮翻页；</li>
 *   <li>每个配置槽上方一枚齿轮按钮：普通右键打开「设置数量」，
 *       <b>Shift+右键切换该槽「无限库存」</b>（界面上以绿色 ∞ 标记）；</li>
 *   <li>左侧工具栏：接口模式 / 导出 / 导入 / IO 速度 四个开关。</li>
 * </ul>
 */
public class OverloadedInterfaceScreen extends UpgradeableScreen<OverloadedInterfaceMenu>
{
    private static final int COLS = 9;
    private static final int SLOT_SPACING = 18;
    /** 齿轮按钮两行的 y 坐标（在配置槽行上方紧贴）。 */
    private static final int AMT_ROW1_Y = 35;
    private static final int AMT_ROW2_Y = 95;
    private static final int AMT_START_X = 8;
    private static final int PAGE_INDICATOR_Y = 18;

    private final InfToggleButton modeButton;
    private final InfToggleButton exportButton;
    private final InfToggleButton importButton;
    private final InfToggleButton speedButton;
    private final InfToggleButton pageButton;
    private final List<SetAmountButton> amountButtons = new ArrayList<>();

    private final List<Slot> configSlots;
    private int lastKnownPage = -1;

    public OverloadedInterfaceScreen(OverloadedInterfaceMenu menu, Inventory playerInventory,
            Component title, ScreenStyle style)
    {
        super(menu, playerInventory, title, style);

        this.pageButton = new InfToggleButton(InfToggleButton.ButtonType.WIRELESS_STRATEGY,
                btn -> this.menu.clientNextPage());
        this.pageButton.setTooltipOn(List.of(
                Component.translatable("gui.godofthings.overloaded_interface.page.next")));
        this.pageButton.setTooltipOff(List.of(
                Component.translatable("gui.godofthings.overloaded_interface.page.prev")));
        this.addToLeftToolbar(this.pageButton);

        this.modeButton = new InfToggleButton(InfToggleButton.ButtonType.MODE,
                btn -> menu.clientCycleInterfaceMode());
        this.modeButton.setTooltipOn(List.of(
                Component.translatable("gui.godofthings.overloaded_interface.mode.wireless")));
        this.modeButton.setTooltipOff(List.of(
                Component.translatable("gui.godofthings.overloaded_interface.mode.normal")));
        this.addToLeftToolbar(this.modeButton);

        this.exportButton = new InfToggleButton(InfToggleButton.ButtonType.AUTO_RETURN,
                btn -> menu.clientCycleExportMode());
        this.exportButton.setTooltipAt(0, List.of(
                Component.translatable("gui.godofthings.overloaded_interface.export.off")));
        this.exportButton.setTooltipAt(1, List.of(
                Component.translatable("gui.godofthings.overloaded_interface.export.auto")));
        this.exportButton.setTooltipAt(2, List.of(
                Component.translatable("gui.godofthings.overloaded_interface.export.auto")));
        this.addToLeftToolbar(this.exportButton);

        this.importButton = new InfToggleButton(InfToggleButton.ButtonType.ADAPTIVE_BATCH,
                btn -> menu.clientCycleImportMode());
        this.importButton.setTooltipAt(0, List.of(
                Component.translatable("gui.godofthings.overloaded_interface.import.off")));
        this.importButton.setTooltipAt(1, List.of(
                Component.translatable("gui.godofthings.overloaded_interface.import.auto")));
        this.importButton.setTooltipAt(2, List.of(
                Component.translatable("gui.godofthings.overloaded_interface.import.eject")));
        this.addToLeftToolbar(this.importButton);

        this.speedButton = new InfToggleButton(InfToggleButton.ButtonType.SPEED,
                btn -> menu.clientCycleIOSpeed());
        this.speedButton.setTooltipOn(List.of(
                Component.translatable("gui.godofthings.overloaded_interface.speed.fast")));
        this.speedButton.setTooltipOff(List.of(
                Component.translatable("gui.godofthings.overloaded_interface.speed.normal")));
        this.addToLeftToolbar(this.speedButton);

        this.configSlots = menu.getAllConfigSlots();
        for (int i = 0; i < this.configSlots.size(); i++)
        {
            final int slotIdx = i;
            var button = new SetAmountButton(btn -> {
                if (net.minecraft.client.gui.screens.Screen.hasShiftDown())
                {
                    menu.clientToggleUnlimited(slotIdx);
                }
                else
                {
                    menu.openSetAmountMenu(slotIdx);
                }
            });
            button.setDisableBackground(true);
            button.setMessage(Component.translatable(
                    "gui.godofthings.overloaded_interface.set_amount"));
            button.setTooltip(Tooltip.create(Component.translatable(
                    "gui.godofthings.overloaded_interface.set_amount.tooltip")));
            this.amountButtons.add(button);
        }
    }

    @Override
    protected void init()
    {
        super.init();
        for (var button : this.amountButtons)
        {
            this.addRenderableWidget(button);
        }
    }

    @Override
    protected void updateBeforeRender()
    {
        super.updateBeforeRender();
        var menu = this.getMenu();
        this.modeButton.setStateIndex(menu.isWirelessMode() ? 1 : 0);
        this.exportButton.setStateIndex(menu.getExportModeOrdinal());
        this.importButton.setStateIndex(menu.getImportModeOrdinal());
        this.speedButton.setStateIndex(menu.getIOSpeedOrdinal());

        // 页码变化时由服务端重排激活槽位
        if (menu.currentPage != this.lastKnownPage)
        {
            this.lastKnownPage = menu.currentPage;
        }

        int page = menu.currentPage;
        int start = page * OverloadedInterfaceMenu.SLOTS_PER_PAGE;
        int end = Math.min(start + OverloadedInterfaceMenu.SLOTS_PER_PAGE,
                this.configSlots.size());

        for (int i = 0; i < this.amountButtons.size(); i++)
        {
            var button = this.amountButtons.get(i);
            if (i >= start && i < end)
            {
                int inPage = i - start;
                int col = inPage % COLS;
                int row = inPage / COLS;
                button.setPosition(
                        this.leftPos + AMT_START_X + col * SLOT_SPACING,
                        this.topPos + (row == 0 ? AMT_ROW1_Y : AMT_ROW2_Y));
                button.visible = !this.configSlots.get(i).getItem().isEmpty();
            }
            else
            {
                button.visible = false;
            }
        }
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY)
    {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);

        var menu = this.getMenu();
        if (menu.totalPages > 1)
        {
            String pageText = (menu.currentPage + 1) + "/" + menu.totalPages;
            int textWidth = this.font.width(pageText);
            guiGraphics.drawString(this.font, pageText,
                    (this.imageWidth - textWidth) / 2, PAGE_INDICATOR_Y,
                    this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB(), false);
        }
        // 无线模式下标题右侧显示连接数
        if (menu.isWirelessMode())
        {
            var text = Component.translatable(
                    "gui.godofthings.overloaded_interface.connections",
                    menu.getConnectionCount());
            guiGraphics.drawString(this.font, text,
                    this.imageWidth - 8 - this.font.width(text), 6,
                    this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB(), false);
        }
        // 无限库存槽画绿色 ∞ 标记
        int page = menu.currentPage;
        int start = page * OverloadedInterfaceMenu.SLOTS_PER_PAGE;
        int end = Math.min(start + OverloadedInterfaceMenu.SLOTS_PER_PAGE,
                this.configSlots.size());
        for (int i = start; i < end; i++)
        {
            if (menu.isSlotUnlimited(i))
            {
                var slot = this.configSlots.get(i);
                if (!slot.getItem().isEmpty())
                {
                    guiGraphics.drawString(this.font, "\u221E",
                            slot.x + 10, slot.y - 10, 0xFF00FF00, true);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (this.menu.totalPages > 1)
        {
            if (scrollY > 0.0)
            {
                this.menu.clientPrevPage();
            }
            else if (scrollY < 0.0)
            {
                this.menu.clientNextPage();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /** 设置数量 / 无限切换按钮（齿轮图标，悬停高亮——照抄上游 SetAmountButton）。 */
    private static class SetAmountButton extends IconButton
    {
        SetAmountButton(OnPress onPress)
        {
            super(onPress);
        }

        @Override
        protected Icon getIcon()
        {
            return this.isHoveredOrFocused() ? Icon.COG : Icon.COG_DISABLED;
        }
    }
}
