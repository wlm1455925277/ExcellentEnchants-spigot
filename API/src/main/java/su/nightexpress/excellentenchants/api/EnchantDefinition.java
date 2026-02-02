package su.nightexpress.excellentenchants.api;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsKeys;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.api.item.ItemSet;
import su.nightexpress.excellentenchants.api.item.ItemSetDefaults;
import su.nightexpress.excellentenchants.api.item.ItemSetRegistry;
import su.nightexpress.excellentenchants.api.wrapper.EnchantCost;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.BukkitThing;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.Randomizer;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;

import java.util.*;

public class EnchantDefinition implements Writeable {

    private static final int LEVEL_CAP  = 255;
    private static final int WEIGHT_CAP = 1024;

    private final String       displayName;
    private final List<String> description;
    private final int          weight;
    private final int          maxLevel;
    private final EnchantCost  minCost;
    private final EnchantCost  maxCost;
    private final int          anvilCost;
    private final ItemSet      primaryItemSet;
    private final ItemSet      supportedItemSet;
    private final Set<String>  exclusiveSet;

    public EnchantDefinition(@NotNull String displayName,
                             @NotNull List<String> description,
                             int weight,
                             int maxLevel,
                             EnchantCost minCost,
                             EnchantCost maxCost,
                             int anvilCost,
                             @NotNull ItemSet supportedItems,
                             @NotNull ItemSet primaryItems,
                             @NotNull Set<String> exclusiveSet) {
        this.displayName = displayName;
        this.description = description;
        this.weight = Math.clamp(weight, 1, WEIGHT_CAP);
        this.maxLevel = Math.clamp(maxLevel, 1, LEVEL_CAP);
        this.minCost = minCost;
        this.maxCost = maxCost;
        this.anvilCost = anvilCost;
        this.supportedItemSet = supportedItems;
        this.primaryItemSet = primaryItems;
        this.exclusiveSet = exclusiveSet;
    }

    @NotNull
    public static Builder builder(@NotNull String name, int maxLevel) {
        return new Builder(name, maxLevel);
    }

    @NotNull
    public static EnchantDefinition read(@NotNull FileConfig config, @NotNull String path, @NotNull ItemSetRegistry itemSetRegistry) throws IllegalStateException {
        String displayName = ConfigValue.create(path + ".DisplayName",
                "null",
                "附魔显示名称。",
                "https://docs.advntr.dev/minimessage/format.html#standard-tags",
                "[*] 仅允许使用一种颜色和装饰样式。",
                "[*] 修改后需要重启生效。"
        ).read(config);

        List<String> description = ConfigValue.create(path + ".Description",
                Collections.emptyList(),
                "附魔描述文本。",
                "[*] 修改后需要重启生效。"
        ).read(config);

        int weight = ConfigValue.create(path + ".Weight",
                5,
                "权重会影响在附魔台或战利品中获取该附魔的概率。",
                "取值范围：1 ~ " + WEIGHT_CAP + "（包含边界）。",
                "[*] 修改后需要重启生效。"
        ).read(config);

        int maxLevel = ConfigValue.create(path + ".MaxLevel",
                3,
                "该附魔的最高等级。",
                "取值范围：1 ~ " + LEVEL_CAP + "（包含边界）。",
                "[*] 修改后需要重启生效。"
        ).read(config);

        EnchantCost minCost = ConfigValue.create(path + ".MinCost", EnchantCost::read,
                new EnchantCost(0, 0),
                "该附魔所需的最低等级花费（经验等级）。",
                "机制说明：https://minecraft.wiki/w/Enchanting_mechanics#How_enchantments_are_chosen",
                "原版花费参考：https://minecraft.wiki/w/Enchanting/Levels",
                "[*] 修改后需要重启生效。"
        ).read(config);

        EnchantCost maxCost = ConfigValue.create(path + ".MaxCost", EnchantCost::read,
                new EnchantCost(0, 0),
                "该附魔所需的最高等级花费（经验等级）。",
                "机制说明：https://minecraft.wiki/w/Enchanting_mechanics#How_enchantments_are_chosen",
                "原版花费参考：https://minecraft.wiki/w/Enchanting/Levels",
                "[*] 修改后需要重启生效。"
        ).read(config);

        int anvilCost = ConfigValue.create(path + ".AnvilCost",
                1,
                "在铁砧上将该附魔应用到另一件物品的基础费用。使用附魔书添加时费用减半，并会再乘以附魔等级。",
                "[*] 修改后需要重启生效。"
        ).read(config);

        String supportedItemsId = ConfigValue.create(path + ".SupportedItems",
                "null",
                "该附魔可通过铁砧或 /enchant 命令应用到的物品集合。",
                EnchantsPlaceholders.WIKI_ITEM_SETS,
                "[*] 修改后需要重启生效。"
        ).read(config);

        String primaryItemsId = ConfigValue.create(path + ".PrimaryItems",
                "null",
                "该附魔会在附魔台中出现（可被随机到）的物品集合。",
                EnchantsPlaceholders.WIKI_ITEM_SETS,
                "[*] 修改后需要重启生效。"
        ).read(config);

        ItemSet supportedItems = itemSetRegistry.getByKey(supportedItemsId);
        if (supportedItems == null) throw new IllegalStateException("无效的 SupportedItems 物品集合");

        ItemSet primaryItems = itemSetRegistry.getByKey(primaryItemsId);
        if (primaryItems == null) throw new IllegalStateException("无效的 PrimaryItems 物品集合");

        Set<String> conflicts = ConfigValue.create(path + ".Exclusives",
                Collections.emptySet(),
                "与该附魔互斥（不兼容）的附魔列表。",
                "[*] 原版附魔必须按如下格式填写：'minecraft:enchant_name'。",
                "[*] ExcellentEnchants 附魔必须按如下格式填写：'%s.".formatted(EnchantsKeys.create("enchant_name")),
                "    如果禁用了自定义命名空间，请使用原版（minecraft）命名空间。",
                "[*] 修改后需要重启生效。"
        ).read(config);

        return new EnchantDefinition(displayName, description, weight, maxLevel, minCost, maxCost, anvilCost, supportedItems, primaryItems, conflicts);
    }

    @Override
    public void write(@NotNull FileConfig config, @NotNull String path) {
        config.set(path + ".DisplayName", this.displayName);
        config.set(path + ".Description", this.description);
        config.set(path + ".Weight", this.weight);
        config.set(path + ".MaxLevel", this.maxLevel);
        config.set(path + ".MinCost", this.minCost);
        config.set(path + ".MaxCost", this.maxCost);
        config.set(path + ".AnvilCost", this.anvilCost);
        config.set(path + ".SupportedItems", this.supportedItemSet.getId());
        config.set(path + ".PrimaryItems", this.primaryItemSet.getId());
        config.set(path + ".Exclusives", this.exclusiveSet);
    }

    @NotNull
    public String getDisplayName() {
        return this.displayName;
    }

    @NotNull
    public List<String> getDescription() {
        return this.description;
    }

    public int getWeight() {
        return this.weight;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    @NotNull
    public EnchantCost getMinCost() {
        return this.minCost;
    }

    @NotNull
    public EnchantCost getMaxCost() {
        return this.maxCost;
    }

    public int getAnvilCost() {
        return this.anvilCost;
    }

    @NotNull
    public ItemSet getSupportedItemSet() {
        return this.supportedItemSet;
    }

    @NotNull
    public ItemSet getPrimaryItemSet() {
        return this.primaryItemSet;
    }

    public boolean hasConflicts() {
        return this.exclusiveSet != null && !this.exclusiveSet.isEmpty();
    }

    @NotNull
    public Set<String> getExclusiveSet() {
        return this.exclusiveSet;
    }

    public static class Builder {

        private String       displayName;
        private List<String> description;
        private int          weight;
        private int          maxLevel;
        private EnchantCost  minCost;
        private EnchantCost  maxCost;
        private int          anvilCost;
        private ItemSet      primaryItemSet;
        private ItemSet      supportedItemSet;
        private Set<String>  exclusives;

        public Builder(@NotNull String displayName, int maxLevel) {
            this.displayName(displayName);
            this.description = new ArrayList<>();
            this.weight = 5;
            this.maxLevel = maxLevel;
            this.anvilCost = Randomizer.nextInt(8);
            this.exclusives = new HashSet<>();

            int costCap = Randomizer.nextInt(45, 65);
            int costPerLevel = (int) ((double) costCap / (double) maxLevel);

            int minCost = Randomizer.nextInt(5);
            int maxCost = maxLevel == 1 ? costPerLevel : Randomizer.nextInt(3, 7) + minCost;

            this.minCost = new EnchantCost(minCost, costPerLevel);
            this.maxCost = new EnchantCost(maxCost, costPerLevel);
        }

        @NotNull
        public EnchantDefinition build() {
            Objects.requireNonNull(this.supportedItemSet, "附魔必须设置 SupportedItems 物品集合");

            return new EnchantDefinition(
                    this.displayName,
                    this.description,
                    this.weight,
                    this.maxLevel,
                    this.minCost,
                    this.maxCost,
                    this.anvilCost,
                    this.supportedItemSet,
                    this.primaryItemSet == null ? this.supportedItemSet : this.primaryItemSet,
                    this.exclusives
            );
        }

        @NotNull
        public Builder displayName(@NotNull String displayName) {
            this.displayName = TagWrappers.COLOR.with("#279CF5").wrap(displayName);
            return this;
        }

        @NotNull
        public Builder description(@NotNull String... description) {
            return this.description(Lists.newList(description));
        }

        @NotNull
        public Builder description(@NotNull List<String> description) {
            this.description = Lists.modify(description, TagWrappers.GRAY::wrap);
            return this;
        }

        @NotNull
        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        @NotNull
        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        @NotNull
        public Builder cost(@NotNull EnchantCost minCost, @NotNull EnchantCost maxCost) {
            this.minCost = minCost;
            this.maxCost = maxCost;
            return this;
        }

        @NotNull
        public Builder anvilCost(int anvilCost) {
            this.anvilCost = anvilCost;
            return this;
        }

        @NotNull
        public Builder items(@NotNull ItemSetDefaults defaults) {
            this.primaryItemSet = defaults.getItemSet();
            this.supportedItemSet = defaults.getItemSet();
            return this;
        }

        @NotNull
        public Builder primaryItems(@NotNull ItemSetDefaults defaults) {
            this.primaryItemSet = defaults.getItemSet();
            return this;
        }

        @NotNull
        public Builder supportedItems(@NotNull ItemSetDefaults defaults) {
            this.supportedItemSet = defaults.getItemSet();
            return this;
        }

        @NotNull
        public Builder exclusives(@NotNull NamespacedKey... exclusives) {
            return this.exclusives(Lists.newSet(exclusives));
        }

        @NotNull
        public Builder exclusives(@NotNull Set<NamespacedKey> exclusives) {
            this.exclusives = Lists.modify(exclusives, BukkitThing::getAsString);
            return this;
        }
    }
}
