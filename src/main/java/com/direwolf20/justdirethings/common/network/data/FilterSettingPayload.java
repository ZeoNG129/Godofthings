package com.direwolf20.justdirethings.common.network.data;

import com.direwolf20.justdirethings.JustDireThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public record FilterSettingPayload(boolean allowList, boolean compareNBT, int blockItemFilter) {
	public static final ResourceLocation ID = new ResourceLocation(JustDireThings.MODID, "filter_setting_packet");

	public FilterSettingPayload(final FriendlyByteBuf buffer) {
		this(buffer.readBoolean(), buffer.readBoolean(), buffer.readInt());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeBoolean(allowList);
		buffer.writeBoolean(compareNBT);
		buffer.writeInt(blockItemFilter);
	}

}
