package com.tombrus.githubPackageDeleter;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

import com.tombrus.githubPackageDeleter.VersionDeleter.Answer;

public class VersionDeleter extends GraphQL<Answer> {
    private static final String                     DELETE_VERSION = """
            mutation {
                deletePackageVersion(input:{packageVersionId:"%s"}) {
                    clientMutationId
                    success
                }
            }""";
    private final        Collection<VersionDetails> versionDetails;

    public VersionDeleter(Collection<VersionDetails> versionDetails) {
        super(Answer.class, true);
        this.versionDetails = versionDetails;
    }

    public void delete() {
        AtomicInteger       doneCount      = new AtomicInteger(versionDetails.size());
        Set<PackageDetails> packageDetails = versionDetails.stream().map(v -> v.packageDetails).collect(Collectors.toSet());
        versionDetails.forEach(v -> {
            CompletableFuture<Answer> answer = queryAsync(String.format(DELETE_VERSION, v.id));
            answer.thenAcceptAsync(a -> {
                String comment = null;
                if (a.errors != null) {
                    comment = "error: " + a.errors.stream().map(e -> e.message).collect(Collectors.joining(", "));
                } else if (a.data == null || a.data.deletePackageVersion == null || !a.data.deletePackageVersion.success) {
                    comment = "error: delete unsuccessful";
                }
                String comment_ = comment;
                SwingUtilities.invokeLater(() -> {
                    DefaultTreeModel packageTreeModel = GithubPackageDeleter.INSTANCE.getPackageTreeModel();
                    if (comment_ == null) {
                        packageTreeModel.removeNodeFromParent(v);
                    } else {
                        v.setCommect(comment_);
                    }
                    packageTreeModel.nodeChanged(v);
                });
                if (doneCount.decrementAndGet() == 0) {
                    packageDetails.forEach(PackageDetails::startDownload);
                }
            });
            answer.exceptionallyAsync(e -> {
                SwingUtilities.invokeLater(() -> v.setCommect("error: " + e.getMessage()));
                if (doneCount.decrementAndGet() == 0) {
                    packageDetails.forEach(PackageDetails::startDownload);
                }
                return null;
            });
        });
    }

    public static class Answer {
        List<ErrorInfo> errors;
        Data            data;
    }

    public static class Data {
        DeletedPackageVersion deletePackageVersion;
    }

    @SuppressWarnings("unused")
    public static class DeletedPackageVersion {
        String  clientMutationId;
        boolean success;
    }
}
