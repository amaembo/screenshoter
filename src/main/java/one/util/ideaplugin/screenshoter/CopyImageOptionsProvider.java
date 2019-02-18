package one.util.ideaplugin.screenshoter;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author Tagir Valeev
 */
@State(
        name = "CopyImageOptionsProvider",
        storages = {
                @Storage(StoragePathMacros.WORKSPACE_FILE)
        }
)
public class CopyImageOptionsProvider implements PersistentStateComponent<CopyImageOptionsProvider.State> {

    private final State myState = new State();

    static CopyImageOptionsProvider getInstance(Project project) {
        return ServiceManager.getService(project, CopyImageOptionsProvider.class);
    }

    @NotNull
    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState.myScale = state.myScale;
        myState.myDirectoryToSave = state.myDirectoryToSave;
        myState.myPadding = state.myPadding;
        if (state.myPadding < 0) myState.myPadding = 0; // restricted to positive number

        // editor
        myState.myChopIndentation = state.myChopIndentation;
        myState.myRemoveCaret = state.myRemoveCaret;
        myState.myShowIndentGuide = state.myShowIndentGuide;
        myState.myShowInnerWhitespace = state.myShowInnerWhitespace;

        // gutter
        myState.myEnableGutter = state.myEnableGutter;
        myState.myShowLineNumber = state.myShowLineNumber;
        myState.myShowGutterIcon = state.myShowGutterIcon;
        myState.myShowFoldOutline = state.myShowFoldOutline;
    }

    @SuppressWarnings("WeakerAccess")
    public static class State {
        public double myScale = 4;
        public String myDirectoryToSave = null;
        public int myPadding = 0;

        // editor
        public boolean myChopIndentation = true;
        public boolean myRemoveCaret = true;
        public boolean myShowIndentGuide = true;
        public boolean myShowInnerWhitespace = false;

        // gutter
        public boolean myEnableGutter = false;
        public boolean myShowLineNumber = true;
        public boolean myShowGutterIcon = false;
        public boolean myShowFoldOutline = false;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return Double.compare(state.myScale, myScale) == 0 &&
                   myPadding == state.myPadding &&
                   myChopIndentation == state.myChopIndentation &&
                   myRemoveCaret == state.myRemoveCaret &&
                   myShowIndentGuide == state.myShowIndentGuide &&
                   myShowInnerWhitespace == state.myShowInnerWhitespace &&
                   myEnableGutter == state.myEnableGutter &&
                   myShowLineNumber == state.myShowLineNumber &&
                   myShowGutterIcon == state.myShowGutterIcon &&
                   myShowFoldOutline == state.myShowFoldOutline &&
                   Objects.equals(myDirectoryToSave, state.myDirectoryToSave);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myScale, myDirectoryToSave, myPadding,
                    myChopIndentation, myRemoveCaret, myShowIndentGuide, myShowInnerWhitespace,
                    myEnableGutter, myShowLineNumber, myShowGutterIcon, myShowFoldOutline);
        }
    }
}
