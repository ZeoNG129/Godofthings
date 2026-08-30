package com.direwolf20.justdirethings.common.network.data;

import com.direwolf20.justdirethings.JustDireThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public record ClickerPayload(int clickType, int clickTarget, boolean sneaking, boolean showFakePlayer,
		int maxHoldTicks) {
	public static final ResourceLocation ID = new ResourceLocation(JustDireThings.MODID, "clicker_packet");

	public ClickerPayload(final FriendlyByteBuf buffer) {
		this(buffer.readInt(), buffer.readInt(), buffer.readBoolean(), buffer.readBoolean(), buffer.readInt());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(clickType);
		buffer.writeInt(clickTarget);
		buffer.writeBoolean(sneaking);
		buffer.writeBoolean(showFakePlayer);
		buffer.writeInt(maxHoldTicks);
	}

}
