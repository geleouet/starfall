package dev.starfall.rig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STYLE.md 4b.6's variety axis: "ordinary enemies come from a seeded generator...
 * Family D covers a young woman, an old woman, a bearded man and a middle-aged
 * man, which is the range the generator must span."
 */
class FaceGenTest {

    /** A capture is reproducible only if a body's face is a pure function of its seed. */
    @Test
    void theSameSeedIsTheSameFace() {
        assertEquals(FaceParams.generate(42L), FaceParams.generate(42L));
        assertEquals(FaceParams.generate(-7L), FaceParams.generate(-7L));
    }

    @Test
    void differentSeedsAreDifferentFaces() {
        int distinct = 0;
        FaceParams first = FaceParams.generate(0);
        for (long s = 1; s <= 20; s++) {
            if (!FaceParams.generate(s).equals(first)) {
                distinct++;
            }
        }
        assertEquals(20, distinct, "twenty seeds, twenty faces");
    }

    /**
     * The family D range, measured over 400 seeds: young and old both reachable,
     * beards on a real share, scars on a minority, brow weight a genuine spread.
     */
    @Test
    void theGeneratorSpansTheFamily() {
        float ageMin = 1f, ageMax = 0f, browMin = 1f, browMax = 0f;
        int beards = 0, scars = 0;
        for (long s = 0; s < 400; s++) {
            FaceParams p = FaceParams.generate(s);
            ageMin = Math.min(ageMin, p.age());
            ageMax = Math.max(ageMax, p.age());
            browMin = Math.min(browMin, p.browWeight());
            browMax = Math.max(browMax, p.browWeight());
            if (p.facialHair() > 0.05f) {
                beards++;
            }
            if (p.scar() >= 0f) {
                scars++;
            }
        }
        assertTrue(ageMin < 0.10f && ageMax > 0.90f, "age spans " + ageMin + ".." + ageMax);
        assertTrue(browMax - browMin > 0.45f, "brow weight spans " + browMin + ".." + browMax);
        assertTrue(beards > 100 && beards < 300, beards + " beards of 400");
        assertTrue(scars > 60 && scars < 200, scars + " scars of 400");
    }

    /**
     * 4b.6: "an old face is not a young face with lines: it is a different
     * silhouette — sunken eye socket, softened jaw." The two derived quantities
     * that move the geometry must actually move with age.
     */
    @Test
    void anOldFaceIsADifferentSilhouette() {
        FaceParams young = new FaceParams(0.05f, 0.5f, 0.5f, 0.5f, 0.5f, 0f, 0.2f, -1f, 1L);
        FaceParams old = new FaceParams(0.95f, 0.5f, 0.5f, 0.5f, 0.5f, 0f, 0.2f, -1f, 1L);
        assertTrue(old.socketDepth() > young.socketDepth() * 1.8f,
                "the socket sinks with age: " + young.socketDepth() + " -> " + old.socketDepth());
        assertTrue(old.jawScale() < young.jawScale() * 0.85f,
                "the jaw softens with age: " + young.jawScale() + " -> " + old.jawScale());
    }

    /**
     * The hero is authored, and the authored face is pinned so a refactor of the
     * generator cannot silently move it: every duel capture's hero face depends
     * on these numbers.
     */
    @Test
    void theHeroFaceIsPinned() {
        FaceParams hero = FaceParams.hero();
        assertEquals(0.22f, hero.age(), 1e-6);
        assertEquals(0.72f, hero.browWeight(), 1e-6);
        assertTrue(hero.scar() < 0f, "the hero is unscarred");
        assertEquals(hero, FaceParams.hero(), "authored means constant");
    }
}
