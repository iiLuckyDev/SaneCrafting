package org.metamechanists.sanecrafting.patches;

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.sanecrafting.SaneCrafting;

import java.lang.reflect.Field;
import java.util.List;


// Once, I saw the mirror in my dreams. It spoke to me.
// 'Idra, go forth, and take my insights. It is time to apply the Holy principles of Reflection for the betterment of Humanity.'
// I woke up, and wrote this code. My life has been 10x better ever since. Follow the mirror.
@UtilityClass
public class RecipeLorePatch {
    private final NamespacedKey KEY = new NamespacedKey("minecraft", "shaped");

    public void apply() {
        try {
            Field recipeTypeItemField = RecipeType.class.getDeclaredField("item");
            recipeTypeItemField.setAccessible(true);

            Field recipeTypeKeyField = RecipeType.class.getDeclaredField("key");
            recipeTypeKeyField.setAccessible(true);

            patchRecipeType(RecipeType.ENHANCED_CRAFTING_TABLE, "Enhanced Crafting Table", recipeTypeItemField, recipeTypeKeyField);
            patchRecipeType(RecipeType.MAGIC_WORKBENCH, "Magic Workbench", recipeTypeItemField, recipeTypeKeyField);
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException | NoSuchFieldException e) {
            SaneCrafting.getInstance().getLogger().info("Failed to apply ChangeRecipeTypePatch");
            e.printStackTrace();
            return;
        }

        SaneCrafting.getInstance().getLogger().info("Applied RecipeLore patch");
    }

    private static void patchRecipeType(RecipeType recipeType, String originalWorkbench, Field recipeTypeItemField, Field recipeTypeKeyField) throws IllegalAccessException {
        recipeTypeItemField.set(recipeType, createRecipeTypeItem(originalWorkbench));
        recipeTypeKeyField.set(recipeType, KEY);
    }

    private static ItemStack createRecipeTypeItem(String originalWorkbench) {
        ItemStack itemStack = new ItemStack(Material.CRAFTING_TABLE);
        itemStack.editMeta(meta -> {
            meta.displayName(Component.text("Crafting Recipe", NamedTextColor.AQUA));
            meta.lore(List.of(
                    Component.text("Craft this item as shown", NamedTextColor.GRAY),
                    Component.text("in a normal Crafting Table.", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("To improve the player experience,", NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC),
                    Component.text("this server lets Slimefun items", NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC),
                    Component.text("be crafted in a regular workbench.", NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC),
                    Component.empty(),
                    Component.text("Normally this would require:", NamedTextColor.DARK_GRAY).decorate(TextDecoration.ITALIC),
                    Component.text(originalWorkbench, NamedTextColor.AQUA)
            ));
        });
        return itemStack;
    }
}
