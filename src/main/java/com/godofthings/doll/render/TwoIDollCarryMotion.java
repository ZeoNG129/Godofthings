package com.godofthings.doll.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class TwoIDollCarryMotion {
   private static final Map<LivingEntity, TwoIDollCarryMotion.Sample> SAMPLES = new WeakHashMap<>();

   private TwoIDollCarryMotion() {
   }

   public static TwoIDollCarryMotion.Pose sample(LivingEntity carrier, float partialTick) {
      TwoIDollCarryMotion.Sample sample = SAMPLES.computeIfAbsent(carrier, ignored -> new TwoIDollCarryMotion.Sample());
      if (sample.tick != carrier.tickCount) {
         double dx = carrier.getX() - sample.x;
         double dy = carrier.getY() - sample.y;
         double dz = carrier.getZ() - sample.z;
         boolean reset = (long)carrier.tickCount - (long)sample.tick != 1L || dx * dx + dy * dy + dz * dz > 16.0;
         if (reset) {
            sample.spring = new TwoIDollCarryMotion.Spring();
            sample.distance = 0.0F;
            dz = 0.0;
            dy = 0.0;
            dx = 0.0;
         }

         boolean airborne = !carrier.onGround()
            && !carrier.isInWater()
            && !carrier.isInLava()
            && !carrier.onClimbable()
            && !carrier.isPassenger()
            && !carrier.isFallFlying()
            && (!(carrier instanceof Player player) || !player.getAbilities().flying);
         sample.distance = airborne ? sample.distance + (float)Math.max(0.0, -dy) : 0.0F;
         float yaw = carrier.yBodyRot * (float) (Math.PI / 180.0);
         float forward = (float)(dz * Math.cos((double)yaw) - dx * Math.sin((double)yaw));
         float sideways = (float)(dx * Math.cos((double)yaw) + dz * Math.sin((double)yaw));
         sample.spring.step(forward, sideways, (float)dy, airborne, sample.distance, reset ? 0.0F : Mth.wrapDegrees(carrier.yBodyRot - sample.yaw));
         sample.yaw = carrier.yBodyRot;
         sample.tick = carrier.tickCount;
         sample.x = carrier.getX();
         sample.y = carrier.getY();
         sample.z = carrier.getZ();
      }

      return sample.spring.pose(partialTick);
   }

   public static record Pose(float lift, float pitch, float roll, float airborne, float falling, float speed, float tailPitch, float tailYaw) {
      public static final TwoIDollCarryMotion.Pose STILL = new TwoIDollCarryMotion.Pose(0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

      public Pose(float lift, float pitch, float roll, float airborne, float falling, float speed) {
         this(lift, pitch, roll, airborne, falling, speed, pitch * 2.0F, roll * 2.0F);
      }

      public Pose(float lift, float pitch, float roll, float airborne, float falling) {
         this(lift, pitch, roll, airborne, falling, 0.0F);
      }

      public void apply(PoseStack stack) {
         stack.translate(0.0F, this.lift, 0.0F);
         stack.mulPose(Axis.XP.rotation(this.pitch));
         stack.mulPose(Axis.ZP.rotation(this.roll));
      }

      public TwoIDollCarryMotion.Pose blend(TwoIDollCarryMotion.Pose next, float t) {
         return new TwoIDollCarryMotion.Pose(
            Mth.lerp(t, this.lift, next.lift),
            Mth.lerp(t, this.pitch, next.pitch),
            Mth.lerp(t, this.roll, next.roll),
            Mth.lerp(t, this.airborne, next.airborne),
            Mth.lerp(t, this.falling, next.falling),
            Mth.lerp(t, this.speed, next.speed),
            Mth.lerp(t, this.tailPitch, next.tailPitch),
            Mth.lerp(t, this.tailYaw, next.tailYaw)
         );
      }
   }

   private static final class Sample {
      TwoIDollCarryMotion.Spring spring = new TwoIDollCarryMotion.Spring();
      int tick = Integer.MIN_VALUE;
      double x;
      double y;
      double z;
      float distance;
      float yaw;
   }

   public static final class Spring {
      private TwoIDollCarryMotion.Pose previous = TwoIDollCarryMotion.Pose.STILL;
      private TwoIDollCarryMotion.Pose current = TwoIDollCarryMotion.Pose.STILL;
      private float velocity;
      private float lastForward;
      private float lastSide;
      private float lastVertical;
      private float tailPitchVelocity;
      private float tailYawVelocity;
      private int airTicks;

      public void step(float forward, float sideways, float vertical, boolean airborne, float fallDistance) {
         this.step(forward, sideways, vertical, airborne, fallDistance, 0.0F);
      }

      public void step(float forward, float sideways, float vertical, boolean airborne, float fallDistance, float turn) {
         this.previous = this.current;
         this.airTicks = airborne ? this.airTicks + 1 : 0;
         float falling = airborne
            ? Mth.clamp(Math.max((fallDistance - 2.0F) / 5.0F, this.airTicks > 10 ? (-vertical - 0.35F) * 1.4F : 0.0F), 0.0F, 1.0F)
            : 0.0F;
         float excitement = Mth.lerp(0.3F, this.current.airborne, airborne ? 1.0F : 0.0F);
         float arms = Mth.lerp(0.3F, this.current.falling, falling);
         float target = airborne ? 0.035F + 0.08F * arms : 0.0F;
         this.velocity = (this.velocity + (target - this.current.lift) * 0.25F + Mth.clamp(this.lastVertical - vertical, -0.15F, 0.15F) * 0.05F) * 0.66F;
         float lift = Mth.clamp(this.current.lift + this.velocity, 0.0F, 0.16F);
         if (lift == 0.0F) {
            this.velocity = Math.max(0.0F, -this.velocity * 0.2F);
         }

         float pitch = Mth.lerp(0.28F, this.current.pitch, Mth.clamp((this.lastForward - forward) * 0.7F - forward * 0.15F, -0.16F, 0.16F));
         float roll = Mth.lerp(0.28F, this.current.roll, Mth.clamp((sideways - this.lastSide) * 0.7F + sideways * 0.15F, -0.14F, 0.14F));
         float speed = Mth.lerp(0.25F, this.current.speed, Mth.clamp((float)Math.sqrt((double)(forward * forward + sideways * sideways)) * 3.0F, 0.0F, 1.0F));
         float tailPitchTarget = Mth.clamp(
            -vertical * 0.65F + (this.lastVertical - vertical) * 1.4F + forward * 0.35F + (this.lastForward - forward) * 0.8F, -0.65F, 0.65F
         );
         float tailYawTarget = Mth.clamp(-turn * 0.035F + sideways * 0.4F + (sideways - this.lastSide) * 1.2F, -0.7F, 0.7F);
         this.tailPitchVelocity = (this.tailPitchVelocity + (tailPitchTarget - this.current.tailPitch) * 0.16F) * 0.78F;
         this.tailYawVelocity = (this.tailYawVelocity + (tailYawTarget - this.current.tailYaw) * 0.14F) * 0.8F;
         this.current = new TwoIDollCarryMotion.Pose(
            lift,
            pitch,
            roll,
            excitement,
            arms,
            speed,
            Mth.clamp(this.current.tailPitch + this.tailPitchVelocity, -0.8F, 0.8F),
            Mth.clamp(this.current.tailYaw + this.tailYawVelocity, -0.85F, 0.85F)
         );
         this.lastForward = forward;
         this.lastSide = sideways;
         this.lastVertical = vertical;
      }

      public TwoIDollCarryMotion.Pose pose(float partialTick) {
         return this.previous.blend(this.current, Mth.clamp(partialTick, 0.0F, 1.0F));
      }
   }
}
