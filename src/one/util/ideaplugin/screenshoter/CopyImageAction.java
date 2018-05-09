package one.util.ideaplugin.screenshoter;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Tagir Valeev
 */
public class CopyImageAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Editor editor = getEditor(event);
        if (editor == null) return;

        Image image = new ImageBuilder(editor).createImage();

        TransferableImage transferableImage = new TransferableImage(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(transferableImage, (clipboard1, contents) -> {
        });
    }
    
    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(getEditor(event) != null);
    }

    static Editor getEditor(@NotNull AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        return PlatformDataKeys.EDITOR.getData(dataContext);
    }

    static class TransferableImage implements Transferable {
        Image image;

        TransferableImage(Image image) {
            this.image = image;
        }

        @NotNull
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(DataFlavor.imageFlavor) && image != null) {
                return image;
            }
            throw new UnsupportedFlavorException(flavor);
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return Arrays.stream(getTransferDataFlavors()).anyMatch(flavor::equals);
        }
    }
}
