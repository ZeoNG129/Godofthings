package com.godofthings.item;

import com.godofthings.Godofthings;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;

/**
 * 造化垂青之杖的伤害类型（对应 useless_mod 的 ModDamageTypes.beefTool）。
 * 伤害类型本身由数据包 JSON 注册：data/godofthings/damage_type/beef_tool.json
 */
public final class WandDamageTypes
{
    public static final ResourceKey<DamageType> BEEF_TOOL = ResourceKey.create(
            Registries.DAMAGE_TYPE, ResourceLocation.tryBuild(Godofthings.MODID, "beef_tool"));

    private WandDamageTypes() {}

    public static DamageSource beefTool(ServerLevel level, Player player)
    {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(BEEF_TOOL);
        return new DamageSource(holder, player, player);
    }
}
