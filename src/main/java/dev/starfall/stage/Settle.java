package dev.starfall.stage;

import java.util.ArrayList;
import java.util.List;

/**
 * When each link of a chain arrives, in seconds. The fix for STYLE.md 7.0.3.
 *
 * <p><b>The debt this discharges.</b> {@code docs/system2-debt.md} E3 measured the
 * chain arriving as a unit -- "head 0.3 / 0.0 / 0.0, chest 0.4 / 0.1 / 0.1, waist
 * 0.5 / 0.0 / 0.1, hip 0.1 / 0.0 / 0.0... everything else stops together" -- and
 * called fixing it "nearly free". STYLE.md 7.0.3 says the same thing from the
 * other end: "it requires only that settle times differ down the chain rather than
 * being shared." The reason it was still open is that nothing owned the numbers.
 * {@code IkChain} has the knobs ({@code settleSeconds} and {@code boneLagSeconds})
 * and explicitly declines to choose values for them; {@code RigIk} chose a set for
 * one gesture. The staging layer owns them across a whole phrase, so this is where
 * they live.
 *
 * <p><b>The band is spent, not sampled.</b> STYLE.md 7.1 gives 0.3-0.6 s. A single
 * chain-wide settle anywhere inside that interval satisfies the letter of 7.1 and
 * violates 7.0.3 and 10's last row, which is exactly the state pass 3 was failed
 * for. So the interval is treated as a range to spread across: the root sits on
 * {@link Timing#SETTLE_ROOT} and the tip on {@link Timing#SETTLE_TIP}, and every
 * link between them is strictly later than the one above it. Nothing arrives
 * outside the band and no two things arrive together.
 *
 * <p><b>Two numbers, because {@code IkChain} has two knobs.</b> {@link #base()} is
 * the chain's own {@code settleSeconds}; {@link #lag(int)} is the extra
 * {@code boneLagSeconds} on each link below the first. That split is not an
 * implementation detail leaking upward -- it is the difference between lagging the
 * chain's <em>target</em> (which moves every bone together, and is the thing 7.0.3
 * bans) and lagging one bone's solved angle behind its parent's, which is the
 * thing 7.0.3 asks for.
 *
 * @param arrivals seconds at which each link comes to rest, root first
 */
public record Settle(List<Double> arrivals) {

    public Settle {
        if (arrivals == null || arrivals.isEmpty()) {
            throw new IllegalArgumentException("a settle profile covers at least one link");
        }
        arrivals = List.copyOf(arrivals);
        for (int i = 0; i < arrivals.size(); i++) {
            double a = arrivals.get(i);
            if (a < Timing.SETTLE_ROOT - 1e-9 || a > Timing.SETTLE_TIP + 1e-9) {
                throw new IllegalArgumentException(
                        "link " + i + " settles at " + a + ", outside STYLE.md 7.1's 0.3-0.6 band");
            }
            if (i > 0 && a <= arrivals.get(i - 1) + 1e-9) {
                throw new IllegalArgumentException(
                        "link " + i + " arrives at " + a + ", not after link " + (i - 1)
                                + " at " + arrivals.get(i - 1) + " -- STYLE.md 7.0.3 forbids a chain "
                                + "arriving as a unit");
            }
        }
    }

    public static Settle of(double... arrivals) {
        List<Double> list = new ArrayList<>(arrivals.length);
        for (double a : arrivals) {
            list.add(a);
        }
        return new Settle(list);
    }

    public int links() {
        return arrivals.size();
    }

    /** The chain's own settle time: {@code IkChain.settleSeconds}. */
    public double base() {
        return arrivals.get(0);
    }

    /** Link {@code index}'s arrival, in seconds after the visible motion ends. */
    public double arrival(int index) {
        return arrivals.get(index);
    }

    /** Extra lag on link {@code index}: {@code IkChain.boneLagSeconds(index, ...)}. */
    public double lag(int index) {
        return arrivals.get(index) - base();
    }

    /** The last link's arrival. What is still drifting after everything else has stopped. */
    public double tip() {
        return arrivals.get(arrivals.size() - 1);
    }

    @Override
    public String toString() {
        return arrivals.toString();
    }
}
