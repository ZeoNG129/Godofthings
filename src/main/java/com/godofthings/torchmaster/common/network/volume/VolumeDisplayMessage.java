package com.godofthings.torchmaster.common.network.volume;

import java.util.function.Supplier;
import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class VolumeDisplayMessage {
   private Vec3i position;
   private int range;
   private int color;
   private boolean showVolume;
   private boolean showLocation;

   public static void encode(VolumeDisplayMessage msg, FriendlyByteBuf buffer) {
      buffer.writeInt(msg.position.getX());
      buffer.writeInt(msg.position.getY());
      buffer.writeInt(msg.position.getZ());
      buffer.writeInt(msg.range);
      buffer.writeInt(msg.color);
      buffer.writeBoolean(msg.showVolume);
      buffer.writeBoolean(msg.showLocation);
   }

   public static VolumeDisplayMessage decode(FriendlyByteBuf buffer) {
      VolumeDisplayMessage msg = new VolumeDisplayMessage();
      int x = buffer.readInt();
      int y = buffer.readInt();
      int z = buffer.readInt();
      msg.position = new Vec3i(x, y, z);
      msg.range = buffer.readInt();
      msg.color = buffer.readInt();
      msg.showVolume = buffer.readBoolean();
      msg.showLocation = buffer.readBoolean();
      return msg;
   }

   public static void dispatch(VolumeDisplayMessage msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> VolumeDisplayMessageHandler.handle(msg, ctx)));
      ctx.get().setPacketHandled(true);
   }

   public static VolumeDisplayMessage create(Vec3i pos, int range, int color, boolean showVolume, boolean showLocation) {
      VolumeDisplayMessage msg = new VolumeDisplayMessage();
      msg.position = pos;
      msg.range = range;
      msg.color = color;
      msg.showVolume = showVolume;
      msg.showLocation = showLocation;
      return msg;
   }

   public Vec3i getPosition() {
      return this.position;
   }

   public int getColor() {
      return this.color;
   }

   public int getRange() {
      return this.range;
   }

   public boolean showVolume() {
      return this.showVolume;
   }

   public boolean showLocation() {
      return this.showLocation;
   }
}
