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
 * Portions Copyright 2008 Alexander Coles (Ikonoklastik Productions).
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
package org.nbgit;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import javax.swing.SwingUtilities;
import org.nbgit.util.GitCommand;
import org.nbgit.util.exclude.Excludes;
import org.nbgit.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSInterceptor;
import org.netbeans.modules.versioning.util.Utils;
import org.openide.util.RequestProcessor;

/**
 * Listens on file system changes and reacts appropriately, mainly refreshing affected files' status.
 *
 * @author Maros Sandor
 */
public class GitInterceptor extends VCSInterceptor {

    private final StatusCache cache;
    private ConcurrentHashMap<File, File> dirsToDelete = new ConcurrentHashMap<File, File>();
    private ConcurrentLinkedQueue<File> filesToRefresh = new ConcurrentLinkedQueue<File>();
    private RequestProcessor.Task refreshTask;
    private static final RequestProcessor refresh = new RequestProcessor("GitRefresh", 1, true);

    public GitInterceptor() {
        cache = Git.getInstance().getStatusCache();
        refreshTask = refresh.create(new RefreshTask());
    }

    @Override
    public boolean beforeDelete(File file) {
        if (file == null) {
            return true;
        }
        if (GitUtils.isPartOfGitMetadata(file)) {
            return false;        // We track the deletion of top level directories
        }
        if (file.isDirectory()) {
            for (File dir : dirsToDelete.keySet()) {
                if (file.equals(dir.getParentFile())) {
                    dirsToDelete.remove(dir);
                }
            }
            if (Excludes.isSharable(file)) {
                dirsToDelete.put(file, file);
            }
        }
        return true;
    }

    @Override
    public void doDelete(File file) throws IOException {
    }

    @Override
    public void afterDelete(final File file) {
        Utils.post(new Runnable() {

            public void run() {
                fileDeletedImpl(file);
            }
        });
    }

    private void fileDeletedImpl(final File file) {
        if (file == null || !file.exists()) {
            return;
        }
        Git git = Git.getInstance();
        final File root = git.getTopmostManagedParent(file);
        RequestProcessor rp = null;
        if (root != null) {
            rp = git.getRequestProcessor(root.getAbsolutePath());
        }
        if (file.isDirectory()) {
            file.delete();
            if (!dirsToDelete.remove(file, file)) {
                return;
            }
            if (root == null) {
                return;
            }
            GitProgressSupport support = new GitProgressSupport() {

                public void perform() {
                    GitCommand.doRemove(root, file, this.getLogger());
                    // We need to cache the status of all deleted files
                    Map<File, StatusInfo> interestingFiles = GitCommand.getInterestingStatus(root, file);
                    if (!interestingFiles.isEmpty()) {
                        Collection<File> files = interestingFiles.keySet();

                        Map<File, Map<File, StatusInfo>> interestingDirs =
                                GitUtils.getInterestingDirs(interestingFiles, files);

                        for (File tmpFile : files) {
                            if (this.isCanceled()) {
                                return;
                            }
                            StatusInfo fi = interestingFiles.get(tmpFile);

                            cache.refreshFileStatus(tmpFile, fi,
                                    interestingDirs.get(tmpFile.isDirectory() ? tmpFile : tmpFile.getParentFile()), true);
                        }
                    }
                }
            };

            support.start(rp, root.getAbsolutePath(),
                    org.openide.util.NbBundle.getMessage(GitInterceptor.class, "MSG_Remove_Progress")); // NOI18N
        } else {
            // If we are deleting a parent directory of this file
            // skip the call to git remove as we will do it for the directory
            file.delete();
            if (root == null) {
                return;
            }
            for (File dir : dirsToDelete.keySet()) {
                File tmpFile = file.getParentFile();
                while (tmpFile != null) {
                    if (tmpFile.equals(dir)) {
                        return;
                    }
                    tmpFile = tmpFile.getParentFile();
                }
            }
            GitProgressSupport support = new GitProgressSupport() {

                public void perform() {
                    GitCommand.doRemove(root, file, this.getLogger());
                    cache.refresh(file, StatusCache.REPOSITORY_STATUS_UNKNOWN);
                }
            };
            support.start(rp, root.getAbsolutePath(),
                    org.openide.util.NbBundle.getMessage(GitInterceptor.class, "MSG_Remove_Progress")); // NOI18N
        }
    }

    @Override
    public boolean beforeMove(File from, File to) {
        if (from == null || to == null || to.exists()) {
            return true;
        }
        Git git = Git.getInstance();
        if (git.isManaged(from)) {
            return git.isManaged(to);
        }
        return super.beforeMove(from, to);
    }

    @Override
    public void doMove(final File from, final File to) throws IOException {
        if (from == null || to == null || to.exists()) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {

            Git.LOG.log(Level.INFO, "Warning: launching external process in AWT", new Exception().fillInStackTrace()); // NOI18N
            final Throwable innerT[] = new Throwable[1];
            Runnable outOfAwt = new Runnable() {

                public void run() {
                    try {
                        gitMoveImplementation(from, to);
                    } catch (Throwable t) {
                        innerT[0] = t;
                    }
                }
            };

            Git.getInstance().getRequestProcessor().post(outOfAwt).waitFinished();
            if (innerT[0] != null) {
                if (innerT[0] instanceof IOException) {
                    throw (IOException) innerT[0];
                } else if (innerT[0] instanceof RuntimeException) {
                    throw (RuntimeException) innerT[0];
                } else if (innerT[0] instanceof Error) {
                    throw (Error) innerT[0];
                } else {
                    throw new IllegalStateException("Unexpected exception class: " + innerT[0]);                // end of hack
                }
            }
        } else {
            gitMoveImplementation(from, to);
        }
    }

    private void gitMoveImplementation(final File srcFile, final File dstFile) throws IOException {
        final Git git = Git.getInstance();
        final File root = git.getTopmostManagedParent(srcFile);
        if (root == null) {
            return;
        }
        RequestProcessor rp = git.getRequestProcessor(root.getAbsolutePath());

        Git.LOG.log(Level.FINE, "gitMoveImplementation(): File: {0} {1}", new Object[]{srcFile, dstFile}); // NOI18N

        srcFile.renameTo(dstFile);
        Runnable moveImpl = new Runnable() {

            public void run() {
                OutputLogger logger = OutputLogger.getLogger(root.getAbsolutePath());
                try {
                    if (dstFile.isDirectory()) {
                        GitCommand.doRenameAfter(root, srcFile, dstFile, logger);
                        return;
                    }
                    int status = GitCommand.getSingleStatus(root, srcFile).getStatus();
                    Git.LOG.log(Level.FINE, "gitMoveImplementation(): Status: {0} {1}", new Object[]{srcFile, status}); // NOI18N
                    if (status == StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY ||
                            status == StatusInfo.STATUS_NOTVERSIONED_EXCLUDED) {
                    } else if (status == StatusInfo.STATUS_VERSIONED_ADDEDLOCALLY) {
                        GitCommand.doRemove(root, srcFile, logger);
                        GitCommand.doAdd(root, dstFile, logger);
                    } else {
                        GitCommand.doRenameAfter(root, srcFile, dstFile, logger);
                    }
                } catch (Exception e) {
                    Git.LOG.log(Level.FINE, "Git failed to rename: File: {0} {1}", new Object[]{srcFile.getAbsolutePath(), dstFile.getAbsolutePath()}); // NOI18N
                } finally {
                    logger.closeLog();
                }
            }
        };

        rp.post(moveImpl);
    }

    @Override
    public void afterMove(final File from, final File to) {
        Utils.post(new Runnable() {

            public void run() {
                fileMovedImpl(from, to);
            }
        });
    }

    private void fileMovedImpl(final File from, final File to) {
        if (from == null || to == null || !to.exists()) {
            return;
        }
        if (to.isDirectory()) {
            return;
        }
        Git git = Git.getInstance();
        final File root = git.getTopmostManagedParent(from);
        if (root == null) {
            return;
        }
        RequestProcessor rp = git.getRequestProcessor(root.getAbsolutePath());

        GitProgressSupport supportCreate = new GitProgressSupport() {

            public void perform() {
                cache.refresh(from, StatusCache.REPOSITORY_STATUS_UNKNOWN);
                cache.refresh(to, StatusCache.REPOSITORY_STATUS_UNKNOWN);
            }
        };

        supportCreate.start(rp, root.getAbsolutePath(),
                org.openide.util.NbBundle.getMessage(GitInterceptor.class, "MSG_Move_Progress")); // NOI18N
    }

    @Override
    public boolean beforeCreate(File file, boolean isDirectory) {
        return super.beforeCreate(file, isDirectory);
    }

    @Override
    public void doCreate(File file, boolean isDirectory) throws IOException {
        super.doCreate(file, isDirectory);
    }

    @Override
    public void afterCreate(final File file) {
        Utils.post(new Runnable() {

            public void run() {
                fileCreatedImpl(file);
            }
        });
    }

    private void fileCreatedImpl(final File file) {
        if (file.isDirectory()) {
            return;
        }
        Git git = Git.getInstance();
        final File root = git.getTopmostManagedParent(file);
        if (root == null) {
            return;
        }
        RequestProcessor rp = git.getRequestProcessor(root.getAbsolutePath());

        GitProgressSupport supportCreate = new GitProgressSupport() {

            public void perform() {
                reScheduleRefresh(file);
            }
        };

        supportCreate.start(rp, root.getAbsolutePath(),
                org.openide.util.NbBundle.getMessage(GitInterceptor.class, "MSG_Create_Progress")); // NOI18N
    }

    @Override
    public void afterChange(final File file) {
        Utils.post(new Runnable() {

            public void run() {
                fileChangedImpl(file);
            }
        });
    }

    private void fileChangedImpl(final File file) {
        if (file.isDirectory()) {
            return;
        }
        Git git = Git.getInstance();
        final File root = git.getTopmostManagedParent(file);
        if (root == null) {
            return;
        }
        RequestProcessor rp = git.getRequestProcessor(root.getAbsolutePath());

        GitProgressSupport supportCreate = new GitProgressSupport() {

            public void perform() {
                Git.LOG.log(Level.FINE, "fileChangedImpl(): File: {0}", file); // NOI18N
                reScheduleRefresh(file);
            }
        };

        supportCreate.start(rp, root.getAbsolutePath(),
                org.openide.util.NbBundle.getMessage(GitInterceptor.class, "MSG_Change_Progress")); // NOI18N
    }

    private void reScheduleRefresh(File fileToRefresh) {
        // There is no point in refreshing the cache for ignored files.
        if (Excludes.isIgnored(fileToRefresh, false)) {
            return;
        }
        if (!filesToRefresh.contains(fileToRefresh)) {
            if (!filesToRefresh.offer(fileToRefresh)) {
                Git.LOG.log(Level.FINE, "reScheduleRefresh failed to add to filesToRefresh queue {0}", fileToRefresh);
            }
        }
        refreshTask.schedule(1000);
    }

    private class RefreshTask implements Runnable {

        public void run() {
            Thread.interrupted();
            File fileToRefresh = filesToRefresh.poll();
            if (fileToRefresh != null) {
                cache.refresh(fileToRefresh, StatusCache.REPOSITORY_STATUS_UNKNOWN);
                fileToRefresh = filesToRefresh.peek();
                if (fileToRefresh != null) {
                    refreshTask.schedule(0);
                }
            }
        }
    }
}
