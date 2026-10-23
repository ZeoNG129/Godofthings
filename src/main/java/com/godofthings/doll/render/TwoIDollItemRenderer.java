package com.godofthings.doll.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class TwoIDollItemRenderer extends BlockEntityWithoutLevelRenderer {
   private static TwoIDollItemRenderer instance;
   private TwoIDollRenderer.Model model;

   private TwoIDollItemRenderer() {
      super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
   }

   public static TwoIDollItemRenderer get() {
      if (instance == null) {
         instance = new TwoIDollItemRenderer();
      }

      return instance;
   }

   public void onResourceManagerReload(ResourceManager resourceManager) {
      this.model = null;
   }

   public static float animationTime() {
      return (float)(Util.getMillis() % 8000000L) / 50.0F;
   }

   public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
      if (context != ItemDisplayContext.HEAD) {
         poseStack.pushPose();
         poseStack.translate(0.5, 0.0, 0.5);
         this.renderAtFeet(stack, poseStack, buffer, light, overlay);
         poseStack.popPose();
      }
   }

   public void renderAtFeet(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
      this.renderCarried(stack, poseStack, buffer, light, overlay, TwoIDollCarryMotion.Pose.STILL);
   }

   public void renderCarried(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, TwoIDollCarryMotion.Pose motion) {
      this.renderCarried(stack, poseStack, buffer, light, overlay, motion, TwoIDollWeather.Pose.CLEAR);
   }

   public void renderCarried(
      ItemStack stack, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay, TwoIDollCarryMotion.Pose motion, TwoIDollWeather.Pose weather
   ) {
      this.renderCarried(stack, poseStack, buffer, light, overlay, motion, weather, animationTime());
   }

   public void renderCarried(
      ItemStack stack,
      PoseStack poseStack,
      MultiBufferSource buffer,
      int light,
      int overlay,
      TwoIDollCarryMotion.Pose motion,
      TwoIDollWeather.Pose weather,
      float time
   ) {
      if (this.model == null) {
         this.model = new TwoIDollRenderer.Model(Minecraft.getInstance().getEntityModels().bakeLayer(TwoIDollRenderer.LAYER));
      }

      this.model.setupDecoration(time);
      this.model.applyCarriedMotion(motion, time);
      this.model.applyWeather(weather, time);
      poseStack.pushPose();
      motion.apply(poseStack);
      poseStack.translate(0.0, 0.93F, 0.0);
      poseStack.scale(0.62F, -0.62F, -0.62F);
      VertexConsumer vertices = ItemRenderer.getFoilBufferDirect(
         buffer, RenderType.entityCutoutNoCull(TwoIDollSkin.texture(this.model.blink())), false, stack.hasFoil()
      );
      this.model.renderToBuffer(poseStack, vertices, light, overlay);
      this.model.renderAccessories(poseStack, buffer, light, overlay);
      poseStack.popPose();
   }
}
