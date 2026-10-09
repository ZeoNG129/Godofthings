package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.block.entity.GodSlaughterBlockEntity;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 神之砍杀机「保护名单」（v5.17.2）回归测试。
 *
 * <p>砍杀机是「范围内全部秒杀」，误伤代价极高，所以盔甲架 / 村民 / 流浪商人 / 已驯服宠物 /
 * 起过名字的生物一律不杀；同时**普通动物照杀**（刷牛羊猪、刷怪塔这些正常用途不能受影响）。</p>
 *
 * <p>实体用 {@code EntityType.create(level)} 直接构造、不放进世界 —— {@link GodSlaughterBlockEntity#isProtected}
 * 只看类型 / 驯服状态 / 自定义名，不需要实体真的存在于世界里。</p>
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class GodSlaughterGameTest
{
    private static final String TEMPLATE = "note_data";

    @GameTest(template = TEMPLATE)
    public static void protectsFriendlyMobs(GameTestHelper helper)
    {
        Level level = helper.getLevel();

        LivingEntity stand = create(helper, level, EntityType.ARMOR_STAND, "盔甲架");
        helper.assertTrue(GodSlaughterBlockEntity.isProtected(stand), "盔甲架应当受保护");

        LivingEntity villager = create(helper, level, EntityType.VILLAGER, "村民");
        helper.assertTrue(GodSlaughterBlockEntity.isProtected(villager), "村民应当受保护");

        LivingEntity trader = create(helper, level, EntityType.WANDERING_TRADER, "流浪商人");
        helper.assertTrue(GodSlaughterBlockEntity.isProtected(trader), "流浪商人应当受保护");

        Wolf wolf = (Wolf) create(helper, level, EntityType.WOLF, "狼");
        helper.assertTrue(!GodSlaughterBlockEntity.isProtected(wolf), "未驯服的狼不该受保护");
        wolf.setTame(true, false);
        helper.assertTrue(GodSlaughterBlockEntity.isProtected(wolf), "已驯服的狼应当受保护");

        Cow cow = (Cow) create(helper, level, EntityType.COW, "牛");
        helper.assertTrue(!GodSlaughterBlockEntity.isProtected(cow), "普通牛不该受保护（刷牛要正常可用）");
        cow.setCustomName(Component.literal("Bessie"));
        helper.assertTrue(GodSlaughterBlockEntity.isProtected(cow), "起过名字的牛应当受保护");

        // 马 / 驴：AbstractHorse **不继承 TamableAnimal**（继承的是 Animal），驯服状态存在自己的
        // isTamed() 里 —— 只判 TamableAnimal 会漏掉它们（用户实测报过）
        Horse horse = (Horse) create(helper, level, EntityType.HORSE, "马");
        helper.assertTrue(!GodSlaughterBlockEntity.isProtected(horse), "没驯服的马不该受保护");
        horse.setTamed(true);
        helper.assertTrue(GodSlaughterBlockEntity.isProtected(horse), "已驯服的马应当受保护");

        Donkey donkey = (Donkey) create(helper, level, EntityType.DONKEY, "驴");
        helper.assertTrue(!GodSlaughterBlockEntity.isProtected(donkey), "没驯服的驴不该受保护");
        donkey.setTamed(true);
        helper.assertTrue(GodSlaughterBlockEntity.isProtected(donkey), "已驯服的驴应当受保护");

        helper.succeed();
    }

    /** 造一个不入世界的实体（create 失败直接让测试红） */
    private static LivingEntity create(GameTestHelper helper, Level level, EntityType<?> type, String label)
    {
        Object entity = type.create(level);
        helper.assertTrue(entity instanceof LivingEntity, label + " 没造出来");
        return (LivingEntity) entity;
    }
}
