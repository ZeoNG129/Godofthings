package com.godofthings.mept.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Set;

/**
 * Utility class for rendering rainbow gradient preview boxes.
 * Provides consistent time-based rainbow gradient rendering across placement tools.
 * 
 * Uses 8-bit color depth (256 steps) for smooth transitions.
 */
public class MeptRainbowRenderHelper {
    
    private MeptRainbowRenderHelper() {}
    
    // Time-based animation speed (full cycle every ~3 seconds for smoother feel)
    private static final float CYCLE_DURATION_MS = 3000.0f;  // 3 seconds per full rainbow cycle
    
    /**
     * Get rainbow color based on current time using HSV to RGB conversion.
     * Uses full 8-bit color depth (256 hue steps) for ultra-smooth transitions.
     * 
     * @return float array {red, green, blue} in range 0-1
     */
    public static float[] getTimeBasedRainbowColor() {
        // Get current time and calculate hue (0-1 range, full cycle)
        float time = (System.currentTimeMillis() % (long)CYCLE_DURATION_MS) / CYCLE_DURATION_MS;
        
        // Convert hue to RGB using HSV (Saturation=1, Value=1)
        return hsvToRgb(time, 1.0f, 1.0f);
    }
    
    /**
     * Convert HSV (Hue, Saturation, Value) to RGB.
     * This provides smooth 8-bit color transitions through the full rainbow spectrum.
     * 
     * @param h Hue (0-1, where 0=red, 0.33=green, 0.66=blue, 1=red)
     * @param s Saturation (0-1)
     * @param v Value/Brightness (0-1)
     * @return float array {r, g, b} in range 0-1
     */
    private static float[] hsvToRgb(float h, float s, float v) {
        float r, g, b;
        
        int i = (int)(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            case 5: r = v; g = p; b = q; break;
            default: r = v; g = t; b = p; break;
        }
        
        return new float[] {r, g, b};
    }
    
    /**
     * Legacy method for compatibility - now uses time-based color.
     */
    public static float[] getRainbowColor(double x, double y, double z) {
        return getTimeBasedRainbowColor();
    }
    
    /**
     * Legacy method for compatibility - now uses time-based color.
     */
    public static float[] getRainbowColor(BlockPos pos) {
        return getTimeBasedRainbowColor();
    }
    
    /**
     * Draw a line with time-based rainbow color.
     */
    public static void drawGradientLine(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float alpha) {
        
        // Get time-based rainbow color (same color for entire line)
        float[] color = getTimeBasedRainbowColor();
        
        // Calculate normal direction for the line
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length > 0) {
            dx /= length;
            dy /= length;
            dz /= length;
        }
        
        // Draw line with time-based color
        buffer.addVertex(pose, x1, y1, z1).setColor(color[0], color[1], color[2], alpha).setNormal(pose, dx, dy, dz);
        buffer.addVertex(pose, x2, y2, z2).setColor(color[0], color[1], color[2], alpha).setNormal(pose, dx, dy, dz);
    }
    
    /**
     * Render a rainbow line box for a single block position.
     * Uses time-based rainbow color that animates smoothly.
     */
    public static void renderRainbowLineBox(
            PoseStack poseStack,
            VertexConsumer buffer,
            BlockPos pos,
            double camX, double camY, double camZ,
            float alpha) {
        
        AABB aabb = new AABB(pos).move(-camX, -camY, -camZ);
        float[] color = getTimeBasedRainbowColor();
        
        // Use standard LevelRenderer with time-based rainbow color
        LevelRenderer.renderLineBox(poseStack, buffer, aabb, color[0], color[1], color[2], alpha);
    }
    
    /**
     * Render rainbow gradient line boxes for a set of block positions.
     * All blocks share the same time-based rainbow color.
     */
    public static void renderRainbowLineBoxes(
            PoseStack poseStack,
            MultiBufferSource buffers,
            Set<BlockPos> blocks,
            Camera camera,
            float alpha) {
        
        VertexConsumer lineBuilder = buffers.getBuffer(RenderType.lines());
        double camX = camera.getPosition().x;
        double camY = camera.getPosition().y;
        double camZ = camera.getPosition().z;
        
        for (BlockPos block : blocks) {
            renderRainbowLineBox(poseStack, lineBuilder, block, camX, camY, camZ, alpha);
        }
    }
    
    /**
     * Render AABB boxes with time-based rainbow color.
     */
    public static void renderRainbowBoxes(
            PoseStack poseStack,
            VertexConsumer buffer,
            BlockPos pos,
            List<AABB> boxes,
            double camX, double camY, double camZ,
            float alpha) {
        
        float[] color = getTimeBasedRainbowColor();
        
        for (AABB box : boxes) {
            VoxelShape shape = Shapes.create(box);
            renderShape(
                    poseStack,
                    buffer,
                    shape,
                    pos.getX() - camX,
                    pos.getY() - camY,
                    pos.getZ() - camZ,
                    color[0],
                    color[1],
                    color[2],
                    alpha);
        }
    }
    
    // Copied from LevelRenderer.renderShape since it is private in 1.21.1
    private static void renderShape(
            PoseStack poseStack,
            VertexConsumer consumer,
            VoxelShape shape,
            double x,
            double y,
            double z,
            float red,
            float green,
            float blue,
            float alpha) {
        PoseStack.Pose last = poseStack.last();
        shape.forAllEdges(
                (k, l, m, n, o, p) -> {
                    float q = (float)(n - k);
                    float r = (float)(o - l);
                    float s = (float)(p - m);
                    float t = net.minecraft.util.Mth.sqrt(q * q + r * r + s * s);
                    q /= t;
                    r /= t;
                    s /= t;
                    consumer.addVertex(last, (float)(k + x), (float)(l + y), (float)(m + z))
                            .setColor(red, green, blue, alpha)
                            .setNormal(last, q, r, s);
                    consumer.addVertex(last, (float)(n + x), (float)(o + y), (float)(p + z))
                            .setColor(red, green, blue, alpha)
                            .setNormal(last, q, r, s);
                });
    }
}
