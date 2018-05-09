package one.util.ideaplugin.screenshoter;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

class CopyImagePlugin {
    static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup(
            "image.saved.id",
            NotificationDisplayType.STICKY_BALLOON,
            false,
            null
    );

    static Editor getEditor(@NotNull AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        return PlatformDataKeys.EDITOR.getData(dataContext);
    }
}
