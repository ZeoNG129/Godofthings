package com.godofthings.block;

/**
 * 天神附魔：神之附魔的升级版，选择附魔时等级默认最高。
 */
public class GodHeavenEnchantBlock extends GodEnchantBlock
{
    public GodHeavenEnchantBlock(Properties properties)
    {
        super(properties);
    }

    @Override
    public boolean isHeavenly()
    {
        return true;
    }
}
