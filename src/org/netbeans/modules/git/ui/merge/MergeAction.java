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
package org.netbeans.modules.git.ui.merge;


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
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 * Merge action for Git:
 * git merge - attempts to merge changes when the repository has 2 heads
 *
 * @author John Rice
 */
public class MergeAction extends ContextAction {

    private final VCSContext context;
    private final static int MULTIPLE_AUTOMERGE_HEAD_LIMIT = 2;
    
    public MergeAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }

    @Override
    public boolean isEnabled() {
        Set<File> ctxFiles = context != null? context.getRootFiles(): null;
        if(GitUtils.getRootFile(context) == null || ctxFiles == null || ctxFiles.size() == 0) 
            return false;
        return true; // #121293: Speed up menu display, warn user if nothing to merge when Merge selected
    }

    public void performAction(ActionEvent ev) {
        final File root = GitUtils.getRootFile(context);
        if (root == null) {
            OutputLogger logger = OutputLogger.getLogger(Git.GIT_OUTPUT_TAB_TITLE);
            logger.outputInRed( NbBundle.getMessage(MergeAction.class,"MSG_MERGE_TITLE")); // NOI18N
            logger.outputInRed( NbBundle.getMessage(MergeAction.class,"MSG_MERGE_TITLE_SEP")); // NOI18N
            logger.outputInRed(
                    NbBundle.getMessage(MergeAction.class, "MSG_MERGE_NOT_SUPPORTED_INVIEW_INFO")); // NOI18N
            logger.output(""); // NOI18N
            logger.closeLog();
            JOptionPane.showMessageDialog(null,
                    NbBundle.getMessage(MergeAction.class, "MSG_MERGE_NOT_SUPPORTED_INVIEW"),// NOI18N
                    NbBundle.getMessage(MergeAction.class, "MSG_MERGE_NOT_SUPPORTED_INVIEW_TITLE"),// NOI18N
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String repository = root.getAbsolutePath();
        RequestProcessor rp = Git.getInstance().getRequestProcessor(repository);
        GitProgressSupport support = new GitProgressSupport() {
            public void perform() {
                OutputLogger logger = getLogger();
                try {
                    List<String> headList = GitCommand.getHeadRevisions(root);
                    String revStr = null;
                    if (headList.size() <= 1) {
                        logger.outputInRed( NbBundle.getMessage(MergeAction.class,"MSG_MERGE_TITLE")); // NOI18N
                        logger.outputInRed( NbBundle.getMessage(MergeAction.class,"MSG_MERGE_TITLE_SEP")); // NOI18N
                        logger.output( NbBundle.getMessage(MergeAction.class,"MSG_NOTHING_TO_MERGE")); // NOI18N
                        logger.outputInRed( NbBundle.getMessage(MergeAction.class, "MSG_MERGE_DONE")); // NOI18N
                        logger.output(""); // NOI18N
                        JOptionPane.showMessageDialog(null,
                            NbBundle.getMessage(MergeAction.class,"MSG_NOTHING_TO_MERGE"),// NOI18N
                            NbBundle.getMessage(MergeAction.class,"MSG_MERGE_TITLE"),// NOI18N
                            JOptionPane.INFORMATION_MESSAGE);
                         return;
                    } else if (headList.size() > MULTIPLE_AUTOMERGE_HEAD_LIMIT){
                        final MergeRevisions mergeDlg = new MergeRevisions(root);
                        if (!mergeDlg.showDialog()) {
                            return;
                        }
                        revStr = mergeDlg.getSelectionRevision();               
                    }
                    logger.outputInRed(
                            NbBundle.getMessage(MergeAction.class, "MSG_MERGE_TITLE")); // NOI18N
                    logger.outputInRed(
                            NbBundle.getMessage(MergeAction.class, "MSG_MERGE_TITLE_SEP")); // NOI18N
                    doMergeAction(root, revStr, logger);
                    GitUtils.forceStatusRefreshProject(context);
                    logger.output(""); // NOI18N
                } catch (GitException ex) {
                    NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
                    DialogDisplayer.getDefault().notifyLater(e);
                }
            }
        };
        support.start(rp, repository, NbBundle.getMessage(MergeAction.class, "MSG_MERGE_PROGRESS")); // NOI18N
    }

    public static boolean doMergeAction(File root, String revStr, OutputLogger logger) throws GitException {
        List<String> listMerge = GitCommand.doMerge(root, revStr);
        Boolean bConflicts = false;
        Boolean bMergeFailed = false;
        
        if (listMerge != null && !listMerge.isEmpty()) {
            logger.output(listMerge);
            for (String line : listMerge) {
                if (GitCommand.isMergeAbortUncommittedMsg(line)){ 
                    bMergeFailed = true;
                    logger.outputInRed(NbBundle.getMessage(MergeAction.class,
                            "MSG_MERGE_FAILED")); // NOI18N
                    JOptionPane.showMessageDialog(null,
                        NbBundle.getMessage(MergeAction.class,"MSG_MERGE_UNCOMMITTED"), // NOI18N
                        NbBundle.getMessage(MergeAction.class,"MSG_MERGE_TITLE"), // NOI18N
                        JOptionPane.WARNING_MESSAGE);
                    break;
                }            

                if (GitCommand.isMergeAbortMultipleHeadsMsg(line)){ 
                    bMergeFailed = true;
                    logger.outputInRed(NbBundle.getMessage(MergeAction.class,
                            "MSG_MERGE_FAILED")); // NOI18N
                    break;
                }
                if (GitCommand.isMergeConflictMsg(line)) {
                    bConflicts = true;
                    String filepath = null;
                    if(Utilities.isWindows()){
                        filepath = line.substring(
                            GitCommand.GIT_MERGE_CONFLICT_WIN1_ERR.length(),
                            line.length() - GitCommand.GIT_MERGE_CONFLICT_WIN2_ERR.length()
                            ).trim().replace("/", "\\"); // NOI18N
                        filepath = root.getAbsolutePath() + File.separator + filepath;
                    }else{
                        filepath = line.substring(GitCommand.GIT_MERGE_CONFLICT_ERR.length());
                    }
                    logger.outputInRed(NbBundle.getMessage(MergeAction.class, "MSG_MERGE_CONFLICT", filepath)); // NOI18N
                    GitCommand.createConflictFile(filepath);
                }
                
                if (GitCommand.isMergeUnavailableMsg(line)){ 
                        JOptionPane.showMessageDialog(null, 
                                NbBundle.getMessage(MergeAction.class, "MSG_MERGE_UNAVAILABLE"), // NOI18N
                                NbBundle.getMessage(MergeAction.class, "MSG_MERGE_TITLE"), // NOI18N
                                JOptionPane.WARNING_MESSAGE);
                        logger.outputInRed(
                                NbBundle.getMessage(MergeAction.class, "MSG_MERGE_INFO"));// NOI18N            
                        logger.outputLink(
                                NbBundle.getMessage(MergeAction.class, "MSG_MERGE_INFO_URL")); // NOI18N 
                }            
            }
                  
            if (bConflicts) {
                logger.outputInRed(NbBundle.getMessage(MergeAction.class, 
                        "MSG_MERGE_DONE_CONFLICTS")); // NOI18N
            }
            if (!bMergeFailed && !bConflicts) {
                logger.outputInRed(NbBundle.getMessage(MergeAction.class, 
                        "MSG_MERGE_DONE")); // NOI18N
            }
        }
        return true;
    }
    
    public static void printMergeWarning(List<String> list, OutputLogger logger){
        if(list == null || list.isEmpty() || list.size() <= 1) return;
        
        if (list.size() == 2) {
            logger.outputInRed(NbBundle.getMessage(MergeAction.class, 
                    "MSG_MERGE_WARN_NEEDED", list)); // NOI18N
            logger.outputInRed(NbBundle.getMessage(MergeAction.class, 
                    "MSG_MERGE_DO_NEEDED")); // NOI18N
        } else {
            logger.outputInRed(NbBundle.getMessage(MergeAction.class, 
                    "MSG_MERGE_WARN_MULTIPLE_HEADS", list.size(), list)); // NOI18N
            logger.outputInRed(NbBundle.getMessage(MergeAction.class, 
                    "MSG_MERGE_DONE_MULTIPLE_HEADS")); // NOI18N
        }
    }

}