package com.godofthings.elevator;

import com.godofthings.Godofthings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 电梯的服务端配置。
 *
 * <p>移植自 ElevatorMod 的 {@code com.vsngarcia.Config}（MIT），逐行对照：键名、默认值、
 * 取值范围全部保留，方便按上游文档对照调参。</p>
 */
@EventBusSubscriber(modid = Godofthings.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ElevatorConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static
    {
        BUILDER.push("General");

        SAME_COLOR = BUILDER
                .comment("Should elevators have the same color in order to teleport ?")
                .define("sameColor", false);

        RANGE = BUILDER
                .comment("Elevator range")
                .defineInRange("range", 384, 3, 4064);

        PRECISION_TARGET = BUILDER
                .comment("Realign players to the center of elevator ?")
                .define("precisionTarget", true);

        MOB_SPAWN = BUILDER
                .worldRestart()
                .comment("Can mobs spawn on elevators ?")
                .define("mobSpawn", false);

        RESET_PITCH_NORMAL = BUILDER
                .comment("Reset pitch to 0 when teleporting to normal elevators ?")
                .define("resetPitchNormal", false);

        RESET_PITCH_DIRECTIONAL = BUILDER
                .comment("Reset pitch to 0 when teleporting to directional elevators ?")
                .define("resetPitchDirectional", true);

        USE_XP = BUILDER
                .comment("Should teleporting require XP ?")
                .define("useXP", false);

        XP_POINTS_AMOUNT = BUILDER
                .comment("Amount of XP points to use when useXP is enabled", "Note this is NOT experience levels")
                .defineInRange("XPPointsAmount", 1, 1, Integer.MAX_VALUE);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private static ModConfigSpec.BooleanValue SAME_COLOR;
    private static ModConfigSpec.IntValue RANGE;
    private static ModConfigSpec.BooleanValue PRECISION_TARGET;
    private static ModConfigSpec.BooleanValue MOB_SPAWN;
    private static ModConfigSpec.BooleanValue RESET_PITCH_NORMAL;
    private static ModConfigSpec.BooleanValue RESET_PITCH_DIRECTIONAL;
    private static ModConfigSpec.BooleanValue USE_XP;
    private static ModConfigSpec.IntValue XP_POINTS_AMOUNT;

    public static final ModConfigSpec SPEC;

    private ElevatorConfig() {}

    /** 传送的两台电梯必须同色吗（默认否）。 */
    public static boolean sameColor()
    {
        return SAME_COLOR.get();
    }

    /** 传送搜索半径（格）。 */
    public static int range()
    {
        return RANGE.get();
    }

    /** 传送后是否对齐到方块中心（默认是）。 */
    public static boolean precisionTarget()
    {
        return PRECISION_TARGET.get();
    }

    /** 生物能否生成在电梯上（默认否）。 */
    public static boolean mobSpawn()
    {
        return MOB_SPAWN.get();
    }

    public static boolean resetPitchNormal()
    {
        return RESET_PITCH_NORMAL.get();
    }

    public static boolean resetPitchDirectional()
    {
        return RESET_PITCH_DIRECTIONAL.get();
    }

    /** 传送是否消耗经验（默认否）。 */
    public static boolean useXP()
    {
        return USE_XP.get();
    }

    /** 每次传送消耗的经验点数（不是等级）。 */
    public static int xpPointsAmount()
    {
        return XP_POINTS_AMOUNT.get();
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent.Loading event)
    {
        // 配置加载即可，值由上面的 getter 现取
    }

    @SubscribeEvent
    public static void onFileChange(ModConfigEvent.Reloading event)
    {
        // 文件变更后 getter 会自动读到新值
    }
}
