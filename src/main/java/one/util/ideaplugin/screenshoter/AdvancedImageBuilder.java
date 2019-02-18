package one.util.ideaplugin.screenshoter;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.SoftWrap;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.ex.SoftWrapModelEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapDrawingType;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.paint.LinePainter2D;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provide features such as gutter capture
 *
 * @author 九条涼果 s1131234@gmail.com
 */
class AdvancedImageBuilder implements ImageProvider {

    private static final int HUGE_FILE_THRESHOLD = 300;

    @NotNull private final CopyImageOptionsProvider.State options;

    // extract this field for editor restoration when exception happens on #calculateSelectionArea()
    private StateSnapshot snapshot;

    AdvancedImageBuilder(@NotNull CopyImageOptionsProvider.State options) {
        this.options = options;
    }

    @Override
    public boolean isCapable(Editor editor) {
        return editor instanceof EditorEx;
    }

    @NotNull
    @Override
    @SuppressWarnings("NumericCastThatLosesPrecision")
    public BufferedImage takeScreenshot(Editor _e) {
        EditorEx editor = (EditorEx) _e;

        snapshot = new StateSnapshot(editor);
        snapshot.tweakEditor(options);

        Rectangle bounds = calculateSelectionArea(editor, snapshot.selectionStart, snapshot.selectionEnd);

        double scaleRatio = options.myScale;
        int padding = options.myPadding;

        int gutterCompWidth = editor.getGutterComponentEx().getWidth();
        int innerFrameWidth = (int) ((gutterCompWidth + bounds.width) * scaleRatio);
        int innerFrameHeight = (int) (bounds.height * scaleRatio);
        int frameWidth = (int) (innerFrameWidth + 2 * padding * scaleRatio);
        int frameHeight = (int) (innerFrameHeight + 2 * padding * scaleRatio);
        BufferedImage img = UIUtil.createImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D palette = img.createGraphics();
        palette.scale(scaleRatio, scaleRatio);
        Graphics framePalette = palette.create(0, 0, frameWidth, frameHeight);
        Graphics innerPalette = palette.create(padding, padding, innerFrameWidth, innerFrameHeight);

        padding(framePalette, editor, gutterCompWidth);
        content(innerPalette, editor, bounds);

        innerPalette.dispose();
        framePalette.dispose();
        palette.dispose();

        snapshot.restore();
        return img;
    }

    /**
     * Draw background
     *
     * @param divider x position of gutter separator
     */
    private void padding(Graphics palette, EditorEx editor, int divider) {
        int padding = options.myPadding;
        if (padding <= 0) return;

        // FIXME: handle editor background image

        int paintHeight = palette.getClipBounds().height;
        drawGutterBackground(palette, editor, paintHeight, padding, divider);
        drawEditorBackground(palette, editor, paintHeight, padding, divider, palette.getClipBounds().width);
    }

    /** Draw gutter and editor */
    private void content(Graphics palette, EditorEx editor, Rectangle bounds) {
        JComponent gutterComp = editor.getGutterComponentEx();
        JComponent contentComp = editor.getContentComponent();

        Graphics gutterPalette = palette.create(0, -bounds.y,
                gutterComp.getWidth(), bounds.height + bounds.y);
        Graphics contentPalette = palette.create(gutterComp.getWidth(), -bounds.y,
                bounds.width, bounds.height + bounds.y);
        contentPalette.translate(-bounds.x, 0);

        gutterComp.paint(gutterPalette);
        contentComp.paint(contentPalette);

        gutterPalette.dispose();
        contentPalette.dispose();
    }

    /**
     * @param height  image height
     * @param divider x position of gutter separator
     */
    private static void drawGutterBackground(Graphics g, EditorEx editor, int height, int padding, int divider) {
        EditorGutterComponentEx comp = editor.getGutterComponentEx();

        // use anonymous code block to restrict variable scope,
        // save some private methods which only be used once.

        // gutter background
        {
            Color gtBg = comp.getBackground();
            g.setColor(gtBg);
            g.fillRect(0, 0, padding + divider, height);
        }
        // editor background
        {
            Color edBg = editor.getBackgroundColor();
            g.setColor(edBg);
            int gutterSeparatorX = comp.getWhitespaceSeparatorOffset();
            int foldingAreaWidth = invokeMethod(findMethod(comp, "getFoldingAreaWidth"), comp);
            g.fillRect(padding + gutterSeparatorX, 0, foldingAreaWidth, height);
        }
        // folding line
        {
            boolean shown = invokeMethod(findMethod(comp, "isFoldingOutlineShown"), comp);
            if (!shown) return;
            Color olBg = invokeMethod(findMethod(comp, "getOutlineColor", boolean.class), comp, false);
            g.setColor(olBg);
            double x = comp.getWhitespaceSeparatorOffset() + padding;
            double w = invokeMethod(findMethod(comp, "getStrokeWidth"), comp);
            LinePainter2D.paint((Graphics2D) g, x, 0, x, height, LinePainter2D.StrokeType.CENTERED, w);
        }
    }

    /**
     * @param height     image height
     * @param divider    x position where editor content begin(on right side)
     * @param imageWidth width of editor content
     */
    private static void drawEditorBackground(Graphics g, EditorEx editor, int height, int padding, int divider,
            int imageWidth) {
        g.setColor(editor.getBackgroundColor());
        g.fillRect(padding + divider, 0,
                (imageWidth - divider) + padding, height);
    }

    private Rectangle calculateSelectionArea(EditorEx editor, int selectionStart, int selectionEnd) {
        Rectangle2D selectedView = new Rectangle2D.Double();

        String selectedText = editor.getDocument().getText(
                new TextRange(selectionStart, selectionEnd));

        SoftWrapModelEx wrapModel = editor.getSoftWrapModel();
        int symbolWidth1 = wrapModel.getMinDrawingWidthInPixels(SoftWrapDrawingType.BEFORE_SOFT_WRAP_LINE_FEED);
        int symbolWidth2 = wrapModel.getMinDrawingWidthInPixels(SoftWrapDrawingType.AFTER_SOFT_WRAP);
        int wsCharWidth = EditorUtil.charWidth(' ', Font.PLAIN, editor);

        int leftEdge = 0;
        int upperEdge = 0;
        int rightEdge = 0;
        int bottomEdge = 0;
        if (selectedText.isEmpty()) { // user select nothing
            int lineCount = editor.getDocument().getLineCount();

            // constructing a huge image, promote user to confirm
            if (lineCount > HUGE_FILE_THRESHOLD) {
                DialogBuilder dialog = new DialogBuilder(editor.getProject());
                dialog.setTitle(UiBundle.message("plugin.title"));
                dialog.setCenterPanel(new JLabel(UiBundle.message("promote.hugeFile", lineCount)));
                boolean continues = dialog.showAndGet();
                if (!continues) {
                    ScreenshotException e = new ScreenshotException();
                    e.hide = true;
                    snapshot.restore();
                    throw e;
                }
            }

            for (int i = 0; i <= lineCount; i++) {
                // add soft warp stops
                for (SoftWrap softWrap : wrapModel.getSoftWrapsForLine(i)) {
                    Point point = editor.offsetToXY(softWrap.getStart() - 1);
                    rightEdge = Math.max(rightEdge, point.x + wsCharWidth + symbolWidth1);
                }
                // add line ends
                LogicalPosition lastColumn = new LogicalPosition(i, Integer.MAX_VALUE);
                int offset = editor.logicalPositionToOffset(lastColumn);
                Point point = editor.offsetToXY(offset);
                rightEdge = Math.max(rightEdge, point.x);
                bottomEdge = point.y;
            }
        } else {
            boolean indentRegion = true;

            if (options.myChopIndentation) leftEdge = Integer.MAX_VALUE;

            for (int i = selectionStart; i <= selectionEnd; i++) {
                VisualPosition pointOnScreen = editor.offsetToVisualPosition(i);
                Point upperLeft = editor.visualPositionToXY(pointOnScreen);

                if (i == selectionStart) {
                    upperEdge = upperLeft.y;
                } else if (i == selectionEnd) {
                    bottomEdge = upperLeft.y;
                } else {
                    rightEdge = Math.max(rightEdge, upperLeft.x);

                    char current = selectedText.charAt(i - selectionStart);
                    if (current == '\n') {
                        indentRegion = true;
                    } else if (indentRegion && !Character.isWhitespace(current)) {
                        indentRegion = false;
                        if (options.myChopIndentation) {
                            leftEdge = Math.min(leftEdge, upperLeft.x);
                        }
                    }

                    if (!wrapModel.isSoftWrappingEnabled()) continue;
                    if (wrapModel.getSoftWrap(i) != null) { // wrap sign on visual line start
                        leftEdge = Math.min(leftEdge, upperLeft.x - symbolWidth2);
                    }
                    if (wrapModel.getSoftWrap(i + 1) != null) { // wrap sign on visual line end
                        rightEdge = Math.max(rightEdge, upperLeft.x + wsCharWidth + symbolWidth1);
                    }
                }
            }
        }
        bottomEdge += editor.getLineHeight();
        Dimension contentSize = editor.getContentSize();
        // there is a small cut off on right edge of the last character,
        // so extend width by 1 scaled pixel.
        contentSize.width = rightEdge - leftEdge + JBUI.scale(1);
        contentSize.height = bottomEdge - upperEdge;
        selectedView.setFrame(new Point(leftEdge, upperEdge), contentSize);
        return selectedView.getBounds(); // case to int
    }

    /** Store editor state before capture */
    private static class StateSnapshot {

        final EditorEx editor;

        final int caretOffset;

        final int selectionStart;
        final int selectionEnd;

        // editor
        final boolean isIndentGuidesShown;
        final boolean isInnerWhitespaceShown;

        // gutter
        final boolean isLineNumberShown;
        final boolean isGutterIconShown;
        final boolean isFoldingOutlineShown;

        StateSnapshot(EditorEx editor) {
            this.editor = editor;

            CaretModel caretModel = editor.getCaretModel();
            caretOffset = caretModel.getOffset();

            SelectionModel selectionModel = editor.getSelectionModel();
            selectionStart = selectionModel.getSelectionStart();
            selectionEnd = selectionModel.getSelectionEnd();

            EditorSettings settings = editor.getSettings();
            isLineNumberShown = settings.isLineNumbersShown();
            isGutterIconShown = settings.areGutterIconsShown();
            isFoldingOutlineShown = settings.isFoldingOutlineShown();
            isIndentGuidesShown = settings.isIndentGuidesShown();
            isInnerWhitespaceShown = settings.isInnerWhitespaceShown();
        }

        /** Update editor appearances according to user provided setting. */
        void tweakEditor(CopyImageOptionsProvider.State options) {
            editor.getSelectionModel().setSelection(0, 0); // clear selection

            if (options.myRemoveCaret) {
                editor.setCaretEnabled(false);
                int start = editor.getSelectionModel().getSelectionStart();
                DocumentEx document = editor.getDocument();
                CaretModel caretModel = editor.getCaretModel();
                caretModel.moveToOffset(start == 0 ? document.getLineEndOffset(document.getLineCount() - 1) : 0);
            }

            EditorSettings settings = editor.getSettings();
            settings.setLineNumbersShown(options.myShowLineNumber);
            settings.setGutterIconsShown(options.myShowGutterIcon);
            settings.setFoldingOutlineShown(options.myShowFoldOutline);
            settings.setInnerWhitespaceShown(options.myShowInnerWhitespace);
            settings.setIndentGuidesShown(options.myShowIndentGuide);
            editor.getScrollPane().validate();
        }

        void restore() {
            editor.setCaretEnabled(true);

            CaretModel caretModel = editor.getCaretModel();
            caretModel.moveToOffset(caretOffset);

            SelectionModel selectionModel = editor.getSelectionModel();
            selectionModel.setSelection(selectionStart, selectionEnd);

            EditorSettings settings = editor.getSettings();
            settings.setLineNumbersShown(isLineNumberShown);
            settings.setGutterIconsShown(isGutterIconShown);
            settings.setFoldingOutlineShown(isFoldingOutlineShown);
            settings.setIndentGuidesShown(isIndentGuidesShown);
            settings.setInnerWhitespaceShown(isInnerWhitespaceShown);
            editor.getScrollPane().validate();
        }
    }

    /** helper methods to execute private methods */
    private static Method findMethod(Object obj, String methodName, Class<?>... paramTypes) {
        try {
            return obj.getClass().getDeclaredMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /** helper methods to execute private methods */
    private static <T> T invokeMethod(Method method, Object instance, Object... params) {
        try {
            method.setAccessible(true);
            //noinspection unchecked
            return (T) method.invoke(instance, params);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
