package com.godofthings.omni;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;

/** 邻居偏移表：6 面 / 26 全向（原样移植自 Omni Upgrades 的 {@code NeighborOffsets}）。 */
public final class NeighborOffsets {

    private static final List<BlockPos> FACE_ONLY = build(false);
    private static final List<BlockPos> WITH_DIAGONAL = build(true);

    private NeighborOffsets() {
    }

    public static List<BlockPos> get(boolean includeDiagonal) {
        return includeDiagonal ? WITH_DIAGONAL : FACE_ONLY;
    }

    private static List<BlockPos> build(boolean includeDiagonal) {
        List<BlockPos> offsets = new ArrayList<>(includeDiagonal ? 26 : 6);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        int axes = (dx != 0 ? 1 : 0) + (dy != 0 ? 1 : 0) + (dz != 0 ? 1 : 0);
                        if (includeDiagonal || axes == 1) {
                            offsets.add(new BlockPos(dx, dy, dz));
                        }
                    }
                }
            }
        }
        return List.copyOf(offsets);
    }
}
