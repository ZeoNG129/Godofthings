package com.godofthings.client.screen;

import com.godofthings.block.entity.GodCraftBlockEntity;
import com.godofthings.menu.GodCraftTemplateMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class GodCraftTemplateScreen extends AbstractContainerScreen<GodCraftTemplateMenu> {
   private static final ResourceLocation TEXTURE = ResourceLocation.tryBuild("godofthings", "textures/gui/god_craft.png");
   private static final int IMG_W = 176;
   private static final int IMG_H = 166;
   private static final int TPL_X0 = 8;
   private static final int TPL_Y0 = 30;
   private static final int TPL_SPACING_X = 22;
   private static final int TPL_SPACING_Y = 22;
   private static final int TPL_SIZE = 18;
   private static final int REC_X0 = 96;
   private static final int REC_Y0 = 30;
   private static final int REC_SPACING = 18;
   private static final int REC_SIZE = 16;
   private static final int RESULT_X = 150;
   private static final int RESULT_Y = 48;
   private static final int RESULT_SIZE = 18;
   private static final int LOAD_X = 96;
   private static final int LOAD_Y = 96;
   private static final int LOAD_W = 64;
   private static final int LOAD_H = 14;
   private static final int BACK_X = 146;
   private static final int BACK_Y = 8;
   private static final int BACK_W = 22;
   private static final int BACK_H = 14;
   private int selectedTemplate = -1;

   public GodCraftTemplateScreen(GodCraftTemplateMenu menu, Inventory playerInventory, Component title) {
      super(menu, playerInventory, title);
      this.imageWidth = 176;
      this.imageHeight = 166;
   }

   protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
      int x = (this.width - this.imageWidth) / 2;
      int y = (this.height - this.imageHeight) / 2;
      gui.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
      gui.drawString(this.font, Component.literal("配方模板（单击预览 / Shift+单击保存）"), x + 8, y + 8, 10133672);
      GodCraftBlockEntity be = ((GodCraftTemplateMenu)this.menu).getBlockEntity();

      for (int t = 0; t < 8; t++) {
         int tx = x + 8 + t % 2 * 22;
         int ty = y + 30 + t / 2 * 22;
         boolean has = be.hasTemplate(t);
         gui.fill(tx, ty, tx + 18, ty + 18, -15329251);
         gui.fill(tx + 1, ty + 1, tx + 18 - 1, ty + 18 - 1, has ? -13934805 : -13947080);
         if (t == this.selectedTemplate) {
            gui.fill(tx - 1, ty - 1, tx + 18 + 1, ty + 18 + 1, -10496);
         }

         ItemStack result = has ? be.getTemplateResult(t) : ItemStack.EMPTY;
         if (!result.isEmpty()) {
            gui.renderItem(result, tx + 1, ty + 1);
         }

         gui.drawString(this.font, Component.literal(String.valueOf(t + 1)), tx + 18 - 8, ty + 18 - 8, 16777215);
      }

      if (this.selectedTemplate >= 0 && be.hasTemplate(this.selectedTemplate)) {
         gui.drawString(this.font, Component.literal("配方"), x + 96, y + 18, 10133672);

         for (int i = 0; i < 9; i++) {
            int rx = x + 96 + i % 3 * 18;
            int ry = y + 30 + i / 3 * 18;
            gui.fill(rx, ry, rx + 16, ry + 16, -15329251);
            gui.fill(rx + 1, ry + 1, rx + 16 - 1, ry + 16 - 1, -13947080);
            ItemStack item = be.getTemplateItem(this.selectedTemplate, i);
            if (!item.isEmpty()) {
               gui.renderItem(item, rx + 1, ry + 1);
            }
         }

         gui.drawString(this.font, Component.literal("产物"), x + 150, y + 30, 10133672);
         ItemStack result = be.getTemplateResult(this.selectedTemplate);
         gui.fill(x + 150, y + 48, x + 150 + 18, y + 48 + 18, -15329251);
         gui.fill(x + 150 + 1, y + 48 + 1, x + 150 + 18 - 1, y + 48 + 18 - 1, -13934805);
         if (!result.isEmpty()) {
            gui.renderItem(result, x + 150 + 1, y + 48 + 1);
         }

         String name = result.isEmpty() ? "无" : result.getHoverName().getString();
         gui.drawString(this.font, Component.literal(name), x + 150, y + 48 + 18 + 1, 13421772);
      } else if (this.selectedTemplate >= 0) {
         gui.drawString(this.font, Component.literal("模板为空"), x + 96, y + 60, 10133672);
      }

      boolean canLoad = this.selectedTemplate >= 0 && be.hasTemplate(this.selectedTemplate);
      this.drawBtn(gui, x + 96, y + 96, 64, 14, Component.literal("加载"), canLoad ? -12615873 : -11908534);
      this.drawBtn(gui, x + 146, y + 8, 22, 14, Component.literal("返"), -12623985);
   }

   private void drawBtn(GuiGraphics gui, int bx, int by, int w, int h, Component label, int color) {
      gui.fill(bx, by, bx + w, by + h, -15329251);
      gui.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, color);
      gui.drawString(this.font, label, bx + 5, by + 3, 16777215);
   }

   protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
      gui.drawString(this.font, Component.translatable("gui.godofthings.inventory"), 8, 74, 4210752, false);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      int relX = (int)mouseX - this.leftPos;
      int relY = (int)mouseY - this.topPos;

      for (int t = 0; t < 8; t++) {
         int tx = 8 + t % 2 * 22;
         int ty = 30 + t / 2 * 22;
         if (relX >= tx && relX < tx + 18 && relY >= ty && relY < ty + 18) {
            if (Screen.hasShiftDown()) {
               this.sendButton(10 + t);
            } else {
               this.selectedTemplate = t;
            }

            return true;
         }
      }

      if (relX >= 96 && relX < 160 && relY >= 96 && relY < 110) {
         if (this.selectedTemplate >= 0) {
            this.sendButton(20 + this.selectedTemplate);
         }

         return true;
      } else if (relX >= 146 && relX < 168 && relY >= 8 && relY < 22) {
         this.sendButton(30);
         return true;
      } else {
         return super.mouseClicked(mouseX, mouseY, button);
      }
   }

   private void sendButton(int buttonId) {
      ClientPacketListener conn = Minecraft.getInstance().getConnection();
      if (conn != null) {
         conn.send(new ServerboundContainerButtonClickPacket(((GodCraftTemplateMenu)this.menu).containerId, buttonId));
      }
   }

   public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(gui);
      super.render(gui, mouseX, mouseY, partialTick);
      this.renderTooltip(gui, mouseX, mouseY);
   }

   public boolean isPauseScreen() {
      return false;
   }
}
