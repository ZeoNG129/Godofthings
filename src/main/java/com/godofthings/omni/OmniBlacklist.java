package com.godofthings.omni;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 物品黑名单（原样移植自 Omni Upgrades 的 {@code CompatBlacklist}）：
 * 列入名单的物品不再被批量功能接管，右键拦截与高亮全部跳过。
 * 客户端个人名单 + 服务端全局名单取并集，客户端无法覆盖服务端名单。
 */
public final class OmniBlacklist {

    private static volatile Parsed local = Parsed.EMPTY;
    private static volatile Parsed serverGlobal = Parsed.EMPTY;
    private static volatile Parsed global = Parsed.EMPTY;
    private static final ConcurrentHashMap<UUID, Parsed> PLAYER = new ConcurrentHashMap<>();

    private OmniBlacklist() {
    }

    public static void setLocal(List<? extends String> raw) {
        local = Parsed.parse(raw);
    }

    public static void setGlobal(List<? extends String> raw) {
        global = Parsed.parse(raw);
    }

    public static void setServerGlobalForClient(List<? extends String> raw) {
        serverGlobal = Parsed.parse(raw);
    }

    public static void overwritePlayer(UUID playerId, List<? extends String> raw) {
        PLAYER.put(playerId, Parsed.parse(raw));
    }

    public static void removePlayer(UUID playerId) {
        PLAYER.remove(playerId);
    }

    public static List<String> getGlobalRaw() {
        return new ArrayList<>(global.raw);
    }

    public static List<String> getLocalRaw() {
        return new ArrayList<>(local.raw);
    }

    public static boolean isBlacklisted(boolean clientSide, Player player, ItemStack stack) {
        if (clientSide) {
            return local.matches(stack) || serverGlobal.matches(stack);
        }
        Parsed playerList = PLAYER.get(player.getUUID());
        return playerList != null && playerList.matches(stack) || global.matches(stack);
    }

    private static record Parsed(List<String> raw, Set<ResourceLocation> ids, Set<TagKey<Item>> tags) {
        static final Parsed EMPTY = new Parsed(List.of(), Set.of(), Set.of());

        static Parsed parse(List<? extends String> source) {
            if (source == null || source.isEmpty()) {
                return EMPTY;
            }
            List<String> raw = new ArrayList<>();
            Set<ResourceLocation> ids = new HashSet<>();
            Set<TagKey<Item>> tags = new HashSet<>();
            for (String line : source) {
                String s = line == null ? "" : line.trim();
                if (s.isEmpty()) {
                    continue;
                }
                raw.add(s);
                try {
                    if (s.startsWith("#")) {
                        ResourceLocation tagId = ResourceLocation.tryParse(s.substring(1));
                        if (tagId != null) {
                            tags.add(TagKey.create(Registries.ITEM, tagId));
                        }
                    } else {
                        ResourceLocation itemId = ResourceLocation.tryParse(s);
                        if (itemId != null && BuiltInRegistries.ITEM.containsKey(itemId)) {
                            ids.add(itemId);
                        }
                    }
                } catch (RuntimeException ignored) {
                }
            }
            return new Parsed(List.copyOf(raw), Set.copyOf(ids), Set.copyOf(tags));
        }

        boolean matches(ItemStack stack) {
            if (ids.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
                return true;
            }
            for (TagKey<Item> tag : tags) {
                if (stack.is(tag)) {
                    return true;
                }
            }
            return false;
        }
    }
}
