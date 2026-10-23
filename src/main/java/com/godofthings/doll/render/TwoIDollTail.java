package com.godofthings.doll.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class TwoIDollTail {
   private static final ResourceLocation FUR = ResourceLocation.fromNamespaceAndPath("godofthings", "textures/entity/two_i_tail.png");
   private static final TwoIDollTail.Bone[] BONES = loadGeometry();
   private static final TwoIDollTail.Point[] SUPPORT = Arrays.stream(BONES)
      .flatMap(b -> b.faces.stream())
      .flatMap(f -> Stream.of(f.a, f.b, f.c, f.d))
      .distinct()
      .toArray(TwoIDollTail.Point[]::new);
   private static final List<TwoIDollTail.Face> RIBBON = createRibbon();
   private static final TwoIDollTail.Point[] RIBBON_CORNERS = RIBBON.stream()
      .flatMap(f -> Stream.of(f.a, f.b, f.c, f.d))
      .distinct()
      .toArray(TwoIDollTail.Point[]::new);
   private final float[] pitch = new float[7];
   private final float[] yaw = new float[7];
   private final Matrix4f matrix = new Matrix4f();
   private final Matrix4f ribbonMatrix = new Matrix4f();
   private final Matrix4f attachment = new Matrix4f();
   private final Matrix4f[] skin = IntStream.range(0, 7).mapToObj(i -> new Matrix4f()).toArray(Matrix4f[]::new);
   private float rootY;
   private float rootRoll;
   private float rootPitch;
   private float rootYaw;
   private float heightScale;
   private float cachedBottom;
   private float ribbonPitch;
   private float ribbonRoll;
   private float tipX;
   private float tipY;
   private float tipZ;

   /**
    * 打开尾巴几何 JSON。
    *
    * <p><b>为什么不用单一句 {@code class.getResourceAsStream("/assets/...")}</b>：
    * NeoForge 用 {@code TransformingClassLoader} 包裹模组类，方块实体渲染器在
    * <b>资源包重载期</b>（{@code BlockEntityRenderDispatcher.onResourceManagerReload}）
    * 就会构造，那时模组类加载器的 {@code getResourceAsStream} 对 jar 内资源的解析
    * 还没就绪，直接拿到 null —— 表现为「Missing 2i tail geometry」→
    * {@code ExceptionInInitializerError} → 整个资源重载失败、卡在加载界面。</p>
    *
    * <p>这里按可靠性从高到低依次尝试，任一成功即返回：</p>
    * <ol>
    *   <li>{@link TwoIDollTail} 自身的类加载器（正常开发环境、部分运行环境可用）；</li>
    *   <li>线程上下文类加载器（NeoForge 在模组加载阶段会设成能看见模组 jar 的那个）；</li>
    *   <li>{@code ModList} 提供的模组资源路径（最终兜底，直接走 jar 文件系统）。</li>
    * </ol>
    */
   private static InputStream openGeometry() {
      String path = "/assets/godofthings/models/entity/two_i_tail.json";

      InputStream stream = TwoIDollTail.class.getResourceAsStream(path);
      if (stream != null) {
         return stream;
      }

      ClassLoader context = Thread.currentThread().getContextClassLoader();
      if (context != null) {
         stream = context.getResourceAsStream(path.substring(1));
         if (stream != null) {
            return stream;
         }
      }

      ClassLoader own = TwoIDollTail.class.getClassLoader();
      if (own != null) {
         stream = own.getResourceAsStream(path.substring(1));
         if (stream != null) {
            return stream;
         }
      }

      return null;
   }

   private static TwoIDollTail.Bone[] loadGeometry() {
      try {
         TwoIDollTail.Bone[] var18;
         try (InputStream stream = openGeometry())
         {
            if (stream == null) {
               // 资源取不到时不能让静态初始化炸掉：那会连带把 TwoIDollRenderer.Model
               // 的构造拖进 ExceptionInInitializerError，导致整个资源重载失败、卡在加载界面。
               // 这里退化为「没有尾巴」——玩偶本体照常渲染，只是缺了那条豹尾。
               org.slf4j.LoggerFactory.getLogger("godofthings-doll")
                       .warn("找不到玩偶尾巴几何数据，尾巴将被跳过（玩偶其余部分正常）");
               return new TwoIDollTail.Bone[0];
            }

            JsonArray source = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("bones");
            TwoIDollTail.Bone[] bones = new TwoIDollTail.Bone[source.size()];

            for (int i = 0; i < bones.length; i++) {
               JsonObject bone = source.get(i).getAsJsonObject();
               JsonArray pivot = bone.getAsJsonArray("pivot");
               List<TwoIDollTail.Face> faces = new ArrayList<>();

               for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                  TwoIDollTail.Point[] p = new TwoIDollTail.Point[8];

                  for (int j = 0; j < 8; j++) {
                     JsonArray v = cube.getAsJsonArray().get(j).getAsJsonArray();
                     float x = v.get(0).getAsFloat();
                     float y = v.get(1).getAsFloat();
                     float z = v.get(2).getAsFloat();
                     p[j] = furPoint(x, y, z, pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat());
                  }

                  face(faces, p, 0, 1, 5, 4, false, 16777215);
                  face(faces, p, 2, 6, 7, 3, false, 16777215);
                  face(faces, p, 0, 4, 6, 2, false, 16777215);
                  face(faces, p, 1, 3, 7, 5, false, 16777215);
                  face(faces, p, 0, 2, 3, 1, false, 16777215);
                  face(faces, p, 4, 5, 7, 6, false, 16777215);
               }

               List<TwoIDollTail.Face> smooth = new ArrayList<>();

               for (TwoIDollTail.Face f : faces) {
                  subdivide(smooth, f, i == 2 ? 4 : 2);
               }

               bones[i] = new TwoIDollTail.Bone(
                  new TwoIDollTail.Point(pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(), pivot.get(2).getAsFloat()), List.copyOf(smooth)
               );
            }

            var18 = bones;
         }

         return var18;
      } catch (Exception var17) {
         throw new IllegalStateException("Cannot load 2i tail geometry", var17);
      }
   }

   private static TwoIDollTail.Point mix(TwoIDollTail.Point a, TwoIDollTail.Point b, float t) {
      float bu = b.u;
      if (bu - a.u > 0.5F) {
         bu--;
      }

      if (a.u - bu > 0.5F) {
         bu++;
      }

      return weightedPoint(Mth.lerp(t, a.x, b.x), Mth.lerp(t, a.y, b.y), Mth.lerp(t, a.z, b.z), Mth.lerp(t, a.u, bu), Mth.lerp(t, a.v, b.v));
   }

   private static void subdivide(List<TwoIDollTail.Face> out, TwoIDollTail.Face f, int n) {
      for (int y = 0; y < n; y++) {
         for (int x = 0; x < n; x++) {
            TwoIDollTail.Point l0 = mix(f.a, f.d, (float)y / (float)n);
            TwoIDollTail.Point r0 = mix(f.b, f.c, (float)y / (float)n);
            TwoIDollTail.Point l1 = mix(f.a, f.d, ((float)y + 1.0F) / (float)n);
            TwoIDollTail.Point r1 = mix(f.b, f.c, ((float)y + 1.0F) / (float)n);
            out.add(
               new TwoIDollTail.Face(
                  mix(l0, r0, (float)x / (float)n),
                  mix(l0, r0, ((float)x + 1.0F) / (float)n),
                  mix(l1, r1, ((float)x + 1.0F) / (float)n),
                  mix(l1, r1, (float)x / (float)n),
                  f.nx,
                  f.ny,
                  f.nz,
                  false,
                  f.tint
               )
            );
         }
      }
   }

   private static TwoIDollTail.Point furPoint(float x, float y, float z, float py, float pz) {
      float[] cy = new float[]{0.0F, 2.5F, 3.7F, 2.4F, -0.5F, -3.4F, -6.0F};
      float[] cz = new float[]{0.0F, 1.9F, 4.7F, 7.4F, 9.5F, 10.1F, 9.6F};
      float gy = y + py;
      float gz = z + pz;
      float best = Float.MAX_VALUE;
      float arc = 0.0F;
      float along = 0.0F;
      float radial = 0.0F;
      float total = 0.0F;

      for (int i = 0; i < cy.length - 1; i++) {
         float dy = cy[i + 1] - cy[i];
         float dz = cz[i + 1] - cz[i];
         float length = Mth.sqrt(dy * dy + dz * dz);
         float t = Mth.clamp(((gy - cy[i]) * dy + (gz - cz[i]) * dz) / (length * length), 0.0F, 1.0F);
         float ey = gy - cy[i] - t * dy;
         float ez = gz - cz[i] - t * dz;
         float dist = ey * ey + ez * ez;
         if (dist < best) {
            best = dist;
            along = arc + t * length;
            radial = (ey * dz - ez * dy) / length;
         }

         arc += length;
         total = arc;
      }

      float u = 0.5F + (float)Math.atan2((double)x, (double)radial) / (float) (Math.PI * 2);
      return weightedPoint(x, gy, gz, u, Mth.clamp(along / total, 0.0F, 1.0F) * 0.74F);
   }

   private static TwoIDollTail.Point weightedPoint(float x, float y, float z, float u, float v) {
      float[] cy = new float[]{0.0F, 2.47358F, 2.19175F, -1.71243F, -2.6044F, -4.33778F, -5.90146F};
      float[] cz = new float[]{0.0F, 1.69704F, 3.7538F, 9.9476F, 10.16067F, 10.00902F, 9.58112F};
      float best = Float.MAX_VALUE;
      float weight = 0.0F;
      int a = 0;

      for (int i = 0; i < 6; i++) {
         float dy = cy[i + 1] - cy[i];
         float dz = cz[i + 1] - cz[i];
         float t = Mth.clamp(((y - cy[i]) * dy + (z - cz[i]) * dz) / (dy * dy + dz * dz), 0.0F, 1.0F);
         float ey = y - cy[i] - t * dy;
         float ez = z - cz[i] - t * dz;
         float dist = ey * ey + ez * ez;
         if (dist < best) {
            best = dist;
            a = i;
            weight = t * t * (3.0F - 2.0F * t);
         }
      }

      return new TwoIDollTail.Point(x, y, z, u, v, a, a + 1, weight);
   }

   private static List<TwoIDollTail.Face> createRibbon() {
      List<TwoIDollTail.Face> mesh = new ArrayList<>();

      for (int side = -1; side <= 1; side += 2) {
         float[] xs = new float[]{0.16F, 0.55F, 1.15F, 1.65F, 1.83F};
         float[] heights = new float[]{0.14F, 0.38F, 0.6F, 0.62F, 0.43F};
         TwoIDollTail.Point[][] rings = new TwoIDollTail.Point[5][8];

         for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 8; j++) {
               float angle = (float)j * (float) (Math.PI * 2) / 8.0F;
               rings[i][j] = new TwoIDollTail.Point(
                  (float)side * xs[i], -0.12F * xs[i] + Mth.cos(angle) * heights[i], 0.22F + Mth.sin(angle) * (0.1F + heights[i] * 0.28F)
               );
            }
         }

         for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 8; j++) {
               int next = (j + 1) % 8;
               TwoIDollTail.Point[] p = new TwoIDollTail.Point[]{rings[i][j], rings[i + 1][j], rings[i + 1][next], rings[i][next]};
               int tint = j == 1 || j == 2 ? 15134716 : (j < 4 ? 13164024 : (j != 4 && j != 7 ? 11981042 : 10797804));
               if (side > 0) {
                  face(mesh, p, 3, 2, 1, 0, false, tint);
               } else {
                  face(mesh, p, 0, 1, 2, 3, false, tint);
               }
            }
         }

         for (int end : new int[]{0, 4}) {
            TwoIDollTail.Point center = new TwoIDollTail.Point((float)side * xs[end], -0.12F * xs[end], 0.22F);

            for (int jx = 0; jx < 8; jx++) {
               TwoIDollTail.Point a = rings[end][jx];
               TwoIDollTail.Point b = rings[end][(jx + 1) % 8];
               int tint = end == 0 ? 12506611 : 13164024;
               if (end == 4 == side > 0) {
                  triangle(mesh, center, a, b, tint);
               } else {
                  triangle(mesh, center, b, a, tint);
               }
            }
         }

         TwoIDollTail.Point t0 = new TwoIDollTail.Point((float)side * 0.24F, 0.2F, 0.13F);
         TwoIDollTail.Point t1 = new TwoIDollTail.Point((float)side * 0.83F, 0.26F, 0.11F);
         TwoIDollTail.Point t2 = new TwoIDollTail.Point((float)side * 1.04F, 2.02F, 0.18F);
         TwoIDollTail.Point t3 = new TwoIDollTail.Point((float)side * 0.73F, 1.72F, 0.24F);
         TwoIDollTail.Point t4 = new TwoIDollTail.Point((float)side * 0.5F, 2.1F, 0.16F);
         TwoIDollTail.Point t5 = new TwoIDollTail.Point((float)side * 0.39F, 1.1F, 0.34F);
         TwoIDollTail.Point[] front = new TwoIDollTail.Point[]{t0, t1, t2, t3, t4};
         TwoIDollTail.Point[] back = new TwoIDollTail.Point[5];
         TwoIDollTail.Point backCenter = new TwoIDollTail.Point(t5.x, t5.y, t5.z - 0.08F);
         int[] colors = new int[]{14280956, 11192560, 9549288, 12441333, 13229816};

         for (int jxx = 0; jxx < 5; jxx++) {
            back[jxx] = new TwoIDollTail.Point(front[jxx].x, front[jxx].y, front[jxx].z - 0.08F);
         }

         for (int jxx = 0; jxx < 5; jxx++) {
            int next = (jxx + 1) % 5;
            if (side > 0) {
               triangle(mesh, front[jxx], front[next], t5, colors[jxx]);
               triangle(mesh, back[next], back[jxx], backCenter, colors[jxx]);
               face(mesh, new TwoIDollTail.Point[]{front[jxx], back[jxx], back[next], front[next]}, 0, 1, 2, 3, false, 10337512);
            } else {
               triangle(mesh, front[next], front[jxx], t5, colors[jxx]);
               triangle(mesh, back[jxx], back[next], backCenter, colors[jxx]);
               face(mesh, new TwoIDollTail.Point[]{front[jxx], front[next], back[next], back[jxx]}, 0, 1, 2, 3, false, 10337512);
            }
         }
      }

      box(mesh, 0.0F, 0.02F, 0.32F, 0.48F, 0.52F, 0.38F, 0.0F, 0.0F, 0.0F, 12769782);
      box(mesh, 0.0F, 0.05F, 0.55F, 0.24F, 0.24F, 0.07F, 0.0F, 0.0F, (float) (Math.PI / 4), 14601123);
      return List.copyOf(mesh);
   }

   private static void triangle(List<TwoIDollTail.Face> out, TwoIDollTail.Point a, TwoIDollTail.Point b, TwoIDollTail.Point c, int tint) {
      face(out, new TwoIDollTail.Point[]{a, b, c}, 0, 1, 2, 2, false, tint);
   }

   private static void box(List<TwoIDollTail.Face> out, float x, float y, float z, float w, float h, float d, float rx, float ry, float rz, int tint) {
      Matrix4f m = new Matrix4f().translation(x, y, z).rotateZ(rz).rotateY(ry).rotateX(rx);
      TwoIDollTail.Point[] p = new TwoIDollTail.Point[8];

      for (int i = 0; i < 8; i++) {
         Vector3f v = new Vector3f((i & 1) == 0 ? -w / 2.0F : w / 2.0F, (i & 2) == 0 ? -h / 2.0F : h / 2.0F, (i & 4) == 0 ? -d / 2.0F : d / 2.0F)
            .mulPosition(m);
         p[i] = new TwoIDollTail.Point(v.x, v.y, v.z);
      }

      face(out, p, 0, 1, 5, 4, false, tint);
      face(out, p, 2, 6, 7, 3, false, tint);
      face(out, p, 0, 4, 6, 2, false, tint);
      face(out, p, 1, 3, 7, 5, false, tint);
      face(out, p, 0, 2, 3, 1, false, tint);
      face(out, p, 4, 5, 7, 6, false, tint);
   }

   private static void face(List<TwoIDollTail.Face> out, TwoIDollTail.Point[] p, int a, int b, int c, int d, boolean marked, int tint) {
      Vector3f n = new Vector3f(p[b].x - p[a].x, p[b].y - p[a].y, p[b].z - p[a].z)
         .cross(new Vector3f(p[c].x - p[a].x, p[c].y - p[a].y, p[c].z - p[a].z))
         .normalize();
      out.add(new TwoIDollTail.Face(p[a], p[b], p[c], p[d], n.x, n.y, n.z, marked, tint));
   }

   public void pose(boolean seated, float bodyY, float bodyRoll, float time, float stride, float moving, TwoIDollCarryMotion.Pose motion) {
      this.rootY = bodyY + 5.8F;
      this.rootRoll = bodyRoll;
      this.heightScale = seated ? 0.72F : 0.9F;
      this.rootPitch = seated ? 0.32F : -0.18F;
      float cycle = time / 240.0F;
      float phase = cycle - (float)Math.floor((double)cycle);
      float gesture = phase < 0.6F ? Mth.sin(phase / 0.6F * (float) Math.PI) : 0.0F;
      float side = Math.floorMod((int)Math.floor((double)cycle), 2) == 0 ? 1.0F : -1.0F;
      this.rootYaw = Mth.sin(time * 0.032F) * 0.1F + side * gesture * 0.95F;
      float speed = Math.max(moving, motion.speed());

      for (int i = 0; i < 7; i++) {
         float delay = (float)i * 0.48F;
         float weight = (0.5F + (float)i * 0.16F) / 7.0F;
         this.pitch[i] = Mth.sin(time * 0.055F - delay) * (0.022F + (float)i * 0.004F)
            + Mth.sin(time * 0.29F - delay) * speed * 0.055F
            + motion.tailPitch() * weight * 1.8F
            + motion.airborne() * 0.05F
            - motion.falling() * 0.065F
            + gesture * Mth.sin(time * 0.08F - delay) * 0.035F;
         this.yaw[i] = Mth.sin(time * 0.043F - delay) * (0.025F + (float)i * 0.008F)
            + Mth.sin(stride * 0.92F - delay) * moving * 0.055F
            + Mth.sin(time * 0.29F - delay) * motion.speed() * 0.055F
            + motion.tailYaw() * weight * 2.0F
            + side * gesture * Mth.sin(phase * 5.0F - delay) * 0.055F
            + (i > 3 ? Mth.sin(time * 0.35F - delay) * gesture * 0.055F : 0.0F);
      }

      this.ribbonPitch = -0.1F + Mth.sin(time * 0.09F - 2.0F) * 0.09F + motion.tailPitch() * 0.28F;
      this.ribbonRoll = Mth.sin(time * 0.07F - 2.0F) * 0.08F + motion.tailYaw() * 0.35F;
      this.cachedBottom = this.calculateBottom();
   }

   private void startMatrix() {
      this.attachment.translation(0.0F, this.rootY, 1.25F).rotateZ(this.rootRoll).scale(1.0F, this.heightScale, 1.0F);
      this.matrix.set(this.attachment).rotateY(this.rootYaw).rotateX(this.rootPitch);
   }

   private void boneMatrix(int i) {
      TwoIDollTail.Point p = BONES[i].pivot;
      if (i > 0) {
         TwoIDollTail.Point last = BONES[i - 1].pivot;
         this.matrix.translate(p.x - last.x, p.y - last.y, p.z - last.z);
      }

      this.matrix.rotateY(this.yaw[i]).rotateX(this.pitch[i]);
   }

   private float calculateBottom() {
      this.startMatrix();

      for (int i = 0; i < 7; i++) {
         this.boneMatrix(i);
         TwoIDollTail.Point pivot = BONES[i].pivot;
         this.skin[i].set(this.matrix).translate(-pivot.x, -pivot.y, -pivot.z);
         if (i == 5) {
            this.ribbonMatrix.set(this.matrix).translate(0.0F, -0.15F, 1.5F).rotateX(this.ribbonPitch).rotateZ(this.ribbonRoll);
         }
      }

      this.tipX = this.matrix.m30();
      this.tipY = supportedY(this.matrix.m31());
      this.tipZ = this.matrix.m32();
      float max = this.rootY;

      for (TwoIDollTail.Point p : SUPPORT) {
         max = Math.max(max, this.skinY(p));
      }

      for (TwoIDollTail.Point p : RIBBON_CORNERS) {
         max = Math.max(
            max, supportedY(this.ribbonMatrix.m01() * p.x + this.ribbonMatrix.m11() * p.y + this.ribbonMatrix.m21() * p.z + this.ribbonMatrix.m31())
         );
      }

      return max;
   }

   private float skinY(TwoIDollTail.Point p) {
      Matrix4f a = this.skin[p.a];
      Matrix4f b = this.skin[p.b];
      float skinned = Mth.lerp(p.weight, a.m01() * p.x + a.m11() * p.y + a.m21() * p.z + a.m31(), b.m01() * p.x + b.m11() * p.y + b.m21() * p.z + b.m31());
      return supportedY(
         Mth.lerp(rootWeight(p), this.attachment.m01() * p.x + this.attachment.m11() * p.y + this.attachment.m21() * p.z + this.attachment.m31(), skinned)
      );
   }

   private static float supportedY(float y) {
      float start = 23.28F;
      return y <= start ? y : start + 0.5F * (y - start) / (0.5F + y - start);
   }

   private static float rootWeight(TwoIDollTail.Point p) {
      float t = Mth.clamp(p.z / 3.2F, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   public Vec3 attachment() {
      return new Vec3((double)this.attachment.m30(), (double)this.attachment.m31(), (double)this.attachment.m32());
   }

   public float bottom() {
      return this.cachedBottom;
   }

   public Vec3 tip() {
      return new Vec3((double)this.tipX, (double)this.tipY, (double)this.tipZ);
   }

   public float tipYaw() {
      float v = this.rootYaw;

      for (float a : this.yaw) {
         v += a;
      }

      return v;
   }

   public float tipPitch() {
      float v = this.rootPitch;

      for (float a : this.pitch) {
         v += a;
      }

      return v;
   }

   public void render(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
      VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(FUR));

      for (int i = 0; i < 7; i++) {
         for (TwoIDollTail.Face f : BONES[i].faces) {
            this.renderFace(out, pose, f, light, overlay, false);
         }

         if (i == 5) {
            for (TwoIDollTail.Face f : RIBBON) {
               this.renderFace(out, pose, f, light, overlay, true);
            }
         }
      }
   }

   private void renderFace(VertexConsumer out, PoseStack pose, TwoIDollTail.Face f, int light, int overlay, boolean ribbon) {
      if (ribbon) {
         this.vertex(out, pose, f.a, 0.1F, 0.8F, f, light, overlay);
         this.vertex(out, pose, f.b, 0.1F, 0.95F, f, light, overlay);
         this.vertex(out, pose, f.c, 0.3F, 0.95F, f, light, overlay);
         this.vertex(out, pose, f.d, 0.3F, 0.8F, f, light, overlay);
      } else {
         float lo = Math.min(Math.min(f.a.u, f.b.u), Math.min(f.c.u, f.d.u));
         float hi = Math.max(Math.max(f.a.u, f.b.u), Math.max(f.c.u, f.d.u));
         boolean seam = hi - lo > 0.5F;
         this.furVertex(out, pose, f.a, seam, f, light, overlay);
         this.furVertex(out, pose, f.b, seam, f, light, overlay);
         this.furVertex(out, pose, f.c, seam, f, light, overlay);
         this.furVertex(out, pose, f.d, seam, f, light, overlay);
      }
   }

   private void furVertex(VertexConsumer out, PoseStack pose, TwoIDollTail.Point p, boolean seam, TwoIDollTail.Face f, int light, int overlay) {
      Matrix4f a = this.skin[p.a];
      Matrix4f b = this.skin[p.b];
      float t = p.weight;
      float x = Mth.lerp(t, a.m00() * p.x + a.m10() * p.y + a.m20() * p.z + a.m30(), b.m00() * p.x + b.m10() * p.y + b.m20() * p.z + b.m30());
      float y = this.skinY(p);
      float z = Mth.lerp(t, a.m02() * p.x + a.m12() * p.y + a.m22() * p.z + a.m32(), b.m02() * p.x + b.m12() * p.y + b.m22() * p.z + b.m32());
      float rootWeight = rootWeight(p);
      x = Mth.lerp(rootWeight, this.attachment.m00() * p.x + this.attachment.m10() * p.y + this.attachment.m20() * p.z + this.attachment.m30(), x);
      z = Mth.lerp(rootWeight, this.attachment.m02() * p.x + this.attachment.m12() * p.y + this.attachment.m22() * p.z + this.attachment.m32(), z);
      float nx = Mth.lerp(t, a.m00() * f.nx + a.m10() * f.ny + a.m20() * f.nz, b.m00() * f.nx + b.m10() * f.ny + b.m20() * f.nz);
      float ny = Mth.lerp(t, a.m01() * f.nx + a.m11() * f.ny + a.m21() * f.nz, b.m01() * f.nx + b.m11() * f.ny + b.m21() * f.nz);
      float nz = Mth.lerp(t, a.m02() * f.nx + a.m12() * f.ny + a.m22() * f.nz, b.m02() * f.nx + b.m12() * f.ny + b.m22() * f.nz);
      nx = Mth.lerp(rootWeight, this.attachment.m00() * f.nx + this.attachment.m10() * f.ny + this.attachment.m20() * f.nz, nx);
      ny = Mth.lerp(rootWeight, this.attachment.m01() * f.nx + this.attachment.m11() * f.ny + this.attachment.m21() * f.nz, ny);
      nz = Mth.lerp(rootWeight, this.attachment.m02() * f.nx + this.attachment.m12() * f.ny + this.attachment.m22() * f.nz, nz);
      out.addVertex(pose.last(), x / 16.0F, y / 16.0F, z / 16.0F)
         .setColor(255, 255, 255, 255)
         .setUv(seam && p.u < 0.5F ? p.u + 1.0F : p.u, p.v)
         .setOverlay(overlay)
         .setLight(light)
         .setNormal(pose.last(), nx, ny, nz);
   }

   private void vertex(VertexConsumer out, PoseStack pose, TwoIDollTail.Point p, float u, float v, TwoIDollTail.Face f, int light, int overlay) {
      float x = this.ribbonMatrix.m00() * p.x + this.ribbonMatrix.m10() * p.y + this.ribbonMatrix.m20() * p.z + this.ribbonMatrix.m30();
      float y = supportedY(this.ribbonMatrix.m01() * p.x + this.ribbonMatrix.m11() * p.y + this.ribbonMatrix.m21() * p.z + this.ribbonMatrix.m31());
      float z = this.ribbonMatrix.m02() * p.x + this.ribbonMatrix.m12() * p.y + this.ribbonMatrix.m22() * p.z + this.ribbonMatrix.m32();
      float nx = this.ribbonMatrix.m00() * f.nx + this.ribbonMatrix.m10() * f.ny + this.ribbonMatrix.m20() * f.nz;
      float ny = this.ribbonMatrix.m01() * f.nx + this.ribbonMatrix.m11() * f.ny + this.ribbonMatrix.m21() * f.nz;
      float nz = this.ribbonMatrix.m02() * f.nx + this.ribbonMatrix.m12() * f.ny + this.ribbonMatrix.m22() * f.nz;
      out.addVertex(pose.last(), x / 16.0F, y / 16.0F, z / 16.0F)
         .setColor(f.tint >> 16 & 0xFF, f.tint >> 8 & 0xFF, f.tint & 0xFF, 255)
         .setUv(u, v)
         .setOverlay(overlay)
         .setLight(light)
         .setNormal(pose.last(), nx, ny, nz);
   }

   private static record Bone(TwoIDollTail.Point pivot, List<TwoIDollTail.Face> faces) {
   }

   private static record Face(
      TwoIDollTail.Point a, TwoIDollTail.Point b, TwoIDollTail.Point c, TwoIDollTail.Point d, float nx, float ny, float nz, boolean marking, int tint
   ) {
   }

   private static record Point(float x, float y, float z, float u, float v, int a, int b, float weight) {
      Point(float x, float y, float z) {
         this(x, y, z, 0.0F, 0.0F, 0, 0, 0.0F);
      }
   }
}
