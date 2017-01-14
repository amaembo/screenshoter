package one.util.ideaplugin.copier;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author Tagir Valeev
 */
public class CopyImageAction extends AnAction {
    private int myScale = 4;
    private boolean myRemoveCaret = true;
    private boolean myChopIndentation = true;

    private static final Pattern EMPTY_SUFFIX = Pattern.compile("\n\\s+$");

    private void includePoint(Rectangle r, Point p) {
        if(r.isEmpty()) {
            r.setLocation(p);
            r.setSize(1, 1);
        } else {
            r.add(p);
        }
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
        int offset = caretModel.getOffset();
        if(myRemoveCaret) {
            caretModel.moveToOffset(0);
        }

        Document document = editor.getDocument();
        String text = document.getText(new TextRange(start, end));

        Rectangle r = new Rectangle();
        for(int i=start; i<=end; i++) {
            if(myChopIndentation &&
                    EMPTY_SUFFIX.matcher(text.substring(0, Math.min(i-start+1, text.length()))).find()) {
                continue;
            }
            VisualPosition pos = editor.offsetToVisualPosition(i);
            includePoint(r, editor.visualPositionToXY(pos));
            pos = pos.leanRight(!pos.leansRight);
            includePoint(r, editor.visualPositionToXY(pos));
        }
        int nextLine = document.getLineNumber(end) + 1;
        if(nextLine < document.getLineCount()) {
            int nextLineStart = document.getLineStartOffset(nextLine);
            r.add(r.x, editor.visualPositionToXY(editor.offsetToVisualPosition(nextLineStart)).y);
        }

        JComponent contentComponent = editor.getContentComponent();

        BufferedImage image = UIUtil.createImage(r.width* myScale, r.height* myScale, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        AffineTransform at = new AffineTransform();
        at.scale(myScale, myScale);
        at.translate(-r.x, -r.y);
        g.setTransform(at);
        contentComponent.paint(g);

        selectionModel.setSelection(start, end);
        caretModel.moveToOffset(offset);

        TransferableImage transferableImage = new TransferableImage(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(transferableImage, (clipboard1, contents) -> { });
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        DataContext dataContext = event.getDataContext();
        Editor editor = PlatformDataKeys.EDITOR.getData(dataContext);
        presentation.setEnabled(editor != null);
    }

    class TransferableImage implements Transferable {
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
