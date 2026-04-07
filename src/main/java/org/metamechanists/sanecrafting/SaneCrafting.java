package org.metamechanists.sanecrafting;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemRegistryFinalizedEvent;
import lombok.Getter;
import lombok.NonNull;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.metamechanists.sanecrafting.patches.CraftingTablePatch;
import org.metamechanists.sanecrafting.patches.RecipeBookResearchPatch;
import org.metamechanists.sanecrafting.patches.RecipeLorePatch;
import org.metamechanists.sanecrafting.patches.UsableInWorkbenchPatch;
import io.github.thebusybiscuit.slimefun4.libraries.dough.updater.BlobBuildUpdater;


public final class SaneCrafting extends JavaPlugin implements SlimefunAddon, Listener {
    private static final int BSTATS_ID = 22737;
    @Getter
    private static SaneCrafting instance;
    private boolean patchesApplied;

    @Override
    public void onEnable() {
        instance = this;

        if (getConfig().getBoolean("auto-update") && !getPluginVersion().contains("MODIFIED")) {
            new BlobBuildUpdater(this, getFile(), "SaneCrafting").start();
        }

        new Metrics(this, BSTATS_ID);

        Bukkit.getPluginManager().registerEvents(this, this);

        // Fallback in case the registry-finalized signal was emitted before we enabled.
        Bukkit.getScheduler().runTaskLater(this, this::applyPatches, 40L);

    }

    @Override
    public void onDisable() {

    }

    @NonNull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Nullable
    @Override
    public String getBugTrackerURL() {
        return null;
    }

    @EventHandler
    public void onRegistryFinalized(@NonNull SlimefunItemRegistryFinalizedEvent event) {
        applyPatches();
    }

    @EventHandler
    public void onServerLoad(@NonNull ServerLoadEvent event) {
        applyPatches();
    }

    private void applyPatches() {
        if (patchesApplied) {
            return;
        }

        patchesApplied = true;
        UsableInWorkbenchPatch.apply();
        CraftingTablePatch.apply();
        RecipeBookResearchPatch.apply();
        RecipeLorePatch.apply();
    }
}
