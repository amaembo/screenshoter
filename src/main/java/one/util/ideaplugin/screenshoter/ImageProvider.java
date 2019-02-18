package one.util.ideaplugin.screenshoter;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;

/**
 * @author 九条涼果 s1131234@gmail.com
 */
interface ImageProvider {

    boolean isCapable(Editor editor);

    /**
     * Capture selected region.
     *
     * Image is HiDPI awareness.
     * If select nothing, capture a whole document(may cause problem)
     *
     * @param editor screen capture target.
     *               {@link EditorEx} is required if gutter capture is enabled.
     * @throws RuntimeException if {@link #isCapable(Editor)} return true
     */
    @NotNull
    BufferedImage takeScreenshot(Editor editor);

    /** @param options current setting of the plugin */
    static ImageProvider forOption(CopyImageOptionsProvider.State options) {
        if (options.myEnableGutter) {
            return new AdvancedImageBuilder(options);
        } else {
            return new ImageBuilder(options);
        }
    }

    static ImageProvider defaultProvider(CopyImageOptionsProvider.State options) {
        return new ImageBuilder(options);
    }

}
