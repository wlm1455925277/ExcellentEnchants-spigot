package su.nightexpress.excellentenchants.enchantment.tool;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.type.BlockDropEnchant;
import su.nightexpress.excellentenchants.api.enchantment.type.BlockEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.EntityUtil;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.PDCUtil;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SilkChestEnchant extends GameEnchantment implements BlockDropEnchant, BlockEnchant {

    private final NamespacedKey chestNameKey;

    private String       chestName;
    private List<String> chestLore;

    public SilkChestEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.chestNameKey = new NamespacedKey(plugin, "silkchest.original_name");
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.chestName = ConfigValue.create(
                "SilkChest.Name",
                EnchantsPlaceholders.GENERIC_NAME + " (" + EnchantsPlaceholders.GENERIC_AMOUNT + " items)",
                "箱子物品的显示名称（Display Name）。",
                "可使用占位符 '" + EnchantsPlaceholders.GENERIC_AMOUNT + "' 来表示箱内物品数量。"
        ).read(config);

        this.chestLore = ConfigValue.create(
                "SilkChest.Lore",
                new ArrayList<>(),
                "箱子物品的 Lore 描述。",
                "可使用占位符 '" + EnchantsPlaceholders.GENERIC_AMOUNT + "' 来表示箱内物品数量。"
        ).read(config);
    }

    @NotNull
    public ItemStack getSilkChest(@NotNull Chest originChest) {
        ItemStack chestStack = new ItemStack(originChest.getType());

        ItemUtil.editMeta(chestStack, BlockStateMeta.class, stateMeta -> {
            String originName = EntityUtil.getNameSerialized(originChest);

            Chest chestItem = (Chest) stateMeta.getBlockState();
            chestItem.getBlockInventory().setContents(originChest.getBlockInventory().getContents());
            chestItem.update(true);

            stateMeta.setBlockState(chestItem);

            PlaceholderContext placeholderContext = PlaceholderContext.builder()
                    .with(EnchantsPlaceholders.GENERIC_NAME, () -> EntityUtil.getNameSerialized(originChest))
                    .with(EnchantsPlaceholders.GENERIC_AMOUNT, () -> String.valueOf(
                            Stream.of(chestItem.getBlockInventory().getContents())
                                    .filter(i -> i != null && !i.getType().isAir())
                                    .count()
                    ))
                    .build();

            // 记录“原箱子”的自定义名字，放到物品的 PDC 里，便于放回去时还原
            PDCUtil.set(stateMeta, this.chestNameKey, originName);

            ItemUtil.setCustomName(stateMeta, placeholderContext.apply(this.chestName));
            ItemUtil.setLore(stateMeta, placeholderContext.apply(this.chestLore));
        });

        this.manager.setBlockEnchant(chestStack, this);

        return chestStack;
    }

    @Override
    @NotNull
    public EnchantPriority getDropPriority() {
        return EnchantPriority.LOW;
    }

    @Override
    public boolean canPlaceInContainers() {
        // 不允许把“丝绸箱子物品”再放进容器里（防止一些嵌套/复制类问题）
        return false;
    }

    @Override
    public boolean onDrop(@NotNull BlockDropItemEvent event, @NotNull LivingEntity player, @NotNull ItemStack itemStack, int level) {
        BlockState state = event.getBlockState();
        if (!(state instanceof Chest chest)) return false;

        Material blockType = state.getType();
        List<Item> drops = event.getItems();

        // 若掉落列表中没有“原箱子方块本体（数量=1）”，则忽略
        Item originalContainerItem = drops.stream()
                .filter(drop -> drop.getItemStack().getType() == blockType && drop.getItemStack().getAmount() == 1)
                .findFirst()
                .orElse(null);

        if (originalContainerItem == null) return false;
        if (drops.size() == 1) return false;

        // 移除原箱子方块掉落，避免 duplication（箱子本体和丝绸箱子同时掉）
        drops.remove(originalContainerItem);

        // 把“本来要掉出来的内容物”塞回箱子方块状态里（后续打包成物品）
        chest.getBlockInventory().addItem(
                drops.stream().map(Item::getItemStack).toList().toArray(new ItemStack[0])
        );

        // 清空掉落列表：箱子内容物不再作为散落物掉落
        drops.clear();

        // 将“带内容的箱子物品（丝绸箱子）”作为最终掉落
        EnchantsUtils.populateResource(event, this.getSilkChest(chest));

        // 清空方块状态中的箱子内容（防止被二次读取）
        chest.getBlockInventory().clear();

        return true;
    }

    @Override
    public void onPlace(@NotNull BlockPlaceEvent event, @NotNull Player player, @NotNull Block block, @NotNull ItemStack itemStack) {
        BlockState state = block.getState();
        if (!(state instanceof Chest chest)) return;

        // 从物品 PDC 里取出“原箱子名字”，放回到放置后的箱子方块上
        String name = PDCUtil.getString(itemStack, this.chestNameKey).orElse(null);

        EntityUtil.setCustomName(chest, name);
        chest.update(true);
    }
}
