package su.nightexpress.excellentenchants.enchantment.weapon;

import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.type.AttackEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.Lists;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.wrapper.UniParticle;

import java.nio.file.Path;
import java.util.Set;

public class BaneOfNetherspawnEnchant extends GameEnchantment implements AttackEnchant {

    private static final Set<EntityType> ENTITY_TYPES = Lists.newSet(
            EntityType.BLAZE, EntityType.MAGMA_CUBE,
            EntityType.WITHER_SKELETON, EntityType.GHAST, EntityType.WITHER,
            EntityType.PIGLIN, EntityType.PIGLIN_BRUTE,
            EntityType.ZOGLIN, EntityType.HOGLIN,
            EntityType.STRIDER, EntityType.ZOMBIFIED_PIGLIN
    );

    private boolean  multiplier;
    private Modifier damageMod;

    public BaneOfNetherspawnEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.multiplier = ConfigValue.create("BaneOfNetherspawn.Damage.Multiplier",
                false,
                "当为 'true' 时：按倍率方式计算伤害（用系数乘上原伤害）。当为 'false' 时：直接在原伤害基础上增加固定值。"
        ).read(config);

        this.damageMod = Modifier.load(config, "BaneOfNetherspawn.Damage.Amount",
                Modifier.addictive(0.75).perLevel(0.25).capacity(1000D),
                "额外伤害数值。"
        );

        this.addPlaceholder(EnchantsPlaceholders.GENERIC_DAMAGE, level -> NumberUtil.format(this.getDamageAmount(level)));
    }

    public double getDamageAmount(int level) {
        return this.damageMod.getValue(level);
    }

    @Override
    @NotNull
    public EnchantPriority getAttackPriority() {
        return EnchantPriority.LOW;
    }

    @Override
    public boolean onAttack(@NotNull EntityDamageByEntityEvent event, @NotNull LivingEntity damager, @NotNull LivingEntity victim, @NotNull ItemStack weapon, int level) {
        if (!ENTITY_TYPES.contains(victim.getType())) return false;

        double damageEvent = event.getDamage();
        double damageAdd = this.getDamageAmount(level);

        // multiplier=true：伤害 = 原伤害 * damageAdd
        // multiplier=false：伤害 = 原伤害 + damageAdd
        event.setDamage(this.multiplier ? damageEvent * damageAdd : damageEvent + damageAdd);

        if (this.hasVisualEffects()) {
            UniParticle.of(Particle.SMOKE).play(victim.getEyeLocation(), 0.25, 0.1, 4);
            UniParticle.of(Particle.LAVA).play(victim.getEyeLocation(), 0.25, 0.1, 1);
        }

        return true;
    }
}
