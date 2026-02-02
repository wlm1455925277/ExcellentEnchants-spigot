package su.nightexpress.excellentenchants.bridge.paper;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import io.papermc.paper.tag.TagEntry;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import su.nightexpress.excellentenchants.EnchantsKeys;
import su.nightexpress.excellentenchants.api.item.ItemSet;
import su.nightexpress.excellentenchants.api.item.ItemSetRegistry;
import su.nightexpress.excellentenchants.api.EnchantDefinition;
import su.nightexpress.excellentenchants.api.EnchantDistribution;
import su.nightexpress.excellentenchants.api.wrapper.TradeType;
import su.nightexpress.excellentenchants.enchantment.DistributionConfig;
import su.nightexpress.excellentenchants.enchantment.EnchantCatalog;
import su.nightexpress.nightcore.bridge.common.NightKey;
import su.nightexpress.nightcore.util.Lists;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class PaperEnchantsBootstrap implements PluginBootstrap {

    @NotNull
    private TagKey<ItemType> customItemTag(@NotNull String name) {
        return TagKey.create(RegistryKey.ITEM, Key.key(EnchantsKeys.NAMESPACE, name));
    }

    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        Path dataDirectory = context.getDataDirectory();

        DistributionConfig distributionConfig = DistributionConfig.load(dataDirectory);
        if (distributionConfig.isUseMinecraftNamespace()) {
            EnchantsKeys.setVanillaNamespace();
        }

        var lifeCycle = context.getLifecycleManager();

        // 为附魔的“主物品集(primary)”与“可用物品集(supported)”创建包含自定义物品的 Tag。
        // 使用 postFlatten 而不是 preFlatten：这样可以访问原版物品 Tag，更方便生成默认 ItemSet。
        lifeCycle.registerEventHandler(LifecycleEvents.TAGS.postFlatten(RegistryKey.ITEM).newHandler(event -> {
            PostFlattenTagRegistrar<ItemType> registrar = event.registrar();
            PaperItemTagLookup tagLookup = new PaperItemTagLookup(registrar);
            ItemSetRegistry itemSetRegistry = new ItemSetRegistry(dataDirectory, tagLookup);

            // 加载默认物品类型集合（ItemSet）。
            itemSetRegistry.load();

            // 为我们的 ItemSet 对象注册自定义 Tag。
            itemSetRegistry.values().forEach(itemSet -> {
                TagKey<ItemType> tagKey = this.customItemTag(itemSet.getId());
                // 将物品名转换为 TagEntry：为每个物品创建对应的 TypedKey。
                var tagEntries = itemSet.getMaterials().stream().map(itemName -> TypedKey.create(RegistryKey.ITEM, Key.key(itemName))).toList();

                registrar.addToTag(tagKey, tagEntries);
            });

            // 加载默认值，并从配置文件读取附魔的 Definition 与 Distribution 设置。
            EnchantCatalog.loadAll(
                    dataDirectory,
                    itemSetRegistry,
                    (entry, exception) -> context.getLogger().error("无法加载 '{}' 附魔：'{}'", entry.getId(), exception.getMessage())
            );
        }));

        // 在附魔注册表冻结（freeze）生命周期事件上注册处理器
        lifeCycle.registerEventHandler(RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {
            var registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

            EnchantCatalog.enabled().forEach((data) -> {
                EnchantDefinition definition = data.getDefinition();
                ItemSet primarySet = definition.getPrimaryItemSet();
                ItemSet supportedSet = definition.getSupportedItemSet();

                TagKey<ItemType> primaryItemsTag = this.customItemTag(primarySet.getId());
                TagKey<ItemType> supportedItemsTag = this.customItemTag(supportedSet.getId());

                Tag<@NonNull ItemType> primaryItems = event.getOrCreateTag(primaryItemsTag);
                Tag<@NonNull ItemType> supportedItems = event.getOrCreateTag(supportedItemsTag);

                RegistryKeySet<@NonNull Enchantment> exclusiveSet = RegistrySet.keySet(RegistryKey.ENCHANTMENT, definition.getExclusiveSet()
                        .stream()
                        .map(rawKey -> {
                            Key key = NightKey.key(rawKey).toBukkit();
                            if (registry.get(key) == null) {
                                context.getLogger().warn(
                                        "在 '{}' 的互斥列表中发现未知附魔 '{}'。请确保被互斥的附魔在被引用前已先加载/注册。",
                                        data.getId(),
                                        key
                                );
                                return null;
                            }
                            return EnchantmentKeys.create(key);
                        })
                        .filter(Objects::nonNull)
                        .toList()
                );

                EquipmentSlotGroup[] activeSlots = Stream.of(supportedSet.getSlots()).map(EquipmentSlot::getGroup).toArray(EquipmentSlotGroup[]::new);
                Key key = data.getKey();
                Component component = MiniMessage.miniMessage().deserialize(definition.getDisplayName());
                String nameOnly = MiniMessage.miniMessage().stripTags(definition.getDisplayName());

                event.registry().register(
                        EnchantmentKeys.create(key),
                        builder -> builder
                                .description(Component.translatable(key.asString(), nameOnly, component.style()))
                                .primaryItems(primaryItems)
                                .supportedItems(supportedItems)
                                .exclusiveWith(exclusiveSet)
                                .anvilCost(definition.getAnvilCost())
                                .maxLevel(definition.getMaxLevel())
                                .weight(definition.getWeight())
                                .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(definition.getMinCost().base(), definition.getMinCost().perLevel()))
                                .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(definition.getMaxCost().base(), definition.getMaxCost().perLevel()))
                                .activeSlots(activeSlots)
                );
            });
        }));

        lifeCycle.registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.ENCHANTMENT).newHandler(event -> {
            var registrar = event.registrar();

            EnchantCatalog.enabled().forEach((data) -> {
                EnchantDistribution distribution = data.getDistribution();
                TypedKey<Enchantment> key = EnchantmentKeys.create(data.getKey());

                TagEntry<Enchantment> entry = TagEntry.valueEntry(key);
                Set<TagEntry<Enchantment>> list = Lists.newSet(entry);

                // 任何附魔都可以是“宝藏附魔”。
                if (distribution.isTreasure()) {
                    registrar.addToTag(EnchantmentTagKeys.TREASURE, list);
                    registrar.addToTag(EnchantmentTagKeys.DOUBLE_TRADE_PRICE, list);
                }
                // 该 Tag 会被包含进其他 Tag，导致无法从其他 Tag 中排除它的附魔。
                //else registrar.addToTag(EnchantmentTagKeys.NON_TREASURE, list);

                // 任何附魔都可以出现在随机战利品中。
                if (distribution.isOnRandomLoot() && distributionConfig.isRandomLootEnabled()) {
                    registrar.addToTag(EnchantmentTagKeys.ON_RANDOM_LOOT, list);
                }

                // 只有非宝藏附魔才应该出现在怪物装备、交易装备以及（未重平衡的）村民交易中。
                if (!distribution.isTreasure()) {
                    if (distribution.isOnMobSpawnEquipment() && distributionConfig.isMobEquipmentEnabled()) {
                        registrar.addToTag(EnchantmentTagKeys.ON_MOB_SPAWN_EQUIPMENT, list);
                    }

                    // 仅在“村民交易重平衡”实验关闭时有效。
                    // 因为该实验使用预定义的 Enchantment Provider（单附魔）交易。
                    // 示例：VillagerTrades.TypeSpecificTrade.oneTradeInBiomes(...)
                    // 相关类参考：
                    // - VillagerTrades
                    // - TradeRebalanceEnchantmentProviders
                    // - VanillaEnchantmentProviders
                    if (distribution.isOnTradedEquipment() && distributionConfig.isTradeEquipmentEnabled()) {
                        registrar.addToTag(EnchantmentTagKeys.ON_TRADED_EQUIPMENT, list);
                    }
                }

                // 任何附魔都可以被交易（附魔书）。
                if (distribution.isTradable() && distributionConfig.isTradingEnabled()) {
                    distribution.getTrades().forEach(tradeType -> {
                        registrar.addToTag(getTradeKey(tradeType), list);
                    });
                    registrar.addToTag(EnchantmentTagKeys.TRADEABLE, list);
                }

                if (data.isCurse()) {
                    registrar.addToTag(EnchantmentTagKeys.CURSE, list);
                }
                else {
                    // 只有非诅咒且非宝藏附魔才应该进入附魔台。
                    if (!distribution.isTreasure()) {
                        if (distribution.isDiscoverable() && distributionConfig.isEnchantingEnabled()) {
                            registrar.addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, list);
                        }
                    }
                }
            });
        }));
    }

    @NotNull
    private static TagKey<Enchantment> getTradeKey(@NotNull TradeType tradeType) {
        return switch (tradeType) {
            case DESERT_COMMON -> EnchantmentTagKeys.TRADES_DESERT_COMMON;
            case DESERT_SPECIAL -> EnchantmentTagKeys.TRADES_DESERT_SPECIAL;
            case PLAINS_COMMON -> EnchantmentTagKeys.TRADES_PLAINS_COMMON;
            case PLAINS_SPECIAL -> EnchantmentTagKeys.TRADES_PLAINS_SPECIAL;
            case SAVANNA_COMMON -> EnchantmentTagKeys.TRADES_SAVANNA_COMMON;
            case SAVANNA_SPECIAL -> EnchantmentTagKeys.TRADES_SAVANNA_SPECIAL;
            case JUNGLE_COMMON -> EnchantmentTagKeys.TRADES_JUNGLE_COMMON;
            case JUNGLE_SPECIAL -> EnchantmentTagKeys.TRADES_JUNGLE_SPECIAL;
            case SNOW_COMMON -> EnchantmentTagKeys.TRADES_SNOW_COMMON;
            case SNOW_SPECIAL -> EnchantmentTagKeys.TRADES_SNOW_SPECIAL;
            case SWAMP_COMMON -> EnchantmentTagKeys.TRADES_SWAMP_COMMON;
            case SWAMP_SPECIAL -> EnchantmentTagKeys.TRADES_SWAMP_SPECIAL;
            case TAIGA_COMMON -> EnchantmentTagKeys.TRADES_TAIGA_COMMON;
            case TAIGA_SPECIAL -> EnchantmentTagKeys.TRADES_TAIGA_SPECIAL;
        };
    }
}
