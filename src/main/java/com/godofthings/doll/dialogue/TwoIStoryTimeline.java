package com.godofthings.doll.dialogue;

import java.util.ArrayList;
import java.util.List;

public final class TwoIStoryTimeline {
   private final List<TwoIStoryTimeline.Cue> cues;
   private int cursor;
   private double remainingMs;

   public TwoIStoryTimeline(String key, String text, boolean player, boolean narration) {
      int[] chars = text.codePoints().toArray();
      List<TwoIStoryTimeline.Cue> result = new ArrayList<>();
      int phraseStart = 0;

      for (int i = 0; i < chars.length; i++) {
         int cp = chars[i];
         int end = i + 1;

         while (end < chars.length && isCombining(chars[end])) {
            end++;
         }

         if (isRepeatedPause(cp)) {
            while (end < chars.length && chars[end] == cp) {
               end++;
            }
         }

         TwoIStoryDirection.Mood mood = TwoIStoryDirection.mood(key, text, i);
         TwoIStoryTimeline.Profile profile = profile(mood);
         int phraseEnd = i;

         while (phraseEnd < chars.length && !isBoundary(chars[phraseEnd])) {
            phraseEnd++;
         }

         int voicedBefore = 0;
         int voicedAfter = 0;

         for (int k = phraseStart; k < i; k++) {
            if (Character.isLetterOrDigit(chars[k])) {
               voicedBefore++;
            }
         }

         for (int kx = i + 1; kx < phraseEnd; kx++) {
            if (Character.isLetterOrDigit(chars[kx])) {
               voicedAfter++;
            }
         }

         double phase = (double)voicedBefore / (double)Math.max(1, voicedBefore + voicedAfter);
         double arch = Math.sin(Math.PI * phase);
         double accent = Math.sin((double)(voicedBefore + 1) * 1.71 + (double)phraseStart * 0.37) * 0.16;
         double contour = arch + accent;
         if (voicedAfter < 4) {
            int total = voicedBefore + voicedAfter + 1;
            int tailStart = Math.max(0, total - 4);
            double tailPhase = (double)tailStart / (double)Math.max(1, total - 1);
            double startContour = Math.sin(Math.PI * tailPhase) + Math.sin((double)(tailStart + 1) * 1.71 + (double)phraseStart * 0.37) * 0.16;
            double terminal = phraseEnd < chars.length ? ending(chars[phraseEnd]) : -0.3;
            double blend = (double)(voicedBefore - tailStart) / (double)Math.max(1, total - 1 - tailStart);
            contour = startContour + (terminal - startContour) * blend;
         }

         float pitch = (float)((player ? 0.95 : (double)profile.pitch) + (double)profile.range * contour);
         float volume = (float)((double)profile.volume * (0.89 + arch * 0.11));
         double cadence = profile.cadence * (1.0 + 0.08 * arch + (voicedBefore == 0 ? profile.hesitation : 0.0));
         if (voicedAfter < 2) {
            cadence *= 1.1;
         }

         double pause = pause(cp) * (mood != TwoIStoryDirection.Mood.SHY && mood != TwoIStoryDirection.Mood.UNEASY ? 1.0 : 1.18);
         result.add(
            new TwoIStoryTimeline.Cue(
               end,
               cp,
               !narration && Character.isLetterOrDigit(cp),
               Math.clamp(pitch, 0.78F, 1.3F),
               volume,
               player ? TwoIStoryTimeline.Timbre.PLAYER : profile.timbre,
               Character.isWhitespace(cp) ? 0.35 : cadence,
               pause
            )
         );
         if (isBoundary(cp)) {
            phraseStart = end;
         }

         i = end - 1;
      }

      this.cues = List.copyOf(result);
   }

   public TwoIStoryTimeline.Cue advance(double elapsedMs, int speed) {
      if (this.cursor >= this.cues.size()) {
         return null;
      } else {
         this.remainingMs = this.remainingMs - Math.max(0.0, elapsedMs);
         if (this.remainingMs > 0.0) {
            return null;
         } else {
            TwoIStoryTimeline.Cue cue = this.cues.get(this.cursor++);
            double base = speed == 1 ? 92.0 : (speed == 4 ? 48.0 : 65.0);
            this.remainingMs = Math.max(48.0, base * cue.cadence()) + cue.pauseMs();
            return cue;
         }
      }
   }

   public List<TwoIStoryTimeline.Cue> cues() {
      return this.cues;
   }

   public boolean complete() {
      return this.cursor == this.cues.size();
   }

   public void finish() {
      this.cursor = this.cues.size();
      this.remainingMs = 0.0;
   }

   private static TwoIStoryTimeline.Profile profile(TwoIStoryDirection.Mood mood) {
      return switch (mood) {
         case WARM -> new TwoIStoryTimeline.Profile(0.99F, 0.045F, 0.32F, 1.07, 0.12, TwoIStoryTimeline.Timbre.SOFT);
         case SHY -> new TwoIStoryTimeline.Profile(1.02F, 0.025F, 0.26F, 1.2, 0.55, TwoIStoryTimeline.Timbre.SOFT);
         case BRIGHT -> new TwoIStoryTimeline.Profile(1.06F, 0.1F, 0.36F, 0.9, 0.0, TwoIStoryTimeline.Timbre.BRIGHT);
         case PLAYFUL -> new TwoIStoryTimeline.Profile(1.03F, 0.12F, 0.34F, 0.94, 0.05, TwoIStoryTimeline.Timbre.BRIGHT);
         case CURIOUS -> new TwoIStoryTimeline.Profile(1.02F, 0.07F, 0.32F, 1.02, 0.15, TwoIStoryTimeline.Timbre.NATURAL);
         case SERIOUS -> new TwoIStoryTimeline.Profile(0.95F, 0.035F, 0.34F, 1.04, 0.0, TwoIStoryTimeline.Timbre.NATURAL);
         case UNEASY -> new TwoIStoryTimeline.Profile(0.98F, 0.04F, 0.27F, 1.17, 0.42, TwoIStoryTimeline.Timbre.SOFT);
         case HURT -> new TwoIStoryTimeline.Profile(0.88F, 0.025F, 0.23F, 1.28, 0.26, TwoIStoryTimeline.Timbre.SOFT);
         case STARTLED -> new TwoIStoryTimeline.Profile(1.12F, 0.08F, 0.37F, 0.91, 0.0, TwoIStoryTimeline.Timbre.BRIGHT);
         case CALM -> new TwoIStoryTimeline.Profile(0.96F, 0.03F, 0.29F, 1.12, 0.1, TwoIStoryTimeline.Timbre.SOFT);
      };
   }

   private static boolean isCombining(int cp) {
      int type = Character.getType(cp);
      return type == 6 || type == 8 || type == 7 || cp == 65039;
   }

   private static boolean isRepeatedPause(int cp) {
      return cp == 8230 || cp == 46 || cp == 8212;
   }

   private static boolean isBoundary(int cp) {
      return "，,、；;：:。.!！?？…—\n".indexOf(cp) >= 0;
   }

   private static double ending(int cp) {
      return switch (cp) {
         case 33, 65281 -> 0.7;
         case 63, 65311 -> 1.6;
         case 8212, 8230 -> -0.65;
         default -> -0.45;
      };
   }

   private static double pause(int cp) {
      return switch (cp) {
         case 10, 59, 65307 -> 180.0;
         case 33, 46, 63, 12290, 65281, 65311 -> 220.0;
         case 44, 58, 12289, 65292, 65306 -> 110.0;
         case 8212 -> 190.0;
         case 8230 -> 330.0;
         default -> 0.0;
      };
   }

   public static record Cue(
      int revealed, int codePoint, boolean voiced, float pitch, float volume, TwoIStoryTimeline.Timbre timbre, double cadence, double pauseMs
   ) {
   }

   private static record Profile(float pitch, float range, float volume, double cadence, double hesitation, TwoIStoryTimeline.Timbre timbre) {
   }

   public static enum Timbre {
      NATURAL,
      SOFT,
      BRIGHT,
      PLAYER;
   }
}
