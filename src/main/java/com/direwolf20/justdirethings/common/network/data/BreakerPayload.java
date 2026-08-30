package com.direwolf20.justdirethings.common.network.data;

import net.minecraft.network.FriendlyByteBuf;

public record BreakerPayload(boolean sneaking) {
	public BreakerPayload(final FriendlyByteBuf buffer) {
		this(buffer.readBoolean());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeBoolean(sneaking);
	}
}
