package one.util.ideaplugin.screenshoter;

import com.intellij.util.ui.UIUtil;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public abstract class TransferableImage<T> implements Transferable {

    public enum Format {
        PNG("png", DataFlavor.imageFlavor) {
            TransferableImage<?> paint(JComponent contentComponent, AffineTransform at,
                                        int width, int height, Color backgroundColor, int padding) {
                BufferedImage img = UIUtil.createImage(
                    contentComponent, width + 2 * padding, height + 2 * padding, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = (Graphics2D) img.getGraphics();
                paint(g, contentComponent, at, width, height, backgroundColor, padding);
                return new Png(img);
            }
        },
        SVG("svg", svgDataFlavor(), DataFlavor.stringFlavor, DataFlavor.plainTextFlavor) {
            TransferableImage<?> paint(JComponent contentComponent, AffineTransform at,
                                       int width, int height, Color backgroundColor, int padding) throws IOException {
                DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
                Document doc = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
                SVGGraphics2D g = new SVGGraphics2D(doc);
                g.setSVGCanvasSize(new Dimension(width + 2 * padding, height + 2 * padding));
                paint(g, contentComponent, at, width, height, backgroundColor, padding);
                CharArrayWriter writer = new CharArrayWriter();
                g.stream(writer, true);
                return new Svg(writer.toString());
            }
        },
        ;

        @NotNull
        private static DataFlavor svgDataFlavor() {
            return new DataFlavor(String.class, "image/svg+xml; charset=utf-8");
        }

        static void paint(Graphics2D g,
                          JComponent contentComponent, AffineTransform at,
                          int width, int height, Color backgroundColor, int padding) {
            int imgWidth = width + 2 * padding;
            int imgHeight = height + 2 * padding;
            g.setColor(backgroundColor);
            g.fillRect(0, 0, imgWidth, imgHeight);
            g.translate(padding, padding);
            g.clipRect(0, 0, width, height);
            g.transform(at);
            contentComponent.paint(g);
        }

        final String ext;
        final DataFlavor[] flavors;
        Format(String ext, DataFlavor... flavors) {
            this.ext = ext;
            this.flavors = flavors;
        }

        abstract TransferableImage<?> paint(JComponent contentComponent, AffineTransform at,
                                            int width, int height, Color backgroundColor, int padding) throws IOException;
    }

    @NotNull final Format format;
    @NotNull final T transferee;

    TransferableImage(@NotNull Format format, @NotNull T transferee) {
        this.format = format;
        this.transferee = transferee;
    }

    @NotNull
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (isDataFlavorSupported(flavor)) {
            return transferee;
        }
        throw new UnsupportedFlavorException(flavor);
    }

    public DataFlavor[] getTransferDataFlavors() {
        return format.flavors.clone();
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return Arrays.stream(format.flavors).anyMatch(flavor::equals);
    }

    abstract void write(OutputStream to) throws IOException;

    static class Png extends TransferableImage<BufferedImage> {
        Png(BufferedImage image) {
            super(Format.PNG, image);
        }

        @Override
        void write(OutputStream to) throws IOException {
            ImageIO.write(transferee, "png", to);
        }
    }

    static class Svg extends TransferableImage<String> {
        Svg(String image) {
            super(Format.SVG, image);
        }

        @NotNull
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(DataFlavor.plainTextFlavor)) {
                return new StringReader(transferee);
            } else {
                return super.getTransferData(flavor);
            }
        }

        @Override
        void write(OutputStream to) throws IOException {
            try (OutputStreamWriter w = new OutputStreamWriter(to, StandardCharsets.UTF_8)) {
                w.write(transferee);
            }
        }
    }

}
