package com.godofthings.wondrous.compat;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.neoforged.fml.ModList;

/** Optional adapters shared by single-target and range acceleration, with no hard mod dependency. */
public final class MachineAccelerationCompat {
    private static final boolean OCCULTISM = ModList.get().isLoaded("occultism");
    private static final boolean RFTOOLS = ModList.get().isLoaded("rftoolsbuilder");

    private MachineAccelerationCompat() { }

    /** A nullable answer separates an authoritative idle state from an unsupported target. */
    public static Boolean knownActivity(BlockEntity target) {
        return null /* [本地改动] Occultism 适配未移植（无可编译目标类）；null=未知，绝不让机器误判休眠 */;
    }

    public static boolean isWorking(BlockEntity target) {
        return false
                ;
    }

    public static BlockEntityTicker<BlockEntity> ticker(BlockEntity target) {
        return null;
    }

    public static boolean advanceSpirit(net.minecraft.world.entity.LivingEntity target, int extra) {
        return false;
    }
}
