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
import org.netbeans.modules.git.FileInformation;
import org.netbeans.modules.git.FileStatusCache;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitException;
import org.netbeans.modules.git.GitProgressSupport;
import org.netbeans.modules.git.OutputLogger;
import org.netbeans.modules.git.ui.actions.ContextAction;
import org.netbeans.modules.git.ui.update.ConflictResolvedAction;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;


/**
 * Pull action for Git: 
 * git pull - pull changes from the specified source
 * 
 * @author John Rice
 */
public class StripAction extends ContextAction {
    
    private final VCSContext context;
    private static String GIT_STRIP_SAVE_BUNDLE = "saving bundle to ";
            
    public StripAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }
    
    public void performAction(ActionEvent e) {
        strip(context);
    }
    
    public static void strip(final VCSContext ctx){
        final File root = GitUtils.getRootFile(ctx);
        if (root == null) return;
        String repository = root.getAbsolutePath();
         
        String rev = null;

        final Strip strip = new Strip(root);
        if (!strip.showDialog()) {
            return;
        }
        rev = strip.getSelectionRevision();
        final boolean doBackup = strip.isBackupRequested();
        final String revStr = rev;

        RequestProcessor rp = Git.getInstance().getRequestProcessor(repository);
        GitProgressSupport support = new GitProgressSupport() {
            public void perform() {
                
                OutputLogger logger = getLogger();
                try {
                    logger.outputInRed(
                                NbBundle.getMessage(StripAction.class,
                                "MSG_STRIP_TITLE")); // NOI18N
                    logger.outputInRed(
                                NbBundle.getMessage(StripAction.class,
                                "MSG_STRIP_TITLE_SEP")); // NOI18N
                    logger.output(
                                NbBundle.getMessage(StripAction.class,
                                "MSG_STRIP_INFO_SEP", revStr, root.getAbsolutePath())); // NOI18N
                    List<String> list = GitCommand.doStrip(root, revStr, false, doBackup, logger);
                    
                    if(list != null && !list.isEmpty()){                      
                        logger.output(list);
                        
                        if(GitCommand.isNoRevStrip(list.get(0))){
                            logger.outputInRed(
                                    NbBundle.getMessage(StripAction.class,
                                    "MSG_NO_REV_STRIP",revStr));     // NOI18N                       
                        }else if(GitCommand.isLocalChangesStrip(list.get(0))){
                            logger.outputInRed(
                                    NbBundle.getMessage(StripAction.class,
                                    "MSG_LOCAL_CHANGES_STRIP"));     // NOI18N                       
                        }else if(GitCommand.isMultipleHeadsStrip(list.get(0))){
                            logger.outputInRed(
                                    NbBundle.getMessage(StripAction.class,
                                    "MSG_MULTI_HEADS_STRIP"));     // NOI18N                       
                        }else{
                            if (GitCommand.hasHistory(root)) {
                                FileStatusCache cache = Git.getInstance().getFileStatusCache();
                                if (cache.listFiles(ctx, FileInformation.STATUS_VERSIONED_CONFLICT).length != 0) {
                                    ConflictResolvedAction.resolved(ctx);
                                }
                                GitUtils.forceStatusRefreshProject(ctx);
                                Git.getInstance().changesetChanged(root);
                            }
                            String savingTo = list.get(list.size()-1);
                            savingTo = savingTo != null? savingTo.substring(GIT_STRIP_SAVE_BUNDLE.length()): null;
                            File savingFile = new File(savingTo);
                            if(savingFile != null && savingFile.exists() && savingFile.canRead()){
                                logger.outputInRed(
                                        NbBundle.getMessage(StripAction.class,
                                        "MSG_STRIP_RESTORE_INFO")); // NOI18N                                
                                logger.output(
                                        NbBundle.getMessage(StripAction.class,
                                        "MSG_STRIP_RESTORE_INFO2", savingFile.getAbsoluteFile())); // NOI18N                                
                            }
                        }
                    }
                } catch (GitException ex) {
                    NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
                    DialogDisplayer.getDefault().notifyLater(e);
                } finally {
                    logger.outputInRed(
                                NbBundle.getMessage(StripAction.class,
                                "MSG_STRIP_DONE")); // NOI18N
                    logger.output(""); // NOI18N
                }
            }
        };
        support.start(rp, repository,org.openide.util.NbBundle.getMessage(StripAction.class, "MSG_STRIP_PROGRESS")); // NOI18N
    }
    
    @Override
    public boolean isEnabled() {
        return GitUtils.getRootFile(context) != null;
    }
}