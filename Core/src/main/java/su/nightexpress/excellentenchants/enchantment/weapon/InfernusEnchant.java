package su.nightexpress.excellentenchants.enchantment.weapon;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.ArrowEffects;
import su.nightexpress.excellentenchants.api.enchantment.type.TridentEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.excellentenchants.protection.ProtectionManager;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;

import java.nio.file.Path;

public class InfernusEnchant extends GameEnchantment implements TridentEnchant {

    private Modifier fireTicks;

    public InfernusEnchant(@NotNull EnchantsPlugin plugin,
                           @NotNull EnchantManager manager,
                           @NotNull Path file,
                           @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        // 粒子拖尾
        this.addComponent(EnchantComponent.ARROW, ArrowEffects.basic(Particle.FLAME));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.fireTicks = Modifier.load(config, "Infernus.Fire_Ticks",
                Modifier.addictive(60).perLevel(20).capacity(120),
                "设置命中后目标被点燃的持续时间（单位：tick）。20 tick = 1 秒。");

        this.addPlaceholder(EnchantsPlaceholders.GENERIC_TIME,
                level -> NumberUtil.format((double) this.getFireTicks(level) / 20D));
    }

    public int getFireTicks(int level) {
        return (int) this.fireTicks.getValue(level);
    }

    @Override
    @NotNull
    public EnchantPriority getLaunchPriority() {
        return EnchantPriority.NORMAL;
    }

    @Override
    public boolean onLaunch(@NotNull ProjectileLaunchEvent event,
                            @NotNull LivingEntity shooter,
                            @NotNull ItemStack trident,
                            int level) {
        // 关键：不要再 setFireTicks(Integer.MAX_VALUE) 这种“硬点燃投掷物”
        // 只做视觉效果，不影响保护区逻辑
        event.getEntity().setVisualFire(true);
        return true;
    }

    @Override
    public void onHit(@NotNull ProjectileHitEvent event,
                      @NotNull LivingEntity shooter,
                      @NotNull Trident projectile,
                      int level) {

        Entity target = event.getHitEntity();
        if (target == null) return;

        int ticks = this.getFireTicks(level);

        // 保险1：保护探测（可选）
        if (shooter instanceof Player player) {
            if (!ProtectionManager.canAffectEntity(player, target)) {
                return;
            }
        }

        // 保险2：抛 combust 事件给保护插件拦截
        EntityCombustEvent combust = (shooter != null)
                ? new EntityCombustByEntityEvent(shooter, target, ticks)
                : new EntityCombustEvent(target, ticks);

        Bukkit.getPluginManager().callEvent(combust);
        if (combust.isCancelled()) return;

        // ✅ Paper 新 API：duration 是 float，转成 int 再 setFireTicks
        float d = combust.getDuration();
        int fire = Math.max(0, (int) Math.ceil(d));
        target.setFireTicks(fire);
    }

    @Override
    public void onDamage(@NotNull EntityDamageByEntityEvent event,
                         @NotNull LivingEntity shooter,
                         @NotNull LivingEntity victim,
                         @NotNull Trident projectile,
                         int level) {
        // 这里保持空即可（点燃逻辑已在 onHit 里处理）
    }
}
