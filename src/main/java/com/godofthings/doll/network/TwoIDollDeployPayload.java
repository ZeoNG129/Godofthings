package com.godofthings.doll.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record TwoIDollDeployPayload(boolean offhand) implements CustomPacketPayload {
   public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<TwoIDollDeployPayload> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type(
      ResourceLocation.fromNamespaceAndPath("godofthings", "two_i_deploy")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TwoIDollDeployPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL, TwoIDollDeployPayload::offhand, TwoIDollDeployPayload::new
   );

   public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
