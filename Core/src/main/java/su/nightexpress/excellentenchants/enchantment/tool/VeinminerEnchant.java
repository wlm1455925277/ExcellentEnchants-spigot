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
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
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
import su.nightexpress.nightcore.util.BukkitThing;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VeinminerEnchant extends GameEnchantment implements MiningEnchant {

    /**
     * 连锁搜索方向：6 邻接（上下东西南北）
     * 注意：没有对角线，所以它是“面相连”判定，不是“角相连”。
     */
    private static final BlockFace[] AREA = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.EAST,
            BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH
    };

    /** 每级最多连锁挖多少块（Modifier：基础+每级增量+上限） */
    private Modifier      blocksLimit;

    /** 哪些方块会被连锁挖（矿物列表） */
    private Set<Material> affectedBlocks;

    /** 潜行禁用 */
    private boolean       disableOnCrouch;

    public VeinminerEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        // 潜行是否禁用
        this.disableOnCrouch = ConfigValue.create("Veinminer.Disable_On_Crouch",
                true,
                "潜行(Shift)时是否禁用连锁挖矿效果。"
        ).read(config);

        // 每级连锁挖矿最大数量：基础4，每级+1，最大16
        this.blocksLimit = Modifier.load(config, "Veinminer.Block_Limit",
                Modifier.addictive(4).perLevel(1).capacity(16),
                "单次最多连锁挖掉多少个方块。");

        /**
         * 受影响的矿物列表：
         * - 允许在配置里覆盖/自定义
         * - 默认值用 Bukkit Tag，把各类矿物（含深板岩版本）都塞进去
         * - 额外加入下界金矿、下界石英矿
         */
        this.affectedBlocks = ConfigValue.forSet("Veinminer.Block_List",
                BukkitThing::getMaterial,                         // 读取：字符串 -> Material
                (cfg, path, set) -> cfg.set(path, set.stream().map(Enum::name).toList()), // 写回：Material -> name
                () -> {
                    Set<Material> set = new HashSet<>();
                    set.addAll(Tag.COAL_ORES.getValues());
                    set.addAll(Tag.COPPER_ORES.getValues());
                    set.addAll(Tag.DIAMOND_ORES.getValues());
                    set.addAll(Tag.EMERALD_ORES.getValues());
                    set.addAll(Tag.GOLD_ORES.getValues());
                    set.addAll(Tag.IRON_ORES.getValues());
                    set.addAll(Tag.LAPIS_ORES.getValues());
                    set.addAll(Tag.REDSTONE_ORES.getValues());
                    set.add(Material.NETHER_GOLD_ORE);
                    set.add(Material.NETHER_QUARTZ_ORE);
                    return set;
                },
                "哪些方块会被连锁挖掉。"
        ).read(config);

        // %amount% 占位符：显示当前等级可连锁挖的数量上限
        this.addPlaceholder(EnchantsPlaceholders.GENERIC_AMOUNT, level -> String.valueOf(this.getBlocksLimit(level)));
    }

    @NotNull
    public Set<Material> getAffectedBlocks() {
        return this.affectedBlocks;
    }

    public int getBlocksLimit(int level) {
        return (int) this.blocksLimit.getValue(level);
    }

    /**
     * 取某个方块的 6 邻接中 “与它同类型” 的方块集合
     * 也就是：只连锁同一种矿（例如只挖钻石矿，不会把旁边石头也挖掉）
     */
    @NotNull
    private Set<Block> getNearby(@NotNull Block block) {
        return Stream.of(AREA).map(block::getRelative)
                .filter(blockAdded -> blockAdded.getType() == block.getType())
                .collect(Collectors.toSet());
    }

    /**
     * 连锁挖矿核心：
     * - 从 source 周围同类型矿开始做“波纹扩散/BFS”
     * - 不断把 prepare 扩展成更多相连矿，直到达到 limit 或没有新块
     */
    private void vein(@NotNull Player player, @NotNull Block source, int level) {
        Set<Block> ores = new HashSet<>();                      // 已收集到的矿集合
        Set<Block> prepare = new HashSet<>(this.getNearby(source)); // 本轮待扩展的矿

        /**
         * 实际限制：
         * - 你配置的 Block_Limit（默认最多16）
         * - 这里又硬加了一个 Math.min(..., 30) 的保险上限（TODO Limit）
         *   所以就算配置写很大，也不会超过 30。
         */
        int limit = Math.min(this.getBlocksLimit(level), 30);
        if (limit < 0) return; // 注意：这里写 <0，意味着 limit==0 也会继续跑，但最终不会挖到（见下面）

        /**
         * while 条件的意思：
         * - ores.addAll(prepare) : 把本轮待扩展加入总集合，并且必须有“新增”才继续
         * - ores.size() < limit  : 达到上限就停
         */
        while (ores.addAll(prepare) && ores.size() < limit) {
            Set<Block> nearby = new HashSet<>();
            prepare.forEach(prepared -> nearby.addAll(this.getNearby(prepared))); // 扩展下一层
            prepare.clear();
            prepare.addAll(nearby);
        }

        // 不处理玩家手动挖掉的那一块（避免重复）
        ores.remove(source);

        // 逐个安全挖掉（通常会处理 busy 标记/事件/保护等）
        ores.forEach(ore -> EnchantsUtils.safeBusyBreak(player, ore));
    }

    @Override
    @NotNull
    public EnchantPriority getBreakPriority() {
        // 最低优先级：尽量让其它附魔/掉落改动先处理
        return EnchantPriority.LOWEST;
    }

    @Override
    public boolean onBreak(@NotNull BlockBreakEvent event, @NotNull LivingEntity entity, @NotNull ItemStack tool, int level) {
        // 只允许玩家
        if (!(entity instanceof Player player)) return false;

        // 防止递归/并发触发
        if (EnchantsUtils.isBusy()) return false;

        // 潜行绕过
        if (this.disableOnCrouch && player.isSneaking()) return false;

        Block block = event.getBlock();

        // 如果用这个工具挖这个方块没有掉落：不触发（挖不动/保护/无效工具等）
        if (block.getDrops(tool, player).isEmpty()) return false;

        // 不是允许连锁的矿：不触发
        if (!this.getAffectedBlocks().contains(block.getType())) return false;

        // 开始连锁挖矿
        this.vein(player, block, level);
        return true;
    }
}
