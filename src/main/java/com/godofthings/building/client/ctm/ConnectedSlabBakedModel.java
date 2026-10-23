package com.godofthings.building.client.ctm;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

/** The same CTM surface mapped onto a slab, with its inset face in the unculled quad group (ported from AE2 Lightning Tech). */
public final class ConnectedSlabBakedModel extends ConnectedTextureBakedModel {
    private final SlabType type;

    public ConnectedSlabBakedModel(TextureAtlasSprite base, TextureAtlasSprite ctm,
            @Nullable TextureAtlasSprite overlay, ChunkRenderTypeSet renderTypes,
            boolean ambientOcclusion, boolean gui3d, boolean usesBlockLight, ItemTransforms transforms, SlabType type) {
        super(base, ctm, overlay, ConnectionPredicates.SAME_SLAB, renderTypes,
                ambientOcclusion, gui3d, usesBlockLight, transforms);
        this.type = type;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
            ModelData data, @Nullable RenderType renderType) {
        if (type == SlabType.DOUBLE) return super.getQuads(state, side, random, data, renderType);
        Direction insetFace = type == SlabType.BOTTOM ? Direction.UP : Direction.DOWN;
        // A neighbour a whole block away cannot occlude the face at y = 0.5.
        if (side == insetFace) return List.of();
        var quads = super.getQuads(state, side == null ? insetFace : side, random, data, renderType);
        var result = new ArrayList<BakedQuad>(quads.size());
        for (var quad : quads) {
            int[] vertices = quad.getVertices().clone();
            int stride = vertices.length / 4;
            for (int v = 0; v < 4; v++) {
                float y = Float.intBitsToFloat(vertices[v * stride + 1]);
                vertices[v * stride + 1] = Float.floatToRawIntBits(y * 0.5F + (type == SlabType.TOP ? 0.5F : 0));
            }
            result.add(new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(),
                    quad.isShade(), quad.hasAmbientOcclusion()));
        }
        return result;
    }
}
