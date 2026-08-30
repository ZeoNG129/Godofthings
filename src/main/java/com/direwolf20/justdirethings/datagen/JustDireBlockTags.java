package com.direwolf20.justdirethings.datagen;

import com.direwolf20.justdirethings.JustDireThings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 精简版的方块标签常量（原模组为 datagen BlockTagsProvider，此处只保留 5 个机器实际用到的 TagKey）。
 */
public class JustDireBlockTags {
    public static final TagKey<Block> NO_AUTO_CLICK = BlockTags
            .create(ResourceLocation.tryBuild(JustDireThings.MODID, "noautoclick"));
    public static final TagKey<Block> SWAPPERDENY = BlockTags
            .create(ResourceLocation.tryBuild(JustDireThings.MODID, "swapper_deny"));
    public static final TagKey<Block> NO_MOVE = BlockTags
            .create(ResourceLocation.tryBuild("forge", "relocation_not_supported"));
    public static final TagKey<Block> TICK_SPEED_DENY = BlockTags
            .create(ResourceLocation.tryBuild(JustDireThings.MODID, "tick_speed_deny"));
}
