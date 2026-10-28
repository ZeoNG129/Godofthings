package com.godofthings.doll.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Container/command validation only; ordinary entity serialization stays intact. */
public final class TwoICaptureProtection {
    private TwoICaptureProtection() { }

    public static boolean containsDoll(CompoundTag tag) {
        if (tag == null) return false;
        if ("godofthings:mugi".equals(tag.getString("id"))) return true;
        for (Tag passenger : tag.getList("Passengers", Tag.TAG_COMPOUND)) {
            if (containsDoll((CompoundTag) passenger)) return true;
        }
        return false;
    }
}
