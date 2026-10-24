package com.godofthings.mept;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.api.util.AEColor;
import appeng.core.definitions.AEParts;
import appeng.core.definitions.ColoredItemDefinition;
import appeng.me.helpers.PlayerSource;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.crafting.CraftAmountMenu;
import appeng.parts.PartPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * ME 线缆放置工具（照抄上游 ItemMECablePlacementTool）：沿直线/平面/分支模式放缆。
 * <p>
 * [本地改动] 能量检查与扣电调用已随「不消耗能源」要求删除。
 * 光谱的钥匙（key_of_spectrum）升级照抄保留：不装钥匙只能用福鲁伊克斯（透明）色，
 * 装了钥匙可选任意颜色（副手持染料时自动重染色，消耗染料）。
 */
public class ItemMECablePlacementTool extends BasePlacementToolItem
{
    /** 每轴允许的最大跨度（格）。照抄上游。 */
    public static final int MAX_AXIS_SPAN = 64;
    /** 单次放置的位置数硬上限。照抄上游。 */
    public static final int MAX_TOTAL_POSITIONS = 4096;

    /** 该位置能否放缆（空气或无中心缆的部件宿主）。照抄上游。 */
    public static boolean canPlaceCableAt(Level level, BlockPos pos)
    {
        var state = level.getBlockState(pos);

        if (state.isAir())
        {
            return true;
        }

        IPartHost host = PartHelper.getPartHost(level, pos);
        if (host != null)
        {
            return host.getPart(null) == null;
        }

        return false;
    }

    /** 取线缆放置的智能目标位置。照抄上游。 */
    public static BlockPos getSmartTargetPos(Level level, BlockPos clickedPos, Direction clickedFace)
    {
        BlockPos adjacentPos = clickedPos.relative(clickedFace);

        IPartHost host = PartHelper.getPartHost(level, clickedPos);
        if (host != null && host.getPart(null) == null)
        {
            if (host.getPart(clickedFace) != null)
            {
                if (canPlaceCableAt(level, adjacentPos))
                {
                    return adjacentPos;
                }
                return clickedPos;
            }

            if (host.getPart(clickedFace.getOpposite()) != null)
            {
                return clickedPos;
            }

            if (canPlaceCableAt(level, adjacentPos))
            {
                return adjacentPos;
            }
            return clickedPos;
        }

        if (canPlaceCableAt(level, adjacentPos))
        {
            return adjacentPos;
        }
        if (canPlaceCableAt(level, clickedPos))
        {
            return clickedPos;
        }
        return adjacentPos;
    }

    /** 放置模式。照抄上游。 */
    public enum PlacementMode
    {
        LINE,
        PLANE_FILL,
        PLANE_BRANCHING
    }

    /** 线缆类型。照抄上游。 */
    public enum CableType
    {
        GLASS(AEParts.GLASS_CABLE),
        COVERED(AEParts.COVERED_CABLE),
        SMART(AEParts.SMART_CABLE),
        DENSE_COVERED(AEParts.COVERED_DENSE_CABLE),
        DENSE_SMART(AEParts.SMART_DENSE_CABLE);

        private final ColoredItemDefinition<?> definition;

        CableType(ColoredItemDefinition<?> definition)
        {
            this.definition = definition;
        }

        public ItemStack getStack(AEColor color)
        {
            return definition.stack(color);
        }
    }

    public ItemMECablePlacementTool(Item.Properties props)
    {
        super(props);
    }

    /** 打开合成请求界面。照抄上游。 */
    private void openCraftingMenu(ServerPlayer player, ItemStack wand, AEKey whatToCraft, int amount)
    {
        int wandSlot = findInventorySlot(player, wand);
        if (wandSlot >= 0)
        {
            CraftAmountMenu.open(player, MenuLocators.forInventorySlot(wandSlot), whatToCraft, amount);
        }
        else if (player.getMainHandItem() == wand)
        {
            CraftAmountMenu.open(player, MenuLocators.forHand(player, InteractionHand.MAIN_HAND), whatToCraft, amount);
        }
        else if (player.getOffhandItem() == wand)
        {
            CraftAmountMenu.open(player, MenuLocators.forHand(player, InteractionHand.OFF_HAND), whatToCraft, amount);
        }
    }

    private int findInventorySlot(Player player, ItemStack itemStack)
    {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++)
        {
            if (inv.getItem(i) == itemStack)
            {
                return i;
            }
        }
        return -1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        // LINE 模式下已设点1时，右键空气按视线方向确认终点。照抄上游。
        PlacementMode mode = getMode(stack);
        BlockPos p1 = getPoint1(stack);

        if (mode == PlacementMode.LINE && p1 != null)
        {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
            {
                BlockPos endpoint = findLine(player, p1);
                if (endpoint != null)
                {
                    setPoint2(stack, endpoint);
                    player.displayClientMessage(Component.translatable(
                            "message.godofthings.mept.point2_set", endpoint.toShortString()), true);
                    boolean craftingTriggered = executePlacement(serverPlayer, stack, level, p1, endpoint);
                    if (!craftingTriggered)
                    {
                        setPoint1(stack, null);
                        setPoint2(stack, null);
                        int slot = hand == InteractionHand.OFF_HAND
                                ? Inventory.SLOT_OFFHAND
                                : player.getInventory().selected;
                        syncPointsToClient(serverPlayer, slot);
                    }
                    return InteractionResultHolder.success(stack);
                }
            }
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level.isClientSide)
        {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer))
        {
            return InteractionResult.PASS;
        }
        int slot = context.getHand() == InteractionHand.OFF_HAND
                ? Inventory.SLOT_OFFHAND
                : player.getInventory().selected;

        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos targetPos = getSmartTargetPos(level, clickedPos, face);

        PlacementMode mode = getMode(stack);
        BlockPos p1 = getPoint1(stack);
        BlockPos p2 = getPoint2(stack);

        if (mode == PlacementMode.PLANE_BRANCHING)
        {
            // 分支模式用 3 个点
            if (p1 == null)
            {
                setPoint1(stack, targetPos);
                player.displayClientMessage(Component.translatable(
                        "message.godofthings.mept.branch_point1_set", targetPos.toShortString()), true);
                syncPointsToClient(serverPlayer, slot);
            }
            else if (p2 == null)
            {
                setPoint2(stack, targetPos);
                player.displayClientMessage(Component.translatable(
                        "message.godofthings.mept.branch_point2_set", targetPos.toShortString()), true);
                syncPointsToClient(serverPlayer, slot);
            }
            else
            {
                setPoint3(stack, targetPos);
                player.displayClientMessage(Component.translatable(
                        "message.godofthings.mept.branch_point3_set", targetPos.toShortString()), true);
                boolean craftingTriggered = executeBranchPlacement(serverPlayer, stack, level, p1, p2, targetPos);
                if (!craftingTriggered)
                {
                    setPoint1(stack, null);
                    setPoint2(stack, null);
                    setPoint3(stack, null);
                    syncPointsToClient(serverPlayer, slot);
                }
            }
        }
        else if (mode == PlacementMode.LINE)
        {
            if (p1 == null)
            {
                setPoint1(stack, targetPos);
                player.displayClientMessage(Component.translatable(
                        "message.godofthings.mept.point1_set", targetPos.toShortString()), true);
                syncPointsToClient(serverPlayer, slot);
            }
            else
            {
                BlockPos endpoint = findLine(player, p1);
                boolean craftingTriggered;
                if (endpoint != null)
                {
                    setPoint2(stack, endpoint);
                    player.displayClientMessage(Component.translatable(
                            "message.godofthings.mept.point2_set", endpoint.toShortString()), true);
                    craftingTriggered = executePlacement(serverPlayer, stack, level, p1, endpoint);
                }
                else
                {
                    setPoint2(stack, targetPos);
                    player.displayClientMessage(Component.translatable(
                            "message.godofthings.mept.point2_set", targetPos.toShortString()), true);
                    craftingTriggered = executePlacement(serverPlayer, stack, level, p1, targetPos);
                }
                if (!craftingTriggered)
                {
                    setPoint1(stack, null);
                    setPoint2(stack, null);
                    syncPointsToClient(serverPlayer, slot);
                }
            }
        }
        else
        {
            // PLANE_FILL 用 2 个点
            if (p1 == null)
            {
                setPoint1(stack, targetPos);
                player.displayClientMessage(Component.translatable(
                        "message.godofthings.mept.point1_set", targetPos.toShortString()), true);
                syncPointsToClient(serverPlayer, slot);
            }
            else
            {
                setPoint2(stack, targetPos);
                player.displayClientMessage(Component.translatable(
                        "message.godofthings.mept.point2_set", targetPos.toShortString()), true);
                boolean craftingTriggered = executePlacement(serverPlayer, stack, level, p1, targetPos);
                if (!craftingTriggered)
                {
                    setPoint1(stack, null);
                    setPoint2(stack, null);
                    syncPointsToClient(serverPlayer, slot);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    /** 找网络里可用的线缆 key：同色优先，其次任意色。照抄上游。 */
    private AEItemKey findAvailableCableKey(MEStorage storage, PlayerSource src, CableType cableType,
            AEColor preferredColor)
    {
        ItemStack preferredStack = cableType.getStack(preferredColor);
        AEItemKey preferredKey = AEItemKey.of(preferredStack);
        if (storage.extract(preferredKey, 1, Actionable.SIMULATE, src) >= 1)
        {
            return preferredKey;
        }

        for (AEColor c : AEColor.values())
        {
            if (c == preferredColor)
            {
                continue;
            }
            ItemStack stack = cableType.getStack(c);
            AEItemKey key = AEItemKey.of(stack);
            if (storage.extract(key, 1, Actionable.SIMULATE, src) >= 1)
            {
                return key;
            }
        }

        return null;
    }

    /** 从线缆 key 反查颜色。照抄上游。 */
    private AEColor getColorFromCableKey(AEItemKey key, CableType cableType)
    {
        for (AEColor c : AEColor.values())
        {
            ItemStack stack = cableType.getStack(c);
            if (AEItemKey.of(stack).equals(key))
            {
                return c;
            }
        }
        return AEColor.TRANSPARENT;
    }

    /**
     * 执行线缆放置。
     *
     * @return 是否触发了合成（触发时应保留点位）
     */
    private boolean executePlacement(ServerPlayer player, ItemStack tool, Level level, BlockPos p1, BlockPos p2)
    {
        PlacementMode mode = getMode(tool);
        CableType cableType = getCableType(tool);

        ColorLogicResult colorLogic = determineColorLogic(player, tool);
        AEColor color = colorLogic.color;

        List<BlockPos> positions = calculatePositions(p1, p2, mode);
        if (positions.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.no_positions"), true);
            return false;
        }

        // [本地改动] 上游此处有电量检查，不消耗能源版本直接跳过

        IGrid grid = this.getLinkedGrid(tool, level, player);
        if (grid == null)
        {
            return false;
        }
        MEStorage storage = grid.getStorageService().getInventory();
        PlayerSource src = new PlayerSource(player);

        ItemStack placeCableStack = cableType.getStack(color);

        return runCablePlacement(player, tool, level, positions, cableType, color, colorLogic, placeCableStack,
                grid, storage, src);
    }

    /**
     * 直线/平面与分支模式共用的放置管线。
     *
     * @return 是否触发了合成（触发时应保留点位）
     */
    private boolean runCablePlacement(ServerPlayer player, ItemStack tool, Level level, List<BlockPos> positions,
            CableType cableType, AEColor color, ColorLogicResult colorLogic, ItemStack placeCableStack,
            IGrid grid, MEStorage storage, PlayerSource src)
    {
        List<BlockPos> validPositions = new ArrayList<>();
        for (BlockPos pos : positions)
        {
            if (canPlaceCableAt(level, pos)
                    && level.mayInteract(player, pos)
                    && player.mayUseItemAt(pos, Direction.UP, placeCableStack))
            {
                validPositions.add(pos);
            }
        }

        if (validPositions.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.no_positions"), true);
            return false;
        }

        int totalNeeded = validPositions.size();

        long totalAvailable = 0;
        for (AEColor c : AEColor.values())
        {
            ItemStack stack = cableType.getStack(c);
            AEItemKey key = AEItemKey.of(stack);
            totalAvailable += storage.extract(key, totalNeeded, Actionable.SIMULATE, src);
            if (totalAvailable >= totalNeeded)
            {
                break;
            }
        }

        // 不够就先触发合成
        if (totalAvailable < totalNeeded)
        {
            var fluixCableStack = cableType.getStack(AEColor.TRANSPARENT);
            var craftKey = AEItemKey.of(fluixCableStack);
            var craftingService = grid.getCraftingService();
            if (craftingService != null && craftKey != null && craftingService.isCraftable(craftKey))
            {
                int missingAmount = (int) (totalNeeded - totalAvailable);
                openCraftingMenu(player, tool, craftKey, missingAmount);
                return true;
            }
            player.displayClientMessage(Component.translatable("message.godofthings.mept.missing_cable",
                    placeCableStack.getHoverName()), true);
            return false;
        }

        int placedCount = 0;
        int dyeConsumed = 0;
        List<MeptUndoHistory.CablePlacementSnapshot> placedSnapshots = new ArrayList<>();

        for (BlockPos pos : validPositions)
        {
            AEItemKey keyToExtract = findAvailableCableKey(storage, src, cableType, color);
            if (keyToExtract == null)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.mept.missing_cable",
                        placeCableStack.getHoverName()), true);
                break;
            }

            AEColor extractedColor = getColorFromCableKey(keyToExtract, cableType);
            boolean needsDyeForThis = colorLogic.needsDye && (extractedColor != color);

            if (needsDyeForThis && color != AEColor.TRANSPARENT)
            {
                if ((dyeConsumed == 0 || placedCount % 8 == 0) && dyeConsumed < (placedCount / 8) + 1)
                {
                    if (!consumeDye(player, storage, src, color, 1))
                    {
                        player.displayClientMessage(Component.translatable("message.godofthings.mept.missing_dye",
                                1, DyeItem.byColor(color.dye).getDescription()), true);
                        break;
                    }
                    dyeConsumed++;
                }
            }

            // 先扣再放；创造模式不动网络
            boolean extracted = !player.isCreative();
            if (extracted && storage.extract(keyToExtract, 1, Actionable.MODULATE, src) < 1)
            {
                player.displayClientMessage(Component.translatable("message.godofthings.mept.missing_cable",
                        placeCableStack.getHoverName()), true);
                break;
            }
            if (placeCable(player, (ServerLevel) level, pos, placeCableStack))
            {
                placedCount++;
                placedSnapshots.add(new MeptUndoHistory.CablePlacementSnapshot(pos, cableType, keyToExtract));
            }
            else if (extracted)
            {
                storage.insert(keyToExtract, 1, Actionable.MODULATE, src);
            }
        }

        if (placedCount > 0)
        {
            player.displayClientMessage(Component.translatable(
                    "message.godofthings.mept.placed_count", placedCount), true);

            BlockPos soundPos = placedSnapshots.get(0).pos;
            var placedState = level.getBlockState(soundPos);
            var soundType = placedState.getSoundType(level, soundPos, player);
            level.playSound(null, soundPos, soundType.getPlaceSound(), SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);

            MeptRegistry.undoHistory().addCablePlacement(player, level, placedSnapshots);
        }
        return false;
    }

    private boolean placeCable(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack cableStack)
    {
        try
        {
            IPartItem<?> partItem = (IPartItem<?>) cableStack.getItem();
            if (PartPlacement.placePart(player, level, partItem, null, pos, Direction.UP) != null)
            {
                return true;
            }
        }
        catch (Exception e)
        {
            // 忽略
        }
        return false;
    }

    /**
     * 3 点分支放置。
     *
     * @return 是否触发了合成（触发时应保留点位）
     */
    private boolean executeBranchPlacement(ServerPlayer player, ItemStack tool, Level level,
            BlockPos p1, BlockPos p2, BlockPos p3)
    {
        CableType cableType = getCableType(tool);

        ColorLogicResult colorLogic = determineColorLogic(player, tool);
        AEColor color = colorLogic.color;

        List<BlockPos> positions = calculateBranchPositions(p1, p2, p3);
        if (positions.isEmpty())
        {
            player.displayClientMessage(Component.translatable("message.godofthings.mept.no_positions"), true);
            return false;
        }

        // [本地改动] 上游此处有电量检查，不消耗能源版本直接跳过

        IGrid grid = this.getLinkedGrid(tool, level, player);
        if (grid == null)
        {
            return false;
        }
        MEStorage storage = grid.getStorageService().getInventory();
        PlayerSource src = new PlayerSource(player);

        ItemStack placeCableStack = cableType.getStack(color);

        return runCablePlacement(player, tool, level, positions, cableType, color, colorLogic, placeCableStack,
                grid, storage, src);
    }

    /** 按模式计算放置位置。照抄上游。 */
    public static List<BlockPos> calculatePositions(BlockPos p1, BlockPos p2, PlacementMode mode)
    {
        List<BlockPos> list = new ArrayList<>();
        int x1 = p1.getX(), y1 = p1.getY(), z1 = p1.getZ();
        int x2 = p2.getX(), y2 = p2.getY(), z2 = p2.getZ();

        if (mode == PlacementMode.LINE)
        {
            if (axisSpanExceeded(p1, p2))
            {
                return list;
            }
            return getLineBlocks(x1, y1, z1, x2, y2, z2);
        }
        else if (mode == PlacementMode.PLANE_FILL)
        {
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
            if (maxX - minX > MAX_AXIS_SPAN || maxY - minY > MAX_AXIS_SPAN || maxZ - minZ > MAX_AXIS_SPAN)
            {
                return list;
            }
            long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
            if (volume > MAX_TOTAL_POSITIONS)
            {
                return list;
            }
            for (int x = minX; x <= maxX; x++)
            {
                for (int y = minY; y <= maxY; y++)
                {
                    for (int z = minZ; z <= maxZ; z++)
                    {
                        list.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return list;
    }

    /** 3 点分支位置计算。照抄上游。 */
    public static List<BlockPos> calculateBranchPositions(BlockPos p1, BlockPos p2, BlockPos p3)
    {
        List<BlockPos> list = new ArrayList<>();

        if (axisSpanExceeded(p1, p2) || axisSpanExceeded(p1, p3) || axisSpanExceeded(p2, p3))
        {
            return list;
        }

        int x1 = p1.getX(), y1 = p1.getY(), z1 = p1.getZ();
        int x2 = p2.getX(), y2 = p2.getY(), z2 = p2.getZ();
        int x3 = p3.getX(), y3 = p3.getY(), z3 = p3.getZ();

        int dx12 = x2 - x1, dy12 = y2 - y1, dz12 = z2 - z1;
        int absDx = Math.abs(dx12), absDy = Math.abs(dy12), absDz = Math.abs(dz12);

        int trunkAxis, trunkDir, interval;

        if (absDx >= absDy && absDx >= absDz)
        {
            trunkAxis = 0;
            trunkDir = dx12 == 0 ? 1 : Integer.signum(dx12);
            interval = Math.max(1, absDx);
        }
        else if (absDy >= absDx && absDy >= absDz)
        {
            trunkAxis = 1;
            trunkDir = dy12 == 0 ? 1 : Integer.signum(dy12);
            interval = Math.max(1, absDy);
        }
        else
        {
            trunkAxis = 2;
            trunkDir = dz12 == 0 ? 1 : Integer.signum(dz12);
            interval = Math.max(1, absDz);
        }

        int dx13 = x3 - x1, dy13 = y3 - y1, dz13 = z3 - z1;

        int branchAxis, branchDir, trunkLength, branchLength;

        if (trunkAxis == 0)
        {
            trunkLength = Math.abs(dx13);
            if (Math.abs(dy13) >= Math.abs(dz13))
            {
                branchAxis = 1;
                branchLength = Math.abs(dy13);
                branchDir = dy13 == 0 ? 1 : Integer.signum(dy13);
            }
            else
            {
                branchAxis = 2;
                branchLength = Math.abs(dz13);
                branchDir = dz13 == 0 ? 1 : Integer.signum(dz13);
            }
        }
        else if (trunkAxis == 1)
        {
            trunkLength = Math.abs(dy13);
            if (Math.abs(dx13) >= Math.abs(dz13))
            {
                branchAxis = 0;
                branchLength = Math.abs(dx13);
                branchDir = dx13 == 0 ? 1 : Integer.signum(dx13);
            }
            else
            {
                branchAxis = 2;
                branchLength = Math.abs(dz13);
                branchDir = dz13 == 0 ? 1 : Integer.signum(dz13);
            }
        }
        else
        {
            trunkLength = Math.abs(dz13);
            if (Math.abs(dx13) >= Math.abs(dy13))
            {
                branchAxis = 0;
                branchLength = Math.abs(dx13);
                branchDir = dx13 == 0 ? 1 : Integer.signum(dx13);
            }
            else
            {
                branchAxis = 1;
                branchLength = Math.abs(dy13);
                branchDir = dy13 == 0 ? 1 : Integer.signum(dy13);
            }
        }

        for (int t = 0; t <= trunkLength; t++)
        {
            int tx = x1, ty = y1, tz = z1;

            if (trunkAxis == 0)
            {
                tx = x1 + t * trunkDir;
            }
            else if (trunkAxis == 1)
            {
                ty = y1 + t * trunkDir;
            }
            else
            {
                tz = z1 + t * trunkDir;
            }

            list.add(new BlockPos(tx, ty, tz));

            if (t % interval == 0)
            {
                for (int b = 1; b <= branchLength; b++)
                {
                    int bx = tx, by = ty, bz = tz;

                    if (branchAxis == 0)
                    {
                        bx = tx + b * branchDir;
                    }
                    else if (branchAxis == 1)
                    {
                        by = ty + b * branchDir;
                    }
                    else
                    {
                        bz = tz + b * branchDir;
                    }

                    list.add(new BlockPos(bx, by, bz));
                }
            }
        }

        return list;
    }

    /** 放置完成后把点位状态同步到客户端。照抄上游。 */
    public static void syncPointsToClient(ServerPlayer player, int slot)
    {
        ItemStack stack = slot == Inventory.SLOT_OFFHAND
                ? player.getOffhandItem()
                : player.getInventory().getItem(slot);
        if (stack.getItem() != MeptItems.ME_CABLE_PLACEMENT_TOOL.get())
        {
            return;
        }

        BlockPos p1 = getPoint1(stack);
        BlockPos p2 = getPoint2(stack);
        BlockPos p3 = getPoint3(stack);

        PacketDistributor.sendToPlayer(player,
                com.godofthings.mept.network.SyncCableToolPointsPayload.create(slot, p1, p2, p3));
    }

    // ==================== 数据组件读写（照抄上游） ====================

    public static void setPoint1(ItemStack stack, @Nullable BlockPos pos)
    {
        CompoundTag tag = stack.getOrDefault(MeptDataComponents.CABLE_POINTS.get(), new CompoundTag());
        if (pos == null)
        {
            tag.remove("Point1");
        }
        else
        {
            tag.put("Point1", NbtUtils.writeBlockPos(pos));
        }
        stack.set(MeptDataComponents.CABLE_POINTS.get(), tag);
    }

    @Nullable
    public static BlockPos getPoint1(ItemStack stack)
    {
        CompoundTag tag = stack.get(MeptDataComponents.CABLE_POINTS.get());
        if (tag != null && tag.contains("Point1"))
        {
            return NbtUtils.readBlockPos(tag, "Point1").orElse(null);
        }
        return null;
    }

    public static void setPoint2(ItemStack stack, @Nullable BlockPos pos)
    {
        CompoundTag tag = stack.getOrDefault(MeptDataComponents.CABLE_POINTS.get(), new CompoundTag());
        if (pos == null)
        {
            tag.remove("Point2");
        }
        else
        {
            tag.put("Point2", NbtUtils.writeBlockPos(pos));
        }
        stack.set(MeptDataComponents.CABLE_POINTS.get(), tag);
    }

    @Nullable
    public static BlockPos getPoint2(ItemStack stack)
    {
        CompoundTag tag = stack.get(MeptDataComponents.CABLE_POINTS.get());
        if (tag != null && tag.contains("Point2"))
        {
            return NbtUtils.readBlockPos(tag, "Point2").orElse(null);
        }
        return null;
    }

    public static void setPoint3(ItemStack stack, @Nullable BlockPos pos)
    {
        CompoundTag tag = stack.getOrDefault(MeptDataComponents.CABLE_POINTS.get(), new CompoundTag());
        if (pos == null)
        {
            tag.remove("Point3");
        }
        else
        {
            tag.put("Point3", NbtUtils.writeBlockPos(pos));
        }
        stack.set(MeptDataComponents.CABLE_POINTS.get(), tag);
    }

    @Nullable
    public static BlockPos getPoint3(ItemStack stack)
    {
        CompoundTag tag = stack.get(MeptDataComponents.CABLE_POINTS.get());
        if (tag != null && tag.contains("Point3"))
        {
            return NbtUtils.readBlockPos(tag, "Point3").orElse(null);
        }
        return null;
    }

    public static void clearAllPoints(ItemStack stack)
    {
        stack.remove(MeptDataComponents.CABLE_POINTS.get());
    }

    public static PlacementMode getMode(ItemStack stack)
    {
        Integer mode = stack.get(MeptDataComponents.CABLE_TOOL_MODE.get());
        if (mode != null && mode >= 0 && mode < PlacementMode.values().length)
        {
            return PlacementMode.values()[mode];
        }
        return PlacementMode.LINE;
    }

    public static void setMode(ItemStack stack, PlacementMode mode)
    {
        stack.set(MeptDataComponents.CABLE_TOOL_MODE.get(), mode.ordinal());
        clearAllPoints(stack);
    }

    public static CableType getCableType(ItemStack stack)
    {
        Integer type = stack.get(MeptDataComponents.CABLE_TYPE.get());
        if (type != null && type >= 0 && type < CableType.values().length)
        {
            return CableType.values()[type];
        }
        return CableType.GLASS;
    }

    public static void setCableType(ItemStack stack, CableType type)
    {
        stack.set(MeptDataComponents.CABLE_TYPE.get(), type.ordinal());
    }

    public static AEColor getColor(ItemStack stack)
    {
        Integer color = stack.get(MeptDataComponents.CABLE_COLOR.get());
        if (color != null && color >= 0 && color < AEColor.values().length)
        {
            return AEColor.values()[color];
        }
        return AEColor.TRANSPARENT;
    }

    public static void setColor(ItemStack stack, AEColor color)
    {
        stack.set(MeptDataComponents.CABLE_COLOR.get(), color.ordinal());
    }

    public static boolean hasUpgrade(ItemStack stack)
    {
        Boolean has = stack.get(MeptDataComponents.HAS_UPGRADE.get());
        return has != null && has;
    }

    public static void setUpgrade(ItemStack stack, boolean has)
    {
        stack.set(MeptDataComponents.HAS_UPGRADE.get(), has);
    }

    public static int[] getColorShortcuts(ItemStack stack)
    {
        CompoundTag tag = stack.get(MeptDataComponents.COLOR_SHORTCUTS.get());
        if (tag != null && tag.contains("shortcuts"))
        {
            return tag.getIntArray("shortcuts");
        }
        return new int[]{-1, -1, -1, -1, -1};
    }

    public static void setColorShortcuts(ItemStack stack, int[] shortcuts)
    {
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("shortcuts", shortcuts);
        stack.set(MeptDataComponents.COLOR_SHORTCUTS.get(), tag);
    }

    // ==================== 颜色逻辑（照抄上游） ====================

    private static class ColorLogicResult
    {
        AEColor color;
        boolean needsDye;

        ColorLogicResult(AEColor color, boolean needsDye)
        {
            this.color = color;
            this.needsDye = needsDye;
        }
    }

    @Nullable
    private AEColor getDyeColorFromStack(ItemStack stack)
    {
        if (!stack.isEmpty() && stack.getItem() instanceof DyeItem dyeItem)
        {
            return AEColor.fromDye(dyeItem.getDyeColor());
        }
        return null;
    }

    private ColorLogicResult determineColorLogic(Player player, ItemStack tool)
    {
        ItemStack offhandStack = player.getItemInHand(InteractionHand.OFF_HAND);
        AEColor offhandDyeColor = getDyeColorFromStack(offhandStack);
        boolean hasUpgradeItem = hasUpgrade(tool);
        AEColor selectedColor = getColor(tool);

        if (offhandDyeColor != null)
        {
            if (hasUpgradeItem)
            {
                return new ColorLogicResult(offhandDyeColor, false);
            }
            else
            {
                return new ColorLogicResult(offhandDyeColor, true);
            }
        }
        else
        {
            if (hasUpgradeItem)
            {
                return new ColorLogicResult(selectedColor, false);
            }
            else
            {
                return new ColorLogicResult(AEColor.TRANSPARENT, false);
            }
        }
    }

    private boolean consumeDye(Player player, MEStorage storage, PlayerSource src, AEColor color, int amount)
    {
        if (amount <= 0 || color == AEColor.TRANSPARENT)
        {
            return true;
        }
        if (player.isCreative())
        {
            return true;
        }

        DyeItem dyeItem = (DyeItem) DyeItem.byColor(color.dye);
        AEItemKey dyeKey = AEItemKey.of(dyeItem);

        int remaining = amount;

        long takenFromAE = storage.extract(dyeKey, remaining, Actionable.MODULATE, src);
        remaining -= takenFromAE;
        if (remaining <= 0)
        {
            return true;
        }

        for (int i = 0; i < player.getInventory().items.size(); i++)
        {
            ItemStack slotStack = player.getInventory().items.get(i);
            if (getDyeColorFromStack(slotStack) == color)
            {
                int take = Math.min(remaining, slotStack.getCount());
                slotStack.shrink(take);
                remaining -= take;
                if (remaining <= 0)
                {
                    break;
                }
            }
        }
        if (remaining <= 0)
        {
            return true;
        }

        ItemStack offhand = player.getItemInHand(InteractionHand.OFF_HAND);
        if (getDyeColorFromStack(offhand) == color)
        {
            int take = Math.min(remaining, offhand.getCount());
            offhand.shrink(take);
            remaining -= take;
        }

        return remaining <= 0;
    }

    // ==================== 直线模式（照抄上游） ====================

    public static BlockPos findLine(Player player, BlockPos firstPos)
    {
        net.minecraft.world.phys.Vec3 look = getPlayerLookVec(player);
        net.minecraft.world.phys.Vec3 start = new net.minecraft.world.phys.Vec3(
                player.getX(), player.getY() + player.getEyeHeight(), player.getZ());

        List<LineCriteria> criteriaList = new ArrayList<>(3);

        double centerX = firstPos.getX() + 0.5;
        double centerY = firstPos.getY() + 0.5;
        double centerZ = firstPos.getZ() + 0.5;

        net.minecraft.world.phys.Vec3 xBound = findXBound(centerX, start, look);
        criteriaList.add(new LineCriteria(xBound, firstPos, start));

        net.minecraft.world.phys.Vec3 yBound = findYBound(centerY, start, look);
        criteriaList.add(new LineCriteria(yBound, firstPos, start));

        net.minecraft.world.phys.Vec3 zBound = findZBound(centerZ, start, look);
        criteriaList.add(new LineCriteria(zBound, firstPos, start));

        int reach = 64;
        criteriaList.removeIf(c -> !c.isValid(start, look, reach));

        if (criteriaList.isEmpty())
        {
            return findLineFallback(player, firstPos);
        }

        LineCriteria selected = criteriaList.get(0);
        if (criteriaList.size() > 1)
        {
            for (int i = 1; i < criteriaList.size(); i++)
            {
                LineCriteria criteria = criteriaList.get(i);
                if (criteria.distToLineSq < 2.0 && selected.distToLineSq < 2.0)
                {
                    if (criteria.distToPlayerSq < selected.distToPlayerSq)
                    {
                        selected = criteria;
                    }
                }
                else
                {
                    if (criteria.distToLineSq < selected.distToLineSq)
                    {
                        selected = criteria;
                    }
                }
            }
        }

        return BlockPos.containing(selected.lineBound);
    }

    private static boolean axisSpanExceeded(BlockPos a, BlockPos b)
    {
        return Math.abs(a.getX() - b.getX()) > MAX_AXIS_SPAN
                || Math.abs(a.getY() - b.getY()) > MAX_AXIS_SPAN
                || Math.abs(a.getZ() - b.getZ()) > MAX_AXIS_SPAN;
    }

    public static List<BlockPos> getLineBlocks(int x1, int y1, int z1, int x2, int y2, int z2)
    {
        List<BlockPos> list = new ArrayList<>();

        if (x1 != x2)
        {
            for (int x = x1; x1 < x2 ? x <= x2 : x >= x2; x += x1 < x2 ? 1 : -1)
            {
                list.add(new BlockPos(x, y1, z1));
            }
        }
        else if (y1 != y2)
        {
            for (int y = y1; y1 < y2 ? y <= y2 : y >= y2; y += y1 < y2 ? 1 : -1)
            {
                list.add(new BlockPos(x1, y, z1));
            }
        }
        else
        {
            for (int z = z1; z1 < z2 ? z <= z2 : z >= z2; z += z1 < z2 ? 1 : -1)
            {
                list.add(new BlockPos(x1, y1, z));
            }
        }

        return list;
    }

    private static net.minecraft.world.phys.Vec3 getPlayerLookVec(Player player)
    {
        net.minecraft.world.phys.Vec3 lookVec = player.getLookAngle();
        double x = lookVec.x, y = lookVec.y, z = lookVec.z;

        if (Math.abs(x) < 0.0001)
        {
            x = 0.0001;
        }
        if (Math.abs(x - 1.0) < 0.0001)
        {
            x = 0.9999;
        }
        if (Math.abs(x + 1.0) < 0.0001)
        {
            x = -0.9999;
        }

        if (Math.abs(y) < 0.0001)
        {
            y = 0.0001;
        }
        if (Math.abs(y - 1.0) < 0.0001)
        {
            y = 0.9999;
        }
        if (Math.abs(y + 1.0) < 0.0001)
        {
            y = -0.9999;
        }

        if (Math.abs(z) < 0.0001)
        {
            z = 0.0001;
        }
        if (Math.abs(z - 1.0) < 0.0001)
        {
            z = 0.9999;
        }
        if (Math.abs(z + 1.0) < 0.0001)
        {
            z = -0.9999;
        }

        return new net.minecraft.world.phys.Vec3(x, y, z);
    }

    /** 常规找线失败时的回退：沿视线主轴取点。照抄上游。 */
    private static BlockPos findLineFallback(Player player, BlockPos firstPos)
    {
        net.minecraft.world.phys.Vec3 look = player.getLookAngle();
        double ax = Math.abs(look.x);
        double ay = Math.abs(look.y);
        double az = Math.abs(look.z);

        int distance = 8;
        if (ax >= ay && ax >= az)
        {
            int dir = look.x > 0 ? 1 : -1;
            return new BlockPos(firstPos.getX() + dir * distance, firstPos.getY(), firstPos.getZ());
        }
        else if (ay >= ax && ay >= az)
        {
            int dir = look.y > 0 ? 1 : -1;
            return new BlockPos(firstPos.getX(), firstPos.getY() + dir * distance, firstPos.getZ());
        }
        else
        {
            int dir = look.z > 0 ? 1 : -1;
            return new BlockPos(firstPos.getX(), firstPos.getY(), firstPos.getZ() + dir * distance);
        }
    }

    private static net.minecraft.world.phys.Vec3 findXBound(double x, net.minecraft.world.phys.Vec3 start,
            net.minecraft.world.phys.Vec3 look)
    {
        double y = (x - start.x) / look.x * look.y + start.y;
        double z = (x - start.x) / look.x * look.z + start.z;
        return new net.minecraft.world.phys.Vec3(x, y, z);
    }

    private static net.minecraft.world.phys.Vec3 findYBound(double y, net.minecraft.world.phys.Vec3 start,
            net.minecraft.world.phys.Vec3 look)
    {
        double x = (y - start.y) / look.y * look.x + start.x;
        double z = (y - start.y) / look.y * look.z + start.z;
        return new net.minecraft.world.phys.Vec3(x, y, z);
    }

    private static net.minecraft.world.phys.Vec3 findZBound(double z, net.minecraft.world.phys.Vec3 start,
            net.minecraft.world.phys.Vec3 look)
    {
        double x = (z - start.z) / look.z * look.x + start.x;
        double y = (z - start.z) / look.z * look.y + start.y;
        return new net.minecraft.world.phys.Vec3(x, y, z);
    }

    private static class LineCriteria
    {
        net.minecraft.world.phys.Vec3 planeBound;
        net.minecraft.world.phys.Vec3 lineBound;
        double distToLineSq;
        double distToPlayerSq;

        LineCriteria(net.minecraft.world.phys.Vec3 planeBound, BlockPos firstPos, net.minecraft.world.phys.Vec3 start)
        {
            this.planeBound = planeBound;
            this.lineBound = toLongestLine(planeBound, firstPos);
            this.distToLineSq = this.lineBound.subtract(planeBound).lengthSqr();
            this.distToPlayerSq = planeBound.subtract(start).lengthSqr();
        }

        private net.minecraft.world.phys.Vec3 toLongestLine(net.minecraft.world.phys.Vec3 boundVec, BlockPos firstPos)
        {
            int bx = (int) Math.floor(boundVec.x);
            int by = (int) Math.floor(boundVec.y);
            int bz = (int) Math.floor(boundVec.z);

            int dx = Math.abs(bx - firstPos.getX());
            int dy = Math.abs(by - firstPos.getY());
            int dz = Math.abs(bz - firstPos.getZ());

            int longest = Math.max(dx, Math.max(dy, dz));
            if (longest == dx && dx > 0)
            {
                return new net.minecraft.world.phys.Vec3(bx, firstPos.getY(), firstPos.getZ());
            }
            if (longest == dy && dy > 0)
            {
                return new net.minecraft.world.phys.Vec3(firstPos.getX(), by, firstPos.getZ());
            }
            if (longest == dz && dz > 0)
            {
                return new net.minecraft.world.phys.Vec3(firstPos.getX(), firstPos.getY(), bz);
            }
            return new net.minecraft.world.phys.Vec3(firstPos.getX(), firstPos.getY(), firstPos.getZ());
        }

        boolean isValid(net.minecraft.world.phys.Vec3 start, net.minecraft.world.phys.Vec3 look, int reach)
        {
            if (planeBound.subtract(start).dot(look) <= 0)
            {
                return false;
            }
            if (distToPlayerSq > reach * reach)
            {
                return false;
            }
            return true;
        }
    }
}
