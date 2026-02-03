package su.nightexpress.excellentenchants.enchantment.bow;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Charges;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.BowEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.excellentenchants.protection.ProtectionManager;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.bukkit.NightItem;

import java.nio.file.Path;

public class BomberEnchant extends GameEnchantment implements BowEnchant {

    private Modifier fuseTicks;

    public BomberEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.addComponent(EnchantComponent.PROBABILITY, Probability.oneHundred());
        this.addComponent(EnchantComponent.CHARGES, Charges.custom(
                Modifier.addictive(50).perLevel(10).build(),
                1, 1,
                NightItem.fromType(Material.TNT)
        ));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.fuseTicks = Modifier.load(config, "Bomber.Fuse_Ticks",
                Modifier.addictive(40).perLevel(10).capacity(200),
                "设置 TNT 的引信时间（单位：tick，20 tick = 1 秒）。"
        );

        this.addPlaceholder(EnchantsPlaceholders.GENERIC_TIME, level -> NumberUtil.format((double) this.getFuseTicks(level) / 20D));
    }

    public int getFuseTicks(int level) {
        return (int) this.fuseTicks.getValue(level);
    }

    @Override
    @NotNull
    public EnchantPriority getShootPriority() {
        return EnchantPriority.LOWEST;
    }

    @Override
    public boolean onShoot(@NotNull EntityShootBowEvent event, @NotNull LivingEntity shooter, @NotNull ItemStack bow, int level) {
        if (!(event.getProjectile() instanceof Projectile projectile)) return false;

        int fuseTicks = Math.max(1, this.getFuseTicks(level));

        TNTPrimed primed = projectile.getWorld().spawn(projectile.getLocation(), TNTPrimed.class);
        primed.setVelocity(projectile.getVelocity().multiply(event.getForce() * 1.25));
        primed.setFuseTicks(fuseTicks);
        primed.setSource(shooter);

        // ✅ 标记这颗 TNT 走统一保护判定
        Player owner = (shooter instanceof Player p) ? p : null;
        ProtectionManager.markExplosionTnt(primed, owner);

        event.setProjectile(primed);
        return true;
    }
}