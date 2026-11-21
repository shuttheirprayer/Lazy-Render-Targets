package com.misanthropy.lazy_render_targets;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public final class LazyTargets {
    public static final byte EAGER = 1, LAZY = 2;

    public static volatile boolean enabled = true;
    static final boolean FORCE_LOG = Boolean.getBoolean("lazy_render_targets.log");
    public static volatile boolean log = FORCE_LOG;
    public static volatile long idleNanos = 20_000_000_000L;
    public static volatile String[] owners = Config.DEFAULT_OWNERS;
    public static volatile String[] skip = new String[0];
    public static long clock = System.nanoTime();

    public static int created, freed;
    public static long createNanos;

    private static final List<WeakReference<Target>> TRACKED = new ArrayList<>();
    private static final float[] CLEAR = new float[4];
    private static final StackWalker WALKER = StackWalker.getInstance();

    private LazyTargets() {
    }

    public interface Target {
        boolean lrt$virtual();

        boolean lrt$allocated();

        long lrt$lastUse();

        long lrt$bytes();

        void lrt$allocate();

        void lrt$release();

        void lrt$touch();
    }

    public static byte classify(RenderTarget target) {
        if (target instanceof MainTarget) {
            return EAGER;
        }
        String[] wanted = owners;
        String[] unwanted = skip;
        Boolean lazy = WALKER.walk(frames -> {
            Boolean found = null;
            java.util.Iterator<StackWalker.StackFrame> it = frames.limit(40).iterator();
            while (it.hasNext()) {
                String name = it.next().getClassName();
                for (String s : unwanted) {
                    if (name.startsWith(s)) {
                        return Boolean.FALSE;
                    }
                }
                if (found == null) {
                    for (String s : wanted) {
                        if (name.startsWith(s)) {
                            found = Boolean.TRUE;
                            break;
                        }
                    }
                }
            }
            return found;
        });
        return lazy != null && lazy ? LAZY : EAGER;
    }

    public static void track(Target target) {
        synchronized (TRACKED) {
            for (WeakReference<Target> ref : TRACKED) {
                if (ref.get() == target) {
                    return;
                }
            }
            TRACKED.add(new WeakReference<>(target));
        }
    }

    public static void allocate(Target target) {
        long start = System.nanoTime();
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int x = GlStateManager.Viewport.x(), y = GlStateManager.Viewport.y(), w = GlStateManager.Viewport.width(), h = GlStateManager.Viewport.height();
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, CLEAR);
        float r = CLEAR[0], g = CLEAR[1], b = CLEAR[2], a = CLEAR[3];
        try {
            target.lrt$allocate();
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GlStateManager._bindTexture(texture);
            GlStateManager._viewport(x, y, w, h);
            GlStateManager._clearColor(r, g, b, a);
        }
        long spent = System.nanoTime() - start;
        created++;
        createNanos += spent;
        if (log) {
            LazyRenderTargets.LOGGER.info("Allocated a {} MB render target in {} us ({} allocated so far, {} released)", target.lrt$bytes() >> 20, spent / 1000, created, freed);
        }
    }

    private static void release(Target target) {
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            target.lrt$release();
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
            GlStateManager._bindTexture(texture);
        }
        freed++;
        if (log) {
            LazyRenderTargets.LOGGER.info("Released an idle {} MB render target ({} allocated so far, {} released)", target.lrt$bytes() >> 20, created, freed);
        }
    }

    public static void sweep() {
        if (!RenderSystem.isOnRenderThread()) {
            return;
        }
        long now = clock;
        long idle = idleNanos;
        synchronized (TRACKED) {
            for (int i = TRACKED.size() - 1; i >= 0; i--) {
                Target target = TRACKED.get(i).get();
                if (target == null) {
                    TRACKED.remove(i);
                } else if (enabled && target.lrt$allocated() && now - target.lrt$lastUse() > idle) {
                    release(target);
                }
            }
        }
    }

    public static int allocateAll() {
        if (!RenderSystem.isOnRenderThread()) {
            return 0;
        }
        List<Target> pending = new ArrayList<>();
        synchronized (TRACKED) {
            for (WeakReference<Target> ref : TRACKED) {
                Target target = ref.get();
                if (target != null && target.lrt$virtual()) {
                    pending.add(target);
                }
            }
        }
        for (Target target : pending) {
            target.lrt$touch();
        }
        return pending.size();
    }

    public static String status() {
        long held = 0, idle = 0;
        int live = 0, virtual = 0;
        synchronized (TRACKED) {
            for (WeakReference<Target> ref : TRACKED) {
                Target target = ref.get();
                if (target == null) {
                    continue;
                }
                if (target.lrt$virtual()) {
                    virtual++;
                    idle += target.lrt$bytes();
                } else if (target.lrt$allocated()) {
                    live++;
                    held += target.lrt$bytes();
                }
            }
        }
        return virtual + " idle targets hold no memory (" + (idle >> 20) + " MB saved), " + live + " in use (" + (held >> 20) + " MB), allocated " + created + " times, "
                + (created == 0 ? 0 : createNanos / created / 1000) + " us each, " + FontPages.small + " small font pages";
    }
}
