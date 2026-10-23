package com.godofthings.doll.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.godofthings.doll.block.TwoIDollDisplayBlock;
import com.godofthings.doll.blockentity.TwoIDollDisplayBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;

public final class TwoIDollDisplayRenderer implements BlockEntityRenderer<TwoIDollDisplayBlockEntity> {
   private final TwoIDollRenderer.Model model;

   public TwoIDollDisplayRenderer(Context context) {
      this.model = new TwoIDollRenderer.Model(context.bakeLayer(TwoIDollRenderer.LAYER));
   }

   public void render(TwoIDollDisplayBlockEntity doll, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
      float offset = (float)Math.floorMod(doll.getBlockPos().hashCode(), 1600);
      this.model.setupDecoration(TwoIDollItemRenderer.animationTime() + offset);
      this.model
         .applyWeather(
            TwoIDollWeather.sample(doll, doll.getLevel(), doll.getBlockPos(), partialTick, doll.getBlockPos().hashCode()),
            TwoIDollItemRenderer.animationTime() + offset
         );
      poseStack.pushPose();
      poseStack.translate(0.5, 0.93F, 0.5);
      double now = doll.getLevel() == null ? 0.0 : (double)((float)doll.getLevel().getGameTime() + partialTick);
      poseStack.mulPose(
         Axis.YP.rotationDegrees(-22.5F * (float)((Integer)doll.getBlockState().getValue(TwoIDollDisplayBlock.ROTATION)).intValue() + doll.rotationAt(now))
      );
      poseStack.scale(0.62F, -0.62F, -0.62F);
      this.model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TwoIDollSkin.texture(this.model.blink()))), light, overlay);
      this.model.renderAccessories(poseStack, buffer, light, overlay);
      poseStack.popPose();
   }

   public int getViewDistance() {
      return 48;
   }
}
