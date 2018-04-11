package one.util.ideaplugin.screenshoter;


import com.intellij.openapi.editor.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

import static com.intellij.codeInsight.hint.EditorFragmentComponent.getBackgroundColor;

class ImageBuilder {
    private static final Pattern EMPTY_SUFFIX = Pattern.compile("\n\\s+$");

    private static final Method utilCreateImage;

    static {
        Method method = null;
        try {
            method = UIUtil.class.getMethod("createImage", Component.class, int.class, int.class, int.class);
        } catch (NoSuchMethodException ignored) {
        }
        utilCreateImage = method;
    }

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

        selectionModel.setSelection(0, 0);

        CaretModel caretModel = editor.getCaretModel();
        
        Document document = editor.getDocument();
        int offset = caretModel.getOffset();
        
        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(editor.getProject()).getState();
        if (options.myRemoveCaret) {
            caretModel.moveToOffset(start == 0 ? document.getLineEndOffset(document.getLineCount() - 1) : 0);
        }

        String text = document.getText(new TextRange(start, end));

        double scale = options.myScale;
        JComponent contentComponent = editor.getContentComponent();
        Graphics2D contentGraphics = (Graphics2D) contentComponent.getGraphics();
        AffineTransform currentTransform = contentGraphics.getTransform();
        AffineTransform newTransform = new AffineTransform(currentTransform);
        newTransform.scale(scale, scale);
        // To flush glyph cache
        paint(contentComponent, newTransform, 1, 1);
        Rectangle2D r = new Rectangle2D.Double();

        for (int i = start; i <= end; i++) {
            if (options.myChopIndentation &&
                    EMPTY_SUFFIX.matcher(text.substring(0, Math.min(i - start + 1, text.length()))).find()) {
                continue;
            }
            VisualPosition pos = editor.offsetToVisualPosition(i);
            Point2D point = getPoint(editor, pos);
            includePoint(r, point);
            includePoint(r, new Point2D.Double(point.getX(), point.getY() + editor.getLineHeight()));
        }

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

    private static void includePoint(Rectangle2D r, Point2D p) {
        if (r.isEmpty()) {
            r.setFrame(p, new Dimension(1, 1));
        } else {
            r.add(p);
        }
    }

    @NotNull
    private static BufferedImage paint(JComponent contentComponent, AffineTransform at, int width, int height) {
        BufferedImage img = null;
        if (utilCreateImage != null) {
            try {
                img = (BufferedImage) utilCreateImage.invoke(null, contentComponent, width, height, BufferedImage
                        .TYPE_INT_RGB);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        if (img == null) {
            //noinspection UndesirableClassUsage
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D graphics = (Graphics2D) img.getGraphics();
        graphics.setTransform(at);
        contentComponent.paint(graphics);
        return img;
    }

    private static Point2D getPoint(Editor editor, VisualPosition pos) {
        return editor.visualPositionToXY(pos);
    }
}
