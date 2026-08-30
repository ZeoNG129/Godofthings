package com.godofthings.toolbelt.slot;

import com.google.common.collect.ImmutableList;
import com.godofthings.toolbelt.ToolBelt;
import com.godofthings.toolbelt.ToolBeltConfig;
import com.godofthings.toolbelt.customslots.ExtensionSlotItemHandler;
import com.godofthings.toolbelt.customslots.IExtensionContainer;
import com.godofthings.toolbelt.customslots.IExtensionSlot;
import com.godofthings.toolbelt.network.SyncBeltSlotContents;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * 自定义皮带栏：给玩家/盔甲架附加一个能力，用于穿戴工具皮带。
 * 原项目：Tool Belt（gigaherz，BSD 3-Clause）。
 */
public class BeltExtensionSlot implements IExtensionContainer, INBTSerializable<CompoundTag>
{
    private static final Logger LOGGER = LogManager.getLogger();

    private static final ResourceLocation CAPABILITY_ID = ResourceLocation.tryBuild(ToolBelt.MODID, "belt_slot");

    public static Capability<BeltExtensionSlot> CAPABILITY
            = CapabilityManager.get(new CapabilityToken<>()
    {
    });

    public static void register()
    {
        MinecraftForge.EVENT_BUS.register(new AttachHandlers());
        MinecraftForge.EVENT_BUS.register(new EventHandlers());
    }

    public static LazyOptional<BeltExtensionSlot> get(LivingEntity player)
    {
        return player.getCapability(CAPABILITY);
    }

    static class AttachHandlers
    {
        @SubscribeEvent
        public void attachCapabilities(AttachCapabilitiesEvent<Entity> event)
        {
            final Entity entity = event.getObject();
            if (entity instanceof Player || entity instanceof ArmorStand)
            {
                event.addCapability(CAPABILITY_ID, new ICapabilitySerializable<CompoundTag>()
                {
                    final BeltExtensionSlot extensionContainer = new BeltExtensionSlot((LivingEntity) entity)
                    {
                        @Override
                        public void onContentsChanged(IExtensionSlot slot)
                        {
                            if (!ToolBeltConfig.customBeltSlotEnabled)
                                return;
                            if (!getOwner().level().isClientSide)
                                syncTo(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(this::getOwner));
                        }
                    };

                    final LazyOptional<BeltExtensionSlot> extensionContainerSupplier = LazyOptional.of(() -> extensionContainer);

                    @Override
                    public CompoundTag serializeNBT()
                    {
                        return extensionContainer.serializeNBT();
                    }

                    @Override
                    public void deserializeNBT(CompoundTag nbt)
                    {
                        extensionContainer.deserializeNBT(nbt);
                    }

                    @Override
                    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction facing)
                    {
                        if (CAPABILITY == capability)
                            return extensionContainerSupplier.cast();

                        return LazyOptional.empty();
                    }
                });
            }
        }
    }

    private static class EventHandlers
    {
        @SubscribeEvent
        public void joinWorld(PlayerEvent.PlayerLoggedInEvent event)
        {
            if (!ToolBeltConfig.customBeltSlotEnabled)
                return;
            Player target = event.getEntity();
            if (target.level().isClientSide)
                return;
            get(target).ifPresent(BeltExtensionSlot::syncToSelf);
        }

        @SubscribeEvent
        public void joinWorld(PlayerEvent.PlayerChangedDimensionEvent event)
        {
            if (!ToolBeltConfig.customBeltSlotEnabled)
                return;
            Player target = event.getEntity();
            if (target.level().isClientSide)
                return;
            get(target).ifPresent(BeltExtensionSlot::syncToSelf);
        }

        @SubscribeEvent
        public void track(PlayerEvent.StartTracking event)
        {
            if (!ToolBeltConfig.customBeltSlotEnabled)
                return;
            Entity target = event.getTarget();
            if (target.level().isClientSide)
                return;
            if (target instanceof Player)
            {
                get((LivingEntity) target).ifPresent(BeltExtensionSlot::syncToSelf);
            }
        }

        @SubscribeEvent
        public void entityTick(TickEvent.PlayerTickEvent event)
        {
            if (event.phase != TickEvent.Phase.END)
                return;

            if (ToolBeltConfig.customBeltSlotEnabled)
                get(event.player).ifPresent(BeltExtensionSlot::tickAllSlots);
            else
                get(event.player).ifPresent(BeltExtensionSlot::dropContents);
        }

        @SubscribeEvent
        public void playerDeath(LivingDropsEvent event)
        {
            if (!ToolBeltConfig.customBeltSlotEnabled)
                return;
            LivingEntity entity = event.getEntity();

            get(entity).ifPresent((instance) -> {
                IExtensionSlot belt = instance.getBelt();
                ItemStack stack = belt.getContents();
                if (EnchantmentHelper.hasVanishingCurse(stack))
                {
                    stack = ItemStack.EMPTY;
                    belt.setContents(stack);
                }
                if (stack.getCount() > 0)
                {
                    if (entity instanceof Player player)
                    {
                        if (!entity.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !player.isSpectator())
                        {
                            Collection<ItemEntity> old = entity.captureDrops(event.getDrops());
                            player.drop(stack, true, false);
                            entity.captureDrops(old);
                            belt.setContents(ItemStack.EMPTY);
                        }
                    }
                    else
                    {
                        entity.spawnAtLocation(stack);
                        belt.setContents(ItemStack.EMPTY);
                    }
                }
            });
        }

        @SubscribeEvent
        public void playerClone(PlayerEvent.Clone event)
        {
            if (!ToolBeltConfig.customBeltSlotEnabled)
                return;
            Player oldPlayer = event.getOriginal();

            // FIXME: workaround for a forge issue that seems to be reappearing too often
            // at this time it's only needed when returning from the end alive
            oldPlayer.revive();

            Player newPlayer = event.getEntity();

            get(oldPlayer).ifPresent((oldBelt) -> {
                ItemStack stack = oldBelt.getBelt().getContents();
                get(newPlayer).map(newBelt -> {
                    // Transfer any remaining item. If it was death and keepInventory was off,
                    // it will have been removed in LivingDropsEvent.
                    newBelt.getBelt().setContents(stack);
                    return Unit.INSTANCE;
                }).orElseGet(() -> {
                    if (stack.getCount() > 0)
                    {
                        oldPlayer.drop(stack, true, false);
                    }
                    return Unit.INSTANCE;
                });
            });
        }
    }

    private void dropContents()
    {
        for (IExtensionSlot slot : slots)
        {
            ItemStack stack = slot.getContents();
            if (stack.getCount() > 0)
            {
                if (owner instanceof Player)
                    ItemHandlerHelper.giveItemToPlayer((Player) owner, stack);
                else
                    owner.spawnAtLocation(stack, 0.1f);
                slot.setContents(ItemStack.EMPTY);
            }
        }
    }

    private void syncToSelf()
    {
        syncTo((Player) owner);
    }

    protected void syncTo(Player target)
    {
        SyncBeltSlotContents message = new SyncBeltSlotContents((Player) owner, this);
        ToolBelt.channel.sendTo(message, ((ServerPlayer) target).connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    protected void syncTo(PacketDistributor.PacketTarget target)
    {
        SyncBeltSlotContents message = new SyncBeltSlotContents((Player) owner, this);
        ToolBelt.channel.send(target, message);
    }

    ////////////////////////////////////////////////////////////
    // Equipment container implementation
    //
    public static final ResourceLocation BELT = ToolBelt.BELT_SLOT_TYPE;

    private final LivingEntity owner;
    private final ItemStackHandler inventory = new ItemStackHandler(1)
    {
        @Override
        protected void onContentsChanged(int slot)
        {
            super.onContentsChanged(slot);
            belt.onContentsChanged();
        }
    };
    private final ExtensionSlotItemHandler belt = new ExtensionSlotItemHandler(this, BELT, inventory, 0);
    private final ImmutableList<IExtensionSlot> slots = ImmutableList.of(belt);

    private BeltExtensionSlot(LivingEntity owner)
    {
        this.owner = owner;
    }

    @Nonnull
    @Override
    public LivingEntity getOwner()
    {
        return owner;
    }

    @Nonnull
    @Override
    public ImmutableList<IExtensionSlot> getSlots()
    {
        return slots;
    }

    @Override
    public void onContentsChanged(IExtensionSlot slot)
    {

    }

    @Nonnull
    public IExtensionSlot getBelt()
    {
        return belt;
    }

    private void tickAllSlots()
    {
        for (IExtensionSlot slot : slots)
        {
            ((ExtensionSlotItemHandler) slot).onWornTick();
        }
    }

    public void setAll(NonNullList<ItemStack> stacks)
    {
        List<IExtensionSlot> slots = getSlots();
        for (int i = 0; i < slots.size(); i++)
        {
            slots.get(i).setContents(stacks.get(i));
        }
    }

    @Override
    public CompoundTag serializeNBT()
    {
        return inventory.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt)
    {
        inventory.deserializeNBT(nbt);
    }
}
