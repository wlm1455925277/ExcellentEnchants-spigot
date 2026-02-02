package su.nightexpress.excellentenchants.enchantment.tool;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.InteractEnchant;
import su.nightexpress.excellentenchants.api.enchantment.type.MiningEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.sound.VanillaSound;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ReplanterEnchant extends GameEnchantment implements InteractEnchant, MiningEnchant {

    private boolean replantOnRightClick;
    private boolean replantOnPlantBreak;

    private static final Map<Material, Material> CROP_MAP = new HashMap<>();

    static {
        CROP_MAP.put(Material.WHEAT_SEEDS, Material.WHEAT);
        CROP_MAP.put(Material.BEETROOT_SEEDS, Material.BEETROOTS);
        CROP_MAP.put(Material.MELON_SEEDS, Material.MELON_STEM);
        CROP_MAP.put(Material.PUMPKIN_SEEDS, Material.PUMPKIN_STEM);
        CROP_MAP.put(Material.POTATO, Material.POTATOES);
        CROP_MAP.put(Material.CARROT, Material.CARROTS);
        CROP_MAP.put(Material.NETHER_WART, Material.NETHER_WART);
    }

    public ReplanterEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.addComponent(EnchantComponent.PROBABILITY, Probability.oneHundred());
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.replantOnRightClick = ConfigValue.create("Replanter.On_Right_Click",
                true,
                "为 true 时：玩家右键耕地/灵魂沙（下方方块）且上方为空时，自动从背包取出对应种子并种下。"
        ).read(config);

        this.replantOnPlantBreak = ConfigValue.create("Replanter.On_Plant_Break",
                true,
                "为 true 时：玩家使用带此附魔的工具破坏成熟作物后，会自动从背包消耗 1 个种子并补种（重置生长阶段）。"
        ).read(config);
    }

    public boolean isReplantOnPlantBreak() {
        return replantOnPlantBreak;
    }

    public boolean isReplantOnRightClick() {
        return replantOnRightClick;
    }

    private boolean takeSeeds(@NotNull Player player, @NotNull Material material) {
        int slot = player.getInventory().first(material);
        if (slot < 0) return false;

        ItemStack seed = player.getInventory().getItem(slot);
        if (seed == null || seed.getType().isAir()) return false;

        seed.setAmount(seed.getAmount() - 1);
        return true;
    }

    @NotNull
    @Override
    public EnchantPriority getInteractPriority() {
        return EnchantPriority.HIGHEST;
    }

    @Override
    @NotNull
    public EnchantPriority getBreakPriority() {
        return EnchantPriority.NORMAL;
    }

    @Override
    public boolean onInteract(@NotNull PlayerInteractEvent event, @NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        if (!(entity instanceof Player player)) return false;
        if (!this.isReplantOnRightClick()) return false;

        // 只处理主手触发，避免同一交互被触发两次
        if (event.getHand() != EquipmentSlot.HAND) return false;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return false;

        // 若副手本来就拿着可种植的种子，则让原版“副手种植”优先生效，不触发本附魔
        ItemStack off = player.getInventory().getItemInOffHand();
        if (!off.getType().isAir() && CROP_MAP.containsKey(off.getType())) return false;

        // 只能对耕地/灵魂沙触发
        Block blockGround = event.getClickedBlock();
        if (blockGround == null) return false;
        if (blockGround.getType() != Material.FARMLAND && blockGround.getType() != Material.SOUL_SAND) return false;

        // 上方必须为空（没有作物/方块）
        Block blockPlant = blockGround.getRelative(BlockFace.UP);
        if (!blockPlant.isEmpty()) return false;

        // 从玩家背包里寻找可用种子并种植（耕地种普通作物，灵魂沙种下界疣）
        for (var entry : CROP_MAP.entrySet()) {
            Material seed = entry.getKey();
            if (seed == Material.NETHER_WART && blockGround.getType() == Material.SOUL_SAND
                    || seed != Material.NETHER_WART && blockGround.getType() == Material.FARMLAND) {
                if (this.takeSeeds(player, seed)) {
                    VanillaSound.of(seed == Material.NETHER_WART ? Sound.ITEM_NETHER_WART_PLANT : Sound.ITEM_CROP_PLANT).play(player);
                    player.swingMainHand();
                    blockPlant.setType(entry.getValue());
                    break;
                }
            }
        }
        return true;
    }

    @Override
    public boolean onBreak(@NotNull BlockBreakEvent event, @NotNull LivingEntity entity, @NotNull ItemStack item, int level) {
        if (!(entity instanceof Player player)) return false;
        if (!this.isReplantOnPlantBreak()) return false;

        Block blockPlant = event.getBlock();

        // 仅支持 CROP_MAP 中的作物类型
        if (!CROP_MAP.containsKey(blockPlant.getBlockData().getPlacementMaterial())) return false;

        // 必须是可生长的作物方块
        BlockData dataPlant = blockPlant.getBlockData();
        if (!(dataPlant instanceof Ageable plant)) return false;

        // 从背包消耗 1 个种子并补种（下 tick 执行，避免与掉落/破坏冲突）
        if (this.takeSeeds(player, dataPlant.getPlacementMaterial())) {
            plugin.runTask(() -> {
                blockPlant.setType(plant.getMaterial());
                plant.setAge(0);
                blockPlant.setBlockData(plant);
            });
        }
        return true;
    }
}
