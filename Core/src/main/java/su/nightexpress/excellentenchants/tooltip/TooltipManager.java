package su.nightexpress.excellentenchants.tooltip;

import org.bukkit.GameMode;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.enchantment.EnchantRegistry;
import su.nightexpress.excellentenchants.api.tooltip.TooltipController;
import su.nightexpress.excellentenchants.api.tooltip.TooltipHandler;
import su.nightexpress.excellentenchants.tooltip.format.ChargesFormat;
import su.nightexpress.excellentenchants.tooltip.handler.PacketTooltipHandler;
import su.nightexpress.excellentenchants.tooltip.handler.ProtocolTooltipHandler;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.nightcore.manager.AbstractManager;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.Plugins;
import su.nightexpress.nightcore.util.placeholder.PlaceholderContext;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.LangUtil;

import java.util.*;
import java.io.File;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class TooltipManager extends AbstractManager<EnchantsPlugin> implements TooltipController {

    private final TooltipSettings             settings;
    private final Map<String, TooltipFactory> factoryMap;
    private final Set<UUID>                   updateStopList;
    private final Map<NamespacedKey, VanillaDescription> vanillaDescriptions;

    private TooltipHandler handler;

    public TooltipManager(@NotNull EnchantsPlugin plugin) {
        super(plugin);
        this.settings = new TooltipSettings();
        this.factoryMap = new LinkedHashMap<>();
        this.updateStopList = new HashSet<>();
        this.vanillaDescriptions = new HashMap<>();
    }

    @Override
    protected void onLoad() {
        this.settings.load(this.plugin.getConfig());
        this.loadFactories();
        this.loadVanillaDescriptions();
        this.loadHandler();

        this.addListener(new TooltipListener(this.plugin, this));
    }

    @Override
    protected void onShutdown() {
        if (this.handler != null) {
            this.handler.shutdown();
            this.handler = null;
        }
        this.factoryMap.clear();
        this.updateStopList.clear();
    }

    private void loadFactories() {
        this.factoryMap.put(TooltipPlugins.PACKET_EVENTS, PacketTooltipHandler::new);
        this.factoryMap.put(TooltipPlugins.PROTOCOL_LIB, provider -> new ProtocolTooltipHandler(this.plugin, provider));
    }

    private void loadHandler() {
        for (var entry : this.factoryMap.entrySet()) {
            if (!Plugins.isInstalled(entry.getKey())) continue;

            this.handler = entry.getValue().create(this);
            this.handler.setup();
            this.plugin.info("正在使用附魔提示处理器：" + entry.getKey());
            break;
        }

        if (this.handler == null) {
            this.plugin.error(
                    "未找到兼容的附魔提示（Tooltip）提供者。请安装以下任一插件以启用该功能：[%s]"
                            .formatted(String.join(", ", this.factoryMap.keySet()))
            );
        }
    }

    @Override
    public boolean hasHandler() {
        return this.handler != null;
    }

    @Override
    public void runInStopList(@NotNull Player player, @NotNull Runnable runnable) {
        this.addToUpdateStopList(player);
        runnable.run();
        this.removeFromUpdateStopList(player);
    }

    @Override
    public void addToUpdateStopList(@NotNull Player player) {
        this.updateStopList.add(player.getUniqueId());
    }

    @Override
    public void removeFromUpdateStopList(@NotNull Player player) {
        this.updateStopList.remove(player.getUniqueId());
    }

    @Override
    public boolean isReadyForTooltipUpdate(@NotNull Player player) {
        return !this.updateStopList.contains(player.getUniqueId()) && player.getGameMode() != GameMode.CREATIVE;
    }

    @Override
    public boolean isEnchantTooltipAllowed(@NotNull ItemStack item) {
        if (this.settings.isForBooksOnly()) {
            return EnchantsUtils.isEnchantedBook(item);
        }
        return true;
    }

    @NotNull
    private String getDescription(@NotNull CustomEnchantment enchantment, int level, int charges) {
        String format = enchantment.isChargeable() ? this.settings.getTooltipFormatWithCharges() : this.settings.getTooltipFormat();

        PlaceholderContext context = PlaceholderContext.builder()
                .with(EnchantsPlaceholders.GENERIC_DESCRIPTION, () -> String.join("\n", enchantment.getDescription(level)))
                .with(EnchantsPlaceholders.GENERIC_NAME, enchantment::getDisplayName)
                .with(EnchantsPlaceholders.GENERIC_CHARGES, () -> {
                    if (!enchantment.isChargeable() || charges < 0) return "";

                    int maxCharges = enchantment.getMaxCharges(level);
                    int percent = (int) Math.ceil((double) charges / (double) maxCharges * 100D);
                    ChargesFormat chargesFormat = this.settings.getTooltipChargesFormat(percent);

                    return chargesFormat == null ? "" : chargesFormat.getFormatted(charges);
                })
                .build();

        return context.apply(format);
    }

    @Override
    @NotNull
    public ItemStack addDescription(@NotNull ItemStack itemStack) {
        if (itemStack.getType().isAir() || !this.isEnchantTooltipAllowed(itemStack)) return itemStack;

        ItemStack copy = new ItemStack(itemStack);
        ItemMeta meta = copy.getItemMeta();
        if (meta == null) return itemStack;

        Map<Enchantment, Integer> all = EnchantsUtils.getEnchantments(meta);
        if (all.isEmpty()) return itemStack;

        // Hide vanilla enchant lines so we can fully control ordering and place description right under its enchant.
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<String> lore = new ArrayList<>();
        boolean showTitles = !EnchantsUtils.isEnchantedBook(itemStack);

        // Sort: vanilla (minecraft namespace) first, then custom; each group by key string for stability.
        List<Map.Entry<Enchantment, Integer>> ordered = all.entrySet().stream()
                .sorted(Comparator
                        .comparing((Map.Entry<Enchantment, Integer> e) -> !e.getKey().getKey().getNamespace().equals(NamespacedKey.MINECRAFT))
                        .thenComparing(e -> e.getKey().getKey().asString()))
                .collect(Collectors.toList());

        ordered.forEach(entry -> {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            CustomEnchantment custom = EnchantRegistry.getByBukkit(enchant);
            if (custom != null) {
                int chargesAmount = custom.getCharges(meta);
                if (showTitles) lore.add(this.getEnchantTitle(custom, level));
                String description = this.getDescription(custom, level, chargesAmount);
                lore.addAll(Arrays.asList(description.split("\n")));
            }
            else {
                if (showTitles) lore.add(this.getVanillaTitle(enchant, level));
                String description = this.getVanillaDescription(enchant, level);
                if (!description.isEmpty()) {
                    lore.addAll(Arrays.asList(description.split("\n")));
                }
            }
        });

        // Append existing custom lore after enchant block so player-written lore stays below.
        lore.addAll(ItemUtil.getLoreSerialized(meta));

        ItemUtil.setLore(meta, lore);
        copy.setItemMeta(meta);
        return copy;
    }

    @NotNull
    private String getEnchantTitle(@NotNull CustomEnchantment enchantment, int level) {
        // Show level for any enchant that can have multiple levels (even when current level is 1)
        boolean showLevel = enchantment.getDefinition().getMaxLevel() > 1;
        String name = enchantment.getDisplayName();
        String color = this.getLegacyColorFromMiniMessage(name);
        if (color.isEmpty()) color = ChatColor.GRAY.toString();
        String levelSuffix = showLevel ? " " + color + NumberUtil.toRoman(level) : "";
        return name + levelSuffix;
    }

    @NotNull
    private String getVanillaTitle(@NotNull Enchantment enchantment, int level) {
        VanillaDescription info = this.vanillaDescriptions.get(enchantment.getKey());

        String displayName;
        String color;

        if (info != null && !info.displayName().isEmpty()) {
            displayName = this.serializeMiniToLegacy(info.displayName());
            color = this.getConfiguredVanillaColor(info.displayName());
        }
        else {
            displayName = LangUtil.getSerializedName(enchantment);
            color = enchantment.isCursed() ? ChatColor.RED.toString() : ChatColor.GRAY.toString();
            displayName = color + displayName;
        }

        if (color.isEmpty()) {
            color = enchantment.isCursed() ? ChatColor.RED.toString() : ChatColor.GRAY.toString();
        }

        // Show level for any vanilla enchant that supports multiple levels (even when current level is 1)
        boolean showLevel = enchantment.getMaxLevel() > 1;
        String levelSuffix = showLevel ? " " + color + NumberUtil.toRoman(level) : "";
        return displayName + levelSuffix;
    }

    /* -------- Vanilla descriptions support -------- */

    private void loadVanillaDescriptions() {
        File file = new File(this.plugin.getDataFolder(), "vanillaenchants.yml");
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) continue;

            NamespacedKey namespacedKey = NamespacedKey.minecraft(key);
            Enchantment enchantment = Enchantment.getByKey(namespacedKey);
            if (enchantment == null) continue;

            String displayName = section.getString("name", "");
            String description = section.getString("description", "");
            if (description.isEmpty()) continue;

            Map<String, String> placeholders = new HashMap<>();

            if (section.isString("placeholder")) {
                placeholders.put("placeholder", section.getString("placeholder", ""));
            }

            ConfigurationSection map = section.getConfigurationSection("placeholders");
            if (map != null) {
                for (String placeholderKey : map.getKeys(false)) {
                    placeholders.put(placeholderKey, map.getString(placeholderKey, ""));
                }
            }

            this.vanillaDescriptions.put(namespacedKey, new VanillaDescription(displayName, description, placeholders));
        }
    }

    @NotNull
    private String getVanillaDescription(@NotNull Enchantment enchantment, int level) {
        VanillaDescription description = this.vanillaDescriptions.get(enchantment.getKey());
        if (description == null) return "";

        Map<String, String> computed = new HashMap<>();
        description.placeholders().forEach((key, expr) -> {
            String value = this.evalExpression(expr, level);
            computed.put(key, value);
        });

        String text = description.text();
        // Replace custom placeholders first.
        for (Map.Entry<String, String> entry : computed.entrySet()) {
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        // Replace %level% if present.
        text = text.replace("%level%", String.valueOf(level));

        String colored = ChatColor.translateAlternateColorCodes('&', text);
        // Prefer color defined in config display name; otherwise fall back to vanilla defaults.
        String baseColor = this.getConfiguredVanillaColor(description.displayName());
        if (baseColor.isEmpty()) {
            baseColor = enchantment.isCursed() ? ChatColor.RED.toString() : ChatColor.GRAY.toString();
        }
        return baseColor + colored;
    }

    private String getLegacyColorFromMiniMessage(@NotNull String mini) {
        try {
            Component component = MiniMessage.miniMessage().deserialize(this.normalizeMiniMessage(mini));
            String legacy = LegacyComponentSerializer.legacySection().serialize(component);
            return ChatColor.getLastColors(legacy);
        }
        catch (Exception ex) {
            return "";
        }
    }

    @NotNull
    private String serializeMiniToLegacy(@NotNull String mini) {
        Component component = MiniMessage.miniMessage().deserialize(this.normalizeMiniMessage(mini));
        return LegacyComponentSerializer.legacySection().serialize(component);
    }

    @NotNull
    private String normalizeMiniMessage(@NotNull String input) {
        // Accept a common shorthand "<c:#RRGGBB>" as "<color:#RRGGBB>"
        if (input.contains("<c:#")) {
            return input.replace("<c:#", "<color:#").replace("</c>", "</color>");
        }
        return input;
    }

    @NotNull
    private String getConfiguredVanillaColor(@NotNull String displayName) {
        if (displayName.isEmpty()) return "";
        return this.getLegacyColorFromMiniMessage(displayName);
    }

    @NotNull
    private String evalExpression(@NotNull String expression, int level) {
        String prepared = expression.replace("%level%", String.valueOf(level))
                .replace("ceil(", "Math.ceil(")
                .replace("min(", "Math.min(")
                .replace("max(", "Math.max(");

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        if (engine == null) return prepared;

        try {
            Object result = engine.eval(prepared);
            if (result instanceof Number number) {
                return NumberUtil.format(number.doubleValue());
            }
            return String.valueOf(result);
        }
        catch (Exception e) {
            return prepared;
        }
    }

    private record VanillaDescription(String displayName, String text, Map<String, String> placeholders) {}
}

