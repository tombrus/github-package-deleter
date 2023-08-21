package com.tombrus.githubPackageDeleter;

import com.tombrus.githubPackageDeleter.VersionLister.Answer;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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
                                                                                  nodes {
                                                                                      name
                                                                                      updatedAt
                                                                                  }
                                                                              }
                                                                              statistics {
                                                                                  downloadsTotalCount
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
        if (answer.extensions != null) {
            answer.extensions.report();
        }
        if (answer.errors != null) {
            throw new Error(answer.errors.stream().map(e -> e.message).toList().toString());
        }
        if (peeker != null) {
            peeker.accept(answer);
        }
        Stream<VersionDetails> packagesStream = answer.data.node.versions.nodes.stream()
                                                                               .map(ver -> {
                                                                                   LocalDateTime at = ver.files.nodes.stream().map(n -> LocalDateTime.parse(n.updatedAt.replaceAll("Z.*", ""), ISO_LOCAL_DATE_TIME)).findFirst().orElse(null);
                                                                                   return new VersionDetails(packageDetails, ver.version, ver.id, ver.files.totalCount, ver.statistics.downloadsTotalCount, at);
                                                                               });
        if (preResult != null) {
            packagesStream = Stream.concat(preResult.stream(), packagesStream);
        }
        return packagesStream.sorted(Comparator.comparing(p -> p.name)).toList();
    }

    public static class Answer {
        Data            data;
        List<ErrorInfo> errors;
        Extensions      extensions;
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
        String                   version;
        String                   id;
        Files                    files;
        PackageVersionStatistics statistics;
    }

    public static class Files {
        int               totalCount;
        List<PackageFile> nodes;
    }

    public static class PackageFile {
        String name;
        String updatedAt;
    }

    public static class PackageVersionStatistics {
        int downloadsTotalCount;
    }
}
