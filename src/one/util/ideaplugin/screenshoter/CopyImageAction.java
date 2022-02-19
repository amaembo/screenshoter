package one.util.ideaplugin.screenshoter;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import kotlin.collections.ArraysKt;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

/**
 * @author Tagir Valeev
 */
public class CopyImageAction extends DumbAwareAction {

    static final long SIZE_LIMIT_TO_WARN = 3_000_000L;

    private static final DataFlavor SVG_FLAVOR =
        new DataFlavor("image/svg+xml; class=java.io.InputStream", "Scalable Vector Graphic");

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        Editor editor = CopyImagePlugin.getEditor(event);
        if (editor == null) {
            CopyImagePlugin.showError(project, "'Copy as Image' is available in text editors only");
            return;
        }
        if (!editor.getSelectionModel().hasSelection()) {
            CopyImagePlugin.showError(project, "Please select the text fragment to copy");
            return;
        }

        ImageBuilder imageBuilder = new ImageBuilder(editor);
        if (imageBuilder.getSelectedSize() > SIZE_LIMIT_TO_WARN) {
            if (Messages.showYesNoDialog(event.getProject(),
                "Copying such a big image could be slow and may take a lot of memory. Proceed?",
                "Code Screenshots", "Yes, Copy It!", "Cancel", null) != Messages.YES) {
                return;
            }
        }
        GraphicsBuffer image = imageBuilder.createImage();

        // hm... SVG generation should be inside the according lambda but that would give us an empty image
        CharArrayWriter writer = new CharArrayWriter();
        try {
            image.stream(writer, true);
        } catch (SVGGraphics2DIOException e) {
            Logger.getInstance(CopyImageAction.class).error(e);
            return;
        }
        char[] chars = writer.toCharArray();
        // end hm...

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(new EasyTransferable(flavor -> Format.rasterize(image), DataFlavor.imageFlavor), null);
        Notification notification = CopyImagePlugin.getNotificationGroup()
            .createNotification("Image was copied to the clipboard as PNG", NotificationType.INFORMATION)
            .setTitle("Code screenshots");

        notification.addAction(DumbAwareAction.create("Copy as SVG", ev -> {
            notification.expire();
            clipboard.setContents(
                new EasyTransferable(
                    flavor -> // hm... SVG generation should be here
                        DataFlavor.stringFlavor.equals(flavor) ? new String(chars) : new CharArrayReader(chars),
                    SVG_FLAVOR, DataFlavor.stringFlavor, DataFlavor.plainTextFlavor
                ),
                null
            );
        }));
        notification.notify(editor.getProject());
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(CopyImagePlugin.getEditor(event) != null);
    }

    private static final class EasyTransferable implements Transferable {

        private final Function<DataFlavor, Object> getTransferData;
        private final DataFlavor[] flavors;

        EasyTransferable(Function<DataFlavor, Object> getTransferData, DataFlavor... flavors) {
            this.getTransferData = getTransferData;
            this.flavors = flavors;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return flavors.clone();
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return ArraysKt.contains(flavors, flavor);
        }

        @NotNull @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            try {
                return getTransferData.apply(flavor);
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
    }

}
