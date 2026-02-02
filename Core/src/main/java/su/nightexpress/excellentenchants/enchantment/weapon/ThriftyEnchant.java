package su.nightexpress.excellentenchants.enchantment.weapon;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.KillEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.BukkitThing;
import su.nightexpress.nightcore.util.Enums;

import java.nio.file.Path;
import java.util.Set;

import static org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

public class ThriftyEnchant extends GameEnchantment implements KillEnchant {

    private Set<EntityType>  ignoredEntityTypes;
    private Set<SpawnReason> ignoredSpawnReasons;
    private boolean          ignoreMythicMobs;

    public ThriftyEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(0, 1));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.ignoredEntityTypes = ConfigValue.forSet("Thrifty.Ignored_Mobs",
                BukkitThing::getEntityType,
                (cfg, path, set) -> cfg.set(path, set.stream().map(Enum::name).toList()),
                Set.of(EntityType.WITHER, EntityType.ENDER_DRAGON),
                "不受该附魔影响的生物列表。"
        ).read(config);

        this.ignoredSpawnReasons = ConfigValue.forSet("Thrifty.Ignored_Spawn_Reasons",
                id -> Enums.get(id, SpawnReason.class),
                (cfg, path, set) -> cfg.set(path, set.stream().map(Enum::name).toList()),
                Set.of(
                        SpawnReason.SPAWNER_EGG,
                        SpawnReason.SPAWNER,
                        SpawnReason.DISPENSE_EGG
                ),
                "当生物以以下生成原因出生时，不受该附魔影响。",
                "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/event/entity/CreatureSpawnEvent.SpawnReason.html"
        ).read(config);

        this.ignoreMythicMobs = ConfigValue.create("Thrifty.Ignored_MythicMobs",
                true,
                "是否忽略（不作用于）MythicMobs 插件生成的怪物。"
        ).read(config);
    }

    @Override
    @NotNull
    public EnchantPriority getKillPriority() {
        return EnchantPriority.NORMAL;
    }

    @Override
    public boolean onKill(@NotNull EntityDeathEvent event, @NotNull LivingEntity entity, @NotNull Player killer, @NotNull ItemStack weapon, int level) {
        if (this.ignoredEntityTypes.contains(entity.getType())) return false;
        if (this.ignoreMythicMobs && EnchantsUtils.isMythicMob(entity)) return false;

        SpawnReason spawnReason = this.manager.getSpawnReason(entity);
        if (spawnReason != null && this.ignoredSpawnReasons.contains(spawnReason)) return false;

        Material material = plugin.getServer().getItemFactory().getSpawnEgg(entity.getType());
        if (material == null) return false;

        ItemStack eggItem = new ItemStack(material);
        event.getDrops().add(eggItem);
        return true;
    }
}
