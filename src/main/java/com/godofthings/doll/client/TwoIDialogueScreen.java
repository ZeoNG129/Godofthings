package com.godofthings.doll.client;

import com.godofthings.doll.client.DollScreenStyle;
import com.godofthings.doll.config.DollConfig;
import com.godofthings.doll.entity.TwoIDollEntity;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.init.DollRegistry;
import com.godofthings.doll.network.DollNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class TwoIDialogueScreen extends Screen implements TwoIInteractionScreen {
   private static final int PANEL_HEIGHT = 250;
   private final int entityId;
   private final boolean itemMode;
   private final InteractionHand itemHand;
   private boolean inviteReady;
   private final boolean invitationUsed;
   private int bond;
   private Button inviteButton;
   private TwoIDollEntity preview;
   private Level previewLevel;
   private int panelX;
   private int panelY;
   private int panelWidth;
   private Button talkButton;
   private Button patButton;
   private Button giftButton;
   private Button reclaimButton;
   private int startWait;

   public TwoIDialogueScreen(int entityId, boolean inviteReady, int giftScore) {
      this(entityId, false, null, inviteReady, false, giftScore);
   }

   private TwoIDialogueScreen(int entityId, boolean itemMode, InteractionHand itemHand, boolean inviteReady, boolean invitationUsed, int bond) {
      super(Component.translatable("gui.godofthings.two_i.title"));
      this.entityId = entityId;
      this.itemMode = itemMode;
      this.itemHand = itemHand;
      this.inviteReady = inviteReady;
      this.invitationUsed = invitationUsed;
      this.bond = Math.max(-100, Math.min(100, bond));
   }

   public static TwoIDialogueScreen forItem(InteractionHand hand, int giftScore, boolean invitationUsed) {
      return new TwoIDialogueScreen(-1, true, hand, giftScore >= 100 || invitationUsed, invitationUsed, giftScore);
   }

   protected void init() {
      this.panelWidth = Math.min(430, this.width - 20);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - 250) / 2;
      int actionX = this.panelX + 145;
      int actionWidth = this.panelWidth - 157;
      int gap = 6;
      int buttonWidth = (actionWidth - gap) / 2;
      int firstY = this.panelY + 167;
      int libraryWidth = Math.min(86, (this.panelWidth - 95) / 2);
      this.addAction(
            this.panelX + this.panelWidth - 2 * libraryWidth - 16,
            this.panelY + 5,
            libraryWidth,
            "library",
            button -> this.minecraft.setScreen(new TwoIStoryLibraryScreen(this, this.bond))
         )
         .setHeight(18);
      if (this.itemMode) {
         this.addRenderableWidget(Button.builder(Component.translatable("gui.godofthings.two_i.deploy"), button -> {
            DollNetwork.sendDeploy(this.itemHand);
            this.onClose();
         }).bounds(actionX, firstY, Math.min(actionWidth, 132), 20).build());
      } else {
         this.talkButton = this.addAction(actionX, firstY, buttonWidth, "talk", button -> this.startStory());
         this.patButton = this.addAction(actionX + buttonWidth + gap, firstY, buttonWidth, "pat", button -> DollNetwork.sendPat(this.entityId));
         this.giftButton = this.addAction(actionX, firstY + 26, buttonWidth, "gift", button -> DollNetwork.sendGift(this.entityId));
         this.inviteButton = this.addAction(
            actionX + buttonWidth + gap,
            firstY + 26,
            buttonWidth,
            !this.invitationUsed && (!this.inviteReady || this.bond >= 100) ? "invite" : "revisit",
            button -> {
               DollNetwork.sendInvite(this.entityId);
               this.onClose();
            }
         );
         this.inviteButton.active = this.inviteReady && true;
         this.reclaimButton = this.addAction(actionX, firstY + 52, buttonWidth, "reclaim", button -> {
            DollNetwork.sendReclaim(this.entityId);
            this.onClose();
         });
         this.addAction(actionX + buttonWidth + gap, firstY + 52, buttonWidth, "smalltalk", button -> DollNetwork.sendTalk(this.entityId));
         this.talkButton.active = this.startWait == 0;
      }
   }

   private Button addAction(int x, int y, int buttonWidth, String action, OnPress onPress) {
      return (Button)this.addRenderableWidget(
         Button.builder(Component.translatable("gui.godofthings.two_i." + action), onPress).bounds(x, y, buttonWidth, 22).build()
      );
   }

   private void startStory() {
      if (!this.itemMode && this.startWait <= 0) {
         this.startWait = 100;
         this.talkButton.active = false;
         DollNetwork.sendStoryStart(this.entityId);
      }
   }

   public void openStory(long sessionId, int chapter, int bond) {
      this.openStory(sessionId, chapter, bond, 0);
   }

   public void openStory(long sessionId, int chapter, int bond, int round) {
      if (this.startWait != 0 && this.minecraft != null) {
         this.onStoryUnavailable();
         this.bond = Math.max(-100, Math.min(100, bond));
         this.minecraft.setScreen(new TwoIStoryScreen(this, this.entityId, sessionId, chapter, bond, round));
      }
   }

   public void onStoryUnavailable() {
      this.startWait = 0;
      if (this.talkButton != null) {
         this.talkButton.active = true;
      }
   }

   public void updateStoryBond(int bond) {
      this.bond = Math.max(-100, Math.min(100, bond));
   }

   public int entityId() {
      return this.entityId;
   }

   @Override
   public int interactionEntityId() {
      return this.itemMode ? -1 : this.entityId;
   }

   public void tick() {
      super.tick();
      if (this.startWait > 0 && --this.startWait == 0) {
         this.onStoryUnavailable();
      }

      if (this.inviteButton != null) {
         this.inviteButton.active = this.inviteReady && true;
      }
   }

   public void updateGiftStatus(boolean inviteReady, int bond) {
      this.inviteReady = inviteReady;
      this.bond = Math.max(-100, Math.min(100, bond));
      if (this.inviteButton != null) {
         this.inviteButton.active = inviteReady && true;
      }
   }

   public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.panelWidth = Math.min(430, this.width - 20);
      this.panelX = (this.width - this.panelWidth) / 2;
      this.panelY = (this.height - 250) / 2;
      graphics.fill(0, 0, this.width, this.height, 671088640);
      DollScreenStyle.drawPanel(graphics, this.panelX, this.panelY, this.panelWidth, 250);
      graphics.fill(this.panelX + 5, this.panelY + 4, this.panelX + this.panelWidth - 5, this.panelY + 25, -5394236);
      DollScreenStyle.drawInset(graphics, this.panelX + 10, this.panelY + 31, this.panelX + 137, this.panelY + 250 - 10);
      graphics.fill(this.panelX + 145, this.panelY + 31, this.panelX + this.panelWidth - 10, this.panelY + 32, -6643788);
      graphics.drawString(this.font, Component.translatable("gui.godofthings.two_i.title"), this.panelX + 10, this.panelY + 9, -12501164, false);
      graphics.drawString(this.font, Component.translatable("gui.godofthings.two_i.interaction"), this.panelX + 145, this.panelY + 40, -9604473, false);
      LivingEntity doll = this.previewEntity();
      LivingEntity status = this.statusEntity();
      if (doll != null) {
         Minecraft minecraft = Minecraft.getInstance();
         InventoryScreen.renderEntityInInventoryFollowsMouse(
            graphics, this.panelX + 12, this.panelY + 35, this.panelX + 135, this.panelY + 250 - 15, 60, 0.0625F, (float)mouseX, (float)mouseY, doll
         );
         if (doll instanceof TwoIDollEntity twoI && !twoI.getBubbleText().isEmpty()) {
            Component bubble = twoI.getBubbleMessage();
            int bubbleWidth = Math.min(118, this.font.width(bubble) + 12);
            int bubbleHeight = this.font.split(bubble, bubbleWidth - 12).size() * 9 + 10;
            int bubbleX = this.panelX + 18;
            int bubbleY = this.panelY + 38;
            graphics.fill(bubbleX, bubbleY, bubbleX + bubbleWidth, bubbleY + bubbleHeight, -303040513);
            graphics.fill(bubbleX + 5, bubbleY + bubbleHeight, bubbleX + 10, bubbleY + bubbleHeight + 4, -303040513);
            graphics.drawWordWrap(this.font, bubble, bubbleX + 6, bubbleY + 5, bubbleWidth - 12, -14408143);
         }

         this.renderEntityStatus(graphics, status == null ? doll : status, this.panelX + 13, this.panelY + 250 - 42, 121);
      }

      int textX = this.panelX + 145;
      int textWidth = this.panelWidth - 157;
      int bondColor = this.bond < 0 ? -6602923 : (this.bond > 0 ? -12880302 : -9604473);
      graphics.drawString(
         this.font, Component.translatable("gui.godofthings.two_i.bond", new Object[]{this.bond}), textX, this.panelY + 55, bondColor, false
      );
      int meterRight = this.panelX + this.panelWidth - 10;
      int meterTop = this.panelY + 68;
      graphics.fill(textX, meterTop, meterRight, meterTop + 4, -6643788);
      int meterWidth = Math.max(0, meterRight - textX - 2);
      int fill = Mth.clamp((this.bond + 100) * meterWidth / 200, 0, meterWidth);
      if (fill > 0) {
         graphics.fill(textX + 1, meterTop + 1, textX + 1 + fill, meterTop + 3, this.bond < 0 ? -6602923 : -11438953);
      }

      Component dialogue = (Component)(this.inviteReady
         ? Component.translatable("gui.godofthings.two_i.invite_hint")
         : (this.itemMode ? Component.translatable("gui.godofthings.two_i.item_dialogue") : this.bondDialogue()));
      graphics.drawWordWrap(this.font, dialogue, textX, this.panelY + 80, textWidth, -12501164);
      if (this.invitationUsed) {
         graphics.drawWordWrap(this.font, Component.translatable("gui.godofthings.two_i.invitation_used"), textX, this.panelY + 116, textWidth, -7893083);
      }

      graphics.drawString(
         this.font,
         Component.translatable(
            "gui.godofthings.two_i.relationship", new Object[]{Component.translatable("gui.godofthings.two_i.relationship." + this.relationship())}
         ),
         textX,
         this.panelY + 148,
         -9604473,
         false
      );
      super.render(graphics, mouseX, mouseY, partialTick);
      if (mouseX >= textX && mouseX < textX + textWidth && mouseY >= this.panelY + 145 && mouseY < this.panelY + 161) {
         graphics.renderTooltip(this.font, this.font.split(Component.translatable("help.godofthings.two_i.relationship"), 240), mouseX, mouseY);
      }
   }

   private String relationship() {
      if (this.bond <= -50) {
         return "hurt";
      } else if (this.bond < 0) {
         return "wary";
      } else if (this.bond >= 70) {
         return "close";
      } else if (this.bond >= 40) {
         return "trusted";
      } else {
         return this.bond >= 20 ? "friend" : "acquaintance";
      }
   }

   private Component bondDialogue() {
      if (this.bond <= -50) {
         return Component.translatable("gui.godofthings.two_i.dialogue.low");
      } else if (this.bond < 0) {
         return Component.translatable("gui.godofthings.two_i.dialogue.uneasy");
      } else if (this.bond >= 70) {
         return Component.translatable("gui.godofthings.two_i.dialogue.close");
      } else {
         return this.bond >= 25
            ? Component.translatable("gui.godofthings.two_i.dialogue.warm")
            : Component.translatable("gui.godofthings.two_i.dialogue");
      }
   }

   private void renderEntityStatus(GuiGraphics graphics, LivingEntity doll, int x, int y, int width) {
      Component action;
      Component environment;
      Object[] var13;
      MutableComponent var15;
      label21: {
         graphics.fill(x - 2, y - 2, x + width + 2, y + 29, -1206906860);
         action = Component.translatable(
            "gui.godofthings.two_i.status.action",
            new Object[]{
               Component.translatable(
                  "gui.godofthings.two_i.status.action." + actionKey(doll instanceof TwoIDollEntity entity ? entity.getPoseAction() : 0)
               )
            }
         );
         environment = Component.translatable(
            "gui.godofthings.two_i.status.environment",
            new Object[]{
               Component.translatable(
                  "gui.godofthings.two_i.status.environment."
                     + environmentKey(doll instanceof TwoIDollEntity entityx ? entityx.getEnvironmentAction() : 0)
               )
            }
         );
         var13 = new Object[1];
         if (doll instanceof TwoIDollEntity entityxx && entityxx.hasSharedUmbrella()) {
            var15 = Component.translatable("gui.godofthings.two_i.status.shared");
            break label21;
         }

         var15 = Component.translatable("gui.godofthings.two_i.status.single");
      }

      var13[0] = var15;
      Component umbrella = Component.translatable("gui.godofthings.two_i.status.umbrella", var13);
      graphics.drawString(this.font, action, x, y, -1710619, false);
      graphics.drawString(this.font, environment, x, y + 10, -4603700, false);
      graphics.drawString(this.font, umbrella, x, y + 20, -4603700, false);
   }

   private static String actionKey(int action) {
      return switch (action) {
         case 1 -> "greeting";
         case 2 -> "curious";
         case 3 -> "patted";
         case 4 -> "gift";
         case 5 -> "invite";
         case 6 -> "swing_feet";
         case 7 -> "umbrella";
         case 8 -> "sneeze";
         case 9 -> "shiver";
         case 10 -> "stretch";
         case 11 -> "dust";
         case 12 -> "shy";
         default -> "idle";
      };
   }

   private static String environmentKey(int environment) {
      if ((environment & 4) != 0) {
         return "thunder";
      } else if ((environment & 2) != 0) {
         return "cold";
      } else {
         return (environment & 1) != 0 ? "rain" : "clear";
      }
   }

   private LivingEntity previewEntity() {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) {
         return null;
      } else {
         if (this.previewLevel != minecraft.level) {
            this.previewLevel = minecraft.level;
            this.preview = null;
         }

         if (this.preview == null) {
            this.preview = (TwoIDollEntity)DollRegistry.TWO_I_DOLL_ENTITY.get().create(minecraft.level);
            if (this.preview != null) {
               this.preview.setNoAi(true);
               this.preview.setNoGravity(true);
               this.preview.noPhysics = true;
               this.preview.setPos(0.0, 0.0, 0.0);
               this.preview.setYRot(180.0F);
               this.preview.yRotO = 180.0F;
               this.preview.yBodyRot = 180.0F;
               this.preview.yBodyRotO = 180.0F;
            }
         }

         if (this.preview == null) {
            return null;
         } else {
            if (!this.itemMode && minecraft.level.getEntity(this.entityId) instanceof TwoIDollEntity entity) {
               this.preview.restoreFromItem(entity.toDollItem());
               this.preview.copyClientPreviewStateFrom(entity);
            } else if (this.itemMode) {
               ItemStack held = minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(this.itemHand);
               if (held.is((Item)DollRegistry.TWO_I_DOLL_ITEM.get())) {
                  this.preview.restoreFromItem(held);
               }
            }

            return this.preview;
         }
      }
   }

   private LivingEntity statusEntity() {
      Minecraft minecraft = Minecraft.getInstance();
      if (!this.itemMode && minecraft.level != null) {
         Entity var3 = minecraft.level.getEntity(this.entityId);
         if (var3 instanceof TwoIDollEntity) {
            return (TwoIDollEntity)var3;
         }
      }

      return this.preview;
   }

   public boolean isPauseScreen() {
      return false;
   }
}
