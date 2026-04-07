package org.metamechanists.sanecrafting;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;


public final class Util {
    private Util() {}

    // Technically could lead to clashes if two shaped recipes for same item but... hopefully not...
    public static @NotNull String generateRecipeId(@NotNull ItemStack output) {
        return generateRecipeId(SlimefunItem.getByItem(output));
    }

    public static @NotNull String generateRecipeId(@NotNull SlimefunItem item) {
        String normalisedName = item.getId().toLowerCase(Locale.ROOT)
                .replace(' ', '_')
                .replaceAll("[^a-z0-9/._\\-]", ""); // remove characters not allowed in id
        return "sanecrafting_" + normalisedName;
    }

    public static @NotNull String describeItem(@NotNull ItemStack itemStack) {
        SlimefunItem slimefunItem = SlimefunItem.getByItem(itemStack);
        if (slimefunItem != null) {
            return slimefunItem.getId();
        }

        if (itemStack.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(itemStack.displayName());
        }

        return itemStack.getType().name();
    }

    public static @Nullable <T extends SlimefunItem> T findMultiblock(Class<T> clazz) {
        for (SlimefunItem item : Slimefun.getRegistry().getEnabledSlimefunItems()) {
            if (clazz.isInstance(item)) {
                return clazz.cast(item);
            }
        }

        SaneCrafting.getInstance().getLogger().severe("Failed to initialise SaneCrafting; EnhancedCraftingTable does not exist!");
        return null;
    }
}
