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
package org.nbgit.options;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.nbgit.Git;
import org.nbgit.GitAnnotator;
import org.nbgit.GitModuleConfig;
import org.nbgit.util.HtmlFormatter;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

final class GitOptionsPanelController extends OptionsPanelController implements ActionListener {

    private GitPanel panel;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;

    public GitOptionsPanelController() {
        panel = new GitPanel(this);

        String tooltip = NbBundle.getMessage(GitPanel.class, "GitPanel.annotationTextField.toolTipText", GitAnnotator.LABELS); // NOI18N

        panel.annotationTextField.setToolTipText(tooltip);
        panel.addButton.addActionListener(this);
    }

    public void update() {
        getPanel().load();
        changed = false;
    }

    public void applyChanges() {
        if (!validateFields()) {
            return;
        }
        getPanel().store();
        // {folder} variable setting
        HtmlFormatter.getInstance().refresh();
        Git.getInstance().refreshAllAnnotations();

        changed = false;
    }

    public void cancel() {
        // need not do anything special, if no changes have been persisted yet
    }

    public boolean isValid() {
        return getPanel().valid();
    }

    public boolean isChanged() {
        return changed;
    }

    public HelpCtx getHelpCtx() {
        return new HelpCtx(GitOptionsPanelController.class);
    }

    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        pcs.removePropertyChangeListener(l);
    }

    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == panel.addButton) {
            onAddClick();
        }
    }

    private Boolean validateFields() {
        String username = panel.emailTextField.getText();
        if (!GitModuleConfig.getDefault().isUserNameValid(username)) {
            JOptionPane.showMessageDialog(null,
                    NbBundle.getMessage(GitPanel.class, "MSG_WARN_USER_NAME_TEXT"), // NOI18N
                    NbBundle.getMessage(GitPanel.class, "MSG_WARN_FIELD_TITLE"), // NOI18N
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private GitPanel getPanel() {
        if (panel == null) {
            panel = new GitPanel(this);
        }
        return panel;
    }

    void changed() {
        if (!changed) {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }

    private class LabelVariable {

        private String description;
        private String variable;

        public LabelVariable(String variable, String description) {
            this.description = description;
            this.variable = variable;
        }

        @Override
        public String toString() {
            return description;
        }

        public String getDescription() {
            return description;
        }

        public String getVariable() {
            return variable;
        }
    }

    private void onAddClick() {
        LabelsPanel labelsPanel = new LabelsPanel();
        List<LabelVariable> variables = new ArrayList<LabelVariable>(GitAnnotator.LABELS.length);
        for (int i = 0; i < GitAnnotator.LABELS.length; i++) {
            LabelVariable variable = new LabelVariable(
                    GitAnnotator.LABELS[i],
                    "{" + GitAnnotator.LABELS[i] + "} - " + NbBundle.getMessage(GitPanel.class, "GitPanel.label." + GitAnnotator.LABELS[i]) // NOI18N
                    );
            variables.add(variable);
        }
        labelsPanel.labelsList.setListData(variables.toArray(new LabelVariable[variables.size()]));

        String title = NbBundle.getMessage(GitPanel.class, "GitPanel.labelVariables.title"); // NOI18N
        String acsd = NbBundle.getMessage(GitPanel.class, "GitPanel.labelVariables.acsd"); // NOI18N

        DialogDescriptor dialogDescriptor = new DialogDescriptor(labelsPanel, title);
        dialogDescriptor.setModal(true);
        dialogDescriptor.setValid(true);

        final Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        dialog.getAccessibleContext().setAccessibleDescription(acsd);

        labelsPanel.labelsList.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    dialog.setVisible(false);
                }
            }
        });

        dialog.setVisible(true);

        if (DialogDescriptor.OK_OPTION.equals(dialogDescriptor.getValue())) {

            Object[] selection = labelsPanel.labelsList.getSelectedValues();

            String variable = ""; // NOI18N
            for (int i = 0; i < selection.length; i++) {
                variable += "{" + ((LabelVariable) selection[i]).getVariable() + "}"; // NOI18N
            }

            String annotation = panel.annotationTextField.getText();

            int pos = panel.annotationTextField.getCaretPosition();
            if (pos < 0) {
                pos = annotation.length();
            }
            StringBuffer sb = new StringBuffer(annotation.length() + variable.length());
            sb.append(annotation.substring(0, pos));
            sb.append(variable);
            if (pos < annotation.length()) {
                sb.append(annotation.substring(pos, annotation.length()));
            }
            panel.annotationTextField.setText(sb.toString());
            panel.annotationTextField.requestFocus();
            panel.annotationTextField.setCaretPosition(pos + variable.length());
        }
    }

    private void onManageClick() {
        final PropertiesPanel panel = new PropertiesPanel();

        final PropertiesTable propTable;

        propTable = new PropertiesTable(panel.labelForTable, PropertiesTable.PROPERTIES_COLUMNS, new String[]{PropertiesTableModel.COLUMN_NAME_VALUE});

        panel.setPropertiesTable(propTable);

        JComponent component = propTable.getComponent();

        panel.propsPanel.setLayout(new BorderLayout());

        panel.propsPanel.add(component, BorderLayout.CENTER);

        GitExtProperties gitProperties = new GitExtProperties(panel, propTable, null);

        DialogDescriptor dd = new DialogDescriptor(panel, NbBundle.getMessage(GitOptionsPanelController.class, "CTL_PropertiesDialog_Title", null), true, null); // NOI18N
        final JButton okButton = new JButton(NbBundle.getMessage(GitOptionsPanelController.class, "CTL_Properties_Action_OK")); // NOI18N
        okButton.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(GitOptionsPanelController.class, "CTL_Properties_Action_OK")); // NOI18N
        final JButton cancelButton = new JButton(NbBundle.getMessage(GitOptionsPanelController.class, "CTL_Properties_Action_Cancel")); // NOI18N
        cancelButton.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(GitOptionsPanelController.class, "CTL_Properties_Action_Cancel")); // NOI18N
        dd.setOptions(new Object[]{okButton, cancelButton}); // NOI18N
        dd.setHelpCtx(new HelpCtx(GitOptionsPanelController.class));
        panel.putClientProperty("contentTitle", null);  // NOI18N
        panel.putClientProperty("DialogDescriptor", dd); // NOI18N
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dd);
        dialog.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(GitOptionsPanelController.class, "CTL_PropertiesDialog_Title")); // NOI18N

        dialog.pack();
        dialog.setVisible(true);
        if (dd.getValue() == okButton) {
            gitProperties.setProperties();
        }
    }
}
