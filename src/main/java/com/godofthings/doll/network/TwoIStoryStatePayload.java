package com.godofthings.doll.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record TwoIStoryStatePayload(int entityId, long sessionId, int chapter, int bond, int result, int round) implements CustomPacketPayload {
   public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<TwoIStoryStatePayload> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type(
      ResourceLocation.fromNamespaceAndPath("godofthings", "two_i_story_state")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TwoIStoryStatePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      TwoIStoryStatePayload::entityId,
      ByteBufCodecs.VAR_LONG,
      TwoIStoryStatePayload::sessionId,
      ByteBufCodecs.VAR_INT,
      TwoIStoryStatePayload::chapter,
      ByteBufCodecs.VAR_INT,
      TwoIStoryStatePayload::bond,
      ByteBufCodecs.VAR_INT,
      TwoIStoryStatePayload::result,
      ByteBufCodecs.VAR_INT,
      TwoIStoryStatePayload::round,
      TwoIStoryStatePayload::new
   );

   public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
