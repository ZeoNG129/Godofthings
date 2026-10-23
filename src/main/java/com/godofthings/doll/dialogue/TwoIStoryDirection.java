package com.godofthings.doll.dialogue;

import java.util.HashMap;
import java.util.Map;

public final class TwoIStoryDirection {
   private static final String PREFIX = "gui.godofthings.two_i.vn.";
   private static final Map<String, TwoIStoryDirection.Mood> LINES = createDirections();
   private static final Map<String, TwoIStoryDirection.Mood> LONG_LINES = new HashMap<>();

   private TwoIStoryDirection() {
   }

   public static TwoIStoryDirection.Mood baseMood(String key) {
      return key == null ? TwoIStoryDirection.Mood.CALM : LONG_LINES.getOrDefault(key, LINES.getOrDefault(key, TwoIStoryDirection.Mood.CALM));
   }

   public static void register(String key, TwoIStoryDirection.Mood mood) {
      LONG_LINES.put(key, mood);
   }

   public static void clearLongDirections() {
      LONG_LINES.clear();
   }

   public static TwoIStoryDirection.Mood at(String key, String text, int codePointIndex) {
      return mood(key, text, codePointIndex);
   }

   public static TwoIStoryDirection.Mood mood(String key, String text, int codePointIndex) {
      TwoIStoryDirection.Mood base = baseMood(key);
      if (key != null && text != null && !text.isEmpty()) {
         int point = Math.max(0, codePointIndex);

         return switch (key) {
            case "gui.godofthings.two_i.vn.0.opening.1" -> {
               TwoIStoryDirection.Mood result = after(text, point, "你呢？", "What's your name?") ? TwoIStoryDirection.Mood.CURIOUS : base;
               if (after(text, point, "……", "...You")) {
                  result = TwoIStoryDirection.Mood.SHY;
               }

               yield result;
            }
            case "gui.godofthings.two_i.vn.0.opening.2" -> {
               TwoIStoryDirection.Mood result = after(text, point, "草里有东西", "草裡有東西", "Something in the grass") ? TwoIStoryDirection.Mood.UNEASY : base;
               if (after(text, point, "哦，叶子", "喔，葉子", "oh. A leaf")) {
                  result = TwoIStoryDirection.Mood.CALM;
               }

               if (after(text, point, "咳，你", "Ahem.")) {
                  result = TwoIStoryDirection.Mood.SHY;
               }

               yield result;
            }
            case "gui.godofthings.two_i.vn.4.opening.2" -> split(
            text, point, base, TwoIStoryDirection.Mood.UNEASY, TwoIStoryDirection.Mood.SHY, "你会不会笑话我呀", "你會不會笑話我呀", "Will you laugh at me?"
         );
            case "gui.godofthings.two_i.vn.4.branch.1.2" -> split(
            text, point, base, TwoIStoryDirection.Mood.HURT, TwoIStoryDirection.Mood.SERIOUS, "不过这一页", "不過這一頁", "I'm keeping this page"
         );
            case "gui.godofthings.two_i.vn.5.branch.2.2" -> split(
            text, point, base, TwoIStoryDirection.Mood.CALM, TwoIStoryDirection.Mood.PLAYFUL, "于是它", "於是它", "so it politely"
         );
            case "gui.godofthings.two_i.vn.2.branch.0.2" -> split(
            text, point, base, TwoIStoryDirection.Mood.WARM, TwoIStoryDirection.Mood.SHY, "我当然也会等", "我當然也會等", "of course I'll wait"
         );
            default -> base;
         };
      } else {
         return base;
      }
   }

   private static TwoIStoryDirection.Mood split(
      String text, int point, TwoIStoryDirection.Mood fallback, TwoIStoryDirection.Mood before, TwoIStoryDirection.Mood after, String... anchors
   ) {
      int boundary = anchorPoint(text, anchors);
      return boundary < 0 ? fallback : (point < boundary ? before : after);
   }

   private static boolean after(String text, int point, String... anchors) {
      int boundary = anchorPoint(text, anchors);
      return boundary >= 0 && point >= boundary;
   }

   private static int anchorPoint(String text, String... anchors) {
      int first = Integer.MAX_VALUE;

      for (String anchor : anchors) {
         int index = text.indexOf(anchor);
         if (index >= 0) {
            first = Math.min(first, index);
         }
      }

      return first == Integer.MAX_VALUE ? -1 : text.codePointCount(0, first);
   }

   private static Map<String, TwoIStoryDirection.Mood> createDirections() {
      Map<String, TwoIStoryDirection.Mood> result = new HashMap<>();
      chapter(
         result,
         0,
         TwoIStoryDirection.Mood.UNEASY,
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.SHY, TwoIStoryDirection.Mood.STARTLED, TwoIStoryDirection.Mood.SERIOUS, TwoIStoryDirection.Mood.CURIOUS
         },
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.WARM,
            TwoIStoryDirection.Mood.WARM,
            TwoIStoryDirection.Mood.UNEASY,
            TwoIStoryDirection.Mood.HURT,
            TwoIStoryDirection.Mood.BRIGHT,
            TwoIStoryDirection.Mood.PLAYFUL
         },
         new TwoIStoryDirection.Mood[]{TwoIStoryDirection.Mood.WARM, TwoIStoryDirection.Mood.PLAYFUL, TwoIStoryDirection.Mood.CURIOUS},
         TwoIStoryDirection.Mood.WARM
      );
      chapter(
         result,
         1,
         TwoIStoryDirection.Mood.SERIOUS,
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.BRIGHT, TwoIStoryDirection.Mood.UNEASY, TwoIStoryDirection.Mood.BRIGHT, TwoIStoryDirection.Mood.CURIOUS
         },
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.BRIGHT,
            TwoIStoryDirection.Mood.WARM,
            TwoIStoryDirection.Mood.SERIOUS,
            TwoIStoryDirection.Mood.SERIOUS,
            TwoIStoryDirection.Mood.PLAYFUL,
            TwoIStoryDirection.Mood.PLAYFUL
         },
         new TwoIStoryDirection.Mood[]{TwoIStoryDirection.Mood.WARM, TwoIStoryDirection.Mood.PLAYFUL, TwoIStoryDirection.Mood.CURIOUS},
         TwoIStoryDirection.Mood.BRIGHT
      );
      chapter(
         result,
         2,
         TwoIStoryDirection.Mood.UNEASY,
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.PLAYFUL, TwoIStoryDirection.Mood.SHY, TwoIStoryDirection.Mood.SERIOUS, TwoIStoryDirection.Mood.WARM
         },
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.WARM,
            TwoIStoryDirection.Mood.SHY,
            TwoIStoryDirection.Mood.HURT,
            TwoIStoryDirection.Mood.HURT,
            TwoIStoryDirection.Mood.BRIGHT,
            TwoIStoryDirection.Mood.PLAYFUL
         },
         new TwoIStoryDirection.Mood[]{TwoIStoryDirection.Mood.WARM, TwoIStoryDirection.Mood.PLAYFUL, TwoIStoryDirection.Mood.CURIOUS},
         TwoIStoryDirection.Mood.WARM
      );
      chapter(
         result,
         3,
         TwoIStoryDirection.Mood.UNEASY,
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.SERIOUS, TwoIStoryDirection.Mood.STARTLED, TwoIStoryDirection.Mood.SERIOUS, TwoIStoryDirection.Mood.WARM
         },
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.WARM,
            TwoIStoryDirection.Mood.SERIOUS,
            TwoIStoryDirection.Mood.SERIOUS,
            TwoIStoryDirection.Mood.HURT,
            TwoIStoryDirection.Mood.PLAYFUL,
            TwoIStoryDirection.Mood.BRIGHT
         },
         new TwoIStoryDirection.Mood[]{TwoIStoryDirection.Mood.WARM, TwoIStoryDirection.Mood.PLAYFUL, TwoIStoryDirection.Mood.PLAYFUL},
         TwoIStoryDirection.Mood.WARM
      );
      chapter(
         result,
         4,
         TwoIStoryDirection.Mood.UNEASY,
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.BRIGHT, TwoIStoryDirection.Mood.SHY, TwoIStoryDirection.Mood.UNEASY, TwoIStoryDirection.Mood.UNEASY
         },
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.BRIGHT,
            TwoIStoryDirection.Mood.WARM,
            TwoIStoryDirection.Mood.HURT,
            TwoIStoryDirection.Mood.HURT,
            TwoIStoryDirection.Mood.BRIGHT,
            TwoIStoryDirection.Mood.PLAYFUL
         },
         new TwoIStoryDirection.Mood[]{TwoIStoryDirection.Mood.WARM, TwoIStoryDirection.Mood.SERIOUS, TwoIStoryDirection.Mood.PLAYFUL},
         TwoIStoryDirection.Mood.PLAYFUL
      );
      chapter(
         result,
         5,
         TwoIStoryDirection.Mood.UNEASY,
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.WARM, TwoIStoryDirection.Mood.UNEASY, TwoIStoryDirection.Mood.CALM, TwoIStoryDirection.Mood.WARM
         },
         new TwoIStoryDirection.Mood[]{
            TwoIStoryDirection.Mood.PLAYFUL,
            TwoIStoryDirection.Mood.WARM,
            TwoIStoryDirection.Mood.HURT,
            TwoIStoryDirection.Mood.HURT,
            TwoIStoryDirection.Mood.CALM,
            TwoIStoryDirection.Mood.PLAYFUL
         },
         new TwoIStoryDirection.Mood[]{TwoIStoryDirection.Mood.WARM, TwoIStoryDirection.Mood.SERIOUS, TwoIStoryDirection.Mood.PLAYFUL},
         TwoIStoryDirection.Mood.WARM
      );
      return Map.copyOf(result);
   }

   private static void chapter(
      Map<String, TwoIStoryDirection.Mood> result,
      int chapter,
      TwoIStoryDirection.Mood preface,
      TwoIStoryDirection.Mood[] opening,
      TwoIStoryDirection.Mood[] branches,
      TwoIStoryDirection.Mood[] choices,
      TwoIStoryDirection.Mood ending
   ) {
      String prefix = "gui.godofthings.two_i.vn." + chapter + ".";
      result.put(prefix + "mood", preface);
      result.put(prefix + "ending.1", ending);

      for (int i = 0; i < opening.length; i++) {
         result.put(prefix + "opening." + (i + 1), opening[i]);
      }

      for (int i = 0; i < choices.length; i++) {
         result.put(prefix + "choice." + i, choices[i]);
         result.put(prefix + "branch." + i + ".1", branches[i * 2]);
         result.put(prefix + "branch." + i + ".2", branches[i * 2 + 1]);
      }
   }

   public static enum Mood {
      WARM,
      SHY,
      BRIGHT,
      PLAYFUL,
      CURIOUS,
      SERIOUS,
      UNEASY,
      HURT,
      STARTLED,
      CALM;
   }
}
