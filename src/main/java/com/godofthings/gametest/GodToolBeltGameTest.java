package com.godofthings.gametest;

import com.godofthings.Godofthings;
import com.godofthings.handler.ToolBeltCompat;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 工具皮带兼容（v5.15.8）回归测试：注入入口在未装 ToolBelt 的环境下必须为无操作且不抛错
 * （GameTest 服务器没有 ToolBelt，正好覆盖"模组未装直接跳过"分支）。
 */
@GameTestHolder(Godofthings.MODID)
@PrefixGameTestTemplate(false)
public class GodToolBeltGameTest
{
    private static final String TEMPLATE = "note_data";

    @GameTest(template = TEMPLATE)
    public static void injectionIsNoOpWithoutToolBelt(GameTestHelper helper)
    {
        ToolBeltCompat.injectWhitelist(); // 未装 ToolBelt：应直接跳过、不抛任何异常
        helper.succeed();
    }
}
