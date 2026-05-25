package org.metamechanists.sanecrafting.patches;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.multiblocks.MultiBlockMachine;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.EnhancedCraftingTable;
import io.github.thebusybiscuit.slimefun4.implementation.items.multiblocks.MagicWorkbench;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.jetbrains.annotations.Nullable;
import org.metamechanists.sanecrafting.SaneCrafting;
import org.metamechanists.sanecrafting.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static org.metamechanists.sanecrafting.Util.generateRecipeId;


public final class CraftingTablePatch implements Listener {
    private static final Set<RecipeType> SUPPORTED_RECIPE_TYPES = Set.of(
            RecipeType.ENHANCED_CRAFTING_TABLE,
            RecipeType.MAGIC_WORKBENCH
    );
    private static final List<WorkbenchRecipe> WORKBENCH_RECIPES = new ArrayList<>();
    private static final Map<String, List<NamespacedKey>> RECIPE_KEYS_BY_ITEM_ID = new HashMap<>();
    private static boolean listenerRegistered;

    private CraftingTablePatch() {}

    private static void convertRecipe(String recipeId, List<ItemStack> input, ItemStack output) {
        SlimefunItem slimefunItem = SlimefunItem.getByItem(output);
        if (slimefunItem == null && !containsSlimefunIngredient(input)) {
            return;
        }

        String recipeGroup = slimefunItem == null ? Util.describeItem(output) : slimefunItem.getId();
        WorkbenchRecipe workbenchRecipe = createRecipe(recipeId, input, output, recipeGroup);
        if (workbenchRecipe == null) {
            return;
        }

        WORKBENCH_RECIPES.add(workbenchRecipe);
        if (slimefunItem != null) {
            RECIPE_KEYS_BY_ITEM_ID.computeIfAbsent(slimefunItem.getId(), id -> new ArrayList<>()).add(workbenchRecipe.key());
        }
        registerBukkitRecipe(workbenchRecipe);
    }

    @Nullable
    private static WorkbenchRecipe createRecipe(String recipeId, List<ItemStack> input, ItemStack output, String recipeGroup) {
        NamespacedKey key = new NamespacedKey(SaneCrafting.getInstance(), recipeId);

        int minX = 3;
        int minY = 3;
        int maxX = -1;
        int maxY = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = input.get(i);
            if (!isEmpty(itemStack)) {
                int x = i % 3;
                int y = i / 3;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX == -1) {
            return null;
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        ItemStack[] ingredients = new ItemStack[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                ItemStack itemStack = input.get((minY + y) * 3 + minX + x);
                ingredients[y * width + x] = isEmpty(itemStack) ? null : itemStack.clone();
            }
        }

        return new WorkbenchRecipe(key, width, height, ingredients, output.clone(), recipeGroup);
    }

    private static void registerBukkitRecipe(WorkbenchRecipe workbenchRecipe) {
        ShapedRecipe recipe = new ShapedRecipe(workbenchRecipe.key(), workbenchRecipe.output());
        recipe.setCategory(workbenchRecipe.category());
        recipe.setGroup(workbenchRecipe.group());
        recipe.shape(workbenchRecipe.shape());
        for (Entry<Character, ItemStack> entry : workbenchRecipe.bukkitIngredients().entrySet()) {
            recipe.setIngredient(entry.getKey(), entry.getValue());
        }
        Bukkit.getServer().addRecipe(recipe);
    }

    public static void apply() {
        removeExistingRecipes();
        WORKBENCH_RECIPES.clear();
        RECIPE_KEYS_BY_ITEM_ID.clear();

        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(new CraftingTablePatch(), SaneCrafting.getInstance());
            listenerRegistered = true;
        }

        int changedRecipes = 0;
        changedRecipes = convertMachineRecipes(Util.findMultiblock(EnhancedCraftingTable.class), changedRecipes);
        changedRecipes = convertMachineRecipes(Util.findMultiblock(MagicWorkbench.class), changedRecipes);

        for (SlimefunItem item : Slimefun.getRegistry().getEnabledSlimefunItems()) {
            if (!isSupportedRecipeType(item.getRecipeType()) || !getRecipeKeys(item).isEmpty()) {
                continue;
            }

            ItemStack[] recipe = item.getRecipe();
            ItemStack output = item.getRecipeOutput();
            if (recipe == null || recipe.length != 9 || isEmpty(output)) {
                continue;
            }

            try {
                convertRecipe(generateRecipeId(item) + "_" + changedRecipes, Arrays.asList(recipe), output);
                changedRecipes++;
            } catch (RuntimeException e) {
                String name = Util.describeItem(output);
                SaneCrafting.getInstance().getLogger().severe("Failed to convert Slimefun workbench recipe for " + name);
                e.printStackTrace();
            }
        }

        SaneCrafting.getInstance().getLogger().info("Applied CraftingTable patch and converted " + changedRecipes + " Slimefun workbench recipes to regular Crafting Table recipes");
    }

    private static int convertMachineRecipes(@Nullable MultiBlockMachine machine, int changedRecipes) {
        if (machine == null) {
            return changedRecipes;
        }

        List<ItemStack[]> recipes = machine.getRecipes();
        for (int j = 0; j < recipes.size(); j += 2) {
            ItemStack[] input = recipes.get(j);
            ItemStack output = recipes.get(j + 1)[0];
            if (SlimefunItem.getByItem(output) == null && !containsSlimefunIngredient(Arrays.asList(input))) {
                continue;
            }

            try {
                convertRecipe(generateRecipeId(output) + "_" + changedRecipes, Arrays.asList(input), output);
            } catch (RuntimeException e) {
                String name = Util.describeItem(output);
                SaneCrafting.getInstance().getLogger().severe("Failed to convert Slimefun workbench recipe for " + name);
                e.printStackTrace();
                continue;
            }

            changedRecipes++;
        }

        return changedRecipes;
    }

    public static List<NamespacedKey> getRecipeKeys(SlimefunItem item) {
        return RECIPE_KEYS_BY_ITEM_ID.containsKey(item.getId())
                ? List.copyOf(RECIPE_KEYS_BY_ITEM_ID.get(item.getId()))
                : Collections.emptyList();
    }

    public static boolean isSupportedRecipeType(@Nullable RecipeType recipeType) {
        return recipeType != null && SUPPORTED_RECIPE_TYPES.contains(recipeType);
    }

    private static void removeExistingRecipes() {
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        List<NamespacedKey> keys = new ArrayList<>();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof Keyed keyed && keyed.getKey().getNamespace().equals(SaneCrafting.getInstance().getName().toLowerCase())) {
                keys.add(keyed.getKey());
            }
        }

        for (NamespacedKey key : keys) {
            Bukkit.removeRecipe(key);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        if (matrix.length != 9) {
            return;
        }

        for (WorkbenchRecipe recipe : WORKBENCH_RECIPES) {
            if (recipe.matches(matrix)) {
                event.getInventory().setResult(recipe.output().clone());
                return;
            }
        }
    }

    private static boolean isEmpty(@Nullable ItemStack itemStack) {
        return itemStack == null || itemStack.getType().isAir();
    }

    private static boolean containsSlimefunIngredient(List<ItemStack> input) {
        for (ItemStack itemStack : input) {
            if (!isEmpty(itemStack) && SlimefunItem.getByItem(itemStack) != null) {
                return true;
            }
        }

        return false;
    }

    private static CraftingBookCategory getCategory(ItemStack output) {
        String typeName = output.getType().name();
        if (isEquipment(typeName)) {
            return CraftingBookCategory.EQUIPMENT;
        }

        if (typeName.contains("REDSTONE") || typeName.contains("PISTON") || typeName.contains("OBSERVER")) {
            return CraftingBookCategory.REDSTONE;
        }

        if (output.getType().isBlock()) {
            return CraftingBookCategory.BUILDING;
        }

        return CraftingBookCategory.MISC;
    }

    private static boolean isEquipment(String typeName) {
        return typeName.endsWith("_SWORD")
                || typeName.endsWith("_AXE")
                || typeName.endsWith("_PICKAXE")
                || typeName.endsWith("_SHOVEL")
                || typeName.endsWith("_HOE")
                || typeName.endsWith("_HELMET")
                || typeName.endsWith("_CHESTPLATE")
                || typeName.endsWith("_LEGGINGS")
                || typeName.endsWith("_BOOTS")
                || typeName.equals("BOW")
                || typeName.equals("CROSSBOW")
                || typeName.equals("TRIDENT")
                || typeName.equals("SHIELD")
                || typeName.equals("FISHING_ROD")
                || typeName.equals("SHEARS")
                || typeName.equals("FLINT_AND_STEEL")
                || typeName.equals("BRUSH");
    }

    private record WorkbenchRecipe(
            NamespacedKey key,
            int width,
            int height,
            ItemStack[] ingredients,
            ItemStack output,
            String slimefunItemId
    ) {
        private CraftingBookCategory category() {
            return getCategory(output);
        }

        private String group() {
            return "sanecrafting_" + slimefunItemId.toLowerCase(Locale.ROOT);
        }

        private String[] shape() {
            String itemCharacters = "abcdefghi";
            String[] shape = new String[height];
            for (int y = 0; y < height; y++) {
                StringBuilder row = new StringBuilder();
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    row.append(isEmpty(ingredients[index]) ? ' ' : itemCharacters.charAt(index));
                }
                shape[y] = row.toString();
            }
            return shape;
        }

        private Map<Character, ItemStack> bukkitIngredients() {
            String itemCharacters = "abcdefghi";
            Map<Character, ItemStack> result = new HashMap<>();
            for (int i = 0; i < ingredients.length; i++) {
                if (!isEmpty(ingredients[i])) {
                    result.put(itemCharacters.charAt(i), ingredients[i]);
                }
            }
            return result;
        }

        private boolean matches(ItemStack[] matrix) {
            for (int offsetY = 0; offsetY <= 3 - height; offsetY++) {
                for (int offsetX = 0; offsetX <= 3 - width; offsetX++) {
                    if (matchesAt(matrix, offsetX, offsetY)) {
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean matchesAt(ItemStack[] matrix, int offsetX, int offsetY) {
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    ItemStack actual = matrix[y * 3 + x];
                    ItemStack expected = getExpectedIngredient(x - offsetX, y - offsetY);
                    if (!matchesItem(expected, actual)) {
                        return false;
                    }
                }
            }

            return true;
        }

        @Nullable
        private ItemStack getExpectedIngredient(int x, int y) {
            if (x < 0 || y < 0 || x >= width || y >= height) {
                return null;
            }

            return ingredients[y * width + x];
        }

        private boolean matchesItem(@Nullable ItemStack expected, @Nullable ItemStack actual) {
            if (isEmpty(expected)) {
                return isEmpty(actual);
            }

            return !isEmpty(actual) && actual.isSimilar(expected);
        }
    }
}
