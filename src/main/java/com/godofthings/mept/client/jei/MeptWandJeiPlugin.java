package com.godofthings.mept.client.jei;

import com.godofthings.Godofthings;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;

import com.godofthings.mept.WandMenu;
import com.godofthings.mept.client.MeptWandScreen;
import com.godofthings.mept.client.MeptCableToolScreen;

import java.util.Collections;
import java.util.List;

/**
 * ME 放置工具的 JEI 插件（照抄上游 WandJeiPlugin）：
 * 幽灵槽支持从 JEI 拖入物品 / 流体；线缆工具界面打开时隐藏 JEI 覆盖层。
 */
@JeiPlugin
public class MeptWandJeiPlugin implements IModPlugin
{
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(Godofthings.MODID, "mept_jei_plugin");

    @Override
    public ResourceLocation getPluginUid()
    {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration)
    {
        registration.addGhostIngredientHandler(MeptWandScreen.class, new MeptWandGhostHandler());

        // 线缆工具界面打开时给 JEI 一个超大排除区，把 JEI 面板整个挡掉
        registration.addGuiContainerHandler(MeptCableToolScreen.class, new IGuiContainerHandler<MeptCableToolScreen>()
        {
            @Override
            public List<Rect2i> getGuiExtraAreas(MeptCableToolScreen screen)
            {
                return Collections.singletonList(new Rect2i(0, 0, 10000, 10000));
            }
        });
    }
}
