package com.tombrus.githubPackageDeleter;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.tombrus.githubPackageDeleter.VersionLister.Answer;

public class VersionLister extends Paginator<Answer, List<VersionDetails>> {
    private final static String           VERSION_QUERY = """
            query {
                node(id:"%s") {
                    ... on Package {
                        name
                        versions(@SELECTOR@) {
                            pageInfo {
                                hasNextPage
                                endCursor
                            }
                            nodes {
                                id
                                version
                                files(first:1) {
                                    totalCount
                                }
                            }
                        }
                    }
                }
            }""";
    private final        PackageDetails   packageDetails;
    private              Consumer<Answer> peeker;

    public VersionLister(PackageDetails packageDetails) {
        super(Answer.class);
        this.packageDetails = packageDetails;
    }

    public VersionLister peek(Consumer<Answer> peeker) {
        this.peeker = peeker;
        return this;
    }

    public CompletableFuture<List<VersionDetails>> list() {
        return start();
    }

    @Override
    protected String getQueryWithPagination() {
        return String.format(VERSION_QUERY, packageDetails.id);
    }

    @Override
    protected PageInfo pageInfo(Answer answer) {
        return answer.data.node.versions.pageInfo;
    }

    @Override
    protected List<VersionDetails> harvest(List<VersionDetails> preResult, Answer answer) {
        if (peeker != null) {
            peeker.accept(answer);
        }
        if (answer.errors != null) {
            throw new Error(answer.errors.stream().map(e -> e.message).toList().toString());
        }
        Stream<VersionDetails> packagesStream = answer.data.node.versions.nodes.stream().map(ver -> new VersionDetails(packageDetails, ver.version, ver.id, ver.files.totalCount));
        if (preResult != null) {
            packagesStream = Stream.concat(preResult.stream(), packagesStream);
        }
        return packagesStream.sorted(Comparator.comparing(p -> p.name)).toList();
    }

    public static class Answer {
        Data            data;
        List<ErrorInfo> errors;
    }

    public static class Data {
        Node node;
    }

    public static class Node {
        Versions versions;
        String   name;
    }

    public static class Versions {
        PageInfo          pageInfo;
        List<VersionNode> nodes;
    }

    public static class VersionNode {
        String version;
        String id;
        Files  files;
    }

    public static class Files {
        int totalCount;
    }
}
