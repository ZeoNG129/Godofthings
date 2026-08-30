package com.direwolf20.justdirethings.common.network.handler;

import com.direwolf20.justdirethings.common.containers.slots.FilterBasicSlot;
import com.direwolf20.justdirethings.common.network.data.GhostSlotPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public class GhostSlotPacket {
	public static final GhostSlotPacket INSTANCE = new GhostSlotPacket();

	public static GhostSlotPacket get() {
		return INSTANCE;
	}

	public static void handle(final GhostSlotPayload payload, final Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			ServerPlayer sender = ctx.get().getSender();
			if (sender == null)
				return;

			AbstractContainerMenu container = sender.containerMenu;
			if (container == null)
				return;

			Slot slot = container.slots.get(payload.slotNumber());
			ItemStack stack = payload.stack();
			stack.setCount(payload.count());
			if (slot instanceof FilterBasicSlot)
				slot.set(stack);
		});
		ctx.get().setPacketHandled(true);
	}
}
