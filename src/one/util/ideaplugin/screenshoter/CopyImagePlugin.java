package one.util.ideaplugin.screenshoter;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class CopyImagePlugin {
    @Nullable
    static Editor getEditor(@NotNull AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        return PlatformDataKeys.EDITOR.getData(dataContext);
    }

    static NotificationGroup getNotificationGroup() {
        return NotificationGroupManager.getInstance().getNotificationGroup("Code Screenshots");
    }

    static void showError(@Nullable Project project, @NotNull String error) {
        getNotificationGroup()
            .createNotification("Code screenshots", null, error, NotificationType.ERROR).notify(project);
    }
}
