package com.godofthings.beef.stretcher.init;

import com.godofthings.beef.stretcher.UselessStretcherMod;
import com.godofthings.beef.stretcher.content.item.WondrousStaffItem;
import com.godofthings.beef.stretcher.content.item.RangeReclaimerItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 荒辰移晷之杖相关物品注册。
 *
 * <p>逐字照抄 UselessStretcher（万象担架）扩展模组的 {@code init.ModItems}，只保留杖相关的两项：
 * <b>荒辰移晷之杖</b>本体与范围加速管理工具 {@code range_reclaimer} —— 后者是范围加速记录的唯一
 * 管理入口（上游同样无配方，仅 /give 获取）。裁掉的三项（{@code omniversal_myriad} 万象模具方块、
 * 万象担架本体、维度方块物品）属于本模组未移植的模具/维度子系统。</p>
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(UselessStretcherMod.MODID);

    public static final DeferredItem<WondrousStaffItem> WONDROUS_STAFF = ITEMS.register("wondrous_staff",
            WondrousStaffItem::new);

    /** No recipe: intended for server operators and normally obtained with /give. */
    public static final DeferredItem<RangeReclaimerItem> RANGE_RECLAIMER = ITEMS.register("range_reclaimer",
            () -> new RangeReclaimerItem(new Item.Properties()));

    private ModItems() {
    }
}
