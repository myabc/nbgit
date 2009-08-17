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
package org.nbgit.ui.init;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import org.netbeans.api.project.Project;
import org.nbgit.Git;
import org.nbgit.GitProgressSupport;
import org.nbgit.OutputLogger;
import org.nbgit.task.StatusTask;
import org.nbgit.ui.ContextAction;
import org.nbgit.util.exclude.Excludes;
import org.nbgit.util.GitProjectUtils;
import org.nbgit.util.GitUtils;
import org.netbeans.api.queries.SharabilityQuery;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.GitIndex.Entry;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.treewalk.FileTreeIterator;
import org.spearce.jgit.treewalk.TreeWalk;

/**
 * Create action for Git:
 * git init - create a new repository in the given directory
 *
 * @author John Rice
 */
public class InitAction extends ContextAction {

    public InitAction(String name, VCSContext context) {
        super(name, context);
    }

    @Override
    public boolean isEnabled() {
        // If it is not a Git managed repository enable action
        File root = GitUtils.getRootFile(context);
        File[] files = context.getRootFiles().toArray(new File[context.getRootFiles().size()]);
        if (files == null || files.length == 0) {
            return false;
        }
        if (root == null) {
            return true;
        } else {
            return false;
        }
    }

    private File getCommonAncestor(File firstFile, File secondFile) {
        if (firstFile.equals(secondFile)) {
            return firstFile;
        }
        File tempFirstFile = firstFile;
        while (tempFirstFile != null) {
            File tempSecondFile = secondFile;
            while (tempSecondFile != null) {
                if (tempFirstFile.equals(tempSecondFile)) {
                    return tempSecondFile;
                }
                tempSecondFile = tempSecondFile.getParentFile();
            }
            tempFirstFile = tempFirstFile.getParentFile();
        }
        return null;
    }

    private File getCommonAncestor(File[] files) {
        File f1 = files[0];

        for (int i = 1; i < files.length; i++) {
            f1 = getCommonAncestor(f1, files[i]);
            if (f1 == null) {
                Git.LOG.log(Level.SEVERE, "Unable to get common parent of {0} and {1} ", // NOI18N
                        new Object[]{f1.getAbsolutePath(), files[i].getAbsolutePath()});
            }
        }
        return f1;
    }

    public void performAction(ActionEvent e) {
        final Git git = Git.getInstance();

        File[] files = context.getRootFiles().toArray(new File[context.getRootFiles().size()]);
        if (files == null || files.length == 0) {
            return;     // If there is a .git directory in an ancestor of any of the files in
        // the context we fail.
        }
        for (File file : files) {
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
            if (git.getTopmostManagedParent(file) != null) {
                Git.LOG.log(Level.SEVERE, "Found .git directory in ancestor of {0} ", // NOI18N
                        file);
                return;
            }
        }

        final Project proj = GitUtils.getProject(context);
        File projFile = GitUtils.getProjectFile(proj);

        if (projFile == null) {
            OutputLogger logger = OutputLogger.getLogger(Git.GIT_OUTPUT_TAB_TITLE);
            logger.outputInRed(NbBundle.getMessage(InitAction.class, "MSG_CREATE_TITLE")); // NOI18N
            logger.outputInRed(NbBundle.getMessage(InitAction.class, "MSG_CREATE_TITLE_SEP")); // NOI18N
            logger.outputInRed(
                    NbBundle.getMessage(InitAction.class, "MSG_CREATE_NOT_SUPPORTED_INVIEW_INFO")); // NOI18N
            logger.output(""); // NOI18N
            JOptionPane.showMessageDialog(null,
                    NbBundle.getMessage(InitAction.class, "MSG_CREATE_NOT_SUPPORTED_INVIEW"),// NOI18N
                    NbBundle.getMessage(InitAction.class, "MSG_CREATE_NOT_SUPPORTED_INVIEW_TITLE"),// NOI18N
                    JOptionPane.INFORMATION_MESSAGE);
            logger.closeLog();
            return;
        }
        String projName = GitProjectUtils.getProjectName(projFile);

        File rootDir = getCommonAncestor(files);
        final File root = getCommonAncestor(rootDir, projFile);
        if (root == null) {
            return;
        }
        final String prjName = projName;
        final Repository repo = git.getRepository(root);

        RequestProcessor rp = git.getRequestProcessor(root.getAbsolutePath());

        GitProgressSupport supportCreate = new GitProgressSupport() {

            public void perform() {
                try {
                    OutputLogger logger = getLogger();
                    logger.outputInRed(
                            NbBundle.getMessage(InitAction.class,
                            "MSG_CREATE_TITLE")); // NOI18N
                    logger.outputInRed(
                            NbBundle.getMessage(InitAction.class,
                            "MSG_CREATE_TITLE_SEP")); // NOI18N
                    logger.output(
                            NbBundle.getMessage(InitAction.class,
                            "MSG_CREATE_INIT", prjName, root)); // NOI18N

                    repo.create();
                } catch (IOException ex) {
                    NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
                    DialogDisplayer.getDefault().notifyLater(e);
                }
            }
        };
        supportCreate.start(rp, root.getAbsolutePath(),
                org.openide.util.NbBundle.getMessage(InitAction.class, "MSG_Create_Progress")); // NOI18N

        GitProgressSupport supportAdd = new GitProgressSupport() {

            public void perform() {
                OutputLogger logger = getLogger();
                try {
                    GitIndex index = repo.getIndex();
                    int newFiles = 0;

                    for (File file : getFileList(repo, root)) {
                        Entry entry = index.add(root, file);

                        entry.setAssumeValid(false);
                        newFiles++;

                        if (newFiles < OutputLogger.MAX_LINES_TO_PRINT) {
                            logger.output("\t" + file.getAbsolutePath()); // NOI18N
                        }
                    }

                    logger.output(
                            NbBundle.getMessage(InitAction.class,
                            "MSG_CREATE_ADD", newFiles, root.getAbsolutePath())); // NOI18N

                    if (newFiles > 0) {
                        index.write();
                    }
                    logger.output(""); // NOI18N
                    logger.outputInRed(NbBundle.getMessage(InitAction.class, "MSG_CREATE_DONE_WARNING")); // NOI18N
                } catch (IOException ex) {
                    NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
                    DialogDisplayer.getDefault().notifyLater(e);
                } finally {
                    logger.outputInRed(NbBundle.getMessage(InitAction.class, "MSG_CREATE_DONE")); // NOI18N
                    logger.output(""); // NOI18N
                }
            }
        };

        supportAdd.start(rp, root.getAbsolutePath(),
                org.openide.util.NbBundle.getMessage(InitAction.class, "MSG_Create_Add_Progress")); // NOI18N

        GitProgressSupport supportStatus = new StatusTask(context) {

            @Override
            public void performAfter() {
                git.versionedFilesChanged();
                git.refreshAllAnnotations();
            }

        };

        supportStatus.start(rp, root.getAbsolutePath(),
                NbBundle.getMessage(InitAction.class, "MSG_Create_Status_Progress")); // NOI18N

    }

    private List<File> getFileList(Repository repo, File rootFile) throws IOException {
        final FileTreeIterator workTree = new FileTreeIterator(rootFile);
        final TreeWalk walk = new TreeWalk(repo);
        final List<File> files = new ArrayList<File>();
        int share = SharabilityQuery.getSharability(rootFile);

        if (share == SharabilityQuery.NOT_SHARABLE) {
            return files;
        }
        walk.reset(); // drop the first empty tree, which we do not need here
        walk.setRecursive(true);
        walk.addTree(workTree);

        while (walk.next()) {
            String path = walk.getPathString();
            File file = new File(rootFile, path);

            if (share == SharabilityQuery.MIXED &&
                    !Excludes.isSharable(file)) {
                continue;
            }
            files.add(file);
        }

        return files;
    }
}
