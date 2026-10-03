package com.godofthings.beef.event;

import com.godofthings.beef.UselessMod;
import com.godofthings.beef.world.dimension.UselessDimensionConfigManager;
import com.godofthings.beef.world.dimension.UselessDimensions;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 照抄自 useless_mod 的服务端事件接线；如今只剩「无用维度」一块。
 *
 * <p><b>死代码清理历程</b>：</p>
 * <ol>
 *   <li>牛排工具框架（{@code EndlessBeafItem} 及其挖掘 / 磁力 / 强制击杀 / 时运 / 连锁 /
 *       飞行 / 连点等模式）已整套删除 —— 那些事件处理器全部以「手持牛排工具」为前提，
 *       而本模组早已没有任何该类物品，永远不可能触发；</li>
 *   <li><b>AE 无线接入点连接子系统</b>已删除（{@code AeDeviceLinker} /
 *       {@code AeLinkChannelBypass} / {@code AeConnectLinkSavedData} / 连接预览包 /
 *       客户端高亮渲染器），随之一并移除的是「Shift + 右键无线访问点绑定目标」
 *       （{@code onBlockInteract}）、AE 连接模式右键接管（{@code onAeConnectInteract}）、
 *       续命闹钟（{@code onServerTick} → {@code AeDeviceLinker.ensureLinks}）与
 *       停机清理（{@code onServerStopped} → {@code AeLinkChannelBypass.clear}）；</li>
 *   <li><b>玩家保护层（无敌 / 高级隐身）</b>已删除：其唯一的触发源是「背包里带着
 *       带本模组数据组件的 omnitools 扳手」，而本模组既没有任何物品会带上那两个组件，
 *       也不再有 {@code BeefInvulnerability*} 包 / 客户端名单 / 8 个保护 Mixin，
 *       因此整层事件处理器（伤害、死亡、仇恨、药水、攻击、玩家 tick、登录/重生/换维度/
 *       开始追踪）全部移除。</li>
 * </ol>
 */
@EventBusSubscriber(modid = UselessMod.MODID)
public class EventHandler {

    /**
     * 无用维度载入时把玩家/数据包配置好的地形设置灌进区块生成器。
     * <p>照抄自上游 EventHandler 的同名方法（此前裁掉，随无用维度子系统一同补回）。</p>
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level
                && UselessDimensions.isUselessDimension(level.dimension())) {
            UselessDimensionConfigManager.apply(level);
        }
    }

    /**
     * 服务器启动时构建配方索引
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // 无用维度：开服时把 3 个维度已保存的地形配置全部应用一遍
        UselessDimensionConfigManager.applyAll(event.getServer());
    }
}
