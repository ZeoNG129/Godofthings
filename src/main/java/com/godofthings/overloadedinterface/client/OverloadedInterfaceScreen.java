package com.godofthings.overloadedinterface.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.implementations.InterfaceScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;

import com.godofthings.infinityprovider.client.InfToggleButton;
import com.godofthings.overloadedinterface.OverloadedInterfaceMenu;

/**
 * 过载接口屏幕（移植自 AE2LT 的 OverloadedInterfaceScreen，裁剪掉频率按钮、
 * 模糊模式按钮、页码翻页与逐槽「设置数量」按钮）。
 *
 * <p>沿用 AE2 原生 {@code InterfaceScreen} 的 9 槽布局，左侧工具栏追加
 * 四个模式开关：接口模式（普通/无线）、导出（关/自动）、导入（关/自动/弹出）、
 * IO 速度（普通/快速）。</p>
 */
public class OverloadedInterfaceScreen extends InterfaceScreen<OverloadedInterfaceMenu>
{
    private static final List<Component> MODE_TIP_NORMAL =
            List.of(Component.translatable("gui.godofthings.overloaded_interface.mode.normal"));
    private static final List<Component> MODE_TIP_WIRELESS =
            List.of(Component.translatable("gui.godofthings.overloaded_interface.mode.wireless"));
    private static final List<Component> EXPORT_TIP_OFF =
            List.of(Component.translatable("gui.godofthings.overloaded_interface.export.off"));
    private static final List<Component> EXPORT_TIP_AUTO =
            List.of(Component.translatable("gui.godofthings.overloaded_interface.export.auto"));
    private static final List<Component> IMPORT_TIP_OFF =
            List.of(Component.translatable("gui.godofthings.overloaded_interface.import.off"));
    private static final List<Component> IMPORT_TIP_AUTO =
            List.of(Component.translatable("gui.godofthings.overloaded_interface.import.auto"));
    private static final List<Component> IMPORT_TIP_EJECT =
            List.of(Component.translatable("gui.godofthings.overloaded_interface.import.eject"));
    private static final List<Component> SPEED_TIP_NORMAL =
            List.of(Component.translatable("gui.godofthings.overloaded_interface.speed.normal"));
    private static final List<Component> SPEED_TIP_FAST =
            List.of(Component.translatable("gui.godofthings.overloaded_interface.speed.fast"));

    private final InfToggleButton modeButton;
    private final InfToggleButton exportButton;
    private final InfToggleButton importButton;
    private final InfToggleButton speedButton;

    public OverloadedInterfaceScreen(OverloadedInterfaceMenu menu, Inventory playerInventory,
            Component title, ScreenStyle style)
    {
        super(menu, playerInventory, title, style);

        this.modeButton = new InfToggleButton(InfToggleButton.ButtonType.MODE,
                btn -> menu.clientCycleInterfaceMode());
        this.modeButton.setTooltipOn(MODE_TIP_WIRELESS);
        this.modeButton.setTooltipOff(MODE_TIP_NORMAL);
        this.addToLeftToolbar(this.modeButton);

        this.exportButton = new InfToggleButton(InfToggleButton.ButtonType.AUTO_RETURN,
                btn -> menu.clientCycleExportMode());
        this.exportButton.setTooltipAt(0, EXPORT_TIP_OFF);
        this.exportButton.setTooltipAt(1, EXPORT_TIP_AUTO);
        this.exportButton.setTooltipAt(2, EXPORT_TIP_AUTO);
        this.addToLeftToolbar(this.exportButton);

        this.importButton = new InfToggleButton(InfToggleButton.ButtonType.ADAPTIVE_BATCH,
                btn -> menu.clientCycleImportMode());
        this.importButton.setTooltipAt(0, IMPORT_TIP_OFF);
        this.importButton.setTooltipAt(1, IMPORT_TIP_AUTO);
        this.importButton.setTooltipAt(2, IMPORT_TIP_EJECT);
        this.addToLeftToolbar(this.importButton);

        this.speedButton = new InfToggleButton(InfToggleButton.ButtonType.SPEED,
                btn -> menu.clientCycleIOSpeed());
        this.speedButton.setTooltipOn(SPEED_TIP_FAST);
        this.speedButton.setTooltipOff(SPEED_TIP_NORMAL);
        this.addToLeftToolbar(this.speedButton);
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
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY)
    {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        // 无线模式下显示连接数（放在标题行右侧的空白处）
        if (this.getMenu().isWirelessMode())
        {
            var text = Component.translatable(
                    "gui.godofthings.overloaded_interface.connections",
                    this.getMenu().getConnectionCount());
            guiGraphics.drawString(this.font, text,
                    this.imageWidth - 8 - this.font.width(text), 6,
                    this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB(), false);
        }
    }
}
