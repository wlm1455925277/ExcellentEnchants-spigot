package su.nightexpress.excellentenchants.enchantment.tool;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.type.MiningEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Lists;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TreefellerEnchant extends GameEnchantment implements MiningEnchant {

    /**
     * 相邻搜索方向：
     * - 上下东西南北
     * - 以及 4 个水平对角（NE/NW/SE/SW）
     *
     * 注意：这里没有包含“上层对角（UP_NORTH_EAST 等）”，
     *       所以树冠如果是斜着往上延伸的，连接判定会弱一点。
     */
    private static final BlockFace[] BLOCK_SIDES = {
            BlockFace.UP, BlockFace.DOWN,
            BlockFace.EAST,
            BlockFace.WEST,
            BlockFace.SOUTH,
            BlockFace.NORTH,
            BlockFace.NORTH_EAST, BlockFace.NORTH_WEST,
            BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST
    };

    /** 最大搜索数量（包括叶子），用于防止无限递归/卡服 */
    private Modifier blocksLimit;

    /** 潜行禁用开关 */
    private boolean  disableOnCrouch;

    public TreefellerEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {

        this.disableOnCrouch = ConfigValue.create("Treefeller.Disable_On_Crouch",
                true,
                "是否允许通过潜行（Shift）绕过砍树效果。",
                "true = 潜行时不触发；false = 潜行也照样触发。"
        ).read(config);

        this.blocksLimit = Modifier.load(config, "Treefeller.Block_Limit",
                Modifier.addictive(16).perLevel(8).capacity(180),
                "最大“搜索块数量”，用于寻找与当前木头相连的木头/叶子（包含叶子）。",
                "数值越大：连锁范围越大，但更耗时。",
                "注意：这里的 limit 统计包含叶子；但最终只会破坏 LOG（木头），不会破坏叶子。"
        );
    }

    /**
     * 取某个方块的“相邻方块”，并且只保留：木头或叶子
     */
    @NotNull
    private Set<Block> getRelatives(@NotNull Block block) {
        return Stream.of(BLOCK_SIDES)
                .map(block::getRelative)
                .filter(b -> isLogOrLeaves(b.getType()))
                .collect(Collectors.toSet());
    }

    /**
     * 从 source 开始，按“连通”递归/扩展，找出所有相连的木头和叶子（上限 limit）
     */
    @NotNull
    private Set<Block> getLogsAndLeaves(@NotNull Block source, int limit) {
        Set<Block> full = new HashSet<>();             // 已收集到的整体集合
        Set<Block> lookupBlocks = Lists.newSet(source); // 本轮要扩展的集合（初始只有 source）

        this.addConnections(full, lookupBlocks, limit);
        return full;
    }

    /**
     * 递归扩展连通集合：
     * - full：已收集到的所有木头/叶子
     * - lookupBlocks：本轮要扩展的节点集合
     * - limit：最大搜寻数量（防止爆栈/卡服）
     *
     * 返回值只是表示这次递归有没有继续深入（对外并没用到）。
     */
    private boolean addConnections(@NotNull Set<Block> full, @NotNull Set<Block> lookupBlocks, int limit) {
        // 到达上限：停止
        if (full.size() >= limit) return false;

        // 把本轮 lookupBlocks 加入 full；
        // 如果一个都没新增（全都已存在），说明没有新连通点 -> 停止
        if (!full.addAll(lookupBlocks)) return false;

        Set<Block> connected = new HashSet<>();

        lookupBlocks.forEach(lookup -> {
            Set<Block> connections = this.getRelatives(lookup);

            /*
             * 重要规则：叶子不继续“叶子 -> 叶子”扩展
             * 也就是说：
             * - 木头：可以扩展到木头 + 叶子
             * - 叶子：只能扩展到木头（因为会删掉叶子的相邻叶子）
             *
             * 目的：防止树叶区域无限扩散到整片森林/玩家造的叶子建筑。
             */
            if (isLeaves(lookup.getType())) {
                connections.removeIf(connect -> isLeaves(connect.getType()));
            }

            connected.addAll(connections);
        });

        // 继续递归扩展
        return this.addConnections(full, connected, limit);
    }

    private static boolean isLogOrLeaves(@NotNull Material material) {
        return isLog(material) || isLeaves(material);
    }

    /** Tag.LOGS 覆盖所有“木头类”（包括原木、去皮原木、菌柄等，取决于版本 Tag 定义） */
    private static boolean isLog(@NotNull Material material) {
        return Tag.LOGS.isTagged(material);
    }

    /** Tag.LEAVES 覆盖所有“树叶类” */
    private static boolean isLeaves(@NotNull Material material) {
        return Tag.LEAVES.isTagged(material);
    }

    /**
     * 真正执行“砍树”：
     * - 先搜连通：木头+叶子（用来判断树的整体连通结构）
     * - 但真正 break 的只有 LOG（木头）
     */
    private void chopTree(@NotNull Player player, @NotNull Block source, @NotNull ItemStack tool, int level) {
        Set<Block> logsToBreak = this.getLogsAndLeaves(source, this.blocksLimit.getIntValue(level));

        // 不重复破坏玩家手动挖的那个 source（避免二次处理/兼容事件原本的挖掘）
        logsToBreak.remove(source);

        for (Block log : logsToBreak) {
            // 工具如果在连锁过程中耐久耗尽导致“消失/损坏”，停止
            // 注意：这里用 amount<=0 作为“工具坏了”的判断，
            // 这依赖 safeBusyBreak 可能会把手上物品数量变动（或置空）。
            if (tool.getAmount() <= 0) break;

            // 只破坏木头，不破坏叶子
            if (!isLog(log.getType())) continue;

            // 安全破坏：一般会处理忙碌标记、保护插件兼容、触发对应事件等
            EnchantsUtils.safeBusyBreak(player, log);
        }
    }

    @Override
    @NotNull
    public EnchantPriority getBreakPriority() {
        return EnchantPriority.LOWEST;
    }

    @Override
    public boolean onBreak(@NotNull BlockBreakEvent event, @NotNull LivingEntity entity, @NotNull ItemStack tool, int level) {
        if (!(entity instanceof Player player)) return false;

        // 全局忙碌标记：避免递归连锁时重复触发其它附魔/逻辑
        if (EnchantsUtils.isBusy()) return false;

        // 潜行禁用：Shift 挖木头就不触发
        if (this.disableOnCrouch && player.isSneaking()) return false;

        Block block = event.getBlock();

        // 只有挖“木头类”方块才触发
        if (!isLog(block.getType())) return false;

        this.chopTree(player, block, tool, level);
        return true;
    }
}
