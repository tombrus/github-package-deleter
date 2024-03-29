package com.tombrus.githubPackageDeleter;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PackageDetails extends Details {
    public final UserOrOrganizationDetails userOrOrganizationDetails;
    public       String                    name;
    public final String                    id;
    public final int                       numVersions;

    public PackageDetails(UserOrOrganizationDetails userOrOrganizationDetails, String name, String id, int totalCount) {
        this.userOrOrganizationDetails = userOrOrganizationDetails;
        this.name                      = name;
        this.id                        = id;
        this.numVersions               = totalCount;
        setAllowsChildren(true);
        add(new DefaultMutableTreeNode("..."));
    }

    public long numVersions() {
        return numVersions;
    }

    public long numFiles() {
        return versionStream().mapToLong(VersionDetails::numFiles).sum();
    }

    public long numBytes() {
        return versionStream().mapToLong(VersionDetails::numBytes).sum();
    }

    public String numBytesHuman() {
        boolean anyDownloading = versionStream().anyMatch(VersionDetails::isDownloading);
        boolean allDownloaded  = versionStream().allMatch(VersionDetails::isDownloaded);
        if (allDownloaded) {
            return U.humanReadable(numBytes());
        }
        if (anyDownloading) {
            return U.humanReadable(numBytes()) + "...";
        }
        return "...";
    }

    @Override
    public String toString() {
        return name + " [" + U.humanReadable(numVersions()) + " versions, " + U.humanReadable(numFiles()) + " files, " + numBytesHuman() + " bytes] " + getComment();
    }

    public void startDownload() {
        new VersionLister(this)
                .peek(a -> name = a.data.node.name)
                .list()
                .whenCompleteAsync((l, thr) -> SwingUtilities.invokeLater(() -> {
                    if (thr != null) {
                        removeAllChildren();
                        add(new DefaultMutableTreeNode(thr.getMessage()));
                        packageTreeModel.nodeStructureChanged(this);
                        packageTreeModel.nodeChanged(userOrOrganizationDetails);
                    } else if (name.startsWith("deleted_")) {
                        int thisIndex = this.getParent().getIndex(this);
                        this.removeFromParent();
                        packageTreeModel.nodesWereRemoved(userOrOrganizationDetails, new int[]{thisIndex}, new Object[]{this});
                    } else {
                        // remove all non Details nodes (can be the initial "..." node)
                        childrenStream().filter(c -> !(c instanceof Details)).toList().forEach(DefaultMutableTreeNode::removeFromParent);

                        Map<String, VersionDetails> oldVersionMap = versionStream().collect(Collectors.toMap(c -> c.name, c -> c));
                        Map<String, VersionDetails> newVersionMap = l.stream().collect(Collectors.toMap(c -> c.name, c -> c));
                        oldVersionMap.values().stream().filter(c -> !newVersionMap.containsKey(c.name)).forEach(DefaultMutableTreeNode::removeFromParent);
                        newVersionMap.values().stream().filter(c -> !oldVersionMap.containsKey(c.name)).forEach(this::add);
                        U.sort(this);

                        packageTreeModel.nodeStructureChanged(this);
                        packageTreeModel.nodeChanged(userOrOrganizationDetails);
                    }
                }));
    }

    @Override
    public void firstExpansion() {
        versionStream().forEach(VersionDetails::startDownload);
    }

    public Stream<VersionDetails> versionStream() {
        return childrenStream().filter(c -> c instanceof VersionDetails).map(c -> (VersionDetails) c);
    }
}
