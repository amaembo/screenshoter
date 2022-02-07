package one.util.ideaplugin.screenshoter;

import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class SaveImageAction extends AnAction {

    private static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;
        Editor editor = CopyImagePlugin.getEditor(event);
        if (editor == null) {
            CopyImagePlugin.showError(project, "'Save as Image' is available in text editors only");
            return;
        }
        if (!editor.getSelectionModel().hasSelection()) {
            CopyImagePlugin.showError(project, "Please select the text fragment to save");
            return;
        }

        ImageBuilder imageBuilder = new ImageBuilder(editor);
        if (imageBuilder.getSelectedSize() > CopyImageAction.SIZE_LIMIT_TO_WARN) {
            if (Messages.showYesNoDialog(project,
                "Saving such a big image could be slow and may take a lot of memory. Proceed?",
                "Code Screenshots", "Yes, Save It!", "Cancel", null) != Messages.YES) {
                return;
            }
        }
        TransferableImage<?> image = imageBuilder.createImage();
        if (image != null) {
            saveImage(image, project);
        }
    }

    private void saveImage(@NotNull TransferableImage<?> image, @NotNull Project project) {
        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(project).getState();
        String toSave = options.myDirectoryToSave;
        if (StringUtil.isEmpty(toSave)) {
            toSave = SystemProperties.getUserHome();
        }
        toSave = toSave.trim();
        LocalDateTime now = LocalDateTime.now();
        String date = DATE_TIME_PATTERN.format(now);
        String fileName = "Shot_" + date + "." + image.format.ext;
        Path path = Paths.get(FileUtil.toSystemDependentName(toSave), fileName);
        try {
            Files.createDirectories(path.getParent());

            try (OutputStream os = Files.newOutputStream(path)) {
                image.write(os);
            }

            String pathRepresentation = StringUtil.escapeXmlEntities(StringUtil.shortenPathWithEllipsis(path.toString(), 50));
            NotificationListener listener = (notification, hyperlinkEvent) -> {
                try {
                    Desktop.getDesktop().open(path.toFile());
                } catch (IOException e) {
                    CopyImagePlugin.showError(project, "Cannot open image:  " + StringUtil.escapeXmlEntities(
                            path.toString()) + ":<br>" + StringUtil.escapeXmlEntities(
                            StringUtil.notNullize(e.getLocalizedMessage())));
                }
            };
            String openLink = Desktop.isDesktopSupported() ? "<a href=''>Open</a>" : "";
            CopyImagePlugin.getNotificationGroup()
                    .createNotification("Code screenshots", "Image was saved:", pathRepresentation + "<br>" + openLink,
                            NotificationType.INFORMATION, listener)
                    .notify(project);
        } catch (FileAlreadyExistsException e) {
            CopyImagePlugin.showError(project, "Cannot save image:  " + StringUtil.escapeXmlEntities(
                    path.toString()) + ":<br>Not a directory: " + StringUtil.escapeXmlEntities(e.getFile()));
        } catch (IOException e) {
            CopyImagePlugin.showError(project, "Cannot save image:  " + StringUtil.escapeXmlEntities(
                    path.toString()) + ":<br>" + StringUtil.escapeXmlEntities(
                    StringUtil.notNullize(e.getLocalizedMessage())));
        }
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(CopyImagePlugin.getEditor(event) != null);
    }
}
