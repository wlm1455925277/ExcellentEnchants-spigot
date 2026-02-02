package su.nightexpress.excellentenchants.enchantment;

import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsFiles;
import su.nightexpress.excellentenchants.bridge.DistributionSettings;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;

import java.nio.file.Path;

public class DistributionConfig extends AbstractConfig implements DistributionSettings {

    @NotNull
    public static DistributionConfig load(@NotNull Path dataDir) {
        FileConfig fileConfig = FileConfig.load(dataDir.toString(), EnchantsFiles.FILE_DISTRIBUTION);
        DistributionConfig settings = new DistributionConfig();
        fileConfig.edit(settings::load);
        return settings;
    }

    @Override
    public void load(@NotNull FileConfig config) {
        if (config.contains("Custom_Namespace.Enabled")) {
            config.set("Core.Use-Minecraft-Namespace", !config.getBoolean("Custom_Namespace.Enabled"));
            config.remove("Custom_Namespace.Enabled");
        }

        super.load(config);
    }

    private final ConfigProperty<Boolean> useMinecraftNamespace = this.addProperty(ConfigTypes.BOOLEAN, "Core.Use-Minecraft-Namespace",
            false,
            "控制 ExcellentEnchants 的附魔是否使用 'minecraft' 命名空间。",
            "[*] 警告：强烈不建议修改。未来版本可能移除此功能；若设为 true，作者可能不提供支持。",
            "[*] 一旦修改，所有物品上已存在的 ExcellentEnchants 自定义附魔将被永久删除，无法恢复。",
            "[*] 需要重启服务器后生效。",
            "[默认值：false]"
    );

    private final ConfigProperty<Boolean> distributionEnchanting = this.addProperty(ConfigTypes.BOOLEAN, "Distribution.Enchanting_Table",
            true,
            "控制 ExcellentEnchants 是否可在附魔台中出现，或用于战利品表生成的物品附魔。",
            "https://minecraft.wiki/w/Enchanting#Enchanting_table",
            "[*] 需要重启服务器后生效。",
            "[默认值：true]"
    );

    private final ConfigProperty<Boolean> distributionTrading = this.addProperty(ConfigTypes.BOOLEAN, "Distribution.Trading",
            true,
            "控制村民是否会出售 ExcellentEnchants 的附魔（附魔书等）。",
            "https://minecraft.wiki/w/Trading#Librarian",
            "[*] 需要重启服务器后生效。",
            "[默认值：true]"
    );

    private final ConfigProperty<Boolean> distributionMobEquipment = this.addProperty(ConfigTypes.BOOLEAN, "Distribution.Mob_Equipment",
            true,
            "控制自然生成/刷新的怪物装备上是否可能带有 ExcellentEnchants 的附魔。",
            "https://minecraft.wiki/w/Armor#Armor_equipped_on_mobs",
            "[*] 需要重启服务器后生效。",
            "[默认值：true]"
    );

    private final ConfigProperty<Boolean> distributionTradeEquipment = this.addProperty(ConfigTypes.BOOLEAN, "Distribution.Trade_Equipment",
            true,
            "控制村民出售的装备上是否可能带有 ExcellentEnchants 的附魔。",
            "https://minecraft.wiki/w/Trading#Trade_offers",
            "[*] 仅在“Villager Trade Rebalance（村民交易重平衡）”实验功能关闭时有效。",
            "[*] 需要重启服务器后生效。",
            "[默认值：true]"
    );

    private final ConfigProperty<Boolean> distributionRandomLoot = this.addProperty(ConfigTypes.BOOLEAN, "Distribution.Random_Loot",
            true,
            "控制自然生成的战利品（战利品表生成的装备）上是否可能带有 ExcellentEnchants 的附魔。",
            "https://minecraft.wiki/w/Loot_table",
            "[*] 需要重启服务器后生效。",
            "[默认值：true]"
    );

    public boolean isUseMinecraftNamespace() {
        return this.useMinecraftNamespace.get();
    }

    @Override
    public boolean isEnchantingEnabled() {
        return this.distributionEnchanting.get();
    }

    @Override
    public boolean isTradingEnabled() {
        return this.distributionTrading.get();
    }

    @Override
    public boolean isMobEquipmentEnabled() {
        return this.distributionMobEquipment.get();
    }

    @Override
    public boolean isTradeEquipmentEnabled() {
        return this.distributionTradeEquipment.get();
    }

    @Override
    public boolean isRandomLootEnabled() {
        return this.distributionRandomLoot.get();
    }
}
