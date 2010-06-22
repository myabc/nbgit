/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Jonas Fonseca <fonseca@diku.dk>
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file.
 *
 * This particular file is subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.nbgit.client;

import org.nbgit.junit.RepositoryTestCase;
import org.eclipse.jgit.lib.Commit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Tree;
import org.eclipse.jgit.lib.TreeEntry;

public class CommitBuilderTest extends RepositoryTestCase {

    public CommitBuilderTest() {
        super(CommitBuilderTest.class.getSimpleName());
    }

    public void testCreate() throws Exception {
        assertNotNull(CommitBuilder.create(repository));
        assertNotNull(CommitBuilder.create(workDir));
    }

    public void testEmptyCommit() throws Exception {
        CommitBuilder.create(repository).
                time(getTime(), getTimeZone()).
                log(logger).
                message(message()).
                write();
        compareCommitFiles();
    }

    public void testNonEmptyCommit() throws Exception {
        CommitBuilder.create(repository).
                time(getTime(), getTimeZone()).
                log(logger).
                addAll(toFiles(workDir, "a")).
                message(message()).
                write();
        compareCommitFiles();
    }

    public void testInitialEmptyCommit() throws Exception {
        toGitDirFile("packed-refs").delete();
        CommitBuilder.create(repository).
                time(getTime(), getTimeZone()).
                log(logger).
                message(message()).
                write();
        compareCommitFiles();
    }

    public void testInitialNonEmptyCommit() throws Exception {
        toGitDirFile("packed-refs").delete();
        CommitBuilder.create(repository).
                time(getTime(), getTimeZone()).
                log(logger).
                addAll(toFiles(workDir, "a")).
                message(message()).
                write();
        compareCommitFiles();
    }

    private String message() {
        return getName() + "\n";
    }

    private void compareCommitFiles() throws Exception {
        refCommit(repository.mapCommit("HEAD"));
        compareReferenceFiles();
    }

    private void refCommit(Commit commit) throws Exception {
        refCommitLine("commit", commit.getCommitId().name());
        refCommitLine("tree", commit.getTreeId().name());
        for (ObjectId id : commit.getParentIds())
            refCommitLine("parent", id.name());
        refCommitLine("author", commit.getAuthor().toExternalString());
        refCommitLine("committer", commit.getCommitter().toExternalString());

        ref("");
        for (String line : commit.getMessage().split("\n")) {
            ref("    " + line);
        }

        if (commit.getTree().memberCount() > 0)
            ref("");
        refTree(commit.getTree());
    }

    private void refCommitLine(String name, String value) {
        ref(name + " " + value);
    }

    private void refTree(Tree tree) throws Exception {
        for (TreeEntry entry : tree.members()) {
            if (entry instanceof Tree) {
                refTree(tree);
            } else {
                refTreeEntry(entry);
            }
        }
    }

    private void refTreeEntry(TreeEntry entry) throws Exception {
        ref(String.format("%o blob %s\t%s",
            entry.getMode().getBits(), entry.getId().name(), entry.getFullName()));
    }

}