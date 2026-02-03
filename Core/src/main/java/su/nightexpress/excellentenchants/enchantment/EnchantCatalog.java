package su.nightexpress.excellentenchants.enchantment;

import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsFiles;
import su.nightexpress.excellentenchants.EnchantsKeys;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantDefinition;
import su.nightexpress.excellentenchants.api.EnchantDistribution;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.api.item.ItemSetDefaults;
import su.nightexpress.excellentenchants.api.item.ItemSetRegistry;
import su.nightexpress.excellentenchants.api.wrapper.TradeType;
import su.nightexpress.excellentenchants.bridge.EnchantCatalogEntry;
import su.nightexpress.excellentenchants.enchantment.armor.*;
import su.nightexpress.excellentenchants.enchantment.bow.*;
import su.nightexpress.excellentenchants.enchantment.fishing.*;
import su.nightexpress.excellentenchants.enchantment.tool.*;
import su.nightexpress.excellentenchants.enchantment.universal.*;
import su.nightexpress.excellentenchants.enchantment.weapon.*;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.FileUtil;
import su.nightexpress.nightcore.util.LowerCase;
import su.nightexpress.nightcore.util.Version;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static su.nightexpress.excellentenchants.EnchantsPlaceholders.*;
import static su.nightexpress.excellentenchants.enchantment.EnchantCatalog.Weight.*;

public enum EnchantCatalog implements EnchantCatalogEntry {

    COLD_STEEL(() -> EnchantDefinition.builder("寒钢", 3)
            .description(TRIGGER_CHANCE + "% 几率对攻击者施加 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.CHESTPLATE_ELYTRA)
            .primaryItems(ItemSetDefaults.CHESTPLATE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SNOW_COMMON),
            ColdSteelEnchant::new
    ),

    DARKNESS_CLOAK(() -> EnchantDefinition.builder("黑暗披风", 3)
            .description(TRIGGER_CHANCE + "% 几率对攻击者施加 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.CHESTPLATE_ELYTRA)
            .primaryItems(ItemSetDefaults.CHESTPLATE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SAVANNA_COMMON),
            DarknessCloakEnchant::new
    ),

    DRAGON_HEART(() -> EnchantDefinition.builder("龙之心", 5)
            .description("获得永久的 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + " 效果。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.CHESTPLATE_ELYTRA)
            .primaryItems(ItemSetDefaults.CHESTPLATE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.JUNGLE_SPECIAL),
            DragonHeartEnchant::new
    ),

    ELEMENTAL_PROTECTION(() -> EnchantDefinition.builder("元素防护", 4)
            .description("使药水伤害与元素伤害降低 " + GENERIC_AMOUNT + "%。")
            .weight(COMMON)
            .items(ItemSetDefaults.ARMOR)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SWAMP_COMMON),
            ElementalProtectionEnchant::new
    ),

    FIRE_SHIELD(() -> EnchantDefinition.builder("火焰护盾", 4)
            .description(TRIGGER_CHANCE + "% 几率点燃攻击者，持续 " + GENERIC_DURATION + " 秒。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.CHESTPLATE_ELYTRA)
            .primaryItems(ItemSetDefaults.CHESTPLATE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.DESERT_COMMON),
            FireShieldEnchant::new
    ),

    FLAME_WALKER(() -> EnchantDefinition.builder("熔岩行者", 2)
            .description("可在岩浆上行走，并免疫岩浆块伤害。")
            .weight(VERY_RARE)
            .items(ItemSetDefaults.BOOTS)
            .exclusives(EnchantsKeys.FROST_WALKER)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.DESERT_SPECIAL),
            FlameWalkerEnchant::new
    ),

    HARDENED(() -> EnchantDefinition.builder("硬化", 2)
            .description(TRIGGER_CHANCE + "% 几率在受伤时获得 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.CHESTPLATE_ELYTRA)
            .primaryItems(ItemSetDefaults.CHESTPLATE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            HardenedEnchant::new
    ),

    ICE_SHIELD(() -> EnchantDefinition.builder("寒冰护盾", 3)
            .description(TRIGGER_CHANCE + "% 几率冻结攻击者，并施加 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(COMMON)
            .supportedItems(ItemSetDefaults.CHESTPLATE_ELYTRA)
            .primaryItems(ItemSetDefaults.CHESTPLATE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SNOW_COMMON),
            IceShieldEnchant::new
    ),

    LIGHTWEIGHT(() -> EnchantDefinition.builder("轻盈", 1)
            .description("你可以安全地跳在海龟蛋、耕地与大型垂滴叶上而不会破坏它们。")
            .weight(COMMON)
            .items(ItemSetDefaults.BOOTS)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SNOW_COMMON),
            LightweightEnchant::new
    ),

    JUMPING(() -> EnchantDefinition.builder("跳跃", 2)
            .description("获得永久的 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + " 效果。")
            .weight(RARE)
            .items(ItemSetDefaults.BOOTS)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            JumpingEnchant::new
    ),

    KAMIKADZE(() -> EnchantDefinition.builder("神风", 3)
            .description(TRIGGER_CHANCE + "% 几率在死亡时发生爆炸。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.CHESTPLATE_ELYTRA)
            .primaryItems(ItemSetDefaults.CHESTPLATE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.JUNGLE_COMMON),
            KamikadzeEnchant::new
    ),

    NIGHT_VISION(() -> EnchantDefinition.builder("夜视", 1)
            .description("获得永久的 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + " 效果。")
            .weight(VERY_RARE)
            .items(ItemSetDefaults.HELMET)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.TAIGA_SPECIAL),
            NightVisionEnchant::new
    ),

    REBOUND(() -> EnchantDefinition.builder("反弹", 1)
            .description("落在黏液块上时会产生弹跳效果。")
            .weight(RARE)
            .items(ItemSetDefaults.BOOTS)
            .exclusives(EnchantsKeys.FEATHER_FALLING)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.SWAMP_COMMON),
            ReboundEnchant::new
    ),

    REGROWTH(() -> EnchantDefinition.builder("再生", 4)
            .description("每隔几秒恢复 " + GENERIC_AMOUNT + "❤。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.CHESTPLATE_ELYTRA)
            .primaryItems(ItemSetDefaults.CHESTPLATE)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.JUNGLE_SPECIAL),
            RegrowthEnchant::new
    ),

    SATURATION(() -> EnchantDefinition.builder("饱和", 2)
            .description("每隔几秒恢复 " + GENERIC_AMOUNT + " 点饥饿值。")
            .weight(RARE)
            .items(ItemSetDefaults.HELMET)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SAVANNA_SPECIAL),
            SaturationEnchant::new
    ),

    SPEED(() -> EnchantDefinition.builder("速度", 2)
            .description("获得永久的 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + " 效果。")
            .weight(RARE)
            .items(ItemSetDefaults.BOOTS)
            .build(),
            () -> EnchantDistribution.regular(TradeType.DESERT_SPECIAL),
            SpeedyEnchant::new
    ),

    STOPPING_FORCE(() -> EnchantDefinition.builder("制动", 3)
            .description(TRIGGER_CHANCE + "% 几率在 " + GENERIC_AMOUNT + "% 范围内降低击退效果。")
            .weight(UNCOMMON)
            .items(ItemSetDefaults.LEGGINGS)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            StoppingForceEnchant::new
    ),

    WATER_BREATHING(() -> EnchantDefinition.builder("水下呼吸", 1)
            .description("获得永久的 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + " 效果。")
            .weight(VERY_RARE)
            .items(ItemSetDefaults.HELMET)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.PLAINS_SPECIAL),
            WaterBreathingEnchant::new
    ),

    BOMBER(() -> EnchantDefinition.builder("轰炸者", 3)
            .description(TRIGGER_CHANCE + "% 几率射出一枚点燃的 TNT（引信 " + GENERIC_TIME + " 秒）。")
            .weight(VERY_RARE)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(EnchantsKeys.FLAME, EnchantsKeys.PUNCH, EnchantsKeys.POWER)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.DESERT_SPECIAL),
            BomberEnchant::new
    ),

    ENDER_BOW(() -> EnchantDefinition.builder("末影弓", 1)
            .description("发射末影珍珠代替箭矢。")
            .weight(VERY_RARE)
            .items(ItemSetDefaults.BOW)
            .exclusives(BOMBER.getKey(), EnchantsKeys.FLAME, EnchantsKeys.PUNCH, EnchantsKeys.POWER)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.PLAINS_SPECIAL),
            EnderBowEnchant::new
    ),

    GHAST(() -> EnchantDefinition.builder("恶魂", 1)
            .description("发射火球代替箭矢。")
            .weight(VERY_RARE)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), BOMBER.getKey(), EnchantsKeys.FLAME, EnchantsKeys.PUNCH, EnchantsKeys.POWER)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.DESERT_COMMON),
            GhastEnchant::new
    ),

    CONFUSING_ARROWS(() -> EnchantDefinition.builder("混乱箭", 3)
            .description(TRIGGER_CHANCE + "% 几率使箭矢附带 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(COMMON)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.SWAMP_COMMON),
            ConfusingArrowsEnchant::new
    ),

    DARKNESS_ARROWS(() -> EnchantDefinition.builder("黑暗箭", 3)
            .description(TRIGGER_CHANCE + "% 几率使箭矢附带 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(COMMON)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.SNOW_COMMON),
            DarknessArrowsEnchant::new
    ),

    DRAGONFIRE_ARROWS(() -> EnchantDefinition.builder("龙息箭", 3)
            .description(TRIGGER_CHANCE + "% 几率使箭矢产生龙息效果（半径 R=" + GENERIC_RADIUS + "，持续 " + GENERIC_DURATION + " 秒）。")
            .weight(RARE)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.SWAMP_SPECIAL),
            DragonfireArrowsEnchant::new
    ),

    ELECTRIFIED_ARROWS(() -> EnchantDefinition.builder("电击箭", 3)
            .description(TRIGGER_CHANCE + "% 几率召唤闪电并额外造成 " + GENERIC_DAMAGE + "❤ 伤害。")
            .weight(UNCOMMON)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            ElectrifiedArrowsEnchant::new
    ),

    EXPLOSIVE_ARROWS(() -> EnchantDefinition.builder("爆炸箭", 3)
            .description(TRIGGER_CHANCE + "% 几率射出爆炸箭。")
            .weight(UNCOMMON)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.TAIGA_COMMON),
            ExplosiveArrowsEnchant::new
    ),

    FLARE(() -> EnchantDefinition.builder("照明弹", 1)
            .description(TRIGGER_CHANCE + "% 几率在箭矢落点生成火把。")
            .weight(UNCOMMON)
            .items(ItemSetDefaults.BOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.treasure(TradeType.SNOW_COMMON),
            FlareEnchant::new
    ),

    HOVER(() -> EnchantDefinition.builder("悬浮箭", 3)
            .description(TRIGGER_CHANCE + "% 几率使箭矢附带 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(COMMON)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_SPECIAL),
            HoverEnchant::new
    ),

    LINGERING(() -> EnchantDefinition.builder("滞留箭", 3)
            .description(TRIGGER_CHANCE + "% 几率使药箭生成滞留效果。")
            .weight(RARE)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.SAVANNA_COMMON),
            LingeringEnchant::new
    ),

    POISONED_ARROWS(() -> EnchantDefinition.builder("剧毒箭", 3)
            .description(TRIGGER_CHANCE + "% 几率使箭矢附带 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(UNCOMMON)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.SWAMP_COMMON),
            PoisonedArrowsEnchant::new
    ),

    SNIPER(() -> EnchantDefinition.builder("狙击", 2)
            .description("使弹射物速度提高 " + GENERIC_AMOUNT + "%。")
            .weight(COMMON)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_SPECIAL),
            SniperEnchant::new
    ),

    VAMPIRIC_ARROWS(() -> EnchantDefinition.builder("吸血箭", 3)
            .description(TRIGGER_CHANCE + "% 几率在命中时恢复 " + GENERIC_AMOUNT + "❤。")
            .weight(RARE)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.SWAMP_SPECIAL),
            VampiricArrowsEnchant::new
    ),

    WITHERED_ARROWS(() -> EnchantDefinition.builder("凋零箭", 3)
            .description(TRIGGER_CHANCE + "% 几率使箭矢附带 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(UNCOMMON)
            .items(ItemSetDefaults.BOW_CROSSBOW)
            .exclusives(ENDER_BOW.getKey(), GHAST.getKey(), BOMBER.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.SNOW_SPECIAL),
            WitheredArrowsEnchant::new
    ),

    AUTO_REEL(() -> EnchantDefinition.builder("自动收线", 1)
            .description("在上钩时自动收回鱼钩。")
            .weight(VERY_RARE)
            .items(ItemSetDefaults.FISHING_ROD)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.JUNGLE_SPECIAL),
            AutoReelEnchant::new,
            false,
            true
    ),

    CURSE_OF_DROWNED(() -> EnchantDefinition.builder("溺尸诅咒", 3)
            .description(TRIGGER_CHANCE + "% 几率钓出一只溺尸。")
            .weight(UNCOMMON)
            .items(ItemSetDefaults.FISHING_ROD)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.SWAMP_COMMON),
            CurseOfDrownedEnchant::new,
            true
    ),

    DOUBLE_CATCH(() -> EnchantDefinition.builder("双倍收获", 3)
            .description("以 " + TRIGGER_CHANCE + "% 的几率使本次收获数量变为 x2。")
            .weight(RARE)
            .items(ItemSetDefaults.FISHING_ROD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            DoubleCatchEnchant::new
    ),

    RIVER_MASTER(() -> EnchantDefinition.builder("江河大师", 5)
            .description("增加抛竿距离。")
            .weight(COMMON)
            .items(ItemSetDefaults.FISHING_ROD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            RiverMasterEnchant::new
    ),

    SEASONED_ANGLER(() -> EnchantDefinition.builder("老练钓手", 3)
            .description("使钓鱼获得的经验值提高 " + GENERIC_AMOUNT + "%。")
            .weight(UNCOMMON)
            .items(ItemSetDefaults.FISHING_ROD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            SeasonedAnglerEnchant::new
    ),

    SURVIVALIST(() -> EnchantDefinition.builder("生存专家", 1)
            .description("若钓到的是生鱼，则自动烹饪。")
            .weight(RARE)
            .items(ItemSetDefaults.FISHING_ROD)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.SNOW_SPECIAL),
            SurvivalistEnchant::new
    ),

    BLAST_MINING(() -> EnchantDefinition.builder("爆破采掘", 5)
            .description(TRIGGER_CHANCE + "% 几率以爆炸方式采掘方块。")
            .weight(RARE)
            .items(ItemSetDefaults.PICKAXE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            BlastMiningEnchant::new
    ),

    GLASSBREAKER(() -> EnchantDefinition.builder("碎玻璃", 1)
            .description("可瞬间破坏玻璃。")
            .weight(COMMON)
            .supportedItems(ItemSetDefaults.TOOL)
            .primaryItems(ItemSetDefaults.MINING_TOOLS)
            .build(),
            () -> EnchantDistribution.regular(TradeType.DESERT_COMMON),
            GlassbreakerEnchant::new
    ),

    HASTE(() -> EnchantDefinition.builder("急迫", 3)
            .description("在挖掘方块时获得 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + " 效果。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.TOOL)
            .primaryItems(ItemSetDefaults.MINING_TOOLS)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SAVANNA_COMMON),
            HasteEnchant::new
    ),

    LUCKY_MINER(() -> EnchantDefinition.builder("幸运矿工", 3)
            .description(TRIGGER_CHANCE + "% 几率使矿石提供的经验值额外增加 " + GENERIC_AMOUNT + "%。")
            .weight(UNCOMMON)
            .items(ItemSetDefaults.PICKAXE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.JUNGLE_COMMON),
            LuckyMinerEnchant::new
    ),

    REPLANTER(() -> EnchantDefinition.builder("自动补种", 1)
            .description("右键或收获时自动补种作物。")
            .weight(VERY_RARE)
            .items(ItemSetDefaults.HOE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            ReplanterEnchant::new
    ),

    SILK_CHEST(() -> EnchantDefinition.builder("精准采箱", 1)
            .description("掉落箱子并保留其中所有物品。")
            .weight(VERY_RARE)
            .supportedItems(ItemSetDefaults.MINING_TOOLS)
            .primaryItems(ItemSetDefaults.AXE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SAVANNA_COMMON),
            SilkChestEnchant::new,
            false,
            true
    ),

    SILK_SPAWNER(() -> EnchantDefinition.builder("精准刷怪笼", 1)
            .description(TRIGGER_CHANCE + "% 几率采集刷怪笼。")
            .weight(VERY_RARE)
            .supportedItems(ItemSetDefaults.MINING_TOOLS)
            .primaryItems(ItemSetDefaults.PICKAXE)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.JUNGLE_SPECIAL),
            SilkSpawnerEnchant::new
    ),

    SMELTER(() -> EnchantDefinition.builder("熔炼", 5)
            .description("以 " + TRIGGER_CHANCE + "% 的几率自动熔炼被挖掘的方块。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.TOOL)
            .primaryItems(ItemSetDefaults.MINING_TOOLS)
            .exclusives(SILK_SPAWNER.getKey(), SILK_CHEST.getKey(), EnchantsKeys.SILK_TOUCH)
            .build(),
            () -> EnchantDistribution.regular(TradeType.DESERT_COMMON),
            SmelterEnchant::new
    ),

    TELEKINESIS(() -> EnchantDefinition.builder("念力", 1)
            .description("将方块掉落物直接传送到你的背包。")
            .weight(VERY_RARE)
            .supportedItems(ItemSetDefaults.TOOL)
            .primaryItems(ItemSetDefaults.MINING_TOOLS)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.DESERT_SPECIAL),
            TelekinesisEnchant::new
    ),

    TREEFELLER(() -> EnchantDefinition.builder("伐木", 1)
            .description("一次砍倒整棵树。")
            .weight(RARE)
            .items(ItemSetDefaults.AXE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.TAIGA_SPECIAL),
            TreefellerEnchant::new
    ),

    TUNNEL(() -> EnchantDefinition.builder("掘进", 3)
            .description("按指定形状一次性挖掘多个方块。")
            .weight(VERY_RARE)
            .supportedItems(ItemSetDefaults.MINING_TOOLS)
            .primaryItems(ItemSetDefaults.PICKAXE)
            .exclusives(BLAST_MINING.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.SAVANNA_SPECIAL),
            TunnelEnchant::new
    ),

    VEINMINER(() -> EnchantDefinition.builder("连锁采矿", 3)
            .description("一次性挖掘最多 " + GENERIC_AMOUNT + " 个相连矿脉方块。")
            .weight(RARE)
            .items(ItemSetDefaults.PICKAXE)
            .exclusives(BLAST_MINING.getKey(), TUNNEL.getKey())
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_SPECIAL),
            VeinminerEnchant::new
    ),

    CURSE_OF_BREAKING(() -> EnchantDefinition.builder("破损诅咒", 3)
            .description(TRIGGER_CHANCE + "% 几率额外消耗 " + GENERIC_AMOUNT + " 点耐久。")
            .weight(COMMON)
            .items(ItemSetDefaults.BREAKABLE)
            .exclusives(EnchantsKeys.UNBREAKING)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.SAVANNA_COMMON),
            CurseOfBreakingEnchant::new,
            true
    ),

    CURSE_OF_FRAGILITY(() -> EnchantDefinition.builder("脆弱诅咒", 1)
            .description("该物品无法使用砂轮去魔或在铁砧上操作。")
            .weight(COMMON)
            .items(ItemSetDefaults.BREAKABLE)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.TAIGA_COMMON),
            CurseOfFragilityEnchant::new,
            true
    ),

    CURSE_OF_MEDIOCRITY(() -> EnchantDefinition.builder("平庸诅咒", 3)
            .description(TRIGGER_CHANCE + "% 几率使物品掉落时失去附魔。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.TOOLS_WEAPONS)
            .primaryItems(ItemSetDefaults.ALL_WEAPON)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.SNOW_COMMON),
            CurseOfMediocrityEnchant::new,
            true
    ),

    CURSE_OF_MISFORTUNE(() -> EnchantDefinition.builder("厄运诅咒", 3)
            .description(TRIGGER_CHANCE + "% 几率使方块或生物不掉落任何物品。")
            .weight(UNCOMMON)
            .items(ItemSetDefaults.TOOLS_WEAPONS)
            .exclusives(EnchantsKeys.FORTUNE, EnchantsKeys.LOOTING)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.TAIGA_COMMON),
            CurseOfMisfortuneEnchant::new,
            true
    ),

    RESTORE(() -> EnchantDefinition.builder("修复", 3)
            .description(TRIGGER_CHANCE + "% 几率在物品即将损坏时保留它，并将耐久恢复到 " + GENERIC_AMOUNT + "%。")
            .weight(RARE)
            .items(ItemSetDefaults.BREAKABLE)
            .build(),
            () -> EnchantDistribution.regular(TradeType.DESERT_COMMON),
            RestoreEnchant::new
    ),

    SOULBOUND(() -> EnchantDefinition.builder("灵魂绑定", 1)
            .description("死亡时不会掉落该物品。")
            .weight(RARE)
            .items(ItemSetDefaults.BREAKABLE)
            .exclusives(EnchantsKeys.VANISHING_CURSE)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.DESERT_SPECIAL),
            SoulboundEnchant::new
    ),

    BANE_OF_NETHERSPAWN(() -> EnchantDefinition.builder("下界杀手", 5)
            .description("对下界生物造成 " + GENERIC_DAMAGE + " 倍伤害。")
            .weight(COMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            BaneOfNetherspawnEnchant::new
    ),

    BLINDNESS(() -> EnchantDefinition.builder("致盲", 2)
            .description(TRIGGER_CHANCE + "% 几率在命中时施加 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(COMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.TAIGA_COMMON),
            BlindnessEnchant::new
    ),

    CONFUSION(() -> EnchantDefinition.builder("混乱", 2)
            .description(TRIGGER_CHANCE + "% 几率在命中时施加 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(COMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SNOW_COMMON),
            ConfusionEnchant::new
    ),

    CURE(() -> EnchantDefinition.builder("治愈", 3)
            .description(TRIGGER_CHANCE + "% 几率在命中时治愈僵尸村民与僵尸猪灵。")
            .weight(COMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SAVANNA_COMMON),
            CureEnchant::new
    ),

    CURSE_OF_DEATH(() -> EnchantDefinition.builder("死亡诅咒", 3)
            .description("击杀玩家时，你也有概率一起死亡。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.DESERT_SPECIAL),
            CurseOfDeathEnchant::new,
            true
    ),

    CUTTER(() -> EnchantDefinition.builder("破甲", 3)
            .description(TRIGGER_CHANCE + "% 几率击落敌人的护甲，并额外对护甲造成 " + GENERIC_DAMAGE + "% 的耐久损耗。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            CutterEnchant::new
    ),

    DECAPITATOR(() -> EnchantDefinition.builder("斩首者", 2)
            .description(TRIGGER_CHANCE + "% 几率获得玩家或生物的头颅。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.SNOW_SPECIAL),
            DecapitatorEnchant::new
    ),

    DOUBLE_STRIKE(() -> EnchantDefinition.builder("双重打击", 2)
            .description(TRIGGER_CHANCE + "% 几率造成双倍伤害。")
            .weight(VERY_RARE)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.TAIGA_COMMON),
            DoubleStrikeEnchant::new
    ),

    EXHAUST(() -> EnchantDefinition.builder("疲惫", 4)
            .description(TRIGGER_CHANCE + "% 几率在命中时施加 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(COMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            ExhaustEnchant::new
    ),

    ICE_ASPECT(() -> EnchantDefinition.builder("寒冰之刃", 3)
            .description("命中时冻结目标并施加 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(COMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SNOW_COMMON),
            IceAspectEnchant::new
    ),

    INFERNUS(() -> EnchantDefinition.builder("炼狱", 3)
            .description("投掷的三叉戟命中时点燃敌人，持续 " + GENERIC_TIME + " 秒。")
            .weight(COMMON)
            .items(ItemSetDefaults.TRIDENT)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SAVANNA_COMMON),
            InfernusEnchant::new
    ),

    NIMBLE(() -> EnchantDefinition.builder("灵巧", 1)
            .description("将生物掉落物直接传送到你的背包。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.JUNGLE_COMMON),
            NimbleEnchant::new
    ),

    PARALYZE(() -> EnchantDefinition.builder("麻痹", 5)
            .description(TRIGGER_CHANCE + "% 几率在命中时施加 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.TAIGA_COMMON),
            ParalyzeEnchant::new
    ),

    RAGE(() -> EnchantDefinition.builder("狂怒", 2)
            .description(TRIGGER_CHANCE + "% 几率在命中时获得 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.DESERT_COMMON),
            RageEnchant::new
    ),

    ROCKET(() -> EnchantDefinition.builder("火箭", 3)
            .description(TRIGGER_CHANCE + "% 几率把敌人发射到天上。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.JUNGLE_COMMON),
            RocketEnchant::new
    ),

    SWIPER(() -> EnchantDefinition.builder("掠夺者", 3)
            .description(TRIGGER_CHANCE + "% 几率从玩家身上偷取 " + GENERIC_AMOUNT + " 点经验值。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SWAMP_COMMON),
            SwiperEnchant::new
    ),

    TEMPER(() -> EnchantDefinition.builder("淬炼", 5)
            .description("你每缺失 " + GENERIC_RADIUS + "❤，造成的伤害提高 " + GENERIC_AMOUNT + "%。")
            .weight(VERY_RARE)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.TAIGA_COMMON),
            TemperEnchant::new
    ),

    THRIFTY(() -> EnchantDefinition.builder("节俭", 3)
            .description(TRIGGER_CHANCE + "% 几率使生物掉落刷怪蛋。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.treasure(TradeType.JUNGLE_SPECIAL),
            ThriftyEnchant::new
    ),

    THUNDER(() -> EnchantDefinition.builder("雷霆", 5)
            .description(TRIGGER_CHANCE + "% 几率召唤闪电并额外造成 " + GENERIC_DAMAGE + "❤ 伤害。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            ThunderEnchant::new
    ),

    VAMPIRE(() -> EnchantDefinition.builder("吸血", 3)
            .description(TRIGGER_CHANCE + "% 几率在命中时恢复 " + GENERIC_AMOUNT + "❤。")
            .weight(RARE)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SAVANNA_COMMON),
            VampireEnchant::new
    ),

    VENOM(() -> EnchantDefinition.builder("毒液", 2)
            .description(TRIGGER_CHANCE + "% 几率在命中时施加 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(COMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SWAMP_COMMON),
            VenomEnchant::new
    ),

    VILLAGE_DEFENDER(() -> EnchantDefinition.builder("村庄守护者", 5)
            .description("对所有灾厄村民额外造成 " + GENERIC_AMOUNT + "❤ 伤害。")
            .weight(COMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.PLAINS_COMMON),
            VillageDefenderEnchant::new
    ),

    WISDOM(() -> EnchantDefinition.builder("智慧", 5)
            .description("生物掉落经验值变为 x" + GENERIC_MODIFIER + "。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.DESERT_COMMON),
            WisdomEnchant::new
    ),

    WITHER(() -> EnchantDefinition.builder("凋零", 2)
            .description(TRIGGER_CHANCE + "% 几率在命中时施加 " + EFFECT_TYPE + " " + EFFECT_AMPLIFIER + "（持续 " + EFFECT_DURATION + " 秒）。")
            .weight(UNCOMMON)
            .supportedItems(ItemSetDefaults.SWORDS_AXES)
            .primaryItems(ItemSetDefaults.SWORD)
            .build(),
            () -> EnchantDistribution.regular(TradeType.SNOW_COMMON),
            WitherEnchant::new
    ),
    ;

    public static void loadAll(@NotNull Path dataDir, @NotNull ItemSetRegistry itemSetRegistry, @NotNull BiConsumer<EnchantCatalog, IllegalStateException> onError) {
        Path enchantsDir = Path.of(dataDir.toString(), EnchantsFiles.DIR_ENCHANTS);
        Path disabledDir = Path.of(enchantsDir.toString(), EnchantsFiles.DIR_DISABLED);

        if (!Files.exists(disabledDir)) {
            try {
                Files.createDirectories(disabledDir);
            }
            catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        Set<String> disabledEnchants = FileUtil.findYamlFiles(disabledDir.toString()).stream().map(FileUtil::getNameWithoutExtension).collect(Collectors.toSet());

        for (EnchantCatalog value : values()) {
            if (disabledEnchants.contains(value.getId())) {
                value.disabled = true;
            }
            else {
                try {
                    value.load(enchantsDir, itemSetRegistry);
                }
                catch (IllegalStateException exception) {
                    onError.accept(value, exception);
                }
            }
        }
    }

    @NotNull
    public static Stream<EnchantCatalog> stream() {
        return Stream.of(values());
    }

    @NotNull
    public static Stream<EnchantCatalog> enabled() {
        return stream().filter(EnchantCatalog::isEnabled);
    }

    private final String  id;
    private final boolean curse;
    private final boolean paperOnly;

    private final Supplier<EnchantDefinition>   initDefinition;
    private final Supplier<EnchantDistribution> initDistribution;
    private final EnchantFactory<?>             factory;

    private EnchantDefinition   definition;
    private EnchantDistribution distribution;
    private boolean             disabled;

    EnchantCatalog(@NotNull Supplier<EnchantDefinition> initDefinition,
                   @NotNull Supplier<EnchantDistribution> initDistribution,
                   @NotNull EnchantFactory<?> factory) {
        this(initDefinition, initDistribution, factory, false);
    }

    EnchantCatalog(@NotNull Supplier<EnchantDefinition> initDefinition,
                   @NotNull Supplier<EnchantDistribution> initDistribution,
                   @NotNull EnchantFactory<?> factory,
                   boolean curse) {
        this(initDefinition, initDistribution, factory, curse, false);
    }

    EnchantCatalog(@NotNull Supplier<EnchantDefinition> initDefinition,
                   @NotNull Supplier<EnchantDistribution> initDistribution,
                   @NotNull EnchantFactory<?> factory,
                   boolean curse,
                   boolean paperOnly) {
        this.id = LowerCase.INTERNAL.apply(this.name());
        this.curse = curse;
        this.paperOnly = paperOnly;
        this.initDefinition = initDefinition;
        this.initDistribution = initDistribution;
        this.factory = factory;
    }

    public void load(@NotNull Path enchantsDir, @NotNull ItemSetRegistry itemSetRegistry) throws IllegalStateException {
        if (this.paperOnly && Version.isSpigot()) throw new IllegalStateException("The enchantment is available for PaperMC only");

        Path file = Path.of(enchantsDir.toString(), FileConfig.withExtension(this.id));
        boolean exists = Files.exists(file);

        FileConfig config = FileConfig.load(file);
        EnchantDefinition definition = this.initDefinition.get();
        EnchantDistribution distribution = this.initDistribution.get();

        if (!exists) {
            config.set("Definition", definition);
            config.set("Distribution", distribution);
        }

        this.definition = EnchantDefinition.read(config, "Definition", itemSetRegistry);
        this.distribution = EnchantDistribution.read(config, "Distribution");

        config.saveChanges();
    }

    @NotNull
    public CustomEnchantment createEnchantment(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        return this.factory.create(plugin, manager, file, context);
    }

    @Override
    @NotNull
    public String getId() {
        return this.id;
    }

    @Override
    @NotNull
    public NamespacedKey getKey() {
        return EnchantsKeys.create(this.id);
    }

    @Override
    @NotNull
    public EnchantDefinition getDefinition() {
        if (this.definition == null) throw new IllegalStateException("Definition is not yet initialized");

        return this.definition;
    }

    @Override
    @NotNull
    public EnchantDistribution getDistribution() {
        if (this.distribution == null) throw new IllegalStateException("Distribution is not yet initialized");

        return this.distribution;
    }

    @Override
    public boolean isCurse() {
        return this.curse;
    }

    public boolean isPaperOnly() {
        return this.paperOnly;
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public boolean isEnabled() {
        return !this.disabled;
    }

    static class Weight {

        static final int COMMON    = 10;
        static final int UNCOMMON  = 5;
        static final int RARE      = 2;
        static final int VERY_RARE = 1;

    }
}
