package com.tombrus.githubPackageDeleter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public class VersionDetails extends Details {
    public final PackageDetails packageDetails;
    final        String         name;
    final        String         id;
    final        long           numFiles;
    final        long           downloadsTotalCount;
    final        LocalDateTime  at;

    private boolean downloading;
    private boolean downloaded;

    public VersionDetails(PackageDetails packageDetails, String name, String id, long totalCount, long downloadsTotalCount, LocalDateTime at) {
        this.packageDetails      = packageDetails;
        this.name                = name;
        this.id                  = id;
        this.numFiles            = totalCount;
        this.downloadsTotalCount = downloadsTotalCount;
        this.at                  = at;
        setAllowsChildren(true);
        add(new DefaultMutableTreeNode("..."));
    }

    public long numFiles() {
        return numFiles;
    }

    public long numBytes() {
        return fileStream().mapToLong(f -> f.size).sum();
    }

    @Override
    public String toString() {
        String atString = at == null ? "" : at.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ", ";
        return name + " [" + U.humanReadable(numFiles()) + " files, " + U.humanReadable(numBytes()) + " bytes, " + atString + U.humanReadable(downloadsTotalCount) + " downloads" + "] " + getComment();
    }

    public void startDownload() {
        downloading = true;
        new FileLister(this).list().whenCompleteAsync((l, thr) -> SwingUtilities.invokeLater(() -> {
            removeAllChildren();
            if (thr == null) {
                l.forEach(this::add);
            } else {
                add(new DefaultMutableTreeNode(thr.getMessage()));
            }
            packageTreeModel.nodeStructureChanged(this);
            packageTreeModel.nodeChanged(packageDetails);
            packageTreeModel.nodeChanged(packageDetails.userOrOrganizationDetails);
            downloading = false;
            downloaded  = true;
        }));
    }

    public boolean isDownloading() {
        return downloading;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    @Override
    public void firstExpansion() {
    }

    public Stream<FileDetails> fileStream() {
        return childrenStream().filter(c -> c instanceof FileDetails).map(c -> (FileDetails) c);
    }
}
