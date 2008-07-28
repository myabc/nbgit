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
package org.netbeans.modules.git.ui.pull;


import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import javax.swing.Action;
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

/**
 * Fetch action for Git: 
 * git fetch - fetch changes from a specified repository, then execute the
 * show command to view the dependency tree for the repository.
 * 
 * @author John Rice
 */
public class FetchAction extends ContextAction {
 
    private final VCSContext context;

    public FetchAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }
    
    public void performAction(ActionEvent e) {
        final File root = GitUtils.getRootFile(context);
        if (root == null) return;
        
        RequestProcessor rp = Git.getInstance().getRequestProcessor(root);
        GitProgressSupport support = new GitProgressSupport() {
            public void perform() { performFetch(root, this.getLogger()); } };

        support.start(rp, root.getAbsolutePath(), org.openide.util.NbBundle.getMessage(FetchAction.class, "MSG_FETCH_PROGRESS")); // NOI18N
    }

    static void performFetch(File root, OutputLogger logger) {
        try {
            logger.outputInRed(NbBundle.getMessage(FetchAction.class, "MSG_FETCH_TITLE")); // NOI18N
            logger.outputInRed(NbBundle.getMessage(FetchAction.class, "MSG_FETCH_TITLE_SEP")); // NOI18N
            
            logger.outputInRed(NbBundle.getMessage(FetchAction.class, 
                    "MSG_FETCH_LAUNCH_INFO", root.getAbsolutePath())); // NOI18N
            
            List<String> list;
            list = GitCommand.doFetch(root, logger);
            
            if (list != null && !list.isEmpty()) {
                logger.output(GitUtils.replaceHttpPassword(list));
            }
        } catch (GitException ex) {
            NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
            DialogDisplayer.getDefault().notifyLater(e);
        }finally{
            logger.outputInRed(NbBundle.getMessage(FetchAction.class, "MSG_FETCH_DONE")); // NOI18N
            logger.output(""); // NOI18N
        }
    }

    @Override
    public boolean isEnabled() {
        return GitUtils.getRootFile(context) != null;
    } 
    
}