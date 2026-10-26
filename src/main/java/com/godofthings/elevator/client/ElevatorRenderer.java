package com.godofthings.elevator.client;

import com.godofthings.Godofthings;
import com.godofthings.elevator.ElevatorBlockBase;
import com.godofthings.elevator.block.ElevatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 电梯的方块实体渲染器：画出「伪装方块」与「朝向箭头」。
 *
 * <p>移植自 ElevatorMod 的伪装渲染（{@code ElevatorBakedModel} + 颜色处理器，MIT）。</p>
 *
 * <h2>[本地改动] 为什么改用 BlockEntityRenderer 而不是包装 BakedModel</h2>
 * 上游把逻辑写进 {@code BakedModel} 包装器，靠 {@code BlockEntity#getModelData()} 把伪装状态
 * 塞进模型数据。本项目的 NeoForge 21.1.249 <b>没有</b> {@code ModelDataProvider} 接口，
 * 而 {@code BakedModel#getQuads} 也拿不到方块坐标去反查方块实体。
 * <br>改为 {@link BlockEntityRenderer}：它能直接拿到 {@link ElevatorBlockEntity}，天然可取伪装状态，
 * 且不依赖任何高版本 API。配套把电梯方块的 {@code getRenderShape} 设为
 * {@code INVISIBLE}（见 {@link ElevatorBlockBase#getRenderShape}）。
 *
 * <p>未伪装时由 BER 画电梯本体模型（16 色贴图），外观与原来一致。</p>
 */
public class ElevatorRenderer implements BlockEntityRenderer<ElevatorBlockEntity>
{
    public ElevatorRenderer(BlockEntityRendererProvider.Context context)
    {
    }

    @Override
    public void render(ElevatorBlockEntity tile, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay)
    {
        if (tile.getLevel() == null)
        {
            return;
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockState ownState = tile.getBlockState();
        BlockState held = tile.getHeldState();

        // 1) 主体：有伪装就画伪装方块，没伪装就画电梯本体
        BlockState toDraw = held != null ? held : ownState;
        dispatcher.renderSingleBlock(toDraw, pose, buffers, packedLight, packedOverlay,
                ModelData.EMPTY, null);

        // 2) 箭头：有方向且未隐藏时叠加（旋转到朝向）
        if (ownState.getValue(ElevatorBlockBase.DIRECTIONAL) && ownState.getValue(ElevatorBlockBase.SHOW_ARROW))
        {
            BakedModel arrow = Minecraft.getInstance().getModelManager().getModel(
                    ModelResourceLocation.standalone(
                            ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "arrow")));
            if (arrow != null)
            {
                Direction facing = ownState.getValue(ElevatorBlockBase.FACING);
                pose.pushPose();
                pose.translate(0.5D, 0D, 0.5D);
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facing.toYRot()));
                pose.translate(-0.5D, 0D, -0.5D);
                dispatcher.getModelRenderer().renderModel(
                        pose.last(), buffers.getBuffer(net.minecraft.client.renderer.RenderType.cutoutMipped()),
                        ownState, arrow, 1.0F, 1.0F, 1.0F, packedLight, packedOverlay,
                        ModelData.EMPTY, net.minecraft.client.renderer.RenderType.cutoutMipped());
                pose.popPose();
            }
        }
    }

    @Override
    public int getViewDistance()
    {
        return 64;
    }
}
