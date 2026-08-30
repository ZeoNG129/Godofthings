package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.blockentities.BlockBreakerT1BE;
import com.direwolf20.justdirethings.common.containers.basecontainers.BaseMachineContainer;
import com.direwolf20.justdirethings.common.network.data.BreakerPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BreakerPacket {
	public static final BreakerPacket INSTANCE = new BreakerPacket();

	public static BreakerPacket get() {
		return INSTANCE;
	}

	public static void handle(final BreakerPayload payload, final Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null)
				return;
			AbstractContainerMenu container = sender.containerMenu;

			if (container instanceof BaseMachineContainer baseMachineContainer
					&& baseMachineContainer.baseMachineBE instanceof BlockBreakerT1BE breaker) {
				breaker.setBreakerSettings(payload.sneaking());
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
