package su.nightexpress.excellentenchants.manager.listener;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.excellentenchants.config.Config;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.manager.AbstractListener;
import su.nightexpress.nightcore.util.PDCUtil;

public class GenericListener extends AbstractListener<EnchantsPlugin> {

    private final EnchantManager manager;
    private final org.bukkit.NamespacedKey ghastKey;
    private final org.bukkit.NamespacedKey ghastFireKey;

    public GenericListener(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager) {
        super(plugin);
        this.manager = manager;
        // Keep in sync with GhastEnchant tags.
        this.ghastKey = new org.bukkit.NamespacedKey(plugin, "ghast_enchant");
        this.ghastFireKey = new org.bukkit.NamespacedKey(plugin, "ghast_fire");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        this.manager.setSpawnReason(event.getEntity(), event.getSpawnReason());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChargesFillOnEnchant(EnchantItemEvent event) {
        if (!Config.isChargesEnabled()) return;

        this.plugin.runTask(() -> {
            Inventory inventory = event.getInventory();

            ItemStack result = inventory.getItem(0);
            if (result == null) return;

            event.getEnchantsToAdd().forEach((enchantment, level) -> {
                EnchantsUtils.restoreCharges(result, enchantment, level);
            });

            inventory.setItem(0, result);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTickedBlockBreak(BlockBreakEvent event) {
        if (this.manager.removeTickedBlock(event.getBlock())) {
            event.setDropItems(false);
            event.setExpToDrop(0);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTickedBlockTNTExplode(BlockExplodeEvent event) {
        event.blockList().forEach(this.manager::removeTickedBlock);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onTickedBlockEntityExplode(EntityExplodeEvent event) {
        event.blockList().forEach(this.manager::removeTickedBlock);

        if (event.getEntity() instanceof LivingEntity entity) {
            this.manager.handleEnchantExplosion(event, entity);
        }

        if (event.getEntity() instanceof Fireball fireball && fireball.getShooter() instanceof Player player) {
            if (PDCUtil.getInt(fireball, this.ghastKey).orElse(0) > 0) {
                event.blockList().removeIf(block -> !this.canBreak(player, block));

                boolean fireSpread = PDCUtil.getInt(fireball, this.ghastFireKey).orElse(0) == 1;
                if (fireSpread) this.spreadControlledFire(event, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onExplosionDamage(EntityDamageByEntityEvent event) {
        DamageSource source = event.getDamageSource();
        DamageType type = source.getDamageType();

        if (type != DamageType.PLAYER_EXPLOSION && type != DamageType.EXPLOSION) return;
        if (!(source.getCausingEntity() instanceof LivingEntity entity)) return;

        this.manager.handleEnchantExplosionDamage(event, entity);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onGhastFireballDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Fireball fireball)) return;
        if (!(fireball.getShooter() instanceof Player player)) return;
        if (PDCUtil.getInt(fireball, this.ghastKey).orElse(0) <= 0) return;

        if (event.getEntity() instanceof Hanging hanging) {
            if (!this.canBreakHanging(player, hanging)) {
                event.setCancelled(true);
            }
            return;
        }

        if (event.getEntity() instanceof Item || event.getEntity() instanceof ItemFrame) {
            event.setCancelled(true);
        }
    }

    private boolean canBreak(@NotNull Player player, @NotNull Block block) {
        BlockBreakEvent probe = new BlockBreakEvent(block, player);
        this.plugin.getPluginManager().callEvent(probe);
        return !probe.isCancelled();
    }

    private boolean canBreakHanging(@NotNull Player player, @NotNull Hanging hanging) {
        HangingBreakByEntityEvent probe = new HangingBreakByEntityEvent(hanging, player);
        this.plugin.getPluginManager().callEvent(probe);
        return !probe.isCancelled();
    }

    private boolean canIgnite(@NotNull Player player, @NotNull Block block) {
        BlockIgniteEvent igniteEvent = new BlockIgniteEvent(block, BlockIgniteEvent.IgniteCause.EXPLOSION, player);
        this.plugin.getPluginManager().callEvent(igniteEvent);
        return !igniteEvent.isCancelled();
    }

    private void spreadControlledFire(@NotNull EntityExplodeEvent event, @NotNull Player player) {
        // Limit fire count to reduce packet spam and griefing.
        int placed = 0;
        for (Block block : event.blockList()) {
            for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                if (placed >= 6) return;
                Block relative = block.getRelative(face);
                if (!relative.getType().isAir()) continue;
                if (!this.canIgnite(player, relative)) continue;

                relative.setType(org.bukkit.Material.FIRE, false);
                placed++;
            }
        }
    }
}
