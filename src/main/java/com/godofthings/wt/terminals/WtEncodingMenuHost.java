package com.godofthings.wt.terminals;

import java.util.function.BiConsumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import appeng.api.implementations.blockentities.IViewCellStorage;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import appeng.parts.encoding.PatternEncodingLogic;

import com.godofthings.wt.core.WtComponents;
import com.godofthings.wt.core.WtMenuHost;
import com.godofthings.wt.core.WtTerminalItem;

/**
 * 无线样板终端宿主（照抄 ae2wtlib 的 {@code WETMenuHost}；样板编码状态存在物品组件
 * {@code godofthings:wt_pattern_encoding_logic} 里）。
 */
public class WtEncodingMenuHost extends WtMenuHost
        implements IViewCellStorage, IPatternTerminalMenuHost, IPatternTerminalLogicHost
{
    private final PatternEncodingLogic logic = new PatternEncodingLogic(this);

    public WtEncodingMenuHost(WtTerminalItem item, Player player, ItemMenuHostLocator locator,
            BiConsumer<Player, ISubMenu> returnToMainMenu)
    {
        super(item, player, locator, returnToMainMenu);
        logic.readFromNBT(getItemStack().getOrDefault(WtComponents.PATTERN_ENCODING_LOGIC, new CompoundTag()),
                player.registryAccess());
    }

    @Override
    public PatternEncodingLogic getLogic()
    {
        return logic;
    }

    @Override
    public Level getLevel()
    {
        return getPlayer().level();
    }

    @Override
    public void markForSave()
    {
        CompoundTag tag = getItemStack().getOrDefault(WtComponents.PATTERN_ENCODING_LOGIC, new CompoundTag());
        logic.writeToNBT(tag, getPlayer().registryAccess());
        getItemStack().set(WtComponents.PATTERN_ENCODING_LOGIC, tag);
    }
}
