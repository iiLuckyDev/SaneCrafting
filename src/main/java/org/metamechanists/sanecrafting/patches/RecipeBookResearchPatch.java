package org.metamechanists.sanecrafting.patches;

import io.github.thebusybiscuit.slimefun4.api.events.ResearchUnlockEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.sanecrafting.SaneCrafting;

public final class RecipeBookResearchPatch implements Listener {
    private RecipeBookResearchPatch() {}

    public static void apply() {
        Bukkit.getServer().getPluginManager().registerEvents(new RecipeBookResearchPatch(), SaneCrafting.getInstance());
        for (Player player : Bukkit.getOnlinePlayers()) {
            discoverAvailableRecipes(player);
        }
        SaneCrafting.getInstance().getLogger().info("Applied RecipeBookResearch patch");
    }

    @EventHandler
    public static void onJoin(@NotNull PlayerJoinEvent e) {
        discoverAvailableRecipes(e.getPlayer());
    }

    private static void discoverAvailableRecipes(@NotNull Player player) {
        if (Slimefun.getRegistry().isResearchingEnabled()) {
            PlayerProfile.get(player, profile -> discoverRecipes(
                    player,
                    item -> !item.hasResearch() || profile.hasUnlocked(item.getResearch())
            ));
        } else {
            discoverRecipes(player, item -> true);
        }
    }

    @EventHandler
    public static void onResearch(@NotNull ResearchUnlockEvent e) {
        for (SlimefunItem item : e.getResearch().getAffectedItems()) {
            if (hasWorkbenchRecipe(item)) {
                for (NamespacedKey key : CraftingTablePatch.getRecipeKeys(item)) {
                    e.getPlayer().discoverRecipe(key);
                }
            }
        }
    }

    @EventHandler
    public static void onCraft(@NotNull CraftItemEvent e) {
        SlimefunItem result = SlimefunItem.getByItem(e.getInventory().getResult());
        if (result == null) {
            return;
        }

        if (!Slimefun.getRegistry().isResearchingEnabled() || !result.hasResearch()) {
            return;
        }

        if (!(e.getWhoClicked() instanceof Player player)) {
            e.setCancelled(true);
            return;
        }

        if (hasUnlockedResearch(player, result)) {
            return;
        }

        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public static void onPrepareCraft(@NotNull PrepareItemCraftEvent e) {
        SlimefunItem result = SlimefunItem.getByItem(e.getInventory().getResult());
        if (result == null) {
            return;
        }

        if (!Slimefun.getRegistry().isResearchingEnabled() || !result.hasResearch()) {
            return;
        }

        if (!(e.getView().getPlayer() instanceof Player player)) {
            e.getInventory().setResult(null);
            return;
        }

        if (hasUnlockedResearch(player, result)) {
            return;
        }

        e.getInventory().setResult(null);
    }

    private static void discoverRecipes(@NotNull Player player, @NotNull java.util.function.Predicate<SlimefunItem> canDiscover) {
        for (SlimefunItem item : Slimefun.getRegistry().getEnabledSlimefunItems()) {
            if (hasWorkbenchRecipe(item) && canDiscover.test(item)) {
                for (NamespacedKey key : CraftingTablePatch.getRecipeKeys(item)) {
                    player.discoverRecipe(key);
                }
            }
        }
    }

    private static boolean hasWorkbenchRecipe(@NotNull SlimefunItem item) {
        return CraftingTablePatch.isSupportedRecipeType(item.getRecipeType()) && !CraftingTablePatch.getRecipeKeys(item).isEmpty();
    }

    private static boolean hasUnlockedResearch(@NotNull Player player, @NotNull SlimefunItem item) {
        return PlayerProfile.find(player)
                .map(profile -> profile.hasUnlocked(item.getResearch()))
                .orElse(false);
    }
}
