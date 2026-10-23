package com.godofthings.doll.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record TwoIDialoguePayload(int entityId, boolean inviteReady, int giftScore, boolean open) implements CustomPacketPayload {
   public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<TwoIDialoguePayload> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type(
      ResourceLocation.fromNamespaceAndPath("godofthings", "two_i_dialogue")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TwoIDialoguePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.VAR_INT,
      TwoIDialoguePayload::entityId,
      ByteBufCodecs.BOOL,
      TwoIDialoguePayload::inviteReady,
      ByteBufCodecs.VAR_INT,
      TwoIDialoguePayload::giftScore,
      ByteBufCodecs.BOOL,
      TwoIDialoguePayload::open,
      TwoIDialoguePayload::new
   );

   public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
