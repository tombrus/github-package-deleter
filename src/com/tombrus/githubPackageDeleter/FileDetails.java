package com.tombrus.githubPackageDeleter;

public class FileDetails extends Details {
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private final VersionDetails versionDetails;
    final         String         name;
    final         String         id;
    final         long           size;

    public FileDetails(VersionDetails versionDetails, String name, String id, long size) {
        this.versionDetails = versionDetails;
        this.name           = name;
        this.id             = id;
        this.size           = size;
        setAllowsChildren(false);
    }

    @Override
    public String toString() {
        return name + " [" + U.humanReadable(size) + " bytes] " + getComment();
    }

    @Override
    public void firstExpansion() {
    }
}
