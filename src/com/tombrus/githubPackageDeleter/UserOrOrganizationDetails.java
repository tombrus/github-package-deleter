package com.tombrus.githubPackageDeleter;

import java.util.stream.Stream;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class UserOrOrganizationDetails extends Details {
    public final String name;
    public       int    numPackages;

    public UserOrOrganizationDetails(String name) {
        this.name   = name;
        numPackages = -1;
        setAllowsChildren(true);
        add(new DefaultMutableTreeNode("..."));
    }

    public long numPackages() {
        return numPackages;
    }

    public long numVersions() {
        return packageStream().mapToLong(PackageDetails::numVersions).sum();
    }

    public long numFiles() {
        return packageStream().mapToLong(PackageDetails::numFiles).sum();
    }

    public long numBytes() {
        return packageStream().mapToLong(PackageDetails::numBytes).sum();
    }

    @Override
    public String toString() {
        return name + " [" + U.humanReadable(numPackages()) + " packages, " + U.humanReadable(numVersions()) + " versions, " + U.humanReadable(numFiles()) + " files, " + U.humanReadable(numBytes()) + " bytes] " + getComment();
    }

    public void startDownload() {
        new PackageLister(this).list().whenCompleteAsync((l, thr) -> SwingUtilities.invokeLater(() -> {
            assert l != null;
            numPackages = thr != null ? -1 : l.size();
            removeAllChildren();
            String message;
            if (thr == null) {
                message = "ok";
                l.forEach(this::add);
                l.forEach(PackageDetails::startDownload);
                githubPackageDeleter.showDeleterPaneIfNoSettingsChange();
            } else {
                message = thr.getCause().getMessage();
                add(new DefaultMutableTreeNode(message));
            }
            githubPackageDeleter.settingsInfoLabel.setText(message);
            packageTreeModel.nodeStructureChanged(this);
        }));
    }

    @Override
    public void firstExpansion() {
    }

    public Stream<PackageDetails> packageStream() {
        return childrenStream().filter(c -> c instanceof PackageDetails).map(c -> (PackageDetails) c);
    }
}
