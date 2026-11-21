package com.misanthropy.lazy_render_targets.mixin;

import com.misanthropy.lazy_render_targets.LazyTargets;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderTarget.class, remap = false, priority = 2000)
public abstract class RenderTargetMixin implements LazyTargets.Target {
    @Shadow
    public int f_83915_;
    @Shadow
    public int f_83916_;
    @Shadow
    public int f_83917_;
    @Shadow
    public int f_83918_;
    @Shadow
    @Final
    public boolean f_83919_;
    @Shadow
    public int f_83920_;
    @Shadow
    @Final
    private float[] f_83921_;
    @Shadow
    public int f_83922_;

    @Shadow
    public abstract void m_83950_(int width, int height, boolean clearError);

    @Shadow
    public abstract void m_83930_();

    @Shadow
    public abstract void m_83936_(int filterMode);

    @Unique
    private byte lrt$kind;
    @Unique
    private boolean lrt$virtual;
    @Unique
    private boolean lrt$forcing;
    @Unique
    private boolean lrt$releasing;
    @Unique
    private boolean lrt$clearError;
    @Unique
    private int lrt$filter = 9728;
    @Unique
    private long lrt$lastUse;

    @Override
    public boolean lrt$virtual() {
        return lrt$virtual;
    }

    @Override
    public boolean lrt$allocated() {
        return lrt$kind == LazyTargets.LAZY && !lrt$virtual && f_83920_ >= 0;
    }

    @Override
    public long lrt$lastUse() {
        return lrt$lastUse;
    }

    @Override
    public long lrt$bytes() {
        return (long) f_83915_ * f_83916_ * (f_83919_ ? 12L : 4L);
    }

    @Override
    public void lrt$allocate() {
        lrt$virtual = false;
        lrt$forcing = true;
        try {
            m_83950_(f_83915_, f_83916_, lrt$clearError);
            if (lrt$filter != 9728) {
                m_83936_(lrt$filter);
            }
        } finally {
            lrt$forcing = false;
        }
        lrt$lastUse = LazyTargets.clock;
    }

    @Override
    public void lrt$release() {
        lrt$filter = f_83922_;
        lrt$releasing = true;
        try {
            m_83930_();
        } finally {
            lrt$releasing = false;
        }
        lrt$virtual = true;
    }

    @Override
    public void lrt$touch() {
        lrt$use();
    }

    @Unique
    private void lrt$use() {
        if (lrt$kind == LazyTargets.LAZY) {
            if (lrt$virtual && RenderSystem.isOnRenderThread()) {
                LazyTargets.allocate(this);
            }
            lrt$lastUse = LazyTargets.clock;
        }
    }

    @Inject(method = "m_83950_", at = @At("HEAD"), cancellable = true, remap = false)
    private void lrt$create(int width, int height, boolean clearError, CallbackInfo ci) {
        if (lrt$forcing) {
            return;
        }
        if (lrt$kind == 0) {
            lrt$kind = LazyTargets.enabled ? LazyTargets.classify((RenderTarget) (Object) this) : LazyTargets.EAGER;
        }
        lrt$virtual = false;
        if (lrt$kind != LazyTargets.LAZY || !LazyTargets.enabled) {
            return;
        }
        int max = RenderSystem.maxSupportedTextureSize();
        if (width <= 0 || width > max || height <= 0 || height > max) {
            return;
        }
        f_83917_ = width;
        f_83918_ = height;
        f_83915_ = width;
        f_83916_ = height;
        lrt$clearError = clearError;
        lrt$filter = 9728;
        f_83922_ = 9728;
        lrt$virtual = true;
        LazyTargets.track(this);
        ci.cancel();
    }

    @Inject(method = "m_83930_", at = @At("HEAD"), remap = false)
    private void lrt$destroy(CallbackInfo ci) {
        if (!lrt$releasing) {
            lrt$virtual = false;
        }
    }

    @Inject(method = "m_83961_", at = @At("HEAD"), remap = false)
    private void lrt$bindWrite(boolean setViewport, CallbackInfo ci) {
        lrt$use();
    }

    @Inject(method = "m_83956_", at = @At("HEAD"), remap = false)
    private void lrt$bindRead(CallbackInfo ci) {
        lrt$use();
    }

    @Inject(method = "m_83975_", at = @At("HEAD"), remap = false)
    private void lrt$color(CallbackInfoReturnable<Integer> cir) {
        lrt$use();
    }

    @Inject(method = "m_83980_", at = @At("HEAD"), remap = false)
    private void lrt$depth(CallbackInfoReturnable<Integer> cir) {
        lrt$use();
    }

    @Inject(method = "m_83971_", at = @At("HEAD"), cancellable = true, remap = false)
    private void lrt$blit(int width, int height, boolean disableBlend, CallbackInfo ci) {
        if (lrt$virtual && !disableBlend && f_83921_[3] == 0.0F && RenderSystem.isOnRenderThread()) {
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._disableDepthTest();
            GlStateManager._depthMask(true);
            GlStateManager._viewport(0, 0, width, height);
            ci.cancel();
            return;
        }
        lrt$use();
    }

    @Inject(method = "m_83945_", at = @At("HEAD"), remap = false)
    private void lrt$copyDepth(RenderTarget other, CallbackInfo ci) {
        lrt$use();
        ((LazyTargets.Target) other).lrt$touch();
    }

    @Inject(method = "m_83936_", at = @At("HEAD"), cancellable = true, remap = false)
    private void lrt$filter(int filterMode, CallbackInfo ci) {
        if (lrt$virtual) {
            lrt$filter = filterMode;
            f_83922_ = filterMode;
            ci.cancel();
        }
    }

    @Inject(method = "m_83954_", at = @At("HEAD"), cancellable = true, remap = false)
    private void lrt$clear(boolean clearError, CallbackInfo ci) {
        if (lrt$virtual && RenderSystem.isOnRenderThread()) {
            GlStateManager._viewport(0, 0, f_83917_, f_83918_);
            GlStateManager._clearColor(f_83921_[0], f_83921_[1], f_83921_[2], f_83921_[3]);
            GlStateManager._glBindFramebuffer(36160, 0);
            ci.cancel();
        }
    }
}
