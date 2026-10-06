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
   public static final IntValue PEELER_WORK_INTERVAL;
   public static final IntValue DROP_WORK_INTERVAL;
   public static final IntValue SPAWN_EGG_WORK_INTERVAL;
   public static final IntValue TRANSMITTER_RANGE;

   static {
      BUILDER.push("miner");
      MINER_MAX_RADIUS = BUILDER.comment("神之矿机最大挖掘半径（方形半径，格）。 / God Miner max mining radius (square radius, in blocks).").defineInRange("maxRadius", 1600, 1, 100000);
      MINER_MAX_BLOCKS_PER_TICK = BUILDER.comment("神之矿机每个 tick 最多处理的方块数，防止卡顿。 / Max blocks the God Miner processes per tick, to avoid lag.")
         .defineInRange("maxBlocksPerTick", 131072, 1, 10000000);
      MINER_TICKS_PER_COLUMN_BASE = BUILDER.comment("神之矿机挖一整列的基础 tick 数（效率每级 -4，最低 1）。 / Base ticks for the God Miner to mine one full column (-4 per efficiency level, minimum 1).")
         .defineInRange("ticksPerColumnBase", 20, 1, 100000);
      BUILDER.pop();

      BUILDER.push("resourceMachine");
      RESOURCE_WORK_INTERVAL = BUILDER.comment("神之资源机工作间隔（tick）：每 N tick 处理输入槽 1 个物品。 / God Resource Machine work interval (ticks): processes 1 item from the input slot every N ticks.")
         .defineInRange("workInterval", 20, 1, 100000);
      BUILDER.pop();

      BUILDER.push("peeler");
      PEELER_WORK_INTERVAL = BUILDER.comment("神之去皮工作间隔（tick）：每 N tick 去皮 1 个原木。 / God Peeler work interval (ticks): strips 1 log every N ticks.")
         .defineInRange("workInterval", 5, 1, 100000);
      BUILDER.pop();

      BUILDER.push("dropMachine");
      DROP_WORK_INTERVAL = BUILDER.comment("神之掉落机工作间隔（tick）：每 N tick 处理刷怪蛋一次。 / God Drop Machine work interval (ticks): processes one spawn egg every N ticks.")
         .defineInRange("workInterval", 20, 1, 100000);
      BUILDER.pop();

      BUILDER.push("spawnEggMachine");
      SPAWN_EGG_WORK_INTERVAL = BUILDER.comment("神之怪蛋工作间隔（tick）：每 N tick 复制一轮刷怪蛋（每种 64 个 × 并行倍率）。 / God Spawn Egg Machine work interval (ticks): duplicates the spawn eggs once per N ticks (64 per type x parallel multiplier).")
         .defineInRange("workInterval", 20, 1, 100000);
      BUILDER.pop();

      BUILDER.push("transmitter");
      TRANSMITTER_RANGE = BUILDER.comment("神之传输的无线充能范围（方块）。0 = 无限距离（同维度内不限距离；跨维度仍由机器界面上的跨维度开关控制）。 / God Transmitter wireless charging range in blocks. 0 = unlimited distance (same dimension only; cross-dimension is still controlled by the transmitter's own cross-dimension toggle).")
         .defineInRange("range", 64, 0, 1000000);
      BUILDER.pop();

      SPEC = BUILDER.build();
   }

   private MachinesConfig() {
   }
}
