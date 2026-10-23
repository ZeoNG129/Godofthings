package com.godofthings.doll.dialogue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

@EventBusSubscriber(
   modid = "godofthings",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public final class TwoILongStory {
   private static final TwoILongStory.Chapter EMPTY = new TwoILongStory.Chapter(List.of(), List.of(), List.of());
   private static final TwoILongStory.Chapter[] CACHE = new TwoILongStory.Chapter[6];
   private static Object resources;
   private static String language;

   private TwoILongStory() {
   }

   @SubscribeEvent
   public static void registerReload(RegisterClientReloadListenersEvent event) {
      event.registerReloadListener(new SimplePreparableReloadListener<Boolean>() {
         protected Boolean prepare(ResourceManager manager, ProfilerFiller profiler) {
            return true;
         }

         protected void apply(Boolean value, ResourceManager manager, ProfilerFiller profiler) {
            TwoILongStory.clearCache();
         }
      });
   }

   public static void clearCache() {
      Arrays.fill(CACHE, null);
      TwoIStoryDirection.clearLongDirections();
      resources = null;
   }

   public static TwoILongStory.Chapter chapter(int chapter) {
      Minecraft client = Minecraft.getInstance();
      String selected = client.getLanguageManager().getSelected();
      if (resources != client.getResourceManager() || !selected.equals(language)) {
         resources = client.getResourceManager();
         language = selected;
         Arrays.fill(CACHE, null);
         TwoIStoryDirection.clearLongDirections();
      }

      int index = Math.clamp((long)chapter, 0, 5);
      if (CACHE[index] == null) {
         CACHE[index] = load(index, selected);
      }

      return CACHE[index];
   }

   private static TwoILongStory.Chapter load(int chapter, String language) {
      ResourceManager manager = Minecraft.getInstance().getResourceManager();
      ResourceLocation localized = ResourceLocation.fromNamespaceAndPath(
         "godofthings", "dialogue/long_stories/" + language + "/chapter_" + chapter + ".json"
      );
      ResourceLocation fallback = ResourceLocation.fromNamespaceAndPath("godofthings", "dialogue/long_stories/chapter_" + chapter + ".json");
      Optional<Resource> resource = manager.getResource(localized).or(() -> manager.getResource(fallback));
      if (resource.isEmpty()) {
         return EMPTY;
      } else {
         try {
            TwoILongStory.Chapter var19;
            try (InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8)) {
               JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
               List<TwoILongStory.Round> rounds = new ArrayList<>();
               JsonArray array = root.getAsJsonArray("rounds");
               if (array != null) {
                  for (int i = 0; i < array.size(); i++) {
                     JsonObject round = array.get(i).getAsJsonObject();
                     List<TwoILongStory.Choice> choices = new ArrayList<>();
                     JsonArray options = round.getAsJsonArray("choices");
                     if (options != null) {
                        for (int option = 0; option < options.size(); option++) {
                           JsonObject value = options.get(option).getAsJsonObject();
                           choices.add(
                              new TwoILongStory.Choice(
                                 value.get("text").getAsString(), lines(chapter, "round." + i + ".branch." + option, value.getAsJsonArray("lines"))
                              )
                           );
                        }
                     }

                     rounds.add(new TwoILongStory.Round(lines(chapter, "round." + i + ".prompt", round.getAsJsonArray("prompt")), List.copyOf(choices)));
                  }
               }

               var19 = new TwoILongStory.Chapter(
                  lines(chapter, "opening", root.getAsJsonArray("opening")), List.copyOf(rounds), lines(chapter, "ending", root.getAsJsonArray("ending"))
               );
            }

            return var19;
         } catch (Exception var18) {
            LogUtils.getLogger().warn("Unable to read 2i long story chapter {}", chapter, var18);
            return EMPTY;
         }
      }
   }

   private static List<TwoIStoryScript.Line> lines(int chapter, String section, JsonArray array) {
      if (array == null) {
         return List.of();
      } else {
         List<TwoIStoryScript.Line> lines = new ArrayList<>();

         for (int i = 0; i < array.size(); i++) {
            JsonObject line = array.get(i).getAsJsonObject();
            String speaker = line.has("speaker") ? line.get("speaker").getAsString() : "2i";
            String expression = line.has("expression") ? line.get("expression").getAsString() : "calm";
            String key = "gui.godofthings.two_i.vn." + chapter + ".long." + section + "." + i;
            TwoIStoryDirection.Mood mood = line.has("mood") ? TwoIStoryDirection.Mood.valueOf(line.get("mood").getAsString()) : TwoIStoryDirection.Mood.CALM;
            TwoIStoryDirection.register(key, mood);
            lines.add(
               new TwoIStoryScript.Line(
                  key,
                  expression,
                  "narrator".equals(speaker),
                  line.has("cg") && line.get("cg").getAsBoolean(),
                  "player".equals(speaker),
                  line.get("text").getAsString()
               )
            );
         }

         return List.copyOf(lines);
      }
   }

   public static record Chapter(List<TwoIStoryScript.Line> opening, List<TwoILongStory.Round> rounds, List<TwoIStoryScript.Line> ending) {
      public TwoILongStory.Round round(int index) {
         return index >= 0 && index < this.rounds.size() ? this.rounds.get(index) : new TwoILongStory.Round(List.of(), List.of());
      }
   }

   public static record Choice(String text, List<TwoIStoryScript.Line> lines) {
   }

   public static record Round(List<TwoIStoryScript.Line> prompt, List<TwoILongStory.Choice> choices) {
      public TwoILongStory.Choice choice(int index) {
         TwoILongStory.Choice var10000;
         if (index >= 0 && index < this.choices.size()) {
            var10000 = this.choices.get(index);
         } else {
            var10000 = new TwoILongStory.Choice(switch (index) {
               case 0 -> "我愿意听你慢慢说。";
               case 1 -> "我现在不想听这些。";
               default -> "我们先歇一会儿吧。";
            }, List.of());
         }

         return var10000;
      }
   }
}
