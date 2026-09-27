/*
 * 照抄自 AE2 Lightning Tech Reborn（作者 MOAKIEE、CystrySU、gjmhmm8、_leng、TedXenon、MHanHanBing）
 * 的 fumo 玩偶系统，仅改动包名与命名空间，并裁掉本模组未移植的「超维猪咪」分支。
 *
 * 许可：本文件源码沿用上游的 GNU LGPL 3.0 —— 见仓库 LICENSES/AE2LT-LGPL-3.0.txt。
 * 相关素材（models/block/hoyoog_fumo.json 那份玩家模型）沿用上游的 CC BY-NC-SA 3.0 ——
 * 见 LICENSES/AE2LT-ASSETS-CC-BY-NC-SA-3.0.md（署名 / 禁止商用 / 相同方式共享）。
 * 本模组的贴图 textures/block/hoyoog_fumo.png 不是上游素材，是作者自己的皮肤。
 */
package com.godofthings.fumo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

import com.godofthings.fumo.block.FumoBlock;
import com.godofthings.fumo.blockentity.FumoBlockEntity;
import com.godofthings.fumo.registry.ModFumos;

public class FumoBlockRenderer implements BlockEntityRenderer<FumoBlockEntity> {

    private static final RandomSource RAND = RandomSource.create();

    public FumoBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FumoBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        boolean spinning = blockEntity.isSpinning();

        BlockState renderState = spinning && state.hasProperty(FumoBlock.FACING)
                ? state.setValue(FumoBlock.FACING, Direction.NORTH)
                : state;

        poseStack.pushPose();
        if (spinning) {
            poseStack.translate(0.5D, 0.0D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(blockEntity.getRenderYRot(partialTick)));
            poseStack.translate(-0.5D, 0.0D, -0.5D);
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = dispatcher.getBlockModel(renderState);
        ModelData modelData = ModelData.EMPTY;
        Level level = blockEntity.getLevel();
        BlockPos pos = blockEntity.getBlockPos();

        // 上游此处还有「超维猪咪」的分支：走 HyperdimensionalPigmeePortalLayer /
        // HyperdimensionalPigmeeTextureLayer 两层自定义渲染 + 物品侧 BlockEntityWithoutLevelRenderer。
        // 那套只服务于超维猪咪那一个收藏品，本模组只移植一个 HoYooG 玩偶，故裁掉；
        // 其余渲染路径与上游逐字一致（世界内走 tesselateBlock，无 level 时走手写 quad 回退）。

        if (level != null) {
            ModelBlockRenderer modelRenderer = dispatcher.getModelRenderer();
            for (RenderType renderType : model.getRenderTypes(renderState, RAND, modelData)) {
                modelRenderer.tesselateBlock(
                        level,
                        model,
                        renderState,
                        pos,
                        poseStack,
                        buffer.getBuffer(renderType),
                        false,
                        RAND,
                        42L,
                        packedOverlay,
                        modelData,
                        renderType);
            }
        } else {
            int color = Minecraft.getInstance().getBlockColors().getColor(renderState, null, null, 0);
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            PoseStack.Pose pose = poseStack.last();

            for (RenderType renderType : model.getRenderTypes(renderState, RAND, modelData)) {
                VertexConsumer consumer = buffer.getBuffer(renderType);
                for (Direction dir : Direction.values()) {
                    RAND.setSeed(42L);
                    renderQuads(pose, consumer, model.getQuads(renderState, dir, RAND, modelData, renderType),
                            r, g, b, packedLight, packedOverlay);
                }
                RAND.setSeed(42L);
                renderQuads(pose, consumer, model.getQuads(renderState, null, RAND, modelData, renderType),
                        r, g, b, packedLight, packedOverlay);
            }
        }

        poseStack.popPose();
    }

    private static void renderQuads(PoseStack.Pose pose, VertexConsumer consumer, List<BakedQuad> quads,
                                    float r, float g, float b, int packedLight, int packedOverlay) {
        for (BakedQuad quad : quads) {
            float shade = getShade(quad);
            float qr;
            float qg;
            float qb;
            if (quad.isTinted()) {
                qr = Mth.clamp(r, 0.0F, 1.0F) * shade;
                qg = Mth.clamp(g, 0.0F, 1.0F) * shade;
                qb = Mth.clamp(b, 0.0F, 1.0F) * shade;
            } else {
                qr = shade;
                qg = shade;
                qb = shade;
            }
            consumer.putBulkData(pose, quad, qr, qg, qb, 1.0F, packedLight, packedOverlay);
        }
    }

    private static float getShade(BakedQuad quad) {
        if (!quad.isShade()) {
            return 1.0F;
        }
        Direction dir = quad.getDirection();
        if (dir == null) {
            return 1.0F;
        }
        return switch (dir) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case EAST, WEST -> 0.6F;
        };
    }
}
