package com.direwolf20.justdirethings.common.network.data;

import com.direwolf20.justdirethings.JustDireThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public record RedstoneSettingPayload(int redstoneMode) {
	public static final ResourceLocation ID = new ResourceLocation(JustDireThings.MODID, "redstone_setting_packet");

	public RedstoneSettingPayload(final FriendlyByteBuf buffer) {
		this(buffer.readInt());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeInt(redstoneMode);
	}

}
