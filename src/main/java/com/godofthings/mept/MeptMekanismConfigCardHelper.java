package com.godofthings.mept;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;

/**
 * 通用机械配置卡设置应用助手（照抄上游 MekanismConfigCardHelper）。
 * <p>
 * 与上游不同：上游编译期依赖 Mekanism 类；本模组通过反射按同签名调用
 * （本模组 libs 里本就有 Mekanism jar，直接 import 亦可——此处照抄上游用直接 import，
 * 因为 build.gradle 已含 Mekanism compileOnly）。
 */
public class MeptMekanismConfigCardHelper
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 玩家副手是否持有已配置的通用机械配置卡。照抄上游。 */
    public static boolean hasConfiguredConfigCard(Player player)
    {
        if (!MeptModCompat.isMekanismLoaded())
        {
            return false;
        }

        try
        {
            return hasConfiguredConfigCardInternal(player);
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    private static boolean hasConfiguredConfigCardInternal(Player player)
    {
        ItemStack offHandStack = player.getItemInHand(InteractionHand.OFF_HAND);
        if (offHandStack.isEmpty())
        {
            return false;
        }

        var itemId = BuiltInRegistries.ITEM.getKey(offHandStack.getItem());
        if (itemId == null || !itemId.getNamespace().equals("mekanism"))
        {
            return false;
        }

        if (!itemId.getPath().equals("configuration_card"))
        {
            return false;
        }

        CompoundTag data = offHandStack.get(mekanism.common.registries.MekanismDataComponents.CONFIGURATION_DATA);
        return data != null && !data.isEmpty()
                && data.contains(mekanism.api.SerializationConstants.DATA_TYPE, Tag.TAG_STRING);
    }

    /** 取副手通用机械配置卡。照抄上游。 */
    public static ItemStack getOffHandConfigCard(Player player)
    {
        if (!MeptModCompat.isMekanismLoaded())
        {
            return ItemStack.EMPTY;
        }

        try
        {
            return getOffHandConfigCardInternal(player);
        }
        catch (Throwable t)
        {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack getOffHandConfigCardInternal(Player player)
    {
        ItemStack offHandStack = player.getItemInHand(InteractionHand.OFF_HAND);
        if (!offHandStack.isEmpty())
        {
            var itemId = BuiltInRegistries.ITEM.getKey(offHandStack.getItem());
            if (itemId != null && itemId.getNamespace().equals("mekanism")
                    && itemId.getPath().equals("configuration_card"))
            {
                return offHandStack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 资源检查结果——通用机械配置卡不需要额外资源。照抄上游。 */
    public static class ResourceCheckResult
    {
        public final boolean sufficient;
        public final String message;

        public ResourceCheckResult(boolean sufficient, String message)
        {
            this.sufficient = sufficient;
            this.message = message;
        }
    }

    public static ResourceCheckResult checkResourcesForMultipleBlocks(Player player, int blockCount)
    {
        return new ResourceCheckResult(true, "");
    }

    /** 把配置卡设置应用到已放置的方块实体。照抄上游。 */
    public static boolean applyConfigCardToBlock(Player player, Level level, BlockPos pos, boolean showMessage)
    {
        if (!MeptModCompat.isMekanismLoaded())
        {
            return false;
        }

        try
        {
            return applyConfigCardToBlockInternal(player, level, pos, showMessage);
        }
        catch (Throwable t)
        {
            LOGGER.warn("Failed to apply Mekanism config card (API error)", t);
            return false;
        }
    }

    private static boolean applyConfigCardToBlockInternal(Player player, Level level, BlockPos pos,
            boolean showMessage)
    {
        ItemStack configCardStack = getOffHandConfigCardInternal(player);
        if (configCardStack.isEmpty())
        {
            return false;
        }

        mekanism.api.IConfigCardAccess configCardAccess = level.getCapability(
                mekanism.common.capabilities.Capabilities.CONFIG_CARD, pos, null);
        if (configCardAccess == null)
        {
            return false;
        }

        CompoundTag cardTag = configCardStack.get(mekanism.common.registries.MekanismDataComponents.CONFIGURATION_DATA);
        if (cardTag == null || cardTag.isEmpty())
        {
            return false;
        }

        if (!cardTag.contains(mekanism.api.SerializationConstants.DATA_TYPE, Tag.TAG_STRING))
        {
            return false;
        }

        String storedTypeName = cardTag.getString(mekanism.api.SerializationConstants.DATA_TYPE);
        ResourceLocation storedTypeId = ResourceLocation.tryParse(storedTypeName);
        if (storedTypeId == null)
        {
            return false;
        }

        Block storedType = BuiltInRegistries.BLOCK.getOptional(storedTypeId).orElse(null);
        if (storedType == null)
        {
            return false;
        }

        if (!configCardAccess.isConfigurationDataCompatible(storedType))
        {
            if (showMessage)
            {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.mept.mek_config_incompatible"), true);
            }
            return false;
        }

        try
        {
            configCardAccess.setConfigurationData(level.registryAccess(), player, cardTag);
            configCardAccess.configurationDataSet();

            if (showMessage)
            {
                player.displayClientMessage(
                        Component.translatable("message.godofthings.mept.mek_config_loaded"), true);
            }
            return true;
        }
        catch (Exception e)
        {
            LOGGER.warn("Failed to apply Mekanism configuration card settings to block at {}", pos, e);
            return false;
        }
    }
}
