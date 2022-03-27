package com.tombrus.githubPackageDeleter;

import java.util.Collections;
import java.util.stream.Stream;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public abstract class Details extends DefaultMutableTreeNode {
    protected final GithubPackageDeleter githubPackageDeleter = GithubPackageDeleter.INSTANCE;
    protected final DefaultTreeModel     packageTreeModel     = githubPackageDeleter.getPackageTreeModel();
    private         String               comment              = "";

    abstract public void firstExpansion();

    public String getComment() {
        return comment;
    }

    public void setCommect(String msg) {
        this.comment = msg;
    }

    public Stream<DefaultMutableTreeNode> childrenStream() {
        return Collections.list(children()).stream().map(c -> (DefaultMutableTreeNode) c);
    }
}
