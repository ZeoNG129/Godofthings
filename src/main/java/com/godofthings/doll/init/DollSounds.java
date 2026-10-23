package com.godofthings.doll.init;

import com.godofthings.Godofthings;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 玩偶音效。
 *
 * <p>原 mod 只有 {@code two_i_pat} 是注册过的 {@link SoundEvent}，另外三个
 * （{@code two_i_voice} / {@code two_i_voice_soft} / {@code two_i_voice_bright}）
 * 是直接按 id 播放的，没有走注册表 —— 这里保持原样，避免改变语音调度逻辑。</p>
 *
 * <p>音频文件（5 个 ogg）是从原 mod 原样搬过来的。等以后换成自己的音效，
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
