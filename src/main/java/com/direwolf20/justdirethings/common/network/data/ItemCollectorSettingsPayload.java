package com.direwolf20.justdirethings.common.network.data;

import com.direwolf20.justdirethings.JustDireThings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

@SuppressWarnings("removal")
public record ItemCollectorSettingsPayload(boolean respectPickupDelay, boolean showParticles) {
	public static final ResourceLocation ID = new ResourceLocation(JustDireThings.MODID,
			"item_collector_settings_packet");

	public ItemCollectorSettingsPayload(final FriendlyByteBuf buffer) {
		this(buffer.readBoolean(), buffer.readBoolean());
	}

	public void write(FriendlyByteBuf buffer) {
		buffer.writeBoolean(respectPickupDelay);
		buffer.writeBoolean(showParticles);
	}
}
