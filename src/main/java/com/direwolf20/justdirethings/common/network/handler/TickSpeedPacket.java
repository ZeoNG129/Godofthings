package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.containers.basecontainers.BaseMachineContainer;
import com.direwolf20.justdirethings.common.network.data.TickSpeedPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public class TickSpeedPacket {
	public static final TickSpeedPacket INSTANCE = new TickSpeedPacket();

	public static TickSpeedPacket get() {
		return INSTANCE;
	}

	public static void handle(final TickSpeedPayload payload, final Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null)
				return;
			AbstractContainerMenu container = sender.containerMenu;

			if (container instanceof BaseMachineContainer baseMachineContainer) {
				baseMachineContainer.baseMachineBE.setTickSpeed(
						Math.min(Math.max(payload.tickSpeed(), 1), 1200));
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
