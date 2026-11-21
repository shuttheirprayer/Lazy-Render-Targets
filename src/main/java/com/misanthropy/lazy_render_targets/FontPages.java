package com.misanthropy.lazy_render_targets;

import java.lang.reflect.Field;

public final class FontPages {
    public static volatile boolean enabled = !Boolean.getBoolean("lazy_render_targets.bigFontPages");
    public static int small;

    private static Field config;
    private static Field flag;
    private static boolean missing;

    private FontPages() {
    }

    public static boolean begin(int pages) {
        if (pages >= 2 || !enabled || missing) {
            return false;
        }
        try {
            if (flag == null) {
                Class<?> owner = Class.forName("net.raphimc.immediatelyfast.ImmediatelyFast");
                config = owner.getField("runtimeConfig");
                flag = config.getType().getField("font_atlas_resizing");
            }
            Object runtime = config.get(null);
            if (runtime == null || !flag.getBoolean(runtime)) {
                return false;
            }
            flag.setBoolean(runtime, false);
            return true;
        } catch (Throwable t) {
            missing = true;
            return false;
        }
    }

    public static void end(int pagesBefore, int pagesAfter) {
        try {
            flag.setBoolean(config.get(null), true);
        } catch (Throwable t) {
            missing = true;
        }
        small += pagesAfter - pagesBefore;
    }
}
