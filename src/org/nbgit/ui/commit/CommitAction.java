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
package org.nbgit.ui.commit;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.nbgit.StatusInfo;
import org.nbgit.StatusCache;
import org.nbgit.Git;
import org.nbgit.GitModuleConfig;
import org.nbgit.GitProgressSupport;
import org.nbgit.OutputLogger;
import org.nbgit.ui.GitFileNode;
import org.nbgit.ui.ContextAction;
import org.nbgit.util.GitCommand;
import org.nbgit.util.GitProjectUtils;
import org.nbgit.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.util.DialogBoundsPreserver;
import org.netbeans.modules.versioning.util.Utils;
import org.netbeans.modules.versioning.util.VersioningEvent;
import org.netbeans.modules.versioning.util.VersioningListener;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Commit action for Git:
 * git commit - commit the specified files or all outstanding changes
 *
 * @author John Rice
 */
public class CommitAction extends ContextAction {

    static final String RECENT_COMMIT_MESSAGES = "recentCommitMessage"; // NOI18N
    static final String SIGN_OFF_MESSAGE = "signOffMessage"; // NOI18N

    public CommitAction(String name, VCSContext context) {
        super(name, context);
    }

    @Override
    public boolean isEnabled() {
        StatusCache cache = Git.getInstance().getStatusCache();
        return cache.containsFileOfStatus(context, StatusInfo.STATUS_LOCAL_CHANGE);
    }

    public void performAction(ActionEvent e) {
        final File root = GitUtils.getRootFile(context);
        if (root == null) {
            OutputLogger logger = OutputLogger.getLogger(Git.GIT_OUTPUT_TAB_TITLE);
            logger.outputInRed(NbBundle.getMessage(CommitAction.class, "MSG_COMMIT_TITLE")); // NOI18N
            logger.outputInRed(NbBundle.getMessage(CommitAction.class, "MSG_COMMIT_TITLE_SEP")); // NOI18N
            logger.outputInRed(
                    NbBundle.getMessage(CommitAction.class, "MSG_COMMIT_NOT_SUPPORTED_INVIEW_INFO")); // NOI18N
            logger.output(""); // NOI18N
            logger.closeLog();
            JOptionPane.showMessageDialog(null,
                    NbBundle.getMessage(CommitAction.class, "MSG_COMMIT_NOT_SUPPORTED_INVIEW"),// NOI18N
                    NbBundle.getMessage(CommitAction.class, "MSG_COMMIT_NOT_SUPPORTED_INVIEW_TITLE"),// NOI18N
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String contentTitle = Utils.getContextDisplayName(context);

        commit(contentTitle, context);
    }

    public static void commit(String contentTitle, final VCSContext ctx) {
        StatusCache cache = Git.getInstance().getStatusCache();
        File[] roots = ctx.getRootFiles().toArray(new File[ctx.getRootFiles().size()]);
        if (roots == null || roots.length == 0) {
            return;
        }
        final File repository = GitUtils.getRootFile(ctx);
        if (repository == null) {
            return;
        }
        String projName = GitProjectUtils.getProjectName(repository);
        if (projName == null) {
            File projFile = GitUtils.getProjectFile(ctx);
            projName = GitProjectUtils.getProjectName(projFile);
        }
        final String prjName = projName;

        File[][] split = Utils.splitFlatOthers(roots);
        List<File> fileList = new ArrayList<File>();
        for (int c = 0; c < split.length; c++) {
            roots = split[c];
            boolean recursive = c == 1;
            if (recursive) {
                File[] files = cache.listFiles(ctx, StatusInfo.STATUS_LOCAL_CHANGE);
                for (int i = 0; i < files.length; i++) {
                    for (int r = 0; r < roots.length; r++) {
                        if (GitUtils.isParentOrEqual(roots[r], files[i])) {
                            if (!fileList.contains(files[i])) {
                                fileList.add(files[i]);
                            }
                        }
                    }
                }
            } else {
                File[] files = GitUtils.flatten(roots, StatusInfo.STATUS_LOCAL_CHANGE);
                for (int i = 0; i < files.length; i++) {
                    if (!fileList.contains(files[i])) {
                        fileList.add(files[i]);
                    }
                }
            }
        }

        if (fileList.size() == 0) {
            return;        // show commit dialog
        }
        final CommitPanel panel = new CommitPanel();
        final CommitTable data = new CommitTable(panel.filesLabel, CommitTable.COMMIT_COLUMNS, new String[]{CommitTableModel.COLUMN_NAME_PATH});

        panel.setCommitTable(data);

        Properties props = GitModuleConfig.getDefault().getProperties(repository);
        String signOff = props.getProperty("nbgit.signoff");
        if (signOff.equals("yes")) {
            signOff = "Signed-off-by: " +props.getProperty("user.name")
                    + " <" + props.getProperty("user.email") + ">";
            panel.putClientProperty(SIGN_OFF_MESSAGE, signOff);
        }

        GitFileNode[] nodes;
        ArrayList<GitFileNode> nodesList = new ArrayList<GitFileNode>(fileList.size());

        for (File file : fileList) {
            GitFileNode node = new GitFileNode(file);
            nodesList.add(node);
        }
        nodes = nodesList.toArray(new GitFileNode[fileList.size()]);
        data.setNodes(nodes);

        JComponent component = data.getComponent();
        panel.filesPanel.setLayout(new BorderLayout());
        panel.filesPanel.add(component, BorderLayout.CENTER);

        DialogDescriptor dd = new DialogDescriptor(panel, org.openide.util.NbBundle.getMessage(CommitAction.class, "CTL_CommitDialog_Title", contentTitle)); // NOI18N
        dd.setModal(true);
        final JButton commitButton = new JButton();
        org.openide.awt.Mnemonics.setLocalizedText(commitButton, org.openide.util.NbBundle.getMessage(CommitAction.class, "CTL_Commit_Action_Commit"));
        commitButton.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(CommitAction.class, "ACSN_Commit_Action_Commit"));
        commitButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CommitAction.class, "ACSD_Commit_Action_Commit"));
        final JButton cancelButton = new JButton(org.openide.util.NbBundle.getMessage(CommitAction.class, "CTL_Commit_Action_Cancel")); // NOI18N
        org.openide.awt.Mnemonics.setLocalizedText(cancelButton, org.openide.util.NbBundle.getMessage(CommitAction.class, "CTL_Commit_Action_Cancel"));
        cancelButton.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(CommitAction.class, "ACSN_Commit_Action_Cancel"));
        cancelButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CommitAction.class, "ACSD_Commit_Action_Cancel"));

        commitButton.setEnabled(false);
        dd.setOptions(new Object[]{commitButton, cancelButton});
        dd.setHelpCtx(new HelpCtx(CommitAction.class));
        panel.addVersioningListener(new VersioningListener() {

            public void versioningEvent(VersioningEvent event) {
                refreshCommitDialog(panel, data, commitButton);
            }
        });
        data.getTableModel().addTableModelListener(new TableModelListener() {

            public void tableChanged(TableModelEvent e) {
                refreshCommitDialog(panel, data, commitButton);
            }
        });
        commitButton.setEnabled(containsCommitable(data));

        panel.putClientProperty("contentTitle", contentTitle);  // NOI18N
        panel.putClientProperty("DialogDescriptor", dd); // NOI18N
        final Dialog dialog = DialogDisplayer.getDefault().createDialog(dd);

        dialog.addWindowListener(new DialogBoundsPreserver(GitModuleConfig.getDefault().getPreferences(), "git.commit.dialog")); // NOI18N
        dialog.pack();
        dialog.setVisible(true);

        if (dd.getValue() == commitButton) {

            final Map<GitFileNode, CommitOptions> commitFiles = data.getCommitFiles();
            String origMessage = panel.messageTextArea.getText();
            String stripSpace = props.getProperty("nbgit.stripspace");
            if (stripSpace.equals("yes")) {
                origMessage = stripSpace(origMessage);
            }
            final String message = origMessage;
            org.netbeans.modules.versioning.util.Utils.insert(GitModuleConfig.getDefault().getPreferences(), RECENT_COMMIT_MESSAGES, origMessage, 20);
            RequestProcessor rp = Git.getInstance().getRequestProcessor(repository.getAbsolutePath());
            GitProgressSupport support = new GitProgressSupport() {

                public void perform() {
                    OutputLogger logger = getLogger();
                    performCommit(message, commitFiles, ctx, this, prjName, logger);
                }
            };
            support.start(rp, repository.getAbsolutePath(), org.openide.util.NbBundle.getMessage(CommitAction.class, "LBL_Commit_Progress")); // NOI18N
        }
    }

    private static boolean containsCommitable(CommitTable data) {
        Map<GitFileNode, CommitOptions> map = data.getCommitFiles();
        for (CommitOptions co : map.values()) {
            if (co != CommitOptions.EXCLUDE) {
                return true;
            }
        }
        return false;
    }

    /**
     * User changed a commit action.
     *
     * @param panel
     * @param commit
     */
    private static void refreshCommitDialog(CommitPanel panel, CommitTable table, JButton commit) {
        ResourceBundle loc = NbBundle.getBundle(CommitAction.class);
        Map<GitFileNode, CommitOptions> files = table.getCommitFiles();
        Set<String> stickyTags = new HashSet<String>();
        boolean conflicts = false;

        boolean enabled = commit.isEnabled();

        for (GitFileNode fileNode : files.keySet()) {

            CommitOptions options = files.get(fileNode);
            if (options == CommitOptions.EXCLUDE) {
                continue;
            //stickyTags.add(GitUtils.getCopy(fileNode.getFile()));
            }
            int status = fileNode.getInformation().getStatus();
            if ((status & StatusInfo.STATUS_REMOTE_CHANGE) != 0 || status == StatusInfo.STATUS_VERSIONED_CONFLICT) {
                enabled = false;
                String msg = (status == StatusInfo.STATUS_VERSIONED_CONFLICT) ? loc.getString("MSG_CommitForm_ErrorConflicts") : // NOI18N
                        loc.getString("MSG_CommitForm_ErrorRemoteChanges"); // NOI18N
                panel.setErrorLabel("<html><font color=\"#002080\">" + msg + "</font></html>");  // NOI18N
                conflicts = true;
            }
        //stickyTags.add(GitUtils.getCopy(fileNode.getFile()));

        }

        if (stickyTags.size() > 1) {
            table.setColumns(new String[]{CommitTableModel.COLUMN_NAME_NAME, CommitTableModel.COLUMN_NAME_BRANCH, CommitTableModel.COLUMN_NAME_STATUS,
                        CommitTableModel.COLUMN_NAME_ACTION, CommitTableModel.COLUMN_NAME_PATH
                    });
        } else {
            table.setColumns(new String[]{CommitTableModel.COLUMN_NAME_NAME, CommitTableModel.COLUMN_NAME_STATUS,
                        CommitTableModel.COLUMN_NAME_ACTION, CommitTableModel.COLUMN_NAME_PATH
                    });
        }
        String contentTitle = (String) panel.getClientProperty("contentTitle"); // NOI18N
// NOI18N
        DialogDescriptor dd = (DialogDescriptor) panel.getClientProperty("DialogDescriptor"); // NOI18N
        String errorLabel;
        if (stickyTags.size() <= 1) {
            String stickyTag = stickyTags.size() == 0 ? null : stickyTags.iterator().next();
            if (stickyTag == null) {
                dd.setTitle(MessageFormat.format(loc.getString("CTL_CommitDialog_Title"), new Object[]{contentTitle})); // NOI18N
                errorLabel = ""; // NOI18N
            } else {
                dd.setTitle(MessageFormat.format(loc.getString("CTL_CommitDialog_Title_Branch"), new Object[]{contentTitle, stickyTag})); // NOI18N
                String msg = MessageFormat.format(loc.getString("MSG_CommitForm_InfoBranch"), new Object[]{stickyTag}); // NOI18N
                errorLabel = "<html><font color=\"#002080\">" + msg + "</font></html>"; // NOI18N
            }
        } else {
            dd.setTitle(MessageFormat.format(loc.getString("CTL_CommitDialog_Title_Branches"), new Object[]{contentTitle})); // NOI18N
            String msg = loc.getString("MSG_CommitForm_ErrorMultipleBranches"); // NOI18N
            errorLabel = "<html><font color=\"#CC0000\">" + msg + "</font></html>"; // NOI18N
        }
        if (!conflicts) {
            panel.setErrorLabel(errorLabel);
            enabled = true;
        }
        commit.setEnabled(enabled && containsCommitable(table));
    }

    private static void performCommit(String message, Map<GitFileNode, CommitOptions> commitFiles,
            VCSContext ctx, GitProgressSupport support, String prjName, OutputLogger logger) {
        StatusCache cache = Git.getInstance().getStatusCache();
        final File repository = GitUtils.getRootFile(ctx);
        List<File> addCandidates = new ArrayList<File>();
        List<File> deleteCandidates = new ArrayList<File>();
        List<File> commitCandidates = new ArrayList<File>();

        List<String> excPaths = new ArrayList<String>();
        List<String> incPaths = new ArrayList<String>();
        for (GitFileNode node : commitFiles.keySet()) {
            if (support.isCanceled()) {
                return;
            }
            CommitOptions option = commitFiles.get(node);
            if (option != CommitOptions.EXCLUDE) {
                int status = cache.getStatus(node.getFile()).getStatus();
                if ((status & StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY) != 0) {
                    addCandidates.add(node.getFile());
                } else if ((status & StatusInfo.STATUS_VERSIONED_DELETEDLOCALLY) != 0) {
                    deleteCandidates.add(node.getFile());
                }
                commitCandidates.add(node.getFile());
                incPaths.add(node.getFile().getAbsolutePath());
            } else {
                excPaths.add(node.getFile().getAbsolutePath());
            }
        }
        if (support.isCanceled()) {
            return;
        }
        if (!excPaths.isEmpty()) {
            GitModuleConfig.getDefault().addExclusionPaths(excPaths);
        }
        if (!incPaths.isEmpty()) {
            GitModuleConfig.getDefault().removeExclusionPaths(incPaths);
        }
        try {
            logger.outputInRed(
                    NbBundle.getMessage(CommitAction.class,
                    "MSG_COMMIT_TITLE")); // NOI18N
            logger.outputInRed(
                    NbBundle.getMessage(CommitAction.class,
                    "MSG_COMMIT_TITLE_SEP")); // NOI18N
            logger.output(message); // NOI18N
            if (addCandidates.size() > 0) {
                GitCommand.doAdd(repository, addCandidates, logger);
                for (File f : addCandidates) {
                    logger.output("git add " + f.getName()); //NOI18N
                }
            }
            if (deleteCandidates.size() > 0) {
                GitCommand.doRemove(repository, deleteCandidates, logger);
                for (File f : deleteCandidates) {
                    logger.output("git delete " + f.getName()); //NOI18N
                }
            }

            GitCommand.doCommit(repository, commitCandidates, message, logger);

            if (commitCandidates.size() == 1) {
                logger.output(
                        NbBundle.getMessage(CommitAction.class,
                        "MSG_COMMIT_INIT_SEP_ONE", commitCandidates.size(), prjName));
            } else {
                logger.output(
                        NbBundle.getMessage(CommitAction.class,
                        "MSG_COMMIT_INIT_SEP", commitCandidates.size(), prjName));
            }
            for (File f : commitCandidates) {
                logger.output("\t" + f.getAbsolutePath()); // NOI18N
            }
        } catch (Exception ex) {
            NotifyDescriptor.Exception e = new NotifyDescriptor.Exception(ex);
            DialogDisplayer.getDefault().notifyLater(e);
        } finally {
            cache.refreshCached(ctx);
            logger.outputInRed(NbBundle.getMessage(CommitAction.class, "MSG_COMMIT_DONE")); // NOI18N
            logger.output(""); // NOI18N
        }
    }

    static String stripSpace(String message) {
        StringBuilder builder = new StringBuilder();
        String[] lines = message.split("\n");
        String sep = "\n";
        int i = 0;

        for (String line : lines) {
            int j = line.length();

            while (j > 0) {
                if (!Character.isWhitespace(line.charAt(j - 1))) {
                    break;
                }
                j--;
            }

            if (j > 0) {
                if (builder.length() > 0) {
                    builder.append(sep);
                }
                builder.append(line, 0, j);
                builder.append("\n");
                sep = "";
            } else if (sep.length() == 0) {
                sep = "\n";
            } else {
                continue;
            }
        }

        return builder.toString();
    }
}
