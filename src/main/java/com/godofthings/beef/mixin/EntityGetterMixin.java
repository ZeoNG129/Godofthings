package com.godofthings.beef.mixin;

import com.godofthings.beef.event.EventHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

/**
 * 把「高级隐身」的玩家从 {@link EntityGetter} 的玩家查询里过滤掉。
 *
 * <p><b>priority = 500 是刻意为之，不要改回默认值。</b>
 * useless_mod 自带一个与本类逐字相同的 {@code EntityGetterMixin}，同样是 {@code @Redirect}
 * 同一批 {@code EntityGetter#players()} 调用点。两个 {@code @Redirect} 打在同一个指令上时
 * Mixin 只能留一个：优先级高者胜；优先级相同时「先注册的先赢」。
 * 而 useless_mod 的 mixin 配置写的是 {@code injectors.defaultRequire = 1} ——
 * 它的重定向一旦被跳过就是<b>致命注入失败，直接崩在启动阶段</b>
 * （实测崩溃：{@code Critical injection failure: Redirector useless_mod$filterProtectedPlayers ... (0/1) succeeded}）。</p>
 *
 * <p>因此这里把优先级压到默认值 1000 以下：原版 useless_mod 在场时它的重定向必胜，
 * 本类被跳过（本模组的 mixin 配置是 {@code defaultRequire = 0}，跳过只记一条警告，不会崩）；
 * 原版不在场时本类照常生效。代价是「同时装 useless_mod 时，本模组那把造化杖少这一层过滤」。</p>
 */
@Mixin(value = EntityGetter.class, priority = 500)
public interface EntityGetterMixin {
    @Redirect(
            method = {
                    "getNearestPlayer(DDDDLjava/util/function/Predicate;)Lnet/minecraft/world/entity/player/Player;",
                    "getNearestPlayer(Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/entity/player/Player;",
                    "getNearestPlayer(Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;DDD)Lnet/minecraft/world/entity/player/Player;",
                    "getNearestPlayer(Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;DDD)Lnet/minecraft/world/entity/player/Player;",
                    "getNearbyPlayers(Lnet/minecraft/world/entity/ai/targeting/TargetingConditions;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;",
                    "hasNearbyAlivePlayer(DDDD)Z"
            },
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/EntityGetter;players()Ljava/util/List;"
            )
    )
    private static List<? extends Player> godofthings$filterProtectedPlayers(EntityGetter getter) {
        List<? extends Player> players = getter.players();
        if (!EventHandler.hasAnyBeefAdvancedStealthPlayers()) {
            return players;
        }

        List<Player> filtered = null;
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (EventHandler.hasBeefAdvancedStealthItem(player)) {
                if (filtered == null) {
                    filtered = new ArrayList<>(players.size() - 1);
                    filtered.addAll(players.subList(0, i));
                }
                continue;
            }

            if (filtered != null) {
                filtered.add(player);
            }
        }
        return filtered == null ? players : filtered;
    }
}
