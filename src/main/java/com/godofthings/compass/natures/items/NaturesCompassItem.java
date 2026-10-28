package com.godofthings.compass.natures.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.godofthings.Godofthings;
import com.godofthings.compass.natures.NaturesCompassData;
import com.godofthings.compass.natures.config.ConfigHandler;
import com.godofthings.compass.natures.gui.GuiWrapper;
import com.godofthings.compass.natures.network.SyncPacket;
import com.godofthings.compass.natures.util.BiomeSearchWorker;
import com.godofthings.compass.natures.util.BiomeUtils;
import com.godofthings.compass.natures.util.CompassState;
import com.godofthings.compass.natures.util.ItemUtils;
import com.godofthings.compass.natures.util.PlayerUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 自然罗盘（移植自 Natures Compass 3.4.0，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>右键打开群系选择界面；潜行右键中断当前搜索并把状态复位。所有状态都记在物品的数据组件上
 * （见 {@link NaturesCompassData}），所以换手 / 掉地上 / 进箱子都不会丢。</p>
 *
 * <p>配置项 {@code compassDurability} 大于 0 时，每成功搜到一次就 +1 计数（{@link NaturesCompassData#DAMAGE}），
 * 计数满了就「坏掉」不能再搜，得用「罗盘 + 指南针」无序合成修（见 repair_natures_compass 配方）。</p>
 */
public class NaturesCompassItem extends Item
{
    /** 物品注册名（注册时用，见 {@link Godofthings#NATURES_COMPASS}） */
    public static final String NAME = "natures";

    /** 当前正在跑的搜索（一个物品实例同时只跑一个；换目标会先 stop 旧的） */
    private BiomeSearchWorker worker;

    public NaturesCompassItem(Properties properties)
    {
        // 上游是 stacksTo(1) 写死在构造器里；这里改成由注册点传进来的 Properties 派生，与其他物品一致
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (!player.isCrouching())
        {
            if (isBroken(player.getItemInHand(hand)))
            {
                return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(hand));
            }

            if (level.isClientSide())
            {
                // 客户端类引用关在 lambda 体内：专用服务端不会执行到这里，也就不会加载它
                ItemStack stack = ItemUtils.getHeldNatureCompass(player);
                GuiWrapper.openGUI(level, player, stack);
            }
            else
            {
                ServerLevel serverLevel = (ServerLevel) level;
                ServerPlayer serverPlayer = (ServerPlayer) player;
                boolean canTeleport = ConfigHandler.GENERAL.allowTeleport.get()
                        && PlayerUtils.canTeleport(serverPlayer.getServer(), player);
                int maxNextSearches = ConfigHandler.GENERAL.maxNextSearches.get();
                boolean hasInfiniteXp = player.hasInfiniteMaterials();
                List<ResourceLocation> allowedBiomeKeys = BiomeUtils.getAllowedBiomeKeys(level);
                Map<ResourceLocation, Integer> xpLevels = BiomeUtils.getXpLevelsForAllowedBiomes(allowedBiomeKeys);
                PacketDistributor.sendToPlayer(serverPlayer, new SyncPacket(canTeleport, maxNextSearches, hasInfiniteXp,
                        allowedBiomeKeys, xpLevels, BiomeUtils.getGeneratingDimensionsForAllowedBiomes(serverLevel)));
            }
        }
        else
        {
            if (worker != null)
            {
                worker.stop();
                worker = null;
            }
            setState(player.getItemInHand(hand), null, CompassState.INACTIVE, player);
        }

        return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(hand));
    }

    @Override
    public boolean isBarVisible(ItemStack stack)
    {
        int max = ConfigHandler.GENERAL.compassDurability.get();
        if (max > 0)
        {
            return stack.getOrDefault(NaturesCompassData.DAMAGE.get(), 0) > 0;
        }
        return false;
    }

    @Override
    public int getBarWidth(ItemStack stack)
    {
        int max = ConfigHandler.GENERAL.compassDurability.get();
        if (max > 0)
        {
            int damage = stack.getOrDefault(NaturesCompassData.DAMAGE.get(), 0);
            return Math.round(Math.max(0, Math.min(13, 13.0f * (1.0f - (float) damage / max))));
        }
        return 13;
    }

    @Override
    public int getBarColor(ItemStack stack)
    {
        int max = ConfigHandler.GENERAL.compassDurability.get();
        int damage = stack.getOrDefault(NaturesCompassData.DAMAGE.get(), 0);
        float f = max > 0 ? (float) damage / max : 0.0f;
        return Mth.hsvToRgb(Math.max(0.0F, (1.0F - f) / 3.0F), 1.0F, 1.0F);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        if (getState(oldStack) == getState(newStack))
        {
            return false;
        }
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    /** 开始搜索某个群系（服务端；经验不够 / 罗盘已坏时整个跳过） */
    public void searchForBiome(ServerLevel level, Player player, ResourceLocation biomeKey, BlockPos pos, ItemStack stack)
    {
        if (isBroken(stack))
        {
            return;
        }
        setSearching(stack, biomeKey, player);
        stack.set(NaturesCompassData.SAMPLES.get(), 0);
        stack.set(NaturesCompassData.SEARCH_RADIUS.get(), 0);
        stack.remove(NaturesCompassData.PREV_POS.get());

        if (worker != null)
        {
            worker.stop();
        }
        List<BlockPos> prevPos = new ArrayList<>();
        worker = new BiomeSearchWorker(level, player, stack, biomeKey, pos, prevPos);
        worker.start();

        int xpLevels = BiomeUtils.getXpLevelsForBiome(biomeKey);
        if (!player.hasInfiniteMaterials() && xpLevels > 0)
        {
            player.giveExperienceLevels(-xpLevels);
        }
    }

    /** 搜索同一个群系的下一个实例（跳过已经找到过的那几片） */
    public void searchForNextBiome(ServerLevel level, Player player, BlockPos pos, ItemStack stack)
    {
        if (isBroken(stack))
        {
            return;
        }
        List<BlockPos> prevPos = stack.getOrDefault(NaturesCompassData.PREV_POS.get(), null);
        String biomeKeyStr = stack.has(NaturesCompassData.BIOME_ID.get())
                ? stack.get(NaturesCompassData.BIOME_ID.get())
                : null;
        if (prevPos != null && biomeKeyStr != null)
        {
            ResourceLocation biomeKey = ResourceLocation.parse(biomeKeyStr);
            setSearching(stack, biomeKey, player);
            stack.set(NaturesCompassData.SAMPLES.get(), 0);
            stack.set(NaturesCompassData.SEARCH_RADIUS.get(), 0);

            if (worker != null)
            {
                worker.stop();
            }
            worker = new BiomeSearchWorker(level, player, stack, biomeKey, pos, prevPos);
            worker.start();

            int xpLevels = BiomeUtils.getXpLevelsForBiome(biomeKey);
            if (!player.hasInfiniteMaterials() && xpLevels > 0)
            {
                player.giveExperienceLevels(-xpLevels);
            }
        }
    }

    /** 搜索成功（由 {@link BiomeSearchWorker} 回调）：记坐标 / 采样数 / 历史位置，并扣一次耐久 */
    public void succeed(ItemStack stack, Player player, int x, int z, List<BlockPos> prevPos, int samples,
            boolean displayCoordinates)
    {
        setFound(stack, x, z, samples, player);
        setDisplayCoordinates(stack, displayCoordinates);
        stack.set(NaturesCompassData.PREV_POS.get(), prevPos);
        damageCompass(stack);
        worker = null;
    }

    /** 搜索失败（由 {@link BiomeSearchWorker} 回调） */
    public void fail(ItemStack stack, Player player, int radius, int samples)
    {
        setNotFound(stack, player, radius, samples);
        worker = null;
    }

    private void damageCompass(ItemStack stack)
    {
        int max = ConfigHandler.GENERAL.compassDurability.get();
        if (max > 0)
        {
            int damage = stack.getOrDefault(NaturesCompassData.DAMAGE.get(), 0) + 1;
            stack.set(NaturesCompassData.DAMAGE.get(), damage);
        }
    }

    /** 用满了配置的次数（{@code compassDurability}）就不能再搜了 */
    public boolean isBroken(ItemStack stack)
    {
        int max = ConfigHandler.GENERAL.compassDurability.get();
        return max > 0 && stack.getOrDefault(NaturesCompassData.DAMAGE.get(), 0) >= max;
    }

    public boolean isActive(ItemStack stack)
    {
        return NaturesCompassData.isCompass(stack) && getState(stack) != CompassState.INACTIVE;
    }

    public void setSearching(ItemStack stack, ResourceLocation biomeKey, Player player)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.BIOME_ID.get(), biomeKey.toString());
            stack.set(NaturesCompassData.COMPASS_STATE.get(), CompassState.SEARCHING.getID());
        }
    }

    public void setFound(ItemStack stack, int x, int z, int samples, Player player)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.COMPASS_STATE.get(), CompassState.FOUND.getID());
            stack.set(NaturesCompassData.FOUND_X.get(), x);
            stack.set(NaturesCompassData.FOUND_Z.get(), z);
            stack.set(NaturesCompassData.SAMPLES.get(), samples);
        }
    }

    public void setNotFound(ItemStack stack, Player player, int searchRadius, int samples)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.COMPASS_STATE.get(), CompassState.NOT_FOUND.getID());
            stack.set(NaturesCompassData.SEARCH_RADIUS.get(), searchRadius);
            stack.set(NaturesCompassData.SAMPLES.get(), samples);
        }
    }

    public void setInactive(ItemStack stack, Player player)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.COMPASS_STATE.get(), CompassState.INACTIVE.getID());
        }
    }

    public void setState(ItemStack stack, BlockPos pos, CompassState state, Player player)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.COMPASS_STATE.get(), state.getID());
        }
    }

    public void setFoundBiomeX(ItemStack stack, int x, Player player)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.FOUND_X.get(), x);
        }
    }

    public void setFoundBiomeZ(ItemStack stack, int z, Player player)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.FOUND_Z.get(), z);
        }
    }

    public void setBiomeKey(ItemStack stack, ResourceLocation biomeKey, Player player)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.BIOME_ID.get(), biomeKey.toString());
        }
    }

    public void setSearchRadius(ItemStack stack, int searchRadius, Player player)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.SEARCH_RADIUS.get(), searchRadius);
        }
    }

    public void setSamples(ItemStack stack, int samples, Player player)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.SAMPLES.get(), samples);
        }
    }

    public CompassState getState(ItemStack stack)
    {
        if (NaturesCompassData.isCompass(stack) && stack.has(NaturesCompassData.COMPASS_STATE.get()))
        {
            return CompassState.fromID(stack.get(NaturesCompassData.COMPASS_STATE.get()));
        }
        return null;
    }

    public void setDisplayCoordinates(ItemStack stack, boolean displayPosition)
    {
        if (NaturesCompassData.isCompass(stack))
        {
            stack.set(NaturesCompassData.DISPLAY_COORDS.get(), displayPosition);
        }
    }

    public int getFoundBiomeX(ItemStack stack)
    {
        if (NaturesCompassData.isCompass(stack) && stack.has(NaturesCompassData.FOUND_X.get()))
        {
            return stack.get(NaturesCompassData.FOUND_X.get());
        }
        return 0;
    }

    public int getFoundBiomeZ(ItemStack stack)
    {
        if (NaturesCompassData.isCompass(stack) && stack.has(NaturesCompassData.FOUND_Z.get()))
        {
            return stack.get(NaturesCompassData.FOUND_Z.get());
        }
        return 0;
    }

    public ResourceLocation getBiomeKey(ItemStack stack)
    {
        if (NaturesCompassData.isCompass(stack) && stack.has(NaturesCompassData.BIOME_ID.get()))
        {
            return ResourceLocation.parse(stack.get(NaturesCompassData.BIOME_ID.get()));
        }
        return ResourceLocation.fromNamespaceAndPath("", "");
    }

    public int getSearchRadius(ItemStack stack)
    {
        if (NaturesCompassData.isCompass(stack) && stack.has(NaturesCompassData.SEARCH_RADIUS.get()))
        {
            return stack.get(NaturesCompassData.SEARCH_RADIUS.get());
        }
        return -1;
    }

    public int getSamples(ItemStack stack)
    {
        if (NaturesCompassData.isCompass(stack) && stack.has(NaturesCompassData.SAMPLES.get()))
        {
            return stack.get(NaturesCompassData.SAMPLES.get());
        }
        return -1;
    }

    public int getDistanceToBiome(Player player, ItemStack stack)
    {
        return BiomeUtils.getDistanceToBiome(player, getFoundBiomeX(stack), getFoundBiomeZ(stack));
    }

    public boolean shouldDisplayCoordinates(ItemStack stack)
    {
        if (NaturesCompassData.isCompass(stack) && stack.has(NaturesCompassData.DISPLAY_COORDS.get()))
        {
            return stack.get(NaturesCompassData.DISPLAY_COORDS.get());
        }
        return true;
    }

    /** 当前正在跑的 worker（界面里「搜索下一个」会先看它有没有在跑） */
    public Optional<BiomeSearchWorker> getWorker()
    {
        return Optional.ofNullable(worker);
    }
}
