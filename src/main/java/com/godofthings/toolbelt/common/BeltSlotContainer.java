package com.godofthings.toolbelt.common;

import com.google.common.collect.Lists;
import com.godofthings.toolbelt.BeltFinder;
import com.godofthings.toolbelt.ToolBelt;
import com.godofthings.toolbelt.customslots.ExtensionSlotSlot;
import com.godofthings.toolbelt.customslots.IExtensionSlot;
import com.godofthings.toolbelt.network.ContainerSlotsHack;
import com.godofthings.toolbelt.slot.BeltExtensionSlot;
import net.minecraft.client.RecipeBookCategories;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public class BeltSlotContainer extends RecipeBookMenu<CraftingContainer>
{
    private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final ExtensionSlotSlot slotBelt;
    private final IExtensionSlot extensionSlot;

    private final CraftingContainer craftingInventory = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer craftResultInventory = new ResultContainer();
    private final Player player;

    public BeltSlotContainer(int id, Inventory playerInventory)
    {
        super(ToolBelt.BELT_SLOT_MENU.get(), id);
        this.player = playerInventory.player;
        this.addSlot(new ResultSlot(playerInventory.player, this.craftingInventory, this.craftResultInventory, 0, 154, 28));

        for (int i = 0; i < 2; ++i)
        {
            for (int j = 0; j < 2; ++j)
            {
                this.addSlot(new Slot(this.craftingInventory, j + i * 2, 98 + j * 18, 18 + i * 18));
            }
        }

        for (int k = 0; k < 4; ++k)
        {
            final EquipmentSlot equipmentslottype = ARMOR_SLOTS[k];
            this.addSlot(new Slot(playerInventory, 39 - k, 8, 8 + k * 18)
            {
                public int getMaxStackSize()
                {
                    return 1;
                }

                public boolean mayPlace(ItemStack stack)
                {
                    return stack.canEquip(equipmentslottype, player);
                }

                public boolean mayPickup(Player playerIn)
                {
                    ItemStack itemstack = this.getItem();
                    return !itemstack.isEmpty() && !playerIn.isCreative() && EnchantmentHelper.hasBindingCurse(itemstack) ? false : super.mayPickup(playerIn);
                }
            });
        }

        for (int l = 0; l < 3; ++l)
        {
            for (int j1 = 0; j1 < 9; ++j1)
            {
                this.addSlot(new Slot(playerInventory, j1 + (l + 1) * 9, 8 + j1 * 18, 84 + l * 18));
            }
        }

        for (int i1 = 0; i1 < 9; ++i1)
        {
            this.addSlot(new Slot(playerInventory, i1, 8 + i1 * 18, 142));
        }

        this.addSlot(new Slot(playerInventory, 40, 77, 62)
        {
            {
                setBackground(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD);
            }
        });

        BeltExtensionSlot container = playerInventory.player.getCapability(BeltExtensionSlot.CAPABILITY)
                .orElseThrow(() -> new RuntimeException("Item handler not present."));

        extensionSlot = container.getBelt();

        this.addSlot(slotBelt = new ExtensionSlotSlot(BeltSlotContainer.this.extensionSlot, 77, 44));

        if (playerInventory.player.level().isClientSide)
        {
            ToolBelt.channel.sendToServer(new ContainerSlotsHack());
        }
    }

    @Override
    public List<RecipeBookCategories> getRecipeBookCategories()
    {
        return Lists.newArrayList(RecipeBookCategories.CRAFTING_SEARCH, RecipeBookCategories.CRAFTING_EQUIPMENT, RecipeBookCategories.CRAFTING_BUILDING_BLOCKS, RecipeBookCategories.CRAFTING_MISC, RecipeBookCategories.CRAFTING_REDSTONE);
    }

    @Override
    public RecipeBookType getRecipeBookType()
    {
        return RecipeBookType.CRAFTING;
    }

    @Override
    public boolean shouldMoveToInventory(int slot)
    {
        return slot != this.getResultSlotIndex();
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedContents itemHelperIn)
    {
        this.craftingInventory.fillStackedContents(itemHelperIn);
    }

    @Override
    public void clearCraftingContent()
    {
        this.craftResultInventory.clearContent();
        this.craftingInventory.clearContent();
    }

    @Override
    public boolean recipeMatches(Recipe<? super CraftingContainer> recipeIn)
    {
        return recipeIn.matches(this.craftingInventory, this.player.level());
    }

    @Override
    public void slotsChanged(Container inventoryIn)
    {
        // 内联 CraftingMenu.slotChangedCraftingGrid（原版为 protected static，避免 access transformer）
        Level level = this.player.level();
        if (!level.isClientSide)
        {
            ServerPlayer serverplayer = (ServerPlayer) this.player;
            ItemStack itemstack = ItemStack.EMPTY;
            Optional<CraftingRecipe> optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, this.craftingInventory, level);
            if (optional.isPresent())
            {
                CraftingRecipe craftingrecipe = optional.get();
                if (this.craftResultInventory.setRecipeUsed(level, serverplayer, craftingrecipe))
                {
                    ItemStack itemstack1 = craftingrecipe.assemble(this.craftingInventory, level.registryAccess());
                    if (itemstack1.isItemEnabled(level.enabledFeatures()))
                    {
                        itemstack = itemstack1;
                    }
                }
            }

            this.craftResultInventory.setItem(0, itemstack);
            this.setRemoteSlot(0, itemstack);
            serverplayer.connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0, itemstack));
        }
    }

    @Override
    public void removed(Player playerIn)
    {
        super.removed(playerIn);

        this.craftResultInventory.clearContent();

        if (!playerIn.level().isClientSide)
        {
            this.clearContainer(playerIn, this.craftingInventory);
            BeltFinder.sendSync(playerIn);
        }
    }

    @Override
    public boolean stillValid(Player playerIn)
    {
        return true;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn)
    {
        return slotIn.container != this.craftResultInventory && super.canTakeItemForPickAll(stack, slotIn);
    }

    @Override
    public int getResultSlotIndex()
    {
        return 0;
    }

    @Override
    public int getGridWidth()
    {
        return this.craftingInventory.getWidth();
    }

    @Override
    public int getGridHeight()
    {
        return this.craftingInventory.getHeight();
    }

    @Override
    public int getSize()
    {
        return 5;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index)
    {
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem())
        {
            ItemStack remaining = ItemStack.EMPTY;
            ItemStack slotContents = slot.getItem();
            remaining = slotContents.copy();

            if (index == slotBelt.index)
            {
                if (!this.moveItemStackTo(slotContents, 9, 45, false))
                {
                    return ItemStack.EMPTY;
                }

                return remaining;
            }
            else if (slot.mayPlace(slotContents))
            {
                if (!this.moveItemStackTo(slotContents, slotBelt.index, slotBelt.index + 1, false))
                {
                    return ItemStack.EMPTY;
                }
            }
        }

        ItemStack itemstack = ItemStack.EMPTY;
        if (slot != null && slot.hasItem())
        {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            EquipmentSlot equipmentslottype = Mob.getEquipmentSlotForItem(itemstack);
            if (index == 0)
            {
                if (!this.moveItemStackTo(itemstack1, 9, 45, true))
                {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            }
            else if (index >= 1 && index < 5)
            {
                if (!this.moveItemStackTo(itemstack1, 9, 45, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 5 && index < 9)
            {
                if (!this.moveItemStackTo(itemstack1, 9, 45, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (equipmentslottype.getType() == EquipmentSlot.Type.ARMOR && !this.slots.get(8 - equipmentslottype.getIndex()).hasItem())
            {
                int i = 8 - equipmentslottype.getIndex();
                if (!this.moveItemStackTo(itemstack1, i, i + 1, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (equipmentslottype == EquipmentSlot.OFFHAND && !this.slots.get(45).hasItem())
            {
                if (!this.moveItemStackTo(itemstack1, 45, 46, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 9 && index < 36)
            {
                if (!this.moveItemStackTo(itemstack1, 36, 45, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (index >= 36 && index < 45)
            {
                if (!this.moveItemStackTo(itemstack1, 9, 36, false))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.moveItemStackTo(itemstack1, 9, 45, false))
            {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty())
            {
                slot.set(ItemStack.EMPTY);
            }
            else
            {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount())
            {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
            if (index == 0)
            {
                playerIn.drop(itemstack1, false);
            }
        }

        return itemstack;
    }
}
