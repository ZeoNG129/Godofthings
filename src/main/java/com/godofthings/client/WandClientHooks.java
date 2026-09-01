package com.godofthings.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * 造化垂青之杖的客户端钩子。
 * 独立类：仅在客户端 dist 判定为 true 时才会被类加载，专用服务器安全。
 */
public final class WandClientHooks
{
    private WandClientHooks() {}

    public static boolean isClient()
    {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    /**
     * 使用中且瞄准可刷方块时返回 UseAnim.BRUSH，否则 null。
     * 逻辑原为 GodFavorWandItem#getUseAnimation 内联的 Minecraft.getInstance() 代码。
     */
    public static UseAnim getBrushUseAnim(ItemStack stack, double reach)
    {
        if (!isClient())
        {
            return null;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        Level level = minecraft.level;

        if (player != null && level != null && player.isUsingItem())
        {
            HitResult hitResult = ProjectileUtil.getHitResultOnViewVector(
                    player,
                    e -> !e.isSpectator() && e.isPickable(),
                    reach
            );

            if (hitResult instanceof BlockHitResult blockHit
                    && hitResult.getType() == HitResult.Type.BLOCK)
            {
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState blockState = level.getBlockState(blockPos);
                if (blockState.getBlock() instanceof BrushableBlock)
                {
                    return UseAnim.BRUSH;
                }
            }
        }
        return null;
    }
}
