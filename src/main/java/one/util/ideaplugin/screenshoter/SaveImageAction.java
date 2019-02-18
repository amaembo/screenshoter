package one.util.ideaplugin.screenshoter;


import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
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

import static one.util.ideaplugin.screenshoter.CopyImagePlugin.NOTIFICATION_GROUP;


public class SaveImageAction extends AnAction {

    private static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    @Override
    @SuppressWarnings("Duplicates")
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        Editor editor = CopyImagePlugin.getEditor(anActionEvent);
        if (editor == null) return;

        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(editor.getProject()).getState();
        ImageProvider provider = ImageProvider.forOption(options);
        if (!provider.isCapable(editor)) {
            provider = ImageProvider.defaultProvider(options);
        }

        BufferedImage image;
        try {
            image = provider.takeScreenshot(editor);
        } catch (ScreenshotException e) {
            if (e.hide) return;
            throw e;
        }
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

            String pathRepresentation = StringUtil.escapeXml(StringUtil.shortenPathWithEllipsis(path.toString(), 50));
            Notification notification = NOTIFICATION_GROUP
                    .createNotification(
                            UiBundle.message("notify.saveToDisk"),
                            null,
                            pathRepresentation,
                            NotificationType.INFORMATION
                    );

            if (Desktop.isDesktopSupported()) {
                notification.addAction(new AnAction("Open") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent event) {
                        try {
                            Desktop.getDesktop().open(path.toFile());
                        } catch (IllegalArgumentException e) {
                            // file doesn't exist
                        } catch (IOException e) {
                            String message = UiBundle.message("notify.error.cannotOpen",
                                    StringUtil.escapeXml(path.toString()),
                                    StringUtil.escapeXml(StringUtil.notNullize(e.getLocalizedMessage())));
                            showError(project, message);
                        }
                    }
                });
            }
            notification.notify(project);
        } catch (FileAlreadyExistsException e) {
            String message = UiBundle.message("notify.error.notDirectory",
                    StringUtil.escapeXml(path.toString()),
                    StringUtil.escapeXml(e.getFile()));
            showError(project, message);
        } catch (IOException e) {
            String message = UiBundle.message("notify.error.ioe",
                    StringUtil.escapeXml(path.toString()),
                    StringUtil.escapeXml(StringUtil.notNullize(e.getLocalizedMessage())));
            showError(project, message);
        }
    }

    private void showError(Project project, String error) {
        NOTIFICATION_GROUP.createNotification(error, MessageType.ERROR).notify(project);
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(CopyImagePlugin.getEditor(event) != null);
    }
}
