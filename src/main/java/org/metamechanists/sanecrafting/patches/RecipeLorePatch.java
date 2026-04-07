package org.metamechanists.sanecrafting.patches;

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.metamechanists.sanecrafting.SaneCrafting;

import java.lang.reflect.Field;


// Once, I saw the mirror in my dreams. It spoke to me.
// 'Idra, go forth, and take my insights. It is time to apply the Holy principles of Reflection for the betterment of Humanity.'
// I woke up, and wrote this code. My life has been 10x better ever since. Follow the mirror.
@UtilityClass
public class RecipeLorePatch {
    private final ItemStack ITEMSTACK = createRecipeTypeItem();
    private final NamespacedKey KEY = new NamespacedKey("minecraft", "shaped");

    public void apply() {
        try {
            Field recipeTypeItemField = RecipeType.class.getDeclaredField("item");
            recipeTypeItemField.setAccessible(true);
            recipeTypeItemField.set(RecipeType.ENHANCED_CRAFTING_TABLE, ITEMSTACK);

            Field recipeTypeKeyField = RecipeType.class.getDeclaredField("key");
            recipeTypeKeyField.setAccessible(true);
            recipeTypeKeyField.set(RecipeType.ENHANCED_CRAFTING_TABLE, KEY);
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException | NoSuchFieldException e) {
            SaneCrafting.getInstance().getLogger().info("Failed to apply ChangeRecipeTypePatch");
            e.printStackTrace();
            return;
        }

        SaneCrafting.getInstance().getLogger().info("Applied RecipeLore patch");
    }

    private static ItemStack createRecipeTypeItem() {
        ItemStack itemStack = new ItemStack(Material.CRAFTING_TABLE);
        itemStack.editMeta(meta -> meta.displayName(Component.text("Shaped Crafting Recipe", NamedTextColor.AQUA)));
        return itemStack;
    }
}
