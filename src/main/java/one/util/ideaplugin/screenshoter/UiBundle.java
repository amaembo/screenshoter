package one.util.ideaplugin.screenshoter;

import com.intellij.BundleBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

/**
 * @author 九条涼果 s1131234@gmail.com
 */
class UiBundle {

    private static final String BUNDLE_NAME = "message";

    private static Reference<ResourceBundle> ourBundle;

    static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params) {
        return BundleBase.message(getBundle(), key, params);
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(ourBundle);
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME);
            ourBundle = new SoftReference<>(bundle);
        }
        return bundle;
    }
}
