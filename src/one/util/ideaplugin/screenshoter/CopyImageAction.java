package one.util.ideaplugin.screenshoter;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author Tagir Valeev
 */
public class CopyImageAction extends AnAction {
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

    @Override
    public void actionPerformed(AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
        if(editor == null) return;

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
            includePoint(r, new Point2D.Double(point.getX(), point.getY()+editor.getLineHeight()));
        }

        newTransform.translate(-r.getX(), -r.getY());
        BufferedImage image = paint(contentComponent, newTransform,
                (int) (r.getWidth() * scale), (int) (r.getHeight() * scale));

        selectionModel.setSelection(start, end);
        caretModel.moveToOffset(offset);

        TransferableImage transferableImage = new TransferableImage(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(transferableImage, (clipboard1, contents) -> { });
    }

    private static void includePoint(Rectangle2D r, Point2D p) {
        if(r.isEmpty()) {
            r.setFrame(p, new Dimension(1, 1));
        } else {
            r.add(p);
        }
    }

    @NotNull
    private static BufferedImage paint(JComponent contentComponent, AffineTransform at, int width, int height) {
        BufferedImage img = null;
        if(utilCreateImage != null) {
            try {
                img = (BufferedImage) utilCreateImage.invoke(null, contentComponent, width, height, BufferedImage
                        .TYPE_INT_RGB);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        if(img == null) {
            //noinspection UndesirableClassUsage
            img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D graphics = (Graphics2D) img.getGraphics();
        graphics.setTransform(at);
        contentComponent.paint(graphics);
        return img;
    }

    private Point2D getPoint(Editor editor, VisualPosition pos) {
        return editor.visualPositionToXY(pos);
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        DataContext dataContext = event.getDataContext();
        Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
        presentation.setEnabled(editor != null);
    }

    static class TransferableImage implements Transferable {
        Image image;

        TransferableImage( Image image ) {
            this.image = image;
        }

        public Object getTransferData( DataFlavor flavor ) throws UnsupportedFlavorException, IOException {
            if ( flavor.equals( DataFlavor.imageFlavor ) && image != null ) {
                return image;
            }
            throw new UnsupportedFlavorException( flavor );
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        public boolean isDataFlavorSupported( DataFlavor flavor ) {
            return Arrays.stream(getTransferDataFlavors()).anyMatch(flavor::equals);
        }
    }
}
