/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
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
package org.nbgit.util;

import org.nbgit.OutputLogger;
import org.nbgit.StatusInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.nbgit.Git;
import org.nbgit.ui.log.RepositoryRevision;
import org.netbeans.api.queries.SharabilityQuery;
import org.openide.util.Exceptions;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.GitIndex.Entry;
import org.spearce.jgit.lib.IndexDiff;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.ObjectLoader;
import org.spearce.jgit.lib.ObjectWriter;
import org.spearce.jgit.lib.PersonIdent;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.TreeEntry;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevWalk;
import org.spearce.jgit.revwalk.filter.RevFilter;
import org.spearce.jgit.treewalk.FileTreeIterator;
import org.spearce.jgit.treewalk.TreeWalk;
import org.spearce.jgit.treewalk.filter.PathFilter;
import org.spearce.jgit.treewalk.filter.PathFilterGroup;

/**
 *
 */
public class GitCommand {

    public static void deleteConflictFile(String absolutePath) {
        // boolean success = (new File(path + GIT_STR_CONFLICT_EXT)).delete();
        throw new UnsupportedOperationException("Not yet implemented");
    }

    static public void doAdd(File root, List<File> addCandidates, OutputLogger logger) {
        try {
            Repository repo = Git.getInstance().getRepository(root);
            GitIndex index = repo.getIndex();

            for (File dstFile : addCandidates) {
                Entry entry = index.add(root, dstFile);
                entry.setAssumeValid(false);
            }

            index.write();

        } catch (Exception ex) {
            logger.output(ex.getMessage());
        }
    }

    public static void doAdd(File root, File dstFile, OutputLogger logger) {
        try {
            Repository repo = Git.getInstance().getRepository(root);
            GitIndex index = repo.getIndex();
            Entry entry = index.add(root, dstFile);

            entry.setAssumeValid(false);
            index.write();

        } catch (Exception ex) {
            logger.output(ex.getMessage());
        }
    }

    public static void doCat(File root, File base, File tempFile, String revision) {
        OutputLogger logger = OutputLogger.getLogger(root.getAbsolutePath());
        try {
            Repository repo = Git.getInstance().getRepository(root);
            Commit commit = repo.mapCommit(revision);

            if (commit == null) {
                logger.output("Revision " + revision + " not found");
                return;
            }

            String relative = toGitPath(getRelative(root, base));

            ObjectId[] ids = {commit.getTree().getId()};
            TreeWalk walk = TreeWalk.forPath(repo, relative, ids);

            ObjectId blobId = walk.getObjectId(0);

            ObjectLoader blob = repo.openBlob(blobId);
            if (blob == null) {
                logger.output("Failed to open blob " + blobId);
                return;
            }

            FileOutputStream out = new FileOutputStream(tempFile);
            out.write(blob.getCachedBytes());

            out.close();

        } catch (Exception ex) {
            // FIXME: Do unload, delete, ... here
            logger.output(ex.getMessage());
        }
    }

    public static void doRevert(File root, List<File> files, String revStr, boolean doBackup, OutputLogger logger) {
        Repository repo = Git.getInstance().getRepository(root);

        try {
            GitIndex index = new GitIndex(repo);
            Tree tree = repo.mapTree(revStr);
            index.readTree(tree);

            for (File file : files) {
                String path = getRelative(root, file);
                Entry entry = index.getEntry(path);

                if (entry == null) {
                    continue;
                }
                if (doBackup) {
                    file.renameTo(new File(file.getAbsolutePath() + ".orig"));
                }
                index.checkoutEntry(root, entry);
            }

        } catch (IOException ex) {
        }
    }

    public static List<String> doUpdateAll(File root, boolean doForcedUpdate, String revStr) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static RepositoryRevision.Walk getLogMessages(String rootPath, Set<File> files, String fromRevision, String toRevision, boolean showMerges, OutputLogger logger) {
        File root = new File(rootPath);
        Repository repo = Git.getInstance().getRepository(root);
        RepositoryRevision.Walk walk = new RepositoryRevision.Walk(repo);

        try {
            if (fromRevision == null) {
                fromRevision = Constants.HEAD;
            }
            ObjectId from = repo.resolve(fromRevision);
            if (from == null) {
                return null;
            }
            walk.markStart(walk.parseCommit(from));
            ObjectId to = toRevision != null ? repo.resolve(toRevision) : null;
            if (to != null) {
                walk.markUninteresting(walk.parseCommit(to));
            }
            List<PathFilter> paths = new ArrayList<PathFilter>();
            for (File file : files) {
                String path = getRelative(root, file);

                if (!(path.length() == 0)) {
                    paths.add(PathFilter.create(path));
                }
            }

            if (!paths.isEmpty()) {
                walk.setTreeFilter(PathFilterGroup.create(paths));
            }
            if (!showMerges) {
                walk.setRevFilter(RevFilter.NO_MERGES);
            }
        } catch (IOException ioe) {
            return null;
        }

        return walk;
    }

    public static boolean isNoUpdates(String get) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static String buildReflogMessage(String commitMessage) {
        String firstLine = commitMessage;
        int newlineIndex = commitMessage.indexOf("\n");

        if (newlineIndex > 0) {
            firstLine = commitMessage.substring(0, newlineIndex);
        }
        return "\tcommit: " + firstLine;
    }

    private static void prepareTrees(File root, List<File> selectedItems,
            HashMap<Repository, Tree> treeMap)
            throws IOException, UnsupportedEncodingException {
        Repository repo = Git.getInstance().getRepository(root);

        for (File file : selectedItems) {
            Tree projTree = treeMap.get(repo);
            if (projTree == null) {
                projTree = repo.mapTree("HEAD");
                if (projTree == null) {
                    projTree = new Tree(repo);
                }
                treeMap.put(repo, projTree);
                System.out.println("Orig tree id: " + projTree.getId());
            }

            GitIndex index = repo.getIndex();
            String repoRelativePath = getRelative(root, file);
            String string = repoRelativePath;

            TreeEntry treeMember = projTree.findBlobMember(repoRelativePath);
            // we always want to delete it from the current tree, since if it's
            // updated, we'll add it again
            if (treeMember != null) {
                treeMember.delete();
            }
            Entry idxEntry = index.getEntry(string);
            if (true /* if modified on disk or missing */) {
                if (!file.isFile()) {
                    index.remove(root, file);
                    index.write();
                    System.out.println("Phantom file, so removing from index");
                    continue;
                }

                if (idxEntry.update(file)) {
                    index.write();
                }
            }

            if (idxEntry != null) {
                projTree.addFile(repoRelativePath);
                TreeEntry newMember = projTree.findBlobMember(repoRelativePath);

                newMember.setId(idxEntry.getObjectId());
                System.out.println("New member id for " + repoRelativePath + ": " + newMember.getId() + " idx id: " + idxEntry.getObjectId());
            }
        }
    }

    private static void writeTreeWithSubTrees(Tree tree) throws IOException {
        if (tree.getId() == null) {
            System.out.println("writing tree for: " + tree.getFullName());

            for (TreeEntry entry : tree.members()) {
                if (entry.isModified()) {
                    if (entry instanceof Tree) {
                        writeTreeWithSubTrees((Tree) entry);
                    } else {
                        System.out.println("BAD JUJU: " + entry.getFullName());
                    }
                }
            }
            ObjectWriter writer = new ObjectWriter(tree.getRepository());
            tree.setId(writer.writeTree(tree));
        }
    }

    public static void doCommit(File root, List<File> commitCandidates, String message, OutputLogger logger) throws IOException {
        HashMap<Repository, Tree> treeMap = new HashMap<Repository, Tree>();

        prepareTrees(root, commitCandidates, treeMap);

        for (java.util.Map.Entry<Repository, Tree> entry : treeMap.entrySet()) {

            Tree tree = entry.getValue();
            Repository repo = tree.getRepository();
            PersonIdent personIdent = new PersonIdent(repo);

            writeTreeWithSubTrees(tree);

            final RefUpdate ru = repo.updateRef(Constants.HEAD);
            ObjectId[] parentIds;
            if (ru.getOldObjectId() != null) {
                parentIds = new ObjectId[]{ru.getOldObjectId()};
            } else {
                parentIds = new ObjectId[0];
            }
            Commit commit = new Commit(repo, parentIds);
            commit.setTree(tree);
            message = message.replaceAll("\r", "\n");

            commit.setMessage(message);
            commit.setAuthor(personIdent);
            commit.setCommitter(personIdent);

            ObjectWriter writer = new ObjectWriter(repo);
            commit.setCommitId(writer.writeCommit(commit));

            ru.setNewObjectId(commit.getCommitId());
            ru.setRefLogMessage(buildReflogMessage(message), false);
            ru.update();
            boolean ok;
            if (ru.getOldObjectId() != null) {
                ok = ru.getResult() == RefUpdate.Result.FAST_FORWARD;
            } else {
                ok = ru.getResult() == RefUpdate.Result.NEW;
            }
            if (!ok) {
                logger.output("Failed to update " + ru.getName() + " to commit " + commit.getCommitId() + ".");
            }
        }
    }

    public static void doRemove(File root, List<File> deleteCandidates, OutputLogger logger) {
        try {
            Repository repo = Git.getInstance().getRepository(root);
            GitIndex index = repo.getIndex();
            boolean dirty = false;

            for (File srcFile : deleteCandidates) {
                if (index.remove(root, srcFile)) {
                    dirty = true;
                }
            }

            if (dirty) {
                index.write();
            }
        } catch (Exception ex) {
            logger.output(ex.getMessage());
        }

    }

    public static void doRemove(File root, File srcFile, OutputLogger logger) {
        try {
            Repository repo = Git.getInstance().getRepository(root);
            GitIndex index = repo.getIndex();

            if (index.remove(root, srcFile)) {
                index.write();
            }
        } catch (Exception ex) {
            logger.output(ex.getMessage());
        }

    }

    public static void doRenameAfter(File root, File srcFile, File dstFile, OutputLogger logger) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static List<String[]> getRevisions(File root, int limit) {
        return getRevisionsForFile(root, null, limit);
    }

    public static List<String[]> getRevisionsForFile(File root, File[] files, int limit) {
        Repository repo = Git.getInstance().getRepository(root);
        RevWalk walk = new RevWalk(repo);
        List<String[]> revs = new ArrayList<String[]>();

        try {
            ObjectId from = repo.resolve(Constants.HEAD);
            if (from == null) {
                return null;
            }
            walk.markStart(walk.parseCommit(from));

            if (files != null) {
                List<PathFilter> paths = new ArrayList<PathFilter>();
                for (File file : files) {
                    String path = getRelative(root, file);

                    if (!(path.length() == 0)) {
                        paths.add(PathFilter.create(path));
                    }
                }

                if (!paths.isEmpty()) {
                    walk.setTreeFilter(PathFilterGroup.create(paths));
                }
            }

            for (RevCommit rev : walk) {
                revs.add(new String[]{rev.getShortMessage(), rev.getId().name()});
                if (--limit <= 0) {
                    break;
                }
            }

        } catch (IOException ioe) {
        }

        return revs;
    }

    private static String getRelative(File root, File dir) {
        return getRelative(root.getAbsolutePath(), dir.getAbsolutePath());
    }

    private static String getRelative(String root, String dir) {
        if (dir.equals(root)) {
            return "";
        }
        return dir.replace(root + File.separator, "");
    }

    private static String toGitPath(String path) {
        return File.separatorChar == '/'
                ? path : path.replace(File.separator, "/");
    }

    private static void put(Set<String> set, String relPath,
            Map<File, StatusInfo> files, File root, int status) {
        for (String path : set) {
            if (relPath.length() > 0 && !path.startsWith(relPath)) {
                continue;
            }
            File file = new File(root, path);
            files.put(file, new StatusInfo(status, null, false));
        }

    }

    /*
     * m odified
     * a dded
     * r emoved
     * d eleted
     * u nknown
     * C opies
     *
     * i gnored
     * c lean
     */
    public static Map<File, StatusInfo> getAllStatus(File root, File dir) throws IOException {
        String relPath = getRelative(root, dir);

        Repository repo = Git.getInstance().getRepository(root);
        Map<File, StatusInfo> files = new HashMap<File, StatusInfo>();

        try {
            repo.refreshFromDisk();
            IndexDiff index = new IndexDiff(repo);
            index.diff();

            put(index.getAdded(), relPath, files, root,
                    StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY);
            put(index.getRemoved(), relPath, files, root,
                    StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY);
            put(index.getMissing(), relPath, files, root,
                    StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY);
            put(index.getChanged(), relPath, files, root,
                    StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY);
            put(index.getModified(), relPath, files, root,
                    StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY);

            final FileTreeIterator workTree = new FileTreeIterator(repo.getWorkDir());
            final TreeWalk walk = new TreeWalk(repo);

            walk.reset(); // drop the first empty tree
            walk.setRecursive(true);
            walk.addTree(workTree);

            int share = SharabilityQuery.getSharability(dir);
            if (share == SharabilityQuery.NOT_SHARABLE) {
                return files;
            }
            while (walk.next()) {
                String path = walk.getPathString();

                if (relPath.length() > 0 && !path.startsWith(relPath)) {
                    continue;
                }
                if (index.getAdded().contains(path) ||
                        index.getRemoved().contains(path) ||
                        index.getMissing().contains(path) ||
                        index.getChanged().contains(path) ||
                        index.getModified().contains(path)) {
                    continue;
                }
                Entry entry = repo.getIndex().getEntry(path);
                File file = new File(root, path);

                int status;
                if (entry != null) {
                    status = StatusInfo.STATUS_VERSIONED_UPTODATE;
                } else {
                    if (share == SharabilityQuery.MIXED &&
                            SharabilityQuery.getSharability(file) == SharabilityQuery.NOT_SHARABLE) {
                        continue;
                    }
                    status = StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY;
                }

                files.put(file, new StatusInfo(status, null, false));
            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return files;
    }

    /*
     * m odified
     * a dded
     * r emoved
     * d eleted
     * u nknown
     * C opies
     */
    public static Map<File, StatusInfo> getInterestingStatus(File root, File dir) {
        String relPath = getRelative(root, dir);

        Repository repo = Git.getInstance().getRepository(root);
        IndexDiff index;

        Map<File, StatusInfo> files = new HashMap<File, StatusInfo>();

        try {
            repo.refreshFromDisk();
            index = new IndexDiff(repo);
            index.diff();

            put(index.getAdded(), relPath, files, root,
                    StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY);
            put(index.getRemoved(), relPath, files, root,
                    StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY);
            put(index.getMissing(), relPath, files, root,
                    StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY);
            put(index.getChanged(), relPath, files, root,
                    StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY);
            put(index.getModified(), relPath, files, root,
                    StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY);

            final FileTreeIterator workTree = new FileTreeIterator(repo.getWorkDir());
            final TreeWalk walk = new TreeWalk(repo);

            walk.reset(); // drop the first empty tree
            walk.setRecursive(true);
            walk.addTree(workTree);

            while (walk.next()) {
                String path = walk.getPathString();

                if (relPath.length() > 0 && !path.startsWith(relPath)) {
                    continue;
                }
                if (index.getAdded().contains(path) ||
                        index.getRemoved().contains(path) ||
                        index.getMissing().contains(path) ||
                        index.getChanged().contains(path) ||
                        index.getModified().contains(path)) {
                    continue;
                }
                Entry entry = repo.getIndex().getEntry(path);
                if (entry != null) {
                    continue;
                }
                int status = StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY;
                File file = new File(root, path);
                files.put(file, new StatusInfo(status, null, false));
            }

        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return files;
    }

    public static StatusInfo getSingleStatus(File root, File file) {
        Repository repo = Git.getInstance().getRepository(root);
        IndexDiff index;

        int share = SharabilityQuery.getSharability(file.getParentFile());
        if (share == SharabilityQuery.NOT_SHARABLE ||
                (share == SharabilityQuery.MIXED &&
                SharabilityQuery.getSharability(file) == SharabilityQuery.NOT_SHARABLE)) {
            return new StatusInfo(StatusInfo.STATUS_NOTVERSIONED_EXCLUDED, null, false);
        }
        int status = StatusInfo.STATUS_UNKNOWN;
        String name = getRelative(root, file);

        try {
            repo.refreshFromDisk();
            index = new IndexDiff(repo);
            index.diff();
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return new StatusInfo(status, null, false);
        }

        if (index.getAdded().contains(name)) {
            status = StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY;
        } else if (index.getRemoved().contains(name)) {
            status = StatusInfo.STATUS_VERSIONED_REMOVEDLOCALLY;
        } else if (index.getMissing().contains(name)) {
            status = StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY;
        } else if (index.getChanged().contains(name)) {
            status = StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY;
        } else if (index.getModified().contains(name)) {
            status = StatusInfo.STATUS_VERSIONED_MODIFIEDLOCALLY;
        } else {
            status = StatusInfo.STATUS_VERSIONED_UPTODATE;
        }
        StatusInfo info = new StatusInfo(status, null, false);

        return info;
    }
}
