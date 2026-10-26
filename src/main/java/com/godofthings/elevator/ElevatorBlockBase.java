package com.godofthings.elevator;

import com.godofthings.elevator.block.ElevatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/**
 * 开放式电梯的方块基类（16 色共用）。
 *
 * <p>移植自 ElevatorMod（作者 VsnGamer / 原 OpenBlocks Team，MIT）的
 * {@code com.vsngarcia.ElevatorBlockBase}，<b>逐行对照编写</b>。</p>
 *
 * <h2>[本地改动] 与上游的差异（全是开发环境 API 代差导致，不是设计改动）</h2>
 * 上游面向的 NeoForge 比本项目（21.1.249）更新，以下重写点在本版本<b>不存在</b>，
 * 因此未移植（伪装方块与这些属性「跟随」相关，属次要体验）：
 * <ul>
 *   <li>{@code getFriction(state, level, pos, entity)} —— 本版只有无参 {@code getFriction()}</li>
 *   <li>{@code getSoundType(state, level, pos, entity)} —— 本版只有 {@code getSoundType(state)}</li>
 *   <li>{@code getLightEmission(state, level, pos)} / {@code hasDynamicLightEmission} —— 本版无</li>
 *   <li>{@code getAppearance} / {@code hidesNeighborFace} / {@code supportsExternalFaceHiding} —— 本版无</li>
 *   <li>{@code shouldCheckWeakPower} / {@code canConnectRedstone} —— 本版无</li>
 *   <li>{@code collisionExtendsVertically} —— 本版无</li>
 * </ul>
 * <p>保留的核心体验：16 色、伪装成任意方块（外观 + 碰撞箱 + 轮廓 + 亮度遮挡）、
 * 跳跃/潜行传送、方向与箭头设置界面。</p>
 */
public abstract class ElevatorBlockBase extends HorizontalDirectionalBlock implements EntityBlock
{
    public static final BooleanProperty DIRECTIONAL = BooleanProperty.create("directional");
    public static final BooleanProperty SHOW_ARROW = BooleanProperty.create("show_arrow");

    private final DyeColor dyeColor;

    public ElevatorBlockBase(DyeColor color)
    {
        super(Block.Properties.of()
                .mapColor(color)
                .sound(SoundType.WOOL)
                .strength(0.8F)
                .dynamicShape()
                .noOcclusion()
                .isValidSpawn(ElevatorBlockBase::isValidSpawn));

        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(DIRECTIONAL, false)
                .setValue(SHOW_ARROW, true));

        this.dyeColor = color;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, DIRECTIONAL, SHOW_ARROW);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(DIRECTIONAL, false)
                .setValue(SHOW_ARROW, true);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext)
    {
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new ElevatorBlockEntity(pos, state);
    }

    /** 打开设置界面（各个平台实现不同，NeoForge 直接 player.openMenu）。 */
    protected abstract void openMenu(Player player, ElevatorBlockEntity tile, BlockPos pos);

    @Override
    public ItemInteractionResult useItemOn(ItemStack itemStack, BlockState state, Level worldIn, BlockPos pos,
                                           Player player, InteractionHand handIn, BlockHitResult hit)
    {
        if (worldIn.isClientSide)
        {
            return ItemInteractionResult.SUCCESS;
        }

        return getElevatorTile(worldIn, pos)
                .map(tile ->
                {
                    Block handBlock = Block.byItem(player.getItemInHand(handIn).getItem());
                    BlockState stateToApply = handBlock.getStateForPlacement(new com.godofthings.elevator.FakeUseContext(player, handIn, hit));
                    if (stateToApply != null && tile.setCamoAndUpdate(stateToApply))
                    {
                        return ItemInteractionResult.SUCCESS; // 成功伪装时不打开菜单
                    }

                    // 潜行右键 = 取消伪装
                    if (player.isCrouching() && tile.getHeldState() != null)
                    {
                        tile.setCamoAndUpdate(null);
                        return ItemInteractionResult.SUCCESS;
                    }

                    openMenu(player, tile, pos);
                    return ItemInteractionResult.SUCCESS;
                })
                .orElse(ItemInteractionResult.FAIL);
    }

    public static boolean isValidSpawn(BlockState state, BlockGetter world, BlockPos pos, EntityType<?> entityType)
    {
        if (!ElevatorConfig.mobSpawn())
        {
            return false;
        }
        if (!(world.getBlockEntity(pos) instanceof ElevatorBlockEntity tile))
        {
            return false;
        }
        BlockState held = tile.getHeldState();
        return held != null
                ? held.isValidSpawn(world, pos, entityType)
                : state.isFaceSturdy(world, pos, Direction.UP);
    }

    // ---- 碰撞 / 轮廓跟随伪装方块 ----

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
    {
        return getHeldState(worldIn, pos)
                .map(s -> s.getCollisionShape(worldIn, pos, context))
                .orElse(super.getCollisionShape(state, worldIn, pos, context));
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter world, BlockPos pos)
    {
        return getHeldState(world, pos)
                .map(s -> s.isCollisionShapeFullBlock(world, pos))
                .orElse(super.isCollisionShapeFullBlock(state, world, pos));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context)
    {
        return getHeldState(worldIn, pos)
                .map(s -> s.getShape(worldIn, pos, context))
                .orElse(super.getShape(state, worldIn, pos, context));
    }

    @Override
    public float getSpeedFactor()
    {
        return super.getSpeedFactor();
    }

    @Override
    public float getJumpFactor()
    {
        return super.getJumpFactor();
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState,
                                  LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos)
    {
        getElevatorTile(worldIn, currentPos).ifPresent(t ->
        {
            if (t.getHeldState() == null)
            {
                return;
            }
            BlockState updatedState = t.getHeldState().updateShape(facing, facingState, worldIn, currentPos, facingPos);
            if (!updatedState.equals(t.getHeldState()))
            {
                t.setHeldState(updatedState);
            }
        });

        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    // ---- 红石 ----

    @Override
    public boolean isSignalSource(BlockState state)
    {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter reader, BlockPos pos, Direction direction)
    {
        return getHeldState(reader, pos)
                .map(s -> s.getSignal(reader, pos, direction))
                .orElse(super.getSignal(state, reader, pos, direction));
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter reader, BlockPos pos, Direction direction)
    {
        return getHeldState(reader, pos)
                .map(s -> s.getDirectSignal(reader, pos, direction))
                .orElse(super.getDirectSignal(state, reader, pos, direction));
    }

    // ---- 光照 ----

    @Override
    public boolean useShapeForLightOcclusion(BlockState state)
    {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos)
    {
        return true;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos)
    {
        return getHeldState(worldIn, pos)
                .map(s -> s.getLightBlock(worldIn, pos))
                .orElse(worldIn.getMaxLightLevel());
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter world, BlockPos pos)
    {
        return getHeldState(world, pos)
                .map(s -> s.getOcclusionShape(world, pos))
                .orElse(super.getOcclusionShape(state, world, pos));
    }

    public DyeColor getColor()
    {
        return dyeColor;
    }

    protected Optional<ElevatorBlockEntity> getElevatorTile(BlockGetter world, BlockPos pos)
    {
        if (world == null || pos == null)
        {
            return Optional.empty();
        }
        return world.getBlockEntity(pos) instanceof ElevatorBlockEntity tile ? Optional.of(tile) : Optional.empty();
    }

    protected Optional<BlockState> getHeldState(BlockGetter world, BlockPos pos)
    {
        return getElevatorTile(world, pos).map(ElevatorBlockEntity::getHeldState);
    }
}
