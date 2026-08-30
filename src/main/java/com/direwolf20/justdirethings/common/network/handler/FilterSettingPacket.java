package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.blockentities.basebe.FilterableBE;
import com.direwolf20.justdirethings.common.containers.basecontainers.BaseMachineContainer;
import com.direwolf20.justdirethings.common.network.data.FilterSettingPayload;
import com.direwolf20.justdirethings.util.interfacehelpers.FilterData;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public class FilterSettingPacket {
	public static final FilterSettingPacket INSTANCE = new FilterSettingPacket();

	public static FilterSettingPacket get() {
		return INSTANCE;
	}

	public static void handle(final FilterSettingPayload payload, final Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null)
				return;
			AbstractContainerMenu container = sender.containerMenu;

			if (container instanceof BaseMachineContainer baseMachineContainer
					&& baseMachineContainer.baseMachineBE instanceof FilterableBE filterableBE) {
				filterableBE.setFilterSettings(
						new FilterData(payload.allowList(), payload.compareNBT(), payload.blockItemFilter()));
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
