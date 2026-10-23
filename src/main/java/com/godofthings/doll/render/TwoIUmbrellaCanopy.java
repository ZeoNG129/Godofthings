package com.godofthings.doll.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class TwoIUmbrellaCanopy {
   private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath("godofthings", "textures/entity/two_i_umbrella.png");
   private static final float SECTOR = (float) (Math.PI / 4);
   public static final float CANOPY_RADIUS = 18.5F;
   private static final int[] FABRIC = new int[]{16249061, 11389422};
   private static final List<TwoIUmbrellaCanopy.Quad> MESH = build();

   private TwoIUmbrellaCanopy() {
   }

   private static TwoIUmbrellaCanopy.Point surface(float radius, float angle, float offset) {
      float fraction = angle / (float) (Math.PI / 4) - (float)Math.floor((double)(angle / (float) (Math.PI / 4)));
      float within = (fraction - 0.5F) * (float) (Math.PI / 4);
      float edge = 18.5F * (float)Math.cos((float) (Math.PI / 8)) / (float)Math.cos((double)within);
      float y = -0.45F + 4.9F * (float)Math.pow((double)radius, 1.65) + 0.38F * (float)Math.sin((double)fraction * Math.PI) * radius * radius;
      return new TwoIUmbrellaCanopy.Point(edge * radius * (float)Math.cos((double)angle), y + offset, edge * radius * (float)Math.sin((double)angle));
   }

   private static List<TwoIUmbrellaCanopy.Quad> build() {
      List<TwoIUmbrellaCanopy.Quad> mesh = new ArrayList<>();

      for (int panel = 0; panel < 8; panel++) {
         for (int arc = 0; arc < 4; arc++) {
            float a = ((float)panel + (float)arc / 4.0F) * (float) (Math.PI / 4);
            float b = ((float)panel + (float)(arc + 1) / 4.0F) * (float) (Math.PI / 4);

            for (int ring = 0; ring < 4; ring++) {
               float inner = (float)ring / 4.0F;
               float outer = (float)(ring + 1) / 4.0F;
               quad(mesh, surface(inner, a, 0.0F), surface(inner, b, 0.0F), surface(outer, b, 0.0F), surface(outer, a, 0.0F), FABRIC[panel % 2]);
            }

            quad(mesh, surface(0.962F, a, -0.035F), surface(0.962F, b, -0.035F), surface(1.0F, b, -0.035F), surface(1.0F, a, -0.035F), 16054271);
         }

         float angle = (float)panel * (float) (Math.PI / 4);

         for (int ring = 0; ring < 4; ring++) {
            float inner = Math.max(0.02F, (float)ring / 4.0F);
            float outer = (float)(ring + 1) / 4.0F * 0.99F;
            quad(
               mesh,
               surface(inner, angle - 0.006F, 0.08F),
               surface(inner, angle + 0.006F, 0.08F),
               surface(outer, angle + 0.006F, 0.08F),
               surface(outer, angle - 0.006F, 0.08F),
               4608363
            );
         }

         float middle = ((float)panel + 0.5F) * (float) (Math.PI / 4);
         TwoIUmbrellaCanopy.Point center = surface(0.71F, middle, -0.045F);

         for (int point = 0; point < 8; point++) {
            TwoIUmbrellaCanopy.Point a = starPoint(middle, point);
            TwoIUmbrellaCanopy.Point b = starPoint(middle, (point + 1) % 8);
            quad(mesh, center, a, b, b, 14270338);
         }
      }

      return List.copyOf(mesh);
   }

   private static TwoIUmbrellaCanopy.Point starPoint(float angle, int point) {
      float phase = (float)point * (float) (Math.PI / 4);
      float length = point % 2 == 0 ? 1.0F : 0.32F;
      return surface(0.71F + (float)Math.cos((double)phase) * length * 0.075F, angle + (float)Math.sin((double)phase) * length * 0.095F, -0.045F);
   }

   private static void quad(
      List<TwoIUmbrellaCanopy.Quad> mesh,
      TwoIUmbrellaCanopy.Point a,
      TwoIUmbrellaCanopy.Point b,
      TwoIUmbrellaCanopy.Point c,
      TwoIUmbrellaCanopy.Point d,
      int color
   ) {
      float ux = c.x - a.x;
      float uy = c.y - a.y;
      float uz = c.z - a.z;
      float vx = d.x - b.x;
      float vy = d.y - b.y;
      float vz = d.z - b.z;
      float nx = uy * vz - uz * vy;
      float ny = uz * vx - ux * vz;
      float nz = ux * vy - uy * vx;
      float length = Math.max(1.0E-5F, (float)Math.sqrt((double)(nx * nx + ny * ny + nz * nz)));
      if (ny > 0.0F) {
         length = -length;
      }

      mesh.add(new TwoIUmbrellaCanopy.Quad(a, b, c, d, nx / length, ny / length, nz / length, color));
   }

   public static void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
      VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));

      for (TwoIUmbrellaCanopy.Quad quad : MESH) {
         vertex(vertices, pose.last(), quad.a, quad, light, overlay);
         vertex(vertices, pose.last(), quad.b, quad, light, overlay);
         vertex(vertices, pose.last(), quad.c, quad, light, overlay);
         vertex(vertices, pose.last(), quad.d, quad, light, overlay);
      }
   }

   private static void vertex(VertexConsumer vertices, Pose pose, TwoIUmbrellaCanopy.Point p, TwoIUmbrellaCanopy.Quad quad, int light, int overlay) {
      vertices.addVertex(pose, p.x / 16.0F, p.y / 16.0F, p.z / 16.0F)
         .setColor(quad.color >> 16 & 0xFF, quad.color >> 8 & 0xFF, quad.color & 0xFF, 255)
         .setUv(0.5F, 0.5F)
         .setOverlay(overlay)
         .setLight(light)
         .setNormal(pose, quad.nx, quad.ny, quad.nz);
   }

   private static record Point(float x, float y, float z) {
   }

   private static record Quad(
      TwoIUmbrellaCanopy.Point a, TwoIUmbrellaCanopy.Point b, TwoIUmbrellaCanopy.Point c, TwoIUmbrellaCanopy.Point d, float nx, float ny, float nz, int color
   ) {
   }
}
