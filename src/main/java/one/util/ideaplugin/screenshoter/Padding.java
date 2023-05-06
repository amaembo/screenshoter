package one.util.ideaplugin.screenshoter;

public class Padding {
    private final int height;
    private final int width;

    public Padding(int height, int width) {
        this.height = height;
        this.width = width;
    }

    public static final Padding EMPTY = new Padding(0, 0);

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

}
