package one.util.ideaplugin.screenshoter;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.ui.MessageType;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Arrays;

import static one.util.ideaplugin.screenshoter.CopyImagePlugin.NOTIFICATION_GROUP;

/**
 * @author Tagir Valeev
 */
public class CopyImageAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Editor editor = CopyImagePlugin.getEditor(event);
        if (editor == null) return;

        Image image = new ImageBuilder((EditorEx) editor).createImage();

        TransferableImage transferableImage = new TransferableImage(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(transferableImage, (clipboard1, contents) -> {
        });
        NOTIFICATION_GROUP
                .createNotification("Image was copied to the clipboard", MessageType.INFO)
                .notify(editor.getProject());
    }
    
    @Override
    public void update(@NotNull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(CopyImagePlugin.getEditor(event) instanceof EditorEx);
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
