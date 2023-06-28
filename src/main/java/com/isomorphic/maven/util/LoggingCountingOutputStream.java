package com.isomorphic.maven.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Daniel Johansson
 * @since 2014-02-17 09:04
 */
public class LoggingCountingOutputStream extends CountingOutputStream {

    private final long expectedByteCount;
    private long snapshotStart = System.currentTimeMillis();
    private long snapshotBytes;
    private long bytesPerSecond;

    /**
     * Constructs a new CountingOutputStream.
     *
     * @param out the OutputStream to write to
     * @param expectedByteCount the number of bytes expected in the stream
     */
    public LoggingCountingOutputStream(final OutputStream out, final long expectedByteCount) {
        super(out);
        this.expectedByteCount = expectedByteCount;
    }

    @Override
    protected void afterWrite(final int bytesWritten) throws IOException {
        super.afterWrite(bytesWritten);

        final long byteCount = getByteCount();
        final double progress = ((double) byteCount / (double) expectedByteCount) * 100.0;

        snapshotBytes += bytesWritten;

        // Lets grab the number of bytes written over the last second and use that as our
        // bytesPerSecond gauge giving the user an indication of speed.
        if (System.currentTimeMillis() - snapshotStart >= 1000) {
            snapshotStart = System.currentTimeMillis();
            bytesPerSecond = snapshotBytes;
            snapshotBytes = 0;
        }

        // Using \r in this print out will cause the console to render this line of text on the
        // same line in order to avoid console spam.
        System.out.print("\r" + StringUtils.rightPad(FileUtils.byteCountToDisplaySize(byteCount) + " / " + FileUtils.byteCountToDisplaySize(expectedByteCount) + " (" + FileUtils.byteCountToDisplaySize(bytesPerSecond) + "/second) " + String.format("%.1f%%", progress), 60, " "));

        if (progress >= 100.0) {
            System.out.println("Done!");
        }
    }
}
