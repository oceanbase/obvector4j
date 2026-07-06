package com.oceanbase.obvector4j.version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OceanBase semantic version for feature gating.
 */
public final class OceanBaseVersion implements Comparable<OceanBaseVersion> {

    /** Minimum version that supports HYBRID_SEARCH SQL interface. */
    public static final OceanBaseVersion HYBRID_SEARCH_SQL_MIN = new OceanBaseVersion(4, 6, 0);

    private static final Pattern VERSION_PATTERN =
            Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final Pattern OB_VERSION_PATTERN =
            Pattern.compile("OceanBase-v?(\\d+)\\.(\\d+)\\.(\\d+)", Pattern.CASE_INSENSITIVE);

    private final int major;
    private final int minor;
    private final int patch;

    public OceanBaseVersion(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Parse version from strings like "5.7.25-OceanBase-v4.3.5.6" or "OceanBase 4.6.0.0".
     * Prefers the OceanBase-specific segment over MySQL compatibility version.
     */
    public static OceanBaseVersion parse(String versionString) {
        if (versionString == null || versionString.trim().isEmpty()) {
            throw new IllegalArgumentException("Version string cannot be empty");
        }
        Matcher obMatcher = OB_VERSION_PATTERN.matcher(versionString);
        if (obMatcher.find()) {
            return new OceanBaseVersion(
                    Integer.parseInt(obMatcher.group(1)),
                    Integer.parseInt(obMatcher.group(2)),
                    Integer.parseInt(obMatcher.group(3)));
        }
        Matcher matcher = VERSION_PATTERN.matcher(versionString);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unable to parse OceanBase version: " + versionString);
        }
        return new OceanBaseVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)));
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public boolean isAtLeast(OceanBaseVersion other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(OceanBaseVersion other) {
        if (major != other.major) {
            return Integer.compare(major, other.major);
        }
        if (minor != other.minor) {
            return Integer.compare(minor, other.minor);
        }
        return Integer.compare(patch, other.patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
