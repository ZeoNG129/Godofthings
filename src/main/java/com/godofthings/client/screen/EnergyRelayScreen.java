package com.godofthings.client.screen;

import com.godofthings.generator.EnergyGenTool;
import com.godofthings.generator.EnergyRelayMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnergyRelayScreen extends AbstractContainerScreen<EnergyRelayMenu> {
   private static final int TEXT_COLOR = 4210752;
   private static final int LEFT_X = 12;
   private static final int ROW_H = 16;
   private static final int ROW_Y0 = 12;
   private static final int VALUE_X = 44;
   private static final int MINUS_X = 62;
   private static final int PLUS_X = 78;
   private static final int MINI_W = 14;
   private static final int MINI_H = 12;
   private static final int SWITCH_X = 56;
   private static final int SWITCH_W = 40;
   private static final int SWITCH_H = 12;
   private static final int FACE_X0 = 104;
   private static final int FACE_X1 = 136;
   private static final int FACE_W = 30;
   private static final int FACE_H = 12;
   private EnergyRelayScreen.StateButton wirelessButton;
   private EnergyRelayScreen.StateButton faceDown;
   private EnergyRelayScreen.StateButton faceUp;
   private EnergyRelayScreen.StateButton faceNorth;
   private EnergyRelayScreen.StateButton faceSouth;
   private EnergyRelayScreen.StateButton faceWest;
   private EnergyRelayScreen.StateButton faceEast;

   public EnergyRelayScreen(EnergyRelayMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 176;
      this.imageHeight = 110;
      this.titleLabelY = 6;
      this.inventoryLabelY = -2000;
   }

   protected void init() {
      super.init();
      this.wirelessButton = new EnergyRelayScreen.StateButton(
         this.leftPos + 56, this.topPos + 12, 40, 12, ((EnergyRelayMenu)this.menu).isWirelessOn(), Component.empty(), button -> this.sendButton(0)
      );
      this.addRenderableWidget(this.wirelessButton);
      this.addRenderableWidget(
         new EnergyRelayScreen.MiniButton(this.leftPos + 62, this.topPos + 12 + 16, 14, 12, Component.literal("-"), button -> this.sendButton(1))
      );
      this.addRenderableWidget(
         new EnergyRelayScreen.MiniButton(this.leftPos + 78, this.topPos + 12 + 16, 14, 12, Component.literal("+"), button -> this.sendButton(2))
      );
      this.addRenderableWidget(
         new EnergyRelayScreen.MiniButton(this.leftPos + 62, this.topPos + 12 + 32, 14, 12, Component.literal("-"), button -> this.sendButton(3))
      );
      this.addRenderableWidget(
         new EnergyRelayScreen.MiniButton(this.leftPos + 78, this.topPos + 12 + 32, 14, 12, Component.literal("+"), button -> this.sendButton(4))
      );
      this.addRenderableWidget(
         new EnergyRelayScreen.MiniButton(this.leftPos + 62, this.topPos + 12 + 48, 14, 12, Component.literal("-"), button -> this.sendButton(5))
      );
      this.addRenderableWidget(
         new EnergyRelayScreen.MiniButton(this.leftPos + 78, this.topPos + 12 + 48, 14, 12, Component.literal("+"), button -> this.sendButton(6))
      );
      this.faceDown = new EnergyRelayScreen.StateButton(
         this.leftPos + 104,
         this.topPos + 12,
         30,
         12,
         ((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.DOWN),
         Component.translatable("screen.godofthings.energy_relay.face.down"),
         button -> this.sendButton(7)
      );
      this.faceUp = new EnergyRelayScreen.StateButton(
         this.leftPos + 136,
         this.topPos + 12,
         30,
         12,
         ((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.UP),
         Component.translatable("screen.godofthings.energy_relay.face.up"),
         button -> this.sendButton(8)
      );
      this.faceNorth = new EnergyRelayScreen.StateButton(
         this.leftPos + 104,
         this.topPos + 12 + 16,
         30,
         12,
         ((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.NORTH),
         Component.translatable("screen.godofthings.energy_relay.face.north"),
         button -> this.sendButton(9)
      );
      this.faceSouth = new EnergyRelayScreen.StateButton(
         this.leftPos + 136,
         this.topPos + 12 + 16,
         30,
         12,
         ((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.SOUTH),
         Component.translatable("screen.godofthings.energy_relay.face.south"),
         button -> this.sendButton(10)
      );
      this.faceWest = new EnergyRelayScreen.StateButton(
         this.leftPos + 104,
         this.topPos + 12 + 32,
         30,
         12,
         ((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.WEST),
         Component.translatable("screen.godofthings.energy_relay.face.west"),
         button -> this.sendButton(11)
      );
      this.faceEast = new EnergyRelayScreen.StateButton(
         this.leftPos + 136,
         this.topPos + 12 + 32,
         30,
         12,
         ((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.EAST),
         Component.translatable("screen.godofthings.energy_relay.face.east"),
         button -> this.sendButton(12)
      );
      this.addRenderableWidget(this.faceDown);
      this.addRenderableWidget(this.faceUp);
      this.addRenderableWidget(this.faceNorth);
      this.addRenderableWidget(this.faceSouth);
      this.addRenderableWidget(this.faceWest);
      this.addRenderableWidget(this.faceEast);
   }

   private void sendButton(int id) {
      if (this.minecraft != null && this.minecraft.player != null) {
         this.minecraft.player.connection.send(new ServerboundContainerButtonClickPacket(((EnergyRelayMenu)this.menu).containerId, id));
      }
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      guiGraphics.fill(this.leftPos - 1, this.topPos - 1, this.leftPos + this.imageWidth + 1, this.topPos + this.imageHeight + 1, -16777216);
      guiGraphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, -15329251);
   }

   protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
      super.renderLabels(guiGraphics, mouseX, mouseY);
      EnergyRelayMenu menu = (EnergyRelayMenu)this.menu;
      guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_relay.wireless"), 12, 14, 4210752, false);
      guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_relay.interval"), 12, 30, 4210752, false);
      guiGraphics.drawString(
         this.font, Component.translatable("screen.godofthings.energy_relay.interval_value", new Object[]{menu.getInterval()}), 44, 30, 4210752, false
      );
      guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_relay.range"), 12, 46, 4210752, false);
      guiGraphics.drawString(this.font, Component.literal(menu.getRange() + "x" + menu.getRange()), 44, 46, 4210752, false);
      guiGraphics.drawString(this.font, Component.translatable("screen.godofthings.energy_relay.repeat"), 12, 62, 4210752, false);
      guiGraphics.drawString(
         this.font, Component.translatable("screen.godofthings.energy_relay.repeat_value", new Object[]{menu.getRepeat()}), 44, 62, 4210752, false
      );
      guiGraphics.drawString(
         this.font,
         Component.translatable(
            "screen.godofthings.energy_relay.energy", new Object[]{EnergyGenTool.formatLong(menu.getEnergy()), EnergyGenTool.formatLong(menu.getMax())}
         ),
         12,
         96,
         4210752,
         false
      );
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      super.renderTooltip(guiGraphics, mouseX, mouseY);
      boolean wireless = ((EnergyRelayMenu)this.menu).isWirelessOn();
      this.wirelessButton.setState(wireless);
      this.wirelessButton
         .setMessage(Component.translatable(wireless ? "screen.godofthings.energy_relay.wireless_on" : "screen.godofthings.energy_relay.wireless_off"));
      this.faceDown.setState(((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.DOWN));
      this.faceUp.setState(((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.UP));
      this.faceNorth.setState(((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.NORTH));
      this.faceSouth.setState(((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.SOUTH));
      this.faceWest.setState(((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.WEST));
      this.faceEast.setState(((EnergyRelayMenu)this.menu).isFaceEnabled(Direction.EAST));
   }

   public boolean isPauseScreen() {
      return false;
   }

   private class MiniButton extends EnergyRelayScreen.SimpleButton {
      MiniButton(int x, int y, int width, int height, Component label, OnPress onPress) {
         super(x, y, width, height, label, onPress);
      }

      protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         this.renderButton(guiGraphics, -8355712);
      }
   }

   private abstract class SimpleButton extends Button {
      SimpleButton(int x, int y, int width, int height, Component label, OnPress onPress) {
         super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
      }

      protected void renderButton(GuiGraphics guiGraphics, int color) {
         guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);
         int borderColor = this.isHovered() ? -256 : -16777216;
         guiGraphics.fill(this.getX() - 1, this.getY() - 1, this.getX() + this.getWidth() + 1, this.getY(), borderColor);
         guiGraphics.fill(
            this.getX() - 1,
            this.getY() + this.getHeight(),
            this.getX() + this.getWidth() + 1,
            this.getY() + this.getHeight() + 1,
            borderColor
         );
         guiGraphics.fill(this.getX() - 1, this.getY(), this.getX(), this.getY() + this.getHeight(), borderColor);
         guiGraphics.fill(
            this.getX() + this.getWidth(), this.getY(), this.getX() + this.getWidth() + 1, this.getY() + this.getHeight(), borderColor
         );
         guiGraphics.drawCenteredString(
            EnergyRelayScreen.this.font, this.getMessage(), this.getX() + this.getWidth() / 2, this.getY() + (this.getHeight() - 8) / 2, -1
         );
      }
   }

   private class StateButton extends EnergyRelayScreen.SimpleButton {
      private boolean state;

      StateButton(int x, int y, int width, int height, boolean initial, Component label, OnPress onPress) {
         super(x, y, width, height, label, onPress);
         this.state = initial;
      }

      void setState(boolean state) {
         this.state = state;
      }

      protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         this.renderButton(guiGraphics, this.state ? -16733696 : -5636096);
      }
   }
}
