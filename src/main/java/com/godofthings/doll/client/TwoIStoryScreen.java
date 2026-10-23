package com.godofthings.doll.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.godofthings.doll.dialogue.TwoIStoryScript;
import com.godofthings.doll.dialogue.TwoIStoryTimeline;
import com.godofthings.doll.dialogue.TwoIStoryVoice;
import com.godofthings.doll.entity.TwoIStorySession;
import com.godofthings.doll.network.DollNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public final class TwoIStoryScreen extends Screen implements TwoIInteractionScreen {
   private static final int CANVAS_W = 800;
   private static final int CANVAS_H = 450;
   private static final int HISTORY_ROWS = 17;
   private static final ResourceLocation FRAME = texture("ui/dialogue_frame");
   private static final ResourceLocation BOW = texture("ui/bow");
   private static int textSpeed = 2;
   private static boolean voiceEnabled = true;
   private final TwoIStoryVoice voice = new TwoIStoryVoice();
   private final TwoIDialogueScreen parent;
   private final Screen returnScreen;
   private final boolean replay;
   private final int interactionEntityId;
   private final int entityId;
   private final int chapter;
   private final long sessionId;
   private final ResourceLocation cg;
   private int bond;
   private List<TwoIStoryScript.Line> lines;
   private int stage;
   private int lineIndex;
   private int round;
   private int chosen = -1;
   private String fullText = "";
   private String visibleText = "";
   private int revealed;
   private int characterCount;
   private int readTicks;
   private int waitingTicks;
   private LongSupplier clock = System::nanoTime;
   private long lastFrameNanos;
   private TwoIStoryTimeline timeline;
   private ResourceLocation expression;
   private boolean automatic;
   private boolean fastForward;
   private boolean hidden;
   private boolean waiting;
   private boolean failed;
   private int overlay;
   private final List<Component> history = new ArrayList<>();
   private List<FormattedCharSequence> historyLines = List.of();
   private int historyScroll;
   private final List<Button> controls = new ArrayList<>();
   private final List<Button> overlayControls = new ArrayList<>();
   private final Button[] choices = new Button[3];
   private Button nextButton;
   private Button autoButton;
   private Button fastButton;
   private Button speedButton;
   private Button voiceButton;
   private Button libraryButton;
   
   public TwoIStoryScreen(TwoIDialogueScreen parent, int entityId, long sessionId, int chapter, int bond) {
      this(parent, entityId, sessionId, chapter, bond, 0);
   }

   public TwoIStoryScreen(TwoIDialogueScreen parent, int entityId, long sessionId, int chapter, int bond, int round) {
      this(parent, parent, entityId, sessionId, chapter, bond, false, round);
   }

   public static TwoIStoryScreen replay(Screen parent, int chapter, int bond) {
      return new TwoIStoryScreen(parent, null, -1, 0L, chapter, bond, true, 0);
   }

   private TwoIStoryScreen(Screen returnScreen, TwoIDialogueScreen parent, int entityId, long sessionId, int chapter, int bond, boolean replay, int round) {
      super(TwoIStoryScript.title(chapter));
      this.parent = parent;
      this.returnScreen = returnScreen;
      this.interactionEntityId = TwoIInteractionScreen.entityFor(returnScreen);
      this.replay = replay;
      this.entityId = entityId;
      this.sessionId = sessionId;
      this.chapter = Mth.clamp(chapter, 0, 5);
      this.bond = bond;
      this.round = Mth.clamp(round, 0, 2);

      this.cg = texture("cg/" + switch (this.chapter) {
         case 1, 3, 4 -> "friends";
         case 2, 5 -> "starfield_invite";
         default -> "first_meet";
      });
      this.lines = this.round == 0 ? TwoIStoryScript.opening(this.chapter, bond) : TwoIStoryScript.prompt(this.chapter, this.round);
      this.enterLine();
   }

   private static ResourceLocation texture(String file) {
      return ResourceLocation.fromNamespaceAndPath("godofthings", "textures/dialogue/" + file + ".png");
   }

   private static Component ui(String key) {
      return Component.translatable("gui.godofthings.two_i.vn." + key);
   }

   private static Component speaker(TwoIStoryScript.Line line) {
      return (Component)(line.narration() ? ui("narrator") : (line.player() ? ui("you") : Component.literal("紬紬")));
   }

   protected void init() {
      this.controls.clear();
      this.overlayControls.clear();
      this.autoButton = this.control(246, 410, 90, "auto", b -> {
         this.automatic = !this.automatic && this.stage != 1 && !this.failed;
         this.fastForward = false;
         this.autoButton.setMessage(ui(this.automatic ? "auto_on" : "auto"));
         this.readTicks = 0;
         this.syncWidgets();
      });
      this.controls.add(this.autoButton);
      this.controls.add(this.control(341, 410, 90, "log", b -> this.openOverlay(1)));
      this.fastButton = this.control(436, 410, 90, "fast", b -> {
         this.fastForward = !this.fastForward && this.stage != 1 && !this.failed;
         this.automatic = false;
         this.readTicks = 0;
         this.syncWidgets();
      });
      this.controls.add(this.fastButton);
      this.controls.add(this.control(531, 410, 90, "settings", b -> this.openOverlay(2)));
      this.nextButton = this.control(626, 410, 142, "continue", b -> this.advance());
      this.controls.add(this.nextButton);
      this.libraryButton = this.control(458, 12, 102, "library", b -> {
         this.automatic = false;
         this.fastForward = false;
         this.voice.stop();
         this.minecraft.setScreen(new TwoIStoryLibraryScreen(this, this.bond));
      });
      this.controls.add(this.libraryButton);
      this.controls.add(this.control(672, 12, 104, "return", b -> this.onClose()));

      for (int i = 0; i < this.choices.length; i++) {
         int choice = i;
         this.choices[i] = (Button)this.addRenderableWidget(
            new TwoIStoryScreen.NovelButton(200, 164 + i * 40, 400, 34, this.choiceLabel(i), b -> this.choose(choice))
         );
         this.choices[i].setTooltip(Tooltip.create(ui(this.replay ? "replay_hint" : "choice_bond_hint")));
      }

      this.overlayControls.add(this.control(546, 90, 96, "close", b -> {
         this.overlay = 0;
         this.syncWidgets();
      }));
      this.overlayControls.add(this.control(170, 312, 106, "previous", b -> this.scrollHistory(-17)));
      this.overlayControls.add(this.control(284, 312, 106, "next_page", b -> this.scrollHistory(17)));
      this.speedButton = this.control(270, 180, 260, "speed", b -> {
         textSpeed = textSpeed == 1 ? 2 : (textSpeed == 2 ? 4 : (textSpeed == 4 ? 0 : 1));
         this.speedButton.setMessage(this.speedLabel());
      });
      this.speedButton.setMessage(this.speedLabel());
      this.overlayControls.add(this.speedButton);
      this.voiceButton = this.control(270, 216, 260, "voice_on", b -> {
         voiceEnabled = !voiceEnabled;
         if (!voiceEnabled) {
            this.voice.stop();
         }

         this.voiceButton.setMessage(ui(voiceEnabled ? "voice_on" : "voice_off"));
      });
      this.voiceButton.setMessage(ui(voiceEnabled ? "voice_on" : "voice_off"));
      this.overlayControls.add(this.voiceButton);
      if (this.overlay == 1) {
         this.rebuildHistory();
      }

      this.syncWidgets();
   }

   private Button control(int x, int y, int w, String key, OnPress action) {
      return (Button)this.addRenderableWidget(new TwoIStoryScreen.NovelButton(x, y, w, 24, ui(key), action));
   }

   private Component speedLabel() {
      return Component.translatable(
         "gui.godofthings.two_i.vn.speed_value",
         new Object[]{ui("speed." + (textSpeed == 0 ? "instant" : (textSpeed == 1 ? "slow" : (textSpeed == 2 ? "normal" : "fast"))))}
      );
   }

   private Component choiceLabel(int choice) {
      Component text = TwoIStoryScript.choice(this.chapter, this.round, choice);
      int delta = TwoIStorySession.bondDelta(choice);
      return text.copy().append(Component.literal(this.replay ? "  [↶]" : (delta > 0 ? "  [+1]" : (delta < 0 ? "  [-1]" : "  [±0]"))));
   }

   private void syncWidgets() {
      this.lastFrameNanos = this.clock.getAsLong();
      if (this.hidden || this.overlay != 0 || this.fastForward || this.waiting || this.failed || this.stage == 1) {
         this.voice.stop();
      }

      for (Button b : this.controls) {
         b.visible = this.overlay == 0 && !this.hidden;
      }

      for (Button b : this.overlayControls) {
         b.visible = false;
      }

      if (this.overlay > 0) {
         this.overlayControls.getFirst().visible = true;
         if (this.overlay == 1) {
            this.overlayControls.get(1).visible = true;
            this.overlayControls.get(2).visible = true;
         } else {
            this.speedButton.visible = true;
            this.voiceButton.visible = true;
         }
      }

      for (int i = 0; i < this.choices.length; i++) {
         Button b = this.choices[i];
         b.setMessage(this.choiceLabel(i));
         b.visible = this.stage == 1 && this.overlay == 0 && !this.hidden;
         b.active = !this.waiting && !this.failed;
      }

      if (this.nextButton != null) {
         this.libraryButton.active = !this.waiting;
                  this.nextButton.active = !this.waiting && this.stage != 1 || this.failed;
         this.nextButton.setMessage(ui(!this.failed && (this.stage != 3 || this.lineIndex != this.lines.size() - 1) ? "continue" : "return"));
         this.autoButton.active = this.stage != 1 && !this.failed;
         this.autoButton.setMessage(ui(this.automatic ? "auto_on" : "auto"));
         this.fastButton.active = this.stage != 1 && !this.failed;
         this.fastButton.setMessage(ui(this.fastForward ? "fast_on" : "fast"));
      }

      if (this.getFocused() instanceof Button b && (!b.visible || !b.active)) {
         this.setFocused(null);
      }
   }

   private void enterLine() {
      TwoIStoryScript.Line line = this.lines.get(this.lineIndex);
      this.fullText = TwoIStoryScript.text(line).getString();
      this.characterCount = this.fullText.codePointCount(0, this.fullText.length());
      this.revealed = 0;
      this.visibleText = "";
      this.voice.stop();
      this.timeline = new TwoIStoryTimeline(line.key(), this.fullText, line.player(), line.narration());
      this.lastFrameNanos = this.clock.getAsLong();
      this.readTicks = 0;
      this.expression = texture("expressions/" + line.expression());
      String who = speaker(line).getString();
      this.history.add(Component.literal(who + "  ·  " + this.fullText));
      if (this.history.size() > 512) {
         this.history.removeFirst();
      }

      if (this.minecraft != null) {
         this.minecraft.getNarrator().sayNow(Component.literal(who + ": " + this.fullText));
      }
   }

   private void reveal() {
      this.timeline.finish();
      this.voice.stop();
      this.revealed = this.characterCount;
      this.visibleText = this.fullText;
      this.readTicks = 0;
   }

   private void advance() {
      if (this.overlay == 0 && !this.hidden && !this.waiting) {
         if (this.failed) {
            this.onClose();
         } else if (this.revealed < this.characterCount) {
            this.reveal();
         } else if (this.stage != 1) {
            if (++this.lineIndex < this.lines.size()) {
               this.enterLine();
            } else if (this.stage == 0) {
               this.lineIndex = this.lines.size() - 1;
               this.stage = 1;
               this.automatic = false;
               this.fastForward = false;
            } else {
               if (this.stage != 2) {
                  this.onClose();
                  return;
               }

               if (++this.round < 3) {
                  this.stage = 0;
                  this.chosen = -1;
                  this.lines = TwoIStoryScript.prompt(this.chapter, this.round);
               } else {
                  this.stage = 3;
                  this.lines = TwoIStoryScript.ending(this.chapter);
               }

               this.lineIndex = 0;
               this.enterLine();
            }

            this.syncWidgets();
         }
      }
   }

   private void choose(int choice) {
      if (this.stage == 1 && !this.waiting && !this.failed && this.chosen < 0) {
         this.automatic = false;
         this.fastForward = false;
         this.chosen = choice;
         if (this.replay) {
            this.showBranch();
         } else {
            this.waiting = true;
            this.waitingTicks = 0;
            DollNetwork.sendStoryChoice(this.entityId, this.sessionId, this.round * 3 + choice);
            this.syncWidgets();
         }
      }
   }

   public int entityId() {
      return this.entityId;
   }

   @Override
   public int interactionEntityId() {
      return this.interactionEntityId;
   }

   public void onChoiceResult(long token, int value) {
      this.onChoiceResult(token, value, this.round);
   }

   public void onChoiceResult(long token, int value, int acceptedRound) {
      if (acceptedRound == this.round) {
         if (!this.replay && token == this.sessionId && this.waiting && this.chosen >= 0) {
            this.waiting = false;
            this.bond = value;
            this.parent.updateStoryBond(value);
            this.showBranch();
         }
      }
   }

   private void showBranch() {
      this.stage = 2;
      this.lineIndex = 0;
      this.lines = TwoIStoryScript.branch(this.chapter, this.round, this.chosen);
      this.enterLine();
      this.syncWidgets();
   }

   public void updateGiftStatus(boolean ready, int value) {
      this.bond = value;
      if (this.parent != null) {
         this.parent.updateGiftStatus(ready, value);
      }
   }

   public void onStoryUnavailable(long token) {
      this.onStoryUnavailable(token, this.round);
   }

   public void onStoryUnavailable(long token, int rejectedRound) {
      if (rejectedRound == this.round) {
         if (token == this.sessionId && this.waiting) {
            this.waiting = false;
            this.automatic = false;
            this.fastForward = false;
            this.failed = true;
            this.syncWidgets();
         }
      }
   }

   public void tick() {
      if (this.waiting && ++this.waitingTicks >= 200) {
         this.onStoryUnavailable(this.sessionId);
      }

      if (!this.hidden && this.overlay == 0 && !this.waiting && !this.failed && this.stage != 1) {
         if (this.revealed < this.characterCount) {
            if (this.fastForward || textSpeed == 0) {
               this.reveal();
            }
         } else {
            if ((this.automatic || this.fastForward) && ++this.readTicks >= (this.fastForward ? 4 : Math.max(40, this.characterCount + 20))) {
               this.advance();
            }
         }
      }
   }

   private void updatePlayback() {
      long now = this.clock.getAsLong();
      double elapsedMs = Math.max(0.0, (double)(now - this.lastFrameNanos) / 1000000.0);
      this.lastFrameNanos = now;
      if (!this.hidden && this.overlay == 0 && !this.waiting && !this.failed && this.stage != 1 && this.revealed < this.characterCount) {
         if (!this.fastForward && textSpeed != 0) {
            TwoIStoryTimeline.Cue cue = this.timeline.advance(elapsedMs, textSpeed);
            if (cue != null) {
               this.revealed = cue.revealed();
               this.visibleText = this.fullText.substring(0, this.fullText.offsetByCodePoints(0, this.revealed));
               this.voice.play(cue, voiceEnabled);
            }
         } else {
            this.reveal();
         }
      }
   }

   private void openOverlay(int type) {
      this.automatic = false;
      this.fastForward = false;
      this.overlay = type;
      if (type == 1) {
         this.rebuildHistory();
         this.historyScroll = Math.max(0, this.historyLines.size() - 17);
      }

      this.syncWidgets();
   }

   private void rebuildHistory() {
      List<FormattedCharSequence> result = new ArrayList<>();

      for (Component entry : this.history) {
         result.addAll(this.font.split(entry, 450));
         result.add(FormattedCharSequence.EMPTY);
      }

      this.historyLines = List.copyOf(result);
   }

   private void scrollHistory(int delta) {
      this.historyScroll = Mth.clamp(this.historyScroll + delta, 0, Math.max(0, this.historyLines.size() - 17));
   }

   private float scale() {
      return Math.min((float)this.width / 800.0F, (float)this.height / 450.0F);
   }

   private float left() {
      return ((float)this.width - 800.0F * this.scale()) / 2.0F;
   }

   private float top() {
      return ((float)this.height - 450.0F * this.scale()) / 2.0F;
   }

   public void renderBackground(GuiGraphics g, int x, int y, float partial) {
   }

   public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
      this.updatePlayback();
      g.fill(0, 0, this.width, this.height, -15130567);
      float scale = this.scale();
      int mx = (int)(((float)mouseX - this.left()) / scale);
      int my = (int)(((float)mouseY - this.top()) / scale);
      g.pose().pushPose();
      g.pose().translate(this.left(), this.top(), 0.0F);
      g.pose().scale(scale, scale, 1.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      g.blit(this.cg, 0, 0, 800, 450, 0.0F, 0.0F, 836, 471, 836, 471);
      if (!this.hidden) {
         g.fillGradient(0, 0, 800, 80, -1340595128, 1582152);
         g.drawString(this.font, this.title, 24, 21, -1, true);
         g.drawString(this.font, Component.translatable("gui.godofthings.two_i.bond", new Object[]{this.bond}), 24, 37, -2037249, true);
         if (this.replay) {
            g.drawString(this.font, ui("replay_hint"), 24, 54, -2037249, true);
         }

         TwoIStoryScript.Line line = this.lines.get(this.lineIndex);
         if (!line.cg()) {
            g.fill(579, 48, 751, 298, -591617);
            g.blit(this.expression, 583, 50, 164, 246, 0.0F, 0.0F, 512, 768, 512, 768);
            g.renderOutline(579, 48, 172, 250, -4995083);
         }

         drawFrame(g, 24, 314, 752, 128);
         g.blit(BOW, 4, 285, 82, 82, 0.0F, 0.0F, 627, 627, 627, 627);
         drawFrame(g, 86, 305, 122, 28);
         g.drawCenteredString(this.font, speaker(line), 147, 315, -13745034);
         Component text = (Component)(this.failed ? ui("rejected") : (this.waiting ? ui("waiting") : Component.literal(this.visibleText)));
         g.pose().pushPose();
         g.pose().translate(67.0F, 342.0F, 0.0F);
         g.pose().scale(1.25F, 1.25F, 1.0F);
         g.drawWordWrap(this.font, text, 0, 0, 528, -14600621);
         g.pose().popPose();
         if (this.stage == 1 && !this.waiting && !this.failed) {
            g.drawCenteredString(this.font, ui("choose"), 400, 148, -1);
         }

         g.drawString(this.font, ui("click_hint"), 63, 399, -10193001, false);
         if (this.overlay > 0) {
            this.drawOverlay(g);
         }

         super.render(g, mx, my, partial);
      } else {
         g.drawString(this.font, ui("show_hint"), 24, 425, -1, true);
      }

      RenderSystem.disableBlend();
      g.pose().popPose();
   }

   private void drawOverlay(GuiGraphics g) {
      g.fill(0, 0, 800, 450, -1743907800);
      drawFrame(g, 140, 58, 520, 324);
      g.drawString(this.font, ui(this.overlay == 1 ? "log" : "settings"), 172, 97, -14403232, false);
      if (this.overlay == 1) {
         int end = Math.min(this.historyLines.size(), this.historyScroll + 17);

         for (int i = this.historyScroll; i < end; i++) {
            g.drawString(this.font, this.historyLines.get(i), 172, 122 + (i - this.historyScroll) * 11, -13351839, false);
         }

         g.drawWordWrap(this.font, ui("log_hint"), 400, 318, 222, -10193001);
      } else {
         g.drawWordWrap(this.font, ui("help"), 172, 118, 454, -13351839);
         g.drawWordWrap(this.font, ui("settings_hint"), 172, 264, 454, -10193001);
      }
   }

   private static void drawFrame(GuiGraphics g, int x, int y, int w, int h) {
      RenderSystem.enableBlend();
      g.blit(FRAME, x, y, w, h, 16.0F, 60.0F, 1060, 252, 1086, 362);
   }

   public boolean mouseClicked(double x, double y, int button) {
      double mx = (x - (double)this.left()) / (double)this.scale();
      double my = (y - (double)this.top()) / (double)this.scale();
      if (button == 1) {
         if (this.overlay > 0) {
            this.overlay = 0;
         } else {
            this.hidden = !this.hidden;
         }

         this.automatic = false;
         this.fastForward = false;
         this.syncWidgets();
         return true;
      } else if (this.hidden) {
         this.hidden = false;
         this.syncWidgets();
         return true;
      } else if (super.mouseClicked(mx, my, button)) {
         return true;
      } else if (button == 0 && this.overlay == 0 && mx >= 24.0 && mx <= 776.0 && my >= 314.0 && my < 410.0) {
         this.advance();
         return true;
      } else {
         return true;
      }
   }

   public boolean mouseReleased(double x, double y, int button) {
      return super.mouseReleased((x - (double)this.left()) / (double)this.scale(), (y - (double)this.top()) / (double)this.scale(), button);
   }

   public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
      if (this.overlay == 1) {
         this.scrollHistory((int)(-Math.signum(vertical)) * 3);
      } else if (vertical > 0.0) {
         this.openOverlay(1);
      }

      return true;
   }

   public boolean keyPressed(int key, int scan, int modifiers) {
      if (key == 256) {
         if (this.overlay > 0) {
            this.overlay = 0;
            this.syncWidgets();
         } else if (this.hidden) {
            this.hidden = false;
            this.syncWidgets();
         } else {
            this.onClose();
         }

         return true;
      } else if (key != 32 && key != 257 && key != 335) {
         return super.keyPressed(key, scan, modifiers);
      } else {
         if (this.getFocused() instanceof Button button && button.visible && button.active) {
            return super.keyPressed(key, scan, modifiers);
         }

         this.advance();
         return true;
      }
   }

   public void onClose() {
      this.voice.stop();
      this.minecraft.setScreen(this.returnScreen);
   }

   public void removed() {
      this.voice.stop();
   }

   public boolean isPauseScreen() {
      return false;
   }

   private final class NovelButton extends Button {
      private NovelButton(int x, int y, int w, int h, Component text, OnPress callback) {
         super(x, y, w, h, text, callback, DEFAULT_NARRATION);
      }

      protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
         TwoIStoryScreen.drawFrame(g, this.getX(), this.getY(), this.getWidth(), this.getHeight());
         boolean selected = this.isHoveredOrFocused() && this.active;
         if (selected) {
            g.renderOutline(this.getX() + 3, this.getY() + 2, this.getWidth() - 6, this.getHeight() - 4, -9792800);
         }

         String label = TwoIStoryScreen.this.font.plainSubstrByWidth(this.getMessage().getString(), this.getWidth() - 16);
         g.drawString(
            TwoIStoryScreen.this.font,
            label,
            this.getX() + (this.getWidth() - TwoIStoryScreen.this.font.width(label)) / 2,
            this.getY() + (this.getHeight() - 8) / 2,
            this.active ? -13613451 : -7760723,
            false
         );
      }
   }
}
