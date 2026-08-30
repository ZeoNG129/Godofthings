package com.direwolf20.justdirethings.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.awt.*;

/**
 * 精简版渲染辅助：只保留区域预览所需的 renderBoxSolid（半透明方块面）与
 * renderLines（AABB 边框线）两个方法。
 */
public class RenderHelpers {
	public static void renderLines(PoseStack matrix, AABB aabb, Color color, MultiBufferSource buffer) {
		float x = (float) aabb.minX;
		float y = (float) aabb.minY;
		float z = (float) aabb.minZ;
		float dx = (float) aabb.maxX;
		float dy = (float) aabb.maxY;
		float dz = (float) aabb.maxZ;

		VertexConsumer builder = buffer.getBuffer(RenderType.lines());

		matrix.pushPose();
		Matrix4f matrix4f = matrix.last().pose();
		Matrix3f matrix3f = matrix.last().normal();
		int colorRGB = color.getRGB();

		builder.vertex(matrix4f, x, y, z).color(colorRGB).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, dx, y, z).color(colorRGB).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, x, y, z).color(colorRGB).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, x, dy, z).color(colorRGB).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, x, y, z).color(colorRGB).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
		builder.vertex(matrix4f, x, y, dz).color(colorRGB).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
		builder.vertex(matrix4f, dx, y, z).color(colorRGB).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, dx, dy, z).color(colorRGB).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, dx, dy, z).color(colorRGB).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, x, dy, z).color(colorRGB).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, x, dy, z).color(colorRGB).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
		builder.vertex(matrix4f, x, dy, dz).color(colorRGB).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
		builder.vertex(matrix4f, x, dy, dz).color(colorRGB).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, x, y, dz).color(colorRGB).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, x, y, dz).color(colorRGB).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, dx, y, dz).color(colorRGB).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, dx, y, dz).color(colorRGB).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
		builder.vertex(matrix4f, dx, y, z).color(colorRGB).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
		builder.vertex(matrix4f, x, dy, dz).color(colorRGB).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, dx, dy, dz).color(colorRGB).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, dx, y, dz).color(colorRGB).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, dx, dy, dz).color(colorRGB).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
		builder.vertex(matrix4f, dx, dy, z).color(colorRGB).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
		builder.vertex(matrix4f, dx, dy, dz).color(colorRGB).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();

		matrix.popPose();
	}

	public static void renderBoxSolid(Matrix4f matrix, MultiBufferSource buffer, AABB aabb, float r, float g, float b,
			float alpha) {
		float startX = (float) aabb.minX;
		float startY = (float) aabb.minY;
		float startZ = (float) aabb.minZ;
		float endX = (float) aabb.maxX;
		float endY = (float) aabb.maxY;
		float endZ = (float) aabb.maxZ;

		VertexConsumer builder = buffer.getBuffer(OurRenderTypes.SolidBoxArea);

		// down
		builder.vertex(matrix, startX, startY, startZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, startY, startZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, startY, endZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, startX, startY, endZ).color(r, g, b, alpha).endVertex();

		// up
		builder.vertex(matrix, startX, endY, startZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, startX, endY, endZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, endY, endZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, endY, startZ).color(r, g, b, alpha).endVertex();

		// east
		builder.vertex(matrix, startX, startY, startZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, startX, endY, startZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, endY, startZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, startY, startZ).color(r, g, b, alpha).endVertex();

		// west
		builder.vertex(matrix, startX, startY, endZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, startY, endZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, endY, endZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, startX, endY, endZ).color(r, g, b, alpha).endVertex();

		// south
		builder.vertex(matrix, endX, startY, startZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, endY, startZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, endY, endZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, endX, startY, endZ).color(r, g, b, alpha).endVertex();

		// north
		builder.vertex(matrix, startX, startY, startZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, startX, startY, endZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, startX, endY, endZ).color(r, g, b, alpha).endVertex();
		builder.vertex(matrix, startX, endY, startZ).color(r, g, b, alpha).endVertex();
	}
}
