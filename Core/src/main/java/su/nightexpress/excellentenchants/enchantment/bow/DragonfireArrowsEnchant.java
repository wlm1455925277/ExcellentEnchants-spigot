package su.nightexpress.excellentenchants.enchantment.bow;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;

import java.nio.file.Path;

public class DragonfireArrowsEnchant extends GameEnchantment implements ArrowEnchant {

    private Modifier duration;
    private Modifier radius;

    public DragonfireArrowsEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.addComponent(EnchantComponent.ARROW, ArrowEffects.basic(Particle.DRAGON_BREATH));
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(4, 3));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.duration = Modifier.load(config, "Dragonfire.Duration",
                Modifier.addictive(40).perLevel(20).capacity(60 * 20),
                "龙息云雾持续时间（tick）。"
        );

        this.radius = Modifier.load(config, "Dragonfire.Radius",
                Modifier.addictive(0).perLevel(1).capacity(5),
                "龙息云雾半径。"
        );

        this.addPlaceholder(EnchantsPlaceholders.GENERIC_DURATION, level -> NumberUtil.format(this.getFireDuration(level) / 20D));
        this.addPlaceholder(EnchantsPlaceholders.GENERIC_RADIUS, level -> NumberUtil.format(this.getFireRadius(level)));
    }

    public int getFireDuration(int level) {
        return (int) this.duration.getValue(level);
    }

    public double getFireRadius(int level) {
        return this.radius.getValue(level);
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
        // 命中实体：这里不做（由 onDamage 处理，避免重复）
        if (event.getHitEntity() != null) return;

        createCloud(shooter, arrow.getLocation(), null, event.getHitBlock(), event.getHitBlockFace(), level);
    }

    @Override
    public void onDamage(@NotNull EntityDamageByEntityEvent event, @NotNull LivingEntity shooter, @NotNull LivingEntity victim, @NotNull Arrow arrow, int level) {
        // 命中实体：在受害者位置生成
        createCloud(shooter, victim.getLocation(), victim, null, null, level);
    }

    private void createCloud(@NotNull ProjectileSource shooter,
                             @NotNull Location location,
                             @Nullable Entity hitEntity,
                             @Nullable Block hitBlock,
                             @Nullable BlockFace hitFace,
                             int level) {

        var world = location.getWorld();
        if (world == null) return;

        // ✅ 只允许玩家触发（更符合 PVP / 保护插件判断）
        if (!(shooter instanceof Player owner)) return;

        int dur = Math.max(1, this.getFireDuration(level));
        float rad = (float) Math.max(0D, this.getFireRadius(level));
        if (rad <= 0.0F) return;

        // ✅ 权限探测：
        // - 命中实体：用 canAffectEntity（更贴近“PVP/伤害/影响”语义）
        // - 命中方块/地面：用 canModifyAt（更贴近“是否允许在此处产生云/放置类效果”）
        if (hitEntity != null) {
            if (!ProtectionManager.canAffectEntity(owner, hitEntity)) return;
        } else {
            Block probe = (hitBlock != null) ? hitBlock : location.getBlock();
            if (!ProtectionManager.canModifyAt(owner, location, probe)) return;
        }

        AreaEffectCloud cloud = world.spawn(location, AreaEffectCloud.class);

        cloud.clearCustomEffects();

        // ✅ source 用玩家（LivingEntity），保护插件识别更可靠
        cloud.setSource(owner);

        cloud.setParticle(Particle.DRAGON_BREATH, 1F);

        cloud.setRadius(rad);
        cloud.setDuration(dur);

        // ✅ 半径随时间缩小（你原来的写法会越变越大）
        cloud.setRadiusPerTick(-rad / (float) dur);

        // Instant Damage 云：每次作用都很狠，保留你原设计
        cloud.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1), true);

        // ✅ 关键：打标记，让 ProtectionManager#onCloudApply 按领地/PVP过滤目标
        ProtectionManager.markCloud(cloud, owner);
    }
}
