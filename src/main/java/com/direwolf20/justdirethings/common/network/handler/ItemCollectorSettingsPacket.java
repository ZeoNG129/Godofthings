package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.blockentities.ItemCollectorBE;
import com.direwolf20.justdirethings.common.containers.ItemCollectorContainer;
import com.direwolf20.justdirethings.common.network.data.ItemCollectorSettingsPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ItemCollectorSettingsPacket {
	public static void handle(final ItemCollectorSettingsPayload payload, final Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null)
				return;
			AbstractContainerMenu container = sender.containerMenu;
			if (container instanceof ItemCollectorContainer itemCollectorContainer
					&& itemCollectorContainer.baseMachineBE instanceof ItemCollectorBE be) {
				be.setSettings(payload.respectPickupDelay(), payload.showParticles());
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
