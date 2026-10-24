package com.godofthings.infinityprovider;

import appeng.api.stacks.AEItemKey;
import appeng.block.crafting.PatternProviderBlock;
import appeng.block.crafting.PushDirection;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.menu.locator.MenuLocators;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ItemInteractionResult;

import appeng.util.InteractionUtil;

/**
 * 四档供应器的方块实体与对应方块（移植自 InfinityPatternProvider）。
 * 唯一实质差异是 {@code createLogic} 传入的槽位数：1 / 32 / 1024 / 32723（无限）。
 */
public final class InfProviderBlockEntity {

    /** 无限档的槽位数（上游取的 32723，恰好能被 32 整除并覆盖 1023 页）。 */
    public static final int INFINITY_SLOTS = 32723;

    private InfProviderBlockEntity() {
    }

    // ==================== 1 槽 ====================

    public static class Provider1Block extends InfProviderBlock<Provider1> {
        public Provider1Block() {
            super(metalProps());
            this.registerDefaultState(this.defaultBlockState()
                    .setValue(PatternProviderBlock.PUSH_DIRECTION, PushDirection.ALL));
        }

        @Override
        public void neighborChanged(BlockState blockState, net.minecraft.world.level.Level level,
                BlockPos blockPosition, Block block, BlockPos fromPosition, boolean isMoving) {
            Provider1 blockEntity = this.getBlockEntity(level, blockPosition);
            if (blockEntity != null) {
                blockEntity.getLogic().updateRedstoneState();
            }
        }

        @Override
        public ItemInteractionResult checkPlayerInteractionWhenUseItem(Provider1 blockEntity, ItemStack itemStack,
                net.minecraft.world.level.Level world, BlockPos blockPosition, BlockHitResult blockHitResult,
                Player player) {
            if (InteractionUtil.canWrenchRotate(itemStack)) {
                this.setSide(world, blockPosition, blockHitResult.getDirection());
                return ItemInteractionResult.sidedSuccess(world.isClientSide());
            }
            return null;
        }

        @Override
        public void openGui(BlockEntity blockEntity, Player player) {
            if (blockEntity instanceof Provider1 patternProvider) {
                patternProvider.openMenu(player, MenuLocators.forBlockEntity(patternProvider));
            }
        }
    }

    public static class Provider1 extends InfEnhancedBlockEntity {
        public Provider1(BlockPos pos, BlockState blockState) {
            super(InfProviderRegistry.P1_BE.get(), pos, blockState);
        }

        @Override
        protected PatternProviderLogic createLogic() {
            return new InfProviderLogic(this.getMainNode(), this, 1, this);
        }

        @Override
        public void openMenu(Player player, MenuHostLocator locator) {
            MenuOpener.open(InfProviderMenu.Provider1Menu.TYPE, player, locator);
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
            MenuOpener.returnTo(InfProviderMenu.Provider1Menu.TYPE, player, subMenu.getLocator());
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return new ItemStack((ItemLike) InfProviderRegistry.P1_BLOCK.get());
        }

        @Override
        public AEItemKey getTerminalIcon() {
            return AEItemKey.of((ItemLike) InfProviderRegistry.P1_BLOCK.get());
        }

        @Override
        public void onReady() {
            super.onReady();
            this.getMainNode().setVisualRepresentation(
                    new ItemStack((ItemLike) InfProviderRegistry.P1_BLOCK.get()));
        }
    }

    // ==================== 32 槽 ====================

    public static class Provider32Block extends InfProviderBlock<Provider32> {
        public Provider32Block() {
            super(metalProps());
            this.registerDefaultState(this.defaultBlockState()
                    .setValue(PatternProviderBlock.PUSH_DIRECTION, PushDirection.ALL));
        }

        @Override
        public void neighborChanged(BlockState blockState, net.minecraft.world.level.Level level,
                BlockPos blockPosition, Block block, BlockPos fromPosition, boolean isMoving) {
            Provider32 blockEntity = this.getBlockEntity(level, blockPosition);
            if (blockEntity != null) {
                blockEntity.getLogic().updateRedstoneState();
            }
        }

        @Override
        public ItemInteractionResult checkPlayerInteractionWhenUseItem(Provider32 blockEntity, ItemStack itemStack,
                net.minecraft.world.level.Level world, BlockPos blockPosition, BlockHitResult blockHitResult,
                Player player) {
            if (InteractionUtil.canWrenchRotate(itemStack)) {
                this.setSide(world, blockPosition, blockHitResult.getDirection());
                return ItemInteractionResult.sidedSuccess(world.isClientSide());
            }
            return null;
        }

        @Override
        public void openGui(BlockEntity blockEntity, Player player) {
            if (blockEntity instanceof Provider32 patternProvider) {
                patternProvider.openMenu(player, MenuLocators.forBlockEntity(patternProvider));
            }
        }
    }

    public static class Provider32 extends InfEnhancedBlockEntity {
        public Provider32(BlockPos pos, BlockState blockState) {
            super(InfProviderRegistry.P32_BE.get(), pos, blockState);
        }

        @Override
        protected PatternProviderLogic createLogic() {
            return new InfProviderLogic(this.getMainNode(), this, 32, this);
        }

        @Override
        public void openMenu(Player player, MenuHostLocator locator) {
            MenuOpener.open(InfProviderMenu.Provider32Menu.TYPE, player, locator);
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
            MenuOpener.returnTo(InfProviderMenu.Provider32Menu.TYPE, player, subMenu.getLocator());
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return new ItemStack((ItemLike) InfProviderRegistry.P32_BLOCK.get());
        }

        @Override
        public AEItemKey getTerminalIcon() {
            return AEItemKey.of((ItemLike) InfProviderRegistry.P32_BLOCK.get());
        }

        @Override
        public void onReady() {
            super.onReady();
            this.getMainNode().setVisualRepresentation(
                    new ItemStack((ItemLike) InfProviderRegistry.P32_BLOCK.get()));
        }
    }

    // ==================== 1024 槽 ====================

    public static class Provider1024Block extends InfProviderBlock<Provider1024> {
        public Provider1024Block() {
            super(metalProps());
            this.registerDefaultState(this.defaultBlockState()
                    .setValue(PatternProviderBlock.PUSH_DIRECTION, PushDirection.ALL));
        }

        @Override
        public void neighborChanged(BlockState blockState, net.minecraft.world.level.Level level,
                BlockPos blockPosition, Block block, BlockPos fromPosition, boolean isMoving) {
            Provider1024 blockEntity = this.getBlockEntity(level, blockPosition);
            if (blockEntity != null) {
                blockEntity.getLogic().updateRedstoneState();
            }
        }

        @Override
        public ItemInteractionResult checkPlayerInteractionWhenUseItem(Provider1024 blockEntity, ItemStack itemStack,
                net.minecraft.world.level.Level world, BlockPos blockPosition, BlockHitResult blockHitResult,
                Player player) {
            if (InteractionUtil.canWrenchRotate(itemStack)) {
                this.setSide(world, blockPosition, blockHitResult.getDirection());
                return ItemInteractionResult.sidedSuccess(world.isClientSide());
            }
            return null;
        }

        @Override
        public void openGui(BlockEntity blockEntity, Player player) {
            if (blockEntity instanceof Provider1024 patternProvider) {
                patternProvider.openMenu(player, MenuLocators.forBlockEntity(patternProvider));
            }
        }
    }

    public static class Provider1024 extends InfEnhancedBlockEntity {
        public Provider1024(BlockPos pos, BlockState blockState) {
            super(InfProviderRegistry.P1024_BE.get(), pos, blockState);
        }

        @Override
        protected PatternProviderLogic createLogic() {
            return new InfProviderLogic(this.getMainNode(), this, 1024, this);
        }

        @Override
        public void openMenu(Player player, MenuHostLocator locator) {
            MenuOpener.open(InfProviderMenu.Provider1024Menu.TYPE, player, locator);
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
            MenuOpener.returnTo(InfProviderMenu.Provider1024Menu.TYPE, player, subMenu.getLocator());
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return new ItemStack((ItemLike) InfProviderRegistry.P1024_BLOCK.get());
        }

        @Override
        public AEItemKey getTerminalIcon() {
            return AEItemKey.of((ItemLike) InfProviderRegistry.P1024_BLOCK.get());
        }

        @Override
        public void onReady() {
            super.onReady();
            this.getMainNode().setVisualRepresentation(
                    new ItemStack((ItemLike) InfProviderRegistry.P1024_BLOCK.get()));
        }
    }

    // ==================== 无限（32723 槽）====================

    public static class ProviderInfBlock extends InfProviderBlock<ProviderInf> {
        public ProviderInfBlock() {
            super(metalProps());
            this.registerDefaultState(this.defaultBlockState()
                    .setValue(PatternProviderBlock.PUSH_DIRECTION, PushDirection.ALL));
        }

        @Override
        public void neighborChanged(BlockState blockState, net.minecraft.world.level.Level level,
                BlockPos blockPosition, Block block, BlockPos fromPosition, boolean isMoving) {
            ProviderInf blockEntity = this.getBlockEntity(level, blockPosition);
            if (blockEntity != null) {
                blockEntity.getLogic().updateRedstoneState();
            }
        }

        @Override
        public ItemInteractionResult checkPlayerInteractionWhenUseItem(ProviderInf blockEntity, ItemStack itemStack,
                net.minecraft.world.level.Level world, BlockPos blockPosition, BlockHitResult blockHitResult,
                Player player) {
            if (InteractionUtil.canWrenchRotate(itemStack)) {
                this.setSide(world, blockPosition, blockHitResult.getDirection());
                return ItemInteractionResult.sidedSuccess(world.isClientSide());
            }
            return null;
        }

        @Override
        public void openGui(BlockEntity blockEntity, Player player) {
            if (blockEntity instanceof ProviderInf patternProvider) {
                patternProvider.openMenu(player, MenuLocators.forBlockEntity(patternProvider));
            }
        }
    }

    public static class ProviderInf extends InfEnhancedBlockEntity {
        public ProviderInf(BlockPos pos, BlockState blockState) {
            super(InfProviderRegistry.PINF_BE.get(), pos, blockState);
        }

        @Override
        protected PatternProviderLogic createLogic() {
            return new InfProviderLogic(this.getMainNode(), this, INFINITY_SLOTS, this);
        }

        @Override
        public void openMenu(Player player, MenuHostLocator locator) {
            MenuOpener.open(InfProviderMenu.ProviderInfMenu.TYPE, player, locator);
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
            MenuOpener.returnTo(InfProviderMenu.ProviderInfMenu.TYPE, player, subMenu.getLocator());
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return new ItemStack((ItemLike) InfProviderRegistry.PINF_BLOCK.get());
        }

        @Override
        public AEItemKey getTerminalIcon() {
            return AEItemKey.of((ItemLike) InfProviderRegistry.PINF_BLOCK.get());
        }

        @Override
        public void onReady() {
            super.onReady();
            this.getMainNode().setVisualRepresentation(
                    new ItemStack((ItemLike) InfProviderRegistry.PINF_BLOCK.get()));
        }
    }
}
