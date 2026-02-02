package su.nightexpress.excellentenchants.api;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.api.wrapper.TradeType;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.config.Writeable;
import su.nightexpress.nightcore.util.Enums;
import su.nightexpress.nightcore.util.Lists;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EnchantDistribution implements Writeable {

    private final boolean        treasure;
    private final boolean        discoverable;
    private final boolean        tradable;
    private final boolean        onMobSpawnEquipment;
    private final boolean        onRandomLoot;
    private final boolean        onTradedEquipment;
    private final Set<TradeType> tradeTypes;

    public EnchantDistribution(@NotNull Set<TradeType> tradeTypes,
                               boolean treasure,
                               boolean discoverable,
                               boolean tradable,
                               boolean onMobSpawnEquipment,
                               boolean onRandomLoot,
                               boolean onTradedEquipment) {
        this.tradeTypes = tradeTypes;
        this.treasure = treasure;
        this.discoverable = discoverable;
        this.tradable = tradable;
        this.onMobSpawnEquipment = onMobSpawnEquipment;
        this.onRandomLoot = onRandomLoot;
        this.onTradedEquipment = onTradedEquipment;
    }

    @NotNull
    public static EnchantDistribution defaults() {
        return new EnchantDistribution(
                new HashSet<>(Arrays.asList(TradeType.values())),
                false,
                true,
                true,
                true,
                true,
                true
        );
    }

    @NotNull
    public static EnchantDistribution regular(@NotNull TradeType... tradeTypes) {
        return new EnchantDistribution(
                Lists.newSet(tradeTypes),
                false,
                true,
                true,
                true,
                true,
                true
        );
    }

    @NotNull
    public static EnchantDistribution treasure(@NotNull TradeType... tradeTypes) {
        return new EnchantDistribution(
                Lists.newSet(tradeTypes),
                true,
                false,
                true,
                false,
                true,
                false
        );
    }

    public static EnchantDistribution read(@NotNull FileConfig config, @NotNull String path) {
        boolean treasure = ConfigValue.create(path + ".Treasure",
                false,
                "设置这个附魔是否为“宝藏附魔”。",
                "宝藏附魔是那些无法通过附魔台获得的附魔，且不会在村民出售的随机附魔装备或怪物装备上生成。",
                "宝藏附魔只能通过抢夺、交易或钓鱼获得。",
                "如果宝藏附魔可以交易，它的价格会是相同等级的非宝藏附魔的两倍（在上限64绿宝石前）。",
                "[*] 更改后需要重启服务器。"
        ).read(config);

        boolean tradable = ConfigValue.create(path + ".Tradeable",
                true,
                "控制这个附魔是否可以通过村民出售。",
                "https://minecraft.wiki/w/Trading#Librarian",
                "[*] 更改后需要重启服务器。"
        ).read(config);

        var tradeTypes = ConfigValue.forSet(path + ".TradeTypes",
                name -> Enums.get(name, TradeType.class),
                (cfg, path2, set) -> cfg.set(path2, set.stream().map(Enum::name).toList()),
                () -> Lists.newSet(TradeType.values()),
                "设置这个附魔在村民交易中可以在哪些村庄生物群系找到。",
                "允许的值：[" + Enums.inline(TradeType.class) + "]",
                "https://minecraft.wiki/w/Villager_Trade_Rebalance#Trading",
                "[*] 更改后需要重启服务器。",
                "[*] 如果'Tradeable'设置为'false'且村民交易平衡已禁用，则此设置不起作用。"
        ).read(config);

        boolean onMobSpawnEquipment = ConfigValue.create(path + ".On_Mob_Spawn_Equipment",
                true,
                "控制这个附魔是否可以在生成的怪物装备上找到。",
                "https://minecraft.wiki/w/Armor#Armor_equipped_on_mobs",
                "[*] 更改后需要重启服务器。",
                "[*] 如果'Treasure'设置为'true'，则此设置不起作用。"
        ).read(config);

        boolean onTradedEquipment = ConfigValue.create(path + ".On_Traded_Equipment",
                true,
                "控制这个附魔是否可以在村民交易中找到的装备上。",
                "https://minecraft.wiki/w/Trading#Trade_offers",
                "[*] 更改后需要重启服务器。",
                "[*] 如果'Treasure'设置为'true'或村民交易平衡启用，则此设置不起作用。"
        ).read(config);

        boolean onRandomLoot = ConfigValue.create(path + ".On_Random_Loot",
                true,
                "控制这个附魔是否可以在自然生成的从掉落表生成的装备上找到。",
                "https://minecraft.wiki/w/Loot_table",
                "[*] 更改后需要重启服务器。"
        ).read(config);

        boolean discoverable = ConfigValue.create(path + ".Discoverable",
                true,
                "控制这个附魔是否可以在附魔台找到，或用于附魔通过掉落表生成的物品。",
                "https://minecraft.wiki/w/Enchanting#Enchanting_table",
                "[*] 更改后需要重启服务器。",
                "[*] 如果'Treasure'设置为'true'，则此设置不起作用。"
        ).read(config);

        return new EnchantDistribution(tradeTypes, treasure, discoverable, tradable, onMobSpawnEquipment, onRandomLoot, onTradedEquipment);
    }

    @Override
    public void write(@NotNull FileConfig config, @NotNull String path) {
        config.set(path + ".Treasure", this.treasure);
        config.set(path + ".Tradeable", this.tradable);
        config.set(path + ".TradeTypes", Lists.modify(this.tradeTypes, Enum::name));
        config.set(path + ".On_Mob_Spawn_Equipment", this.onMobSpawnEquipment);
        config.set(path + ".On_Traded_Equipment", this.onTradedEquipment);
        config.set(path + ".On_Random_Loot", this.onRandomLoot);
        config.set(path + ".Discoverable", this.discoverable);
    }

    public boolean isTreasure() {
        return this.treasure;
    }

    public boolean isTradable() {
        return this.tradable;
    }

    @NotNull
    public Set<TradeType> getTrades() {
        return this.tradeTypes;
    }

    public boolean isDiscoverable() {
        return this.discoverable;
    }

    public boolean isOnMobSpawnEquipment() {
        return this.onMobSpawnEquipment;
    }

    public boolean isOnRandomLoot() {
        return this.onRandomLoot;
    }

    public boolean isOnTradedEquipment() {
        return this.onTradedEquipment;
    }
}
