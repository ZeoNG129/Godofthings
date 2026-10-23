package com.godofthings.doll.client;

import com.godofthings.doll.dialogue.TwoIStoryScript;
import com.godofthings.doll.client.DollScreenStyle;
import com.godofthings.doll.network.DollNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TwoIStoryLibraryScreen extends Screen implements TwoIInteractionScreen {
   private static int nextRequestId;
   private final Screen parent;
   private final int bond;
   private final int interactionEntityId;
   private int requestId;
   private int completedMask;
   private int waitingTicks;
   private boolean requested;
   private boolean loaded;
   private boolean failed;
   private int panelX;
   private int panelY;
   private int panelWidth;
   private int panelHeight;
   private Button retry;
   private final Button[] chapters = new Button[6];

   public TwoIStoryLibraryScreen(Screen parent, int bond) {
      super(ui("library"));
      this.parent = parent;
      this.interactionEntityId = TwoIInteractionScreen.entityFor(parent);
      this.bond = bond;
   }

   private static Component ui(String key) {
      return Component.translatable("gui.godofthings.two_i.vn." + key);
   }

   protected void init() {
      this.panelWidth = Math.min(560, this.width - 16);
      this.panelHeight = Math.min(270, this.height - 16);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - this.panelHeight) / 2;
      int half = (this.panelWidth - 34) / 2;
      int rowsY = this.panelY + 70;

      for (int i = 0; i < this.chapters.length; i++) {
         int chapter = i;
         this.chapters[i] = (Button)this.addRenderableWidget(Button.builder(TwoIStoryScript.title(i), b -> {
            if (this.loaded && (this.completedMask & 1 << chapter) != 0) {
               this.minecraft.setScreen(TwoIStoryScreen.replay(this, chapter, this.bond));
            }
         }).bounds(this.panelX + 12 + i % 2 * (half + 10), rowsY + i / 2 * 31, half, 25).build());
      }

      int bottom = this.panelY + this.panelHeight - 34;
      this.addRenderableWidget(Button.builder(ui("return"), b -> this.onClose()).bounds(this.panelX + 22 + half, bottom, half, 22).build());
      this.retry = (Button)this.addRenderableWidget(
         Button.builder(ui("library_retry"), b -> this.requestLibrary()).bounds(this.panelX + this.panelWidth - 92, this.panelY + 12, 80, 20).build()
      );
      this.updateButtons();
      if (!this.requested) {
         this.requestLibrary();
      }
   }

   private void requestLibrary() {
      nextRequestId = nextRequestId == Integer.MAX_VALUE ? 1 : nextRequestId + 1;
      this.requestId = nextRequestId;
      this.requested = true;
      this.loaded = false;
      this.failed = false;
      this.waitingTicks = 0;
      this.updateButtons();
      DollNetwork.requestStoryLibrary(this.requestId);
   }

   public void acceptLibrary(int requestId, int completedMask) {
      if (requestId == this.requestId) {
         this.completedMask = completedMask & 63;
         this.loaded = true;
         this.failed = false;
         this.updateButtons();
      }
   }

   private void updateButtons() {
      for (int i = 0; i < this.chapters.length; i++) {
         if (this.chapters[i] != null) {
            boolean unlocked = this.loaded && (this.completedMask & 1 << i) != 0;
            this.chapters[i].active = unlocked;
            this.chapters[i].setTooltip(Tooltip.create(ui(unlocked ? "replay_hint" : "chapter_locked")));
         }
      }

      if (this.retry != null) {
         this.retry.visible = this.failed;
      }
   }

   public void tick() {
      if (!this.loaded && !this.failed && ++this.waitingTicks >= 200) {
         this.failed = true;
         this.updateButtons();
      }
   }

   public void renderBackground(GuiGraphics g, int x, int y, float partial) {
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      g.fill(0, 0, this.width, this.height, -1206312903);
      DollScreenStyle.drawPanel(g, this.panelX, this.panelY, this.panelWidth, this.panelHeight);
      g.drawString(this.font, this.title, this.panelX + 12, this.panelY + 15, -12501164, false);
      g.drawWordWrap(
         this.font,
         ui(this.failed ? "library_failed" : (!this.loaded ? "library_loading" : "library_hint")),
         this.panelX + 12,
         this.panelY + 40,
         this.panelWidth - 24,
         -9604473
      );
      super.render(g, mouseX, mouseY, partial);
   }

   public void onClose() {
      this.minecraft.setScreen(this.parent);
   }

   public void removed() {
      if (!this.loaded) {
         this.requested = false;
      }
   }

   @Override
   public int interactionEntityId() {
      return this.interactionEntityId;
   }

   public boolean isPauseScreen() {
      return false;
   }
}
