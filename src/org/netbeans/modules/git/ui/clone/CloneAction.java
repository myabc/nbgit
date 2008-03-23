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
package org.netbeans.modules.git.ui.clone;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitException;
import org.netbeans.modules.git.GitModuleConfig;
import org.netbeans.modules.git.GitProgressSupport;
import org.netbeans.modules.git.OutputLogger;
import org.netbeans.modules.git.config.GitConfigFiles;
import org.netbeans.modules.git.ui.actions.ContextAction;
import org.netbeans.modules.git.ui.properties.GitProperties;
import org.netbeans.modules.git.util.GitCommand;
import org.netbeans.modules.git.util.GitProjectUtils;
import org.netbeans.modules.git.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;

/**
 * Clone action for Git: 
 * git clone - Create a copy of an existing repository in a new directory.
 * 
 * @author John Rice
 */
public class CloneAction extends ContextAction {
    
  private final VCSContext context;

    public CloneAction(String name, VCSContext context) {
        this.context = context;
        putValue(Action.NAME, name);
    }
    
    public void performAction(ActionEvent ev){
        final File root = GitUtils.getRootFile(context);
        if (root == null) return;
        
        // Get unused Clone Folder name
        File tmp = root.getParentFile();
        File projFile = GitUtils.getProjectFile(context);
        String folderName = root.getName();
        Boolean projIsRepos = true;
        if (!root.equals(projFile))  {
            // Git Repository is not the same as project root
            projIsRepos = false;
        }
        for(int i = 0; i < 10000; i++){
            if (!new File(tmp,folderName+"_clone"+i).exists()){ // NOI18N
                tmp = new File(tmp, folderName +"_clone"+i); // NOI18N
                break;
            }
        }
        Clone clone = new Clone(root, tmp);
        if (!clone.showDialog()) {
            return;
        }
        performClone(root.getAbsolutePath(), clone.getOutputFileName(), projIsRepos, projFile, true, null, null);
    }

    public static void performClone(final String source, final String target, boolean projIsRepos, 
            File projFile, final String pullPath, final String pushPath) {
        performClone(source, target, projIsRepos, projFile, false, pullPath, pushPath);
    }

    private static void performClone(final String source, final String target, 
            boolean projIsRepos, File projFile, final boolean isLocalClone, final String pullPath, final String pushPath) {
        final Git git = Git.getInstance();
        final ProjectManager projectManager = ProjectManager.getDefault();
        final File prjFile = projFile;
        final Boolean prjIsRepos = projIsRepos;
        final File cloneFolder = new File (target);
        final File normalizedCloneFolder = FileUtil.normalizeFile(cloneFolder);
        String projName = null;
        if (projFile != null) projName = GitProjectUtils.getProjectName(projFile);
        final String prjName = projName;
        File cloneProjFile;
        if (!prjIsRepos) {
            String name = null;
            if(prjFile != null)
                name = prjFile.getAbsolutePath().substring(source.length() + 1);
            else
                name = target;
            cloneProjFile = new File (normalizedCloneFolder, name);
        } else {
            cloneProjFile = normalizedCloneFolder;
        }
        final File clonePrjFile = cloneProjFile;
        
        RequestProcessor rp = Git.getInstance().getRequestProcessor(source);
        GitProgressSupport support = new GitProgressSupport() {
            Runnable doOpenProject = new Runnable () {
                public void run()  {
                    // Open and set focus on the cloned project if possible
                    OutputLogger logger = getLogger();
                    try {
                        FileObject cloneProj = FileUtil.toFileObject(clonePrjFile);
                        Project prj = null;
                        if(clonePrjFile != null && cloneProj != null)
                            prj = projectManager.findProject(cloneProj);
                        if(prj != null){
                            GitProjectUtils.openProject(prj, this, GitModuleConfig.getDefault().getSetMainProject());
                            git.versionedFilesChanged();
                            git.refreshAllAnnotations();
                        }else{
                            logger.outputInRed( NbBundle.getMessage(CloneAction.class,
                                    "MSG_EXTERNAL_CLONE_PRJ_NOT_FOUND_CANT_SETASMAIN")); // NOI18N
                        }
            
                    } catch (java.lang.Exception ex) {
                        NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(new GitException(ex.toString()));
                        DialogDisplayer.getDefault().notifyLater(e);
                    } finally{
                       logger.outputInRed(NbBundle.getMessage(CloneAction.class, "MSG_CLONE_DONE")); // NOI18N
                       logger.output(""); // NOI18N
                    }
                }
            };
            public void perform() {
                OutputLogger logger = getLogger();
                try {
                    // TODO: We need to annotate the cloned project 
                    // See http://qa.netbeans.org/issues/show_bug.cgi?id=112870
                    logger.outputInRed(
                            NbBundle.getMessage(CloneAction.class,
                            "MSG_CLONE_TITLE")); // NOI18N
                    logger.outputInRed(
                            NbBundle.getMessage(CloneAction.class,
                            "MSG_CLONE_TITLE_SEP")); // NOI18N
                    List<String> list = GitCommand.doClone(source, target, logger);
                    if(list != null && !list.isEmpty()){
                        GitUtils.createIgnored(cloneFolder);
                        logger.output(list);
               
                        if (prjName != null) {
                            logger.outputInRed(
                                    NbBundle.getMessage(CloneAction.class,
                                    "MSG_CLONE_FROM", prjName, source)); // NOI18N
                            logger.outputInRed(
                                    NbBundle.getMessage(CloneAction.class,
                                    "MSG_CLONE_TO", prjName, target)); // NOI18N
                        } else {
                            logger.outputInRed(
                                    NbBundle.getMessage(CloneAction.class,
                                    "MSG_EXTERNAL_CLONE_FROM", source)); // NOI18N
                            logger.outputInRed(
                                    NbBundle.getMessage(CloneAction.class,
                                    "MSG_EXTERNAL_CLONE_TO", target)); // NOI18N

                        }
                        logger.output(""); // NOI18N

                        if (isLocalClone){
                            SwingUtilities.invokeLater(doOpenProject);
                        } else if (GitModuleConfig.getDefault().getShowCloneCompleted()) {
                            CloneCompleted cc = new CloneCompleted(cloneFolder);
                            if (isCanceled()) {
                                return;
                            }
                            cc.scanForProjects(this);
                        }
                    }
                } catch (GitException ex) {
                    NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
                    DialogDisplayer.getDefault().notifyLater(e);
                }finally {
                    // #125835 - Push to default was not being set automatically by git after Clone
                    // but was after you opened the Git -> Properties, inconsistent
                    GitConfigFiles git = new GitConfigFiles(cloneFolder);
                    String defaultPull = git.getDefaultPull(false);
                    String defaultPush = git.getDefaultPush(false);
                    if(pullPath != null && !pullPath.equals("")) defaultPull = pullPath;
                    if(pushPath != null && !pushPath.equals("")) defaultPush = pushPath;
                    git.setProperty(GitProperties.GITPROPNAME_DEFAULT_PULL, defaultPull);
                    git.setProperty(GitProperties.GITPROPNAME_DEFAULT_PUSH, defaultPush);
                        
                    //#121581: Work around for ini4j bug on Windows not handling single '\' correctly
                    // git clone creates the default gitconfig, we just overwrite it's contents with 
                    // default path contianing '\\'
                    if(isLocalClone && Utilities.isWindows()){ 
                        fixLocalPullPushPathsOnWindows(cloneFolder.getAbsolutePath(), defaultPull, defaultPush);
                    }
                    if(!isLocalClone){
                        logger.outputInRed(NbBundle.getMessage(CloneAction.class, "MSG_CLONE_DONE")); // NOI18N
                        logger.output(""); // NOI18N
                    }
                }
            }
        };
        support.start(rp, source, org.openide.util.NbBundle.getMessage(CloneAction.class, "LBL_Clone_Progress", source)); // NOI18N
    }

    @Override
    public boolean isEnabled() {
        return GitUtils.getRootFile(context) != null;
    }
   
    private static final String GIT_PATHS_SECTION_ENCLOSED = "[" + GitConfigFiles.GIT_PATHS_SECTION + "]";// NOI18N
    private static void fixLocalPullPushPathsOnWindows(String root, String defaultPull, String defaultPush) {
        File gitConfigFile = null;
        File tempFile = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        
        try {
            gitConfigFile = new File(root + File.separator + GitConfigFiles.GIT_REPO_DIR, GitConfigFiles.GITCONFIG_FILE);
            if (!gitConfigFile.isFile() || !gitConfigFile.canWrite()) return;
            
            String defaultPullWinStr = GitConfigFiles.GIT_DEFAULT_PULL_VALUE + " = " + defaultPull.replace("\\", "\\\\") + "\n"; // NOI18N
            String defaultPushWinStr = GitConfigFiles.GIT_DEFAULT_PUSH_VALUE + " = " + defaultPush.replace("\\", "\\\\") + "\n"; // NOI18N

            tempFile = new File(gitConfigFile.getAbsolutePath() + ".tmp"); // NOI18N
            if (tempFile == null) return;
            
            br = new BufferedReader(new FileReader(gitConfigFile));
            pw = new PrintWriter(new FileWriter(tempFile));

            String line = null;
            
            boolean bInPaths = false;
            boolean bPullDone = false;
            boolean bPushDone = false;
            while ((line = br.readLine()) != null) {
                if (line.startsWith(GIT_PATHS_SECTION_ENCLOSED)) {
                    bInPaths = true;
                }else if (line.startsWith("[")) { // NOI18N
                    bInPaths = false;
                }

                if (bInPaths && !bPullDone && line.startsWith(GitConfigFiles.GIT_DEFAULT_PULL_VALUE) && 
                        !line.startsWith(GitConfigFiles.GIT_DEFAULT_PUSH_VALUE)) {
                    pw.println(defaultPullWinStr);
                    bPullDone = true;
                } else if (bInPaths && !bPullDone && line.startsWith(GitConfigFiles.GIT_DEFAULT_PULL)) {
                    pw.println(defaultPullWinStr);
                    bPullDone = true;
                } else if (bInPaths && !bPushDone && line.startsWith(GitConfigFiles.GIT_DEFAULT_PUSH_VALUE)) {
                    pw.println(defaultPushWinStr);
                    bPushDone = true;
                } else {
                    pw.println(line);
                    pw.flush();
                }
            }
        } catch (IOException ex) {
            // Ignore
        } finally {
            try {
                if(pw != null) pw.close();
                if(br != null) br.close();
                if(tempFile != null && tempFile.isFile() && tempFile.canWrite() && gitConfigFile != null){ 
                    gitConfigFile.delete();
                    tempFile.renameTo(gitConfigFile);
                }
            } catch (IOException ex) {
            // Ignore
            }
        }
    }
}