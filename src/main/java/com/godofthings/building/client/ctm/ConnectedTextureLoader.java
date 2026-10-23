package com.godofthings.building.client.ctm;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

/**
 * Loader for {@code "loader": "godofthings:connected_texture"} models
 * (ported from AE2 Lightning Tech).
 *
 * <p>Recognised fields: {@code connection} (predicate id, default
 * {@code godofthings:same_block}), {@code render_type} (default translucent),
 * {@code ambientocclusion}, {@code gui3d}, and {@code uses_block_light}.
 * Optional {@code slab_type} selects bottom, top or double slab geometry.
 * Textures come from the standard {@code textures} block (keys {@code base},
 * {@code ctm}, and optional transparent {@code overlay}).</p>
 */
public class ConnectedTextureLoader implements IGeometryLoader<ConnectedTextureGeometry> {

    @Override
    public ConnectedTextureGeometry read(JsonObject json, JsonDeserializationContext context) {
        ResourceLocation connection = ResourceLocation.parse(
                GsonHelper.getAsString(json, "connection", "godofthings:same_block"));
        RenderType renderType = parseRenderType(GsonHelper.getAsString(json, "render_type", "minecraft:translucent"));
        boolean ambientOcclusion = GsonHelper.getAsBoolean(json, "ambientocclusion", true);
        boolean gui3d = GsonHelper.getAsBoolean(json, "gui3d", true);
        boolean usesBlockLight = GsonHelper.getAsBoolean(json, "uses_block_light", true);
        var slabType = json.has("slab_type")
                ? SlabType.valueOf(
                        GsonHelper.getAsString(json, "slab_type").toUpperCase(java.util.Locale.ROOT)) : null;
        return new ConnectedTextureGeometry(connection, ChunkRenderTypeSet.of(renderType),
                ambientOcclusion, gui3d, usesBlockLight, slabType);
    }

    private static RenderType parseRenderType(String name) {
        return switch (name) {
            case "solid", "minecraft:solid" -> RenderType.solid();
            case "cutout", "minecraft:cutout" -> RenderType.cutout();
            case "cutout_mipped", "minecraft:cutout_mipped" -> RenderType.cutoutMipped();
            default -> RenderType.translucent();
        };
    }
}
