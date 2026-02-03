package su.nightexpress.excellentenchants.enchantment.bow;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.EnchantsUtils;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.BowEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.PDCUtil;

import java.nio.file.Path;

public class GhastEnchant extends GameEnchantment implements BowEnchant {

    private boolean  fireSpread;
    private Modifier yield;
    private final NamespacedKey ghastKey;
    private final NamespacedKey ghastFireKey;

    public GhastEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.ghastKey = new NamespacedKey(plugin, "ghast_enchant");
        this.ghastFireKey = new NamespacedKey(plugin, "ghast_fire");
        this.addComponent(EnchantComponent.PROBABILITY, Probability.oneHundred());
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.fireSpread = ConfigValue.create("Fireball.Fire_Spread",
                true,
                "控制火球爆炸是否会点燃附近方块（引燃火焰）。"
        ).read(config);

        this.yield = Modifier.load(config, "Fireball.Yield",
                Modifier.addictive(2).perLevel(0).capacity(5),
                "火球爆炸威力（强度）。"
        );
    }

    public boolean isFireSpread() {
        return this.fireSpread;
    }

    public float getYield(int level) {
        return (float) this.yield.getValue(level);
    }

    @Override
    @NotNull
    public EnchantPriority getShootPriority() {
        return EnchantPriority.LOWEST;
    }

    @Override
    public boolean onShoot(@NotNull EntityShootBowEvent event, @NotNull LivingEntity shooter, @NotNull ItemStack bow, int level) {
        if (!(event.getProjectile() instanceof Projectile projectile)) return false;

        Fireball fireball;

        // 若弓上存在“多重射击”（Multishot）附魔，则发射小火球：
        // 大火球速度较慢，并且多发时会在射出瞬间互相碰撞/顶开。
        if (EnchantsUtils.contains(bow, Enchantment.MULTISHOT)) {
            fireball = shooter.launchProjectile(SmallFireball.class);
            fireball.setVelocity(projectile.getVelocity().normalize().multiply(0.5f));
        }
        else {
            fireball = shooter.launchProjectile(Fireball.class);
            fireball.setDirection(projectile.getVelocity());
        }
        fireball.setIsIncendiary(false); // manual, permission-checked ignition happens in listener
        fireball.setYield(this.getYield(level));

        // Mark so listeners can apply region checks and controlled fire spread.
        PDCUtil.set(fireball, this.ghastKey, level);
        PDCUtil.set(fireball, this.ghastFireKey, this.fireSpread ? 1 : 0);

        event.setProjectile(fireball);
        return true;
    }

    @NotNull
    public NamespacedKey getGhastKey() {
        return this.ghastKey;
    }

    @NotNull
    public NamespacedKey getGhastFireKey() {
        return this.ghastFireKey;
    }
}
