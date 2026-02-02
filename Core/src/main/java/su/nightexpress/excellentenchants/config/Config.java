package su.nightexpress.excellentenchants.config;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.util.bukkit.NightItem;

import static su.nightexpress.excellentenchants.EnchantsPlaceholders.URL_WIKI_ITEMS;
import static su.nightexpress.excellentenchants.EnchantsPlaceholders.WIKI_CHRAGES;

public class Config extends AbstractConfig {

    @Override
    public void load(@NotNull FileConfig config) {
        if (config.contains("Description.Enabled")) {
            config.set("Modules.EnchantTooltip", config.getBoolean("Description.Enabled"));
            config.remove("Description.Enabled");
        }

        super.load(config);
    }

    private final ConfigProperty<Boolean> featuresEnchantTooltip = this.addProperty(ConfigTypes.BOOLEAN, "Modules.EnchantTooltip",
            true,
            "当为 'true' 时，会在物品 Lore 中的附魔名称下方显示附魔描述。"
    );

    public static final ConfigValue<Boolean> CHARGES_ENABLED = ConfigValue.create("Charges.Enabled",
            false,
            "启用“充能/次数（Charges）”功能：" + WIKI_CHRAGES
    );

    public static final ConfigValue<Boolean> CHARGES_FUEL_IGNORE_META = ConfigValue.create("Charges.Fuel.Ignore_Meta",
            false,
            "控制在判断燃料物品是否有效时，是否忽略物品 Meta（例如：显示名称、Lore、模型数据等）。",
            "[默认值为 false]"
    );

    public static final ConfigValue<NightItem> CHARGES_FUEL_ITEM = ConfigValue.create("Charges.Fuel.Item",
            NightItem.fromType(Material.LAPIS_LAZULI),
            "用于在铁砧上为附魔充能的默认燃料物品。",
            "每个附魔也可以单独配置自己的自定义燃料物品。",
            URL_WIKI_ITEMS
    );

    public boolean isEnchantTooltipEnabled() {
        return this.featuresEnchantTooltip.get();
    }

    public static boolean isChargesEnabled() {
        return CHARGES_ENABLED.get();
    }
}
