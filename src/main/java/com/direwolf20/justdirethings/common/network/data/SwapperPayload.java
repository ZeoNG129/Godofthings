package com.direwolf20.justdirethings.common.network.data;

import com.direwolf20.justdirethings.JustDireThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public record SwapperPayload(boolean swapBlocks, int swap_entity_type) {
	public static final ResourceLocation ID = new ResourceLocation(JustDireThings.MODID, "swapper_packet");

	public SwapperPayload(final FriendlyByteBuf buffer) {
		this(buffer.readBoolean(), buffer.readInt());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeBoolean(swapBlocks);
		buffer.writeInt(swap_entity_type);
	}

}
