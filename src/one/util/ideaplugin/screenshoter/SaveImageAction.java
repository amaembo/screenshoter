package one.util.ideaplugin.screenshoter;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
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
        Editor editor = CopyImagePlugin.getEditor(event);
        if (editor == null) return;

        ImageBuilder imageBuilder = new ImageBuilder(editor);
        if (imageBuilder.getSelectedSize() > CopyImageAction.SIZE_LIMIT_TO_WARN) {
            if (Messages.showYesNoDialog(event.getProject(),
                "Saving such a big image could be slow and may take a lot of memory. Proceed?",
                "Code Screenshots", "Yes, Save It!", "Cancel", null) != Messages.YES) {
                return;
            }
        }
        BufferedImage image = imageBuilder.createImage();
        saveImage(editor, image);
    }

    private void saveImage(@NotNull Editor editor, @NotNull BufferedImage image) {
        Project project = editor.getProject();
        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(project).getState();
        String toSave = options.myDirectoryToSave;
        if (StringUtil.isEmpty(toSave)) {
            toSave = SystemProperties.getUserHome();
        }
        toSave = toSave.trim();
        LocalDateTime now = LocalDateTime.now();
        String date = DATE_TIME_PATTERN.format(now);
        String fileName = "Shot_" + date + ".png";
        Path path = Paths.get(FileUtil.toSystemDependentName(toSave), fileName);
        try {
            Files.createDirectories(path.getParent());

            try (OutputStream os = Files.newOutputStream(path)) {
                ImageIO.write(image, "png", os);
            }

            String pathRepresentation = StringUtil.escapeXmlEntities(StringUtil.shortenPathWithEllipsis(path.toString(), 50));
            NotificationListener listener = (notification, hyperlinkEvent) -> {
                try {
                    Desktop.getDesktop().open(path.toFile());
                } catch (IOException e) {
                    showError(project, "Cannot open image:  " + StringUtil.escapeXmlEntities(
                            path.toString()) + ":<br>" + StringUtil.escapeXmlEntities(
                            StringUtil.notNullize(e.getLocalizedMessage())));
                }
            };
            String openLink = Desktop.isDesktopSupported() ? "<a href=''>Open</a>" : "";
            NotificationGroupManager.getInstance().getNotificationGroup("image.saved.id")
                    .createNotification("Image was saved:",
                            pathRepresentation + "<br>" + openLink,
                            NotificationType.INFORMATION, listener)
                    .notify(project);
        } catch (FileAlreadyExistsException e) {
            showError(project, "Cannot save image:  " + StringUtil.escapeXmlEntities(
                    path.toString()) + ":<br>Not a directory: " + StringUtil.escapeXmlEntities(e.getFile()));
        } catch (IOException e) {
            showError(project, "Cannot save image:  " + StringUtil.escapeXmlEntities(
                    path.toString()) + ":<br>" + StringUtil.escapeXmlEntities(
                    StringUtil.notNullize(e.getLocalizedMessage())));
        }
    }

    private void showError(Project project, String error) {
        NotificationGroupManager.getInstance().getNotificationGroup("image.saved.id")
            .createNotification(error, MessageType.ERROR).notify(project);
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(CopyImagePlugin.getEditor(event) != null);
    }
}
