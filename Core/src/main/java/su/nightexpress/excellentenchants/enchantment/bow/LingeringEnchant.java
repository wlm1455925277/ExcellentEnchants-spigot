package su.nightexpress.excellentenchants.enchantment.bow;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.ArrowEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.excellentenchants.protection.ProtectionManager;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.ItemUtil;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class LingeringEnchant extends GameEnchantment implements ArrowEnchant {

    public LingeringEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(5, 5));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        // 原版没配置项，保持空
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
        // 只在命中方块/地面时触发（保持你原逻辑）
        if (event.getHitEntity() != null) return;

        createCloud(
                arrow,
                shooter,
                arrow.getLocation(),
                null,
                event.getHitBlock(),
                event.getHitBlockFace()
        );
    }

    @Override
    public void onDamage(@NotNull EntityDamageByEntityEvent event, @NotNull LivingEntity shooter, @NotNull LivingEntity victim, @NotNull Arrow arrow, int level) {
        // 原版这里为空，保持
    }

    private void createCloud(@NotNull Arrow arrow,
                             @NotNull ProjectileSource shooter,
                             @NotNull Location location,
                             @Nullable Entity hitEntity,
                             @Nullable Block hitBlock,
                             @Nullable BlockFace hitFace) {

        var world = location.getWorld();
        if (world == null) return;

        // ✅ 只允许玩家触发（更符合保护插件的“谁造成效果”判断）
        if (!(shooter instanceof Player owner)) return;

        // ✅ 权限探测（落点生成云：按“能否修改/放置”语义）
        Block probe = (hitBlock != null) ? hitBlock : location.getBlock();
        if (!ProtectionManager.canModifyAt(owner, location, probe)) return;

        // 收集箭上的药水效果
        Set<PotionEffect> effects = new HashSet<>();
        if (arrow.hasCustomEffects()) effects.addAll(arrow.getCustomEffects());
        if (arrow.getBasePotionType() != null) effects.addAll(arrow.getBasePotionType().getPotionEffects());
        if (effects.isEmpty()) return;

        // 生成一个“事件用”的 lingering potion item
        ItemStack item = new ItemStack(Material.LINGERING_POTION);
        ItemUtil.editMeta(item, meta -> {
            if (meta instanceof PotionMeta potionMeta) {
                effects.forEach(pe -> potionMeta.addCustomEffect(pe, true));
                if (arrow.getBasePotionType() != null) potionMeta.setBasePotionType(arrow.getBasePotionType());
            }
        });

        // ✅ 直接 spawn 一个 ThrownPotion（不发射不乱飞），只用于构造 LingeringPotionSplashEvent
        ThrownPotion potion = world.spawn(location, ThrownPotion.class, p -> {
            p.setItem(item);
            p.setShooter(owner);
            p.setVelocity(new Vector(0, 0, 0));
        });

        // ✅ 生成云
        AreaEffectCloud cloud = world.spawn(location, AreaEffectCloud.class);
        cloud.clearCustomEffects();

        cloud.setSource(owner); // source 用玩家，保护插件更好识别
        cloud.setWaitTime(10);
        cloud.setRadius(3F);
        cloud.setRadiusOnUse(-0.5F);
        cloud.setDuration(600);
        cloud.setRadiusPerTick(-cloud.getRadius() / (float) cloud.getDuration());

        if (arrow.getBasePotionType() != null) cloud.setBasePotionType(arrow.getBasePotionType());
        effects.forEach(pe -> cloud.addCustomEffect(pe, false));

        // ✅ 关键：先 mark，让你的 ProtectionManager 在 apply 时过滤受影响实体
        ProtectionManager.markCloud(cloud, owner);

        // ✅ 再抛事件，让外部保护插件有机会整体取消
        LingeringPotionSplashEvent splashEvent = new LingeringPotionSplashEvent(potion, hitEntity, hitBlock, hitFace, cloud);
        plugin.getPluginManager().callEvent(splashEvent);
        if (splashEvent.isCancelled()) {
            cloud.remove();
        }

        // 清理事件用实体
        potion.remove();
    }
}
