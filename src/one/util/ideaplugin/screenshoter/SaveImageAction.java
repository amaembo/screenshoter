package one.util.ideaplugin.screenshoter;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
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
import java.text.SimpleDateFormat;
import java.util.Date;


public class SaveImageAction extends AnAction {

    private static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup(
            "image.saved.id",
            NotificationDisplayType.STICKY_BALLOON,
            false,
            null
    );

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Editor editor = CopyImageAction.geEditor(anActionEvent);
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
        try {
            SimpleDateFormat dt = new SimpleDateFormat("yyyyymmdd_hhmmss");
            String date = dt.format(new Date());

            File outputfile = new File(FileUtil.toSystemDependentName(toSave + "/" + "Shot_" + date + ".png"));
            ImageIO.write(image, "png", outputfile);

            NOTIFICATION_GROUP.createNotification("Image \n" + outputfile.getPath() + "\nwas saved",
                    MessageType.INFO).notify(project);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        presentation.setEnabled(CopyImageAction.geEditor(event) != null);
    }


}
