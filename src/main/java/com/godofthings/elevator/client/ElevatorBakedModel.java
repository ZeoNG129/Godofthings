package com.godofthings.elevator.client;

import com.godofthings.Godofthings;
import com.godofthings.elevator.ElevatorBlockBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.QuadTransformers;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 电梯的 baked model 包装器：把「伪装方块的外观」直接拷进电梯的静态区块几何。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.neoforge.client.render.ElevatorBakedModel}（MIT），
 * 逐行对照编写。</p>
 *
 * <h2>为什么原来那套 BlockEntityRenderer 不行（v7.18.2 → 7.18.3）</h2>
 * v7.18.1 / 7.18.2 用方块实体渲染器画电梯，配套把方块 {@code getRenderShape} 设成 {@code INVISIBLE}。
 * {@code INVISIBLE} 的语义是「方块模型交给方块实体渲染器」，而 BER 只在**实体渲染阶段**（每帧，
 * 随方块实体可见范围）执行；静态区块几何阶段什么都不画。结果是电梯放在世界里长期不可见
 * （只在某些瞬间闪一下），伪装也一并失效。
 * <br>上游根本不重写 {@code getRenderShape}（保持默认 {@code MODEL}），也不用 BER：
 * 它把电梯的 baked model 包一层，用 {@link ModelData} 把伪装状态传进来，
 * 在 {@code getQuads} 里直接返回伪装方块自己的 quad —— 全部走静态区块几何，稳定且无每帧开销。
 *
 * <h2>伪装状态怎么传进来</h2>
 * {@code ElevatorBlockEntity#getModelData()} 把 heldState 塞进 {@link #HELD_STATE}；
 * 该实体的 {@code setChanged()} 再调 {@code requestModelDataUpdate()} 触发区块重烘焙。
 * 所以这里<b>不读方块实体</b>，只从 {@code extraData} 取值（与上游一致）。
 */
public class ElevatorBakedModel extends BakedModelWrapper<BakedModel>
{
    /** 伪装方块的方块状态（null = 未伪装）。 */
    public static final ModelProperty<BlockState> HELD_STATE = new ModelProperty<>();

    public ElevatorBakedModel(BakedModel originalModel)
    {
        super(originalModel);
    }

    @Nonnull
    @Override
    public TextureAtlasSprite getParticleIcon(@Nonnull ModelData data)
    {
        BlockState state = data.get(HELD_STATE);
        if (state != null)
        {
            return Minecraft.getInstance().getBlockRenderer().getBlockModel(state).getParticleIcon(data);
        }

        return super.getParticleIcon(data);
    }

    /**
     * 图层集合：有伪装用伪装方块的图层，没有用本体图层，最后无条件并上 {@code cutoutMipped()}
     * （箭头那一层用它画）。少了这个 union，箭头永远不进区块渲染。
     */
    @Nonnull
    @Override
    public ChunkRenderTypeSet getRenderTypes(@Nonnull BlockState state, @Nonnull RandomSource rand,
                                             @Nonnull ModelData data)
    {
        BlockState heldState = data.get(HELD_STATE);
        ChunkRenderTypeSet types;
        if (heldState != null)
        {
            types = Minecraft.getInstance().getBlockRenderer().getBlockModel(heldState).getRenderTypes(heldState, rand, data);
        }
        else
        {
            types = super.getRenderTypes(state, rand, data);
        }

        return ChunkRenderTypeSet.union(types, ChunkRenderTypeSet.of(RenderType.cutoutMipped()));
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull RandomSource rand,
                                    @Nonnull ModelData extraData, @Nullable RenderType renderType)
    {
        List<BakedQuad> result = new ArrayList<>();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        // 1) 方向箭头：只在 cutoutMipped 层，且「有方向 + 未隐藏」时画，按 FACING 绕方块中心旋转
        if (renderType == RenderType.cutoutMipped())
        {
            if (state != null
                    && state.getValue(ElevatorBlockBase.DIRECTIONAL)
                    && state.getValue(ElevatorBlockBase.SHOW_ARROW))
            {
                BakedModel arrowModel = dispatcher.getBlockModelShaper().getModelManager().getModel(
                        ModelResourceLocation.standalone(
                                ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "arrow")));
                BlockModelRotation rot = BlockModelRotation.by(0, (int) state.getValue(ElevatorBlockBase.FACING).toYRot());
                IQuadTransformer transformer = QuadTransformers.applying(rot.getRotation().blockCenterToCorner());

                result.addAll(transformer.process(arrowModel.getQuads(state, null, rand, extraData, renderType)));
            }
        }

        // 2) 主体：有伪装就画伪装方块，并且不再画本体
        BlockState heldState = extraData.get(HELD_STATE);
        if (heldState != null)
        {
            var types = dispatcher.getBlockModel(heldState).getRenderTypes(heldState, rand, ModelData.EMPTY);

            if (renderType == null || types.contains(renderType))
            {
                BakedModel model = dispatcher.getBlockModel(heldState);
                result.addAll(model.getQuads(heldState, side, rand, extraData, renderType));
            }

            return result;
        }

        // 3) 未伪装：走本体模型（16 色贴图）
        result.addAll(originalModel.getQuads(state, side, rand, extraData, renderType));
        return result;
    }
}
