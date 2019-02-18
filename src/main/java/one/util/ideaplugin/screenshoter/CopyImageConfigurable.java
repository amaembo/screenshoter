package one.util.ideaplugin.screenshoter;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.SwingHelper;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Tagir Valeev
 */
public class CopyImageConfigurable implements SearchableConfigurable, Configurable.NoScroll {

    private final CopyImageOptionsProvider myManager;
    private final Project myProject;
    private CopyImageOptionsPanel myPanel;

    public CopyImageConfigurable(CopyImageOptionsProvider manager, Project project) {
        myManager = manager;
        this.myProject = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Copy code as image";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @NotNull
    @Override
    public String getId() {
        return "screenshoter";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        myPanel = new CopyImageOptionsPanel();
        myPanel.init();
        return myPanel.myWholePanel;
    }

    @Override
    public boolean isModified() {
        return !myManager.getState().equals(myPanel.toState());
    }

    @Override
    public void apply() {
        myManager.loadState(myPanel.toState());
    }

    @Override
    public void reset() {
        myPanel.fromState(myManager.getState());
    }

    @Override
    public void disposeUIResources() {
        myPanel = null;
    }

    public class CopyImageOptionsPanel {

        private static final double SLIDER_SCALE = 2;

        private JTextField myScale;
        private JCheckBox myChopIndentation;
        private JCheckBox myRemoveCaret;
        private JPanel myWholePanel;
        private JSlider mySlider;
        private JPanel mySaveDirectoryPanel;
        private JTextField myPadding;
        private JCheckBox myEnableGutter;
        private JCheckBox myShowLineNumber;
        private JCheckBox myShowGutterIcon;
        private JCheckBox myShowFoldOutline;
        private JCheckBox myShowIndentGuide;
        private JCheckBox myShowInnerWhitespace;
        private TextFieldWithHistoryWithBrowseButton mySaveDirectory;

        CopyImageOptionsProvider.State toState() {
            CopyImageOptionsProvider.State state = new CopyImageOptionsProvider.State();
            state.myChopIndentation = myChopIndentation.isSelected();
            state.myRemoveCaret = myRemoveCaret.isSelected();
            try {
                state.myScale = Double.parseDouble(myScale.getText().trim());
            } catch (NumberFormatException ignored) {
            }

            state.myDirectoryToSave = StringUtil.nullize(mySaveDirectory.getText());
            try {
                state.myPadding = Integer.parseInt(myPadding.getText());
            } catch (NumberFormatException ignored) {
            }

            state.myShowIndentGuide = myShowIndentGuide.isSelected();
            state.myShowInnerWhitespace = myShowInnerWhitespace.isSelected();

            state.myEnableGutter = myEnableGutter.isSelected();
            state.myShowLineNumber = myShowLineNumber.isSelected();
            state.myShowGutterIcon = myShowGutterIcon.isSelected();
            state.myShowFoldOutline = myShowFoldOutline.isSelected();
            return state;
        }

        void fromState(CopyImageOptionsProvider.State state) {
            myChopIndentation.setSelected(state.myChopIndentation);
            myRemoveCaret.setSelected(state.myRemoveCaret);
            mySlider.setValue((int) (state.myScale * SLIDER_SCALE));
            mySaveDirectory.setText(StringUtil.notNullize(state.myDirectoryToSave));
            myPadding.setText(String.valueOf(state.myPadding));

            myShowIndentGuide.setSelected(state.myShowIndentGuide);
            myShowInnerWhitespace.setSelected(state.myShowInnerWhitespace);

            boolean enableGutter = state.myEnableGutter;
            myEnableGutter.setSelected(enableGutter);
            myShowLineNumber.setSelected(state.myShowLineNumber);
            myShowGutterIcon.setSelected(state.myShowGutterIcon);
            myShowFoldOutline.setSelected(state.myShowFoldOutline);
            myShowLineNumber.setEnabled(enableGutter);
            myShowGutterIcon.setEnabled(enableGutter);
            myShowFoldOutline.setEnabled(enableGutter);
        }

        void init() {
            mySlider.addChangeListener(e -> myScale.setText(String.valueOf(mySlider.getValue() / SLIDER_SCALE)));
            myEnableGutter.addActionListener(e -> {
                boolean enableGutter = myEnableGutter.isSelected();
                myShowLineNumber.setEnabled(enableGutter);
                myShowGutterIcon.setEnabled(enableGutter);
                myShowFoldOutline.setEnabled(enableGutter);
            });
        }

        private void createUIComponents() {
            FileChooserDescriptor singleFolderDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
            TextFieldWithHistoryWithBrowseButton field = SwingHelper.createTextFieldWithHistoryWithBrowseButton(
                    myProject,
                    "Save to directory",
                    singleFolderDescriptor,
                    ContainerUtil::emptyList);
            mySaveDirectoryPanel = field;
            mySaveDirectory = field;
        }
    }
}
