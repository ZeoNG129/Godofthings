package com.godofthings.doll.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.godofthings.doll.entity.TwoIDollEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.EntityRendererProvider.Context;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class TwoIDollRenderer extends MobRenderer<TwoIDollEntity, TwoIDollRenderer.Model> {
   public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("godofthings", "textures/entity/two_i.png");
   public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("godofthings", "two_i_doll"), "main");
   public static final float RENDER_SCALE = 0.62F;
   public static final float FEET_Y = 24.0F;

   public TwoIDollRenderer(Context context) {
      super(context, new TwoIDollRenderer.Model(context.bakeLayer(LAYER)), 0.18F);
      this.addLayer(
         new RenderLayer<TwoIDollEntity, TwoIDollRenderer.Model>(this) {
            public void render(
               PoseStack pose,
               MultiBufferSource buffers,
               int light,
               TwoIDollEntity doll,
               float swing,
               float amount,
               float partial,
               float age,
               float yaw,
               float pitch
            ) {
               ((TwoIDollRenderer.Model)this.getParentModel()).renderAccessories(pose, buffers, light, OverlayTexture.NO_OVERLAY);
            }
         }
      );
   }

   protected void scale(TwoIDollEntity entity, PoseStack poseStack, float partialTick) {
      poseStack.scale(0.62F, 0.62F, 0.62F);
   }

   public ResourceLocation getTextureLocation(TwoIDollEntity entity) {
      return TwoIDollSkin.texture(((TwoIDollRenderer.Model)this.model).blink());
   }

   public static final class Model extends PlayerModel<TwoIDollEntity> {
      private static final float ARM_WIDTH = 0.5625F;
      private static final float HEAD_SIZE = 1.18F;
      private static final float BODY_HEIGHT = 0.75F;
      private static final float LEG_HEIGHT = 0.5F;
      private static final float LEG_DEPTH = 0.75F;
      private static final float HIP_Y = 18.0F;
      private static final float NECK_Y = 9.0F;
      private final ModelPart[] resetParts;
      private final ModelPart umbrella;
      private final ModelPart noseDrop;
      private final TwoIDollTail tail = new TwoIDollTail();
      private float tailStride;
      private float tailMoving;
      private TwoIDollCarryMotion.Pose carryMotion = TwoIDollCarryMotion.Pose.STILL;
      private float blink;
      private boolean seated;
      private boolean sharedUmbrella;
      private boolean umbrellaHolder = true;
      private static final ResourceLocation ACCESSORIES = ResourceLocation.fromNamespaceAndPath("godofthings", "textures/entity/two_i_accessories.png");

      public static LayerDefinition createLayer() {
         MeshDefinition mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
         PartDefinition umbrella = mesh.getRoot()
            .addOrReplaceChild(
               "umbrella",
               CubeListBuilder.create()
                  .texOffs(0, 0)
                  .addBox(-0.18F, -19.55F, -0.18F, 0.36F, 21.1F, 0.36F)
                  .addBox(-0.45F, -20.1F, -0.45F, 0.9F, 0.65F, 0.9F)
                  .addBox(-0.4F, -0.65F, -0.4F, 0.8F, 2.1F, 0.8F)
                  .addBox(-0.3F, 1.2F, -1.35F, 0.6F, 0.65F, 1.7F)
                  .addBox(-0.3F, 0.55F, -1.55F, 0.6F, 1.1F, 0.6F),
               PartPose.ZERO
            );
         umbrella.addOrReplaceChild("canopy", CubeListBuilder.create(), PartPose.offset(2.25F, -20.0F, 0.0F));
         mesh.getRoot()
            .addOrReplaceChild(
               "nose_drop", CubeListBuilder.create().texOffs(48, 0).addBox(-0.32F, 0.0F, -4.56F, 0.64F, 1.5F, 0.45F), PartPose.offset(0.0F, -2.2F, 0.0F)
            );
         return LayerDefinition.create(mesh, 64, 64);
      }

      public Model(ModelPart root) {
         super(root, false);
         this.umbrella = root.getChild("umbrella");
         this.noseDrop = root.getChild("nose_drop");
         initialPivot(this.head, 0.0F, 9.0F, 0.0F);
         initialPivot(this.body, 0.0F, 9.0F, 0.0F);
         initialPivot(this.leftArm, 4.15F, 10.4F, 0.0F);
         initialPivot(this.rightArm, -4.15F, 10.4F, 0.0F);
         initialPivot(this.leftLeg, 1.8F, 18.0F, 0.0F);
         initialPivot(this.rightLeg, -1.8F, 18.0F, 0.0F);
         this.resetParts = root.getAllParts().toArray(ModelPart[]::new);
      }

      private static void initialPivot(ModelPart part, float x, float y, float z) {
         PartPose pose = PartPose.offset(x, y, z);
         part.setInitialPose(pose);
         part.loadPose(pose);
      }

      public void setupAnim(TwoIDollEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
         this.setupStanding(
            limbSwing,
            limbSwingAmount,
            ageInTicks,
            netHeadYaw,
            headPitch,
            entity.getPoseAction(),
            entity.getPoseActionProgress(ageInTicks - (float)entity.tickCount)
         );
         this.setSharedUmbrella(entity.hasSharedUmbrella(), entity.isSharedUmbrellaHolder());
         this.applyTailMotion(TwoIDollCarryMotion.sample(entity, ageInTicks - (float)entity.tickCount), ageInTicks);
         this.applyWeather(
            TwoIDollWeather.sample(entity, entity.level(), entity.blockPosition(), ageInTicks - (float)entity.tickCount, entity.getUUID().hashCode()),
            ageInTicks
         );
      }

      public void setupStanding(float limbSwing, float limbSwingAmount, float time, float headYaw, float headPitch, int action, float progress) {
         this.setupStanding(limbSwing, limbSwingAmount, time, headYaw, headPitch, action, progress, 0, 0.0F);
      }

      public void setupStanding(
         float limbSwing,
         float limbSwingAmount,
         float time,
         float headYaw,
         float headPitch,
         int action,
         float progress,
         int environmentAction,
         float environmentProgress
      ) {
         this.resetDoll();
         float moving = Mth.clamp(limbSwingAmount * 2.4F, 0.0F, 1.0F);
         this.tailStride = limbSwing;
         this.tailMoving = moving;
         float still = 1.0F - moving;
         float phase = limbSwing * 0.92F;
         float step = Mth.sin(phase);
         float bounce = Mth.abs(step) * moving * 0.16F;
         float breath = Mth.sin(time * 0.09F) * still;
         this.moveTorso(-bounce + breath * 0.045F);
         this.head.yRot = Mth.clamp(headYaw * (float) (Math.PI / 180.0), -0.72F, 0.72F);
         this.head.xRot = Mth.clamp(headPitch * (float) (Math.PI / 180.0), -0.38F, 0.42F);
         this.head.zRot = Mth.sin(time * 0.033F) * 0.035F * still;
         this.body.zRot = step * moving * 0.022F;
         this.head.x += step * moving * 0.07F;
         this.leftArm.xRot = -step * moving * 0.38F;
         this.rightArm.xRot = step * moving * 0.38F;
         this.leftArm.zRot = -0.07F - breath * 0.015F;
         this.rightArm.zRot = 0.07F + breath * 0.015F;
         this.leftLeg.xRot = step * moving * 0.57F;
         this.rightLeg.xRot = -step * moving * 0.57F;
         plantFoot(this.leftLeg, Mth.clamp(-step, 0.0F, 1.0F) * moving * 0.65F);
         plantFoot(this.rightLeg, Mth.clamp(step, 0.0F, 1.0F) * moving * 0.65F);
         float gesture = Mth.sin(Mth.clamp(progress, 0.0F, 1.0F) * (float) Math.PI);
         switch (action) {
            case 1:
               this.rightArm.xRot = Mth.lerp(gesture, this.rightArm.xRot, -2.45F);
               this.rightArm.zRot = this.rightArm.zRot + (0.45F + Mth.sin(time * 0.32F) * 0.2F) * gesture;
               this.head.zRot -= 0.12F * gesture;
               break;
            case 2:
               this.head.zRot += 0.25F * gesture;
               break;
            case 3:
               this.leftArm.xRot = Mth.lerp(gesture, this.leftArm.xRot, -2.65F);
               this.leftArm.yRot = 0.24F * gesture;
               this.leftArm.zRot -= 0.28F * gesture;
               this.head.xRot -= 0.12F * gesture;
               this.head.zRot += 0.1F * gesture;
               this.head.xScale = this.head.zScale = 1.18F * (1.0F + 0.055F * gesture);
               this.head.yScale = 1.18F * (1.0F - 0.1F * gesture);
               this.moveTorso(0.3F * gesture);
               break;
            case 4:
               this.leftArm.xRot = this.rightArm.xRot = -1.18F * gesture;
               this.leftArm.yRot = 0.25F * gesture;
               this.rightArm.yRot = -0.25F * gesture;
               this.head.xRot += 0.18F * gesture;
               break;
            case 5:
               this.leftArm.zRot -= 0.72F * gesture;
               this.rightArm.zRot += 0.72F * gesture;
               this.leftArm.xRot = this.rightArm.xRot = -0.55F * gesture;
               this.head.zRot += 0.12F * gesture;
               break;
            case 6:
               this.leftLeg.xRot = this.leftLeg.xRot + Mth.sin(time * 0.19F) * 0.18F * still * gesture;
               plantFoot(this.leftLeg, 0.15F * still * gesture);
            case 7:
            case 8:
            case 9:
            default:
               break;
            case 10:
               this.leftArm.zRot -= 2.1F * gesture;
               this.rightArm.zRot += 2.1F * gesture;
               this.head.xRot -= 0.18F * gesture;
               break;
            case 11:
               this.rightArm.xRot = -0.7F * gesture;
               this.rightArm.yRot = -0.65F * gesture;
               this.rightArm.zRot = this.rightArm.zRot + Mth.sin(time * 0.22F) * 0.15F * gesture;
               this.head.xRot += 0.16F * gesture;
               break;
            case 12:
               this.leftArm.xRot = this.rightArm.xRot = -2.35F * gesture;
               this.head.zRot += 0.18F * gesture;
         }

         float glance = Mth.sin(time * 0.021F) * 0.11F + Mth.sin(time * 0.047F) * 0.045F;
         this.head.yRot += glance * still;
         this.body.zRot = this.body.zRot + Mth.sin(time * 0.031F) * 0.018F * still;
         this.blink = this.blinkEnvelope(time);
         this.applyEnvironment(environmentAction, environmentProgress, time);
         if (action == 3) {
            this.head.xScale = this.head.zScale = 1.18F * (1.0F + 0.055F * gesture);
            this.head.yScale = 1.18F * (1.0F - 0.1F * gesture);
         }

         this.copySkinLayers();
      }

      private float blinkEnvelope(float time) {
         int cycle = Math.floorDiv((int)Math.floor((double)time), 83);
         float phase = time - (float)cycle * 83.0F;
         float first = Mth.clamp(1.0F - Math.abs(phase - 42.0F) / 3.0F, 0.0F, 1.0F);
         float second = cycle % 7 == 0 ? Mth.clamp(1.0F - Math.abs(phase - 49.0F) / 2.0F, 0.0F, 1.0F) : 0.0F;
         return Math.max(first, second);
      }

      private void applyEnvironment(int action, float progress, float time) {
         int flags = action == 7 ? 1 : (action == 8 ? 2 : (action == 9 ? 4 : 0));
         this.applyWeather(new TwoIDollWeather.Pose(flags, action == 7 ? progress : 0.0F, action == 8 ? progress : -1.0F), time);
      }

      public void applyWeather(TwoIDollWeather.Pose weather, float time) {
         float open = Mth.sin(weather.open() * (float) (Math.PI / 2));
         this.umbrella.visible = open > 0.01F && (!this.sharedUmbrella || this.umbrellaHolder);
         ModelPart canopy = this.umbrella.getChild("canopy");
         float sharedScale = this.sharedUmbrella ? 1.35F : 1.0F;
         canopy.xScale = canopy.zScale = Math.max(0.05F, open * sharedScale);
         canopy.yScale = 0.65F + open * 0.35F;
         this.noseDrop.visible = (weather.flags() & 2) != 0;
         if (this.umbrella.visible) {
            this.rightArm.xRot = -0.5F;
            this.rightArm.yRot = 0.0F;
            this.rightArm.zRot = 1.14F;
            this.head.yRot = this.head.yRot + Mth.sin(time * 0.09F) * 0.05F * open;
         }

         if (this.noseDrop.visible) {
            this.noseDrop.yScale = 0.55F + (Mth.sin(time * 0.07F) + 1.0F) * 0.22F;
            if (weather.sneeze() >= 0.0F) {
               float sneeze = Mth.sin(weather.sneeze() * (float) Math.PI);
               this.head.xRot += 0.42F * sneeze;
               this.head.y += 0.22F * sneeze;
               this.leftArm.xRot = Mth.lerp(sneeze, this.leftArm.xRot, -1.35F);
               this.leftArm.zRot -= 0.18F * sneeze;
               this.blink = Math.max(this.blink, sneeze);
            }
         }

         if ((weather.flags() & 4) != 0) {
            float shiver = Mth.sin(time * 0.95F) * 0.055F;
            this.body.zRot += shiver;
            this.head.zRot += shiver * 1.3F;
            this.leftArm.xRot = -0.6F + shiver;
            if (!this.umbrella.visible) {
               this.rightArm.xRot = -0.6F - shiver;
            }

            this.head.xRot += 0.1F;
         }

         if (this.seated) {
            float bottom = 9.1875F * Mth.cos(this.body.zRot) + 4.25F * this.body.xScale * Math.abs(Mth.sin(this.body.zRot));
            this.moveTorso(24.0F - bottom - this.body.y);
         }

         if (this.umbrella.visible) {
            this.head.yRot = Mth.clamp(this.head.yRot, -0.4F, 0.4F);
            this.head.zRot = Mth.clamp(this.head.zRot, -0.07F, 0.07F);
            this.head.xRot = Mth.clamp(this.head.xRot, -0.3F, 0.4F);
            float handX = -this.rightArm.xScale;
            float handY = 10.0F * this.rightArm.yScale;
            float reachY = handY * (float)Math.cos((double)this.rightArm.xRot);
            float rollCos = (float)Math.cos((double)this.rightArm.zRot);
            float rollSin = (float)Math.sin((double)this.rightArm.zRot);
            this.umbrella
               .setPos(
                  this.rightArm.x + handX * rollCos - reachY * rollSin,
                  this.rightArm.y + handX * rollSin + reachY * rollCos,
                  this.rightArm.z + handY * (float)Math.sin((double)this.rightArm.xRot)
               );
         }

         this.updateTail(time);
         this.copySkinLayers();
      }

      public void setSharedUmbrella(boolean shared, boolean holder) {
         this.sharedUmbrella = shared;
         this.umbrellaHolder = holder;
      }

      public float blink() {
         return this.blink;
      }

      public boolean noseVisible() {
         return this.noseDrop.visible;
      }

      public void renderAccessories(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
         this.tail.render(pose, buffers, light, overlay);
         VertexConsumer vertices = buffers.getBuffer(RenderType.entityCutoutNoCull(ACCESSORIES));
         this.umbrella.render(pose, vertices, light, overlay);
         if (this.noseDrop.visible) {
            pose.pushPose();
            this.head.translateAndRotate(pose);
            this.noseDrop.render(pose, vertices, light, overlay);
            pose.popPose();
         }

         if (this.umbrella.visible) {
            pose.pushPose();
            this.umbrella.translateAndRotate(pose);
            this.umbrella.getChild("canopy").translateAndRotate(pose);
            TwoIUmbrellaCanopy.render(pose, buffers, light, overlay);
            pose.popPose();
         }
      }

      public boolean umbrellaVisible() {
         return this.umbrella.visible;
      }

      private static void plantFoot(ModelPart leg, float lift) {
         leg.y = 24.0F - soleDepth(leg.xRot) - lift;
      }

      private static float soleDepth(float angle) {
         return 6.0F * Mth.cos(angle) + 1.5F * Mth.abs(Mth.sin(angle));
      }

      public float footBottom(ModelPart leg) {
         return leg.y + soleDepth(leg.xRot);
      }

      private void resetDoll() {
         for (ModelPart part : this.resetParts) {
            part.resetPose();
         }

         this.young = this.riding = this.crouching = false;
         this.attackTime = this.swimAmount = 0.0F;
         this.head.xScale = this.head.yScale = this.head.zScale = 1.18F;
         this.umbrella.visible = false;
         this.noseDrop.visible = false;
         this.blink = 0.0F;
         this.carryMotion = TwoIDollCarryMotion.Pose.STILL;
         this.tailStride = this.tailMoving = 0.0F;
         this.seated = false;
         this.body.xScale = 0.875F;
         this.body.yScale = 0.75F;
         this.body.zScale = 1.125F;
         this.leftArm.xScale = this.rightArm.xScale = 0.5625F;
         this.leftArm.yScale = this.rightArm.yScale = 0.5F;
         this.leftArm.zScale = this.rightArm.zScale = 0.75F;
         this.leftLeg.xScale = this.rightLeg.xScale = 0.75F;
         this.leftLeg.yScale = this.rightLeg.yScale = 0.5F;
         this.leftLeg.zScale = this.rightLeg.zScale = 0.75F;
      }

      private void moveTorso(float dy) {
         this.head.y += dy;
         this.body.y += dy;
         this.leftArm.y += dy;
         this.rightArm.y += dy;
      }

      public void setupDecoration(float time) {
         this.resetDoll();
         this.seated = true;
         float seatedHip = 22.3125F;
         this.moveTorso(5.8125F);
         this.leftLeg.y = this.rightLeg.y = seatedHip;
         this.leftLeg.z = this.rightLeg.z = -1.6F;
         this.leftLeg.x = 2.6F;
         this.rightLeg.x = -2.6F;
         this.leftLeg.xRot = this.rightLeg.xRot = (float) (-Math.PI / 2);
         this.leftLeg.yRot = -0.24F;
         this.rightLeg.yRot = 0.24F;
         float breath = Mth.sin(time * 0.08F);
         this.head.zRot = Mth.sin(time * 0.029F) * 0.04F;
         this.head.y += breath * 0.04F;
         this.leftArm.xRot = this.rightArm.xRot = -0.13F;
         this.leftArm.zRot = -0.1F;
         this.rightArm.zRot = 0.1F;
         int slot = Math.floorDiv((int)time, 200);
         float progress = (time - (float)slot * 200.0F) / 120.0F;
         float envelope = progress < 1.0F ? Mth.sin(progress * (float) Math.PI) : 0.0F;
         switch (Math.floorMod(slot, 12)) {
            case 0:
               this.head.yRot = Mth.sin(progress * (float) (Math.PI * 2)) * 0.4F * envelope;
               this.head.xRot = -0.1F * envelope;
               break;
            case 1:
               this.leftLeg.xRot = this.leftLeg.xRot + Mth.sin(time * 0.2F) * 0.17F * envelope;
               this.rightLeg.xRot = this.rightLeg.xRot - Mth.sin(time * 0.2F) * 0.17F * envelope;
               break;
            case 2:
               this.rightArm.xRot = -2.4F * envelope;
               this.rightArm.zRot = this.rightArm.zRot + (0.36F + Mth.sin(time * 0.28F) * 0.2F) * envelope;
               this.head.zRot += 0.12F * envelope;
               break;
            case 3:
               this.leftArm.zRot -= 2.25F * envelope;
               this.rightArm.zRot += 2.25F * envelope;
               this.head.xRot -= 0.2F * envelope;
               break;
            case 4:
               this.head.xRot = this.head.xRot + (0.28F + Mth.sin(time * 0.08F) * 0.09F) * envelope;
               this.head.zRot += 0.13F * envelope;
               this.head.y += 0.13F * envelope;
               break;
            case 5:
               this.rightArm.xRot = -0.8F * envelope;
               this.rightArm.yRot = -0.65F * envelope;
               this.rightArm.zRot = this.rightArm.zRot + Mth.sin(time * 0.19F) * 0.17F * envelope;
               this.head.xRot += 0.16F * envelope;
               break;
            case 6:
               this.head.zRot += 0.27F * envelope;
               this.leftLeg.xRot = this.leftLeg.xRot + Mth.sin(time * 0.16F) * 0.08F * envelope;
               break;
            case 7:
               this.leftArm.xRot = this.rightArm.xRot = -2.55F * envelope;
               this.leftArm.yRot = 0.2F * envelope;
               this.rightArm.yRot = -0.2F * envelope;
               this.head.xRot += 0.13F * envelope;
               break;
            case 8:
               this.head.zRot += 0.08F * envelope;
               this.head.yRot -= 0.28F * envelope;
               this.leftArm.xRot -= 0.22F * envelope;
               this.rightArm.xRot += 0.22F * envelope;
               break;
            case 9:
               this.head.xRot += 0.1F * envelope;
               this.head.yRot = this.head.yRot + Mth.sin(time * 0.21F) * 0.2F * envelope;
               this.leftArm.zRot -= 0.35F * envelope;
               this.rightArm.zRot -= 0.18F * envelope;
               break;
            case 10:
               this.head.zRot -= 0.12F * envelope;
               this.leftArm.xRot = -0.72F * envelope;
               this.rightArm.xRot = -0.42F * envelope;
               this.head.xRot -= 0.16F * envelope;
               break;
            case 11:
               this.leftLeg.xRot += 0.22F * envelope;
               this.rightLeg.xRot -= 0.22F * envelope;
               this.head.zRot += 0.18F * envelope;
         }

         this.head.yRot = this.head.yRot + Mth.sin(time * 0.021F) * 0.1F + Mth.sin(time * 0.047F) * 0.04F;
         this.blink = this.blinkEnvelope(time);
         seatLeg(this.leftLeg);
         seatLeg(this.rightLeg);
         this.updateTail(time);
         this.copySkinLayers();
      }

      private static void seatLeg(ModelPart leg) {
         float cosine = Mth.cos(leg.xRot);
         float bottom = (cosine >= 0.0F ? 12.25F : -0.25F) * 0.5F * cosine + 1.6875F * Math.abs(Mth.sin(leg.xRot));
         leg.y = 24.0F - bottom;
      }

      public void applyCarriedMotion(TwoIDollCarryMotion.Pose motion, float time) {
         this.applyTailMotion(motion, time);
         this.head.yRot = this.head.yRot + Mth.sin(time * 0.17F) * 0.32F * motion.airborne();
         this.head.xRot = this.head.xRot - 0.15F * motion.airborne();
         this.head.zRot = this.head.zRot + 0.08F * Mth.sin(time * 0.12F) * motion.airborne();
         this.leftArm.xRot = Mth.lerp(motion.falling(), this.leftArm.xRot, -2.85F);
         this.rightArm.xRot = Mth.lerp(motion.falling(), this.rightArm.xRot, -2.85F);
         this.leftArm.zRot = Mth.lerp(motion.falling(), this.leftArm.zRot, 0.45F);
         this.rightArm.zRot = Mth.lerp(motion.falling(), this.rightArm.zRot, -0.45F);
         this.copySkinLayers();
      }

      public void applyTailMotion(TwoIDollCarryMotion.Pose motion, float time) {
         this.carryMotion = motion;
         this.updateTail(time);
      }

      private void updateTail(float time) {
         this.tail.pose(this.seated, this.body.y, this.body.zRot, time, this.tailStride, this.tailMoving, this.carryMotion);
      }

      public float tailYaw() {
         return this.tail.tipYaw();
      }

      public float tailPitch() {
         return this.tail.tipPitch();
      }

      public float tailBottom() {
         return this.tail.bottom();
      }

      public Vec3 tailTip() {
         return this.tail.tip();
      }

      public Vec3 tailAttachment() {
         return this.tail.attachment();
      }

      private void copySkinLayers() {
         this.hat.copyFrom(this.head);
         this.jacket.copyFrom(this.body);
         this.leftSleeve.copyFrom(this.leftArm);
         this.rightSleeve.copyFrom(this.rightArm);
         this.leftPants.copyFrom(this.leftLeg);
         this.rightPants.copyFrom(this.rightLeg);
      }
   }
}
