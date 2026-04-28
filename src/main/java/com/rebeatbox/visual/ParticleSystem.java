package com.rebeatbox.visual;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.swing.JComponent;
import javax.swing.Timer;

/**
 * Cyber-block particle burst system rendered on a transparent overlay.
 *
 * <p>Renders up to 200 square particles with random rotation and neon glow
 * borders. Particles burst from a configurable emit origin on every note
 * event, with lifetime and size proportional to MIDI velocity.
 *
 * <p>Uses off-screen accumulation — all particles are drawn to a
 * BufferedImage, then a single blit to the screen each frame. The component
 * is mouse-transparent (contains() always returns false) so events pass
 * through to underlying UI.
 *
 * <h3>Design Decisions</h3>
 * <ul>
 *   <li>D-08: Cyber-block style — squares with glow borders, no circles</li>
 *   <li>D-09: GlassPane overlay, transparent to mouse events</li>
 *   <li>D-10: Particle color from NoteColorMapper.forPitch(note)</li>
 *   <li>D-12: 200-particle cap; oldest 2 merge when full</li>
 *   <li>D-13: Lifetime 300ms (vel=0) to 800ms (vel=127)</li>
 *   <li>D-14: Size 1px (vel=0) to 8px (vel=127)</li>
 *   <li>D-15: Independent javax.swing.Timer(16ms)</li>
 * </ul>
 */
public final class ParticleSystem extends JComponent {

    /** Maximum number of particles before merge-oldest kicks in (D-12). */
    static final int MAX_PARTICLES = 200;

    /** Timer interval in ms — ~60fps (D-15). */
    private static final int TIMER_INTERVAL_MS = 16;

    /** Minimum particle speed in px/s. */
    private static final float MIN_SPEED = 80f;

    /** Maximum particle speed in px/s. */
    private static final float MAX_SPEED = 200f;

    /** Minimum rotation speed in degrees/s (converted to rad/s at creation). */
    private static final float MIN_ROTATION_SPEED = 90f;

    /** Maximum rotation speed in degrees/s (converted to rad/s at creation). */
    private static final float MAX_ROTATION_SPEED = 360f;

    /** Pre-calculated conversion factor: degrees to radians. */
    private static final float DEG_TO_RAD = (float) Math.PI / 180f;

    /** Fixed timestep in seconds for physics integration. */
    private static final float FIXED_DT = TIMER_INTERVAL_MS / 1000f;

    /** Maximum merged particle size in px. */
    private static final float MAX_MERGED_SIZE = 12f;

    /** Random position offset range around emit origin in px. */
    private static final float EMIT_SPREAD = 10f;

    // ------------------------------------------------------------------
    // Inner class: Particle
    // ------------------------------------------------------------------

    /**
     * A single cyber-block particle with position, velocity, size, color,
     * lifetime, and rotation state.
     */
    private static final class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        final Color color;
        final long spawnNanos;
        final long lifetimeNanos;
        float rotation;
        final float rotationSpeed;

        Particle(float x, float y, float vx, float vy, float size, Color color,
                 long spawnNanos, long lifetimeNanos, float rotationSpeed) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
            this.color = color;
            this.spawnNanos = spawnNanos;
            this.lifetimeNanos = lifetimeNanos;
            this.rotation = 0f;
            this.rotationSpeed = rotationSpeed;
        }

        boolean isDead(long nowNanos) {
            return (nowNanos - spawnNanos) >= lifetimeNanos;
        }

        /**
         * Linear alpha decay: 1.0 at spawn, 0.0 at death.
         */
        float alpha(long nowNanos) {
            long elapsed = nowNanos - spawnNanos;
            if (elapsed >= lifetimeNanos) return 0f;
            return 1.0f - (float) elapsed / (float) lifetimeNanos;
        }

        /**
         * Remaining lifetime in nanoseconds at the given point in time.
         */
        long remainingNanos(long nowNanos) {
            long elapsed = nowNanos - spawnNanos;
            if (elapsed >= lifetimeNanos) return 0L;
            return lifetimeNanos - elapsed;
        }
    }

    // ------------------------------------------------------------------
    // Fields
    // ------------------------------------------------------------------

    private final List<Particle> particles = new ArrayList<>(MAX_PARTICLES);
    private BufferedImage offscreen;
    private final Timer timer;
    private final Random random = new Random();
    private float emitOriginX;
    private float emitOriginY;

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    public ParticleSystem() {
        setOpaque(false);
        timer = new Timer(TIMER_INTERVAL_MS, e -> updateAndRepaint());
        timer.setInitialDelay(0);
    }

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /** Starts the animation timer. Idempotent — safe to call when already running. */
    public void start() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    /** Stops the animation timer. Idempotent — safe to call when already stopped. */
    public void stop() {
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    /**
     * Emits a particle burst for the given MIDI note and velocity.
     *
     * <p>Color is derived from {@link NoteColorMapper#forPitch(int)}.
     * Lifetime and size are linearly interpolated from velocity.
     * When the particle cap is reached, the two oldest particles
     * merge into one larger particle.
     *
     * @param note     MIDI note number (0-127)
     * @param velocity MIDI velocity (0-127), controls lifetime and size
     */
    public void emit(int note, int velocity) {
        Color color = NoteColorMapper.forPitch(note);

        // D-13: lifetime = 300ms (vel=0) to 800ms (vel=127)
        int lifetimeMs = 300 + (velocity * 500 / 127);
        long lifetimeNanos = (long) lifetimeMs * 1_000_000L;

        // D-14: size = 1px (vel=0) to 8px (vel=127)
        int size = 1 + (velocity * 7 / 127);

        // D-12: cap enforcement via merge
        if (particles.size() >= MAX_PARTICLES) {
            mergeOldestParticles();
        }

        // Random spawn position around emit origin with spread
        float spawnX = emitOriginX + (random.nextFloat() * 2f - 1f) * EMIT_SPREAD;
        float spawnY = emitOriginY + (random.nextFloat() * 2f - 1f) * EMIT_SPREAD;

        // Random direction (uniform 0-360 degrees)
        float angle = random.nextFloat() * 2f * (float) Math.PI;

        // Random speed in [MIN_SPEED, MAX_SPEED]
        float speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED);

        float vx = speed * (float) Math.cos(angle);
        float vy = speed * (float) Math.sin(angle);

        // Random rotation speed in [MIN_ROTATION_SPEED, MAX_ROTATION_SPEED] deg/s -> rad/s
        float rotDegPerSec = MIN_ROTATION_SPEED
                + random.nextFloat() * (MAX_ROTATION_SPEED - MIN_ROTATION_SPEED);
        float rotRadPerSec = rotDegPerSec * DEG_TO_RAD;

        Particle p = new Particle(spawnX, spawnY, vx, vy, size, color,
                System.nanoTime(), lifetimeNanos, rotRadPerSec);
        particles.add(p);

        // Auto-start timer if not running (first emit starts animation)
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    /**
     * Sets the origin point from which particles emit.
     * Particles spawn at this position with a +/-10px random offset.
     *
     * @param x X coordinate in component space
     * @param y Y coordinate in component space
     */
    public void setEmitOrigin(float x, float y) {
        this.emitOriginX = x;
        this.emitOriginY = y;
    }

    // ------------------------------------------------------------------
    // Mouse transparency (D-09)
    // ------------------------------------------------------------------

    @Override
    public boolean contains(int x, int y) {
        return false; // All mouse events pass through to underlying components
    }

    // ------------------------------------------------------------------
    // Package-private accessors for testing
    // ------------------------------------------------------------------

    int getParticleCount() {
        return particles.size();
    }

    float getParticleSize(int index) {
        return particles.get(index).size;
    }

    Color getParticleColor(int index) {
        return particles.get(index).color;
    }

    // ------------------------------------------------------------------
    // Animation loop
    // ------------------------------------------------------------------

    private void updateAndRepaint() {
        long now = System.nanoTime();

        // Remove dead particles
        particles.removeIf(p -> p.isDead(now));

        // Update positions and rotations with fixed timestep
        for (Particle p : particles) {
            p.x += p.vx * FIXED_DT;
            p.y += p.vy * FIXED_DT;
            p.rotation += p.rotationSpeed * FIXED_DT;
        }

        repaint();
    }

    // ------------------------------------------------------------------
    // Merge algorithm (D-12)
    // ------------------------------------------------------------------

    /**
     * Merges the two oldest particles (by remaining lifetime) into one
     * larger particle. Called automatically when the particle cap is hit.
     *
     * <p>Merge properties:
     * <ul>
     *   <li>Position: arithmetic mean of the two particles</li>
     *   <li>Size: sum of sizes, capped at 12px</li>
     *   <li>Color: color of the particle with largest remaining lifetime</li>
     *   <li>Lifetime: average of remaining lifetimes</li>
     *   <li>Velocity: vector average, magnitude = (|v1| + |v2|) / 2</li>
     * </ul>
     */
    private void mergeOldestParticles() {
        if (particles.size() < 2) return;

        long now = System.nanoTime();

        // Find 2 oldest by remaining lifetime (ascending — smallest = oldest)
        int oldestIdx = 0;
        int secondOldestIdx = 1;
        long oldestRemaining = particles.get(0).remainingNanos(now);
        long secondRemaining = particles.get(1).remainingNanos(now);

        if (secondRemaining < oldestRemaining) {
            int tmpIdx = oldestIdx;
            oldestIdx = secondOldestIdx;
            secondOldestIdx = tmpIdx;
            long tmpRemaining = oldestRemaining;
            oldestRemaining = secondRemaining;
            secondRemaining = tmpRemaining;
        }

        for (int i = 2; i < particles.size(); i++) {
            long remaining = particles.get(i).remainingNanos(now);
            if (remaining < oldestRemaining) {
                secondOldestIdx = oldestIdx;
                secondRemaining = oldestRemaining;
                oldestIdx = i;
                oldestRemaining = remaining;
            } else if (remaining < secondRemaining) {
                secondOldestIdx = i;
                secondRemaining = remaining;
            }
        }

        // Remove in reverse order to preserve indices
        Particle p1 = (oldestIdx > secondOldestIdx)
                ? particles.remove(oldestIdx)
                : particles.remove(secondOldestIdx);
        Particle p2 = (oldestIdx < secondOldestIdx)
                ? particles.remove(oldestIdx)
                : particles.remove(secondOldestIdx - (oldestIdx > secondOldestIdx ? 0 : 1));

        // Merged position: arithmetic mean
        float mergedX = (p1.x + p2.x) / 2f;
        float mergedY = (p1.y + p2.y) / 2f;

        // Merged size: sum, capped at MAX_MERGED_SIZE
        float mergedSize = Math.min(p1.size + p2.size, MAX_MERGED_SIZE);

        // Merged color: particle with largest remaining lifetime
        Color mergedColor = (oldestRemaining <= secondRemaining) ? p2.color : p1.color;

        // Recompute remaining at merge time for the two selected particles
        long p1Remaining = p1.remainingNanos(now);
        long p2Remaining = p2.remainingNanos(now);
        long mergedRemaining = (p1Remaining + p2Remaining) / 2L;

        // Merged velocity: vector average, magnitude = (|v1| + |v2|) / 2
        float avgVx = (p1.vx + p2.vx) / 2f;
        float avgVy = (p1.vy + p2.vy) / 2f;
        float avgMag = (float) Math.sqrt(avgVx * avgVx + avgVy * avgVy);

        float targetMag = ((float) Math.sqrt(p1.vx * p1.vx + p1.vy * p1.vy)
                + (float) Math.sqrt(p2.vx * p2.vx + p2.vy * p2.vy)) / 2f;

        float mergedVx, mergedVy;
        if (avgMag > 0.0001f) {
            float scale = targetMag / avgMag;
            mergedVx = avgVx * scale;
            mergedVy = avgVy * scale;
        } else {
            mergedVx = avgVx;
            mergedVy = avgVy;
        }

        // Merged rotation: average of current rotations
        float mergedRotationSpeed = (p1.rotationSpeed + p2.rotationSpeed) / 2f;

        Particle merged = new Particle(mergedX, mergedY, mergedVx, mergedVy,
                mergedSize, mergedColor, now, mergedRemaining, mergedRotationSpeed);

        // Carry forward accumulated rotation
        merged.rotation = (p1.rotation + p2.rotation) / 2f;

        particles.add(merged);
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        // Create or recreate offscreen buffer if size changed
        if (offscreen == null || offscreen.getWidth() != w || offscreen.getHeight() != h) {
            offscreen = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
        }

        Graphics2D og = offscreen.createGraphics();
        try {
            // Clear with Src composite (fully transparent)
            og.setComposite(AlphaComposite.Src);
            og.setColor(new Color(0, 0, 0, 0));
            og.fillRect(0, 0, w, h);
            og.setComposite(AlphaComposite.SrcOver);

            // Enable antialiasing for rotated squares
            og.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            long now = System.nanoTime();

            for (Particle p : particles) {
                float alpha = p.alpha(now);
                if (alpha <= 0f) continue;

                float halfSize = p.size / 2f;

                // Save transform state
                AffineTransform saved = og.getTransform();

                // Translate to particle center, rotate
                og.translate(p.x, p.y);
                og.rotate(p.rotation);

                // Draw cyber-block body
                og.setComposite(AlphaComposite.SrcOver.derive(alpha));
                og.setColor(p.color);
                og.fillRect((int) (-halfSize), (int) (-halfSize),
                        (int) p.size, (int) p.size);

                // Draw glow border — slightly brighter color
                int glowR = Math.min(255, p.color.getRed() + 40);
                int glowG = Math.min(255, p.color.getGreen() + 40);
                int glowB = Math.min(255, p.color.getBlue() + 40);
                int glowA = Math.min(255, Math.max(0, (int) (alpha * 255)));
                og.setColor(new Color(glowR, glowG, glowB, glowA));
                og.setStroke(new BasicStroke(1.0f));
                og.drawRect((int) (-halfSize), (int) (-halfSize),
                        (int) p.size - 1, (int) p.size - 1);

                // Restore transform
                og.setTransform(saved);
            }
        } finally {
            og.dispose();
        }

        // Single blit to screen
        Graphics2D g2d = (Graphics2D) g;
        g2d.drawImage(offscreen, 0, 0, null);
    }
}
