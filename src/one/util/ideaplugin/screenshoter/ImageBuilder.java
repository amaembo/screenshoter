package one.util.ideaplugin.screenshoter;


import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.regex.Pattern;

import static com.intellij.codeInsight.hint.EditorFragmentComponent.getBackgroundColor;

class ImageBuilder {
    private static final Pattern EMPTY_SUFFIX = Pattern.compile("\n\\s+$");

    @NotNull
    private final Editor editor;

    ImageBuilder(@NotNull Editor editor) {
        this.editor = editor;
    }

    @NotNull
    BufferedImage createImage() {
        SelectionModel selectionModel = editor.getSelectionModel();
        int start = selectionModel.getSelectionStart();
        int end = selectionModel.getSelectionEnd();

        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(editor.getProject()).getState();
        Rectangle2D r = getSelectionRectangle();

        CaretModel caretModel = editor.getCaretModel();

        selectionModel.setSelection(0, 0);
        int offset = caretModel.getOffset();
        if (options.myRemoveCaret) {
            if (editor instanceof EditorEx) {
                ((EditorEx) editor).setCaretEnabled(false);
            }
            Document document = editor.getDocument();
            caretModel.moveToOffset(start == 0 ? document.getLineEndOffset(document.getLineCount() - 1) : 0);
        }

        try {
            double scale = options.myScale;
            JComponent contentComponent = editor.getContentComponent();
            Graphics2D contentGraphics = (Graphics2D) contentComponent.getGraphics();
            AffineTransform currentTransform = contentGraphics.getTransform();
            AffineTransform newTransform = new AffineTransform(currentTransform);
            newTransform.scale(scale, scale);
            // To flush glyph cache
            paint(contentComponent, newTransform, 1, 1);

            newTransform.translate(-r.getX(), -r.getY());
            BufferedImage editorImage = paint(contentComponent, newTransform,
                (int) (r.getWidth() * scale), (int) (r.getHeight() * scale));
            return padding(options, editor, editorImage);
        }
        finally {
            if (options.myRemoveCaret) {
                if (editor instanceof EditorEx) {
                    ((EditorEx) editor).setCaretEnabled(true);
                }
                caretModel.moveToOffset(offset);
            }
            selectionModel.setSelection(start, end);
        }
    }

    long getSelectedSize() {
        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(editor.getProject()).getState();
        Rectangle2D rectangle = getSelectionRectangle();
        double sizeX = rectangle.getWidth() + options.myPadding * 2;
        double sizeY = rectangle.getHeight() + options.myPadding * 2;
        return (long)(sizeX * sizeY * options.myScale * options.myScale);
    }

    @NotNull
    Rectangle2D getSelectionRectangle() {
        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(editor.getProject()).getState();
        TextRange range = getRange();
        Document document = editor.getDocument();
        String text = document.getText(range);
        return getSelectionRectangle(range, text, options);
    }

    TextRange getRange() {
        SelectionModel selectionModel = editor.getSelectionModel();
        int start = selectionModel.getSelectionStart();
        int end = selectionModel.getSelectionEnd();
        return new TextRange(start, end);
    }

    @NotNull
    private Rectangle2D getSelectionRectangle(TextRange range, String text, CopyImageOptionsProvider.State options) {
        int start = range.getStartOffset();
        int end = range.getEndOffset();
        Rectangle2D r = new Rectangle2D.Double();

        for (int i = start; i <= end; i++) {
            if (options.myChopIndentation &&
                EMPTY_SUFFIX.matcher(text.substring(0, Math.min(i - start + 1, text.length()))).find()) {
                continue;
            }
            VisualPosition pos = editor.offsetToVisualPosition(i);
            Point2D point = editor.visualPositionToXY(pos);
            includePoint(r, point);
            includePoint(r, new Point2D.Double(point.getX(), point.getY() + editor.getLineHeight()));
        }
        for (Inlay<?> inlay : editor.getInlayModel().getInlineElementsInRange(start, end)) {
            r.add(inlay.getBounds());
        }
        return r;
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

    private static void includePoint(Rectangle2D r, Point2D p) {
        if (r.isEmpty()) {
            r.setFrame(p, new Dimension(1, 1));
        } else {
            r.add(p);
        }
    }

    @NotNull
    private static BufferedImage paint(JComponent contentComponent, AffineTransform at, int width, int height) {
        BufferedImage img = UIUtil.createImage(contentComponent, width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = (Graphics2D) img.getGraphics();
        graphics.setTransform(at);
        contentComponent.paint(graphics);
        return img;
    }
}
