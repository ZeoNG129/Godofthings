package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.blockentities.BlockSwapperT1BE;
import com.direwolf20.justdirethings.common.containers.basecontainers.BaseMachineContainer;
import com.direwolf20.justdirethings.common.network.data.SwapperPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public class SwapperPacket {
	public static final SwapperPacket INSTANCE = new SwapperPacket();

	public static SwapperPacket get() {
		return INSTANCE;
	}

	public static void handle(final SwapperPayload payload, final Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null)
				return;
			AbstractContainerMenu container = sender.containerMenu;

			if (container instanceof BaseMachineContainer baseMachineContainer
					&& baseMachineContainer.baseMachineBE instanceof BlockSwapperT1BE swapper) {
				swapper.setSwapperSettings(payload.swapBlocks(), payload.swap_entity_type());
			}
		});
		ctx.get().setPacketHandled(true);
	}
}
