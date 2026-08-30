package com.godofthings.torcherino.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.godofthings.torcherino.Torcherino;
import com.godofthings.torcherino.api.TierSupplier;
import com.godofthings.torcherino.block.entity.TorcherinoBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 加速火把（墙挂火把）：参考 Torcherino Forge 版 ForgeWallTorcherinoBlock。
 */
@SuppressWarnings("deprecation")
public final class WallTorcherinoBlock extends TorcherinoBlock implements EntityBlock, TierSupplier
{
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final Map<Direction, VoxelShape> AABBS;
    private final ResourceLocation tierID;

    public WallTorcherinoBlock(Properties properties, ResourceLocation tier)
    {
        super(properties, tier);
        this.tierID = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public String getDescriptionId()
    {
        return this.asItem().getDescriptionId();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return getShape(state);
    }

    public static VoxelShape getShape(BlockState state)
    {
        return AABBS.get(state.getValue(FACING));
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        Direction facing = state.getValue(FACING);
        BlockPos attachPos = pos.relative(facing.getOpposite());
        BlockState attachState = level.getBlockState(attachPos);
        return attachState.isFaceSturdy(level, attachPos, facing);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        BlockState state = this.defaultBlockState();
        LevelReader level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        for (Direction direction : context.getNearestLookingDirections())
        {
            if (direction.getAxis().isHorizontal())
            {
                Direction facing = direction.getOpposite();
                state = state.setValue(FACING, facing);
                if (state.canSurvive(level, pos))
                {
                    return state;
                }
            }
        }
        return null;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos facingPos)
    {
        return facing.getOpposite() == state.getValue(FACING) && !state.canSurvive(level, currentPos)
                ? Blocks.AIR.defaultBlockState()
                : state;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING);
    }

    @Override
    public ResourceLocation getTier()
    {
        return tierID;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new TorcherinoBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        return TorcherinoLogic.getTicker(level, state, type);
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state)
    {
        return PushReaction.IGNORE;
    }

    @Override
    public void onPlace(BlockState newState, Level level, BlockPos pos, BlockState state, boolean flag)
    {
        this.neighborChanged(newState, level, pos, null, null, false);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        return TorcherinoLogic.onUse(state, level, pos, player, hand, hit);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean flag)
    {
        TorcherinoLogic.neighborUpdate(state, level, pos, neighborBlock, neighborPos, flag, (be) -> be.setPoweredByRedstone(
                level.hasSignal(pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()),
                        state.getValue(BlockStateProperties.HORIZONTAL_FACING))));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
        TorcherinoLogic.onPlaced(level, pos, state, placer, stack, this);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random)
    {
        Direction facing = state.getValue(FACING);
        double x = (double) pos.getX() + 0.5;
        double y = (double) pos.getY() + 0.7;
        double z = (double) pos.getZ() + 0.5;
        Direction opposite = facing.getOpposite();
        level.addParticle(ParticleTypes.SMOKE, x + 0.27 * (double) opposite.getStepX(), y + 0.22,
                z + 0.27 * (double) opposite.getStepZ(), 0.0, 0.0, 0.0);
        ResourceLocation blockName = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockName != null && blockName.equals(ResourceLocation.tryBuild(Torcherino.MOD_ID, "wall_torcherino")))
        {
            level.addParticle(Torcherino.PARTICLE_FLAME.get(), x + 0.27 * (double) opposite.getStepX(), y + 0.22,
                    z + 0.27 * (double) opposite.getStepZ(), 0.0, 0.0, 0.0);
        }
        else if (blockName != null && blockName.equals(ResourceLocation.tryBuild(Torcherino.MOD_ID, "wall_compressed_torcherino")))
        {
            level.addParticle(Torcherino.PARTICLE_COMPRESSED_FLAME.get(), x + 0.27 * (double) opposite.getStepX(), y + 0.22,
                    z + 0.27 * (double) opposite.getStepZ(), 0.0, 0.0, 0.0);
        }
        else if (blockName != null && blockName.equals(ResourceLocation.tryBuild(Torcherino.MOD_ID, "wall_double_compressed_torcherino")))
        {
            level.addParticle(Torcherino.PARTICLE_DOUBLE_COMPRESSED_FLAME.get(), x + 0.27 * (double) opposite.getStepX(), y + 0.22,
                    z + 0.27 * (double) opposite.getStepZ(), 0.0, 0.0, 0.0);
        }
    }

    static
    {
        AABBS = Maps.newEnumMap(ImmutableMap.of(
                Direction.NORTH, Block.box(5.5, 3.0, 11.0, 10.5, 13.0, 16.0),
                Direction.SOUTH, Block.box(5.5, 3.0, 0.0, 10.5, 13.0, 5.0),
                Direction.WEST, Block.box(11.0, 3.0, 5.5, 16.0, 13.0, 10.5),
                Direction.EAST, Block.box(0.0, 3.0, 5.5, 5.0, 13.0, 10.5)));
    }
}
