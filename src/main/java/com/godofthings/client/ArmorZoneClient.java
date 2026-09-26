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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 机械共鸣的木棍选区（客户端）：选角输入 + 选区可视化 + 模式切换 + 功能面板。
 * <p>
 * <b>机制与表现移植自 Zifeng Skill Tree（子枫的百宝箱）的 ZoneSelectionInputHandler
 * / ZoneSkillRenderer / StickToolHudRenderer</b>
 * （Copyright (c) 2026 zifeng, MIT License，见 README「第三方代码与许可」）：
 * <ul>
 *   <li>手持木棍（主手或副手）激活；同时要求穿齐全套 + 已开启至少一个选区技能</li>
 *   <li>左键：第一次记第一角，第二次记第二角 → 成区（潜行 + 左键清除）</li>
 *   <li>选区期间取消原版攻击/挖掘，木棍不会把方块敲掉</li>
 *   <li>双层线框（外层压暗 + 内层亮色，颜色按模式）+ 半透明填充</li>
 *   <li>第二角预览：选完第一角后，视线所指处实时以青框预览</li>
 *   <li>右下角功能面板：彩色圆点 + 当前模式名 + 操作提示 + 选区尺寸</li>
 *   <li>H 切模式时自动跳过未解锁的模式（与参考模组一致）</li>
 * </ul>
 */
@EventBusSubscriber(modid = Godofthings.MODID, value = Dist.CLIENT)
public final class ArmorZoneClient
{
    /** 当前模式（客户端本地状态） */
    private static int mode = ArmorZoneData.MODE_PLACE;
    /** 已选定的第一个角点（null = 还没开始选） */
    private static BlockPos firstCorner;

    private ArmorZoneClient() {}

    public static int getMode()
    {
        return mode;
    }

    /** 该模式对应的技能是否已开启 */
    private static boolean isModeUnlocked(int m)
    {
        return switch (m)
        {
            case ArmorZoneData.MODE_PLACE -> ArmorSkillData.clientEnabled(ArmorSkills.MACHINE_ZONE_PLACE);
            case ArmorZoneData.MODE_EXCAVATE -> ArmorSkillData.clientEnabled(ArmorSkills.MACHINE_ZONE_EXCAVATE);
            case ArmorZoneData.MODE_ATTACK -> ArmorSkillData.clientEnabled(ArmorSkills.MACHINE_ZONE_ATTACK);
            default -> ArmorSkillData.clientEnabled(ArmorSkills.MACHINE_ZONE_PROTECT);
        };
    }

    private static List<Integer> unlockedModes()
    {
        List<Integer> out = new ArrayList<>(4);
        for (int i = 0; i < ArmorZoneData.MODE_COUNT; i++)
        {
            if (isModeUnlocked(i))
            {
                out.add(i);
            }
        }
        return out;
    }

    /** 切换模式：自动跳过未解锁的模式 */
    public static void cycleMode()
    {
        List<Integer> unlocked = unlockedModes();
        Minecraft mc = Minecraft.getInstance();
        if (unlocked.isEmpty())
        {
            return;
        }
        int idx = unlocked.indexOf(mode);
        mode = unlocked.get((idx + 1) % unlocked.size());
        firstCorner = null;
        if (mc.player != null)
        {
            mc.player.displayClientMessage(Component.translatable(
                    "chat.godofthings.armor.zone.mode",
                    Component.translatable("gui.godofthings.armor.zone.mode." + ArmorZoneData.modeSuffix(mode))), true);
        }
    }

    /** 可用选区状态：手持（主手/副手）木棍 + 穿齐全套 + 至少开了一个选区技能 */
    private static boolean canSelect(Minecraft mc)
    {
        if (mc.player == null || mc.level == null)
        {
            return false;
        }
        if (!mc.player.getMainHandItem().is(Items.STICK) && !mc.player.getOffhandItem().is(Items.STICK))
        {
            return false;
        }
        if (!GodArmorHandler.isFullSetWorn(mc.player))
        {
            return false;
        }
        return !unlockedModes().isEmpty();
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

        BlockPos pos = lookedAtBlock(mc);
        if (pos == null)
        {
            return;
        }
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

    private static BlockPos lookedAtBlock(Minecraft mc)
    {
        if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK
                && mc.hitResult instanceof BlockHitResult hit)
        {
            return hit.getBlockPos();
        }
        return null;
    }

    /** 选区可视化：双层线框 + 半透明填充 + 第二角预览 */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (!canSelect(mc))
        {
            return;
        }
        ArmorZoneData.Zone zone = ArmorZoneData.getClient(mode);
        if (zone == null && firstCorner == null)
        {
            return;
        }
        BlockPos preview = firstCorner != null ? lookedAtBlock(mc) : null;
        PoseStack pose = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);

        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        VertexConsumer fill = buffers.getBuffer(RenderType.debugFilledBox());

        if (zone != null)
        {
            int color = ArmorZoneData.modeColor(mode);
            LevelRenderer.renderVoxelShape(pose, fill, shapeOf(zone.min(), zone.max()), 0.0, 0.0, 0.0,
                    ((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                    0.35F, false);
            drawBox(lines, pose, zone.min(), zone.max(), color);
        }
        if (firstCorner != null)
        {
            drawBox(lines, pose, firstCorner, firstCorner, 0xFFFFFFFF);
            if (preview != null)
            {
                drawBox(lines, pose, firstCorner, preview, 0xFF26F2FF);
            }
        }
        pose.popPose();
        buffers.endBatch(RenderType.lines());
        buffers.endBatch(RenderType.debugFilledBox());
    }

    private static VoxelShape shapeOf(BlockPos min, BlockPos max)
    {
        return Shapes.box(min.getX(), min.getY(), min.getZ(),
                max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);
    }

    /** 双层线框：外层压暗且略外扩，内层亮色 */
    private static void drawBox(VertexConsumer lines, PoseStack pose, BlockPos a, BlockPos b, int color)
    {
        double x0 = Math.min(a.getX(), b.getX());
        double y0 = Math.min(a.getY(), b.getY());
        double z0 = Math.min(a.getZ(), b.getZ());
        double x1 = Math.max(a.getX(), b.getX()) + 1.0;
        double y1 = Math.max(a.getY(), b.getY()) + 1.0;
        double z1 = Math.max(a.getZ(), b.getZ()) + 1.0;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float bl = (color & 0xFF) / 255.0F;
        LevelRenderer.renderLineBox(pose, lines, x0 - 0.002, y0 - 0.002, z0 - 0.002,
                x1 + 0.002, y1 + 0.002, z1 + 0.002, r * 0.78F, g * 0.60F, bl * 0.20F, 1.0F);
        LevelRenderer.renderLineBox(pose, lines, x0, y0, z0, x1, y1, z1,
                Math.min(1.0F, r * 1.35F), Math.min(1.0F, g * 1.25F), Math.min(1.0F, bl * 1.20F), 1.0F);
    }

    /** 右下角功能面板（移植参考模组 StickToolHudRenderer） */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || !canSelect(mc))
        {
            return;
        }
        GuiGraphics gg = event.getGuiGraphics();
        String title = "\u25CF " + Component.translatable(
                "gui.godofthings.armor.zone.mode." + ArmorZoneData.modeSuffix(mode)).getString();
        String sub = Component.translatable(
                "gui.godofthings.armor.zone.hint." + ArmorZoneData.modeSuffix(mode)).getString();
        ArmorZoneData.Zone zone = ArmorZoneData.getClient(mode);
        String size = zone == null
                ? Component.translatable("gui.godofthings.armor.zone.no_sel").getString()
                : Component.translatable("gui.godofthings.armor.zone.size", zone.volume(),
                        zone.max().getX() - zone.min().getX() + 1,
                        zone.max().getY() - zone.min().getY() + 1,
                        zone.max().getZ() - zone.min().getZ() + 1).getString();

        int right = mc.getWindow().getGuiScaledWidth() - 4;
        int bottom = mc.getWindow().getGuiScaledHeight() - 35;
        int pad = 4;
        int maxW = Math.max(mc.font.width(title), Math.max(mc.font.width(sub), mc.font.width(size)));
        int totalH = 3 * 10 + pad * 2 - 4;
        int bgX = right - maxW - pad * 2;
        gg.fill(bgX - 1, bottom - totalH - 1, right + 1, bottom + 1, 0xAA000000);
        int y = bottom - totalH + pad - 2;
        gg.drawString(mc.font, title, bgX + pad, y, ArmorZoneData.modeColor(mode), false);
        gg.drawString(mc.font, sub, bgX + pad, y + 10, 0xFFDDDDDD, false);
        gg.drawString(mc.font, size, bgX + pad, y + 20, 0xFFAAAAAA, false);
    }
}