package com.godofthings.compass.natures.client;

import com.godofthings.Godofthings;
import com.godofthings.compass.natures.items.NaturesCompassItem;
import com.godofthings.compass.natures.util.CompassState;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 自然罗盘的客户端注册（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>做两件事：</p>
 * <ol>
 *   <li>注册物品模型谓词 {@code godofthings:angle} —— 罗盘指针转向目标群系的动画
 *       （照抄原版指南针 {@code CompassItemPropertyFunction} 的算法：按朝向算角度、
 *       加上平滑抖动 {@code wobble}，再归一化到 [0,1) 给模型 overrides 用）；</li>
 *   <li>把 {@link NaturesCompassOverlay} 注册成 {@code BOSS_OVERLAY} 之上的一层 HUD。</li>
 * </ol>
 *
 * <p>本类有 {@code @SubscribeEvent} 方法，所以 {@code @EventBusSubscriber} 注解是合规的
 * （AGENTS.md：没有订阅方法的类不许挂这个注解）。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NaturesCompassClient
{
    private NaturesCompassClient()
    {
    }

    @SubscribeEvent
    public static void clientInit(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> ItemProperties.register(Godofthings.NATURES_COMPASS.get(),
                ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "angle"),
                new ClampedItemPropertyFunction()
                {
                    private double rotation;
                    private double rota;
                    private long lastUpdateTick;

                    @Override
                    public float unclampedCall(ItemStack stack, ClientLevel world, LivingEntity entityLiving, int seed)
                    {
                        if (entityLiving == null && !stack.isFramed())
                        {
                            return 0.0F;
                        }
                        final boolean entityExists = entityLiving != null;
                        final Entity entity = entityExists ? entityLiving : stack.getFrame();
                        ClientLevel level = world;
                        if (level == null && entity.level() instanceof ClientLevel clientLevel)
                        {
                            level = clientLevel;
                        }

                        double rotation = entityExists ? entity.getYRot() : getFrameRotation((ItemFrame) entity);
                        rotation = rotation % 360.0D;
                        double adjusted = Math.PI
                                - ((rotation - 90.0D) * 0.01745329238474369D - getAngle(level, entity, stack));

                        if (entityExists)
                        {
                            adjusted = wobble(level, adjusted);
                        }

                        final float f = (float) (adjusted / (Math.PI * 2D));
                        return Mth.positiveModulo(f, 1.0F);
                    }

                    /** 指针抖一下再回正（和原版指南针同一套手感） */
                    private double wobble(ClientLevel level, double amount)
                    {
                        if (level.getGameTime() != lastUpdateTick)
                        {
                            lastUpdateTick = level.getGameTime();
                            double d0 = amount - rotation;
                            d0 = Mth.positiveModulo(d0 + Math.PI, Math.PI * 2D) - Math.PI;
                            d0 = Mth.clamp(d0, -1.0D, 1.0D);
                            rota += d0 * 0.1D;
                            rota *= 0.8D;
                            rotation += rota;
                        }
                        return rotation;
                    }

                    private double getFrameRotation(ItemFrame itemFrame)
                    {
                        Direction direction = itemFrame.getDirection();
                        int i = direction.getAxis().isVertical() ? 90 * direction.getAxisDirection().getStep() : 0;
                        return Mth.wrapDegrees(180 + direction.get2DDataValue() * 90 + itemFrame.getRotation() * 45 + i);
                    }

                    /** 玩家（或展示框）到目标群系中心的方位角；没有目标就用世界出生点 */
                    private double getAngle(ClientLevel level, Entity entity, ItemStack stack)
                    {
                        if (stack.getItem() == Godofthings.NATURES_COMPASS.get())
                        {
                            NaturesCompassItem compassItem = (NaturesCompassItem) stack.getItem();
                            BlockPos pos;
                            if (compassItem.getState(stack) == CompassState.FOUND)
                            {
                                pos = new BlockPos(compassItem.getFoundBiomeX(stack), 0, compassItem.getFoundBiomeZ(stack));
                            }
                            else
                            {
                                pos = level.getSharedSpawnPos();
                            }
                            return Math.atan2((double) pos.getZ() - entity.position().z(),
                                    (double) pos.getX() - entity.position().x());
                        }
                        return 0.0D;
                    }
                }));
    }

    @SubscribeEvent
    public static void registerOverlay(RegisterGuiLayersEvent event)
    {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY,
                ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "natures_compass"),
                new NaturesCompassOverlay());
    }
}
