package su.nightexpress.excellentenchants.api.wrapper;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;

public class EnchantCost implements Writeable {

    private final int base;
    private final int perLevel;

    public EnchantCost(int base, int perLevel) {
        this.base = base;
        this.perLevel = perLevel;
    }

    @NotNull
    public static EnchantCost read(@NotNull FileConfig config, @NotNull String path) {
        int base = ConfigValue.create(path + ".Base",
                0,
                "一级（I）附魔的基础花费（等级）。"
        ).read(config);

        int perLevel = ConfigValue.create(path + ".Per_Level",
                0,
                "当附魔等级高于一级（I）时，每提升 1 级在 Base 基础上额外增加的等级数。"
        ).read(config);

        return new EnchantCost(base, perLevel);
    }

    @Override
    public void write(@NotNull FileConfig config, @NotNull String path) {
        config.set(path + ".Base", this.base);
        config.set(path + ".Per_Level", this.perLevel);
    }

    public int calculate(int level) {
        return this.base + this.perLevel * (level - 1);
    }

    public int base() {
        return this.base;
    }

    public int perLevel() {
        return this.perLevel;
    }
}
