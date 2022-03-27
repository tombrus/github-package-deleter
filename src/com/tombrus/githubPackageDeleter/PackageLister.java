package com.tombrus.githubPackageDeleter;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.tombrus.githubPackageDeleter.PackageLister.Answer;

public class PackageLister extends Paginator<Answer, List<PackageDetails>> {
    private final static String                    ORGANIZATION_QUERY = """
            query {
                organization(login: "%s") {
                    packages(first:1) {
                        nodes { id }
                    }
                }
                user(login: "%s") {
                    packages(first:1) {
                        nodes { id }
                    }
                }
            }""";
    private final static String                    PACKAGE_QUERY      = """
            query {
                %s(login: "%s") {
                    packages(@SELECTOR@) {
                        pageInfo {
                            hasNextPage
                            endCursor
                        }
                        nodes {
                            name
                            id
                            versions(first: 1) {
                                totalCount
                            }
                        }
                    }
                }
            }""";
    private final        UserOrOrganizationDetails userOrOrganizationDetails;
    private              String                    userOrOrganization;

    public PackageLister(UserOrOrganizationDetails userOrOrganizationDetails) {
        super(Answer.class);
        this.userOrOrganizationDetails = userOrOrganizationDetails;
    }

    public CompletableFuture<List<PackageDetails>> list() {
        return CompletableFuture.supplyAsync(this::checkUserOrOrganization).handleAsync((a, e) -> {
            userOrOrganization = a.data.organization != null ? "organization" : a.data.user != null ? "user" : null;
            if (userOrOrganization == null) {
                throw new IllegalArgumentException("'" + userOrOrganizationDetails.name + "' is no Organization and no User");
            }
            return start().join();
        });
    }

    private Answer checkUserOrOrganization() {
        return new GraphQL<>(Answer.class, false).query(String.format(ORGANIZATION_QUERY, userOrOrganizationDetails.name, userOrOrganizationDetails.name));
    }

    @Override
    protected String getQueryWithPagination() {
        return String.format(PACKAGE_QUERY, userOrOrganization, userOrOrganizationDetails.name);
    }

    protected PageInfo pageInfo(Answer answer) {
        return answer.data.packages().pageInfo;
    }

    @Override
    protected List<PackageDetails> harvest(List<PackageDetails> previous, Answer answer) {
        if (answer.errors != null) {
            throw new Error(answer.errors.stream().map(e -> e.message).toList().toString());
        }
        Stream<PackageDetails> packagesStream = answer.data.packages().nodes.stream()
                .filter(p -> !p.name.startsWith("deleted_"))
                .map(p -> new PackageDetails(userOrOrganizationDetails, p.name, p.id, p.versions.totalCount));
        if (previous != null) {
            packagesStream = Stream.concat(previous.stream(), packagesStream);
        }
        return packagesStream.sorted(Comparator.comparing(p -> p.name)).toList();
    }

    public static class Answer {
        Data            data;
        List<ErrorInfo> errors;
    }

    public static class Data {
        Organization organization;
        User         user;

        public Packages packages() {
            if (organization != null) {
                return organization.packages;
            }
            if (user != null) {
                return user.packages;
            }
            throw new IllegalArgumentException();
        }
    }

    public static class Organization {
        Packages packages;
    }

    public static class User {
        Packages packages;
    }

    public static class Packages {
        PageInfo          pageInfo;
        List<PackageNode> nodes;
    }

    public static class PackageNode {
        String   name;
        String   id;
        Versions versions;
    }

    public static class Versions {
        int totalCount;
    }
}
