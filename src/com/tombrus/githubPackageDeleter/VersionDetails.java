package com.tombrus.githubPackageDeleter;

import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class VersionDetails extends Details {
    public final PackageDetails packageDetails;
    final        String         name;
    final        String         id;
    final        long           numFiles;

    public VersionDetails(PackageDetails packageDetails, String name, String id, int totalCount) {
        this.packageDetails = packageDetails;
        this.name           = name;
        this.id             = id;
        this.numFiles       = totalCount;
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
        return name + " [" + U.humanReadable(numFiles()) + " files, " + U.humanReadable(numBytes()) + " bytes] " + getComment();
    }

    public void startDownload() {
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
        }));
    }

    @Override
    public void firstExpansion() {
    }

    public Stream<FileDetails> fileStream() {
        return childrenStream().filter(c -> c instanceof FileDetails).map(c -> (FileDetails) c);
    }
}
