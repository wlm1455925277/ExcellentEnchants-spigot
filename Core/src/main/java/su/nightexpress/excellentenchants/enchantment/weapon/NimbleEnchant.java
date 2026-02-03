package su.nightexpress.excellentenchants.enchantment.weapon;

import io.lumine.mythic.api.mobs.MythicMob;
import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.drops.DropExecutor;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.KillEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Players;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class NimbleEnchant extends GameEnchantment implements KillEnchant {

    private boolean ignorePlayers;

    // MythicMobs integration
    private boolean collectMythicDrops;
    private double  mythicSearchRadius;
    private boolean debugLogging;

    // PDC marker for MythicMobs FancyDrops
    private static final NamespacedKey MYTHIC_FANCYDROP_KEY = NamespacedKey.fromString("mythicmobs:fancydrop");

    public NimbleEnchant(@NotNull EnchantsPlugin plugin,
                         @NotNull EnchantManager manager,
                         @NotNull Path file,
                         @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.addComponent(EnchantComponent.PROBABILITY, Probability.oneHundred());
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.ignorePlayers = ConfigValue.create(
                "Nimble.Ignore_Players",
                false,
                "Sets whether or not to ignore drops from players."
        ).read(config);

        this.collectMythicDrops = ConfigValue.create(
                "Nimble.Collect_Mythic_Drops",
                true,
                "If enabled, Nimble will try to move MythicMobs ground drops into killer's inventory."
        ).read(config);

        this.mythicSearchRadius = ConfigValue.create(
                "Nimble.Mythic_Search_Radius",
                6.0D,
                "Radius around death location to look for MythicMobs item drops."
        ).read(config);

        this.debugLogging = ConfigValue.create(
                "Nimble.Debug_Logging",
                false,
                "If enabled, Nimble will print debug information to console."
        ).read(config);

        if (this.debugLogging) {
            this.plugin.getLogger().info("[Nimble] Config loaded: Ignore_Players=" + this.ignorePlayers +
                    ", Collect_Mythic_Drops=" + this.collectMythicDrops +
                    ", Mythic_Search_Radius=" + this.mythicSearchRadius);
        }
    }

    @NotNull
    @Override
    public EnchantPriority getKillPriority() {
        return EnchantPriority.MONITOR;
    }

    @Override
    public boolean onKill(@NotNull EntityDeathEvent event,
                          @NotNull LivingEntity entity,
                          @NotNull Player killer,
                          @NotNull ItemStack weapon,
                          int level) {

        if (this.ignorePlayers && entity instanceof Player) return false;

        Boolean preventOtherDrops = getMythicPreventOtherDrops(entity); // null = not mythic
        boolean isMythic = preventOtherDrops != null;

        if (this.debugLogging) {
            this.plugin.getLogger().info("[Nimble] onKill: victim=" + entity.getType() +
                    ", killer=" + killer.getName() +
                    ", world=" + entity.getWorld().getName() +
                    ", drops=" + event.getDrops().size() +
                    ", isMythic=" + isMythic +
                    ", preventOtherDrops=" + preventOtherDrops);
        }

        // 1) Handle Bukkit drops
        if (!event.getDrops().isEmpty()) {
            if (isMythic && Boolean.TRUE.equals(preventOtherDrops)) {
                if (this.debugLogging) {
                    List<String> names = event.getDrops().stream()
                            .map(item -> item.getType() + "x" + item.getAmount())
                            .collect(Collectors.toList());
                    this.plugin.getLogger().info("[Nimble] MythicMob PreventOtherDrops=true, clearing Bukkit drops without giving: " + names);
                }
                event.getDrops().clear();
            }
            else {
                if (this.debugLogging) {
                    List<String> names = event.getDrops().stream()
                            .map(item -> item.getType() + "x" + item.getAmount())
                            .collect(Collectors.toList());
                    this.plugin.getLogger().info("[Nimble] Moving Bukkit drops to inventory: " + names);
                }
                for (ItemStack drop : event.getDrops()) {
                    stripMythicFancyDropTag(drop);
                    Players.addItem(killer, drop);
                }
                event.getDrops().clear();
            }
        }

        // 2) Sweep MythicMobs fancy/ground drops
        if (this.collectMythicDrops && isMythic) {
            final Location deathLoc = entity.getLocation();
            final Player player = killer;
            final double radius = this.mythicSearchRadius;

            if (this.debugLogging) {
                this.plugin.getLogger().info("[Nimble] Scheduling Mythic ground-drop sweep at " +
                        locToString(deathLoc) + ", radius=" + radius);
            }

            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (deathLoc.getWorld() == null) return;

                // Respect FancyDrop owner mapping when available (Mythic 5.11+)
                Map<UUID, UUID> fancyPickupMap = null;
                try {
                    MythicBukkit mm = MythicBukkit.inst();
                    if (mm != null && mm.getDropManager() instanceof DropExecutor executor) {
                        fancyPickupMap = executor.getItemPickupMap();
                    }
                }
                catch (Throwable ignored) {
                    // If API differs, fallback to null (no owner filtering)
                }

                int collected = 0;

                for (Entity nearby : deathLoc.getWorld().getNearbyEntities(deathLoc, radius, radius, radius)) {
                    if (!(nearby instanceof Item itemEntity)) continue;

                    ItemStack stack = itemEntity.getItemStack();
                    if (stack == null || stack.getType().isAir()) continue;

                    boolean isFancyDrop = isMythicFancyDrop(stack);

                    if (isFancyDrop && fancyPickupMap != null) {
                        UUID owner = fancyPickupMap.get(itemEntity.getUniqueId());
                        if (owner != null && !owner.equals(player.getUniqueId())) {
                            if (debugLogging) {
                                plugin.getLogger().info("[Nimble] Skipping FancyDrop owned by another player at " +
                                        locToString(itemEntity.getLocation()) +
                                        " (" + stack.getType() + "x" + stack.getAmount() + ", owner=" + owner + ")");
                            }
                            continue;
                        }
                    }

                    if (cannotPickupForPlayer(itemEntity, player)) {
                        if (debugLogging) {
                            plugin.getLogger().info("[Nimble] Skipping item not pickable for " +
                                    player.getName() + " at " + locToString(itemEntity.getLocation()));
                        }
                        continue;
                    }

                    stripMythicFancyDropTag(stack);

                    if (debugLogging) {
                        plugin.getLogger().info("[Nimble] Collecting Mythic ground drop: " +
                                stack.getType() + "x" + stack.getAmount() +
                                " at " + locToString(itemEntity.getLocation()));
                    }

                    Players.addItem(player, stack);
                    itemEntity.remove();
                    collected++;
                }

                if (debugLogging) {
                    plugin.getLogger().info("[Nimble] Mythic ground-drop sweep finished. Collected entities: " + collected);
                }
            });
        }

        return true;
    }

    /**
     * @return PreventOtherDrops flag if entity is MythicMob, otherwise null.
     */
    private Boolean getMythicPreventOtherDrops(@NotNull LivingEntity entity) {
        try {
            MythicBukkit mm = MythicBukkit.inst();
            if (mm == null || mm.getMobManager() == null) return null;

            ActiveMob activeMob = mm.getMobManager().getMythicMobInstance(entity);
            if (activeMob == null) return null;

            MythicMob type = activeMob.getType();
            if (type == null) return null;

            return type.getPreventOtherDrops();
        }
        catch (Throwable t) {
            if (this.debugLogging) {
                this.plugin.getLogger().warning("[Nimble] Failed to resolve Mythic data for entity "
                        + entity.getType() + ": " + t.getClass().getSimpleName() + " - " + t.getMessage());
            }
            return null;
        }
    }

    private boolean cannotPickupForPlayer(@NotNull Item item, @NotNull Player player) {
        try {
            UUID owner = item.getOwner();
            return owner != null && !owner.equals(player.getUniqueId());
        }
        catch (Throwable ignored) {
        }
        return false;
    }

    private boolean isMythicFancyDrop(@NotNull ItemStack stack) {
        if (MYTHIC_FANCYDROP_KEY == null) return false;
        if (!stack.hasItemMeta()) return false;
        try {
            ItemMeta meta = stack.getItemMeta();
            if (meta == null) return false;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            return pdc != null && pdc.has(MYTHIC_FANCYDROP_KEY, PersistentDataType.STRING);
        }
        catch (Throwable ignored) {
        }
        return false;
    }

    private void stripMythicFancyDropTag(@NotNull ItemStack stack) {
        if (MYTHIC_FANCYDROP_KEY == null) return;
        if (!stack.hasItemMeta()) return;

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        try {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc != null && pdc.has(MYTHIC_FANCYDROP_KEY, PersistentDataType.STRING)) {
                pdc.remove(MYTHIC_FANCYDROP_KEY);
                stack.setItemMeta(meta);

                if (this.debugLogging) {
                    this.plugin.getLogger().info("[Nimble] Stripped mythicmobs:fancydrop from " +
                            stack.getType() + "x" + stack.getAmount());
                }
            }
        }
        catch (Throwable ignored) {
        }
    }

    private String locToString(@NotNull Location loc) {
        if (loc.getWorld() == null) {
            return "(null_world," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + ")";
        }
        return "(" + loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + ")";
    }
}
