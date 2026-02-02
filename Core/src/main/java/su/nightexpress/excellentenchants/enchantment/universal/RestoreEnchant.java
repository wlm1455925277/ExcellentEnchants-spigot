package su.nightexpress.excellentenchants.enchantment.universal;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.Modifier;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.DurabilityEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.NumberUtil;
import su.nightexpress.nightcore.util.sound.VanillaSound;

import java.nio.file.Path;

public class RestoreEnchant extends GameEnchantment implements DurabilityEnchant {

    /** 复原比例（百分比）：按“最大耐久的百分比”来设置恢复到什么程度 */
    private Modifier amount;

    public RestoreEnchant(@NotNull EnchantsPlugin plugin,
                          @NotNull EnchantManager manager,
                          @NotNull Path file,
                          @NotNull EnchantContext context) {
        super(plugin, manager, file, context);

        // 概率组件：常见理解为基础 20%，每级 +5%（以框架实现为准）
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(20, 5));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        // Restore.Amount：当物品即将损坏时，把耐久“恢复到”最大耐久的一定百分比
        // 例如 Amount=15：表示恢复 15% 的耐久（也就是把 damage 设置为 85% 的 maxDurability）
        this.amount = Modifier.load(config, "Restore.Amount",
                Modifier.addictive(15).perLevel(5).capacity(100),
                "当物品即将损坏时，恢复的耐久百分比（按物品最大耐久的百分比计算）。"
        );

        // 占位符：显示 Amount 数值
        this.addPlaceholder(EnchantsPlaceholders.GENERIC_AMOUNT, level -> NumberUtil.format(this.getAmount(level)));
    }

    public double getAmount(int level) {
        return this.amount.getValue(level);
    }

    @Override
    @NotNull
    public EnchantPriority getItemDamagePriority() {
        // MONITOR：通常意味着“最后观察阶段”，常用于在最终结果基础上做判断（但注意是否允许取消事件取决于框架/事件设计）
        return EnchantPriority.MONITOR;
    }

    @Override
    public boolean onItemDamage(@NotNull PlayerItemDamageEvent event,
                                @NotNull Player player,
                                @NotNull ItemStack itemStack,
                                int level) {

        // 物品必须有 Damageable meta（可损坏）
        if (!(itemStack.getItemMeta() instanceof Damageable damageable)) return false;

        int damage = event.getDamage();
        int maxDurability = itemStack.getType().getMaxDurability();

        // 如果“本次损伤后仍不会碎”，就不触发
        // damageable.getDamage() 是当前已损伤值，越大越接近碎
        if (damageable.getDamage() + damage < maxDurability) return false;

        // 触发：阻止物品破碎
        event.setCancelled(true);

        // 计算：把物品的 damage 设置为 maxDurability 的 (100 - Amount)%，
        // 也就是把“剩余耐久”恢复为 Amount%
        double damagePercent = 100D - this.getAmount(level);
        int restoredDamage = (int) (maxDurability * (damagePercent / 100D));

        // 设置新的损伤值（相当于“救回一命”）
        damageable.setDamage(restoredDamage);

        // 移除这个附魔（一次性触发，用完即消失）
        damageable.removeEnchant(this.getBukkitEnchantment());

        // 写回物品
        itemStack.setItemMeta(damageable);

        // 视觉/音效：图腾使用声
        if (this.hasVisualEffects()) {
            VanillaSound.of(Sound.ITEM_TOTEM_USE).play(event.getPlayer());
        }
        return true;
    }
}
