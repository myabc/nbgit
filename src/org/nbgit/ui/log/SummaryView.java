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
package org.nbgit.ui.log;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.nbgit.Git;
import org.nbgit.GitModuleConfig;
import org.nbgit.GitProgressSupport;
import org.nbgit.ui.diff.DiffSetupSource;
import org.nbgit.ui.update.RevertModificationsAction;
import org.nbgit.util.GitUtils;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/**
 * Shows Search History results in a JList.
 *
 * @author Maros Sandor
 */
class SummaryView implements MouseListener, ComponentListener, MouseMotionListener, DiffSetupSource {

    private static final String SUMMARY_DIFF_PROPERTY = "Summary-Diff-";
    private static final String SUMMARY_REVERT_PROPERTY = "Summary-Revert-";
    private static final String SUMMARY_EXPORTDIFFS_PROPERTY = "Summary-ExportDiffs-";
    private final SearchHistoryPanel master;
    private JList resultsList;
    private JScrollPane scrollPane;
    private final List dispResults;
    private String message;
    private AttributeSet searchHiliteAttrs;
    private List<RepositoryRevision> results;

    public SummaryView(SearchHistoryPanel master, List<RepositoryRevision> results) {
        this.master = master;
        this.results = results;
        this.dispResults = expandResults(results);
        FontColorSettings fcs = MimeLookup.getLookup(MimePath.get("text/x-java")).lookup(FontColorSettings.class); // NOI18N
        searchHiliteAttrs = fcs.getFontColors("highlight-search"); // NOI18N
        message = master.getCriteria().getCommitMessage();
        resultsList = new JList(new SummaryListModel());
        resultsList.setFixedCellHeight(-1);
        resultsList.addMouseListener(this);
        resultsList.addMouseMotionListener(this);
        resultsList.setCellRenderer(new SummaryCellRenderer());
        resultsList.getAccessibleContext().setAccessibleName(NbBundle.getMessage(SummaryView.class, "ACSN_SummaryView_List")); // NOI18N
        resultsList.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(SummaryView.class, "ACSD_SummaryView_List")); // NOI18N
        scrollPane = new JScrollPane(resultsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        master.addComponentListener(this);
        resultsList.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F10, KeyEvent.SHIFT_DOWN_MASK), "org.openide.actions.PopupAction");
        resultsList.getActionMap().put("org.openide.actions.PopupAction", new AbstractAction() {

            public void actionPerformed(ActionEvent e) {
                onPopup(org.netbeans.modules.versioning.util.Utils.getPositionForPopup(resultsList));
            }
        });
    }

    public void componentResized(ComponentEvent e) {
        int[] selection = resultsList.getSelectedIndices();
        resultsList.setModel(new SummaryListModel());
        resultsList.setSelectedIndices(selection);
    }

    public void componentHidden(ComponentEvent e) {
        // not interested
    }

    public void componentMoved(ComponentEvent e) {
        // not interested
    }

    public void componentShown(ComponentEvent e) {
        // not interested
    }

    @SuppressWarnings("unchecked")
    private List expandResults(List<RepositoryRevision> results) {
        ArrayList newResults = new ArrayList(results.size());
        for (RepositoryRevision repositoryRevision : results) {
            newResults.add(repositoryRevision);
            List<RepositoryRevision.Event> events = repositoryRevision.getEvents();
            for (RepositoryRevision.Event event : events) {
                newResults.add(event);
            }
        }
        return newResults;
    }

    public void mouseClicked(MouseEvent e) {
        int idx = resultsList.locationToIndex(e.getPoint());
        if (idx == -1) {
            return;
        }
        Rectangle rect = resultsList.getCellBounds(idx, idx);
        Point p = new Point(e.getX() - rect.x, e.getY() - rect.y);
        Rectangle diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_DIFF_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            diffPrevious(idx);
        }
        diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_REVERT_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            revertModifications(new int[]{idx});
        }
    }

    public void mouseEntered(MouseEvent e) {
        // not interested
    }

    public void mouseExited(MouseEvent e) {
        // not interested
    }

    public void mousePressed(MouseEvent e) {
        if (!master.isIncomingSearch() && e.isPopupTrigger()) {
            onPopup(e);
        }
    }

    public void mouseReleased(MouseEvent e) {
        if (!master.isIncomingSearch() && e.isPopupTrigger()) {
            onPopup(e);
        }
    }

    public void mouseDragged(MouseEvent e) {
    }

    public void mouseMoved(MouseEvent e) {
        int idx = resultsList.locationToIndex(e.getPoint());
        if (idx == -1) {
            return;
        }
        Rectangle rect = resultsList.getCellBounds(idx, idx);
        Point p = new Point(e.getX() - rect.x, e.getY() - rect.y);
        Rectangle diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_DIFF_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            resultsList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return;
        }
        diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_REVERT_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            resultsList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return;
        }
        diffBounds = (Rectangle) resultsList.getClientProperty(SUMMARY_EXPORTDIFFS_PROPERTY + idx); // NOI18N
        if (diffBounds != null && diffBounds.contains(p)) {
            resultsList.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return;
        }
        resultsList.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public Collection getSetups() {
        Node[] nodes = TopComponent.getRegistry().getActivatedNodes();
        if (nodes.length == 0) {
            return master.getSetups(results.toArray(new RepositoryRevision[results.size()]), new RepositoryRevision.Event[0]);
        }
        Set<RepositoryRevision.Event> events = new HashSet<RepositoryRevision.Event>();
        Set<RepositoryRevision> revisions = new HashSet<RepositoryRevision>();

        int[] sel = resultsList.getSelectedIndices();
        for (int i : sel) {
            Object revCon = dispResults.get(i);
            if (revCon instanceof RepositoryRevision) {
                revisions.add((RepositoryRevision) revCon);
            } else {
                events.add((RepositoryRevision.Event) revCon);
            }
        }
        return master.getSetups(revisions.toArray(new RepositoryRevision[revisions.size()]), events.toArray(new RepositoryRevision.Event[events.size()]));
    }

    public String getSetupDisplayName() {
        return null;
    }

    private void onPopup(MouseEvent e) {
        onPopup(e.getPoint());
    }

    private void onPopup(Point p) {
        int[] sel = resultsList.getSelectedIndices();
        if (sel.length == 0) {
            int idx = resultsList.locationToIndex(p);
            if (idx == -1) {
                return;
            }
            resultsList.setSelectedIndex(idx);
            sel = new int[]{idx};
        }
        final int[] selection = sel;

        JPopupMenu menu = new JPopupMenu();

        String previousRevision = null;
        RepositoryRevision container = null;
        final RepositoryRevision.Event[] drev;

        Object revCon = dispResults.get(selection[0]);


        boolean noExDeletedExistingFiles = true;
        boolean revisionSelected;
        boolean missingFile = false;
        boolean oneRevisionMultiselected = true;

        if (revCon instanceof RepositoryRevision) {
            revisionSelected = true;
            container = (RepositoryRevision) dispResults.get(selection[0]);
            drev = new RepositoryRevision.Event[0];
            oneRevisionMultiselected = true;
            noExDeletedExistingFiles = true;
        } else {
            revisionSelected = false;
            drev = new RepositoryRevision.Event[selection.length];

            for (int i = 0; i < selection.length; i++) {
                drev[i] = (RepositoryRevision.Event) dispResults.get(selection[i]);

                if (!missingFile && drev[i].getFile() == null) {
                    missingFile = true;
                }
                if (oneRevisionMultiselected && i > 0 &&
                        drev[0].getLogInfoHeader().getRevision().equals(drev[i].getLogInfoHeader().getRevision())) {
                    oneRevisionMultiselected = false;
                }
                if (drev[i].getFile() != null && drev[i].getFile().exists() && drev[i].getChangedPath().getAction() == 'D') {
                    noExDeletedExistingFiles = false;
                }
            }
            container = drev[0].getLogInfoHeader();
        }
        long revision = 0; //Long.parseLong(container.getLog().getRevision());

        final boolean rollbackToEnabled = !missingFile && !revisionSelected && oneRevisionMultiselected;
        final boolean rollbackChangeEnabled = !missingFile && oneRevisionMultiselected && (drev.length == 0 || noExDeletedExistingFiles); // drev.length == 0 => the whole revision was selected
        final boolean viewEnabled = selection.length == 1 && !revisionSelected && drev[0].getFile() != null && drev[0].getFile().exists() && !drev[0].getFile().isDirectory();
        final boolean diffToPrevEnabled = selection.length == 1;

        if (revision > 0) {
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_DiffToPrevious", "" + previousRevision)) { // NOI18N

                {
                    setEnabled(diffToPrevEnabled);
                }

                public void actionPerformed(ActionEvent e) {
                    diffPrevious(selection[0]);
                }
            }));

        /* TBD - Support Rollback Changes:
         * Need to figure out how to implement SVN functionality to allow you to rollback a change
         * currently for svn this cmd runs: merge -R <revX>:<revY> it basically diffs the two revs
         * and creates a patch which is then applied ot the file in the working dir, effectively removing
         * the change applied between revX and revY
         * In hg we'd need to do a hg diff -r revX -r revY, generate a patch and then apply it to the working dir copy
         * /
        menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_RollbackChange")) { // NOI18N
        {
        setEnabled(false); // rollbackChangeEnabled);
        }
        public void actionPerformed(ActionEvent e) {
        revertModifications(selection);
        }
        }));
         */
        }
        if (!revisionSelected) {
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_RollbackTo", "" + revision)) { // NOI18N

                {
                    setEnabled(rollbackToEnabled);
                }

                public void actionPerformed(ActionEvent e) {
                    revertModifications(selection);
                }

                /*public void actionPerformed(ActionEvent e) {
                RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                rollback(drev);
                }
                });
                }*/
            }));
            menu.add(new JMenuItem(new AbstractAction(NbBundle.getMessage(SummaryView.class, "CTL_SummaryView_View")) { // NOI18N

                {
                    setEnabled(viewEnabled);
                }

                public void actionPerformed(ActionEvent e) {
                    RequestProcessor.getDefault().post(new Runnable() {

                        public void run() {
                            view(selection[0]);
                        }
                    });
                }
            }));
        }

        menu.show(resultsList, p.x, p.y);
    }

    /**
     * Overwrites local file with this revision.
     *
     * @param event
     */
    static void rollback(RepositoryRevision.Event event) {
        rollback(new RepositoryRevision.Event[]{event});
    }

    /**
     * Overwrites local file with this revision.
     *
     * @param event
     */
    static void rollback(final RepositoryRevision.Event[] events) {
        String repository = events[0].getLogInfoHeader().getRepositoryRootUrl();
        RequestProcessor rp = Git.getInstance().getRequestProcessor(repository);
        GitProgressSupport support = new GitProgressSupport() {

            public void perform() {
                for (RepositoryRevision.Event event : events) {
                    rollback(event, this);
                }
            }
        };
        support.start(rp, repository, NbBundle.getMessage(SummaryView.class, "MSG_Rollback_Progress")); // NOI18N

    }

    private static void rollback(RepositoryRevision.Event event, GitProgressSupport progress) {
        File file = event.getFile();
        File parent = file.getParentFile();
        parent.mkdirs();
        File oldFile = null;
        try {
            oldFile = GitUtils.getFileRevision(file, event.getLogInfoHeader().getRevision());
            file.delete();
            FileUtil.copyFile(FileUtil.toFileObject(oldFile), FileUtil.toFileObject(parent), file.getName(), "");
        } catch (IOException e) {
            ErrorManager.getDefault().notify(e);
        } finally {
            if (oldFile != null && oldFile != file)
                oldFile.delete();
        }
    }

    private void revertModifications(int[] selection) {
        Set<RepositoryRevision.Event> events = new HashSet<RepositoryRevision.Event>();
        Set<RepositoryRevision> revisions = new HashSet<RepositoryRevision>();
        for (int idx : selection) {
            Object o = dispResults.get(idx);
            if (o instanceof RepositoryRevision) {
                revisions.add((RepositoryRevision) o);
            } else {
                events.add((RepositoryRevision.Event) o);
            }
        }
        revert(master, revisions.toArray(new RepositoryRevision[revisions.size()]), events.toArray(new RepositoryRevision.Event[events.size()]));
    }

    static void revert(final SearchHistoryPanel master, final RepositoryRevision[] revisions, final RepositoryRevision.Event[] events) {
        String url = master.getSearchRepositoryRootUrl();
        RequestProcessor rp = Git.getInstance().getRequestProcessor(url);
        GitProgressSupport support = new GitProgressSupport() {

            public void perform() {
                revertImpl(master, revisions, events, this);
            }
        };
        support.start(rp, url, NbBundle.getMessage(SummaryView.class, "MSG_Revert_Progress")); // NOI18N
    }

    private static void revertImpl(SearchHistoryPanel master, RepositoryRevision[] revisions, RepositoryRevision.Event[] events, GitProgressSupport progress) {
        List<File> revertFiles = new ArrayList<File>();
        boolean doBackup = GitModuleConfig.getDefault().getBackupOnRevertModifications();
        for (RepositoryRevision revision : revisions) {
            File root = new File(revision.getRepositoryRootUrl());
            for (RepositoryRevision.Event event : revision.getEvents()) {
                if (event.getFile() == null) {
                    continue;
                }
                revertFiles.add(event.getFile());
            }
            RevertModificationsAction.performRevert(
                    root, revision.getRevision(), revertFiles, doBackup, progress.getLogger());

            revertFiles.clear();
        }

        Map<File, List<RepositoryRevision.Event>> revertMap = new HashMap<File, List<RepositoryRevision.Event>>();
        for (RepositoryRevision.Event event : events) {
            if (event.getFile() == null) {
                continue;
            }
            File root = Git.getInstance().getTopmostManagedParent(event.getFile());
            if (revertMap == null) {
                revertMap = new HashMap<File, List<RepositoryRevision.Event>>();
            }
            List<RepositoryRevision.Event> revEvents = revertMap.get(root);
            if (revEvents == null) {
                revEvents = new ArrayList<RepositoryRevision.Event>();
                revertMap.put(root, revEvents);
            }
            revEvents.add(event);
        }
        if (events != null && events.length > 0 && revertMap != null && !revertMap.isEmpty()) {
            Set<File> roots = revertMap.keySet();
            for (File root : roots) {
                List<RepositoryRevision.Event> revEvents = revertMap.get(root);
                for (RepositoryRevision.Event event : revEvents) {
                    if (event.getFile() == null) {
                        continue;
                    }
                    revertFiles.add(event.getFile());
                }
                if (revEvents != null && !revEvents.isEmpty()) {
                    // Assuming all files in a given repository reverting to same revision
            /*
                    RevertModificationsAction.performRevert(
                    root, revEvents.get(0).getLogInfoHeader().getLog().getRevision(), revertFiles, doBackup, progress.getLogger());
                     */
                }
            }
        }

    }

    private void view(int idx) {
        Object o = dispResults.get(idx);
        if (o instanceof RepositoryRevision.Event) {
            try {
                RepositoryRevision.Event drev = (RepositoryRevision.Event) o;
                File file = GitUtils.getFileRevision(drev.getFile(), drev.getLogInfoHeader().getRevision());

                FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(file));
                org.netbeans.modules.versioning.util.Utils.openFile(fo, drev.getLogInfoHeader().getRevision());
            } catch (IOException ex) {
                // Ignore if file not available in cache
            }
        }
    }

    private void diffPrevious(int idx) {
        Object o = dispResults.get(idx);
        if (o instanceof RepositoryRevision.Event) {
            RepositoryRevision.Event drev = (RepositoryRevision.Event) o;
            master.showDiff(drev);
        } else {
            RepositoryRevision container = (RepositoryRevision) o;
            master.showDiff(container);
        }
    }

    public JComponent getComponent() {
        return scrollPane;
    }

    private class SummaryListModel extends AbstractListModel {

        public int getSize() {
            return dispResults.size();
        }

        public Object getElementAt(int index) {
            return dispResults.get(index);
        }
    }

    private class SummaryCellRenderer extends JPanel implements ListCellRenderer {

        private static final String FIELDS_SEPARATOR = "        "; // NOI18N
        private static final double DARKEN_FACTOR = 0.95;
        private Style selectedStyle;
        private Style normalStyle;
        private Style filenameStyle;
        private Style indentStyle;
        private Style noindentStyle;
        private Style hiliteStyle;
        private JTextPane textPane = new JTextPane();
        private JPanel actionsPane = new JPanel();
        private DateFormat defaultFormat;
        private int index;
        private HyperlinkLabel diffLink;
        private HyperlinkLabel revertLink;

        public SummaryCellRenderer() {
            selectedStyle = textPane.addStyle("selected", null); // NOI18N
            StyleConstants.setForeground(selectedStyle, UIManager.getColor("List.selectionForeground")); // NOI18N
            normalStyle = textPane.addStyle("normal", null); // NOI18N
            StyleConstants.setForeground(normalStyle, UIManager.getColor("List.foreground")); // NOI18N
            filenameStyle = textPane.addStyle("filename", normalStyle); // NOI18N
            StyleConstants.setBold(filenameStyle, true);
            indentStyle = textPane.addStyle("indent", null); // NOI18N
            StyleConstants.setLeftIndent(indentStyle, 50);
            noindentStyle = textPane.addStyle("noindent", null); // NOI18N
            StyleConstants.setLeftIndent(noindentStyle, 0);
            defaultFormat = DateFormat.getDateTimeInstance();

            hiliteStyle = textPane.addStyle("hilite", normalStyle); // NOI18N
            Color c = (Color) searchHiliteAttrs.getAttribute(StyleConstants.Background);
            if (c != null) {
                StyleConstants.setBackground(hiliteStyle, c);
            }
            c = (Color) searchHiliteAttrs.getAttribute(StyleConstants.Foreground);
            if (c != null) {
                StyleConstants.setForeground(hiliteStyle, c);
            }
            setLayout(new BorderLayout());
            add(textPane);
            add(actionsPane, BorderLayout.PAGE_END);
            actionsPane.setLayout(new FlowLayout(FlowLayout.TRAILING, 2, 5));

            diffLink = new HyperlinkLabel();
            diffLink.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
            actionsPane.add(diffLink);

            revertLink = new HyperlinkLabel();
            revertLink.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
            //actionsPane.add(revertLink);

            textPane.setBorder(null);
        }

        public Color darker(Color c) {
            return new Color(Math.max((int) (c.getRed() * DARKEN_FACTOR), 0),
                    Math.max((int) (c.getGreen() * DARKEN_FACTOR), 0),
                    Math.max((int) (c.getBlue() * DARKEN_FACTOR), 0));
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value instanceof RepositoryRevision) {
                renderContainer(list, (RepositoryRevision) value, index, isSelected);
            } else {
                renderRevision(list, (RepositoryRevision.Event) value, index, isSelected);
            }
            return this;
        }

        private void renderContainer(JList list, RepositoryRevision container, int index, boolean isSelected) {

            StyledDocument sd = textPane.getStyledDocument();

            Style style;
            Color backgroundColor;
            Color foregroundColor;

            if (isSelected) {
                foregroundColor = UIManager.getColor("List.selectionForeground"); // NOI18N
                backgroundColor = UIManager.getColor("List.selectionBackground"); // NOI18N
                style = selectedStyle;
            } else {
                foregroundColor = UIManager.getColor("List.foreground"); // NOI18N
                backgroundColor = UIManager.getColor("List.background"); // NOI18N
                backgroundColor = darker(backgroundColor);
                style = normalStyle;
            }
            textPane.setBackground(backgroundColor);
            actionsPane.setBackground(backgroundColor);

            this.index = index;

            try {
                sd.remove(0, sd.getLength());
                sd.setParagraphAttributes(0, sd.getLength(), noindentStyle, false);

                sd.insertString(0, container.getRevision(), null); // NOI18N
                sd.setCharacterAttributes(0, sd.getLength(), filenameStyle, false);
                sd.insertString(sd.getLength(), FIELDS_SEPARATOR + container.getAuthor(), null);
                sd.insertString(sd.getLength(), FIELDS_SEPARATOR + defaultFormat.format(container.getDate()), null);

                String commitMessage = container.getMessage();
                if (commitMessage.endsWith("\n")) {
                    commitMessage = commitMessage.substring(0, commitMessage.length() - 1); // NOI18N
                }
                sd.insertString(sd.getLength(), "\n", null);

                sd.insertString(sd.getLength(), commitMessage, null);

                if (message != null && !isSelected) {
                    int idx = commitMessage.indexOf(message);
                    if (idx != -1) {
                        int len = commitMessage.length();
                        int doclen = sd.getLength();
                        sd.setCharacterAttributes(doclen - len + idx, message.length(), hiliteStyle, false);
                    }
                }

                resizePane(commitMessage, list.getFontMetrics(list.getFont()));
                sd.setCharacterAttributes(0, Integer.MAX_VALUE, style, false);
            } catch (BadLocationException e) {
                ErrorManager.getDefault().notify(e);
            }

            actionsPane.setVisible(true);
            if (!master.isIncomingSearch()) {
                diffLink.set(NbBundle.getMessage(SummaryView.class, "CTL_Action_Diff"), foregroundColor, backgroundColor);// NOI18N
                revertLink.set(NbBundle.getMessage(SummaryView.class, "CTL_Action_Revert"), foregroundColor, backgroundColor); // NOI18N
            }
        }

        private void renderRevision(JList list, RepositoryRevision.Event dispRevision, final int index, boolean isSelected) {
            Style style;
            StyledDocument sd = textPane.getStyledDocument();

            Color backgroundColor;
            Color foregroundColor;

            if (isSelected) {
                foregroundColor = UIManager.getColor("List.selectionForeground"); // NOI18N
                backgroundColor = UIManager.getColor("List.selectionBackground"); // NOI18N
                style = selectedStyle;
            } else {
                foregroundColor = UIManager.getColor("List.foreground"); // NOI18N
                backgroundColor = UIManager.getColor("List.background"); // NOI18N
                style = normalStyle;
            }
            textPane.setBackground(backgroundColor);
            actionsPane.setVisible(false);

            this.index = -1;
            try {
                sd.remove(0, sd.getLength());
                sd.setParagraphAttributes(0, sd.getLength(), indentStyle, false);

                sd.insertString(sd.getLength(), String.valueOf(dispRevision.getChangedPath().getAction()), null);
                sd.insertString(sd.getLength(), FIELDS_SEPARATOR + dispRevision.getChangedPath().getPath(), null);

                sd.setCharacterAttributes(0, Integer.MAX_VALUE, style, false);
                resizePane(sd.getText(0, sd.getLength() - 1), list.getFontMetrics(list.getFont()));
            } catch (BadLocationException e) {
                ErrorManager.getDefault().notify(e);
            }
        }

        @SuppressWarnings("empty-statement")
        private void resizePane(String text, FontMetrics fm) {
            if (text == null) {
                text = "";
            }
            int width = master.getWidth();
            if (width > 0) {
                Rectangle2D rect = fm.getStringBounds(text, textPane.getGraphics());
                int nlc, i;
                for (nlc = -1      , i = 0; i != -1; i = text.indexOf('\n', i + 1), nlc++);
                nlc++;
                int lines = (int) (rect.getWidth() / (width - 80) + 1);
                int ph = fm.getHeight() * (lines + nlc) + 0;
                textPane.setPreferredSize(new Dimension(width - 50, ph));
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (index == -1) {
                return;
            }
            Rectangle apb = actionsPane.getBounds();

            {
                Rectangle bounds = diffLink.getBounds();
                bounds.setBounds(bounds.x, bounds.y + apb.y, bounds.width, bounds.height);
                resultsList.putClientProperty(SUMMARY_DIFF_PROPERTY + index, bounds); // NOI18N
            }

            Rectangle bounds = revertLink.getBounds();
            bounds.setBounds(bounds.x, bounds.y + apb.y, bounds.width, bounds.height);
            resultsList.putClientProperty(SUMMARY_REVERT_PROPERTY + index, bounds); // NOI18N
        }
    }

    private static class HyperlinkLabel extends JLabel {

        public HyperlinkLabel() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void set(String text, Color foreground, Color background) {
            StringBuilder sb = new StringBuilder(100);
            if (foreground.equals(UIManager.getColor("List.foreground"))) { // NOI18N
                sb.append("<html><a href=\"\">"); // NOI18N
                sb.append(text);
                sb.append("</a>"); // NOI18N
            } else {
                sb.append("<html><a href=\"\" style=\"color:"); // NOI18N
                sb.append("rgb("); // NOI18N
                sb.append(foreground.getRed());
                sb.append(","); // NOI18N
                sb.append(foreground.getGreen());
                sb.append(","); // NOI18N
                sb.append(foreground.getBlue());
                sb.append(")"); // NOI18N
                sb.append("\">"); // NOI18N
                sb.append(text);
                sb.append("</a>"); // NOI18N
            }
            setText(sb.toString());
            setBackground(background);
        }
    }
}
