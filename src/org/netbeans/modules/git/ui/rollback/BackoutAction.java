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
package org.netbeans.modules.git.ui.rollback;


import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import javax.swing.Action;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitException;
import org.netbeans.modules.git.GitProgressSupport;
import org.netbeans.modules.git.OutputLogger;
import org.netbeans.modules.git.ui.actions.ContextAction;
import org.netbeans.modules.git.ui.merge.MergeAction;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;


/**
 * Pull action for Git: 
 * git pull - pull changes from the specified source
 * 
 * @author John Rice
 */
public class BackoutAction extends ContextAction {
    
    private final VCSContext context;
    private static final String GIT_BACKOUT_REVISION_REPLACE = "\\{revision}";
    public static final String GIT_BACKOUT_REVISION = " {revision}";
            
    public BackoutAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }
    
    public void performAction(ActionEvent e) {
        backout(context);
    }
    
    public static void backout(final VCSContext ctx){
        final File root = GitUtils.getRootFile(ctx);
        if (root == null) return;
        String repository = root.getAbsolutePath();
         
        String rev = null;
        String commitMsg = null;

        final Backout backout = new Backout(root);
        if (!backout.showDialog()) {
            return;
        }
        rev = backout.getSelectionRevision();
        commitMsg = backout.getCommitMessage();
        final boolean doMerge = false; // Now handling this using our own merge mechanism, not backout's
        final String revStr = rev;
        commitMsg = commitMsg.replaceAll(GIT_BACKOUT_REVISION_REPLACE, revStr); //NOI18N
        final String commitMsgStr = commitMsg;
        
        RequestProcessor rp = Git.getInstance().getRequestProcessor(repository);
        GitProgressSupport support = new GitProgressSupport() {
            public void perform() {
                
                OutputLogger logger = getLogger();
                try {
                    logger.outputInRed(
                                NbBundle.getMessage(BackoutAction.class,
                                "MSG_BACKOUT_TITLE")); // NOI18N
                    logger.outputInRed(
                                NbBundle.getMessage(BackoutAction.class,
                                "MSG_BACKOUT_TITLE_SEP")); // NOI18N
                    logger.output(
                                NbBundle.getMessage(BackoutAction.class,
                                "MSG_BACKOUT_INFO_SEP", revStr, root.getAbsolutePath())); // NOI18N
                    List<String> list = GitCommand.doRevert(root, revStr, doMerge, commitMsgStr, logger);
                    
                    if(list != null && !list.isEmpty()){ 
                        boolean bMergeNeededDueToBackout = GitCommand.isBackoutMergeNeededMsg(list.get(list.size() - 1));
                        if(bMergeNeededDueToBackout){
                            list.remove(list.size() - 1);
                            list.remove(list.size() - 1);
                        }
                        logger.output(list);                            
                        
                        if(GitCommand.isUncommittedChangesBackout(list.get(0))){
                            logger.outputInRed(
                                    NbBundle.getMessage(BackoutAction.class,
                                    "MSG_UNCOMMITTED_CHANGES_BACKOUT"));     // NOI18N           
                            return;
                        } else if(GitCommand.isMergeChangesetBackout(list.get(0))){
                            logger.outputInRed(
                                    NbBundle.getMessage(BackoutAction.class,
                                    "MSG_MERGE_CSET_BACKOUT",revStr));     // NOI18N        
                            return;
                        } else if(GitCommand.isNoRevStrip(list.get(0))){
                            logger.outputInRed(
                                    NbBundle.getMessage(BackoutAction.class,
                                    "MSG_NO_REV_BACKOUT",revStr));     // NOI18N        
                            return;
                        }
                        
                        // Handle Merge - both automatic and merge with conflicts
                        boolean bConfirmMerge = false;
                        if (bMergeNeededDueToBackout) {
                            bConfirmMerge = GitUtils.confirmDialog(
                                    BackoutAction.class, "MSG_BACKOUT_MERGE_CONFIRM_TITLE", "MSG_BACKOUT_MERGE_CONFIRM_QUERY"); // NOI18N
                        }
                        if (bConfirmMerge) {
                            logger.output(""); // NOI18N
                            logger.outputInRed(NbBundle.getMessage(BackoutAction.class, "MSG_BACKOUT_MERGE_DO")); // NOI18N
                            MergeAction.doMergeAction(root, null, logger);
                        } else {
                            List<String> headRevList = GitCommand.getHeadRevisions(root);
                            if (headRevList != null && headRevList.size() > 1) {
                                MergeAction.printMergeWarning(headRevList, logger);
                            }
                        }  
                        GitUtils.forceStatusRefreshProject(ctx);
                        // refresh filesystem to take account of deleted files.
                        FileObject rootObj = FileUtil.toFileObject(root);
                        try {
                            rootObj.getFileSystem().refresh(true);
                        } catch (java.lang.Exception ex) {
                        }                        
                    }
                } catch (GitException ex) {
                    NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
                    DialogDisplayer.getDefault().notifyLater(e);
                } finally {
                    logger.outputInRed(
                                NbBundle.getMessage(BackoutAction.class,
                                "MSG_BACKOUT_DONE")); // NOI18N
                    logger.output(""); // NOI18N
                }
            }
        };
        support.start(rp, repository,org.openide.util.NbBundle.getMessage(BackoutAction.class, "MSG_BACKOUT_PROGRESS")); // NOI18N
    }
    
    @Override
    public boolean isEnabled() {
        return GitUtils.getRootFile(context) != null;
    }
}