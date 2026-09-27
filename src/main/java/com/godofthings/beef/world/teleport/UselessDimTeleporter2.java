package com.godofthings.beef.world.teleport;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.init.ModBlocks;
import com.godofthings.beef.init.ModPOIs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

public class UselessDimTeleporter2 extends AbstractDimensionTeleporter {

    private static final ResourceKey<Level> DIMENSION_KEY =
            ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                               ResourceLocation.fromNamespaceAndPath(UselessMod.MODID, "uselessdim2")
            );

    public static void teleport(ServerPlayer player, BlockPos pos) {
        new UselessDimTeleporter2().handleTeleport(player, pos);
    }

    @Override
    protected ResourceKey<Level> getDimensionKey() {
        return DIMENSION_KEY;
    }

    @Override
    protected Supplier<Block> getTeleportBlock() {
        return ModBlocks.TELEPORT_BLOCK_2;
    }

    @Override
    protected Holder<PoiType> getPOI() {
        return ModPOIs.TELEPORT_PAD_2_POI;
    }
}
