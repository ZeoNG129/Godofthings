package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.containers.basecontainers.BaseMachineContainer;
import com.direwolf20.justdirethings.common.network.data.DirectionSettingPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public class DirectionSettingPacket {
	public static final DirectionSettingPacket INSTANCE = new DirectionSettingPacket();

	public static DirectionSettingPacket get() {
		return INSTANCE;
	}

	public static void handle(final DirectionSettingPayload payload, final Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null)
				return;
			AbstractContainerMenu container = sender.containerMenu;

			if (container instanceof BaseMachineContainer baseMachineContainer) {
				baseMachineContainer.baseMachineBE.setDirection(payload.direction());
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
