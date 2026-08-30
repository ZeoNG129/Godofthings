package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.blockentities.ClickerT1BE;
import com.direwolf20.justdirethings.common.containers.basecontainers.BaseMachineContainer;
import com.direwolf20.justdirethings.common.network.data.ClickerPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public class ClickerPacket {
	public static final ClickerPacket INSTANCE = new ClickerPacket();

	public static ClickerPacket get() {
		return INSTANCE;
	}

	public static void handle(final ClickerPayload payload, final Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null)
				return;
			AbstractContainerMenu container = sender.containerMenu;

			if (container instanceof BaseMachineContainer baseMachineContainer
					&& baseMachineContainer.baseMachineBE instanceof ClickerT1BE clicker) {
				clicker.setClickerSettings(payload.clickType(), payload.clickTarget(), payload.sneaking(),
						payload.showFakePlayer(), payload.maxHoldTicks());
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
