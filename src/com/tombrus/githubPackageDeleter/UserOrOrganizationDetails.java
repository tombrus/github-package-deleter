package com.tombrus.githubPackageDeleter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        return children.stream().filter(c -> c instanceof PackageDetails).map(c -> (PackageDetails) c).mapToLong(PackageDetails::numVersions).sum();
    }

    public long numFiles() {
        return children.stream().filter(c -> c instanceof PackageDetails).map(c -> (PackageDetails) c).mapToLong(PackageDetails::numFiles).sum();
    }

    public long numBytes() {
        return children.stream().filter(c -> c instanceof PackageDetails).map(c -> (PackageDetails) c).mapToLong(PackageDetails::numBytes).sum();
    }

    @Override
    public String toString() {
        return name + " [" + U.humanReadable(numPackages()) + " packages, " + U.humanReadable(numVersions()) + " versions, " + U.humanReadable(numFiles()) + " files, " + U.humanReadable(numBytes()) + " bytes] " + getComment();
    }

    public void startDownload() {
        CompletableFuture<List<PackageDetails>> list = new PackageLister(this).list();
        list.thenAcceptAsync(l -> {
            numPackages = l.size();
            SwingUtilities.invokeLater(() -> {
                removeAllChildren();
                l.forEach(this::add);
                packageTreeModel.nodeStructureChanged(this);
                l.forEach(PackageDetails::startDownload);
                githubPackageDeleter.showDeleterPaneIfNoSettingsChange();
                githubPackageDeleter.settingsInfoLabel.setText("ok");
            });
        });
        list.exceptionallyAsync(throwable -> {
            numPackages = -1;
            SwingUtilities.invokeLater(() -> {
                removeAllChildren();
                String message = throwable.getCause().getMessage();
                add(new DefaultMutableTreeNode(message));
                packageTreeModel.nodeStructureChanged(this);
                githubPackageDeleter.settingsInfoLabel.setText(message);
            });
            return null;
        });
    }

    @Override
    public void firstExpansion() {
    }
}
