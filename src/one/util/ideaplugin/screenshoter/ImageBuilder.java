package one.util.ideaplugin.screenshoter;


import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Inlay;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.ui.UIUtil;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.regex.Pattern;

import static com.intellij.codeInsight.hint.EditorFragmentComponent.getBackgroundColor;

class ImageBuilder {
    private static final Pattern EMPTY_SUFFIX = Pattern.compile("\n\\s+$");

    @NotNull
    private final Editor editor;
    @NotNull
    private final Project project;

    ImageBuilder(@NotNull Editor editor) {
        this.editor = editor;
        this.project = Objects.requireNonNull(editor.getProject());
    }

    @NotNull
    BufferedImage createImage() {
        TextRange range = getRange(editor);

        Document document = editor.getDocument();
        EditorState state = EditorState.from(editor);
        try {
            resetEditor();
            CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(project).getState();
            double scale = options.myScale;
            JComponent contentComponent = editor.getContentComponent();
            Graphics2D contentGraphics = (Graphics2D) contentComponent.getGraphics();
            AffineTransform currentTransform = contentGraphics.getTransform();
            AffineTransform newTransform = new AffineTransform(currentTransform);
            newTransform.scale(scale, scale);
            // To flush glyph cache
            paint(contentComponent, newTransform, 1, 1);
            String text = document.getText(range);
            Rectangle2D r = getSelectionRectangle(range, text, options);

            newTransform.translate(-r.getX(), -r.getY());
            BufferedImage editorImage = paint(contentComponent, newTransform,
                (int) (r.getWidth() * scale), (int) (r.getHeight() * scale));
            return padding(options, editor, editorImage);
        }
        finally {
            state.restore(editor);
        }
    }

    private void resetEditor() {
        Document document = editor.getDocument();
        TextRange range = getRange(editor);
        editor.getSelectionModel().setSelection(0, 0);
        if (CopyImageOptionsProvider.getInstance(project).getState().myRemoveCaret) {
            editor.getCaretModel().moveToOffset(range.getStartOffset() == 0 ?
                document.getLineEndOffset(document.getLineCount() - 1) : 0);
            if (editor instanceof EditorEx) {
                ((EditorEx) editor).setCaretEnabled(false);
            }
            editor.getSettings().setCaretRowShown(false);
        }
    }

    long getSelectedSize() {
        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(project).getState();
        Rectangle2D rectangle = getSelectionRectangle();
        double sizeX = rectangle.getWidth() + options.myPadding * 2;
        double sizeY = rectangle.getHeight() + options.myPadding * 2;
        return (long)(sizeX * sizeY * options.myScale * options.myScale);
    }

    @NotNull
    private Rectangle2D getSelectionRectangle() {
        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(project).getState();
        TextRange range = getRange(editor);
        Document document = editor.getDocument();
        String text = document.getText(range);
        return getSelectionRectangle(range, text, options);
    }

    private static TextRange getRange(Editor editor) {
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
            Rectangle bounds = inlay.getBounds();
            if (bounds != null) {
                r.add(bounds);
            }
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

    public static class EditorState {
        private final TextRange range;
        private final int offset;
        private final boolean caretRow;

        EditorState(TextRange range, int offset, boolean caretRow) {
            this.range = range;
            this.offset = offset;
            this.caretRow = caretRow;
        }

        @NotNull
        static EditorState from(Editor editor) {
            TextRange range = getRange(editor);
            int offset = editor.getCaretModel().getOffset();

            return new EditorState(range, offset, editor.getSettings().isCaretRowShown());
        }

        void restore(Editor editor) {
            editor.getSettings().setCaretRowShown(caretRow);
            SelectionModel selectionModel = editor.getSelectionModel();
            CaretModel caretModel = editor.getCaretModel();
            CopyImageOptionsProvider provider =
              CopyImageOptionsProvider.getInstance(Objects.requireNonNull(editor.getProject()));
            if (provider.getState().myRemoveCaret) {
                if (editor instanceof EditorEx) {
                    ((EditorEx) editor).setCaretEnabled(true);
                }
                caretModel.moveToOffset(offset);
            }
            selectionModel.setSelection(range.getStartOffset(), range.getEndOffset());
        }
    }
}
