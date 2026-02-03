package su.nightexpress.excellentenchants.enchantment.tool;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.BlockDropEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.bridge.wrap.NightSound;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.BukkitThing;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.LocationUtil;
import su.nightexpress.nightcore.util.sound.VanillaSound;
import su.nightexpress.nightcore.util.wrapper.UniParticle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmelterEnchant extends GameEnchantment implements BlockDropEnchant {

    private NightSound sound;
    private boolean    disableOnCrouch;

    /** 不受“熔炼”影响的方块/物品类型（免疫列表） */
    private final Set<Material>      exemptedItems;

    /** 服务器当前注册的熔炉配方（只缓存 FurnaceRecipe），并剔除免疫输入 */
    private final Set<FurnaceRecipe> recipes;

    public SmelterEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);

        // 触发概率：基础 15%，每级 +5%
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(15, 5));

        this.exemptedItems = new HashSet<>();
        this.recipes = new HashSet<>();
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {

        this.disableOnCrouch = ConfigValue.create("Smelter.Disable_On_Crouch",
                true,
                "是否在潜行（Shift）时禁用该附魔效果。",
                "true = 玩家潜行挖掘时不会触发熔炼；false = 潜行也照样熔炼。"
        ).read(config);

        this.sound = ConfigValue.create("Smelter.Sound",
                VanillaSound.of(Sound.BLOCK_LAVA_EXTINGUISH),
                "触发熔炼时播放的音效。"
        ).read(config);

        // 重新加载配置时清空缓存
        this.recipes.clear();
        this.exemptedItems.clear();

        this.exemptedItems.addAll(ConfigValue.forSet("Smelter.Exempted_Blocks",
                BukkitThing::getMaterial,
                (cfg, path, set) -> cfg.set(path, set.stream().map(BukkitThing::getAsString).toList()),
                Lists.newSet(Material.COBBLESTONE),
                "免疫列表：这些方块/物品不会被“熔炼”转换。",
                "例如默认 COBBLESTONE（圆石）不熔成 STONE。",
                "可填写方块/物品的 Bukkit Material 名称，如：COBBLESTONE, SAND, GOLD_ORE ..."
        ).read(config));

        // 扫描服务器已注册的配方，缓存所有 FurnaceRecipe（熔炉类配方）
        // 注意：这里只缓存 FurnaceRecipe；高炉/烟熏炉等如果底层不是 FurnaceRecipe 可能不会进入（取决于服务端实现）
        this.plugin.getServer().recipeIterator().forEachRemaining(recipe -> {
            if (!(recipe instanceof FurnaceRecipe furnaceRecipe)) return;

            // 如果该配方输入物品在免疫列表里，就跳过
            if (!this.exemptedItems.contains(furnaceRecipe.getInput().getType())) {
                this.recipes.add(furnaceRecipe);
            }
        });
    }

//    @Override
//    public void clear() {
//        this.recipes.clear();
//        this.exemptedItems.clear();
//    }

    @Override
    @NotNull
    public EnchantPriority getDropPriority() {
        return EnchantPriority.LOW;
    }

    @Override
    public boolean onDrop(@NotNull BlockDropItemEvent event, @NotNull LivingEntity entity, @NotNull ItemStack item, int level) {

        // 潜行禁用：Shift 挖矿时不触发
        if (this.disableOnCrouch && entity instanceof Player player && player.isSneaking()) return false;

        BlockState state = event.getBlockState();

        // 容器方块不处理（箱子/熔炉/漏斗等），避免把容器破坏掉落内容给“熔炼”导致奇怪问题/刷物
        if (state instanceof Container) return false;

        List<ItemStack> smelts = new ArrayList<>();

        /*
         * 核心逻辑：
         * - 遍历此次事件中“将要掉落的掉落物实体”
         * - 若掉落物匹配某个熔炉配方的输入，则把它替换为配方产物（并保留数量）
         *
         * 注意：这里每个掉落物会通过 stream 查找第一个匹配的 FurnaceRecipe。
         *       配方很多时性能一般但可接受；如果你后续要极限优化可做 Map<Material, FurnaceRecipe> 预索引。
         */
        event.getItems().removeIf(drop -> {
            FurnaceRecipe recipe = this.recipes.stream()
                    .filter(rec -> rec.getInputChoice().test(drop.getItemStack()))
                    .findFirst().orElse(null);

            if (recipe == null) return false;

            // 保留原掉落物数量：兼容 Fortune（时运）等导致掉落数量变化的情况
            // 这里假设“一个输入 -> 一个输出”，未来如果配方支持多输入数量，这里就不严谨了（作者也在注释里吐槽了）
            int amount = drop.getItemStack().getAmount();
            ItemStack result = new ItemStack(recipe.getResult());
            result.setAmount(amount);

            smelts.add(result);

            // 从 event 掉落列表中移除原掉落物实体（相当于“替换”）
            return true;
        });

        if (smelts.isEmpty()) return false;

        // 把熔炼后的产物重新塞回掉落事件（尊重保护插件/掉落归属等）
        smelts.forEach(itemStack -> EnchantsUtils.populateResource(event, itemStack));

        // 视觉/音效
        Block block = state.getBlock();
        if (this.hasVisualEffects()) {
            Location location = LocationUtil.setCenter3D(block.getLocation());
            UniParticle.of(Particle.FLAME).play(location, 0.25, 0.05, 4);
            this.sound.play(location);
        }
        return true;
    }
}
