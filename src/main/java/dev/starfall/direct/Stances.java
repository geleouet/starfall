package dev.starfall.direct;

import dev.starfall.anim.Pose;
import dev.starfall.stage.Stance;

import java.util.EnumMap;
import java.util.Map;

/**
 * What each {@link Stance} does to the trunk, as a small set of bone-local
 * deltas.
 *
 * <h2>Why this is a condition and not a clip</h2>
 *
 * <p>{@code docs/system2-debt.md} E1 is the sharpest criticism in the project's
 * record and it is a criticism of exactly this channel's absence: <i>"the poetry
 * is an <b>event</b> rather than a <b>condition</b>. The hip moves during the
 * gesture and returns. Images 1 and 2 are standing still and are more loaded than
 * any frame here."</i> {@code Stance}'s own javadoc restates it and says what the
 * fix has to be -- a channel the schedule can hold a value on for a whole phrase,
 * which the IK targets cannot be because an IK target is an event.
 *
 * <p>So every entry below is a <em>held</em> shape, and every one of them is
 * small. The numbers are degrees of local rotation on seven bones, and the
 * largest of them is fourteen. That restraint is the point: this layer writes the
 * bone locals the IK chains then lean on, and STYLE.md 7.0.2 wants the figure
 * "gravity-loaded and reluctant" rather than posed. A stance big enough to read
 * on its own would be a clip, and the rig has no clips.
 *
 * <p><b>Nothing here is mirrored by hand.</b> These are local rotations, and a
 * local rotation under a mirrored root produces the mirrored world rotation for
 * free -- "lean into the blow" leans the right way for a figure facing either
 * direction. The one thing that would need a sign is a local <em>x</em>
 * translation, and there are none.
 *
 * <h2>The asymmetry, which is deliberate</h2>
 *
 * <p>Every entry counter-rotates the chest against the hips and takes the head
 * off the trunk axis. STYLE.md 7.0.1: "in every reference image the figure is a
 * spiral". A stance whose hips, chest and head all rotate the same way is a
 * figure leaning; one where they alternate is a figure wound. {@code shoulderR}
 * -- the off arm -- carries the third turn, because it is the only bone in the
 * upper body no IK chain touches, so whatever it is given survives the solve.
 */
final class Stances {

    /**
     * One stance's deltas. Seven bones, because that is the whole trunk plus the
     * off shoulder; the sword arm and both legs belong to IK chains and anything
     * written here would be overwritten by weight.
     */
    private record Shape(float hipDy, float hipRot, float spineRot, float chestRot,
                         float neckRot, float headRot, float offShoulderRot) {

        static Shape lerp(Shape a, Shape b, float u) {
            return new Shape(
                    mix(a.hipDy, b.hipDy, u),
                    mix(a.hipRot, b.hipRot, u),
                    mix(a.spineRot, b.spineRot, u),
                    mix(a.chestRot, b.chestRot, u),
                    mix(a.neckRot, b.neckRot, u),
                    mix(a.headRot, b.headRot, u),
                    mix(a.offShoulderRot, b.offShoulderRot, u));
        }

        private static float mix(float a, float b, float u) {
            return a + (b - a) * u;
        }
    }

    private static final Map<Stance, Shape> SHAPES = new EnumMap<>(Stance.class);

    static {
        // The loaded rest of reference images 1 and 2. Hip settled a little onto
        // the weighted foot, ribcage turned back against it, head off the axis.
        // "Never a symmetric A-pose", says Stance.READY, and this is the smallest
        // shape that is not one.
        SHAPES.put(Stance.READY, new Shape(-0.015f, 3f, -4f, 5f, -3f, -4f, 6f));

        // Gathering: STYLE.md 7.1's 40% wind-up seen as a whole-body condition.
        // The trunk coils away from where the hand is going while the head has
        // already committed to it -- the gaze leads, the body follows, which is
        // 4b.6's channel doing the anticipation the arm cannot do alone.
        SHAPES.put(Stance.GATHERING, new Shape(-0.030f, -5f, -6f, -8f, 4f, 5f, 10f));

        // The blade held across. Chest lifted and squared, head tucked down
        // behind it, off arm up to support the hilt. This is the condition the
        // deflection curve of STYLE.md 7.2 is drawn out of, so it must already be
        // a guard before the blades touch -- which is why the scheduler raises it
        // one contact span *before* the crossing.
        SHAPES.put(Stance.GUARD_RAISED, new Shape(-0.010f, 2f, 3f, 6f, -5f, -6f, -14f));

        // Two braced bodies: combat-design.md 2.1's "shoulder-and-hilt check...
        // contact is sustained, not instantaneous". Weight dropped and forward,
        // both shoulders down into it.
        SHAPES.put(Stance.BRACED, new Shape(-0.045f, -3f, 5f, 4f, -2f, -3f, 4f));

        // STYLE.md 7.3's fourth hit reaction, and the one the section is most
        // insistent about: "the whole figure gives on a soft IK curve, spine
        // bending, rather than recoiling on a hard offset". So the give is
        // authored as spine and chest bend plus a sink at the hip -- there is no
        // translation of the body anywhere in this entry, because a translation
        // is precisely the hard offset the rule forbids.
        SHAPES.put(Stance.YIELDING, new Shape(-0.050f, 6f, 10f, 9f, 6f, 8f, -8f));

        // Carried: 7.2's knockback, "like a sheet of silk caught in wind". A
        // different condition from being struck -- less sink, more trail: the
        // upper body is behind the hips for the whole of the travel, which is
        // what makes the figure look pushed rather than posed.
        SHAPES.put(Stance.CARRIED, new Shape(-0.020f, 4f, 8f, 10f, 7f, 9f, -6f));

        // Two figures passing through each other. Narrowed: the chest turns hard
        // out of frame so the silhouette thins as the two cross, which is the
        // only way an interpenetration with no impact reads as anything at all.
        SHAPES.put(Stance.PASSING, new Shape(0f, -6f, -5f, -10f, 6f, 7f, 14f));

        // combat-design.md 1.4's Stillness: "the figure's pigment dries". A thing
        // to draw and not an absence of one, but barely -- the tell is that the
        // spiral goes out of it, not that it adopts a new shape.
        SHAPES.put(Stance.DRIED, new Shape(0f, 0f, 1f, 1f, 0f, 1f, 0f));

        // Going. The body gives well before the ink has finished, so this is the
        // largest shape in the file and it is all in one direction: everything
        // folds forward and down.
        SHAPES.put(Stance.SLACK, new Shape(-0.090f, 8f, 14f, 12f, 10f, 14f, -12f));
    }

    private Stances() {
    }

    /**
     * Writes the blend of two stances into {@code pose}.
     *
     * <p>{@code pose} is reused rather than rebuilt: this runs sixty times a
     * second for every body on the lane, and {@code Pose.set} overwrites by name.
     */
    static void blend(Pose pose, Stance from, Stance to, float u) {
        Shape s = Shape.lerp(SHAPES.get(from), SHAPES.get(to), Math.max(0f, Math.min(1f, u)));
        pose.set("hips", 0f, s.hipDy(), s.hipRot());
        pose.set("spine", 0f, 0f, s.spineRot());
        pose.set("chest", 0f, 0f, s.chestRot());
        pose.set("neck", 0f, 0f, s.neckRot());
        pose.set("head", 0f, 0f, s.headRot());
        pose.set("shoulderR", 0f, 0f, s.offShoulderRot());
    }
}
