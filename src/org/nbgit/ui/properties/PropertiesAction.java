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

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JComponent;
import org.nbgit.ui.ContextAction;
import org.nbgit.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;

/**
 * Properties for Git:
 * Set git repository properties
 *
 * @author John Rice
 */
public class PropertiesAction extends ContextAction {

    public PropertiesAction(String name, VCSContext context) {
        super(name, context);
    }

    public void performAction(ActionEvent e) {
        File root = GitUtils.getRootFile(context);
        if (root == null) {
            return;
        }
        final PropertiesPanel panel = new PropertiesPanel();
        final PropertiesTable propTable;

        propTable = new PropertiesTable(panel.labelForTable, PropertiesTable.PROPERTIES_COLUMNS);
        panel.setPropertiesTable(propTable);

        JComponent component = propTable.getComponent();

        panel.propsPanel.setLayout(new BorderLayout());
        panel.propsPanel.add(component, BorderLayout.CENTER);

        GitProperties gitProperties = new GitProperties(panel, propTable, root);

        DialogDescriptor dd = new DialogDescriptor(panel, org.openide.util.NbBundle.getMessage(PropertiesAction.class, "CTL_PropertiesDialog_Title", null), true, null); // NOI18N

        JButton okButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(okButton, org.openide.util.NbBundle.getMessage(PropertiesAction.class, "CTL_Properties_Action_OK"));
        okButton.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(PropertiesAction.class, "ACSN_Properties_Action_OK")); // NOI18N
        okButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PropertiesAction.class, "ACSD_Properties_Action_OK"));

        JButton cancelButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(cancelButton, org.openide.util.NbBundle.getMessage(PropertiesAction.class, "CTL_Properties_Action_Cancel"));
        cancelButton.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(PropertiesAction.class, "ACSN_Properties_Action_Cancel")); // NOI18N
        cancelButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PropertiesAction.class, "ACSD_Properties_Action_Cancel"));
        dd.setOptions(new Object[]{okButton, cancelButton});
        dd.setHelpCtx(new HelpCtx(PropertiesAction.class));
        panel.putClientProperty("contentTitle", null);  // NOI18N
        panel.putClientProperty("DialogDescriptor", dd); // NOI18N

        Dialog dialog = DialogDisplayer.getDefault().createDialog(dd);
        dialog.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PropertiesAction.class, "ACSD_Properties_Dialog")); // NOI18N
        dialog.pack();
        dialog.setVisible(true);
        if (dd.getValue() == okButton) {
            gitProperties.updateLastSelection();
            gitProperties.setProperties();
        }
    }
}
