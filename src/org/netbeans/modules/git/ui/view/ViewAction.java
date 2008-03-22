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
package org.netbeans.modules.git.ui.view;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.netbeans.modules.git.GitException;
import org.netbeans.modules.git.OutputLogger;
import org.netbeans.modules.git.config.GitConfigFiles;
import org.netbeans.modules.git.ui.actions.ContextAction;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * View action for Git: 
 * git view - launch git view to view the dependency tree for the repository
 * 
 * @author John Rice
 */
public class ViewAction extends ContextAction {
    
    private final VCSContext context;
    private static final String HG_SCRIPTS_DIR = "scripts";

    public ViewAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }
    
    public void performAction(ActionEvent e) {
        final File root = GitUtils.getRootFile(context);
        if (root == null) return;
        String repository = root.getAbsolutePath();
        RequestProcessor rp = RequestProcessor.getDefault();
        rp.post(new Runnable() {
            public void run() {
                performView(root);
            }
        });
    }

    static void performView(File root) {
        OutputLogger logger = OutputLogger.getLogger(root.getAbsolutePath());
        try {
            logger.outputInRed(NbBundle.getMessage(ViewAction.class, "MSG_VIEW_TITLE")); // NOI18N
            logger.outputInRed(NbBundle.getMessage(ViewAction.class, "MSG_VIEW_TITLE_SEP")); // NOI18N

            String hgkCommand = GitCommand.GITK_COMMAND;
//            if(Utilities.isWindows()){ 
//                hgkCommand = hgkCommand + GitCommand.GIT_WINDOWS_CMD;
//            }
            boolean bHgkFound = false;
//            if(GitUtils.isInUserPath(hgkCommand)){
//                    bHgkFound = true;                
//            } else if(GitUtils.isSolaris()){
//                File f = new File(GitCommand.HG_HGK_PATH_SOLARIS10, hgkCommand);
//                if(f.exists() && f.isFile()) 
//                    bHgkFound = true;
//            }else if(Utilities.isWindows()){
//                bHgkFound = HgUtils.isInUserPath(HG_SCRIPTS_DIR + File.separator + hgkCommand);                    
//            }
            boolean bHgkPropExists = GitConfigFiles.getInstance().containsProperty(
                            GitConfigFiles.HG_EXTENSIONS, GitConfigFiles.HG_EXTENSIONS_HGK);
            
            if(!bHgkFound){
                logger.outputInRed(
                            NbBundle.getMessage(ViewAction.class, "MSG_VIEW_HGK_NOT_FOUND_INFO")); // NOI18N
                logger.output(""); // NOI18N
                JOptionPane.showMessageDialog(null,
                        NbBundle.getMessage(ViewAction.class, "MSG_VIEW_HGK_NOT_FOUND"),// NOI18N
                        NbBundle.getMessage(ViewAction.class, "MSG_VIEW_HGK_NOT_FOUND_TITLE"),// NOI18N
                        JOptionPane.INFORMATION_MESSAGE);
                logger.closeLog();
                return;
            }
            if(!bHgkPropExists){
                boolean bConfirmSetHgkProp = false;
                bConfirmSetHgkProp = GitUtils.confirmDialog(
                        ViewAction.class, "MSG_VIEW_SETHGK_PROP_CONFIRM_TITLE", // NOI18N
                        "MSG_VIEW_SETHGK_PROP_CONFIRM_QUERY"); // NOI18N                
                if (bConfirmSetHgkProp) {
                    logger.outputInRed(
                            NbBundle.getMessage(ViewAction.class, "MSG_VIEW_SETHGK_PROP_DO_INFO")); // NOI18N
                    GitConfigFiles.getInstance().setProperty(GitConfigFiles.HG_EXTENSIONS_HGK, ""); // NOI18N
                }else{
                    logger.outputInRed(
                            NbBundle.getMessage(ViewAction.class, "MSG_VIEW_NOTSETHGK_PROP_INFO")); // NOI18N
                    logger.output(""); // NOI18N
                    logger.closeLog();
                    return;
                }
            }
            
            logger.outputInRed(NbBundle.getMessage(ViewAction.class, 
                    "MSG_VIEW_LAUNCH_INFO", root.getAbsolutePath())); // NOI18N
            logger.output(""); // NOI18N
            GitCommand.doView(root, logger);
            logger.closeLog();
        } catch (GitException ex) {
            NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
            DialogDisplayer.getDefault().notifyLater(e);
        }
    }

    @Override
    public boolean isEnabled() {
        return GitUtils.getRootFile(context) != null;
    } 
}