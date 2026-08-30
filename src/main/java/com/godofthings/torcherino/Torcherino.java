package com.godofthings.torcherino;

import com.godofthings.Godofthings;
import com.godofthings.torcherino.api.Tier;
import com.godofthings.torcherino.block.TorcherinoBlock;
import com.godofthings.torcherino.block.WallTorcherinoBlock;
import com.godofthings.torcherino.block.entity.TorcherinoBlockEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 加速火把（Torcherino）子模块：已收编进 God of Things，不再独立作为 mod 加载。
 * 原项目：Torcherino（NinjaPhenix，MIT）。提供三种等级（普通/压缩/二重压缩）的
 * 加速火把、墙挂火把、加速灯笼与加速南瓜灯，可加速范围内方块的随机刻与方块实体 tick。
 */
public final class Torcherino
{
    public static final String MOD_ID = Godofthings.MODID;
    public static final Logger LOGGER = LogUtils.getLogger();

    // ---- 等级注册表与黑名单 ----
    private static final Map<ResourceLocation, Tier> TIERS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Tier> REMOTE_TIERS = new LinkedHashMap<>();
    private static final Set<Block> BLACKLISTED_BLOCKS = new HashSet<>();
    private static final Set<BlockEntityType<?>> BLACKLISTED_TILES = new HashSet<>();

    // ---- 方块实体 ----
    public static RegistryObject<BlockEntityType<TorcherinoBlockEntity>> BLOCK_ENTITY;

    // ---- 粒子 ----
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, MOD_ID);
    public static final RegistryObject<SimpleParticleType> PARTICLE_FLAME =
            PARTICLE_TYPES.register("torcherino_flame", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PARTICLE_COMPRESSED_FLAME =
            PARTICLE_TYPES.register("torcherino_compressed_flame", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> PARTICLE_DOUBLE_COMPRESSED_FLAME =
            PARTICLE_TYPES.register("torcherino_double_compressed_flame", () -> new SimpleParticleType(false));

    // ---- 内容收集（供方块实体类型懒构建 + 客户端 cutout 渲染注册） ----
    private static final List<RegistryObject<Block>> ALL_BLOCKS = new ArrayList<>();
    private static final List<RegistryObject<Block>> CUTOUT_BLOCKS = new ArrayList<>();
    private static final List<RegistryObject<Item>> ALL_ITEMS = new ArrayList<>();
    private static final Set<ResourceLocation> TO_BLACKLIST = new HashSet<>();

    private Torcherino() {}

    // ---- 等级注册表 API ----

    public static void registerTier(ResourceLocation name, int maxSpeed, int xzRange, int yRange)
    {
        if (TIERS.containsKey(name))
        {
            LOGGER.warn("[Torcherino] Tier with id {} has already been registered.", name);
            return;
        }
        TIERS.put(name, new Tier(maxSpeed, xzRange, yRange));
    }

    public static Map<ResourceLocation, Tier> getTiers()
    {
        return TIERS;
    }

    /** 客户端从服务端同步等级（S2CTierSyncMessage 调用）。 */
    public static void setRemoteTiers(Map<ResourceLocation, Tier> tiers)
    {
        REMOTE_TIERS.clear();
        REMOTE_TIERS.putAll(tiers);
    }

    public static Tier getTier(ResourceLocation name)
    {
        return REMOTE_TIERS.get(name);
    }

    // ---- 黑名单 API ----

    public static boolean blacklistBlock(ResourceLocation blockId)
    {
        Optional<Block> block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(blockId);
        if (block.isPresent())
        {
            return blacklistBlock(block.get());
        }
        LOGGER.warn("[Torcherino] Block with id {} does not exist.", blockId);
        return false;
    }

    public static boolean blacklistBlock(Block block)
    {
        if (BLACKLISTED_BLOCKS.contains(block))
        {
            return false;
        }
        BLACKLISTED_BLOCKS.add(block);
        return true;
    }

    public static boolean isBlockBlacklisted(Block block)
    {
        return BLACKLISTED_BLOCKS.contains(block);
    }

    public static boolean blacklistBlockEntity(ResourceLocation blockEntityTypeId)
    {
        Optional<BlockEntityType<?>> type =
                net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getOptional(blockEntityTypeId);
        if (type.isPresent())
        {
            return blacklistBlockEntity(type.get());
        }
        LOGGER.warn("[Torcherino] BlockEntityType with id {} does not exist.", blockEntityTypeId);
        return false;
    }

    public static boolean blacklistBlockEntity(BlockEntityType<?> blockEntityType)
    {
        if (BLACKLISTED_TILES.contains(blockEntityType))
        {
            return false;
        }
        BLACKLISTED_TILES.add(blockEntityType);
        return true;
    }

    public static boolean isBlockEntityBlacklisted(BlockEntityType<?> blockEntityType)
    {
        return BLACKLISTED_TILES.contains(blockEntityType);
    }

    // ---- 注册（由 Godofthings 主类构造时调用） ----

    public static void register(IEventBus modEventBus)
    {
        PARTICLE_TYPES.register(modEventBus);

        // 方块实体类型：懒构建，收集所有已注册的 torcherino 方块
        BLOCK_ENTITY = Godofthings.BLOCK_ENTITIES.register("torcherino",
                () -> BlockEntityType.Builder.of(TorcherinoBlockEntity::new,
                        ALL_BLOCKS.stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

        // 按等级注册方块 / 物品
        for (ResourceLocation tierID : TIERS.keySet())
        {
            registerTierContent(tierID);
        }
    }

    private static String getPath(ResourceLocation tierID, String type)
    {
        return (tierID.getPath().equals("normal") ? "" : tierID.getPath() + "_") + type;
    }

    private static void registerTierContent(ResourceLocation tierID)
    {
        if (!tierID.getNamespace().equals(MOD_ID))
        {
            return;
        }
        String torcherinoPath = getPath(tierID, "torcherino");

        TO_BLACKLIST.add(ResourceLocation.tryBuild(MOD_ID, torcherinoPath));
        TO_BLACKLIST.add(ResourceLocation.tryBuild(MOD_ID, "wall_" + torcherinoPath));

        RegistryObject<Block> standingBlock = Godofthings.BLOCKS.register(torcherinoPath,
                () -> new TorcherinoBlock(BlockBehaviour.Properties.copy(Blocks.TORCH).pushReaction(PushReaction.IGNORE), tierID));
        RegistryObject<Block> wallBlock = Godofthings.BLOCKS.register("wall_" + torcherinoPath,
                () -> new WallTorcherinoBlock(BlockBehaviour.Properties.copy(Blocks.WALL_TORCH)
                        .pushReaction(PushReaction.IGNORE).dropsLike(standingBlock.get()), tierID));

        ALL_BLOCKS.add(standingBlock);
        ALL_BLOCKS.add(wallBlock);

        RegistryObject<Item> torchItem = Godofthings.ITEMS.register(torcherinoPath,
                () -> new StandingAndWallBlockItem(standingBlock.get(), wallBlock.get(), new Item.Properties(), Direction.DOWN));
        ALL_ITEMS.add(torchItem);

        // 需要 cutout 渲染的方块（火把 / 墙挂火把）
        CUTOUT_BLOCKS.add(standingBlock);
        CUTOUT_BLOCKS.add(wallBlock);
    }

    /** 客户端注册 cutout 渲染层。 */
    public static List<RegistryObject<Block>> getCutoutBlocks()
    {
        return CUTOUT_BLOCKS;
    }

    /** 供创造标签展示的火把物品。 */
    public static List<RegistryObject<Item>> getItems()
    {
        return ALL_ITEMS;
    }

    /** 在 FMLCommonSetup 中调用：将火把自身及水/岩浆/空气加入加速黑名单，避免自加速与无效方块。 */
    public static void blacklistStuff()
    {
        for (ResourceLocation block : TO_BLACKLIST)
        {
            blacklistBlock(block);
        }
        blacklistBlock(Blocks.WATER);
        blacklistBlock(Blocks.LAVA);
        blacklistBlock(Blocks.AIR);
        blacklistBlock(Blocks.CAVE_AIR);
        blacklistBlock(Blocks.VOID_AIR);
    }
}
