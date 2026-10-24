package com.godofthings.infinityprovider.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.api.config.ActionItems;
import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ActionButton;

import com.godofthings.infinityprovider.InfProviderMenu;

/**
 * 增强供应器主屏幕：在 AE2 供应器屏幕左侧工具栏追加
 * 无线模式 / 回收模式 / 自适应倍增 / 高级设置按钮
 * （移植自 AE2LT 的 OverloadedPatternProviderScreen，裁剪掉频率与阻挡模式替换）。
 */
public class InfProviderEnhancedScreen<M extends InfProviderMenu.EnhancedActions>
        extends PatternProviderScreen<M> {

    private static final List<Component> RETURN_TIP_OFF =
            List.of(Component.translatable("gui.godofthings.infprovider.return.off"));
    private static final List<Component> RETURN_TIP_AUTO =
            List.of(Component.translatable("gui.godofthings.infprovider.return.auto"));
    private static final List<Component> RETURN_TIP_EJECT =
            List.of(Component.translatable("gui.godofthings.infprovider.return.eject"));
    private static final List<Component> ADAPTIVE_TIP_ON =
            List.of(Component.translatable("gui.godofthings.infprovider.adaptive.on"));
    private static final List<Component> ADAPTIVE_TIP_OFF =
            List.of(Component.translatable("gui.godofthings.infprovider.adaptive.off"));

    private final InfToggleButton modeButton;
    private final InfToggleButton autoReturnButton;
    private final InfToggleButton adaptiveBatchButton;
    private final ActionButton advancedSettingsButton;

    public InfProviderEnhancedScreen(M menu, Inventory playerInventory,
            Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.modeButton = new InfToggleButton(InfToggleButton.ButtonType.MODE,
                btn -> menu.clientToggleMode());
        this.modeButton.setTooltipOn(List.of(Component.translatable("gui.godofthings.infprovider.mode.wireless")));
        this.modeButton.setTooltipOff(List.of(Component.translatable("gui.godofthings.infprovider.mode.normal")));
        this.addToLeftToolbar(this.modeButton);

        this.autoReturnButton = new InfToggleButton(InfToggleButton.ButtonType.AUTO_RETURN,
                btn -> menu.clientToggleReturn());
        this.autoReturnButton.setTooltipAt(0, RETURN_TIP_OFF);
        this.autoReturnButton.setTooltipAt(1, RETURN_TIP_AUTO);
        this.autoReturnButton.setTooltipAt(2, RETURN_TIP_EJECT);
        this.addToLeftToolbar(this.autoReturnButton);

        this.adaptiveBatchButton = new InfToggleButton(InfToggleButton.ButtonType.ADAPTIVE_BATCH,
                btn -> menu.clientToggleAdaptive());
        this.adaptiveBatchButton.setTooltipOn(ADAPTIVE_TIP_ON);
        this.adaptiveBatchButton.setTooltipOff(ADAPTIVE_TIP_OFF);
        this.addToLeftToolbar(this.adaptiveBatchButton);

        this.advancedSettingsButton = new ActionButton(ActionItems.COG,
                () -> this.switchToScreen(new InfProviderAdvancedScreen<>(this)));
        this.advancedSettingsButton.setMessage(
                Component.translatable("gui.godofthings.infprovider.advanced.open"));
        this.addToLeftToolbar(this.advancedSettingsButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        var menu = this.getMenu();
        this.modeButton.setStateIndex(menu.isWirelessMode() ? 1 : 0);
        this.autoReturnButton.setStateIndex(menu.getReturnModeOrdinal());
        this.adaptiveBatchButton.setState(menu.isAdaptiveBatch());
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(guiGraphics, offsetX, offsetY, mouseX, mouseY);
        // 无线模式下把连接数画在「物品返回栏」标题那一行的右侧空位。
        // 行高不能硬编码：1 槽版返回栏标签在 top=62，其余三档在 top=116，
        // 直接读当前屏幕 json 里 interface_stored_items 的坐标即可自适应。
        if (this.getMenu().isWirelessMode()) {
            var text = Component.translatable("gui.godofthings.infprovider.connections",
                    this.getMenu().getConnectionCount());
            guiGraphics.drawString(this.font, text,
                    this.imageWidth - 8 - this.font.width(text), this.storedItemsLabelY(),
                    this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB(), false);
        }
    }

    /** 返回栏标题的 y 坐标（各档界面不同，从样式 json 读，缺省回退到 116）。 */
    private int storedItemsLabelY() {
        var label = this.style.getText().get("interface_stored_items");
        if (label != null && label.getPosition() != null && label.getPosition().getTop() != null) {
            return label.getPosition().getTop();
        }
        return 116;
    }
}
