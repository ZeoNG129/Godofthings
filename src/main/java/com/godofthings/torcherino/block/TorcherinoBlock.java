package com.godofthings.torcherino.block;

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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 加速火把（落地火把）：参考 Torcherino Forge 版 ForgeTorcherinoBlock。
 * 站在方块上的火把，右键打开调节界面。
 */
@SuppressWarnings("deprecation")
public class TorcherinoBlock extends Block implements EntityBlock, TierSupplier
{
    protected static final VoxelShape AABB = Block.box(6.0, 0.0, 6.0, 10.0, 10.0, 10.0);
    private final ResourceLocation tierID;

    public TorcherinoBlock(Properties properties, ResourceLocation tier)
    {
        super(properties);
        tierID = tier;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return AABB;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos facingPos)
    {
        return facing == Direction.DOWN && !this.canSurvive(state, level, currentPos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        return canSupportCenter(level, pos.below(), Direction.UP);
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
        this.neighborChanged(null, level, pos, null, null, false);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        return TorcherinoLogic.onUse(state, level, pos, player, hand, hit);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean flag)
    {
        TorcherinoLogic.neighborUpdate(state, level, pos, neighborBlock, neighborPos, flag, (be) ->
                be.setPoweredByRedstone(level.hasSignal(pos.below(), Direction.UP)));
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
    {
        TorcherinoLogic.onPlaced(level, pos, state, placer, stack, this);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random)
    {
        double x = (double) pos.getX() + 0.5;
        double y = (double) pos.getY() + 0.7;
        double z = (double) pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        ResourceLocation blockName = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (blockName != null && blockName.equals(ResourceLocation.tryBuild(Torcherino.MOD_ID, "torcherino")))
        {
            level.addParticle(Torcherino.PARTICLE_FLAME.get(), x, y, z, 0.0, 0.0, 0.0);
        }
        else if (blockName != null && blockName.equals(ResourceLocation.tryBuild(Torcherino.MOD_ID, "compressed_torcherino")))
        {
            level.addParticle(Torcherino.PARTICLE_COMPRESSED_FLAME.get(), x, y, z, 0.0, 0.0, 0.0);
        }
        else if (blockName != null && blockName.equals(ResourceLocation.tryBuild(Torcherino.MOD_ID, "double_compressed_torcherino")))
        {
            level.addParticle(Torcherino.PARTICLE_DOUBLE_COMPRESSED_FLAME.get(), x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
