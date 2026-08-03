package dev.starfall.rig;

import java.util.Random;

/**
 * Everything that makes one face this face — STYLE.md 4b.6's variety axis.
 *
 * <p>The hero and bosses are <b>authored</b>: their parameters are constants in
 * this file, chosen against family D. Ordinary Charted Shadows come from
 * {@link #generate(long)}, a seeded generator over the range family D actually
 * spans — a young woman, an old woman, a bearded man, a middle-aged man — so
 * every enemy face is different and every one is deterministic for a given seed,
 * which is what lets a capture be reproduced.
 *
 * <p>4b.6's own warning governs the age axis: <i>"an old face is not a young
 * face with lines: it is a different silhouette — sunken eye socket, softened
 * jaw, different hair mass."</i> So {@code age} here moves <em>geometry</em>
 * (socket depth, jaw radius, brow height), never a wrinkle texture.
 *
 * <p>All values 0..1 unless noted. Marks are placed by the mesh author from
 * {@code seed}, asymmetrically — 4b.7 fails symmetric marks on sight.
 */
public record FaceParams(
        /* 0 young .. 1 old: sinks the socket, softens the jaw, thins the brow height */
        float age,
        /* heaviness of the single brow stroke, the primary expression carrier */
        float browWeight,
        /* 0 soft/receding .. 1 square/jutting chin and jaw */
        float jawShape,
        /* scale of the bridge notch and nose jut on the profile contour */
        float noseDepth,
        /* size of the one visible eye */
        float eyeSize,
        /* 0 none .. 1 full beard mass under the jaw (beard is hair: it may fray) */
        float facialHair,
        /* how loud the skin's ochre staining is */
        float stainAmount,
        /* < 0 for no scar; otherwise where on the cheek/brow the one scar sits, 0..1 */
        float scar,
        /* placement noise for marks and asymmetries */
        long seed) {

    /**
     * The Night Pilgrim. Authored, not generated: young, focused, a heavy brow
     * over a deep-set eye, unscarred, skin quiet — the face family D's first
     * portrait would give the dark duellist.
     */
    public static FaceParams hero() {
        // facialHair 0.32 as of System 3b pass 2: reference image 3's dark
        // duellist — the figure this face is authored against — carries a
        // moustache and a chin beard, and the pass-1 review's matched-scale
        // part count lists "lip + moustache" and "chin + beard" as two of the
        // corpus head's 27 readable parts. A clean-shaven hero was an
        // authoring choice the corpus never made.
        return new FaceParams(0.22f, 0.72f, 0.55f, 0.62f, 0.52f, 0.32f, 0.18f, -1f, 0x51A2F00DL);
    }

    /**
     * A seeded Charted Shadow face. Same seed, same face, forever — captures are
     * reproducible and a returning enemy keeps the face it had.
     */
    public static FaceParams generate(long seed) {
        Random r = new Random(seed * 0x9E3779B97F4A7C15L + 0xB5297A4D3F84D5B9L);
        float age = r.nextFloat();
        return new FaceParams(
                age,
                0.30f + 0.65f * r.nextFloat(),
                0.15f + 0.75f * r.nextFloat(),
                0.35f + 0.60f * r.nextFloat(),
                0.35f + 0.45f * r.nextFloat() * (1f - 0.35f * age), // old eyes narrow
                r.nextFloat() < 0.45f ? 0.35f + 0.65f * r.nextFloat() : 0f,
                0.10f + 0.55f * r.nextFloat(),
                r.nextFloat() < 0.30f ? r.nextFloat() : -1f,
                r.nextLong());
    }

    /** Sunken-socket depth in world units, driven by age. */
    float socketDepth() {
        return 0.010f + 0.026f * age;
    }

    /** Jaw wedge softening: an old jaw recedes and rounds. */
    float jawScale() {
        return (0.85f + 0.30f * jawShape) * (1f - 0.22f * age);
    }
}
