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
package org.netbeans.modules.git.ui.push;


import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitException;
import org.netbeans.modules.git.GitProgressSupport;
import org.netbeans.modules.git.OutputLogger;
import org.netbeans.modules.git.ui.actions.ContextAction;
import org.netbeans.modules.git.ui.merge.MergeAction;
import org.netbeans.modules.git.ui.pull.PullAction;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitProjectUtils;
import org.netbeans.modules.git.util.GitRepositoryContextCache;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Push action for Git: 
 * git push - push changes to the default destination
 * 
 * @author John Rice
 */
public class PushAction extends ContextAction {

  private final VCSContext context;

    public PushAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }
    
    public void performAction(ActionEvent e) {
        final File root = GitUtils.getRootFile(context);
        if (root == null) {
            OutputLogger logger = OutputLogger.getLogger(Git.GIT_OUTPUT_TAB_TITLE);
            logger.outputInRed( NbBundle.getMessage(PushAction.class,"MSG_PUSH_TITLE")); // NOI18N
            logger.outputInRed( NbBundle.getMessage(PushAction.class,"MSG_PUSH_TITLE_SEP")); // NOI18N
            logger.outputInRed(
                    NbBundle.getMessage(PushAction.class, "MSG_PUSH_NOT_SUPPORTED_INVIEW_INFO")); // NOI18N
            logger.output(""); // NOI18N
            JOptionPane.showMessageDialog(null,
                    NbBundle.getMessage(PushAction.class, "MSG_PUSH_NOT_SUPPORTED_INVIEW"),// NOI18N
                    NbBundle.getMessage(PushAction.class, "MSG_PUSH_NOT_SUPPORTED_INVIEW_TITLE"),// NOI18N
                    JOptionPane.INFORMATION_MESSAGE);
            logger.closeLog();
            return;
        }

        push(context);
    }

    @Override
    public boolean isEnabled() {
        Set<File> ctxFiles = context != null? context.getRootFiles(): null;
        if(GitUtils.getRootFile(context) == null || ctxFiles == null || ctxFiles.size() == 0) 
            return false;
        return true; // #121293: Speed up menu display, warn user if not set when Push selected
    }
    
    public static void push(final VCSContext ctx){
        final File root = GitUtils.getRootFile(ctx);
        if (root == null) return;
        String repository = root.getAbsolutePath();

        RequestProcessor rp = Git.getInstance().getRequestProcessor(repository);
        GitProgressSupport support = new GitProgressSupport() {
            public void perform() { getDefaultAndPerformPush(ctx, root, this.getLogger()); } };
        support.start(rp, repository, 
                org.openide.util.NbBundle.getMessage(PushAction.class, "MSG_PUSH_PROGRESS")); // NOI18N
        
    }
                
    static void getDefaultAndPerformPush(VCSContext ctx, File root, OutputLogger logger) {
        // If the repository has no default pull path then inform user
        String tmpPushPath = GitRepositoryContextCache.getPushDefault(ctx);
        if(tmpPushPath == null) {
            tmpPushPath = GitRepositoryContextCache.getPullDefault(ctx);
        }
        if(tmpPushPath == null) {
            logger.outputInRed( NbBundle.getMessage(PushAction.class,"MSG_PUSH_TITLE")); // NOI18N
            logger.outputInRed( NbBundle.getMessage(PushAction.class,"MSG_PUSH_TITLE_SEP")); // NOI18N
            logger.output(NbBundle.getMessage(PushAction.class, "MSG_NO_DEFAULT_PUSH_SET_MSG")); // NOI18N
            logger.outputInRed(NbBundle.getMessage(PushAction.class, "MSG_PUSH_DONE")); // NOI18N
            logger.output(""); // NOI18N
            JOptionPane.showMessageDialog(null,
                NbBundle.getMessage(PushAction.class,"MSG_NO_DEFAULT_PUSH_SET"),
                NbBundle.getMessage(PushAction.class,"MSG_PUSH_TITLE"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        final String pushPath = tmpPushPath;
        final String fromPrjName = GitProjectUtils.getProjectName(root);
        final String toPrjName = GitProjectUtils.getProjectName(new File(pushPath));
        performPush(root, pushPath, fromPrjName, toPrjName, logger);

    }
    static void performPush(File root, String pushPath, String fromPrjName, String toPrjName, OutputLogger logger) {
        try {
            logger.outputInRed(NbBundle.getMessage(PushAction.class, "MSG_PUSH_TITLE")); // NOI18N
            logger.outputInRed(NbBundle.getMessage(PushAction.class, "MSG_PUSH_TITLE_SEP")); // NOI18N

            List<String> listOutgoing = GitCommand.doOutgoing(root, pushPath, logger);
            if ((listOutgoing == null) || listOutgoing.isEmpty()) {
                return;
            }

            File pushFile = new File(pushPath);
            boolean bLocalPush = (FileUtil.toFileObject(FileUtil.normalizeFile(pushFile)) != null);
            boolean bNoChanges = GitCommand.isNoChanges(listOutgoing.get(listOutgoing.size() - 1));

            if (bLocalPush) {
                // Warn user if there are local changes which Push will overwrite
                if (!bNoChanges && !PullAction.confirmWithLocalChanges(pushFile, PushAction.class,
                        "MSG_PUSH_LOCALMODS_CONFIRM_TITLE", "MSG_PUSH_LOCALMODS_CONFIRM_QUERY", listOutgoing, logger)) { // NOI18N
                    logger.outputInRed(NbBundle.getMessage(PushAction.class, "MSG_PUSH_LOCALMODS_CANCEL")); // NOI18N
                    logger.output(""); // NOI18N
                    return;
                }
            }

            List<String> list;
            if (bNoChanges) {
                list = listOutgoing;
            } else {
                list = GitCommand.doPush(root, pushPath, logger);
            }
            if (!list.isEmpty() &&
                    GitCommand.isErrorAbortPush(list.get(list.size() - 1))) {
                logger.output(list);
                logger.output("");
                GitUtils.warningDialog(PushAction.class,
                        "MSG_PUSH_ERROR_TITLE", "MSG_PUSH_ERROR_QUERY"); // NOI18N 
                logger.outputInRed(NbBundle.getMessage(PushAction.class, "MSG_PUSH_ERROR_CANCELED")); // NOI18N
                return;
            }

            if (list != null && !list.isEmpty()) {

                if (!GitCommand.isNoChanges(listOutgoing.get(listOutgoing.size() - 1))) {
                    logger.outputInRed(NbBundle.getMessage(PushAction.class, "MSG_CHANGESETS_TO_PUSH")); // NOI18N
                    for (String s : listOutgoing) {
                        if (s.indexOf(Git.CHANGESET_STR) == 0) {
                            logger.outputInRed(s);
                        } else if (!s.equals("")) { // NOI18N
                            logger.output(GitUtils.replaceHttpPassword(s));
                        }
                    }
                    logger.output(""); // NOI18N
                }

                logger.output(GitUtils.replaceHttpPassword(list));

                if (toPrjName == null) { 
                    logger.outputInRed(
                            NbBundle.getMessage(PushAction.class,
                            "MSG_PUSH_TO_NONAME", bLocalPush ? GitUtils.stripDoubleSlash(pushPath) : GitUtils.replaceHttpPassword(pushPath))); // NOI18N
                } else {
                    logger.outputInRed(
                            NbBundle.getMessage(PushAction.class,
                            "MSG_PUSH_TO", toPrjName, bLocalPush ? GitUtils.stripDoubleSlash(pushPath) : GitUtils.replaceHttpPassword(pushPath))); // NOI18N
                }

                if (fromPrjName == null ){
                    logger.outputInRed(
                            NbBundle.getMessage(PushAction.class,
                            "MSG_PUSH_FROM_NONAME", root)); // NOI18N
                } else {
                    logger.outputInRed(
                            NbBundle.getMessage(PushAction.class,
                            "MSG_PUSH_FROM", fromPrjName, root)); // NOI18N
                }

                boolean bMergeNeeded = GitCommand.isHeadsCreated(list.get(list.size() - 1));
                boolean bConfirmMerge = false;
                // Push does not do an Update of the target Working Dir
                if (!bMergeNeeded) {
                    if (bNoChanges) {
                        return;
                    }
                    if (bLocalPush) {
                        list = GitCommand.doUpdateAll(pushFile, false, null, false);
                        logger.output(list);
                        if (toPrjName != null) {
                            logger.outputInRed(
                                    NbBundle.getMessage(PushAction.class,
                                    "MSG_PUSH_UPDATE_DONE", toPrjName, GitUtils.stripDoubleSlash(pushPath))); // NOI18N
                        } else {
                            logger.outputInRed(
                                    NbBundle.getMessage(PushAction.class,
                                    "MSG_PUSH_UPDATE_DONE_NONAME", GitUtils.stripDoubleSlash(pushPath))); // NOI18N
                        }
                        boolean bOutStandingUncommittedMerges = GitCommand.isMergeAbortUncommittedMsg(list.get(list.size() - 1));
                        if (bOutStandingUncommittedMerges) {
                            bConfirmMerge = GitUtils.confirmDialog(PushAction.class, "MSG_PUSH_MERGE_CONFIRM_TITLE", "MSG_PUSH_MERGE_UNCOMMITTED_CONFIRM_QUERY"); // NOI18N 
                        }
                    }
                } else {
                    bConfirmMerge = GitUtils.confirmDialog(PushAction.class, "MSG_PUSH_MERGE_CONFIRM_TITLE", "MSG_PUSH_MERGE_CONFIRM_QUERY"); // NOI18N 
                }

                if (bConfirmMerge) {
                    logger.output(""); // NOI18N
                    logger.outputInRed(
                            NbBundle.getMessage(PushAction.class,
                            "MSG_PUSH_MERGE_DO")); // NOI18N
                    MergeAction.doMergeAction(pushFile, null, logger);
                } else {
                    List<String> headRevList = GitCommand.getHeadRevisions(pushPath);
                    if (headRevList != null && headRevList.size() > 1) {
                        MergeAction.printMergeWarning(headRevList, logger);
                    }
                }
            }
            if (bLocalPush && !bNoChanges) {
                GitUtils.forceStatusRefresh(pushFile);
                // refresh filesystem to take account of deleted files
                FileObject rootObj = FileUtil.toFileObject(pushFile);
                try {
                    rootObj.getFileSystem().refresh(true);
                } catch (java.lang.Exception ex) {
                }
            }
        } catch (GitException ex) {
            NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
            DialogDisplayer.getDefault().notifyLater(e);
        } finally {
            logger.outputInRed(NbBundle.getMessage(PushAction.class, "MSG_PUSH_DONE")); // NOI18N
            logger.output(""); // NOI18N
        }
    }
    
}