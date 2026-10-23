package com.godofthings.doll.init;

import com.godofthings.Godofthings;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 玩偶音效。
 *
 * <p>原 mod 的 three voice 音效（{@code two_i_voice*}）已随剧情功能一并删除，
 * 只剩摸头音效（注册的 {@code two_i_pat}，3 个变体随机播放）。</p>
 *
 * <p>音频文件（3 个 ogg）是从原 mod 原样搬过来的。等以后换成自己的音效，
 * 只需替换 {@code assets/godofthings/sounds/} 下的 ogg，代码不用动。</p>
 */
public final class DollSounds
{
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, Godofthings.MODID);

    /** 摸头音效（3 个变体随机） */
    public static final DeferredHolder<SoundEvent, SoundEvent> TWO_I_PAT =
            SOUNDS.register("two_i_pat", SoundEvent::createVariableRangeEvent);

    private DollSounds()
    {
    }
}
