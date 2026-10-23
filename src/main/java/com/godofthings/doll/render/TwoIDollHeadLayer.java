package com.godofthings.doll.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.godofthings.doll.init.DollRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin.Model;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.AddLayers;

@EventBusSubscriber(
   modid = "godofthings",
   value = {Dist.CLIENT},
   bus = Bus.MOD
)
public final class TwoIDollHeadLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
   public static final float HEAD_CLEARANCE = 0.540625F;
   public static final float HEAD_DOLL_SCALE = 0.8F;

   public TwoIDollHeadLayer(PlayerRenderer renderer) {
      super(renderer);
   }

   @SubscribeEvent
   public static void addLayers(AddLayers event) {
      for (Model skin : event.getSkins()) {
         if (event.getSkin(skin) instanceof PlayerRenderer playerRenderer) {
            playerRenderer.addLayer(new TwoIDollHeadLayer(playerRenderer));
         }
      }
   }

   public void render(
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      float limbSwing,
      float limbSwingAmount,
      float partialTick,
      float age,
      float yaw,
      float pitch
   ) {
      ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
      if (stack.is((Item)DollRegistry.TWO_I_DOLL_ITEM.get())) {
         pose.pushPose();
         transformToHead(((PlayerModel)this.getParentModel()).head, pose);
         TwoIDollItemRenderer.get()
            .renderCarried(
               stack,
               pose,
               buffers,
               light,
               OverlayTexture.NO_OVERLAY,
               TwoIDollCarryMotion.sample(player, partialTick),
               TwoIDollWeather.sample(player, player.level(), player.blockPosition(), partialTick, player.getUUID().hashCode())
            );
         pose.popPose();
      }
   }

   public static void renderOnHead(ItemStack stack, ModelPart head, PoseStack pose, MultiBufferSource buffers, int light) {
      pose.pushPose();
      transformToHead(head, pose);
      TwoIDollItemRenderer.get().renderAtFeet(stack, pose, buffers, light, OverlayTexture.NO_OVERLAY);
      pose.popPose();
   }

   public static void transformToHead(ModelPart head, PoseStack pose) {
      head.translateAndRotate(pose);
      pose.translate(0.0F, -0.540625F, 0.0F);
      pose.scale(0.8F, -0.8F, -0.8F);
   }
}
