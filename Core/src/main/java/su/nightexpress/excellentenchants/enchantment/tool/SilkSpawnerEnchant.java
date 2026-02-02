package su.nightexpress.excellentenchants.enchantment.tool;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.spawner.Spawner;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.EnchantsPlaceholders;
import su.nightexpress.excellentenchants.EnchantsPlugin;
import su.nightexpress.excellentenchants.api.EnchantPriority;
import su.nightexpress.excellentenchants.api.enchantment.component.EnchantComponent;
import su.nightexpress.excellentenchants.api.enchantment.meta.Probability;
import su.nightexpress.excellentenchants.api.enchantment.type.BlockEnchant;
import su.nightexpress.excellentenchants.api.enchantment.type.MiningEnchant;
import su.nightexpress.excellentenchants.enchantment.EnchantContext;
import su.nightexpress.excellentenchants.enchantment.GameEnchantment;
import su.nightexpress.excellentenchants.manager.EnchantManager;
import su.nightexpress.nightcore.config.ConfigValue;
import su.nightexpress.nightcore.config.FileConfig;
import su.nightexpress.nightcore.util.ItemUtil;
import su.nightexpress.nightcore.util.LangUtil;
import su.nightexpress.nightcore.util.LocationUtil;
import su.nightexpress.nightcore.util.text.night.wrapper.TagWrappers;

import java.nio.file.Path;
import java.util.HashSet;

public class SilkSpawnerEnchant extends GameEnchantment implements MiningEnchant, BlockEnchant {

    private String spawnerName;

    public SilkSpawnerEnchant(@NotNull EnchantsPlugin plugin, @NotNull EnchantManager manager, @NotNull Path file, @NotNull EnchantContext context) {
        super(plugin, manager, file, context);
        this.addComponent(EnchantComponent.PROBABILITY, Probability.addictive(0, 25));
    }

    @Override
    protected void loadAdditional(@NotNull FileConfig config) {
        this.spawnerName = ConfigValue.create("SilkSpawner.Name",
                TagWrappers.GREEN.wrap(
                        LangUtil.getSerializedName(Material.SPAWNER) + " " +
                                TagWrappers.GRAY.wrap("(" + TagWrappers.WHITE.wrap(EnchantsPlaceholders.GENERIC_TYPE) + ")")
                ),
                "刷怪笼物品的显示名称（Display Name）。",
                "可使用占位符 '" + EnchantsPlaceholders.GENERIC_TYPE + "' 来显示怪物名称。"
        ).read(config);
    }

    @NotNull
    public ItemStack getSpawner(@NotNull CreatureSpawner spawnerBlock) {
        ItemStack itemSpawner = new ItemStack(Material.SPAWNER);
        BlockStateMeta stateMeta = (BlockStateMeta) itemSpawner.getItemMeta();
        if (stateMeta == null || spawnerBlock.getSpawnedType() == null) return itemSpawner;

        CreatureSpawner spawnerItem = (CreatureSpawner) stateMeta.getBlockState();

        // 将方块刷怪笼的各项参数“拷贝”到物品刷怪笼（BlockStateMeta）中
        this.transferSettings(spawnerBlock, spawnerItem);
        spawnerItem.update(true);

        stateMeta.setBlockState(spawnerItem);

        // 设置显示名：把 %type%（GENERIC_TYPE）替换为刷怪类型的本地化名称
        ItemUtil.setCustomName(
                stateMeta,
                this.spawnerName.replace(
                        EnchantsPlaceholders.GENERIC_TYPE,
                        LangUtil.getSerializedName(spawnerBlock.getSpawnedType())
                )
        );

        itemSpawner.setItemMeta(stateMeta);

        this.manager.setBlockEnchant(itemSpawner, this);
        return itemSpawner;
    }

    @Override
    @NotNull
    public EnchantPriority getBreakPriority() {
        return EnchantPriority.LOW;
    }

    @Override
    public boolean canPlaceInContainers() {
        // 允许把“丝绸刷怪笼物品”放进容器（箱子/潜影盒等）
        return true;
    }

    @Override
    public boolean onBreak(@NotNull BlockBreakEvent event, @NotNull LivingEntity player, @NotNull ItemStack item, int level) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof CreatureSpawner spawner)) return false;

        // 刷怪笼不掉经验
        event.setExpToDrop(0);

        // 需要开启 setDropItems(true)，否则刷怪笼不会触发掉落相关事件/流程
        event.setDropItems(true);

        Location location = LocationUtil.setCenter3D(block.getLocation());
        World world = block.getWorld();

        // 直接在世界中掉落（因为刷怪笼在某些情况下 BlockDropItemEvent 不会触发，除非 setDropItems=true）
        world.dropItemNaturally(location, this.getSpawner(spawner));
        return true;
    }

    @Override
    public void onPlace(@NotNull BlockPlaceEvent event, @NotNull Player player, @NotNull Block block, @NotNull ItemStack itemStack) {
        if (block.getType() != Material.SPAWNER) return;
        if (!(itemStack.getItemMeta() instanceof BlockStateMeta stateMeta)) return;

        CreatureSpawner spawnerItem = (CreatureSpawner) stateMeta.getBlockState();
        CreatureSpawner spawnerBlock = (CreatureSpawner) block.getState();

        // 把物品中的刷怪笼设置还原到方块刷怪笼中
        this.transferSettings(spawnerItem, spawnerBlock);
        spawnerBlock.update(true);
    }

    private void transferSettings(@NotNull Spawner from, @NotNull Spawner to) {
        // 清空潜在刷怪列表（避免旧数据叠加）
        to.setPotentialSpawns(new HashSet<>());

        // 如果没有潜在刷怪列表，就只设置单一刷怪类型
        if (from.getPotentialSpawns().isEmpty()) {
            to.setSpawnedType(from.getSpawnedType());
        }
        // 否则把潜在刷怪列表完整复制过去（可支持多种刷怪权重/规则）
        else {
            from.getPotentialSpawns().forEach(to::addPotentialSpawn);
        }

        // 复制刷怪笼的各项参数
        to.setDelay(from.getMinSpawnDelay());
        to.setMinSpawnDelay(from.getMinSpawnDelay());
        to.setMaxSpawnDelay(from.getMaxSpawnDelay());
        to.setMaxNearbyEntities(from.getMaxNearbyEntities());
        to.setRequiredPlayerRange(from.getRequiredPlayerRange());
        to.setSpawnCount(from.getSpawnCount());
        to.setSpawnRange(from.getSpawnRange());
    }
}
