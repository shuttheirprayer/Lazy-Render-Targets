package com.misanthropy.lazy_render_targets.mixin;

import com.misanthropy.lazy_render_targets.FontPages;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.FontTexture;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = FontSet.class, remap = false)
public abstract class FontSetMixin {
    @Shadow
    @Final
    private List<FontTexture> f_95059_;

    @Unique
    private int lrt$pagesBefore = -1;

    @Inject(method = "m_232556_", at = @At("HEAD"))
    private void lrt$beforeStitch(SheetGlyphInfo info, CallbackInfoReturnable<BakedGlyph> cir) {
        int pages = this.f_95059_.size();
        this.lrt$pagesBefore = FontPages.begin(pages) ? pages : -1;
    }

    @Inject(method = "m_232556_", at = @At("RETURN"))
    private void lrt$afterStitch(SheetGlyphInfo info, CallbackInfoReturnable<BakedGlyph> cir) {
        if (this.lrt$pagesBefore >= 0) {
            FontPages.end(this.lrt$pagesBefore, this.f_95059_.size());
            this.lrt$pagesBefore = -1;
        }
    }
}
