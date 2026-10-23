package com.godofthings.doll.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record TwoIStoryLibraryRequestPayload(int requestId) implements CustomPacketPayload {
   public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<TwoIStoryLibraryRequestPayload> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type(
      ResourceLocation.fromNamespaceAndPath("godofthings", "two_i_story_library_request")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TwoIStoryLibraryRequestPayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT, TwoIStoryLibraryRequestPayload::requestId, TwoIStoryLibraryRequestPayload::new
   );

   public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
