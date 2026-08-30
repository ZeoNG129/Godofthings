package com.godofthings.client.screen;

import com.godofthings.generator.EnergyGenTool;
import com.godofthings.generator.EnergyGeneratorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * FE 能量发电机 GUI。
 * <p>
 * 展示当前发电量、当前电量、下次增长的发电量、增长百分比（含进度条），
 * 并提供无线充电开关、扫描间隔、区块范围、重复传电次数以及六个输电面的独立开关。
 */
@OnlyIn(Dist.CLIENT)
public class EnergyGeneratorScreen extends AbstractContainerScreen<EnergyGeneratorMenu>
{
    private static final ResourceLocation TEXTURE = ResourceLocation.tryBuild("godofthings", "textures/gui/energy_generator_gui.png");
    private static final int TEXT_COLOR = 4210752; // 0x404040 深灰

    private StateButton wirelessButton;
    private StateButton faceDown;
    private StateButton faceUp;
    private StateButton faceNorth;
    private StateButton faceSouth;
    private StateButton faceWest;
    private StateButton faceEast;

    public EnergyGeneratorScreen(EnergyGeneratorMenu menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 310;
        this.inventoryLabelY = 214;
    }

    @Override
    protected void init()
    {
        super.init();
        // 无线充电开关
        this.wirelessButton = new StateButton(this.leftPos + 108, this.topPos + 77, 40, 12, this.menu.isWirelessOn(), Component.empty(), button -> sendButton(EnergyGeneratorMenu.BUTTON_WIRELESS));
        this.addRenderableWidget(this.wirelessButton);
        // 扫描间隔
        this.addRenderableWidget(new MiniButton(this.leftPos + 108, this.topPos + 93, 16, 12, Component.literal("-"), button -> sendButton(EnergyGeneratorMenu.BUTTON_INTERVAL_DOWN)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 132, this.topPos + 93, 16, 12, Component.literal("+"), button -> sendButton(EnergyGeneratorMenu.BUTTON_INTERVAL_UP)));
        // 区块范围
        this.addRenderableWidget(new MiniButton(this.leftPos + 108, this.topPos + 109, 16, 12, Component.literal("-"), button -> sendButton(EnergyGeneratorMenu.BUTTON_RANGE_DOWN)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 132, this.topPos + 109, 16, 12, Component.literal("+"), button -> sendButton(EnergyGeneratorMenu.BUTTON_RANGE_UP)));
        // 重复传电次数
        this.addRenderableWidget(new MiniButton(this.leftPos + 128, this.topPos + 173, 16, 12, Component.literal("-"), button -> sendButton(EnergyGeneratorMenu.BUTTON_REPEAT_DOWN)));
        this.addRenderableWidget(new MiniButton(this.leftPos + 148, this.topPos + 173, 16, 12, Component.literal("+"), button -> sendButton(EnergyGeneratorMenu.BUTTON_REPEAT_UP)));
        // 六个输电面
        this.faceDown = new StateButton(this.leftPos + 17, this.topPos + 136, 44, 12, this.menu.isFaceEnabled(Direction.DOWN), Component.translatable("screen.godofthings.energy_generator.face.down"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_DOWN));
        this.faceUp = new StateButton(this.leftPos + 66, this.topPos + 136, 44, 12, this.menu.isFaceEnabled(Direction.UP), Component.translatable("screen.godofthings.energy_generator.face.up"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_UP));
        this.faceNorth = new StateButton(this.leftPos + 115, this.topPos + 136, 44, 12, this.menu.isFaceEnabled(Direction.NORTH), Component.translatable("screen.godofthings.energy_generator.face.north"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_NORTH));
        this.faceSouth = new StateButton(this.leftPos + 17, this.topPos + 152, 44, 12, this.menu.isFaceEnabled(Direction.SOUTH), Component.translatable("screen.godofthings.energy_generator.face.south"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_SOUTH));
        this.faceWest = new StateButton(this.leftPos + 66, this.topPos + 152, 44, 12, this.menu.isFaceEnabled(Direction.WEST), Component.translatable("screen.godofthings.energy_generator.face.west"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_WEST));
        this.faceEast = new StateButton(this.leftPos + 115, this.topPos + 152, 44, 12, this.menu.isFaceEnabled(Direction.EAST), Component.translatable("screen.godofthings.energy_generator.face.east"), button -> sendButton(EnergyGeneratorMenu.BUTTON_TRANSFER_EAST));
        this.addRenderableWidget(this.faceDown);
        this.addRenderableWidget(this.faceUp);
        this.addRenderableWidget(this.faceNorth);
        this.addRenderableWidget(this.faceSouth);
        this.addRenderableWidget(this.faceWest);
        this.addRenderableWidget(this.faceEast);
    }

    private void sendButton(int id)
    {
        if (this.minecraft != null && this.minecraft.player != null)
        {
            this.minecraft.player.connection.send(new ServerboundContainerButtonClickPacket(this.menu.containerId, id));
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);
        // 增长进度条
        int trackLeft = this.leftPos + 12;
        int trackRight = this.leftPos + 164;
        int trackTop = this.topPos + 60;
        guiGraphics.fill(trackLeft, trackTop, trackRight, trackTop + 4, 0xFF555555);
        int percent = growthPercent();
        if (percent > 0)
        {
            int fill = (trackRight - trackLeft) * percent / 100;
            guiGraphics.fill(trackLeft, trackTop, trackLeft + fill, trackTop + 4, 0xFF00AA00);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        super.renderLabels(guiGraphics, mouseX, mouseY);
        EnergyGeneratorMenu menu = this.menu;
        boolean maxed = menu.getOutput() >= menu.getMax();
        guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_generator.energy", EnergyGenTool.formatLong(menu.getEnergy())), 12, 19, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_generator.output", EnergyGenTool.formatLong(menu.getOutput())), 12, 29, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_generator.next", maxed ? Component.translatable("screen.godofthings.energy_generator.next_max") : Component.literal(EnergyGenTool.formatLong(menu.getNextIncrease()))), 12, 39, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_generator.growth", growthPercent()), 12, 49, TEXT_COLOR, false);
        // 无线充电参数
        guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_generator.wireless"), 12, 79, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_generator.interval"), 12, 95, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_generator.interval_value", menu.getInterval()), 64, 95, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_generator.range"), 12, 111, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.literal(menu.getRange() + "x" + menu.getRange()), 64, 111, TEXT_COLOR, false);
        // 重复传电次数
        guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_generator.repeat"), 12, 175, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_generator.repeat_value", menu.getRepeat()), 56, 175, TEXT_COLOR, false);
        // 加速槽标签
        Item starItem = menu.getStarItem();
        guiGraphics.drawString(this.font, starItem.getDescription(), 28, 195, TEXT_COLOR, false);
        // 充电槽标签
        Component chargeLabel = Component.translatable("screen.godofthings.energy_generator.charge_slot");
        guiGraphics.drawString(this.font, chargeLabel, 150 - this.font.width(chargeLabel), 195, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        boolean wireless = this.menu.isWirelessOn();
        this.wirelessButton.setState(wireless);
        this.wirelessButton.setMessage(Component.translatable(wireless ? "screen.godofthings.energy_generator.wireless_on" : "screen.godofthings.energy_generator.wireless_off"));
        this.faceDown.setState(this.menu.isFaceEnabled(Direction.DOWN));
        this.faceUp.setState(this.menu.isFaceEnabled(Direction.UP));
        this.faceNorth.setState(this.menu.isFaceEnabled(Direction.NORTH));
        this.faceSouth.setState(this.menu.isFaceEnabled(Direction.SOUTH));
        this.faceWest.setState(this.menu.isFaceEnabled(Direction.WEST));
        this.faceEast.setState(this.menu.isFaceEnabled(Direction.EAST));
    }

    private int growthPercent()
    {
        if (this.menu.getOutput() >= this.menu.getMax())
        {
            return 100;
        }
        int second = Math.max(1, this.menu.getSecond());
        double percent = this.menu.getTickCount() / (second * 20.0) * 100.0;
        return (int) Math.max(0, Math.min(100, percent));
    }

    private class StateButton extends SimpleButton
    {
        private boolean state;

        StateButton(int x, int y, int width, int height, boolean initial, Component label, OnPress onPress)
        {
            super(x, y, width, height, label, onPress);
            this.state = initial;
        }

        void setState(boolean state)
        {
            this.state = state;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
        {
            renderButton(guiGraphics, this.state ? 0xFF00AA00 : 0xFFAA0000);
        }
    }

    private class MiniButton extends SimpleButton
    {
        MiniButton(int x, int y, int width, int height, Component label, OnPress onPress)
        {
            super(x, y, width, height, label, onPress);
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
        {
            renderButton(guiGraphics, 0xFF808080);
        }
    }

    private abstract class SimpleButton extends Button
    {
        SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress)
        {
            super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        }

        protected void renderButton(GuiGraphics guiGraphics, int color)
        {
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
            int borderColor = this.isHovered() ? 0xFFFFFF00 : 0xFF000000;
            guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY() + this.getHeight(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight() + 1, borderColor);
            guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
            guiGraphics.fill(this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor);
            guiGraphics.drawCenteredString(EnergyGeneratorScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, 0xFFFFFFFF);
        }
    }
}
