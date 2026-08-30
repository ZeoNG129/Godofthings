package com.godofthings.torchmaster;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;
import org.apache.commons.lang3.tuple.Pair;

public class TorchmasterConfig {
   public static final ForgeConfigSpec spec;
   public static final TorchmasterConfig.General GENERAL;

   static {
      Pair<TorchmasterConfig.General, ForgeConfigSpec> specPair = new Builder().configure(TorchmasterConfig.General::new);
      spec = (ForgeConfigSpec)specPair.getRight();
      GENERAL = (TorchmasterConfig.General)specPair.getLeft();
   }

   public static class General {
      public final ConfigValue<Boolean> beginnerTooltips;
      public final ConfigValue<Boolean> blockOnlyNaturalSpawns;
      public final ConfigValue<Boolean> blockVillageSieges;
      public final ConfigValue<Integer> megaTorchRadius;
      public final ConfigValue<Integer> dreadLampRadius;
      public final ConfigValue<List<? extends String>> megaTorchEntityBlockListOverrides;
      public final ConfigValue<List<? extends String>> dreadLampEntityBlockListOverrides;
      public final ConfigValue<Boolean> aggressiveSpawnChecks;
      public final ConfigValue<Boolean> logSpawnChecks;

      private General(Builder builder) {
         builder.push("General");
         this.beginnerTooltips = builder.comment("Show additional information in the tooltip of certain items and blocks")
            .translation("godofthings.config.beginnerTooltips.description")
            .define("beginnerTooltips", true);
         this.blockOnlyNaturalSpawns = builder.comment(
               "By default, mega torches only block natural spawns (i.e. from low light levels). Setting this to false will also block spawns from spawners"
            )
            .translation("godofthings.config.blockOnlyNaturalSpawns.description")
            .define("blockOnlyNaturalSpawns", true);
         this.blockVillageSieges = builder.comment("If this setting is enabled, the mega torch will block village sieges from zombies")
            .translation("godofthings.config.villagesiege.description")
            .define("blockVillageSieges", true);
         this.megaTorchRadius = builder.comment("The radius of the mega torch in each direction (cube) with the torch at its center")
            .translation("godofthings.config.megaTorchRadius.description")
            .defineInRange("megaTorchRadius", 64, 0, Integer.MAX_VALUE);
         this.dreadLampRadius = builder.comment("The radius of the dread lamp in each direction (cube) with the torch at its center")
            .translation("godofthings.config.dreadLampRadius.description")
            .defineInRange("dreadLampRadius", 64, 0, Integer.MAX_VALUE);
         this.megaTorchEntityBlockListOverrides = builder.comment(
               new String[]{
                  "Use this setting to override the internal lists for entity blocking",
                  "You can use this to block more entities or even allow certain entities to still spawn",
                  "The + prefix will add the entity to the list, effectivly denying its spawns",
                  "The - prefix will remove the entity from the list (if necessary), effectivly allowing its spawns",
                  "Note: Each entry needs to be put in quotes! Multiple Entries should be separated by comma.",
                  "Block zombies: \"+minecraft:zombie\"",
                  "Allow creepers: \"-minecraft:creeper\""
               }
            )
            .translation("godofthings.config.megaTorch.blockListOverrides.description")
            .defineList("megaTorchEntityBlockListOverrides", new ArrayList(), o -> o instanceof String);
         this.dreadLampEntityBlockListOverrides = builder.comment(
               new String[]{
                  "Same as the mega torch block list override, just for the dread lamp", "Block squid: +minecraft:squid", "Allow pigs: -minecraft:pig"
               }
            )
            .translation("godofthings.config.dreadLamp.blockListOverrides.description")
            .defineList("dreadLampEntityBlockListOverrides", new ArrayList(), o -> o instanceof String);
         this.logSpawnChecks = builder.comment("Print entity spawn checks to the debug log")
            .translation("godofthings.config.logSpawnChecks.description")
            .define("logSpawnChecks", false);
         this.aggressiveSpawnChecks = builder.comment(
               "Configures the spawn check to be more aggressive, effectivly overriding the CheckSpawn results of other mods"
            )
            .translation("godofthings.config.aggressiveSpawnChecks.description")
            .define("aggressiveSpawnChecks", false);
         builder.pop();
      }
   }
}
