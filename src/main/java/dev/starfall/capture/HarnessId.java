package dev.starfall.capture;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * Who took this picture: the source revision, and a fingerprint of the capture path itself.
 *
 * <p>STYLE.md §11.2b(d), which was written after a harness bug was found to have corrupted
 * every frame this project ever captured:
 *
 * <blockquote>Every claim that survived the re-capture was <i>relative</i>. Every claim that
 * died was <i>absolute</i> — coverage percentages, luminance standard deviations, the
 * brightest interior pixel, mark-run counts. <b>An absolute pixel statistic is valid only
 * against the harness that produced it</b>, so {@code capture.txt} records {@code commit=}
 * and {@code harness=}, and any comparison spanning two harness versions is void by default
 * rather than by discovery.</blockquote>
 *
 * <h2>Why two fields and not one</h2>
 *
 * <p>{@code commit} answers "what source was this?" and is the thing a human reads. It is not
 * sufficient on its own: it is unavailable when git is not on the path, it says nothing about
 * uncommitted edits beyond a {@code -dirty} suffix, and — the case that actually matters — two
 * different commits usually share an identical capture path, so refusing every cross-commit
 * comparison would void almost every legitimate before/after this project runs.
 *
 * <p>{@code harness} answers the narrower question §11.2b(d) is really asking: <b>did the
 * apparatus change between these two captures?</b> It is a digest of the compiled bytecode of
 * the classes that turn a scene into PNG bytes — nothing else. A shader edit, a rig change or
 * a new scene leaves it alone, because those are the <em>subject</em>; the flip bug that
 * composited instead of assigning would have changed it, because that is the <em>apparatus</em>.
 * So a before/after across a shader change stays legal and a before/after across a capture-path
 * change is refused, which is the distinction the section draws.
 */
public final class HarnessId {

    /**
     * The classes that constitute the capture path. Everything a pixel passes through between
     * {@code Scene.render()} and the bytes on disk, and the clock that decides which moment is
     * being drawn.
     */
    private static final Class<?>[] APPARATUS = {
            CaptureApp.class,
            CaptureSpec.class,
            Framebuffers.class,
            ContactSheet.class,
            SceneClock.class,
            TimingApp.class,
    };

    private static String cachedHarness;
    private static String cachedCommit;

    private HarnessId() {
    }

    /**
     * A 12-hex-character digest of the apparatus above, or {@code "unknown"} if the class
     * bytes cannot be read (which happens only in an exotic classloader).
     */
    public static synchronized String harness() {
        if (cachedHarness != null) {
            return cachedHarness;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Class<?> c : APPARATUS) {
                String path = "/" + c.getName().replace('.', '/') + ".class";
                try (InputStream in = HarnessId.class.getResourceAsStream(path)) {
                    if (in == null) {
                        cachedHarness = "unknown";
                        return cachedHarness;
                    }
                    md.update(readAll(in));
                }
            }
            byte[] d = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", d[i]));
            }
            cachedHarness = sb.toString();
        } catch (Exception e) {
            cachedHarness = "unknown";
        }
        return cachedHarness;
    }

    /**
     * {@code git rev-parse --short HEAD}, with {@code -dirty} appended when the working tree
     * has modifications. {@code "unknown"} when git is unavailable — recorded as such rather
     * than omitted, because "nobody wrote it down" and "this was a clean tree" must not read
     * the same, which is §11.3 applied to the manifest.
     */
    public static synchronized String commit() {
        if (cachedCommit != null) {
            return cachedCommit;
        }
        String head = git("rev-parse", "--short", "HEAD");
        if (head == null) {
            cachedCommit = "unknown";
            return cachedCommit;
        }
        String status = git("status", "--porcelain", "--untracked-files=no");
        cachedCommit = status != null && !status.isEmpty() ? head + "-dirty" : head;
        return cachedCommit;
    }

    private static String git(String... args) {
        try {
            String[] cmd = new String[args.length + 1];
            cmd[0] = "git";
            System.arraycopy(args, 0, cmd, 1, args.length);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(false);
            Process p = pb.start();
            byte[] out = readAll(p.getInputStream());
            if (!p.waitFor(10, TimeUnit.SECONDS) || p.exitValue() != 0) {
                return null;
            }
            return new String(out).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
