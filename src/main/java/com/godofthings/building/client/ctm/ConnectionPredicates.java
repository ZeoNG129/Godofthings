package com.godofthings.building.client.ctm;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

import com.godofthings.Godofthings;

/**
 * Registry of named {@link ConnectionPredicate}s referenced by the {@code connection}
 * field of a connected-texture model. Ported from AE2 Lightning Tech; only the two
 * generic predicates the decorative building blocks need are kept here.
 */
public final class ConnectionPredicates {

    private static final Map<ResourceLocation, ConnectionPredicate> REGISTRY = new HashMap<>();

    /** Generic: always active, connects to any adjacent block of the same type. */
    public static final ConnectionPredicate SAME_BLOCK = new ConnectionPredicate() {
        @Override
        public boolean isActive(BlockAndTintGetter level, BlockPos pos, BlockState self) {
            return true;
        }

        @Override
        public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, Direction dir) {
            return connects(level, pos, self, pos.relative(dir));
        }

        @Override
        public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, BlockPos neighbourPos) {
            return level.getBlockState(neighbourPos).is(self.getBlock());
        }
    };

    /** Half-slabs only join coplanar neighbours; separated upper/lower surfaces keep their borders. */
    public static final ConnectionPredicate SAME_SLAB = new ConnectionPredicate() {
        @Override
        public boolean isActive(BlockAndTintGetter level, BlockPos pos, BlockState self) {
            return true;
        }

        @Override
        public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, Direction dir) {
            return connects(level, pos, self, pos.relative(dir));
        }

        @Override
        public boolean connects(BlockAndTintGetter level, BlockPos pos, BlockState self, BlockPos neighbourPos) {
            var type = self.getValue(SlabBlock.TYPE);
            if (type != SlabType.DOUBLE
                    && neighbourPos.getY() != pos.getY()) return false;
            var neighbour = level.getBlockState(neighbourPos);
            return neighbour.is(self.getBlock()) && neighbour.getValue(SlabBlock.TYPE) == type;
        }
    };

    static {
        register(rl("same_block"), SAME_BLOCK);
        register(rl("same_slab"), SAME_SLAB);
    }

    private ConnectionPredicates() {
    }

    public static void register(ResourceLocation id, ConnectionPredicate predicate) {
        REGISTRY.put(id, predicate);
    }

    public static ConnectionPredicate get(ResourceLocation id) {
        return REGISTRY.getOrDefault(id, SAME_BLOCK);
    }

    private static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, path);
    }
}
