package su.nightexpress.excellentenchants.api.enchantment.component.impl;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Period;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.TimeUtil;

public class PeriodComponent implements EnchantComponent<Period> {

    @Override
    @NotNull
    public String getName() {
        return "periodic";
    }

    @Override
    @NotNull
    public Period read(@NotNull FileConfig config, @NotNull Period defaultValue) {
        if (config.contains("Period.Tick_Interval")) {
            long oldValue = config.getLong("Period.Tick_Interval");
            config.set("Period.Interval", (int) Math.max(1, TimeUtil.ticksToSeconds(oldValue)));
            config.remove("Period.Tick_Interval");
        }

        long interval = ConfigValue.create("Period.Interval",
                defaultValue.getInterval(),
                "仅当实体的存活时间（秒）能被该数值整除时才会触发。",
                "该值应大于主插件配置中的全局 'Tick_Interval'，并且必须能被全局 'Tick_Interval' 整除。"
        ).read(config);

        return new Period(interval);
    }
}
