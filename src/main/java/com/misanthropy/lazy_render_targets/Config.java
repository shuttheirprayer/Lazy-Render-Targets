package com.misanthropy.lazy_render_targets;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

final class Config {
    static final String[] DEFAULT_OWNERS = {"net.minecraft.client.renderer.PostChain", "com.gametechbc.traveloptics.api.client.shader.DirectPostShaderRenderer"};

    static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec.BooleanValue ENABLED;
    static final ForgeConfigSpec.IntValue IDLE_SECONDS;
    static final ForgeConfigSpec.BooleanValue LOG;
    static final ForgeConfigSpec.BooleanValue SMALL_FONT_PAGES;
    static final ForgeConfigSpec.ConfigValue<List<? extends String>> OWNERS;
    static final ForgeConfigSpec.ConfigValue<List<? extends String>> SKIP;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ENABLED = b.comment("Post effect buffers get their memory when something first draws to them, and give it back after sitting unused. Needs a restart to turn off fully.")
                .define("enabled", true);
        IDLE_SECONDS = b.comment("Seconds a buffer must sit unused before its memory is given back.")
                .defineInRange("idleSeconds", 20, 2, 3600);
        LOG = b.comment("Log every buffer that gets memory or gives it back.")
                .define("log", false);
        OWNERS = b.comment("Buffers created by these classes (or classes whose name starts with these) are handled. Post effect chains are the safe default.")
                .defineListAllowEmpty(List.of("owners"), () -> List.of(DEFAULT_OWNERS), o -> o instanceof String);
        SKIP = b.comment("Buffers created by these classes are never handled, even if they match the list above. Add a mod's class here if one of its effects breaks.")
                .defineListAllowEmpty(List.of("skip"), List::of, o -> o instanceof String);
        SMALL_FONT_PAGES = b.comment("With ImmediatelyFast every font starts with a 2048x2048 page (16 MB) even if it only ever draws a few letters. With this on, the first two pages of a font are the normal 256x256 and only later pages are big.")
                .define("smallFontPages", true);
        SPEC = b.build();
    }

    private Config() {
    }

    static void apply() {
        LazyTargets.enabled = ENABLED.get();
        LazyTargets.idleNanos = IDLE_SECONDS.get() * 1_000_000_000L;
        LazyTargets.log = LOG.get() || LazyTargets.FORCE_LOG;
        LazyTargets.owners = OWNERS.get().toArray(new String[0]);
        LazyTargets.skip = SKIP.get().toArray(new String[0]);
        FontPages.enabled = SMALL_FONT_PAGES.get() && !Boolean.getBoolean("lazy_render_targets.bigFontPages");
    }
}
