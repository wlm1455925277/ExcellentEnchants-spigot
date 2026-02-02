package su.nightexpress.excellentenchants.enchantment.universal;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
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

import java.nio.file.Path;

public class CurseOfBreakingEnchant extends GameEnchantment implements DurabilityEnchant {

    /** 额外扣除的耐久点数（每次触发在原有损耗基础上再加） */
    private Modifier durabilityAmount;

    public CurseOfBreakingEnchant(@NotNull EnchantsPlugin plugin,
                                  @NotNull EnchantManager manager,
                                  @NotNull Path file,
                                  @NotNull EnchantContext context) {
        super(plugin, manager, file, context);

        // 概率组件：Probability.addictive(0, 10)
        // 一般表示：基础 0%，每级 +10%（具体算法以 Probability 实现为准）
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(0, 10));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        // 配置读取：每次触发额外损耗多少点耐久
        // 默认：基础0，每级+1，最大5
        this.durabilityAmount = Modifier.load(config, "CurseOfBreaking.Amount",
                Modifier.addictive(0).perLevel(1).capacity(5),
                "每次触发时额外扣除的耐久点数。"
        );

        // 占位符：%amount% 显示当前等级额外扣多少
        this.addPlaceholder(EnchantsPlaceholders.GENERIC_AMOUNT,
                level -> NumberUtil.format(this.getDurabilityAmount(level)));
    }

    /** 当前等级额外扣耐久点数 */
    public int getDurabilityAmount(int level) {
        return (int) this.durabilityAmount.getValue(level);
    }

    @Override
    @NotNull
    public EnchantPriority getItemDamagePriority() {
        // 物品耐久结算阶段的优先级：NORMAL
        return EnchantPriority.NORMAL;
    }

    @Override
    public boolean onItemDamage(@NotNull PlayerItemDamageEvent event,
                                @NotNull Player player,
                                @NotNull ItemStack itemStack,
                                int level) {

        // 如果这次耐久损耗为 0（例如被某些机制取消/保护/不消耗），则不处理
        if (event.getDamage() == 0) return false;

        // 计算额外耐久损耗
        int durabilityAmount = this.getDurabilityAmount(level);
        if (durabilityAmount <= 0) return false;

        // 在原本损耗基础上再加：等于“更容易坏”的诅咒
        event.setDamage(event.getDamage() + durabilityAmount);
        return true;
    }
}
