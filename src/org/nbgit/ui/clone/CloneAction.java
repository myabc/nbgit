/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2009 Sun Microsystems, Inc. All rights reserved.
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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2009 Sun
 * Microsystems, Inc. All Rights Reserved.
 * Portions Copyright 2009 Alexander Coles (Ikonoklastik Productions).
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
package org.nbgit.ui.clone;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.SwingUtilities;
import org.nbgit.Git;
import org.nbgit.GitModuleConfig;
import org.nbgit.GitProgressMonitor;
import org.nbgit.GitProgressSupport;
import org.nbgit.OutputLogger;
import org.nbgit.ui.ContextAction;
import org.nbgit.util.GitProjectUtils;
import org.nbgit.util.GitUtils;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.spearce.jgit.errors.NotSupportedException;
import org.spearce.jgit.errors.TransportException;
import org.spearce.jgit.lib.Commit;
import org.spearce.jgit.lib.Constants;
import org.spearce.jgit.lib.GitIndex;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.RefUpdate;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryConfig;
import org.spearce.jgit.lib.Tree;
import org.spearce.jgit.lib.WorkDirCheckout;
import org.spearce.jgit.transport.FetchResult;
import org.spearce.jgit.transport.RefSpec;
import org.spearce.jgit.transport.RemoteConfig;
import org.spearce.jgit.transport.Transport;
import org.spearce.jgit.transport.URIish;

public class CloneAction extends ContextAction {

    public CloneAction(String name, VCSContext context) {
        super(name, context);
    }

    @Override
    protected void performAction(ActionEvent event) {
        final File root = GitUtils.getRootFile(context);
        if (root == null) return;

        // Get unused Clone Folder name
        File tmp = root.getParentFile();
        File projFile = GitUtils.getProjectFile(context);
        String folderName = root.getName();
        Boolean projIsRepos = true;
        if (!root.equals(projFile)) {
            // Git Repository is not the same as project root
            projIsRepos = false;
        }

        for (int i = 0; i < 10000; i++) {
            if (!new File(tmp, folderName + "_clone" + i).exists()) { // NOI18N
                tmp = new File(tmp, folderName + "_clone" + i); // NOI18N
                break;
            }
        }
        Clone clone = new Clone(root, tmp);
        if (!clone.showDialog()) {
            return;
        }

        URIish source = null;
        try {
            source = new URIish(root.toURL());
        } catch (MalformedURLException ex) {
            Exceptions.printStackTrace(ex);
        }

        performClone(source, clone.getTargetDir(), projIsRepos, projFile, true, true);
    }

    public static RequestProcessor.Task performClone(final URIish source, final File target, boolean projIsRepos,
            File projFile, boolean scanForProjects) {
        return performClone(source, target, projIsRepos, projFile, false, scanForProjects);
    }

    private static RequestProcessor.Task performClone(
            final URIish source,
            final File target,
            final Boolean projIsRepos,
            final File projFile,
            final boolean isLocalClone,
            final boolean scanForProjects) {

        RequestProcessor rp = Git.getInstance().getRequestProcessor(source.toString());
        final GitProgressSupport support = new GitProgressSupport() {

            Repository repo = Git.getInstance().getRepository(target);

            @Override
            protected void perform() {
                String projName = (projFile != null)
                                  ? GitProjectUtils.getProjectName(projFile)
                                  : null;
                OutputLogger logger = getLogger();
                try {

                    if (projName != null) {
                        logger.outputInRed(
                                NbBundle.getMessage(CloneAction.class,
                                "MSG_CLONE_FROM", projName, source)); // NOI18N
                        logger.outputInRed(
                                NbBundle.getMessage(CloneAction.class,
                                "MSG_CLONE_TO", projName, target)); // NOI18N
                    } else {
                        logger.outputInRed(
                                NbBundle.getMessage(CloneAction.class,
                                "MSG_EXTERNAL_CLONE_FROM", source)); // NOI18N
                        logger.outputInRed(
                                NbBundle.getMessage(CloneAction.class,
                                "MSG_EXTERNAL_CLONE_TO", target)); // NOI18N
                    }
                    logger.output(""); // NOI18N

                    doInit(repo, source, logger);
                    FetchResult r = doFetch(repo, logger);
                    Ref branch = r.getAdvertisedRef(Constants.HEAD);
                    if (branch == null) {
                        this.cancel();
                    }
                    doCheckout(repo, branch, logger);

                    if (isLocalClone) {
                        Git git = Git.getInstance();
                        ProjectManager projectManager = ProjectManager.getDefault();
                        File normalizedCloneFolder = FileUtil.normalizeFile(target);
                        File cloneProjFile;
                        if (!projIsRepos) {
                            String name = (projFile != null)
                                    ? projFile.getAbsolutePath().substring(source.getPath().length() + 1)
                                    : target.getAbsolutePath();
                            cloneProjFile = new File(normalizedCloneFolder, name);
                        } else {
                            cloneProjFile = normalizedCloneFolder;
                        }
                        openProject(cloneProjFile, projectManager, git);
                    } else if (scanForProjects) {
                        CloneCompleted cc = new CloneCompleted(target);
                        if (isCanceled()) {
                            return;
                        }
                        cc.scanForProjects(this);
                    }

                } catch (URISyntaxException usex) {
                    NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(usex);
                    DialogDisplayer.getDefault().notifyLater(e);
                } catch (IOException ex) {
                    NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
                    DialogDisplayer.getDefault().notifyLater(e);
                } finally {
                    if (!isLocalClone) {
                        logger.outputInRed(NbBundle.getMessage(CloneAction.class, "MSG_CLONE_DONE")); // NOI18N
                        logger.output(""); // NOI18N
                    }
                }
            }

            private void openProject(final File cloneProjFile, final ProjectManager projectManager, final Git git) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        // Open and set focus on the cloned project if possible
                        OutputLogger logger = getLogger();
                        try {
                            FileObject cloneProj = FileUtil.toFileObject(cloneProjFile);
                            Project proj = null;
                            if (cloneProjFile != null && cloneProj != null) {
                                proj = projectManager.findProject(cloneProj);
                            }
                            if (proj != null) {
                                GitProjectUtils.openProject(proj, this, false);
                                                      // TODO: GitModuleConfig.getDefault().getSetMainProject()
                                git.versionedFilesChanged();
                                git.refreshAllAnnotations();
                            } else {
                                logger.outputInRed(NbBundle.getMessage(CloneAction.class, "MSG_EXTERNAL_CLONE_PRJ_NOT_FOUND_CANT_SETASMAIN")); // NOI18N
                            }
                        } catch (IOException ioe) {
                            NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ioe);
                            DialogDisplayer.getDefault().notifyLater(e);
                        } finally {
                            logger.outputInRed(NbBundle.getMessage(CloneAction.class, "MSG_CLONE_DONE")); // NOI18N
                            logger.output("");  // NOI18N
                        }
                    }
                });
            }
        };

        //support.setRepositoryRoot(source);

        return support.start(rp, source.toString(), org.openide.util.NbBundle.getMessage(CloneAction.class, "LBL_Clone_Progress", source)); // NO
    }


    public static void doInit(Repository repo, URIish uri, OutputLogger logger) throws IOException, URISyntaxException {
        repo.create();

        repo.getConfig().setBoolean("core", null, "bare", false);
        repo.getConfig().save();

        logger.output("Initialized empty Git repository in " + repo.getWorkDir().getAbsolutePath());
        logger.flushLog();

        // save remote
        final RemoteConfig rc = new RemoteConfig(repo.getConfig(), "origin");
        rc.addURI(uri);
        rc.addFetchRefSpec(new RefSpec().setForceUpdate(true).setSourceDestination(Constants.R_HEADS + "*",
                Constants.R_REMOTES + "origin" + "/*"));
        rc.update(repo.getConfig());
        repo.getConfig().save();
    }

    public static FetchResult doFetch(Repository repo, OutputLogger logger) throws NotSupportedException, TransportException, URISyntaxException {
        final FetchResult r;
        final Transport tn = Transport.open(repo, "origin");
        try {
            r = tn.fetch(new GitProgressMonitor(), null);
        } finally {
            tn.close();
        }
        logger.output("--- Fetch Completed ---");
        return r;
    }

    public static void doCheckout(Repository repo, Ref branch, OutputLogger logger) throws IOException {

        final GitIndex index = new GitIndex(repo);
        final Commit mapCommit = repo.mapCommit(branch.getObjectId());
        final Tree tree = mapCommit.getTree();
        final RefUpdate u;
        final WorkDirCheckout co;

        u = repo.updateRef(Constants.HEAD);
        u.setNewObjectId(mapCommit.getCommitId());
        u.forceUpdate();

        // checking out files
        co = new WorkDirCheckout(repo, repo.getWorkDir(), index, tree);
        co.checkout();
        // writing index
        index.write();
    }

}
