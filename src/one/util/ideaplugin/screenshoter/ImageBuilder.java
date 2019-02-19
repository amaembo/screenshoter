package one.util.ideaplugin.screenshoter;


import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.SoftWrapModelEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.editor.impl.softwrap.SoftWrapDrawingType;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.intellij.codeInsight.hint.EditorFragmentComponent.getBackgroundColor;

class ImageBuilder {

    @NotNull
    private final EditorEx editor;

    ImageBuilder(@NotNull EditorEx editor) {
        this.editor = editor;
    }

    @NotNull
    BufferedImage createImage() {
        SelectionModel selectionModel = editor.getSelectionModel();
        int start = selectionModel.getSelectionStart();
        int end = selectionModel.getSelectionEnd();

        selectionModel.setSelection(0, 0);

        CaretModel caretModel = editor.getCaretModel();
        
        Document document = editor.getDocument();
        int offset = caretModel.getOffset();
        
        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(editor.getProject()).getState();
        if (options.myRemoveCaret) {
            caretModel.moveToOffset(start == 0 ? document.getLineEndOffset(document.getLineCount() - 1) : 0);
        }

        double scale = options.myScale;
        JComponent contentComponent = editor.getContentComponent();
        Graphics2D contentGraphics = (Graphics2D) contentComponent.getGraphics();
        AffineTransform currentTransform = contentGraphics.getTransform();
        AffineTransform newTransform = new AffineTransform(currentTransform);
        newTransform.scale(scale, scale);
        // To flush glyph cache
        paint(contentComponent, newTransform, 1, 1);

        Rectangle2D r = calculateSelectionArea(options, editor, start, end);

        newTransform.translate(-r.getX(), -r.getY());
        BufferedImage editorImage = paint(contentComponent, newTransform,
                (int) (r.getWidth() * scale), (int) (r.getHeight() * scale));
        BufferedImage image = padding(options, editor, editorImage);

        selectionModel.setSelection(start, end);
        caretModel.moveToOffset(offset);
        return image;
    }


    private BufferedImage padding(CopyImageOptionsProvider.State options,
                                  @NotNull Editor editor,
                                  @NotNull BufferedImage image) {
        int padding = options.myPadding;
        if (padding == 0) return image;

        int sizeX = image.getWidth() + padding * 2;
        int sizeY = image.getHeight() + padding * 2;
        //noinspection UndesirableClassUsage / already scaled
        BufferedImage newImage = new BufferedImage(sizeX, sizeY, BufferedImage.TYPE_INT_RGB);
        
        Color backgroundColor = getBackgroundColor(editor, false);
        Graphics g = newImage.getGraphics();
        g.setColor(backgroundColor);
        g.fillRect(0, 0, sizeX, sizeY);
        g.drawImage(image, padding, padding, null);
        g.dispose();

        return newImage;
    }

    private Rectangle2D calculateSelectionArea(CopyImageOptionsProvider.State options, EditorEx editor, int selectionStart, int selectionEnd) {
        Rectangle2D selectedView = new Rectangle2D.Double();

        String selectedText = editor.getDocument().getText(
                new TextRange(selectionStart, selectionEnd));

        SoftWrapModelEx wrapModel = editor.getSoftWrapModel();
        int symbolWidth = wrapModel.getMinDrawingWidthInPixels(SoftWrapDrawingType.BEFORE_SOFT_WRAP_LINE_FEED);
        int wsCharWidth = EditorUtil.charWidth(' ', Font.PLAIN, editor);

        int leftEdge = 0;
        int upperEdge = 0;
        int rightEdge = 0;
        int bottomEdge = 0;
        if (selectedText.isEmpty()) { // user select nothing
            int lineCount = editor.getDocument().getLineCount();

            for (int i = 0; i <= lineCount; i++) {
                // add soft warp stops
                for (SoftWrap softWrap : wrapModel.getSoftWrapsForLine(i)) {
                    Point point = Compatibility.offsetToXY(editor, softWrap.getStart() - 1);
                    rightEdge = Math.max(rightEdge, point.x + wsCharWidth + symbolWidth);
                }
                // add line ends
                LogicalPosition lastColumn = new LogicalPosition(i, Integer.MAX_VALUE);
                int offset = editor.logicalPositionToOffset(lastColumn);
                Point point = Compatibility.offsetToXY(editor, offset);
                rightEdge = Math.max(rightEdge, point.x);
                bottomEdge = point.y;
            }
            bottomEdge += editor.getLineHeight();
        } else {
            boolean indentRegion = true;

            if (options.myChopIndentation) {
                leftEdge = Integer.MAX_VALUE;
            }

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

                    if (!wrapModel.isSoftWrappingEnabled()) {
                        continue;
                    }
                    if (wrapModel.getSoftWrap(i) != null) { // wrap sign on visual line start
                        leftEdge = Math.min(leftEdge, upperLeft.x);
                    }
                    if (wrapModel.getSoftWrap(i + 1) != null) { // wrap sign on visual line end
                        rightEdge = Math.max(rightEdge, upperLeft.x + wsCharWidth + symbolWidth);
                    }
                }
            }
            if (selectedText.charAt(selectedText.length() - 1) != '\n') {
                bottomEdge += editor.getLineHeight();
            }
        }
        Dimension contentSize = editor.getContentSize();
        // there is a small cut off on right edge of the last character,
        // so extend width by 1 scaled pixel.
        contentSize.width = rightEdge - leftEdge + JBUI.scale(1);
        contentSize.height = bottomEdge - upperEdge;
        selectedView.setFrame(new Point(leftEdge, upperEdge), contentSize);
        return selectedView;
    }

    @NotNull
    private static BufferedImage paint(JComponent contentComponent, AffineTransform at, int width, int height) {
        BufferedImage img = Compatibility.createImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = (Graphics2D) img.getGraphics();
        graphics.setTransform(at);
        contentComponent.paint(graphics);
        return img;
    }

    @SuppressWarnings({"JavadocReference", "SameParameterValue", "JavaReflectionMemberAccess"})
    private static class Compatibility {
        /**
         * {@link com.intellij.util.ui.UIUtil#createImage(int, int, int)}
         */
        private static final Method utilCreateImage;

        /**
         * {@link com.intellij.openapi.editor.Editor#offsetToXY(int)}
         */
        private static final Method editorOffsetToXY;

        static {
            Method method;

            method = null;
            try {
                method = UIUtil.class.getMethod("createImage", int.class, int.class, int.class);
            } catch (NoSuchMethodException ignored) {
            }
            utilCreateImage = method;

            method = null;
            try {
                method = Editor.class.getMethod("offsetToXY", int.class);
            } catch (NoSuchMethodException ignored) {
            }
            editorOffsetToXY = method;
        }

        private static BufferedImage createImage(int width, int height, int type) {
            if (utilCreateImage == null) {
                //noinspection UndesirableClassUsage
                return new BufferedImage(width, height, type);
            }
            return (BufferedImage) invoke(utilCreateImage, null, width, height, type);
        }

        private static Point offsetToXY(Editor editor, int offset) {
            if (editorOffsetToXY == null) {
                VisualPosition visualPosition = editor.offsetToVisualPosition(offset);
                return editor.visualPositionToXY(visualPosition);
            }
            return (Point) invoke(editorOffsetToXY, editor, offset);
        }

        private static Object invoke(Method method, Object bind, Object... params) {
            try {
                method.setAccessible(true);
                return method.invoke(bind, params);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
