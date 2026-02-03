package su.nightexpress.excellentenchants.enchantment.weapon;

import org.bukkit.attribute.Attribute;
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
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.EntityUtil;
import su.nightexpress.nightcore.util.NumberUtil;

import java.nio.file.Path;

public class TemperEnchant extends GameEnchantment implements AttackEnchant {

    /** 每一档增加的伤害百分比（%） */
    private Modifier damageAmount;

    /** 每损失 X% 最大生命值触发一档（%） */
    private Modifier damageStepPercent;

    public TemperEnchant(@NotNull EnchantsPlugin plugin,
                         @NotNull EnchantManager manager,
                         @NotNull Path file,
                         @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {

        // 目标：每损失 2% 最大生命值 -> 伤害 +1%
        // damageAmount：默认 1%，不随等级增长（想随等级增再改 perLevel）
        this.damageAmount = Modifier.load(config, "Temper.Damage_Amount",
                Modifier.addictive(1.0).perLevel(0).capacity(100),
                "每触发一档额外伤害（百分比 %）。",
                "默认：每触发 1 档，伤害 +1%。"
        );

        // damageStepPercent：默认 2%，不随等级增长
        this.damageStepPercent = Modifier.load(config, "Settings.Damage.Step",
                Modifier.addictive(2.0).perLevel(0).capacity(100),
                "每损失 X% 最大生命值就提高一次伤害倍率，X 即为该数值。",
                "默认：每损失 2% 最大生命值（Max HP）触发 1 档。"
        );

        // amount = 每档加成（%）
        this.addPlaceholder(EnchantsPlaceholders.GENERIC_AMOUNT,
                level -> NumberUtil.format(this.getDamageAmount(level)));

        // radius：这里沿用原 placeholder，但含义为 stepPercent（%）
        this.addPlaceholder(EnchantsPlaceholders.GENERIC_RADIUS,
                level -> NumberUtil.format(this.getDamageStepPercent(level)));
    }

    public double getDamageAmount(int level) {
        return this.damageAmount.getValue(level);
    }

    /** 每档阈值：损失最大生命值的百分比（%） */
    public double getDamageStepPercent(int level) {
        return this.damageStepPercent.getValue(level);
    }

    @Override
    @NotNull
    public EnchantPriority getAttackPriority() {
        return EnchantPriority.LOWEST;
    }

    @Override
    public boolean onAttack(@NotNull EntityDamageByEntityEvent event,
                            @NotNull LivingEntity damager,
                            @NotNull LivingEntity victim,
                            @NotNull ItemStack weapon,
                            int level) {

        double maxHealth = EntityUtil.getAttributeValue(damager, Attribute.MAX_HEALTH);
        if (maxHealth <= 0D) return false;

        double health = damager.getHealth();
        if (health >= maxHealth) return false;

        // 损失百分比（0~100）
        double missingPercent = ((maxHealth - health) / maxHealth) * 100D;

        double stepPercent = this.getDamageStepPercent(level);
        if (stepPercent <= 0D || missingPercent < stepPercent) return false;

        double steps = Math.floor(missingPercent / stepPercent);
        if (steps <= 0D) return false;

        // 伤害倍率 = 1 + (每档加成% * 档数 / 100)
        double multiplier = 1D + (this.getDamageAmount(level) * steps / 100D);

        event.setDamage(event.getDamage() * multiplier);
        return true;
    }
}
