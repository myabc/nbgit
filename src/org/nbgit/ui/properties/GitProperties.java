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
package org.nbgit.ui.properties;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.nbgit.Git;
import org.nbgit.GitModuleConfig;
import org.nbgit.GitProgressSupport;
import org.nbgit.util.GitUtils;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.lib.RepositoryConfig;

/**
 *
 * @author Padraig O'Briain
 */
public class GitProperties implements ListSelectionListener {

    private PropertiesPanel panel;
    private File root;
    private PropertiesTable propTable;
    private GitProgressSupport support;

    /** Creates a new instance of GitProperties */
    public GitProperties(PropertiesPanel panel, PropertiesTable propTable, File root) {
        this.panel = panel;
        this.propTable = propTable;
        this.root = root;
        propTable.getTable().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        propTable.getTable().getSelectionModel().addListSelectionListener(this);

        refreshProperties();
    }

    public PropertiesPanel getPropertiesPanel() {
        return panel;
    }

    public void setPropertiesPanel(PropertiesPanel panel) {
        this.panel = panel;
    }

    public File getRoot() {
        return root;
    }

    public void setRoot(File root) {
        this.root = root;
    }

    protected String getPropertyValue() {
        return panel.txtAreaValue.getText();
    }

    protected void refreshProperties() {
        RequestProcessor rp = Git.getInstance().getRequestProcessor(root.getAbsolutePath());
        try {
            support = new GitProgressSupport() {

                protected void perform() {
                    Properties props = GitModuleConfig.getDefault().getProperties(root);
                    GitPropertiesNode[] gitProps = new GitPropertiesNode[props.size()];
                    int i = 0;

                    for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
                        String name = (String) e.nextElement();
                        String tmp = props.getProperty(name);
                        String value = tmp != null ? tmp : ""; // NOI18N
                        gitProps[i] = new GitPropertiesNode(name, value);
                        i++;
                    }
                    propTable.setNodes(gitProps);
                }
            };
            support.start(rp, root.getAbsolutePath(), org.openide.util.NbBundle.getMessage(GitProperties.class, "LBL_Properties_Progress")); // NOI18N
        } finally {
            support = null;
        }
    }

    public void setProperties() {
        RequestProcessor rp = Git.getInstance().getRequestProcessor(root.getAbsolutePath());
        try {
            support = new GitProgressSupport() {

                protected void perform() {
                    Repository repo = Git.getInstance().getRepository(root);
                    if (repo == null) {
                        return;
                    }
                    RepositoryConfig config = repo.getConfig();
                    boolean save = false;
                    GitPropertiesNode[] gitPropertiesNodes = propTable.getNodes();
                    for (int i = 0; i < gitPropertiesNodes.length; i++) {
                        String name = gitPropertiesNodes[i].getName();
                        String value = gitPropertiesNodes[i].getValue().trim();
                        if (value.length() == 0) {
                            continue;
                        }

                        if (name.equals("user.name")) {
                            config.setString("user", null, "name", value);
                            save = true;
                        }

                        if (name.equals("user.email")) {
                            if (!GitModuleConfig.getDefault().isEmailValid(value)) {
                                GitUtils.warningDialog(GitProperties.class,
                                                       "MSG_WARN_EMAIL_TEXT", // NOI18N
                                                       "MSG_WARN_EMAIL_TITLE"); // NOI18N
                                return;
                            }
                            config.setString("user", null, "email", value);
                            save = true;
                        }

                        if (name.equals("nbgit.signoff")) {
                            config.setString("nbgit", null, "signoff", value);
                            save = true;
                        }

                        if (name.equals("nbgit.stripspace")) {
                            config.setString("nbgit", null, "stripspace", value);
                            save = true;
                        }
                    }

                    try {
                        if (save)
                            config.save();
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            };
            support.start(rp, root.getAbsolutePath(), org.openide.util.NbBundle.getMessage(GitProperties.class, "LBL_Properties_Progress")); // NOI18N
        } finally {
            support = null;
        }
    }
    private int lastIndex = -1;

    public void updateLastSelection() {
        GitPropertiesNode[] gitPropertiesNodes = propTable.getNodes();
        if (lastIndex >= 0) {
            gitPropertiesNodes[lastIndex].setValue(getPropertyValue());
        }
    }

    public void valueChanged(ListSelectionEvent e) {
        int index = propTable.getTable().getSelectedRow();
        if (index < 0) {
            lastIndex = -1;
            return;
        }
        GitPropertiesNode[] gitPropertiesNodes = propTable.getNodes();
        if (lastIndex >= 0) {
            gitPropertiesNodes[lastIndex].setValue(getPropertyValue());
        }
        panel.txtAreaValue.setText(gitPropertiesNodes[index].getValue());
        lastIndex = index;
    }
}
