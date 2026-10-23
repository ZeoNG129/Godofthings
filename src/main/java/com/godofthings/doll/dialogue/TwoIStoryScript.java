package com.godofthings.doll.dialogue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;

public final class TwoIStoryScript {
   private static final String PREFIX = "gui.godofthings.two_i.vn.";

   private TwoIStoryScript() {
   }

   public static List<TwoIStoryScript.Line> opening(int chapter, int bond) {
      int story = chapterIndex(chapter);
      List<TwoIStoryScript.Line> lines = new ArrayList<>();
      lines.add(line(story, "opening.0", "calm", true, true));
      if (bond < 0) {
         lines.add(line(story, "mood", "confused", false, false));
      }

      switch (story) {
         case 0:
            lines.add(line(story, "opening.1", "shy", false, false));
            lines.add(line(story, "opening.2", "surprised", false, false));
            lines.add(line(story, "opening.3", "tsundere", false, false));
            lines.add(line(story, "opening.4", "smile", false, false));
            break;
         case 1:
            lines.add(line(story, "opening.1", "determined", false, false));
            lines.add(line(story, "opening.2", "guilty", false, false));
            lines.add(line(story, "opening.3", "happy", false, false));
            lines.add(line(story, "opening.4", "confused", false, false));
            break;
         case 2:
            lines.add(line(story, "opening.1", "smile", false, false));
            lines.add(line(story, "opening.2", "shy_alt", false, false));
            lines.add(line(story, "opening.3", "determined", false, false));
            lines.add(line(story, "opening.4", "calm", false, false));
            break;
         case 3:
            lines.add(line(story, "opening.1", "surprised", false, false));
            lines.add(line(story, "opening.2", "confused", false, false));
            lines.add(line(story, "opening.3", "tsundere", false, false));
            lines.add(line(story, "opening.4", "shy", false, false));
            break;
         case 4:
            lines.add(line(story, "opening.1", "determined", false, false));
            lines.add(line(story, "opening.2", "guilty", false, false));
            lines.add(line(story, "opening.3", "happy", false, false));
            lines.add(line(story, "opening.4", "smile", false, false));
            break;
         default:
            lines.add(line(story, "opening.1", "shy_alt", false, false));
            lines.add(line(story, "opening.2", "calm", false, false));
            lines.add(line(story, "opening.3", "determined", false, false));
            lines.add(line(story, "opening.4", "smile", false, false));
      }

      return List.copyOf(lines);
   }

   public static List<TwoIStoryScript.Line> branch(int chapter, int choice) {
      int story = chapterIndex(chapter);
      int answer = choiceIndex(choice);
      String path = "branch." + answer + ".";

      String[] expressions = switch (answer) {
         case 0 -> new String[]{"surprised", "smile", "shy_alt", "happy"};
         case 1 -> new String[]{"surprised", "confused", "tsundere", "calm"};
         default -> new String[]{"confused", "determined", "guilty", "smile"};
      };
      return List.of(
         new TwoIStoryScript.Line("gui.godofthings.two_i.vn." + story + ".choice." + answer, "calm", false, false, true),
         line(story, path + "0", expressions[0], true, false),
         line(story, path + "1", expressions[1], false, false),
         line(story, path + "2", expressions[2], false, false),
         line(story, path + "3", expressions[3], true, false)
      );
   }

   public static List<TwoIStoryScript.Line> prompt(int chapter, int round) {
      TwoILongStory.Chapter authored = TwoILongStory.chapter(chapterIndex(chapter));
      List<TwoIStoryScript.Line> lines = new ArrayList<>();
      if (round == 1) {
         lines.addAll(authored.opening());
      }

      lines.addAll(authored.round(round).prompt());
      return lines.isEmpty() ? List.of(line(chapterIndex(chapter), "opening.4", "calm", false, false)) : lines;
   }

   public static List<TwoIStoryScript.Line> branch(int chapter, int round, int choice) {
      if (round == 0) {
         return branch(chapter, choice);
      } else {
         TwoILongStory.Choice option = TwoILongStory.chapter(chapterIndex(chapter)).round(round).choice(choiceIndex(choice));
         List<TwoIStoryScript.Line> lines = new ArrayList<>();
         String key = "gui.godofthings.two_i.vn." + chapterIndex(chapter) + ".long.choice." + round + "." + choice;

         TwoIStoryDirection.register(key, switch (choice) {
            case 0 -> TwoIStoryDirection.Mood.WARM;
            case 1 -> TwoIStoryDirection.Mood.SERIOUS;
            default -> TwoIStoryDirection.Mood.CURIOUS;
         });
         lines.add(new TwoIStoryScript.Line(key, "calm", false, false, true, option.text()));
         lines.addAll(option.lines());
         return List.copyOf(lines);
      }
   }

   public static List<TwoIStoryScript.Line> ending(int chapter) {
      int story = chapterIndex(chapter);
      List<TwoIStoryScript.Line> lines = new ArrayList<>();
      lines.add(line(story, "ending.0", "calm", true, true));
      lines.add(line(story, "ending.1", "smile", false, true));
      lines.addAll(TwoILongStory.chapter(story).ending());
      return List.copyOf(lines);
   }

   public static Component title(int chapter) {
      return Component.translatable("gui.godofthings.two_i.vn." + chapterIndex(chapter) + ".title");
   }

   public static Component choice(int chapter, int choice) {
      return Component.translatable("gui.godofthings.two_i.vn." + chapterIndex(chapter) + ".choice." + choiceIndex(choice));
   }

   public static Component choice(int chapter, int round, int choice) {
      return (Component)(round == 0
         ? choice(chapter, choice)
         : Component.literal(TwoILongStory.chapter(chapterIndex(chapter)).round(round).choice(choiceIndex(choice)).text()));
   }

   public static Component text(TwoIStoryScript.Line line) {
      return line.fallback() == null ? Component.translatable(line.key()) : Component.translatableWithFallback(line.key(), line.fallback());
   }

   private static TwoIStoryScript.Line line(int chapter, String key, String expression, boolean narration, boolean cg) {
      return new TwoIStoryScript.Line("gui.godofthings.two_i.vn." + chapter + "." + key, expression, narration, cg);
   }

   private static int chapterIndex(int chapter) {
      return Math.max(0, Math.min(5, chapter));
   }

   private static int choiceIndex(int choice) {
      if (choice >= 0 && choice <= 2) {
         return choice;
      } else {
         throw new IllegalArgumentException("Unknown story choice: " + choice);
      }
   }

   public static record Line(String key, String expression, boolean narration, boolean cg, boolean player, String fallback) {
      public Line(String key, String expression, boolean narration, boolean cg) {
         this(key, expression, narration, cg, false, null);
      }

      public Line(String key, String expression, boolean narration, boolean cg, boolean player) {
         this(key, expression, narration, cg, player, null);
      }
   }
}
