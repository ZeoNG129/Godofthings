package com.godofthings.torcherino.api;

import net.minecraft.resources.ResourceLocation;

/**
 * 标记方块所属的加速等级。
 * 移植自 Torcherino（MIT License，Copyright (c) 2021 NinjaPhenix）。
 */
public interface TierSupplier {
    ResourceLocation getTier();
}
