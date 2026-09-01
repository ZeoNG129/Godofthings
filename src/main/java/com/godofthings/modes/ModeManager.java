package com.godofthings.modes;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import com.godofthings.item.WandModes;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 模式管理器：处理模式的激活状态与互斥逻辑。
 * 移植自 useless_mod 的 ModeManager（1.20.1），原 NBT 标签 "ToolModes" 改存 CUSTOM_DATA（键名不变）。
 */
public class ModeManager
{
    private static final Set<Set<ToolMode>> MUTUAL_EXCLUSION_GROUPS = new HashSet<>();

    static
    {
        Set<ToolMode> enchantGroup = new HashSet<>();
        enchantGroup.add(ToolMode.SILK_TOUCH);
        enchantGroup.add(ToolMode.FORTUNE);
        MUTUAL_EXCLUSION_GROUPS.add(enchantGroup);

        Set<ToolMode> toolGroup = new HashSet<>();
        toolGroup.add(ToolMode.WRENCH_MODE);
        toolGroup.add(ToolMode.SCREWDRIVER_MODE);
        toolGroup.add(ToolMode.MALLET_MODE);
        toolGroup.add(ToolMode.CROWBAR_MODE);
        toolGroup.add(ToolMode.HAMMER_MODE);
        toolGroup.add(ToolMode.OMNITOOL_MODE);
        MUTUAL_EXCLUSION_GROUPS.add(toolGroup);
    }

    private final Map<ToolMode, Boolean> activeModes;

    public ModeManager()
    {
        this.activeModes = new EnumMap<>(ToolMode.class);
        for (ToolMode mode : ToolMode.values())
        {
            activeModes.put(mode, false);
        }
    }

    public void loadFromStack(ItemStack stack)
    {
        CompoundTag modesTag = WandModes.getCompound(stack, WandModes.TOOL_MODES);
        for (ToolMode mode : ToolMode.values())
        {
            activeModes.put(mode, modesTag.getBoolean(mode.getName()));
        }
        ensureMutualExclusion();
    }

    public void saveToStack(ItemStack stack)
    {
        CompoundTag modesTag = new CompoundTag();
        for (Map.Entry<ToolMode, Boolean> entry : activeModes.entrySet())
        {
            modesTag.putBoolean(entry.getKey().getName(), entry.getValue());
        }
        WandModes.setCompound(stack, WandModes.TOOL_MODES, modesTag);
    }

    public void toggleMode(ToolMode mode)
    {
        if (mode == ToolMode.SILK_TOUCH || mode == ToolMode.FORTUNE)
        {
            ToolMode opposite = mode == ToolMode.SILK_TOUCH ? ToolMode.FORTUNE : ToolMode.SILK_TOUCH;
            if (activeModes.get(mode))
            {
                activeModes.put(mode, false);
                activeModes.put(opposite, true);
            }
            else
            {
                activeModes.put(mode, true);
                activeModes.put(opposite, false);
            }
        }
        else
        {
            activeModes.put(mode, !activeModes.get(mode));
            handleMutualExclusion(mode);
        }
    }

    public void setModeActive(ToolMode mode, boolean active)
    {
        if (mode == ToolMode.SILK_TOUCH || mode == ToolMode.FORTUNE)
        {
            ToolMode opposite = mode == ToolMode.SILK_TOUCH ? ToolMode.FORTUNE : ToolMode.SILK_TOUCH;
            activeModes.put(mode, active);
            activeModes.put(opposite, !active);
        }
        else
        {
            activeModes.put(mode, active);
            if (active)
            {
                handleMutualExclusion(mode);
            }
        }
    }

    public boolean isModeActive(ToolMode mode)
    {
        return activeModes.getOrDefault(mode, false);
    }

    public Set<ToolMode> getActiveModes()
    {
        Set<ToolMode> result = new HashSet<>();
        for (Map.Entry<ToolMode, Boolean> entry : activeModes.entrySet())
        {
            if (entry.getValue())
            {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private void handleMutualExclusion(ToolMode activatedMode)
    {
        if (!activeModes.get(activatedMode))
        {
            return;
        }
        for (Set<ToolMode> group : MUTUAL_EXCLUSION_GROUPS)
        {
            if (group.contains(activatedMode))
            {
                for (ToolMode mode : group)
                {
                    if (mode != activatedMode)
                    {
                        activeModes.put(mode, false);
                    }
                }
            }
        }
    }

    private void ensureMutualExclusion()
    {
        for (Set<ToolMode> group : MUTUAL_EXCLUSION_GROUPS)
        {
            int activeCount = 0;
            ToolMode lastActive = null;
            for (ToolMode mode : group)
            {
                if (activeModes.get(mode))
                {
                    activeCount++;
                    lastActive = mode;
                }
            }
            if (activeCount > 1)
            {
                for (ToolMode mode : group)
                {
                    activeModes.put(mode, mode == lastActive);
                }
            }
            else if (activeCount == 0 && group.contains(ToolMode.SILK_TOUCH) && group.contains(ToolMode.FORTUNE))
            {
                activeModes.put(ToolMode.FORTUNE, true);
                activeModes.put(ToolMode.SILK_TOUCH, false);
            }
        }
    }

    public int getTotalModes()
    {
        return ToolMode.getTotalModes();
    }

    public ModeManager copy()
    {
        ModeManager copy = new ModeManager();
        copy.activeModes.putAll(this.activeModes);
        return copy;
    }
}
