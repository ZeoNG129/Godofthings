package com.godofthings.construction;

import com.godofthings.Godofthings;
import com.godofthings.construction.client.ConstructionForceClient;
import com.godofthings.construction.client.GodConstructionScreen;
import com.godofthings.construction.network.ConstructionForcePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 神之建筑 / 天神建筑：建筑棒模组的「下界合金棒」精简移植（上游 ConstructionSticks，作者 Mrbysco，MIT License；
 * AE 部分参照 Applied Construction Sticks，作者 benbenlaw）。
 *
 * <p>本类服务<b>同族两个物品</b>：神之建筑（{@code godofthings:construction_min}，品阶
 * {@link ConstructionTier#GOD}）与天神建筑（{@code godofthings:construction_max}，品阶
 * {@link ConstructionTier#HEAVENLY}）。两者能力完全一样，只有四个上限不同 —— 上限<b>不再是类上的常量</b>，
 * 而是按物品的品阶（{@link #getTier(ItemStack)}）取值，见 {@link ConstructionTier}。
 * 「是不是本族物品」一律用 {@link #isWand(ItemStack)} 判断，不要写成
 * {@code instanceof GodConstructionItem} 或 {@code is(Godofthings.GOD_CONSTRUCTION.get())}
 * ——那样会把天神建筑漏掉。</p>
 *
 * <p>本物品<b>没有耐久</b>（不毁，永久生效），只靠一个数据组件记模式，三种模式循环：</p>
 * <ul>
 *   <li><b>潜行 + 右键方块</b>：切换模式 0 → 1 → 2 → 0，行动栏提示当前模式。</li>
 *   <li><b>建造（0）</b>：右键方块 = 贴着被点面「平面扩展」铺方块（神之建筑最多
 *       {@value #MAX_BUILD_BLOCKS} 格，天神建筑无限；<b>副手拿着方块物品时铺的是副手那个方块</b>，
 *       否则铺被点的方块）；右键空气 = 天使放置（副手的方块，
 *       在眼睛沿视线方向前进 {@value #ANGEL_DISTANCE} 格处放 1 格）。</li>
 *   <li><b>销毁（1）</b>：右键方块 = 拆掉被点方块的连通块（神之建筑最多 {@value #MAX_DESTROY_BLOCKS} 格，
 *       天神建筑无限，都<b>不掉落</b>）。</li>
 *   <li><b>替换（2）</b>：右键方块 = 把被点方块的连通块换成副手拿的方块（神之建筑最多
 *       {@value #MAX_REPLACE_BLOCKS} 格，天神建筑无限），旧方块掉成物品。</li>
 *   <li><b>潜行 + 右键空气</b>：打开设置界面（模式 / 撤销 / 预览 / 相似方块开关，纯客户端 Screen）。</li>
 *   <li><b>Ctrl + 右键方块</b>：在本该打开界面的方块（箱子 / 熔炉 / 本模组机器）上强制施工，见下面那段。</li>
 * </ul>
 *
 * <p>材料（建造 / 替换 / 天使放置）<b>先扣副手</b>，再扣主背包 36 格 + 快捷栏，
 * 不够再去 AE 网络里抽（见 {@link ConstructionAeSource}，用物品绑定的无线访问点）。
 * 创造模式（{@code hasInfiniteMaterials}）不扣材料。</p>
 *
 * <p>建造时<b>副手拿着方块物品就用副手的方块</b>（照上游 {@code SupplierInventory#getSupply} 的副手优先语义）：
 * 放下去的是副手那个方块、材料也从副手开始扣；但「背后那一格必须是同种方块」这条判定<b>仍以被点方块为准</b>
 * —— 被点方块才是要贴着铺的表面。副手不是方块物品时一切照旧，用被点方块当材料与模板。</p>
 *
 * <p>右键方块一律走 {@link #onItemUseFirst}（在服务端它早于方块自己的交互跑）。<b>会开界面的方块默认不拦</b>
 * （箱子 / 熔炉 / 本模组机器，判定见 {@link #opensGui}），右键照常打开界面；想在这类方块上建造 / 销毁 / 替换
 * 就<b>按住 Ctrl 右键</b> —— 服务端读不到 Ctrl（原版只同步 Shift），所以由客户端判定后先发一条
 * {@code ConstructionForcePayload}：服务端记一个 {@link ConstructionForce} 的一次性标记并<b>当场发起作业</b>，
 * 紧随其后的那一发原版右键只把标记消费掉并返回 {@code SUCCESS}，用来挡住界面。
 * 这个顺序是有保证的：客户端先跑 {@code onItemUseFirst}（强制包在这里发出）、之后才把
 * {@code ServerboundUseItemOnPacket} 排进发送队列，两包又在服务端主线程按到达顺序处理；
 * 万一某处插队把界面真打开了，强制包处理时会顺手把那个菜单关掉兜底。
 * 改方块的动作为了权威性只在服务端执行，客户端只回报成功。
 * 批量动作本身是跨 tick 的作业（见 {@link ConstructionJob}），点一下只是「登记一个作业」。</p>
 */
public class GodConstructionItem extends Item
{
    /** 建造模式：平面扩展建造（右键方块） / 天使放置（右键空气） */
    public static final int MODE_BUILD = 0;
    /** 销毁模式：拆掉同种方块的连通块，不掉落 */
    public static final int MODE_DESTROY = 1;
    /** 替换模式：把同种方块的连通块换成副手方块 */
    public static final int MODE_REPLACE = 2;
    /** 模式总数（切换时 0 → 1 → 2 → 0） */
    public static final int MODE_COUNT = 3;

    /**
     * 神之建筑（{@link ConstructionTier#GOD}）一次最多铺多少格（对齐下界合金棒的 limit）。
     *
     * <p>这里留常量只是为了让外部老引用和 javadoc 的 {@code @value} 继续有效，
     * {@link ConstructionTier#GOD} 直接引用它，所以两边不可能改歪；真正生效的上限一律读
     * {@link #getTier(ItemStack)}。</p>
     */
    public static final int MAX_BUILD_BLOCKS = 1024;
    /** 神之建筑（{@link ConstructionTier#GOD}）一次最多拆多少格（对齐下界合金棒的 destruction） */
    public static final int MAX_DESTROY_BLOCKS = 81;
    /** 神之建筑（{@link ConstructionTier#GOD}）一次最多换多少格（对齐下界合金棒的 limit） */
    public static final int MAX_REPLACE_BLOCKS = 1024;

    /** 神之建筑的天使放置距离：从眼睛沿视线方向前进几格（上游默认 angel = 16 是「隔着空气补一格」的距离，这里按移植要求固定 2） */
    public static final int ANGEL_DISTANCE = 2;

    /** 本物品的品阶（上限全从这里读；由 {@code Godofthings} 注册时注入） */
    private final ConstructionTier tier;

    public GodConstructionItem(Properties properties, ConstructionTier tier)
    {
        super(properties);
        this.tier = tier;
    }

    /**
     * 这个物品栈是不是本族物品（神之建筑 / 天神建筑）。
     *
     * <p>所有「认不认这把棒子」的判断点都走它：物品类型判断、组件读写、AE 链接、GUI 只认手上物品、
     * 预览渲染条件、作业取消条件。</p>
     */
    public static boolean isWand(ItemStack stack)
    {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof GodConstructionItem;
    }

    /** 主手优先、其次副手；手上没有本族物品就返回空栈 */
    public static ItemStack findWand(Player player)
    {
        ItemStack main = player.getMainHandItem();
        if (isWand(main))
        {
            return main;
        }
        ItemStack off = player.getOffhandItem();
        return isWand(off) ? off : ItemStack.EMPTY;
    }

    /**
     * 物品栈的品阶（上限 / 天使距离都从它取）。
     *
     * <p>理论上调用前都已经用 {@link #isWand(ItemStack)} 挡过；万一传进来别的东西，
     * 退回 {@link ConstructionTier#GOD}（数值最小、最保守）。</p>
     */
    public static ConstructionTier getTier(ItemStack stack)
    {
        if (stack != null && stack.getItem() instanceof GodConstructionItem wand)
        {
            return wand.tier;
        }
        return ConstructionTier.GOD;
    }

    /** 本物品实例的品阶 */
    public ConstructionTier getTier()
    {
        return this.tier;
    }

    /** 当前模式；没写过组件就是建造模式 */
    public static int getMode(ItemStack stack)
    {
        return stack.getOrDefault(Godofthings.GOD_CONSTRUCTION_MODE.get(), MODE_BUILD);
    }

    /** 预览线框开关；没写过组件就是开（默认开） */
    public static boolean isPreviewEnabled(ItemStack stack)
    {
        return stack.getOrDefault(Godofthings.GOD_CONSTRUCTION_PREVIEW.get(), Boolean.TRUE);
    }

    /** 相似方块开关；没写过组件就是关（默认关） */
    public static boolean isSimilarEnabled(ItemStack stack)
    {
        return stack.getOrDefault(Godofthings.GOD_CONSTRUCTION_SIMILAR.get(), Boolean.FALSE);
    }

    /**
     * 右键方块统一从这里进。
     *
     * <p><b>为什么不用 {@code useOn}：</b>原版只有在方块自己「没吃掉这次右键」时才会调用
     * {@code Item#useOn}，而箱子 / 熔炉 / 本模组机器右键是 {@code useWithoutItem}（打开界面），
     * 一旦 {@code consumesAction} 就轮不到 {@code useOn} 了 —— 对着这些方块根本造不起来。
     * {@code onItemUseFirst} 在服务端早于方块自己的交互跑，所以能抢先；客户端里它同样是最先跑的，
     * 原版那发 {@code ServerboundUseItemOnPacket} 要等它返回之后才进发送队列
     * （{@code MultiPlayerGameMode#performUseItemOn} → {@code startPrediction}）。</p>
     *
     * <p>会开界面的方块（{@link #opensGui}）默认<b>不拦</b>：返回 {@code PASS}，让原版 / 方块自己
     * 正常打开界面。按住 Ctrl 才是强制施工，两端分工：</p>
     * <ul>
     *   <li><b>客户端</b>：Ctrl + 会开界面的方块 → 发 {@link ConstructionForcePayload}
     *       （服务端记一次性标记并当场发起作业），本地返回 {@code SUCCESS} 压住方块交互；
     *       不是 Ctrl → {@code PASS}。</li>
     *   <li><b>服务端</b>：有未过期的标记 → 消费掉并返回 {@code SUCCESS}（挡住界面，且<b>不重复施工</b>）；
     *       没有标记 → 会开界面的方块 {@code PASS}，其余照旧施工。</li>
     * </ul>
     *
     * <p>潜行仍然优先：潜行 + 右键方块 = 切模式，这个手势不受 Ctrl / 界面影响。</p>
     */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context)
    {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null)
        {
            return InteractionResult.PASS;
        }

        // 批量操作里一律用不可变坐标：后面 BFS 会到处 relative，可变 pos 被复用会串格
        BlockPos clickedPos = context.getClickedPos().immutable();
        // clickedFace = 玩家点中的那个面（也就是玩家面向的那一面），
        // 它对建造来说是「上方」，对被点方块来说是「外侧」；
        // face.getOpposite() 指回方块本体，是每一格的「背面」。
        Direction face = context.getClickedFace();
        // 这个方块右键会不会开界面（箱子 / 熔炉 / 本模组机器都走这条）
        boolean gui = opensGui(level, clickedPos, level.getBlockState(clickedPos));

        // 客户端：只有「会开界面 + 按住 Ctrl + 没潜行」才是强制施工，其余照旧压住方块交互 / 放行界面
        if (level.isClientSide)
        {
            if (gui && !player.isShiftKeyDown() && ConstructionForceClient.isForceModifierDown())
            {
                ConstructionForcePayload.send(clickedPos, face, getMode(stack));
                return InteractionResult.SUCCESS;
            }
            return gui ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }

        if (player.isShiftKeyDown())
        {
            cycleMode(player, stack);
            return InteractionResult.SUCCESS;
        }

        // 刚才那一下 Ctrl 点击：作业已经在强制包里发起过了，这一发只用来挡住界面
        if (ConstructionForce.consume(player))
        {
            return InteractionResult.SUCCESS;
        }

        // 会开界面的方块：不拦，让原版 / 方块自己打开界面
        if (gui)
        {
            return InteractionResult.PASS;
        }

        ConstructionActions.perform(player, stack, level, clickedPos, face, getMode(stack));
        return InteractionResult.SUCCESS;
    }

    /**
     * 这个方块右键会不会打开界面（箱子 / 熔炉 / 本模组所有机器都走这条）。
     *
     * <p>判定照原版 {@code ServerPlayerGameMode#useItemOn} 旁观者分支用的那一句
     * {@code state.getMenuProvider(level, pos) != null}；再加一条：方块是 {@link EntityBlock}、
     * 且它的方块实体自己就是 {@link MenuProvider}（有些机器只在方块实体上给 MenuProvider）。</p>
     */
    private static boolean opensGui(Level level, BlockPos pos, BlockState state)
    {
        if (state.getMenuProvider(level, pos) != null)
        {
            return true;
        }
        return state.getBlock() instanceof EntityBlock && level.getBlockEntity(pos) instanceof MenuProvider;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack wand = player.getItemInHand(hand);
        if (level.isClientSide)
        {
            // 潜行 + 右键空气 = 打开设置界面（纯客户端 Screen，服务端什么都不做）
            if (player.isShiftKeyDown())
            {
                GodConstructionScreen.open();
            }
            return InteractionResultHolder.success(wand);
        }

        // 右键空气只有建造模式有事可做：天使放置。
        // 潜行是「切模式 / 开界面」的手势，不在脚下顺手放方块。
        if (getMode(wand) == MODE_BUILD && !player.isShiftKeyDown())
        {
            ConstructionActions.angel(player, wand, level);
        }
        return InteractionResultHolder.success(wand);
    }

    /** 模式 0 → 1 → 2 → 0，并给一条行动栏提示 */
    private static void cycleMode(Player player, ItemStack wand)
    {
        int next = (getMode(wand) + 1) % MODE_COUNT;
        wand.set(Godofthings.GOD_CONSTRUCTION_MODE.get(), next);
        player.displayClientMessage(modeMessage(next), true);
    }

    /** 三个模式键分别写死字面量，方便 check-lang.ps1 静态扫描到 */
    private static Component modeMessage(int mode)
    {
        return switch (mode)
        {
            case MODE_DESTROY -> Component.translatable("message.godofthings.construction_min.mode_destroy");
            case MODE_REPLACE -> Component.translatable("message.godofthings.construction_min.mode_replace");
            default -> Component.translatable("message.godofthings.construction_min.mode_build");
        };
    }

    /**
     * 扣 1 个材料：<b>先扣副手</b>，再扣主背包 36 格 + 快捷栏，不够再走 AE 网络。
     *
     * <p>副手优先是照上游 {@code SupplierInventory#getSupply} 的语义：建造 / 替换 / 天使放置都吃副手那摞方块，
     * 副手扣完才动背包（副手的方块本来就是「这次要用的材料」，先扣它玩家才看得见消耗）。</p>
     *
     * @return true = 扣到了（或是创造模式）；false = 材料用完了，批量操作应当停下
     */
    public static boolean takeMaterial(Player player, ItemStack wand, ItemStack wanted)
    {
        if (wanted.isEmpty())
        {
            return false;
        }
        if (player.hasInfiniteMaterials())
        {
            return true; // 创造 / 旁观：不扣材料
        }

        Inventory inventory = player.getInventory();
        // 副手优先：副手扣完了再扣主背包，最后才是 AE 网络
        if (takeFromStacks(inventory.offhand, wanted) || takeFromStacks(inventory.items, wanted))
        {
            return true;
        }
        return player instanceof ServerPlayer serverPlayer
                && ConstructionAeSource.extract(serverPlayer, wand, wanted, 1) > 0;
    }

    /** 在这些槽位里找「同物品同组件」的栈，扣掉 1 个 */
    private static boolean takeFromStacks(List<ItemStack> stacks, ItemStack wanted)
    {
        for (ItemStack stack : stacks)
        {
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, wanted))
            {
                continue;
            }
            stack.shrink(1);
            return true;
        }
        return false;
    }

    /** 一次批量操作只在第一格播放一次放置音效 */
    public static void playPlaceSound(Level level, BlockPos pos)
    {
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
