package org.metamechanists.sanecrafting.patches;

import io.github.thebusybiscuit.slimefun4.api.events.ResearchUnlockEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.metamechanists.sanecrafting.SaneCrafting;
import org.metamechanists.sanecrafting.Util;

import java.util.UUID;


public final class RecipeBookResearchPatch implements Listener {
    private RecipeBookResearchPatch() {}

    public static void apply() {
        Bukkit.getServer().getPluginManager().registerEvents(new RecipeBookResearchPatch(), SaneCrafting.getInstance());
        SaneCrafting.getInstance().getLogger().info("Applied RecipeBookResearch patch");
    }

    @EventHandler
    public static void onJoin(@NotNull PlayerJoinEvent e) {
        if (Slimefun.getRegistry().isResearchingEnabled()) {
            PlayerProfile.get(e.getPlayer(), profile -> discoverRecipes(
                    e.getPlayer(),
                    item -> !item.hasResearch() || profile.hasUnlocked(item.getResearch())
            ));
        } else {
            discoverRecipes(e.getPlayer(), item -> true);
        }
    }

    @EventHandler
    public static void onResearch(@NotNull ResearchUnlockEvent e) {
        for (SlimefunItem item : e.getResearch().getAffectedItems()) {
            if (hasWorkbenchRecipe(item)) {
                e.getPlayer().discoverRecipe(new NamespacedKey(SaneCrafting.getInstance(), Util.generateRecipeId(item)));
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

    @EventHandler
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
                player.discoverRecipe(new NamespacedKey(SaneCrafting.getInstance(), Util.generateRecipeId(item)));
            }
        }
    }

    private static boolean hasWorkbenchRecipe(@NotNull SlimefunItem item) {
        return item.getRecipeType() == RecipeType.ENHANCED_CRAFTING_TABLE;
    }

    private static boolean hasUnlockedResearch(@NotNull Player player, @NotNull SlimefunItem item) {
        return PlayerProfile.find(player)
                .map(profile -> profile.hasUnlocked(item.getResearch()))
                .orElse(false);
    }
}
