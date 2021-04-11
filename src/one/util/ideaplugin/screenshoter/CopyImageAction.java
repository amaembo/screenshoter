package one.util.ideaplugin.screenshoter;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.Arrays;

/**
 * @author Tagir Valeev
 */
public class CopyImageAction extends DumbAwareAction {

    static final long SIZE_LIMIT_TO_WARN = 3_000_000L;

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Editor editor = CopyImagePlugin.getEditor(event);
        if (editor == null) return;

        ImageBuilder imageBuilder = new ImageBuilder(editor);
        if (imageBuilder.getSelectedSize() > SIZE_LIMIT_TO_WARN) {
            if (Messages.showYesNoDialog(event.getProject(),
                "Copying such a big image could be slow and may take a lot of memory. Proceed?",
                "Code Screenshots", "Yes, Copy It!", "Cancel", null) != Messages.YES) {
                return;
            }
        }
        Image image = imageBuilder.createImage();

        Transferable transferableImage = new TransferableImage(image);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(transferableImage, (clipboard1, contents) -> {
        });
        NotificationGroupManager.getInstance().getNotificationGroup("image.saved.id")
                .createNotification("Image was copied to the clipboard", MessageType.INFO)
                .notify(editor.getProject());
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(CopyImagePlugin.getEditor(event) != null);
    }

    static class TransferableImage implements Transferable {
        final Image image;

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
