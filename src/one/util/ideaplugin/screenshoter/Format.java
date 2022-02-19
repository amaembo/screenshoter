package one.util.ideaplugin.screenshoter;

import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public enum Format {
    PNG {
        @Override
        void write(GraphicsBuffer src, OutputStream dest) throws IOException {
            ImageIO.write(rasterize(src), "png", dest);
        }
    },
    SVG {
        @Override
        void write(GraphicsBuffer src, OutputStream dest) throws IOException {
            src.stream(new OutputStreamWriter(dest), true);
        }
    };

    @NotNull static BufferedImage rasterize(GraphicsBuffer src) {
        Dimension size = src.getSVGCanvasSize();
        //noinspection UndesirableClassUsage
        BufferedImage img = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        src.draw(img.createGraphics());
        return img;
    }

    abstract void write(GraphicsBuffer src, OutputStream dest) throws IOException;
}
