package com.godofthings.measurement;

// 基于 Measurements（作者 Mrbysco，MIT License）移植并改名「神之测量」：
// https://github.com/Mrbysco/measurements
// 客户端事件接线（原作的 ClientHandler + ClientClass 合并）：
//  - 每 tick：未手持卷尺就清空测量框；未完成的框跟随准星实时更新终点
//  - 世界渲染后：画所有测量框（半透明方块之后）
//  - 进出存档 / 服务器：清空测量框

import com.godofthings.Godofthings;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public class MeasureClientHandler
{
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        Player player = event.getEntity();
        if (!player.level().isClientSide || Minecraft.getInstance().player != player)
        {
            return;
        }

        if (!player.isHolding(Godofthings.GOD_MEASURE.get()))
        {
            MeasureBoxHandler.clear();
            return;
        }

        var boxList = MeasureBoxHandler.getBoxList();
        if (!boxList.isEmpty())
        {
            MeasureBox lastBox = boxList.get(boxList.size() - 1);
            if (!lastBox.isFinished())
            {
                HitResult rayHit = Minecraft.getInstance().hitResult;

                if (rayHit != null && rayHit.getType() == HitResult.Type.BLOCK)
                {
                    BlockHitResult blockHitResult = (BlockHitResult) rayHit;
                    lastBox.setBlockEnd(new BlockPos(blockHitResult.getBlockPos()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            return;
        }

        final Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isHolding(Godofthings.GOD_MEASURE.get()))
        {
            return;
        }

        Matrix4f projectionMatrix = event.getProjectionMatrix();
        PoseStack poseStack = event.getPoseStack();
        RenderBuffers renderBuffers = minecraft.renderBuffers();
        Camera camera = minecraft.gameRenderer.getMainCamera();

        final ResourceKey<Level> currentDimension = player.level().dimension();
        poseStack.pushPose();
        MeasureBoxHandler.getBoxList().forEach(box -> box.render(currentDimension, poseStack, renderBuffers, camera, projectionMatrix));
        poseStack.popPose();
    }

    @SubscribeEvent
    public static void onLogIn(ClientPlayerNetworkEvent.LoggingIn event)
    {
        MeasureBoxHandler.clear();
    }

    @SubscribeEvent
    public static void onLogOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        MeasureBoxHandler.clear();
    }
}
