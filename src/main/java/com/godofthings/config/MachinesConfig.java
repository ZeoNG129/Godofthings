package com.godofthings.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public class MachinesConfig {
   // 注意初始化顺序：BUILDER 先声明；所有 define 必须在 build() 之前调用；
   // SPEC 在全部 define 之后由静态块构建，否则 ConfigValue.get() 会抛 "Cannot get config value before spec is built"。
   private static final Builder BUILDER = new Builder();
   public static final ModConfigSpec SPEC;

   public static final IntValue MINER_MAX_RADIUS;
   public static final IntValue MINER_MAX_BLOCKS_PER_TICK;
   public static final IntValue MINER_TICKS_PER_COLUMN_BASE;
   public static final IntValue RESOURCE_WORK_INTERVAL;
   public static final IntValue DROP_WORK_INTERVAL;

   static {
      BUILDER.push("miner");
      MINER_MAX_RADIUS = BUILDER.comment("神之矿机最大挖掘半径（方形半径，格）。").defineInRange("maxRadius", 1600, 1, 100000);
      MINER_MAX_BLOCKS_PER_TICK = BUILDER.comment("神之矿机每个 tick 最多处理的方块数，防止卡顿。")
         .defineInRange("maxBlocksPerTick", 131072, 1, 10000000);
      MINER_TICKS_PER_COLUMN_BASE = BUILDER.comment("神之矿机挖一整列的基础 tick 数（效率每级 -4，最低 1）。")
         .defineInRange("ticksPerColumnBase", 20, 1, 100000);
      BUILDER.pop();

      BUILDER.push("resourceMachine");
      RESOURCE_WORK_INTERVAL = BUILDER.comment("神之资源机工作间隔（tick）：每 N tick 处理输入槽 1 个物品。")
         .defineInRange("workInterval", 20, 1, 100000);
      BUILDER.pop();

      BUILDER.push("dropMachine");
      DROP_WORK_INTERVAL = BUILDER.comment("神之掉落机工作间隔（tick）：每 N tick 处理刷怪蛋一次。")
         .defineInRange("workInterval", 20, 1, 100000);
      BUILDER.pop();

      SPEC = BUILDER.build();
   }

   private MachinesConfig() {
   }
}
