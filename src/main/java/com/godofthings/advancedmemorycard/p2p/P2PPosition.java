package com.godofthings.advancedmemorycard.p2p;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record P2PPosition(
        BlockPos position,
        Direction direction,
        String dimension
) {}
