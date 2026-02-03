package su.nightexpress.excellentenchants.enchantment.bow;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.ArrowEffects;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.ArrowEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.excellentenchants.protection.ProtectionManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;

import java.nio.file.Path;

public class ExplosiveArrowsEnchant extends GameEnchantment implements ArrowEnchant {

    private boolean fireSpread;
    private boolean damageItems;
    private boolean damageBlocks;
    private Modifier power;

    public ExplosiveArrowsEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.addComponent(EnchantComponent.ARROW, ArrowEffects.basic(Particle.SMOKE));
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(3, 2));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.fireSpread = ConfigValue.create("Explosion.Fire_Spread", true).read(config);
        this.damageItems = ConfigValue.create("Explosion.Damage_Items", false).read(config);
        this.damageBlocks = ConfigValue.create("Explosion.Damage_Blocks", false).read(config);

        this.power = Modifier.load(config, "Explosion.Power",
                Modifier.addictive(1).perLevel(1).capacity(5)
        );

        this.addPlaceholder(EnchantsPlaceholders.GENERIC_RADIUS, level -> NumberUtil.format(this.getPower(level)));
    }

    public final double getPower(int level) {
        return this.power.getValue(level);
    }

    @Override
    @NotNull
    public EnchantPriority getShootPriority() {
        return EnchantPriority.NORMAL;
    }

    @Override
    public boolean onShoot(@NotNull EntityShootBowEvent event, @NotNull LivingEntity shooter, @NotNull ItemStack bow, int level) {
        return true;
    }

    @Override
    public void onHit(@NotNull ProjectileHitEvent event, @NotNull LivingEntity shooter, @NotNull Arrow arrow, int level) {
        NamespacedKey onceKey = new NamespacedKey(this.plugin, "ee_explosive_once");
        if (arrow.getPersistentDataContainer().has(onceKey, PersistentDataType.BYTE)) return;
        arrow.getPersistentDataContainer().set(onceKey, PersistentDataType.BYTE, (byte) 1);

        Location loc = arrow.getLocation();
        float power = (float) this.getPower(level);

        Player owner = (shooter instanceof Player p) ? p : null;

        ArmorStand marker = loc.getWorld().spawn(loc, ArmorStand.class, as -> {
            as.setInvisible(true);
            as.setMarker(true);
            as.setInvulnerable(true);
            as.setSilent(true);
            as.setGravity(false);
            as.setPersistent(false);
            as.setRemoveWhenFarAway(true);
        });

        ProtectionManager.markExplosionSource(
                marker,
                owner,
                true,
                true,
                true,
                this.damageBlocks,
                this.damageItems
        );

        loc.getWorld().createExplosion(loc, power, this.fireSpread, this.damageBlocks, marker);

        marker.remove();
        arrow.remove();
    }

    @Override
    public void onDamage(@NotNull EntityDamageByEntityEvent event, @NotNull LivingEntity shooter, @NotNull LivingEntity victim, @NotNull Arrow arrow, int level) {
    }
}
