package su.nightexpress.excellentenchants.enchantment.universal;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.KillEnchant;
import su.nightexpress.excellentenchants.api.enchantment.type.MiningEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;

import java.nio.file.Path;

public class CurseOfMisfortuneEnchant extends GameEnchantment implements MiningEnchant, KillEnchant {

    /** 是否允许掉落经验（默认 false：不掉经验） */
    private boolean dropXP;

    public CurseOfMisfortuneEnchant(@NotNull EnchantsPlugin plugin,
                                    @NotNull EnchantManager manager,
                                    @NotNull Path file,
                                    @NotNull EnchantContext context) {
        super(plugin, manager, file, context);

        // 概率组件：通常理解为基础 0%，每级 +7%（以 Probability 实现为准）
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(0, 7));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        // 配置：是否掉落经验
        // false：既不掉落物品，也不掉经验
        // true：不掉落物品，但仍掉经验
        this.dropXP = ConfigValue.create("CurseOfMisfortune.Drop_XP",
                false,
                "是否允许掉落经验（XP）。"
        ).read(config);
    }

    public boolean isDropXP() {
        return this.dropXP;
    }

    @Override
    @NotNull
    public EnchantPriority getBreakPriority() {
        // 最高优先级：尽可能“最先/最强硬”地处理方块掉落
        return EnchantPriority.HIGHEST;
    }

    @NotNull
    @Override
    public EnchantPriority getKillPriority() {
        // 最高优先级：尽可能“最先/最强硬”地处理实体死亡掉落
        return EnchantPriority.HIGHEST;
    }

    @Override
    public boolean onBreak(@NotNull BlockBreakEvent event,
                           @NotNull LivingEntity player,
                           @NotNull ItemStack item,
                           int level) {

        // 破坏方块时：直接禁止方块掉落物品
        event.setDropItems(false);

        // 如果不允许掉经验，则经验也归零
        if (!this.dropXP) event.setExpToDrop(0);

        return true;
    }

    @Override
    public boolean onKill(@NotNull EntityDeathEvent event,
                          @NotNull LivingEntity entity,
                          @NotNull Player killer,
                          @NotNull ItemStack weapon,
                          int level) {

        // 击杀实体时：清空死亡掉落列表（不掉落任何物品）
        event.getDrops().clear();

        // 如果不允许掉经验，则死亡经验也归零
        if (!this.dropXP) event.setDroppedExp(0);

        return true;
    }
}
