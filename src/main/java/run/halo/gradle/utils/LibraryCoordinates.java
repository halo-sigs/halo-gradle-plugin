package run.halo.gradle.utils;

/**
 * Encapsulates information about the artifact coordinates of a library.
 */
public interface LibraryCoordinates {

    /**
     * Return the group ID of the coordinates.
     *
     * @return the group ID
     */
    String getGroupId();

    /**
     * Return the artifact ID of the coordinates.
     *
     * @return the artifact ID
     */
    String getArtifactId();

    /**
     * Return the version of the coordinates.
     *
     * @return the version
     */
    String getVersion();

    /**
     * Utility method that returns the given coordinates using the standard
     * {@code group:artifact:version} form.
     *
     * @param coordinates the coordinates to convert (may be {@code null})
     * @return the standard notation form or {@code "::"} when the coordinates are null
     */
    static String toStandardNotationString(LibraryCoordinates coordinates) {
        if (coordinates == null) {
            return "::";
        }
        StringBuilder builder = new StringBuilder();
        builder.append((coordinates.getGroupId() != null) ? coordinates.getGroupId() : "");
        builder.append(":");
        builder.append((coordinates.getArtifactId() != null) ? coordinates.getArtifactId() : "");
        builder.append(":");
        builder.append((coordinates.getVersion() != null) ? coordinates.getVersion() : "");
        return builder.toString();
    }

}