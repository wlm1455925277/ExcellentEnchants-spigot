package su.nightexpress.excellentenchants.protection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.*;

public final class ProtectionManager implements Listener {

    private static ProtectionManager INSTANCE;

    public static void init(Plugin plugin) {
        if (INSTANCE != null) return;
        INSTANCE = new ProtectionManager(plugin);
        Bukkit.getPluginManager().registerEvents(INSTANCE, plugin);
    }

    private static void ensureInit() {
        if (INSTANCE == null) throw new IllegalStateException("ProtectionManager not initialized. Call ProtectionManager.init(plugin) in enable().");
    }

    private final Plugin plugin;

    private final NamespacedKey keyManaged;
    private final NamespacedKey keyOwner;

    private final NamespacedKey keyStrictBlocks;
    private final NamespacedKey keyStrictDamage;
    private final NamespacedKey keyStrictKnockback;

    private final NamespacedKey keyAllowBlocks;
    private final NamespacedKey keyAllowItems;

    private final NamespacedKey keyCloudManaged;
    private final NamespacedKey keyCloudOwner;

    private static final ThreadLocal<Boolean> TL_PROBING_DAMAGE = ThreadLocal.withInitial(() -> false);

    private ProtectionManager(Plugin plugin) {
        this.plugin = plugin;

        this.keyManaged         = new NamespacedKey(plugin, "ee_pm_managed");
        this.keyOwner           = new NamespacedKey(plugin, "ee_pm_owner");

        this.keyStrictBlocks    = new NamespacedKey(plugin, "ee_pm_strict_blocks");
        this.keyStrictDamage    = new NamespacedKey(plugin, "ee_pm_strict_damage");
        this.keyStrictKnockback = new NamespacedKey(plugin, "ee_pm_strict_knockback");

        this.keyAllowBlocks     = new NamespacedKey(plugin, "ee_pm_allow_blocks");
        this.keyAllowItems      = new NamespacedKey(plugin, "ee_pm_allow_items");

        this.keyCloudManaged    = new NamespacedKey(plugin, "ee_pm_cloud_managed");
        this.keyCloudOwner      = new NamespacedKey(plugin, "ee_pm_cloud_owner");
    }

    // ------------------------- public API -------------------------

    public static void markExplosionTnt(TNTPrimed tnt, Player owner) {
        markExplosionTnt(tnt, owner, true, true, true, true, true);
    }

    public static void markExplosionTnt(TNTPrimed tnt, Player owner,
                                        boolean strictBlocks,
                                        boolean strictDamage,
                                        boolean strictKnockback) {
        markExplosionTnt(tnt, owner, strictBlocks, strictDamage, strictKnockback, true, true);
    }

    public static void markExplosionTnt(TNTPrimed tnt, Player owner,
                                        boolean strictBlocks,
                                        boolean strictDamage,
                                        boolean strictKnockback,
                                        boolean allowBlocks,
                                        boolean allowItems) {
        ensureInit();
        markExplosionSource(tnt, owner, strictBlocks, strictDamage, strictKnockback, allowBlocks, allowItems);
    }

    public static void markExplosionSource(Entity source, Player owner,
                                           boolean strictBlocks,
                                           boolean strictDamage,
                                           boolean strictKnockback,
                                           boolean allowBlocks,
                                           boolean allowItems) {
        ensureInit();
        PersistentDataContainer pdc = source.getPersistentDataContainer();

        pdc.set(INSTANCE.keyManaged, PersistentDataType.BYTE, (byte) 1);

        if (owner != null) pdc.set(INSTANCE.keyOwner, PersistentDataType.STRING, owner.getUniqueId().toString());
        else pdc.remove(INSTANCE.keyOwner);

        pdc.set(INSTANCE.keyStrictBlocks,    PersistentDataType.BYTE, strictBlocks ? (byte) 1 : (byte) 0);
        pdc.set(INSTANCE.keyStrictDamage,    PersistentDataType.BYTE, strictDamage ? (byte) 1 : (byte) 0);
        pdc.set(INSTANCE.keyStrictKnockback, PersistentDataType.BYTE, strictKnockback ? (byte) 1 : (byte) 0);

        pdc.set(INSTANCE.keyAllowBlocks,     PersistentDataType.BYTE, allowBlocks ? (byte) 1 : (byte) 0);
        pdc.set(INSTANCE.keyAllowItems,      PersistentDataType.BYTE, allowItems ? (byte) 1 : (byte) 0);
    }

    public static boolean canModifyAt(Player player, Location location, Block preferredBlock) {
        ensureInit();
        if (player == null || location == null) return false;
        Block block = preferredBlock != null ? preferredBlock : location.getBlock();
        return INSTANCE.canBuildProbe(player, block, BlockFace.UP);
    }

    public static void markCloud(AreaEffectCloud cloud, Player owner) {
        ensureInit();
        PersistentDataContainer pdc = cloud.getPersistentDataContainer();
        pdc.set(INSTANCE.keyCloudManaged, PersistentDataType.BYTE, (byte) 1);
        if (owner != null) pdc.set(INSTANCE.keyCloudOwner, PersistentDataType.STRING, owner.getUniqueId().toString());
        else pdc.remove(INSTANCE.keyCloudOwner);
    }

    /**
     * ✅ 新增：给附魔/技能类调用的公共判断
     * 用你现有的“probe damage event”方式，判断 owner 在当前位置是否允许影响 target
     */
    public static boolean canAffectEntity(Player owner, Entity target) {
        ensureInit();
        if (owner == null || target == null) return false;
        return INSTANCE.canOwnerAffectEntityHere(owner, target);
    }

    // ------------------------- explode blocks + knockback -------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onExplode(EntityExplodeEvent event) {
        Entity src = event.getEntity();
        if (src == null || !isManaged(src)) return;

        boolean strictBlocks = getRule(src, keyStrictBlocks);
        boolean strictKnock  = getRule(src, keyStrictKnockback);
        boolean allowBlocks  = getRule(src, keyAllowBlocks);

        Player owner = getOwnerOnline(src);

        if (!allowBlocks) {
            event.blockList().clear();
        } else if (strictBlocks) {
            if (owner == null) {
                event.blockList().clear();
            } else {
                for (Block b : new ArrayList<>(event.blockList())) {
                    if (!canBuildProbe(owner, b, BlockFace.UP)) {
                        event.blockList().clear();
                        break;
                    }
                }
            }
        }

        if (strictKnock) {
            rollbackKnockbackNextTick(event.getLocation(), owner);
        }
    }

    private void rollbackKnockbackNextTick(Location center, Player owner) {
        if (center == null || center.getWorld() == null) return;

        Collection<Entity> nearby = center.getWorld().getNearbyEntities(center, 8.0, 8.0, 8.0);
        Map<UUID, Location> before = new HashMap<>();
        for (Entity e : nearby) {
            if (!e.isValid()) continue;
            before.put(e.getUniqueId(), e.getLocation().clone());
        }

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            for (Map.Entry<UUID, Location> entry : before.entrySet()) {
                Entity e = Bukkit.getEntity(entry.getKey());
                if (e == null || !e.isValid()) continue;

                boolean allow = owner != null && canOwnerAffectEntityHere(owner, e);
                if (!allow) {
                    e.teleport(entry.getValue());
                    e.setVelocity(new Vector(0, 0, 0));
                }
            }
        });
    }

    // ------------------------- explosion damage + items/hanging -------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (TL_PROBING_DAMAGE.get()) return;

        Entity damager = event.getDamager();
        if (damager == null || !isManaged(damager)) return;

        boolean strictDamage = getRule(damager, keyStrictDamage);
        if (!strictDamage) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return;

        boolean allowItems = getRule(damager, keyAllowItems);
        if (!allowItems && event.getEntity() instanceof org.bukkit.entity.Item) {
            event.setCancelled(true);
            return;
        }

        Player owner = getOwnerOnline(damager);
        if (owner == null) {
            event.setCancelled(true);
            return;
        }

        if (!canOwnerAffectEntityHere(owner, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Entity remover = event.getRemover();
        if (remover == null || !isManaged(remover)) return;

        boolean strictDamage = getRule(remover, keyStrictDamage);
        if (!strictDamage) return;

        boolean allowItems = getRule(remover, keyAllowItems);
        if (!allowItems) {
            event.setCancelled(true);
            return;
        }

        Player owner = getOwnerOnline(remover);
        if (owner == null) {
            event.setCancelled(true);
            return;
        }

        if (!canOwnerAffectEntityHere(owner, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    // ------------------------- cloud apply filter -------------------------

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCloudApply(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        PersistentDataContainer pdc = cloud.getPersistentDataContainer();

        Byte m = pdc.get(this.keyCloudManaged, PersistentDataType.BYTE);
        if (m == null || m != (byte) 1) return;

        Player owner = getOwnerOnlineFromCloud(cloud);
        if (owner == null) {
            event.getAffectedEntities().clear();
            return;
        }

        event.getAffectedEntities().removeIf(e -> !canOwnerAffectEntityHere(owner, e));
    }

    // ------------------------- probes -------------------------

    private boolean canBuildProbe(Player player, Block target, BlockFace againstFace) {
        if (player == null || target == null) return false;

        Block against = target.getRelative(againstFace != null ? againstFace : BlockFace.UP);
        BlockState replaced = target.getState();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) item = new ItemStack(Material.STONE);

        BlockPlaceEvent probe = new BlockPlaceEvent(
                target, replaced, against, item, player, true, EquipmentSlot.HAND
        );

        Bukkit.getPluginManager().callEvent(probe);
        return !probe.isCancelled();
    }

    @SuppressWarnings("deprecation")
    private boolean canOwnerAffectEntityHere(Player owner, Entity target) {
        TL_PROBING_DAMAGE.set(true);
        try {
            EntityDamageByEntityEvent probeAttack = new EntityDamageByEntityEvent(
                    owner, target, EntityDamageEvent.DamageCause.ENTITY_ATTACK, 0.0
            );
            Bukkit.getPluginManager().callEvent(probeAttack);
            if (probeAttack.isCancelled()) return false;

            EntityDamageByEntityEvent probeExplosion = new EntityDamageByEntityEvent(
                    owner, target, EntityDamageEvent.DamageCause.ENTITY_EXPLOSION, 0.0
            );
            Bukkit.getPluginManager().callEvent(probeExplosion);
            return !probeExplosion.isCancelled();
        } finally {
            TL_PROBING_DAMAGE.set(false);
        }
    }

    // ------------------------- helpers -------------------------

    private boolean isManaged(Entity e) {
        Byte v = e.getPersistentDataContainer().get(keyManaged, PersistentDataType.BYTE);
        return v != null && v == (byte) 1;
    }

    private boolean getRule(Entity e, NamespacedKey key) {
        Byte v = e.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return v != null && v == (byte) 1;
    }

    private Player getOwnerOnline(Entity e) {
        String s = e.getPersistentDataContainer().get(keyOwner, PersistentDataType.STRING);
        if (s == null || s.isEmpty()) return null;
        try { return Bukkit.getPlayer(UUID.fromString(s)); } catch (Exception ignored) { return null; }
    }

    private Player getOwnerOnlineFromCloud(AreaEffectCloud cloud) {
        String s = cloud.getPersistentDataContainer().get(keyCloudOwner, PersistentDataType.STRING);
        if (s == null || s.isEmpty()) return null;
        try { return Bukkit.getPlayer(UUID.fromString(s)); } catch (Exception ignored) { return null; }
    }
}
