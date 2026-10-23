package com.godofthings.doll.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record TwoIItemDialoguePayload(boolean offhand, int bond, boolean invitationUsed) implements CustomPacketPayload {
   public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<TwoIItemDialoguePayload> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type(
      ResourceLocation.fromNamespaceAndPath("godofthings", "two_i_item_dialogue")
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, TwoIItemDialoguePayload> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.BOOL,
      TwoIItemDialoguePayload::offhand,
      ByteBufCodecs.VAR_INT,
      TwoIItemDialoguePayload::bond,
      ByteBufCodecs.BOOL,
      TwoIItemDialoguePayload::invitationUsed,
      TwoIItemDialoguePayload::new
   );

   public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
