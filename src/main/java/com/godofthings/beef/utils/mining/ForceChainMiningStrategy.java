package com.godofthings.beef.utils.mining;

import net.minecraft.world.item.ItemStack;

/**
 * R键连锁破坏策略
 * 使用R键特殊的掉落逻辑进行连锁破坏
 */
public class ForceChainMiningStrategy extends ChainMiningStrategy {
    private final boolean enhanced;

    ForceChainMiningStrategy(boolean enhanced) {
        super(enhanced);
        this.enhanced = enhanced;
    }

    @Override
    protected boolean isForceMining(ItemStack hand) {
        return true;
    }

    @Override
    protected String getResultTranslationKey() {
        return this.enhanced
                ? "gui.godofthings.force_enhanced_chain_mining_result"
                : "gui.godofthings.force_chain_mining_result";
    }
}
