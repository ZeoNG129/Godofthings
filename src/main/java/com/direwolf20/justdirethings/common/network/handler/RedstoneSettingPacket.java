package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.blockentities.basebe.RedstoneControlledBE;
import com.direwolf20.justdirethings.common.containers.basecontainers.BaseMachineContainer;
import com.direwolf20.justdirethings.common.network.data.RedstoneSettingPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public class RedstoneSettingPacket {
	public static final RedstoneSettingPacket INSTANCE = new RedstoneSettingPacket();

	public static RedstoneSettingPacket get() {
		return INSTANCE;
	}

	public static void handle(final RedstoneSettingPayload payload, final Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null)
				return;
			AbstractContainerMenu container = sender.containerMenu;

			if (container instanceof BaseMachineContainer baseMachineContainer
					&& baseMachineContainer.baseMachineBE instanceof RedstoneControlledBE redstoneControlledBE) {
				redstoneControlledBE.setRedstoneSettings(payload.redstoneMode());
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
