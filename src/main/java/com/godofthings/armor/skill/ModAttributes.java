package com.godofthings.armor.skill;

import com.godofthings.Godofthings;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 神之套装技能树用到的自定义属性。
 * <p>
 * <b>移植自 Zifeng Skill Tree（子枫的百宝箱）的 {@code ModAttributes}</b>
 * （Copyright (c) 2026 zifeng, MIT License，见 README「第三方代码与许可」）。
 *
 * <h3>为什么需要「物理减伤」这个自定义属性</h3>
 * 原版护甲减伤公式（{@code CombatRules}）本身封顶在 80% 左右，护甲值再高也没用。
 * 因此把「金身真解」投入的等级转化为本属性（0~1，100% 封顶），
 * 由伤害事件按比例<b>独立乘算</b>减免物理伤害——不改动任何原版代码，与其他模组零冲突。
 */
public final class ModAttributes
{
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, Godofthings.MODID);

    /** 物理减伤（0~1）：金身真解每级 +1%，上限 80 级 = 80% */
    public static final DeferredHolder<Attribute, Attribute> DAMAGE_REDUCTION =
            ATTRIBUTES.register("damage_reduction",
                    () -> new RangedAttribute("attribute." + Godofthings.MODID + ".damage_reduction",
                            0.0, 0.0, 1.0).setSyncable(true));

    private ModAttributes() {}

    /** 把自定义属性挂到玩家身上（否则玩家没有该属性实例，修饰符无处可挂）。 */
    @EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration
    {
        private Registration() {}

        @SubscribeEvent
        public static void onEntityAttributeModification(EntityAttributeModificationEvent event)
        {
            if (!event.has(EntityType.PLAYER, DAMAGE_REDUCTION))
            {
                event.add(EntityType.PLAYER, DAMAGE_REDUCTION);
            }
        }
    }
}
