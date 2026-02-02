package su.nightexpress.excellentenchants.api.enchantment.component.impl;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Charges;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.bukkit.NightItem;

public class ChargesComponent implements EnchantComponent<Charges> {

    @Override
    @NotNull
    public String getName() {
        return "charges";
    }

    @Override
    @NotNull
    public Charges read(@NotNull FileConfig config, @NotNull Charges defaultValue) {
        Modifier maxAmount = Modifier.load(config, "Charges.Max_Amount",
                defaultValue.getMaxAmount(),
                "该附魔可拥有的最大充能次数。"
        );

        int consumeAmount = ConfigValue.create("Charges.Consume_Amount",
                defaultValue.getConsumeAmount(),
                "控制该附魔每次触发时消耗的充能次数。"
        ).read(config);

        int rechargeAmount = ConfigValue.create("Charges.Recharge_Amount",
                defaultValue.getRechargeAmount(),
                "控制每消耗 1 个燃料物品时恢复的充能次数。"
        ).read(config);

        boolean customFuelEnabled = ConfigValue.create("Charges.CustomFuel.Enabled",
                defaultValue.isCustomFuelEnabled(),
                "控制该附魔是否使用自定义燃料物品进行充能。"
        ).read(config);

        NightItem customFuelItem = ConfigValue.create("Charges.CustomFuel.Item",
                defaultValue.getCustomFuelItem(),
                "自定义燃料物品。",
                EnchantsPlaceholders.URL_WIKI_ITEMS
        ).read(config);

        return new Charges(maxAmount, consumeAmount, rechargeAmount, customFuelEnabled, customFuelItem);
    }
}
