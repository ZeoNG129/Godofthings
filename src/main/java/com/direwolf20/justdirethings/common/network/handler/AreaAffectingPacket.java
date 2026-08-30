package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.blockentities.basebe.AreaAffectingBE;
import com.direwolf20.justdirethings.common.containers.basecontainers.BaseMachineContainer;
import com.direwolf20.justdirethings.common.network.data.AreaAffectingPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public class AreaAffectingPacket {
	public static final AreaAffectingPacket INSTANCE = new AreaAffectingPacket();

	public static AreaAffectingPacket get() {
		return INSTANCE;
	}

	public static void handle(final AreaAffectingPayload payload, final Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null)
				return;
			AbstractContainerMenu container = sender.containerMenu;

			if (container instanceof BaseMachineContainer baseMachineContainer
					&& baseMachineContainer.baseMachineBE instanceof AreaAffectingBE areaAffectingBE) {
				areaAffectingBE.setAreaSettings(payload.xRadius(), payload.yRadius(), payload.zRadius(),
						payload.xOffset(), payload.yOffset(), payload.zOffset(), payload.renderArea());
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
