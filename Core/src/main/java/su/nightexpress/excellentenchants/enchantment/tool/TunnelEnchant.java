package su.nightexpress.excellentenchants.enchantment.tool;

import org.bukkit.Material;
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
import su.nightexpress.excellentenchants.api.enchantment.type.MiningEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Lists;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TunnelEnchant extends GameEnchantment implements MiningEnchant {

    /**
     * 3x3 的“相对偏移模板”（X, Z）
     * 顺序很重要：你后面 blocksBroken = 2/5/9 是按这里的前 N 个来挖。
     *
     * 0: (0,0)   中心
     * 1: (0,-1)  上（Z-）
     * 2: (-1,0)  左（X-）
     * 3: (0,1)   下（Z+）
     * 4: (1,0)   右（X+）
     * 5~8: 四个对角
     */
    private static final int[][] MINING_COORD_OFFSETS = new int[][]{
            {0, 0},
            {0, -1},
            {-1, 0},
            {0, 1},
            {1, 0},
            {-1, -1},
            {-1, 1},
            {1, -1},
            {1, 1},
    };

    /**
     * “可交互方块”白名单补丁：
     * 因为大多数 interactable 方块（箱子、按钮、门等）不该被 AoE 自动挖掉；
     * 但红石矿在 Bukkit 里通常也被认为是 interactable（会发光/触发）。
     * 所以这里把红石矿加入白名单，允许被连锁挖。
     */
    private static final Set<Material> INTERACTABLE_BLOCKS = new HashSet<>();

    static {
        INTERACTABLE_BLOCKS.add(Material.REDSTONE_ORE);
        INTERACTABLE_BLOCKS.add(Material.DEEPSLATE_REDSTONE_ORE);
    }

    /** 潜行禁用开关 */
    private boolean disableOnSneak;

    public TunnelEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.disableOnSneak = ConfigValue.create("Tunnel.Disable_On_Crouch",
                true,
                "是否在潜行（Shift）时禁用隧道挖掘效果。",
                "true = 潜行不触发；false = 潜行也触发。"
        ).read(config);
    }

    @Override
    @NotNull
    public EnchantPriority getBreakPriority() {
        // 最低优先级：尽量让其它挖掘逻辑（掉落修改/方块替换）先处理
        return EnchantPriority.LOWEST;
    }

    @Override
    public boolean onBreak(@NotNull BlockBreakEvent event, @NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        // 只允许玩家触发
        if (!(entity instanceof Player player)) return false;

        // 防止递归/并发：连锁挖掘过程中通常会把 Busy 置为 true，避免二次触发其它附魔
        if (EnchantsUtils.isBusy()) return false;

        // 潜行绕过
        if (this.disableOnSneak && player.isSneaking()) return false;

        Block block = event.getBlock();

        // 交互方块保护：
        // 如果被挖的主方块是可交互（箱子、门等）并且不在白名单（红石矿），则不触发 AoE。
        if (block.getType().isInteractable() && !INTERACTABLE_BLOCKS.contains(block.getType())) return false;

        // 工具对这个方块没有掉落（例如挖不动、无效工具、保护方块等）则不触发 AoE
        if (block.getDrops(item).isEmpty()) return false;

        /**
         * 关键：用“视线目标方块”判断玩家正在朝哪个面挖，从而决定 AoE 的朝向/平面
         *
         * getLastTwoTargetBlocks(透明列表, 距离)
         * 返回：最后两个命中的方块（通常是 [相邻空气/液体, 实体方块] 或者 [空气, 方块]）
         * 你这里忽略了 AIR/WATER/LAVA，让射线穿过它们去找“真正的挡住视线的方块”。
         */
        final List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(
                Lists.newSet(Material.AIR, Material.WATER, Material.LAVA),
                10
        );

        // 必须要正好拿到两个方块，且“较远的那个”必须是遮挡（实体方块），否则不触发
        if (lastTwoTargetBlocks.size() != 2 || !lastTwoTargetBlocks.get(1).getType().isOccluding()) {
            return false;
        }

        final Block targetBlock = lastTwoTargetBlocks.get(1); // 射线命中的实体方块
        final Block adjacentBlock = lastTwoTargetBlocks.get(0); // 紧贴在前面的那格（空气/液体等）

        // 通过“target 与 adjacent 的相对关系”得到玩家挖掘的方向（面对哪个面）
        final BlockFace dir = targetBlock.getFace(adjacentBlock);

        // isZ：当 dir 是 EAST/WEST 时，说明玩家面对的是 X 方向的面，此时 AoE 平面需要旋转
        boolean isZ = dir == BlockFace.EAST || dir == BlockFace.WEST;

        /**
         * 根据等级决定挖几个格：
         * - 默认 1（但下面又写：level==1 -> 2, level==2 -> 5, level>=3 -> 9）
         * 实际效果：
         * - Tunnel 1：挖 2 格（中心 + 上面那格）
         * - Tunnel 2：挖 5 格（中心 + 十字）
         * - Tunnel 3+：挖 9 格（3x3）
         *
         * 注意：这里的“形状”由 MINING_COORD_OFFSETS 的顺序决定。
         */
        int blocksBroken = 1;
        if (level == 1) blocksBroken = 2;
        else if (level == 2) blocksBroken = 5;
        else if (level >= 3) blocksBroken = 9;

        // 逐个计算要额外挖的方块
        for (int i = 0; i < blocksBroken; i++) {
            // 工具被挖爆/变空气（某些实现会直接把手上物品置空）则停止
            if (item.getType().isAir()) break;

            int xAdd = MINING_COORD_OFFSETS[i][0];
            int zAdd = MINING_COORD_OFFSETS[i][1];

            Block blockAdd;

            /**
             * dir 是 UP/DOWN：表示玩家在“挖顶/挖脚下”，那么 AoE 直接在 XZ 平面扩展
             * 即：add(x, 0, z)
             */
            if (dir == BlockFace.UP || dir == BlockFace.DOWN) {
                blockAdd = block.getLocation().clone().add(xAdd, 0, zAdd).getBlock();
            }
            /**
             * 其它方向：玩家是水平挖墙面，此时 AoE 应该在“竖直平面”扩展（类似 3x3 隧道面）
             * 这里用 isZ 做了一次坐标旋转：
             * - 面向 EAST/WEST：把 xAdd 当作 Y 偏移，zAdd 当作 Z 偏移
             * - 面向 NORTH/SOUTH：把 xAdd 当作 X 偏移，zAdd 当作 Y 偏移
             *
             * 这样你挖墙面时，会挖出一个“竖着的 3x3 面”。
             */
            else {
                blockAdd = block.getLocation().clone().add(
                        isZ ? 0 : xAdd,   // X 偏移（E/W 时为 0）
                        zAdd,             // Y 偏移（用 zAdd 当高度）
                        isZ ? xAdd : 0    // Z 偏移（E/W 时用 xAdd，当左右扩展）
                ).getBlock();
            }

            // 跳过中心方块（因为它本来就被玩家手动挖了）
            if (blockAdd.equals(block)) continue;

            // 额外方块对该工具没有掉落 -> 不挖（兼容挖不动、方块保护、需要特定工具等）
            if (blockAdd.getDrops(item).isEmpty()) continue;

            // 液体不挖
            if (blockAdd.isLiquid()) continue;

            Material addType = blockAdd.getType();

            // 额外方块的保护规则：交互方块（非白名单）不挖
            if (addType.isInteractable() && !INTERACTABLE_BLOCKS.contains(addType)) continue;

            // 禁止破坏：基岩、末地门、末地门框架
            if (addType == Material.BEDROCK || addType == Material.END_PORTAL || addType == Material.END_PORTAL_FRAME) continue;

            /**
             * 黑曜石特殊规则：
             * - 如果 addType 是 OBSIDIAN，但它和“主方块类型”不一致，就不挖
             * 也就是：主方块挖的是黑曜石时（block.getType() 也是 OBSIDIAN），才允许 AoE 挖到黑曜石；
             * 否则避免 AoE 顺带挖到旁边黑曜石（比如地狱门框、装饰等）。
             */
            if (addType == Material.OBSIDIAN && addType != block.getType()) continue;

            // 安全连锁破坏（一般会处理 busy 标记、触发正确事件、尊重保护等）
            EnchantsUtils.safeBusyBreak(player, blockAdd);
        }

        return true;
    }
}
