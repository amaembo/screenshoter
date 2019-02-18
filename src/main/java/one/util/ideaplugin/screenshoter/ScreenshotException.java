package one.util.ideaplugin.screenshoter;

/**
 * @author 九条涼果 s1131234@gmail.com
 */
public class ScreenshotException extends RuntimeException {

    private static final long serialVersionUID = -7246186236182563641L;

    /** whether this error will not showing to user */
    boolean hide;
}
