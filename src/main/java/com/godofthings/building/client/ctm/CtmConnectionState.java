package com.godofthings.building.client.ctm;

import net.minecraft.core.Direction;

/**
 * Pre-computed connection data for one block, carried via ModelData to the baked
 * model (ported from AE2 Lightning Tech).
 */
public record CtmConnectionState(boolean[] faceCulled, int[] edgeConnect, int[] cornerConnect) {

    public boolean culled(Direction face) {
        return faceCulled[face.get3DDataValue()];
    }

    public int edges(Direction face) {
        return edgeConnect[face.get3DDataValue()];
    }

    public int corners(Direction face) {
        return cornerConnect[face.get3DDataValue()];
    }
}
