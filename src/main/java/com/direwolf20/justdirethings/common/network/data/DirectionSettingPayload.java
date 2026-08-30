package com.direwolf20.justdirethings.common.network.data;

import com.direwolf20.justdirethings.JustDireThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public record DirectionSettingPayload(int direction) {
	public static final ResourceLocation ID = new ResourceLocation(JustDireThings.MODID, "direction_setting_packet");

	public DirectionSettingPayload(final FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(direction);
	}

}
