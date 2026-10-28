package com.godofthings.compass.explorers.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.godofthings.Godofthings;
import com.godofthings.compass.explorers.ExplorersCompassData;
import com.godofthings.compass.explorers.config.BetterUI;
import com.godofthings.compass.explorers.config.ConfigHandler;
import com.godofthings.compass.explorers.gui.GuiWrapper;
import com.godofthings.compass.explorers.network.SyncPacket;
import com.godofthings.compass.explorers.util.CompassState;
import com.godofthings.compass.explorers.util.ItemUtils;
import com.godofthings.compass.explorers.util.PlayerUtils;
import com.godofthings.compass.explorers.util.StructureUtils;
import com.godofthings.compass.explorers.worker.SearchWorkerManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 探险者指南针（移植自 Explorer's Compass 1.20.x，作者 ChaosTheDude，CC BY-NC-SA 4.0）。
 *
 * <p>右键打开结构选择界面；潜行右键中断当前搜索并把状态复位。所有状态都记在物品的
 * {@code minecraft:custom_data} 组件上（键名与上游 NBT 完全一致，见
 * {@link ExplorersCompassData#getTag}），所以换手 / 掉地上 / 进箱子都不会丢。</p>
 *
 * <p>上游这里用的是 {@code stack.getTag()} / {@code setTag()}（1.20 的 NBT 直挂）；
 * 1.21 改成数据组件之后，读写统一走 {@link ExplorersCompassData#updateTag} 与
 * {@link ExplorersCompassData#getTag}。{@code CustomModelData} 也从裸 NBT 键换成了
 * {@link DataComponents#CUSTOM_MODEL_DATA} 组件。</p>
 */
public class ExplorersCompassItem extends Item
{
    /** 物品注册名（注册时用，见 {@link Godofthings#EXPLORERS_COMPASS}） */
    public static final String NAME = "explorers";

    /** 当前正在跑的搜索（一个物品实例同时只跑一个；换目标会先 stop 旧的） */
    private final SearchWorkerManager workerManager;

    public ExplorersCompassItem(Properties properties)
    {
        // 上游把 stacksTo(1) 写死在构造器里；这里由注册点传进来的 Properties 派生，与仓库其它物品一致
        super(properties.stacksTo(1));
        workerManager = new SearchWorkerManager();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        if (!player.isCrouching())
        {
            if (level.isClientSide())
            {
                // 客户端类引用关在 lambda 体内：专用服务端不会执行到这里，也就不会加载它
                ItemStack stack = ItemUtils.getHeldExplorersCompass(player);
                GuiWrapper.openGUI(level, player, stack);
            }
            else
            {
                ServerLevel serverLevel = (ServerLevel) level;
                ServerPlayer serverPlayer = (ServerPlayer) player;
                boolean canTeleport = ConfigHandler.GENERAL.allowTeleport.get()
                        && PlayerUtils.canTeleport(player.getServer(), player);
                PacketDistributor.sendToPlayer(serverPlayer, new SyncPacket(canTeleport,
                        StructureUtils.getAllowedStructureKeys(serverLevel),
                        StructureUtils.getGeneratingDimensionsForAllowedStructures(serverLevel),
                        StructureUtils.getStructureKeysToTypeKeys(serverLevel),
                        StructureUtils.getTypeKeysToStructureKeys(serverLevel)));
            }
        }
        else
        {
            workerManager.stop();
            workerManager.clear();
            setState(player.getItemInHand(hand), null, CompassState.INACTIVE, player);
        }
        return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(hand));
    }

    /**
     * 状态没变就不播「切换物品」动画（上游 1.20 Forge 重写的是
     * {@code Item#shouldCauseReequipAnimation}；NeoForge 1.21 把同名方法挪到了
     * {@code IItemExtension} 默认方法上，签名一致，所以直接 {@code @Override}）。
     */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        if (getState(oldStack) == getState(newStack))
        {
            return false;
        }
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    /** 开始搜索（客户端发来的请求由服务端在这里真正派 worker） */
    public void searchForStructure(Level level, Player player, ResourceLocation categoryKey,
            List<ResourceLocation> structureKeys, BlockPos pos, ItemStack stack)
    {
        setSearching(stack, categoryKey, player);
        setSearchRadius(stack, 0, player);
        if (level instanceof ServerLevel serverLevel)
        {
            List<Structure> structures = new ArrayList<>();
            for (ResourceLocation key : structureKeys)
            {
                structures.add(StructureUtils.getStructureForKey(serverLevel, key));
            }
            workerManager.stop();
            workerManager.createWorkers(serverLevel, player, stack, structures, pos);
            boolean started = workerManager.start();
            if (!started)
            {
                setNotFound(stack, 0, 0);
            }
        }
    }

    /** 搜到了：写坐标 + 可选的自定义模型数据 + 是否显示坐标，并清空任务队列 */
    public void succeed(ItemStack stack, ResourceLocation structureKey, int x, int z, int samples,
            boolean displayCoordinates)
    {
        setFound(stack, structureKey, x, z, samples);
        if (ConfigHandler.GENERAL.customResource.get())
        {
            List<Map.Entry<String, Integer>> entries = BetterUI.getEntries();
            for (Map.Entry<String, Integer> entry : entries)
            {
                if (structureKey != null && structureKey.toString().equals(entry.getKey()))
                {
                    stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(entry.getValue()));
                }
            }
        }
        setDisplayCoordinates(stack, displayCoordinates);
        workerManager.clear();
    }

    public void fail(ItemStack stack, int radius, int samples)
    {
        workerManager.pop();
        boolean started = workerManager.start();
        if (!started)
        {
            setNotFound(stack, radius, samples);
        }
    }

    public boolean isActive(ItemStack stack)
    {
        return getState(stack) != null && getState(stack) != CompassState.INACTIVE;
    }

    public void setSearching(ItemStack stack, ResourceLocation structureKey, Player player)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            ExplorersCompassData.updateTag(stack, tag ->
            {
                tag.putString("StructureKey", structureKey == null ? "" : structureKey.toString());
                tag.putInt("State", CompassState.SEARCHING.getID());
            });
        }
    }

    public void setFound(ItemStack stack, ResourceLocation structureKey, int x, int z, int samples)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            ExplorersCompassData.updateTag(stack, tag ->
            {
                tag.putInt("State", CompassState.FOUND.getID());
                tag.putString("StructureKey", structureKey == null ? "" : structureKey.toString());
                tag.putInt("FoundX", x);
                tag.putInt("FoundZ", z);
                tag.putInt("Samples", samples);
            });
            setHaveSearched(stack, structureKey, true, x, z);
        }
    }

    public void setNotFound(ItemStack stack, int searchRadius, int samples)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            ExplorersCompassData.updateTag(stack, tag ->
            {
                tag.putInt("State", CompassState.NOT_FOUND.getID());
                tag.putInt("SearchRadius", searchRadius);
                tag.putInt("Samples", samples);
            });
            setHaveSearched(stack, getStructureKey(stack), false, 0, 0);
        }
    }

    public void setInactive(ItemStack stack, Player player)
    {
        setState(stack, null, CompassState.INACTIVE, player);
    }

    public void setState(ItemStack stack, BlockPos pos, CompassState state, Player player)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            ExplorersCompassData.updateTag(stack, tag -> tag.putInt("State", state.getID()));
        }
    }

    public void setFoundStructureX(ItemStack stack, int x, Player player)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            ExplorersCompassData.updateTag(stack, tag -> tag.putInt("FoundX", x));
        }
    }

    public void setFoundStructureZ(ItemStack stack, int z, Player player)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            ExplorersCompassData.updateTag(stack, tag -> tag.putInt("FoundZ", z));
        }
    }

    public void setStructureKey(ItemStack stack, ResourceLocation structureKey, Player player)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            ExplorersCompassData.updateTag(stack,
                    tag -> tag.putString("StructureKey", structureKey == null ? "" : structureKey.toString()));
        }
    }

    public void setSearchRadius(ItemStack stack, int searchRadius, Player player)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            ExplorersCompassData.updateTag(stack, tag -> tag.putInt("SearchRadius", searchRadius));
        }
    }

    public void setSamples(ItemStack stack, int samples, Player player)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            ExplorersCompassData.updateTag(stack, tag -> tag.putInt("Samples", samples));
        }
    }

    public void setDisplayCoordinates(ItemStack stack, boolean displayPosition)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            ExplorersCompassData.updateTag(stack, tag -> tag.putBoolean("DisplayCoordinates", displayPosition));
        }
    }

    public CompassState getState(ItemStack stack)
    {
        if (ExplorersCompassData.isCompass(stack))
        {
            CompoundTag tag = ExplorersCompassData.getTag(stack);
            if (tag.contains("State"))
            {
                return CompassState.fromID(tag.getInt("State"));
            }
        }
        return null;
    }

    public int getFoundStructureX(ItemStack stack)
    {
        return ExplorersCompassData.getTag(stack).getInt("FoundX");
    }

    public int getFoundStructureX1(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("HaveSearched"))
        {
            return tag.getCompound("HaveSearched").getInt("FoundX1");
        }
        return 0;
    }

    public int getFoundStructureX2(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("HaveSearched"))
        {
            return tag.getCompound("HaveSearched").getInt("FoundX2");
        }
        return 0;
    }

    public int getFoundStructureZ(ItemStack stack)
    {
        return ExplorersCompassData.getTag(stack).getInt("FoundZ");
    }

    public int getFoundStructureZ1(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("HaveSearched"))
        {
            return tag.getCompound("HaveSearched").getInt("FoundZ1");
        }
        return 0;
    }

    public int getFoundStructureZ2(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("HaveSearched"))
        {
            return tag.getCompound("HaveSearched").getInt("FoundZ2");
        }
        return 0;
    }

    public ResourceLocation getStructureKey(ItemStack stack)
    {
        return parseKey(ExplorersCompassData.getTag(stack).getString("StructureKey"));
    }

    public ResourceLocation getStructureKey1(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("HaveSearched"))
        {
            return parseKey(tag.getCompound("HaveSearched").getString("StructureKey1"));
        }
        return ExplorersCompassData.NONE;
    }

    public ResourceLocation getStructureKey2(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("HaveSearched"))
        {
            return parseKey(tag.getCompound("HaveSearched").getString("StructureKey2"));
        }
        return ExplorersCompassData.NONE;
    }

    public int getSearchRadius(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        return tag.contains("SearchRadius") ? tag.getInt("SearchRadius") : -1;
    }

    public int getSamples(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        return tag.contains("Samples") ? tag.getInt("Samples") : -1;
    }

    public int getDistanceToBiome(Player player, ItemStack stack)
    {
        return StructureUtils.getHorizontalDistanceToLocation(player, getFoundStructureX(stack),
                getFoundStructureZ(stack));
    }

    public boolean shouldDisplayCoordinates(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("DisplayCoordinates"))
        {
            return tag.getBoolean("DisplayCoordinates");
        }
        return true;
    }

    public int getOrInitHaveFoundTimes(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("HaveSearched"))
        {
            return tag.getInt("HaveFoundTimes");
        }
        return 0;
    }

    public boolean getSearchResult1(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("HaveSearched"))
        {
            return tag.getCompound("HaveSearched").getBoolean("SearchState1");
        }
        return false;
    }

    public boolean getSearchResult2(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("HaveSearched"))
        {
            return tag.getCompound("HaveSearched").getBoolean("SearchState2");
        }
        return false;
    }

    public int getSearchedTimes(ItemStack stack)
    {
        CompoundTag tag = ExplorersCompassData.getTag(stack);
        if (tag.contains("HaveSearched"))
        {
            return tag.getCompound("HaveSearched").getInt("SearchedTimes");
        }
        return 0;
    }

    /**
     * 维护「最近两次搜索」的历史（HUD 右上角那两行）。上游这里直接改
     * {@code stack.getTag().getCompound("HaveSearched")} 拿到的<b>活对象</b>；1.21 的
     * {@code CustomData#copyTag} 返回副本，所以必须整体写回去（见 {@link ExplorersCompassData#updateTag}）。
     */
    public void setHaveSearched(ItemStack stack, ResourceLocation structureKey, boolean isFound, int x, int z)
    {
        if (!ExplorersCompassData.isCompass(stack))
        {
            return;
        }
        String key = structureKey == null ? "" : structureKey.toString();
        ExplorersCompassData.updateTag(stack, tag ->
        {
            if (!tag.contains("HaveSearched"))
            {
                CompoundTag history = new CompoundTag();
                history.putString("StructureKey", key);
                history.putString("StructureKey1", "");
                history.putString("StructureKey2", "");

                history.putBoolean("SearchState", isFound);
                history.putBoolean("SearchState1", false);
                history.putBoolean("SearchState2", false);
                history.putInt("FoundX", x);
                history.putInt("FoundX1", 0);
                history.putInt("FoundX2", 0);
                history.putInt("FoundZ", z);
                history.putInt("FoundZ1", 0);
                history.putInt("FoundZ2", 0);
                history.putInt("SearchedTimes", 0);
                tag.put("HaveSearched", history);
            }
            else
            {
                CompoundTag history = tag.getCompound("HaveSearched");
                history.putString("StructureKey2", history.getString("StructureKey1"));
                history.putString("StructureKey1", history.getString("StructureKey"));
                history.putString("StructureKey", key);
                history.putBoolean("SearchState2", history.getBoolean("SearchState1"));
                history.putBoolean("SearchState1", history.getBoolean("SearchState"));
                history.putBoolean("SearchState", isFound);
                history.putInt("FoundX2", history.getInt("FoundX1"));
                history.putInt("FoundZ2", history.getInt("FoundZ1"));
                history.putInt("FoundX1", history.getInt("FoundX"));
                history.putInt("FoundZ1", history.getInt("FoundZ"));
                history.putInt("FoundX", x);
                history.putInt("FoundZ", z);
                history.putInt("SearchedTimes", history.getInt("SearchedTimes") + 1);
                tag.put("HaveSearched", history);
            }
        });
    }

    /** 空串 / 非法 id 一律退回 {@link ExplorersCompassData#NONE}（1.21 的 ResourceLocation 构造器私有，解析失败会抛） */
    private static ResourceLocation parseKey(String key)
    {
        if (key == null || key.isEmpty())
        {
            return ExplorersCompassData.NONE;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(key);
        return parsed == null ? ExplorersCompassData.NONE : parsed;
    }
}
