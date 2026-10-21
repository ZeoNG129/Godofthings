package com.godofthings.construction.client;

import com.godofthings.Godofthings;
import com.godofthings.construction.ConstructionActions;
import com.godofthings.construction.GodConstructionItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * 神之建筑的建造预览线框（纯客户端）。
 *
 * <p>手持本物品、预览开关打开、处于<b>建造模式</b>、准星对着方块时，用
 * {@link ConstructionActions#collectBuild}（和真正建造<b>完全同一套 BFS</b>）算出将要铺的坐标，
 * 每格画一个白色线框。这样预览与实际结果不会跑偏。</p>
 *
 * <p>位置上限 {@value #MAX_PREVIEW_BOXES} 格：全画出来要上万条线，明显掉帧；只画前
 * {@value #MAX_PREVIEW_BOXES} 格足够提示「要铺多大一片」，所以 BFS 的 {@code max} 也直接传这个数
 * （天神建筑的上限是 int 上限，不设这个截断会算到天荒地老）。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class ConstructionPreviewRenderer
{
    /** 预览最多画多少格 */
    private static final int MAX_PREVIEW_BOXES = 256;
    /** 线框往里缩一点，免得和方块自身的描边完全重合而闪烁 */
    private static final double SHRINK = 0.005;
    /** 白色线框的不透明度 */
    private static final float LINE_ALPHA = 0.6F;

    private ConstructionPreviewRenderer()
    {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event)
    {
        // 半透明方块之后画：和神之测量的框同一个阶段（线框不会被半透明方块盖住）
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null)
        {
            return;
        }

        // 只在「主手拿着本族物品（神之建筑 / 天神建筑）+ 预览开着 + 建造模式」时画（判断条件与改造前一致，只是多认天神建筑）
        ItemStack wand = player.getMainHandItem();
        if (!GodConstructionItem.isWand(wand))
        {
            return;
        }
        if (!GodConstructionItem.isPreviewEnabled(wand) || GodConstructionItem.getMode(wand) != GodConstructionItem.MODE_BUILD)
        {
            return;
        }

        // 准星必须打在方块上（点击面要一并取出来，和右键时的 getClickedFace() 是同一个值）
        HitResult hit = minecraft.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK)
        {
            return;
        }
        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos clickedPos = blockHit.getBlockPos().immutable();

        // 与建造完全同一套算法（不落方块、不扣材料），只取前 MAX_PREVIEW_BOXES 个
        List<BlockPos> positions = ConstructionActions.collectBuild(
                minecraft.level, player, wand, clickedPos, blockHit.getDirection(), MAX_PREVIEW_BOXES);
        if (positions.isEmpty())
        {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer builder = buffers.getBuffer(RenderType.LINES);

        poseStack.pushPose();
        // 世界原点渲染：平移掉相机位置（照神之测量 MeasureBox 的写法）
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        int count = Math.min(positions.size(), MAX_PREVIEW_BOXES);
        for (int i = 0; i < count; i++)
        {
            AABB box = new AABB(positions.get(i)).inflate(-SHRINK);
            LevelRenderer.renderLineBox(poseStack, builder, box, 1.0F, 1.0F, 1.0F, LINE_ALPHA);
        }
        buffers.endBatch(RenderType.LINES);
        poseStack.popPose();
    }
}
