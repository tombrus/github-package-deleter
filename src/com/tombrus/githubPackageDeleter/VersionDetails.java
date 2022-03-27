package com.tombrus.githubPackageDeleter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        return children.stream().filter(c -> c instanceof FileDetails).map(c -> (FileDetails) c).mapToLong(f -> f.size).sum();
    }

    @Override
    public String toString() {
        return name + " [" + U.humanReadable(numFiles()) + " files, " + U.humanReadable(numBytes()) + " bytes] " + getComment();
    }

    public void startDownload() {
        CompletableFuture<List<FileDetails>> list = new FileLister(this).list();
        list.thenAcceptAsync(l -> SwingUtilities.invokeLater(() -> {
            removeAllChildren();
            l.forEach(this::add);
            packageTreeModel.nodeStructureChanged(this);
            packageTreeModel.nodeChanged(packageDetails);
            packageTreeModel.nodeChanged(packageDetails.userOrOrganizationDetails);
        }));
        list.exceptionallyAsync(throwable -> {
            SwingUtilities.invokeLater(() -> {
                removeAllChildren();
                add(new DefaultMutableTreeNode(throwable.getMessage()));
                packageTreeModel.nodeStructureChanged(this);
                packageTreeModel.nodeChanged(packageDetails);
                packageTreeModel.nodeChanged(packageDetails.userOrOrganizationDetails);
            });
            return null;
        });
    }

    @Override
    public void firstExpansion() {
    }
}
