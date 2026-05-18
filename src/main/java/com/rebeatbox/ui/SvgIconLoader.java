package com.rebeatbox.ui;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.util.SVGConstants;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * Loads SVG icons from classpath resources and caches them as {@link BufferedImage}
 * instances keyed by icon name and render size.
 *
 * <p>Uses Apache Batik 1.19 {@link ImageTranscoder} for SVG rasterization (D-24).
 * Icons are loaded at startup via {@link #preload()} and retrieved via
 * {@link #loadSvg(String, int, int)} or the convenience {@link #getIcon(String, int)}.</p>
 */
public final class SvgIconLoader {

    /**
     * Package-private Batik ImageTranscoder that captures the rasterized image
     * into a BufferedImage accessible via {@link #getBufferedImage()}.
     */
    static class BufferedImageTranscoder extends ImageTranscoder {
        private BufferedImage img;

        @Override
        public BufferedImage createImage(int w, int h) {
            return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }

        @Override
        public void writeImage(BufferedImage img, TranscoderOutput output) {
            this.img = img;
        }

        public BufferedImage getBufferedImage() {
            return img;
        }
    }

    /**
     * Two-level cache: icon name -> (render width -> BufferedImage).
     * Same name+size returns the same instance (identity equality).
     */
    private static final Map<String, Map<Integer, BufferedImage>> cache = new HashMap<>();

    /**
     * Loads an SVG icon from classpath and returns it rendered at the requested dimensions.
     * Results are cached — subsequent calls with the same name and width return the
     * same BufferedImage instance.
     *
     * @param name   icon file name without extension (e.g. "play", "pause")
     * @param width  render width in pixels
     * @param height render height in pixels
     * @return rasterized BufferedImage
     * @throws IllegalArgumentException if the SVG resource is not found on classpath
     * @throws TranscoderException      if Batik fails to rasterize the SVG
     */
    public static BufferedImage loadSvg(String name, int width, int height)
            throws TranscoderException {

        // 1. Check cache
        BufferedImage cached = cache.getOrDefault(name, Collections.emptyMap()).get(width);
        if (cached != null) {
            return cached;
        }

        // 2. Build resource path
        String iconPath = "/icons/" + name + ".svg";

        // 3. Open input stream
        InputStream svgStream = SvgIconLoader.class.getResourceAsStream(iconPath);

        // 4. Validate stream
        if (svgStream == null) {
            throw new IllegalArgumentException("SVG icon not found: " + iconPath);
        }

        // 5. Create transcoder with hints
        BufferedImageTranscoder transcoder = new BufferedImageTranscoder();
        transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, (float) width);
        transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, (float) height);
        transcoder.addTranscodingHint(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
        transcoder.addTranscodingHint(ImageTranscoder.KEY_DOM_IMPLEMENTATION,
                SVGDOMImplementation.getDOMImplementation());
        transcoder.addTranscodingHint(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI,
                SVGConstants.SVG_NAMESPACE_URI);
        transcoder.addTranscodingHint(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");

        // 6. Transcode
        transcoder.transcode(new TranscoderInput(svgStream), null);

        // 7. Get result
        BufferedImage result = transcoder.getBufferedImage();

        // 8. Cache by name + width
        cache.computeIfAbsent(name, k -> new HashMap<>()).put(width, result);

        return result;
    }

    /**
     * Convenience method for square icons. Delegates to {@link #loadSvg(String, int, int)}.
     *
     * @param name icon file name without extension
     * @param size render size in pixels (square, so width == height)
     * @return rasterized BufferedImage
     */
    public static BufferedImage getIcon(String name, int size) throws TranscoderException {
        return loadSvg(name, size, size);
    }

    /**
     * Pre-loads all 12 application icons at their fixed sizes.
     * Errors are logged to {@code System.err} rather than thrown, so the
     * application can still start even if some icons fail to load.
     */
    public static void preload() {
        preloadIcon("play", 38, 38);
        preloadIcon("pause", 38, 38);
        preloadIcon("stop", 38, 38);
        preloadIcon("restart", 38, 38);
        preloadIcon("open-file", 38, 38);
        preloadIcon("collapse-left", 24, 24);
        preloadIcon("expand-right", 24, 24);
        preloadIcon("app-icon", 64, 64);
        preloadIcon("bpm", 16, 16);
        preloadIcon("volume", 16, 16);
        preloadIcon("keyboard-mode", 24, 24);
        preloadIcon("drum-mode", 24, 24);
    }

    private static void preloadIcon(String name, int width, int height) {
        try {
            loadSvg(name, width, height);
        } catch (TranscoderException | IllegalArgumentException e) {
            System.err.println("SvgIconLoader: failed to preload icon '" + name + "': " + e.getMessage());
        }
    }

    /** Private constructor — static utility class, no instantiation. */
    private SvgIconLoader() {}
}
