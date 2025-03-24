package kurwi.krevent2.listeners;

import kurwi.krevent2.MainPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class BarrelListener implements Listener {
    private final MainPlugin plugin;
    private final Set<Location> processedBarrels = new HashSet<>();
    private final Random random = new Random();

    // Веса для категорий
    private static final Map<String, Integer> CATEGORY_WEIGHTS = Map.of(
            "armor", 40,
            "weapon", 30,
            "food", 20,
            "potion", 10
    );

    private static final Map<String, Integer> ARMOR_WEIGHTS = Map.of(
            "leather", 50,
            "chainmail", 30,
            "iron", 20
    );

    private static final Map<String, Integer> WEAPON_WEIGHTS = Map.of(
            "wooden_hoe", 40,
            "stone_hoe", 30,
            "golden_sword", 25,
            "stone_sword", 20,
            "iron_sword", 10
    );

    private static final Map<String, Integer> FOOD_WEIGHTS = Map.of(
            "steak", 40,
            "apple", 30,
            "rotten_flesh", 25,
            "golden_apple", 5
    );

    private static final Map<String, Integer> POTION_WEIGHTS = Map.of(
            "healing", 20,
            "regeneration", 15,
            "strength", 10,
            "slowness", 5,
            "blindness", 3
    );

    // Тип зелья слепоты (если такого нет – используется SLOWNESS в качестве запасного варианта)
    private final PotionType blindnessPotion;

    public BarrelListener(MainPlugin plugin) {
        this.plugin = plugin;
        this.blindnessPotion = getBlindnessPotionType();
    }

    private PotionType getBlindnessPotionType() {
        try {
            return PotionType.valueOf("BLINDNESS");
        } catch (IllegalArgumentException e) {
            return PotionType.SLOWNESS; // Fallback
        }
    }

    @EventHandler
    public void onBarrelOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof Barrel)) return;

        Barrel barrel = (Barrel) event.getInventory().getHolder();
        Location loc = barrel.getLocation();
        // Генерируем лут только при первом открытии бочки в игре
        if (!processedBarrels.contains(loc)) {
            generateLoot(event.getInventory());
            processedBarrels.add(loc);
        }
    }

    /**
     * Метод для сброса состояния бочек.
     * Его необходимо вызывать при запуске новой игры (например, из команды /startgame).
     */
    public void resetBarrels() {
        processedBarrels.clear();
    }

    private void generateLoot(Inventory inventory) {
        inventory.clear();
        int itemsCount = 2 + random.nextInt(5);
        Set<Integer> usedSlots = new HashSet<>();

        for (int i = 0; i < itemsCount; i++) {
            ItemStack item = generateRandomItem();
            int slot = getUniqueSlot(usedSlots);
            inventory.setItem(slot, item);
        }
    }

    private int getUniqueSlot(Set<Integer> usedSlots) {
        int slot;
        do {
            slot = random.nextInt(27);
        } while (usedSlots.contains(slot));
        usedSlots.add(slot);
        return slot;
    }

    private ItemStack generateRandomItem() {
        String category = getWeightedRandom(CATEGORY_WEIGHTS);
        switch (category) {
            case "armor":
                return generateArmor();
            case "weapon":
                return generateWeapon();
            case "food":
                return generateFood();
            case "potion":
                return generatePotion();
            default:
                return new ItemStack(Material.AIR);
        }
    }

    private ItemStack generateArmor() {
        String type = getWeightedRandom(ARMOR_WEIGHTS);
        Material[] parts;
        switch (type) {
            case "leather":
                parts = new Material[]{
                        Material.LEATHER_HELMET,
                        Material.LEATHER_CHESTPLATE,
                        Material.LEATHER_LEGGINGS,
                        Material.LEATHER_BOOTS
                };
                break;
            case "chainmail":
                parts = new Material[]{
                        Material.CHAINMAIL_HELMET,
                        Material.CHAINMAIL_CHESTPLATE,
                        Material.CHAINMAIL_LEGGINGS,
                        Material.CHAINMAIL_BOOTS
                };
                break;
            case "iron":
                parts = new Material[]{
                        Material.IRON_HELMET,
                        Material.IRON_CHESTPLATE,
                        Material.IRON_LEGGINGS,
                        Material.IRON_BOOTS
                };
                break;
            default:
                return new ItemStack(Material.AIR);
        }
        return createArmorPiece(parts[random.nextInt(parts.length)]);
    }

    private ItemStack createArmorPiece(Material material) {
        ItemStack item = new ItemStack(material);
        if (random.nextDouble() < 0.3) {
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        return item;
    }

    private ItemStack generateWeapon() {
        String type = getWeightedRandom(WEAPON_WEIGHTS);
        switch (type) {
            case "wooden_hoe":
                return new ItemStack(Material.WOODEN_HOE);
            case "stone_hoe":
                return new ItemStack(Material.STONE_HOE);
            case "golden_sword":
                ItemStack sword = new ItemStack(Material.GOLDEN_SWORD);
                sword.addEnchantment(Enchantment.DAMAGE_ALL, 1);
                return sword;
            case "stone_sword":
                return new ItemStack(Material.STONE_SWORD);
            case "iron_sword":
                return new ItemStack(Material.IRON_SWORD);
            default:
                return new ItemStack(Material.STICK);
        }
    }

    private ItemStack generateFood() {
        String type = getWeightedRandom(FOOD_WEIGHTS);
        switch (type) {
            case "steak":
                return new ItemStack(Material.COOKED_BEEF, 1 + random.nextInt(3));
            case "apple":
                return new ItemStack(Material.APPLE, 1 + random.nextInt(2));
            case "rotten_flesh":
                return new ItemStack(Material.ROTTEN_FLESH, 2 + random.nextInt(3));
            case "golden_apple":
                return new ItemStack(Material.GOLDEN_APPLE);
            default:
                return new ItemStack(Material.BREAD);
        }
    }

    private ItemStack generatePotion() {
        String type = getWeightedRandom(POTION_WEIGHTS);
        ItemStack potion = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        switch (type) {
            case "healing":
                meta.setBasePotionData(new PotionData(PotionType.INSTANT_HEAL));
                break;
            case "regeneration":
                meta.setBasePotionData(new PotionData(PotionType.REGEN));
                break;
            case "strength":
                meta.setBasePotionData(new PotionData(PotionType.STRENGTH));
                break;
            case "slowness":
                meta.setBasePotionData(new PotionData(PotionType.SLOWNESS));
                break;
            case "blindness":
                meta.setBasePotionData(new PotionData(blindnessPotion));
                break;
        }
        potion.setItemMeta(meta);
        return potion;
    }

    private String getWeightedRandom(Map<String, Integer> weights) {
        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        int roll = random.nextInt(total);
        for (Map.Entry<String, Integer> entry : weights.entrySet()) {
            if (roll < entry.getValue()) {
                return entry.getKey();
            }
            roll -= entry.getValue();
        }
        return weights.keySet().iterator().next();
    }
}
