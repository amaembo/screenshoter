package one.util.ideaplugin.screenshoter;

import com.intellij.openapi.components.*;
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
        myState.myRemoveCaret = state.myRemoveCaret;
        myState.myChopIndentation = state.myChopIndentation;
        myState.myDirectoryToSave = state.myDirectoryToSave;
        myState.myPadding = state.myPadding;
    }

    @SuppressWarnings("WeakerAccess")
    public static class State {
        public double myScale = 4;
        public boolean myRemoveCaret = true;
        public boolean myChopIndentation = true;
        public String myDirectoryToSave = null;
        public int myPadding = 0;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            State state = (State) o;
            return Double.compare(state.myScale, myScale) == 0 &&
                    myRemoveCaret == state.myRemoveCaret &&
                    myChopIndentation == state.myChopIndentation &&
                    myPadding == state.myPadding &&
                    Objects.equals(myDirectoryToSave, state.myDirectoryToSave);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myScale, myRemoveCaret, myChopIndentation, myDirectoryToSave, myPadding);
        }
    }
}
