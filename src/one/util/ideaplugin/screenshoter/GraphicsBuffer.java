package one.util.ideaplugin.screenshoter;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.ext.awt.g2d.GraphicContext;
import org.apache.batik.svggen.DOMGroupManager;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

class GraphicsBuffer extends SVGGraphics2D {

    static final List<Consumer<Graphics2D>> BLACK_HOLE_LIST = new AbstractList<>() {
        @Override public Consumer<Graphics2D> get(int index) { throw new UnsupportedOperationException(); }
        @Override public int size() { throw new UnsupportedOperationException(); }
        @Override public boolean add(Consumer<Graphics2D> graphics2DConsumer) { return true; }
    };

    private List<Consumer<Graphics2D>> ops;

    GraphicsBuffer(List<Consumer<Graphics2D>> ops, int width, int height) {
        super(newCtx(), true);
        this.ops = ops;
        setSVGCanvasSize(new Dimension(width, height));
    }

    @NotNull
    private static SVGGeneratorContext newCtx() {
        SVGGeneratorContext ctx = SVGGeneratorContext.createDefault(
            GenericDOMImplementation.getDOMImplementation()
                .createDocument("http://www.w3.org/2000/svg", "svg", null));
        ctx.setEmbeddedFontsOn(true);
        ctx.setComment(null);
        return ctx;
    }

    // AbstractGraphics2D

    @Override public void translate(int x, int y) {
        ops.add(g -> g.translate(x, y));
        super.translate(x, y);
    }

    @Override public void setColor(Color c) {
        ops.add(g -> g.setColor(c));
        super.setColor(c);
    }

    @Override public void setFont(Font font) {
        ops.add(g -> g.setFont(font));
        super.setFont(font);
    }

    @Override public void clipRect(int x, int y, int width, int height) {
        ops.add(g -> g.clipRect(x, y, width, height));
        super.clipRect(x, y, width, height);
    }

    @Override public void setClip(int x, int y, int width, int height) {
        ops.add(g -> g.setClip(x, y, width, height));
        super.setClip(x, y, width, height);
    }

    @Override public void setClip(Shape clip) {
        Shape copy = copyShape(clip);
        ops.add(g -> g.setClip(copy));
        super.setClip(clip);
    }

    @Override public void clearRect(int x, int y, int width, int height) {
        ops.add(g -> clearRect(x, y, width, height));
        super.clearRect(x, y, width, height);
    }

    @Override public void setComposite(Composite comp) {
        ops.add(g -> g.setComposite(comp));
        super.setComposite(comp);
    }

    @Override public void setPaint(Paint paint) {
        ops.add(g -> g.setPaint(paint));
        super.setPaint(paint);
    }

    @Override public void setStroke(Stroke s) {
        ops.add(g -> g.setStroke(s));
        super.setStroke(s);
    }

    @Override public void setRenderingHint(RenderingHints.Key hintKey, Object hintValue) {
        ops.add(g -> g.setRenderingHint(hintKey, hintValue));
        super.setRenderingHint(hintKey, hintValue);
    }

    @Override public void setRenderingHints(Map hints) {
        //noinspection unchecked,rawtypes
        Map<?, ?> copy = new LinkedHashMap(hints);
        ops.add(g -> g.setRenderingHints(copy));
        super.setRenderingHints(hints);
    }

    @Override public void addRenderingHints(Map hints) {
        //noinspection unchecked,rawtypes
        Map<?, ?> copy = new LinkedHashMap(hints);
        ops.add(g -> g.addRenderingHints(copy));
        super.addRenderingHints(hints);
    }

    @Override public void translate(double tx, double ty) {
        ops.add(g -> g.translate(tx, ty));
        super.translate(tx, ty);
    }

    @Override public void rotate(double theta) {
        ops.add(g -> g.rotate(theta));
        super.rotate(theta);
    }

    @Override public void rotate(double theta, double x, double y) {
        ops.add(g -> g.rotate(theta, x, y));
        super.rotate(theta, x, y);
    }

    @Override public void scale(double sx, double sy) {
        ops.add(g -> g.scale(sx, sy));
        super.scale(sx, sy);
    }

    @Override public void shear(double shx, double shy) {
        ops.add(g -> g.shear(shx, shy));
        super.shear(shx, shy);
    }

    @Override public void transform(AffineTransform Tx) {
        AffineTransform copy = Tx == null ? null : new AffineTransform(Tx);
        ops.add(g -> g.transform(copy));
        super.transform(Tx);
    }

    @Override public void setTransform(AffineTransform Tx) {
        AffineTransform copy = Tx == null ? null : new AffineTransform(Tx);
        ops.add(g -> g.setTransform(copy));
        super.setTransform(Tx);
    }

    @Override public void setBackground(Color color) {
        ops.add(g -> g.setBackground(color));
        super.setBackground(color);
    }

    @Override public void clip(Shape s) {
        Shape copy = copyShape(s);
        ops.add(g -> g.clip(copy));
        super.clip(s);
    }

    // SVGGraphics2D

    @Override public Graphics create() {
        try {
            GraphicsBuffer copy = (GraphicsBuffer) clone();
            copy.gc = (GraphicContext) gc.clone();
            copy.domGroupManager = new DOMGroupManager(copy.gc, copy.domTreeManager);
            copy.domTreeManager.addGroupManager(copy.domGroupManager);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
        ops.add(g -> g.drawImage(img, x, y, observer));
        return super.drawImage(img, x, y, observer);
    }

    @Override public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
        ops.add(g -> g.drawImage(img, x, y, width, height, observer));
        return super.drawImage(img, x, y, width, height, observer);
    }

    @Override public void dispose() {
        ops.add(Graphics::dispose);
        ops = Collections.unmodifiableList(ops);
    }

    @Override public void draw(Shape s) {
        if (gc.getStroke() instanceof BasicStroke) {
            Shape copy = copyShape(s);
            ops.add(g -> g.draw(copy));
        } // else super.draw(s) will call fill(stroke.createStrokedShape(s))
        super.draw(s);
    }

    @Override public void drawRenderedImage(RenderedImage img, AffineTransform trans2) {
        AffineTransform copy = trans2 == null ? null : new AffineTransform(trans2);
        ops.add(g -> g.drawRenderedImage(img, copy));
        super.drawRenderedImage(img, trans2);
    }

    @Override public void drawRenderableImage(RenderableImage img, AffineTransform trans2) {
        AffineTransform copy = trans2 == null ? null : new AffineTransform(trans2);
        ops.add(g -> g.drawRenderableImage(img, copy));
        super.drawRenderableImage(img, trans2);
    }

    @Override public void fill(Shape s) {
        Shape copy = copyShape(s);
        ops.add(g -> g.fill(copy));
        super.fill(s);
    }

    // our own public interface

    public void draw(Graphics2D into) {
        for (Consumer<Graphics2D> op : ops) {
            op.accept(into);
        }
    }

    public void clear() {
        setGeneratorContext(newCtx());
        ops = new ArrayList<>();
    }

    // private util

    private static Function<Shape, Shape> copyShape;
    private static Shape copyShape(Shape original) {
        Function<Shape, Shape> f = copyShape;
        if (f != null) return f.apply(original);
        Function<Shape, Shape> fallback = Path2D.Double::new;
        try {
            Method m = Object.class.getDeclaredMethod("clone");
            m.setAccessible(true);
            f = shape -> {
                try {
                    return (Shape) m.invoke(shape);
                } catch (ReflectiveOperationException e) {
                    return fallback.apply(shape);
                }
            };
        } catch (ReflectiveOperationException e) {
            f = fallback;
        }
        return (copyShape = f).apply(original);
    }

}
