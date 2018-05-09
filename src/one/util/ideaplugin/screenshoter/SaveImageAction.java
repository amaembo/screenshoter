package one.util.ideaplugin.screenshoter;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class SaveImageAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Editor editor = CopyImagePlugin.getEditor(anActionEvent);
        if (editor == null) return;

        BufferedImage image = new ImageBuilder(editor).createImage();
        saveImage(editor, image);
    }

    private void saveImage(@NotNull Editor editor, @NotNull BufferedImage image) {
        Project project = editor.getProject();
        CopyImageOptionsProvider.State options = CopyImageOptionsProvider.getInstance(project).getState();
        String toSave = options.myDirectoryToSave;
        if (StringUtil.isEmpty(toSave)) {
            toSave = SystemProperties.getUserHome();
        }
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String date = formatter.format(now);
        File outFile = new File(FileUtil.toSystemDependentName(toSave + "/" + "Shot_" + date + ".png"));
        try {
            FileUtil.createParentDirs(outFile);

            ImageIO.write(image, "png", outFile);

            CopyImagePlugin.NOTIFICATION_GROUP
                    .createNotification("Image \n" + outFile.getPath() + "\nwas saved", MessageType.INFO)
                    .notify(project);
        } catch (IOException e) {
            CopyImagePlugin.NOTIFICATION_GROUP
                    .createNotification("Cannot save image:  " + outFile.getPath() + ":\n" + StringUtil.notNullize(e.getLocalizedMessage()), MessageType.ERROR)
                    .notify(project);
        }
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(CopyImagePlugin.getEditor(event) != null);
    }
}
