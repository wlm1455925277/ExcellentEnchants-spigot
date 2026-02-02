package su.nightexpress.excellentenchants.enchantment;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsFiles;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.nightcore.configuration.AbstractConfig;
import su.nightexpress.nightcore.configuration.ConfigProperty;
import su.nightexpress.nightcore.configuration.ConfigTypes;
import su.nightexpress.nightcore.util.LowerCase;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class EnchantSettings extends AbstractConfig {

    private final ConfigProperty<Long> arrowEffectsTickInterval = this.addProperty(ConfigTypes.LONG, "Arrow_Effects.Tick_Interval",
            1L,
            "设置由附魔添加的“箭矢/三叉戟 粒子拖尾效果”的刷新间隔（tick）。",
            "[提高数值 = 更省性能；降低数值 = 更顺滑的视觉效果]",
            "[20 tick = 1 秒]",
            "[默认值：1]"
    );

    private final ConfigProperty<Integer> passiveEnchantsTickInterval = this.addProperty(ConfigTypes.INT, "Passive_Enchants.Interval",
            1,
            "被动类附魔的触发间隔（单位：秒）。",
            "被动附魔依赖实体的 ticksLived 值；修改该值可能导致被动附魔触发不准确。",
            "[默认值：1]"
    );

    private final ConfigProperty<Boolean> passiveEnchantsAllowForMobs = this.addProperty(ConfigTypes.BOOLEAN, "Passive_Enchants.AllowForMobs",
            true,
            "控制怪物是否也会受到“被动附魔效果”的影响。",
            "[关闭以提升性能；开启以获得更完整的游戏体验]",
            "[默认值：true]"
    );

    private final ConfigProperty<Map<String, Set<String>>> disabledEnchantsByWorld = this.addProperty(
            ConfigTypes.forMapWithLowerKeys(ConfigTypes.STRING_SET_LOWER_CASE),
            "Disabled.ByWorld",
            Map.of(
                    "your_world_name", Set.of("enchantment_name", "ice_aspect"),
                    "another_world", Set.of("another_enchantment", "ice_aspect")
            ),
            "在此处填写你想在指定世界禁用的“自定义附魔名称”。",
            "如需对某个世界禁用所有附魔，请使用 '" + EnchantsPlaceholders.WILDCARD + "' 代替附魔名称列表。",
            "附魔名称等同于 '" + EnchantsFiles.DIR_ENCHANTS + "' 目录下的附魔配置文件名。",
            "[*] 注意：此设置只会禁用附魔效果，不会影响该世界的附魔获取/分布（掉落、附魔台、交易等）！"
    );

    private final ConfigProperty<Integer> anvilEnchantLimit = this.addProperty(ConfigTypes.INT, "Anvil.Enchant_Limit",
            5,
            "当物品已拥有指定数量的自定义附魔时，阻止继续通过铁砧附魔（限制上限）。",
            "[默认值：5]"
    );

    public long getArrowEffectsTickInterval() {
        return this.arrowEffectsTickInterval.get();
    }

    public int getPassiveEnchantsTickInterval() {
        return this.passiveEnchantsTickInterval.get();
    }

    public boolean isPassiveEnchantsAllowedForMobs() {
        return this.passiveEnchantsAllowForMobs.get();
    }

    public boolean isEnchantDisabledInWorld(@NotNull World world, @NotNull CustomEnchantment enchantment) {
        return this.disabledEnchantsByWorld.get()
                .getOrDefault(LowerCase.INTERNAL.apply(world.getName()), Collections.emptySet())
                .contains(enchantment.getId());
    }

    public int getAnvilEnchantsLimit() {
        return this.anvilEnchantLimit.get();
    }
}
