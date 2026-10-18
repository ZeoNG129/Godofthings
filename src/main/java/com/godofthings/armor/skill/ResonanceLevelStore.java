package com.godofthings.armor.skill;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 「神之共鸣」用的技能等级快照（按主人 UUID 持久化）。
 *
 * <p>机器的共鸣判定不再要求主人在线、也不要求穿着神之套装，因此必须能在主人离线时
 * 拿到他的增幅等级 —— 玩家自己的技能数据挂在实体上，离线读不到，这里在每次看到
 * 主人在线时把等级表抄一份存进存档（{@code world/data/godofthings_resonance.dat}）。</p>
 *
 * <p>只存"等级"，开关的正负号照原样保留：共鸣开关的开关状态依赖正负号判定，
 * 机器读增幅倍率时才取绝对值（见 {@code ArmorSkillHandler}）。</p>
 */
public final class ResonanceLevelStore extends SavedData
{
    private static final String FILE_ID = "godofthings_resonance";

    private final Map<UUID, Map<String, Integer>> byOwner = new HashMap<>();

    public static ResonanceLevelStore get(MinecraftServer server)
    {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(ResonanceLevelStore::new, ResonanceLevelStore::load),
                FILE_ID);
    }

    /** 记一份主人当前的技能数据（内容没变则不动，避免每 tick 标脏） */
    public void remember(UUID owner, Map<String, Integer> levels)
    {
        Map<String, Integer> copy = new HashMap<>(levels);
        if (!copy.equals(byOwner.get(owner)))
        {
            byOwner.put(owner, copy);
            setDirty();
        }
    }

    /** 取该主人的快照；没有则 null */
    public Map<String, Integer> levelsOf(UUID owner)
    {
        return byOwner.get(owner);
    }

    public static ResonanceLevelStore load(CompoundTag tag, HolderLookup.Provider provider)
    {
        ResonanceLevelStore store = new ResonanceLevelStore();
        ListTag list = tag.getList("Owners", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++)
        {
            CompoundTag entry = list.getCompound(i);
            if (!entry.hasUUID("Id"))
            {
                continue;
            }
            Map<String, Integer> levels = new HashMap<>();
            CompoundTag skills = entry.getCompound("Skills");
            for (String key : skills.getAllKeys())
            {
                levels.put(key, skills.getInt(key));
            }
            store.byOwner.put(entry.getUUID("Id"), levels);
        }
        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider)
    {
        ListTag list = new ListTag();
        byOwner.forEach((owner, levels) ->
        {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", owner);
            CompoundTag skills = new CompoundTag();
            levels.forEach(skills::putInt);
            entry.put("Skills", skills);
            list.add(entry);
        });
        tag.put("Owners", list);
        return tag;
    }
}
