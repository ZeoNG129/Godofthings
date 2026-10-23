package com.godofthings.doll.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record TwoIDollStoryStartPayload(int entityId) implements CustomPacketPayload {
   public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<TwoIDollStoryStartPayload> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type(
      ResourceLocation.fromNamespaceAndPath("godofthings", "two_i_story_start")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TwoIDollStoryStartPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, TwoIDollStoryStartPayload::entityId, TwoIDollStoryStartPayload::new
   );

   public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
