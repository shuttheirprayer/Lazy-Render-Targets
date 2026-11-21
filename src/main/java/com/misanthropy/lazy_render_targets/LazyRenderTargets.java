package com.misanthropy.lazy_render_targets;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(LazyRenderTargets.MOD_ID)
public final class LazyRenderTargets {
    public static final String MOD_ID = "lazy_render_targets";
    public static final Logger LOGGER = LoggerFactory.getLogger("Lazy Render Targets");

    private static final boolean SELF_TEST = Boolean.getBoolean("lazy_render_targets.selftest");
    private static int ticks;

    public LazyRenderTargets() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(LazyRenderTargets::config);
            MinecraftForge.EVENT_BUS.addListener(LazyRenderTargets::tick);
        }
    }

    private static void config(ModConfigEvent event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            Config.apply();
        }
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        LazyTargets.clock = System.nanoTime();
        ticks++;
        if (ticks % 20 == 0) {
            LazyTargets.sweep();
        }
        if (SELF_TEST && ticks % 1200 == 900) {
            LOGGER.info("Self test: forced {} idle targets into memory", LazyTargets.allocateAll());
        }
        if (ticks == 2400 || LazyTargets.log && ticks % 1200 == 0) {
            LOGGER.info(LazyTargets.status());
        }
    }
}
