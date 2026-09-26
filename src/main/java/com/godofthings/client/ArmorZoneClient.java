package com.godofthings.client;

import com.godofthings.Godofthings;
import com.godofthings.armor.skill.ArmorSkillData;
import com.godofthings.armor.skill.ArmorSkills;
import com.godofthings.armor.skill.ArmorZoneData;
import com.godofthings.handler.GodArmorHandler;
import com.godofthings.network.ArmorSkillMessages;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * 机械共鸣的木棍选区（客户端）：选角输入 + 选区线框渲染 + 模式切换。
 * <p>
 * <b>机制移植自 Zifeng Skill Tree（子枫的百宝箱）的 {@code ZoneSelectionInputHandler}
 * 与 {@code ZoneSkillRenderer}</b>（Copyright (c) 2026 zifeng, MIT License，
 * 见 README「第三方代码与许可」）。
 *
 * <h3>操作方式（与原版一致）</h3>
 * <ul>
 *   <li><b>手持木棍</b>时激活（同时要求穿齐全套神之护甲、且已开启至少一个选区技能）</li>
 *   <li><b>左键</b>：第一次记下第一角，第二次记下第二角 → 成区并按当前模式上报服务端</li>
 *   <li><b>潜行 + 左键</b>：清除当前模式下已框选的区域</li>
 *   <li>选区期间<b>取消原版攻击/挖掘</b>，避免木棍把方块敲掉</li>
 *   <li>每个模式（放置 / 挖掘 / 攻击 / 防护）各记一个选区，互不覆盖</li>
 * </ul>
 * 线框颜色按模式区分（放置=金黄 / 挖掘=青 / 攻击=红 / 防护=绿），与参考模组一致。
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class ArmorZoneClient
{
    /** 当前模式（客户端本地状态，随每次请求一起发给服务端） */
    private static int mode = ArmorZoneData.MODE_PLACE;
    /** 已选定的第一个角点（null = 还没开始选） */
    private static BlockPos firstCorner;

    private ArmorZoneClient() {}

    public static int getMode()
    {
        return mode;
    }

    /** 切换模式（放置 → 挖掘 → 攻击 → 防护 → 放置…） */
    public static void cycleMode()
    {
        mode = (mode + 1) % ArmorZoneData.MODE_COUNT;
        firstCorner = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null)
        {
            mc.player.displayClientMessage(Component.translatable(
                    "chat.godofthings.armor.zone.mode",
                    Component.translatable("gui.godofthings.armor.zone.mode." + ArmorZoneData.modeSuffix(mode))), true);
        }
    }

    /** 是否处于"可用选区"状态：手持木棍 + 穿齐全套 + 至少开了一个选区技能 */
    private static boolean canSelect(Minecraft mc)
    {
        if (mc.player == null || mc.level == null)
        {
            return false;
        }
        if (!mc.player.getMainHandItem().is(Items.STICK))
        {
            return false;
        }
        if (!GodArmorHandler.isFullSetWorn(mc.player))
        {
            return false;
        }
        return ArmorSkillData.clientEnabled(ArmorSkills.MACHINE_ZONE_PLACE)
                || ArmorSkillData.clientEnabled(ArmorSkills.MACHINE_ZONE_EXCAVATE)
                || ArmorSkillData.clientEnabled(ArmorSkills.MACHINE_ZONE_ATTACK)
                || ArmorSkillData.clientEnabled(ArmorSkills.MACHINE_ZONE_PROTECT);
    }

    /** 左键：选角 / 潜行清除 */
    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event)
    {
        if (!event.isAttack())
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!canSelect(mc))
        {
            return;
        }
        // 选区期间不要让木棍打方块/攻击
        event.setCanceled(true);
        event.setSwingHand(false);

        if (mc.player.isShiftKeyDown())
        {
            firstCorner = null;
            ArmorZoneData.setClient(mode, null);
            ArmorSkillMessages.sendZoneClear(mode);
            mc.player.displayClientMessage(Component.translatable("chat.godofthings.armor.zone.cleared"), true);
            return;
        }

        if (!(mc.hitResult instanceof BlockHitResult hit) || mc.hitResult.getType() != HitResult.Type.BLOCK)
        {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (firstCorner == null)
        {
            firstCorner = pos;
            mc.player.displayClientMessage(Component.translatable("chat.godofthings.armor.zone.corner1",
                    pos.getX(), pos.getY(), pos.getZ()), true);
            return;
        }
        ArmorZoneData.Zone zone = ArmorZoneData.of(firstCorner, pos);
        if (zone.volume() > ArmorZoneData.MAX_VOLUME)
        {
            mc.player.displayClientMessage(Component.translatable("chat.godofthings.armor.zone.too_big",
                    zone.volume(), ArmorZoneData.MAX_VOLUME), true);
            firstCorner = null;
            return;
        }
        ArmorZoneData.setClient(mode, zone);
        ArmorSkillMessages.sendZoneSet(mode, zone);
        firstCorner = null;
        mc.player.displayClientMessage(Component.translatable("chat.godofthings.armor.zone.selected",
                zone.volume()), true);
    }

    /** 选中模式下的选区（含正在选的第一角提示）与待选角点一起画线框 */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !canSelect(mc))
        {
            return;
        }
        ArmorZoneData.Zone zone = ArmorZoneData.getClient(mode);
        if (zone == null && firstCorner == null)
        {
            return;
        }
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        MultiBufferSource.BufferSource buffers =
                Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        if (zone != null)
        {
            int color = ArmorZoneData.modeColor(mode);
            float r = ((color >> 16) & 0xFF) / 255.0F;
            float g = ((color >> 8) & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;
            LevelRenderer.renderLineBox(pose, lines, boxOf(zone), r, g, b, 1.0F);
        }
        if (firstCorner != null)
        {
            // 第一角：白色小框提示
            LevelRenderer.renderLineBox(pose, lines,
                    new AABB(firstCorner).inflate(0.002), 1.0F, 1.0F, 1.0F, 1.0F);
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
    }

    private static AABB boxOf(ArmorZoneData.Zone zone)
    {
        BlockPos min = zone.min();
        BlockPos max = zone.max();
        return new AABB(min.getX(), min.getY(), min.getZ(),
                max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0).inflate(0.002);
    }
}
