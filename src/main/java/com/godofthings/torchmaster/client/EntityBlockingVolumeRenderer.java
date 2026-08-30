package com.godofthings.torchmaster.client;

import com.godofthings.torchmaster.common.ModBlocks;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(
   modid = "godofthings",
   value = {Dist.CLIENT},
   bus = Bus.FORGE
)
public class EntityBlockingVolumeRenderer {
   private static final ResourceLocation FORCEFIELD_LOCATION = ResourceLocation.tryParse("textures/misc/forcefield.png");
   private static final Map<Vec3i, Tuple<Integer, Integer>> volumeLights = new HashMap<>();
   private static final Map<Vec3i, Integer> locationLights = new HashMap<>();

   private static BoundingBox createVolume(Vec3i pos, int halfRange) {
      Vec3i min = pos.offset(-halfRange, -halfRange, -halfRange);
      Vec3i max = pos.offset(halfRange + 1, halfRange + 1, halfRange + 1);
      return BoundingBox.fromCorners(min, max);
   }

   private static void renderLightVolume(Vec3i pos, int torchRange, Camera cam, int color) {
      Minecraft mc = Minecraft.getInstance();
      int blockRenderDistance = mc.options.getEffectiveRenderDistance() * 16;
      BoundingBox torchVol = createVolume(pos, torchRange);
      BoundingBox playerVolume = createVolume(cam.getBlockPosition(), blockRenderDistance);
      if (playerVolume.intersects(torchVol)) {
         BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
         double camX = cam.getPosition().x;
         double camZ = cam.getPosition().z;
         double camY = cam.getPosition().y;
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ZERO, DestFactor.ONE);
         RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
         RenderSystem.depthMask(Minecraft.useShaderTransparency());
         PoseStack posestack = RenderSystem.getModelViewStack();
         posestack.pushPose();
         RenderSystem.applyModelViewMatrix();
         float red = (float)(color >> 16 & 0xFF) / 255.0F;
         float green = (float)(color >> 8 & 0xFF) / 255.0F;
         float blue = (float)(color & 0xFF) / 255.0F;
         RenderSystem.setShaderColor(red, green, blue, 1.0F);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.polygonOffset(-3.0F, -3.0F);
         RenderSystem.enablePolygonOffset();
         RenderSystem.disableCull();
         float slide = (float)(Util.getMillis() % 3000L) / 3000.0F;
         bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         double vMinX = (double)torchVol.minX() - camX;
         double vMaxX = (double)torchVol.maxX() - camX;
         double vMinY = (double)torchVol.minY() - camY;
         double vMaxY = (double)torchVol.maxY() - camY;
         double vMinZ = (double)torchVol.minZ() - camZ;
         double vMaxZ = (double)torchVol.maxZ() - camZ;
         float uv1 = (float)torchRange + slide;
         bufferbuilder.vertex(vMaxX, vMinY, vMinZ).uv(uv1, uv1).endVertex();
         bufferbuilder.vertex(vMaxX, vMinY, vMaxZ).uv(uv1, slide).endVertex();
         bufferbuilder.vertex(vMaxX, vMaxY, vMaxZ).uv(slide, slide).endVertex();
         bufferbuilder.vertex(vMaxX, vMaxY, vMinZ).uv(slide, uv1).endVertex();
         bufferbuilder.vertex(vMinX, vMinY, vMinZ).uv(uv1, uv1).endVertex();
         bufferbuilder.vertex(vMinX, vMinY, vMaxZ).uv(uv1, slide).endVertex();
         bufferbuilder.vertex(vMinX, vMaxY, vMaxZ).uv(slide, slide).endVertex();
         bufferbuilder.vertex(vMinX, vMaxY, vMinZ).uv(slide, uv1).endVertex();
         bufferbuilder.vertex(vMinX, vMinY, vMaxZ).uv(uv1, uv1).endVertex();
         bufferbuilder.vertex(vMaxX, vMinY, vMaxZ).uv(uv1, slide).endVertex();
         bufferbuilder.vertex(vMaxX, vMaxY, vMaxZ).uv(slide, slide).endVertex();
         bufferbuilder.vertex(vMinX, vMaxY, vMaxZ).uv(slide, uv1).endVertex();
         bufferbuilder.vertex(vMinX, vMinY, vMinZ).uv(uv1, uv1).endVertex();
         bufferbuilder.vertex(vMaxX, vMinY, vMinZ).uv(uv1, slide).endVertex();
         bufferbuilder.vertex(vMaxX, vMaxY, vMinZ).uv(slide, slide).endVertex();
         bufferbuilder.vertex(vMinX, vMaxY, vMinZ).uv(slide, uv1).endVertex();
         bufferbuilder.vertex(vMinX, vMaxY, vMinZ).uv(uv1, uv1).endVertex();
         bufferbuilder.vertex(vMaxX, vMaxY, vMinZ).uv(uv1, slide).endVertex();
         bufferbuilder.vertex(vMaxX, vMaxY, vMaxZ).uv(slide, slide).endVertex();
         bufferbuilder.vertex(vMinX, vMaxY, vMaxZ).uv(slide, uv1).endVertex();
         bufferbuilder.vertex(vMinX, vMinY, vMinZ).uv(uv1, uv1).endVertex();
         bufferbuilder.vertex(vMaxX, vMinY, vMinZ).uv(uv1, slide).endVertex();
         bufferbuilder.vertex(vMaxX, vMinY, vMaxZ).uv(slide, slide).endVertex();
         bufferbuilder.vertex(vMinX, vMinY, vMaxZ).uv(slide, uv1).endVertex();
         BufferUploader.drawWithShader(bufferbuilder.end());
         RenderSystem.enableCull();
         RenderSystem.polygonOffset(0.0F, 0.0F);
         RenderSystem.disablePolygonOffset();
         RenderSystem.disableBlend();
         RenderSystem.defaultBlendFunc();
         posestack.popPose();
         RenderSystem.applyModelViewMatrix();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.depthMask(true);
      }
   }

   public static void showVolumeAt(Vec3i pos, int range, int color) {
      volumeLights.put(pos, new Tuple(range, color));
   }

   public static void removeVolumeAt(Vec3i pos) {
      volumeLights.remove(pos);
   }

   public static void showLocationAt(Vec3i pos, int color) {
      locationLights.put(pos, color);
   }

   public static void removeLocationAt(Vec3i pos) {
      locationLights.remove(pos);
   }

   public static void clearAll() {
      volumeLights.clear();
      locationLights.clear();
   }

   private static void cleanupRemovedTorches() {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         List<Vec3i> toRemove = new ArrayList<>();

         for (Entry<Vec3i, Tuple<Integer, Integer>> light : volumeLights.entrySet()) {
            Vec3i pos = light.getKey();
            Block block = level.getBlockState(new BlockPos(pos.getX(), pos.getY(), pos.getZ())).getBlock();
            if (block != ModBlocks.blockMegaTorch.get() && block != ModBlocks.blockDreadLamp.get()) {
               toRemove.add(pos);
            }
         }

         for (Vec3i pos : toRemove) {
            volumeLights.remove(pos);
            locationLights.remove(pos);
         }
      }
   }

   @SubscribeEvent
   public static void onRenderLevelStageEvent(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_WEATHER) {
         cleanupRemovedTorches();

         for (Entry<Vec3i, Tuple<Integer, Integer>> light : volumeLights.entrySet()) {
            renderLightVolume(light.getKey(), (Integer)light.getValue().getA(), event.getCamera(), (Integer)light.getValue().getB());
         }

         for (Entry<Vec3i, Integer> light : locationLights.entrySet()) {
            renderTorchLocation(light.getKey(), light.getValue(), event.getCamera());
         }
      }
   }

   private static void renderTorchLocation(Vec3i pos, int color, Camera cam) {
      BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
      double camX = cam.getPosition().x;
      double camZ = cam.getPosition().z;
      double camY = cam.getPosition().y;
      RenderSystem.enableBlend();
      RenderSystem.disableDepthTest();
      RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ONE, DestFactor.ZERO);
      RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
      RenderSystem.depthMask(Minecraft.useShaderTransparency());
      PoseStack posestack = RenderSystem.getModelViewStack();
      posestack.pushPose();
      RenderSystem.applyModelViewMatrix();
      float red = (float)(color >> 16 & 0xFF) / 255.0F;
      float green = (float)(color >> 8 & 0xFF) / 255.0F;
      float blue = (float)(color & 0xFF) / 255.0F;
      RenderSystem.setShaderColor(red, green, blue, 1.0F);
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.polygonOffset(-3.0F, -3.0F);
      RenderSystem.enablePolygonOffset();
      RenderSystem.disableCull();
      float slide = (float)(Util.getMillis() % 3000L) / 3000.0F;
      bufferbuilder.begin(Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      double vMinX = (double)pos.getX() - camX;
      double vMaxX = (double)pos.getX() - camX + 1.0;
      double vMinY = (double)pos.getY() - camY;
      double vMaxY = (double)pos.getY() - camY + 1.0;
      double vMinZ = (double)pos.getZ() - camZ;
      double vMaxZ = (double)pos.getZ() - camZ + 1.0;
      int uv0 = 0;
      int uv1 = 1;
      bufferbuilder.vertex(vMaxX, vMinY, vMinZ).uv((float)uv1, (float)uv1).endVertex();
      bufferbuilder.vertex(vMaxX, vMinY, vMaxZ).uv((float)uv1, (float)uv0).endVertex();
      bufferbuilder.vertex(vMaxX, vMaxY, vMaxZ).uv((float)uv0, (float)uv0).endVertex();
      bufferbuilder.vertex(vMaxX, vMaxY, vMinZ).uv((float)uv0, (float)uv1).endVertex();
      bufferbuilder.vertex(vMinX, vMinY, vMinZ).uv((float)uv1, (float)uv1).endVertex();
      bufferbuilder.vertex(vMinX, vMinY, vMaxZ).uv((float)uv1, (float)uv0).endVertex();
      bufferbuilder.vertex(vMinX, vMaxY, vMaxZ).uv((float)uv0, (float)uv0).endVertex();
      bufferbuilder.vertex(vMinX, vMaxY, vMinZ).uv((float)uv0, (float)uv1).endVertex();
      bufferbuilder.vertex(vMinX, vMinY, vMaxZ).uv((float)uv1, (float)uv1).endVertex();
      bufferbuilder.vertex(vMaxX, vMinY, vMaxZ).uv((float)uv1, (float)uv0).endVertex();
      bufferbuilder.vertex(vMaxX, vMaxY, vMaxZ).uv((float)uv0, (float)uv0).endVertex();
      bufferbuilder.vertex(vMinX, vMaxY, vMaxZ).uv((float)uv0, (float)uv1).endVertex();
      bufferbuilder.vertex(vMinX, vMinY, vMinZ).uv((float)uv1, (float)uv1).endVertex();
      bufferbuilder.vertex(vMaxX, vMinY, vMinZ).uv((float)uv1, (float)uv0).endVertex();
      bufferbuilder.vertex(vMaxX, vMaxY, vMinZ).uv((float)uv0, (float)uv0).endVertex();
      bufferbuilder.vertex(vMinX, vMaxY, vMinZ).uv((float)uv0, (float)uv1).endVertex();
      bufferbuilder.vertex(vMinX, vMaxY, vMinZ).uv((float)uv1, (float)uv1).endVertex();
      bufferbuilder.vertex(vMaxX, vMaxY, vMinZ).uv((float)uv1, (float)uv0).endVertex();
      bufferbuilder.vertex(vMaxX, vMaxY, vMaxZ).uv((float)uv0, (float)uv0).endVertex();
      bufferbuilder.vertex(vMinX, vMaxY, vMaxZ).uv((float)uv0, (float)uv1).endVertex();
      bufferbuilder.vertex(vMinX, vMinY, vMinZ).uv((float)uv1, (float)uv1).endVertex();
      bufferbuilder.vertex(vMaxX, vMinY, vMinZ).uv((float)uv1, (float)uv0).endVertex();
      bufferbuilder.vertex(vMaxX, vMinY, vMaxZ).uv((float)uv0, (float)uv0).endVertex();
      bufferbuilder.vertex(vMinX, vMinY, vMaxZ).uv((float)uv0, (float)uv1).endVertex();
      BufferUploader.drawWithShader(bufferbuilder.end());
      RenderSystem.enableCull();
      RenderSystem.polygonOffset(0.0F, 0.0F);
      RenderSystem.disablePolygonOffset();
      RenderSystem.disableBlend();
      RenderSystem.defaultBlendFunc();
      posestack.popPose();
      RenderSystem.applyModelViewMatrix();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.depthMask(true);
   }
}
