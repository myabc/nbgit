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
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.git.Git;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.ProjectChooser;
import org.openide.explorer.ExplorerManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Simpliied nb_all/projects/projectui/src/org/netbeans/modules/project/ui/ProjectUtilities.java,
 * nb_all/projects/projectui/src/org/netbeans/modules/project/ui/ProjectTab.java and
 * nb_all/ide/welcome/src/org/netbeans/modules/welcome/ui/TitlePanel.java copy.
 *
 * @author Petr Kuzel       
 */
final class ProjectUtilities {

    private static final String ProjectTab_ID_LOGICAL = "projectTabLogical_tc"; // NOI18N

    public static void selectAndExpandProject( final Project p ) {

        // invoke later to select the being opened project if the focus is outside ProjectTab
        SwingUtilities.invokeLater (new Runnable () {

            final ExplorerManager.Provider ptLogial = findDefault(ProjectTab_ID_LOGICAL);

            public void run () {
                Node root = ptLogial.getExplorerManager ().getRootContext ();
                // Node projNode = root.getChildren ().findChild( p.getProjectDirectory().getName () );
                Node projNode = root.getChildren ().findChild( ProjectUtils.getInformation( p ).getName() );
                if ( projNode != null ) {
                    try {
                        ptLogial.getExplorerManager ().setSelectedNodes( new Node[] { projNode } );
                    } catch (Exception ignore) {
                        // may ignore it
                    }
                }
            }
        });

    }

    /* Singleton accessor. As ProjectTab is persistent singleton this
     * accessor makes sure that ProjectTab is deserialized by window system.
     * Uses known unique TopComponent ID TC_ID = "projectTab_tc" to get ProjectTab instance
     * from window system. "projectTab_tc" is name of settings file defined in module layer.
     * For example ProjectTabAction uses this method to create instance if necessary.
     */
    private static synchronized ExplorerManager.Provider findDefault( String tcID ) {
        TopComponent tc = WindowManager.getDefault().findTopComponent( tcID );
        return (ExplorerManager.Provider) tc;
    }

    /**
     * Runs <i>New Project...</i> wizard with redefined defaults:
     * <ul>
     * <li>default project directory to working folder to
     * capture creating new project in placeholder
     * directory prepared by CVS server admin
     * <li>CommonProjectActions.EXISTING_SOURCES_FOLDER
     * pointing to working folder to capture
     * typical <i>... from Existing Sources</i> panel
     * <i>Add</i> button behaviour.
     * </ul>
     */
    public static void newProjectWizard(File workingDirectory) {
        if(workingDirectory == null) return;
        
        Action action = CommonProjectActions.newProjectAction();
        if (action != null) {
            File original = ProjectChooser.getProjectsFolder();
            ProjectChooser.setProjectsFolder(workingDirectory);
            FileObject workingFolder = FileUtil.toFileObject(workingDirectory);
            action.putValue(CommonProjectActions.EXISTING_SOURCES_FOLDER, workingFolder);
            performAction(action);
            try {
                ProjectChooser.setProjectsFolder(original);
            } catch (IllegalArgumentException e) {
                // it seems the original folder is invalid, ignore this
            }
        }
    }

    /**
     * Scans given folder (and subfolder into deep 5) for projects.
     * @return List of {@link Project}s never <code>null</code>.
     */
    public static List<Project> scanForProjects(FileObject scanRoot) {
        ProjectManager.getDefault().clearNonProjectCache();
        return scanForProjectsRecursively(scanRoot, 5);
    }

    private static List<Project> scanForProjectsRecursively(FileObject scanRoot, int deep) {
        if (scanRoot == null || deep <= 0) return Collections.emptyList();
        List<Project> projects = new LinkedList<Project>();
        ProjectManager projectManager = ProjectManager.getDefault();
        if (scanRoot.isFolder() && projectManager.isProject(scanRoot)) {
            try {
                Project prj = projectManager.findProject(scanRoot);
                if(prj != null) {
                    projects.add(prj);   
                }                
            } catch (IOException e) {
                // it happens for all apisupport projects unless
                // checked out into directory that contains nbbuild and openide folders
                // apisupport project is valid only if placed in defined directory structure
                Throwable cause = new Throwable("HG.PU: ignoring suspicious project folder...");  // NOI18N
                e.initCause(cause);
                Git.LOG.log(Level.INFO, null, e);
            }
        }
        Enumeration en = scanRoot.getChildren(false);
        while (en.hasMoreElements()) {
            FileObject fo = (FileObject) en.nextElement();
            if (fo.isFolder()) {
                List<Project> nested = scanForProjectsRecursively(fo, deep -1);  // RECURSION
                projects.addAll(nested);
            }
        }
        return projects;
    }

    private static boolean performAction (Action a) {
        if (a == null) {
            return false;
        }
        ActionEvent ae = new ActionEvent(ProjectUtilities.class, ActionEvent.ACTION_PERFORMED, "command");  // NOI18N
        try {
            a.actionPerformed(ae);
            return true;
        } catch (Exception e) {
            Git.LOG.log(Level.WARNING, null, e);
            return false;
        }
    }

    
}