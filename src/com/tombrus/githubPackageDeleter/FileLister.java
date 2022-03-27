package com.tombrus.githubPackageDeleter;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.tombrus.githubPackageDeleter.FileLister.Answer;

public class FileLister extends Paginator<Answer, List<FileDetails>> {
    private final static String         VERSION_QUERY = """
            query {
                node(id: "%s") {
                    ... on PackageVersion {
                        files(@SELECTOR@) {
                            pageInfo {
                                hasNextPage
                                endCursor
                            }
                            nodes {
                                id
                                name
                                size
                            }
                        }
                    }
                }

            }""";
    private final        VersionDetails versionDetails;

    public FileLister(VersionDetails versionDetails) {
        super(Answer.class);
        this.versionDetails = versionDetails;
    }

    public CompletableFuture<List<FileDetails>> list() {
        return start();
    }

    @Override
    protected String getQueryWithPagination() {
        return String.format(VERSION_QUERY, versionDetails.id);
    }

    @Override
    protected PageInfo pageInfo(Answer answer) {
        return answer.data.node.files.pageInfo;
    }

    @Override
    protected List<FileDetails> harvest(List<FileDetails> preResult, Answer answer) {
        if (answer.errors != null) {
            throw new Error(answer.errors.stream().map(e -> e.message).toList().toString());
        }
        Stream<FileDetails> packagesStream = answer.data.node.files.nodes.stream().map(ver -> new FileDetails(versionDetails, ver.name, ver.id, ver.size));
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
        Files files;
    }

    public static class Files {
        PageInfo       pageInfo;
        List<FileNode> nodes;
    }

    public static class FileNode {
        String id;
        String name;
        long   size;
    }
}
