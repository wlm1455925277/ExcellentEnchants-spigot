package su.nightexpress.excellentenchants.api.item;

import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.NotNull;
import su.nightexpress.excellentenchants.bridge.ItemTagLookup;
import su.nightexpress.nightcore.util.Lists;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public enum ItemSetDefaults {

    HELMET(tagLookup -> ItemSet.buildByName("helmet", tagLookup.getHelmets()).slots(EquipmentSlot.HEAD).name("头盔").build()),
    CHESTPLATE(tagLookup -> ItemSet.buildByName("chestplate", tagLookup.getChestplates()).slots(EquipmentSlot.CHEST).name("胸甲").build()),
    LEGGINGS(tagLookup -> ItemSet.buildByName("leggings", tagLookup.getLeggings()).slots(EquipmentSlot.LEGS).name("护腿").build()),
    BOOTS(tagLookup -> ItemSet.buildByName("boots", tagLookup.getBoots()).slots(EquipmentSlot.FEET).name("靴子").build()),
    ELYTRA(tagLookup -> ItemSet.buildByType("elytra", Material.ELYTRA).slots(EquipmentSlot.CHEST).name("鞘翅").build()),
    SWORD(tagLookup -> ItemSet.buildByName("sword", tagLookup.getSwords()).slots(EquipmentSlot.HAND).name("剑").build()),
    AXE(tagLookup -> ItemSet.buildByName("axe", tagLookup.getAxes()).slots(EquipmentSlot.HAND).name("斧").build()),
    HOE(tagLookup -> ItemSet.buildByName("hoe", tagLookup.getHoes()).slots(EquipmentSlot.HAND).name("锄").build()),
    PICKAXE(tagLookup -> ItemSet.buildByName("pickaxe", tagLookup.getPickaxes()).slots(EquipmentSlot.HAND).name("镐").build()),
    SHOVEL(tagLookup -> ItemSet.buildByName("shovel", tagLookup.getShovels()).slots(EquipmentSlot.HAND).name("铲").build()),
    TRIDENT(tagLookup -> ItemSet.buildByType("trident", Material.TRIDENT).slots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND).name("三叉戟").build()),
    BOW(tagLookup -> ItemSet.buildByType("bow", Material.BOW).slots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND).name("弓").build()),
    CROSSBOW(tagLookup -> ItemSet.buildByType("crossbow", Material.CROSSBOW).slots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND).name("弩").build()),
    FISHING_ROD(tagLookup -> ItemSet.buildByType("fishing_rod", Material.FISHING_ROD).slots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND).name("钓鱼竿").build()),
    SHIELD(tagLookup -> ItemSet.buildByType("shield", Material.SHIELD).slots(EquipmentSlot.OFF_HAND).name("盾牌").build()),
    BREAKABLE(tagLookup -> ItemSet.buildByName("breakable", tagLookup.getBreakable()).slots(EquipmentSlot.values()).name("可损坏物品").build()),
    ARMOR(tagLookup -> ItemSet.buildByName("armor", getArmors(tagLookup)).slots(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET).name("护甲").build()),
    TOOL(tagLookup -> ItemSet.buildByName("tool", getTools(tagLookup)).slots(EquipmentSlot.HAND).name("工具").build()),
    SWORDS_AXES(tagLookup -> ItemSet.buildByName("swords_axes", getSwordsAxes(tagLookup)).slots(EquipmentSlot.HAND).name("武器").build()),
    BOW_CROSSBOW(tagLookup -> ItemSet.buildByType("bow_crossbow", Lists.newSet(Material.BOW, Material.CROSSBOW)).slots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND).name("弓 / 弩").build()),
    CHESTPLATE_ELYTRA(tagLookup -> ItemSet.buildByName("chestplate_elytra", getTorso(tagLookup)).slots(EquipmentSlot.CHEST).name("胸甲 / 鞘翅").build()),
    ALL_WEAPON(tagLookup -> ItemSet.buildByName("weapon", getAllRangeWeapon(tagLookup)).slots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND).name("武器").build()),
    MINING_TOOLS(tagLookup -> ItemSet.buildByName("mining_tools", getMiningTools(tagLookup)).slots(EquipmentSlot.HAND).name("挖掘工具").build()),
    TOOLS_WEAPONS(tagLookup -> ItemSet.buildByName("tools_weapons", getToolsAndWeapon(tagLookup)).slots(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND).name("工具与武器").build())
    ;

    public static void initializeAll(@NotNull ItemTagLookup tagLookup) {
        stream().forEach(set -> set.initialize(tagLookup));
    }

    @NotNull
    public static Stream<ItemSetDefaults> stream() {
        return Stream.of(values());
    }

    public static void clearAll() {
        for (ItemSetDefaults value : values()) {
            value.clear();
        }
    }

    @NotNull
    private static Set<String> getArmors(@NotNull ItemTagLookup tagLookup) {
        Set<String> materials = new HashSet<>();
        materials.addAll(tagLookup.getHelmets());
        materials.addAll(tagLookup.getChestplates());
        materials.addAll(tagLookup.getLeggings());
        materials.addAll(tagLookup.getBoots());
        return materials;
    }

    @NotNull
    private static Set<String> getTools(@NotNull ItemTagLookup tagLookup) {
        Set<String> materials = new HashSet<>();
        materials.addAll(tagLookup.getAxes());
        materials.addAll(tagLookup.getHoes());
        materials.addAll(tagLookup.getPickaxes());
        materials.addAll(tagLookup.getShovels());
        materials.add(Material.SHEARS.name());
        return materials;
    }

    @NotNull
    private static Set<String> getMiningTools(@NotNull ItemTagLookup tagLookup) {
        Set<String> materials = new HashSet<>();
        materials.addAll(tagLookup.getAxes());
        materials.addAll(tagLookup.getPickaxes());
        materials.addAll(tagLookup.getShovels());
        return materials;
    }

    @NotNull
    private static Set<String> getToolsAndWeapon(@NotNull ItemTagLookup tagLookup) {
        Set<String> materials = new HashSet<>();
        materials.addAll(getTools(tagLookup));
        materials.addAll(getAllRangeWeapon(tagLookup));
        return materials;
    }

    @NotNull
    private static Set<String> getSwordsAxes(@NotNull ItemTagLookup tagLookup) {
        Set<String> materials = new HashSet<>();
        materials.addAll(tagLookup.getAxes());
        materials.addAll(tagLookup.getSwords());
        return materials;
    }

    @NotNull
    private static Set<String> getAllRangeWeapon(@NotNull ItemTagLookup tagLookup) {
        Set<String> materials = new HashSet<>();
        materials.addAll(tagLookup.getAxes());
        materials.addAll(tagLookup.getSwords());
        materials.add(Material.BOW.name());
        materials.add(Material.CROSSBOW.name());
        materials.add(Material.TRIDENT.name());
        materials.add(Material.MACE.name());
        return materials;
    }

    @NotNull
    private static Set<String> getTorso(@NotNull ItemTagLookup tagLookup) {
        Set<String> materials = new HashSet<>(tagLookup.getChestplates());
        materials.add(Material.ELYTRA.name());
        return materials;
    }

    private Function<ItemTagLookup, ItemSet> initFunction;
    private ItemSet itemSet;

    ItemSetDefaults(@NotNull Function<ItemTagLookup, ItemSet> initFunction) {
        this.initFunction = initFunction;
    }

    public void clear() {
        this.initFunction = null;
        this.itemSet = null;
    }

    public void initialize(@NotNull ItemTagLookup tagLookup) {
        if (this.itemSet == null) {
            this.itemSet = this.initFunction.apply(tagLookup);
        }
    }

    @NotNull
    public ItemSet getItemSet() {
        if (this.itemSet == null) {
            throw new IllegalStateException("物品套装尚未初始化！");
        }
        return this.itemSet;
    }
}
