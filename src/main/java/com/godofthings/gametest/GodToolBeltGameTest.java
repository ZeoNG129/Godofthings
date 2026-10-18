package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.belt.ConfigData;
import com.godofthings.belt.belt.ToolBeltItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 神之皮带（v5.18.0）回归测试：移植后皮带不再依赖外部 ToolBelt 模组，
 * 准入判断改由本模组自己的内部白名单负责 —— 这里验证三条不变量：
 *
 * <ol>
 *   <li>皮带恒为满级（{@link ToolBeltItem#getSlotsCount} 恒返回上限，不再有升级状态）；</li>
 *   <li>本模组的工具（请神 / 神之更改 / 神之测量 / 神之黑盒 / 神之绑定）默认允许放进皮带；</li>
 *   <li>皮带自己不许放进自己（防止套娃）。</li>
 * </ol>
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class GodToolBeltGameTest
{
    private static final String TEMPLATE = "note_data";

    @GameTest(template = TEMPLATE)
    public static void beltIsFullLevelAndWhitelistsGodTools(GameTestHelper helper)
    {
        ItemStack belt = new ItemStack(Godofthings.GOD_BELT.get());
        if (ToolBeltItem.getSlotsCount(belt) != ToolBeltItem.MAX_SLOTS)
        {
            helper.fail("god_belt must always be at max level (" + ToolBeltItem.MAX_SLOTS + " slots)");
            return;
        }

        if (!ConfigData.isItemStackAllowed(new ItemStack(Godofthings.GOD_INVITE.get())))
        {
            helper.fail("god_invite must be allowed inside the belt (internal whitelist)");
            return;
        }

        if (ConfigData.isItemStackAllowed(belt))
        {
            helper.fail("a belt must not be placeable inside another belt");
            return;
        }

        helper.succeed();
    }
}
