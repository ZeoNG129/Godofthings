package com.godofthings.elevator.client;

import com.godofthings.elevator.ElevatorBlockBase;
import com.godofthings.elevator.block.ElevatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * 电梯的方块实体渲染器：画「伪装方块」与「朝向箭头」。
 *
 * <p>移植自 ElevatorMod 的伪装渲染（{@code ElevatorBakedModel} + 颜色处理器，MIT）。</p>
 *
 * <h2>[本地改动] 为什么改用 BlockEntityRenderer 而不是包装 BakedModel</h2>
 * 上游把逻辑写进 {@code BakedModel} 包装器，靠 {@code BlockEntity#getModelData()} 把伪装状态
 * 塞进模型数据。本项目的 NeoForge 21.1.249 <b>没有</b> {@code ModelDataProvider} 接口，
 * 且 {@code BakedModel#getQuads} 也拿不到方块坐标去反查方块实体。
 * <br>改为 {@link BlockEntityRenderer}：它能直接拿到 {@link ElevatorBlockEntity}，天然可取伪装状态，
 * 且不依赖任何高版本 API。配套把电梯方块的 {@code getRenderShape} 设为 {@code INVISIBLE}。
 *
 * <h2>两个踩过的坑（勿改回去）</h2>
 * <ol>
 *   <li>画方块必须用<b>五参</b> {@code renderSingleBlock(...)}。六参版本最后一个参数是
 *       {@link RenderType}，传 {@code null} 等于「不画任何图层」——方块会完全透明
 *       （v7.18.1 实机现象）。</li>
 *   <li>箭头是独立模型（{@code godofthings:arrow}），必须在 {@code ModelEvent.RegisterAdditional}
 *       里注册过才取得到，否则拿到的是缺失模型。</li>
 * </ol>
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

        // 1) 主体：有伪装画伪装方块，没伪装画电梯本体（五参重载，自动选图层）
        BlockState toDraw = held != null ? held : ownState;
        dispatcher.renderSingleBlock(toDraw, pose, buffers, packedLight, packedOverlay);

        // 2) 箭头：只有「有方向 + 未隐藏」才画，且绕方块中心旋转到朝向
        if (ownState.getValue(ElevatorBlockBase.DIRECTIONAL) && ownState.getValue(ElevatorBlockBase.SHOW_ARROW))
        {
            Direction facing = ownState.getValue(ElevatorBlockBase.FACING);
            pose.pushPose();
            pose.translate(0.5D, 0D, 0.5D);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-facing.toYRot()));
            pose.translate(-0.5D, 0D, -0.5D);
            dispatcher.getModelRenderer().renderModel(
                    pose.last(),
                    buffers.getBuffer(RenderType.cutoutMipped()),
                    ownState,
                    Minecraft.getInstance().getModelManager().getModel(
                            net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                            com.godofthings.Godofthings.MODID, "arrow"))),
                    1.0F, 1.0F, 1.0F, packedLight, packedOverlay,
                    ModelData.EMPTY, RenderType.cutoutMipped());
            pose.popPose();
        }
    }

    @Override
    public int getViewDistance()
    {
        return 64;
    }
}
