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
package org.netbeans.modules.git.ui.annotate;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.CharConversionException;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javax.accessibility.Accessible;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import javax.swing.text.View;
import org.netbeans.api.diff.Difference;
import org.netbeans.api.editor.fold.FoldHierarchy;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.BaseTextUI;
import org.netbeans.editor.EditorUI;
import org.netbeans.editor.StatusBar;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.git.Git;
import org.netbeans.modules.git.GitProgressSupport;
import org.netbeans.modules.git.ui.diff.DiffAction;
import org.netbeans.modules.git.ui.update.RevertModifications;
import org.netbeans.modules.git.ui.update.RevertModificationsAction;
import org.netbeans.modules.git.util.GitLogMessage;
import org.netbeans.modules.versioning.util.Utils;
import org.netbeans.spi.diff.DiffProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.xml.XMLUtil;

/**
 * Represents annotation sidebar componnet in editor. It's
 * created by {@link AnnotationBarManager}.
 *
 * <p>It reponds to following external signals:
 * <ul>
 *   <li> {@link #annotate} message
 * </ul>
 *
 * @author Petr Kuzel
 */
final class AnnotationBar extends JComponent implements Accessible, PropertyChangeListener, DocumentListener, ChangeListener, ActionListener, Runnable, ComponentListener {

    /**
     * Target text component for which the annotation bar is aiming.
     */
    private final JTextComponent textComponent;

    /**
     * User interface related to the target text component.
     */
    private final EditorUI editorUI;

    /**
     * Fold hierarchy of the text component user interface.
     */
    private final FoldHierarchy foldHierarchy;

    /** 
     * Document related to the target text component.
     */
    private final BaseDocument doc;

    /**
     * Caret of the target text component.
     */
    private final Caret caret;

    /**
     * Caret batch timer launched on receiving
     * annotation data structures (AnnotateLine).
     */
    private Timer caretTimer;

    /**
     * Controls annotation bar visibility.
     */
    private boolean annotated;

    /**
     * Maps document {@link javax.swing.text.Element}s (representing lines) to
     * {@link AnnotateLine}. <code>null</code> means that
     * no data are available, yet. So alternative
     * {@link #elementAnnotationsSubstitute} text shoudl be used.
     *
     * @thread it is accesed from multiple threads all mutations
     * and iterations must be under elementAnnotations lock,
     */
    private Map<Element, AnnotateLine> elementAnnotations;

    /**
     * Represents text that should be displayed in
     * visible bar with yet <code>null</code> elementAnnotations.
     */
    private String elementAnnotationsSubstitute;
    
    private Color backgroundColor = Color.WHITE;
    private Color foregroundColor = Color.BLACK;
    private Color selectedColor = Color.BLUE;

    /**
     * Most recent status message.
     */
    private String recentStatusMessage;
    
    /**
     * Revision associated with caret line.
     */
    private String recentRevision;
    
    /**
     * File for revision associated with caret line.
     */
    private File recentFile;
    
    /**
     * Request processor to create threads that may be cancelled.
     */
    RequestProcessor requestProcessor = null;
    
    /**
     * Latest annotation comment fetching task launched.
     */
    private RequestProcessor.Task latestAnnotationTask = null;

    /**
     * Holds false if Rollback Changes action is NOT valid for current ievision, true otherwise. 
     */ 
    private boolean recentRevisionCanBeRolledBack;

    /**
     * The log messaages for the file stored in the AnnotationBar;
     */
    private GitLogMessage [] logs;

    /**
     * Creates new instance initializing final fields.
     */
    public AnnotationBar(JTextComponent target) {
        this.textComponent = target;
        this.editorUI = Utilities.getEditorUI(target);
        this.foldHierarchy = FoldHierarchy.get(editorUI.getComponent());
        this.doc = editorUI.getDocument();
        this.caret = textComponent.getCaret();
    }
    
    // public contract ~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Makes the bar visible and sensitive to
     * LogOutoutListener events that should deliver
     * actual content to be displayed.
     */
    public void annotate() {
        annotated = true;
        elementAnnotations = null;

        doc.addDocumentListener(this);
        textComponent.addComponentListener(this);
        editorUI.addPropertyChangeListener(this);

        revalidate();  // resize the component
    }

    public void setAnnotationMessage(String message) {
        elementAnnotationsSubstitute = message;
        revalidate();
    }
    
    /**
     * Result computed show it...
     * Takes AnnotateLines and shows them.
     */
    public void annotationLines(File file, List<AnnotateLine> annotateLines) {
        List<AnnotateLine> lines = new LinkedList<AnnotateLine>(annotateLines);
        int lineCount = lines.size();
        /** 0 based line numbers => 1 based line numbers*/
        int ann2editorPermutation[] = new int[lineCount];
        for (int i = 0; i< lineCount; i++) {
            ann2editorPermutation[i] = i+1;
        }

        DiffProvider diff = (DiffProvider) Lookup.getDefault().lookup(DiffProvider.class);
        if (diff != null) {
            Reader r = new LinesReader(lines);
            Reader docReader = Utils.getDocumentReader(doc);
            try {

                Difference[] differences = diff.computeDiff(r, docReader);

                // customize annotation line numbers to match different reality
                // compule line permutation

                for (int i = 0; i < differences.length; i++) {
                    Difference d = differences[i];
                    if (d.getType() == Difference.ADD) continue;

                    int editorStart;
                    int firstShift = d.getFirstEnd() - d.getFirstStart() +1;
                    if (d.getType() == Difference.CHANGE) {
                        int firstLen = d.getFirstEnd() - d.getFirstStart();
                        int secondLen = d.getSecondEnd() - d.getSecondStart();
                        if (secondLen >= firstLen) continue; // ADD or pure CHANGE
                        editorStart = d.getSecondStart();
                        firstShift = firstLen - secondLen;
                    } else {  // DELETE
                        editorStart = d.getSecondStart() + 1;
                    }

                    for (int c = editorStart + firstShift -1; c<lineCount; c++) {
                        ann2editorPermutation[c] -= firstShift;
                    }
                }

                for (int i = differences.length -1; i >= 0; i--) {
                    Difference d = differences[i];
                    if (d.getType() == Difference.DELETE) continue;

                    int firstStart;
                    int firstShift = d.getSecondEnd() - d.getSecondStart() +1;
                    if (d.getType() == Difference.CHANGE) {
                        int firstLen = d.getFirstEnd() - d.getFirstStart();
                        int secondLen = d.getSecondEnd() - d.getSecondStart();
                        if (secondLen <= firstLen) continue; // REMOVE or pure CHANGE
                        firstShift = secondLen - firstLen;
                        firstStart = d.getFirstStart();
                    } else {
                        firstStart = d.getFirstStart() + 1;
                    }

                    for (int k = firstStart-1; k<lineCount; k++) {
                        ann2editorPermutation[k] += firstShift;
                    }
                }

            } catch (IOException e) {
                Git.LOG.log(Level.INFO, "Cannot compute local diff required for annotations, ignoring...");  // NOI18N 
            }
        }

        try {
            doc.atomicLock();
            StyledDocument sd = (StyledDocument) doc;
            Iterator<AnnotateLine> it = lines.iterator();
            elementAnnotations = Collections.synchronizedMap(new HashMap<Element, AnnotateLine>(lines.size()));
            while (it.hasNext()) {
                AnnotateLine line = it.next();
                int lineNum = ann2editorPermutation[line.getLineNum() -1];
                try {
                    int lineOffset = NbDocument.findLineOffset(sd, lineNum -1);
                    Element element = sd.getParagraphElement(lineOffset);
                    elementAnnotations.put(element, line);
                } catch (IndexOutOfBoundsException ex) {
                    // TODO how could I get line behind document end?
                    // furtunately user does not spot it
                    Git.LOG.log(Level.INFO, null, ex);
                }
            }
        } finally {
            doc.atomicUnlock();
        }

        // lazy listener registration
        caret.addChangeListener(this);
        this.caretTimer = new Timer(500, this);
        caretTimer.setRepeats(false);

        onCurrentLine();
        revalidate();
        repaint();
    }

    // implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Gets a the file related to the document
     *
     * @return the file related to the document, <code>null</code> if none
     * exists.
     */
    private File getCurrentFile() {
        File result = null;
        
        DataObject dobj = (DataObject)doc.getProperty(Document.StreamDescriptionProperty);
        if (dobj != null) {
            FileObject fo = dobj.getPrimaryFile();
            result = FileUtil.toFile(fo);
        }
        
        return result;
    }
    
    /**
     * Registers "close" popup menu, tooltip manager // NOI18N
     * and repaint on documet change manager.
     */
    public void addNotify() {
        super.addNotify();


        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    e.consume();
                    createPopup().show(e.getComponent(),
                               e.getX(), e.getY());
                }
            }
        });

        // register with tooltip manager
        setToolTipText(""); // NOI18N

    }

    private JPopupMenu createPopup() {
        final ResourceBundle loc = NbBundle.getBundle(AnnotationBar.class);
        final JPopupMenu popupMenu = new JPopupMenu();

        final File file = getCurrentFile();
        
        final JMenuItem diffMenu = new JMenuItem(loc.getString("CTL_MenuItem_DiffToRevision")); // NOI18N
        diffMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (recentRevision != null) {
                    if (getPreviousRevision(recentRevision) != null) {
                        DiffAction.diff(recentFile, getPreviousRevision(recentRevision), recentRevision);
                    }
                }
            }
        });
        popupMenu.add(diffMenu);

        JMenuItem rollbackMenu = new JMenuItem(loc.getString("CTL_MenuItem_Revert")); // NOI18N
        rollbackMenu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                revert(file, recentRevision);
            }
        });
        popupMenu.add(rollbackMenu);
        rollbackMenu.setEnabled(recentRevisionCanBeRolledBack);

        JMenuItem menu;
        menu = new JMenuItem(loc.getString("CTL_MenuItem_CloseAnnotations")); // NOI18N
        menu.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hideBar();
            }
        });
        popupMenu.add(new JSeparator());
        popupMenu.add(menu);

        diffMenu.setVisible(false);
        rollbackMenu.setVisible(false);
        if (recentRevision != null) {
            if (getPreviousRevision(recentRevision) != null) {
                String format = loc.getString("CTL_MenuItem_DiffToRevision"); // NOI18N
                diffMenu.setText(MessageFormat.format(format, new Object [] { recentRevision, getPreviousRevision(recentRevision) }));
                diffMenu.setVisible(true);
                rollbackMenu.setVisible(true);
            }
        }

        return popupMenu;
    }

    private void revert(final File file, String revision) {
        final File root = Git.getInstance().getTopmostManagedParent(file);
        File[] files = new File [1];
        files[0] = file; 
        final RevertModifications revertModifications = new RevertModifications(root, files, revision);
        if(!revertModifications.showDialog()) {
            return;
        }
        final String revStr =  revertModifications.getSelectionRevision();
        final boolean doBackup = revertModifications.isBackupRequested();

        RequestProcessor rp = Git.getInstance().getRequestProcessor(root);
        GitProgressSupport support = new GitProgressSupport() {
            public void perform() {                 
                RevertModificationsAction.performRevert(root, revStr, file, doBackup, this.getLogger());
            }
        };
        support.start(rp, root.getAbsolutePath(), NbBundle.getMessage(AnnotationBar.class, "MSG_Revert_Progress")); // NOI18N
    }

    private String getPreviousRevision(String revision) {
        for(int i = 0; i < logs.length; i++) {
            if (logs[i].getRevision() == Long.parseLong(revision)) {
                if (i < logs.length - 1)
                    return Long.toString(logs[i+1].getRevision());
            }
        }
        return null;
    }

    /**
     * Hides the annotation bar from user. 
     */
    void hideBar() {
        annotated = false;
        revalidate();
        release();
    }

    /**
     * Gets a request processor which is able to cancel tasks.
     */
    private RequestProcessor getRequestProcessor() {
        if (requestProcessor == null) {
            requestProcessor = new RequestProcessor("AnnotationBarRP", 1, true);  // NOI18N
        }
        
        return requestProcessor;
    }
    
    /**
     * Shows commit message in status bar and or revision change repaints side
     * bar (to highlight same revision). This process is started in a
     * seperate thread.
     */
    private void onCurrentLine() {
        if (latestAnnotationTask != null) {
            latestAnnotationTask.cancel();
        }
        
        latestAnnotationTask = getRequestProcessor().post(this);
    }

    // latestAnnotationTask business logic
    public void run() {
        // get resource bundle
        ResourceBundle loc = NbBundle.getBundle(AnnotationBar.class);
        // give status bar "wait" indication // NOI18N
        StatusBar statusBar = editorUI.getStatusBar();
        recentStatusMessage = loc.getString("CTL_StatusBar_WaitFetchAnnotation"); // NOI18N
        statusBar.setText(StatusBar.CELL_MAIN, recentStatusMessage);
        
        recentRevisionCanBeRolledBack = false;
        // determine current line
        int line = -1;
        int offset = caret.getDot();
        try {
            line = Utilities.getLineOffset(doc, offset);
        } catch (BadLocationException ex) {
            Git.LOG.log(Level.SEVERE, "Can not get line for caret at offset ", offset); // NOI18N
            clearRecentFeedback();
            return;
        }

        // handle locally modified lines
        AnnotateLine al = getAnnotateLine(line);
        if (al == null) {
            AnnotationMarkProvider amp = AnnotationMarkInstaller.getMarkProvider(textComponent);
            if (amp != null) {
                amp.setMarks(Collections.<AnnotationMark>emptyList());
            }
            clearRecentFeedback();
            if (recentRevision != null) {
                recentRevision = null;
                repaint();
            }
            return;
        }

        // handle unchanged lines
        String revision = al.getRevision();
        if (revision.equals(recentRevision) == false) {
            recentRevision = revision;
            File file = Git.getInstance().getTopmostManagedParent(getCurrentFile());
            recentFile = new File(file, al.getFileName());
            recentRevisionCanBeRolledBack = al.canBeRolledBack();
            repaint();

            AnnotationMarkProvider amp = AnnotationMarkInstaller.getMarkProvider(textComponent);
            if (amp != null) {
            
                List<AnnotationMark> marks = new ArrayList<AnnotationMark>(elementAnnotations.size());
                // I cannot affort to lock elementAnnotations for long time
                // it's accessed from editor thread too
                Iterator<Map.Entry<Element, AnnotateLine>> it2;
                synchronized(elementAnnotations) {
                    it2 = new HashSet<Map.Entry<Element, AnnotateLine>>(elementAnnotations.entrySet()).iterator();
                }
                while (it2.hasNext()) {
                    Map.Entry<Element, AnnotateLine> next = it2.next();                        
                    AnnotateLine annotateLine = next.getValue();
                    if (revision.equals(annotateLine.getRevision())) {
                        Element element = next.getKey();
                        if (elementAnnotations.containsKey(element) == false) {
                            continue;
                        }
                        int elementOffset = element.getStartOffset();
                        int lineNumber = NbDocument.findLineNumber((StyledDocument)doc, elementOffset);
                        AnnotationMark mark = new AnnotationMark(lineNumber, revision);
                        marks.add(mark);
                    }

                    if (Thread.interrupted()) {
                        clearRecentFeedback();
                        return;
                    }
                }
                amp.setMarks(marks);
            }
        }

        if (al.getCommitMessage() != null) {
            recentStatusMessage = al.getCommitMessage();
            statusBar.setText(StatusBar.CELL_MAIN, al.getAuthor() + ": " + recentStatusMessage); // NOI18N
        } else {
            clearRecentFeedback();
        };
    }
    
    /**
     * Clears the status bar if it contains the latest status message
     * displayed by this annotation bar.
     */
    private void clearRecentFeedback() {
        StatusBar statusBar = editorUI.getStatusBar();
        if (statusBar.getText(StatusBar.CELL_MAIN) == recentStatusMessage) {
            statusBar.setText(StatusBar.CELL_MAIN, "");  // NOI18N
        }
    }

    /**
     * Components created by SibeBarFactory are positioned
     * using a Layout manager that determines componnet size
     * by retireving preferred size.
     *
     * <p>Once componnet needs resizing it simply calls
     * {@link #revalidate} that triggers new layouting
     * that consults prefered size.
     */
    public Dimension getPreferredSize() {
        Dimension dim = textComponent.getSize();
        int width = annotated ? getBarWidth() : 0;
        dim.width = width;
        dim.height *=2;  // XXX
        return dim;
    }

    /**
     * Gets the maximum size of this component.
     *
     * @return the maximum size of this component
     */
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    /**
     * Gets the preferred width of this component.
     *
     * @return the preferred width of this component
     */
    private int getBarWidth() {
        String longestString = "";  // NOI18N
        if (elementAnnotations == null) {
            longestString = elementAnnotationsSubstitute;
        } else {
            synchronized(elementAnnotations) {
                Iterator<AnnotateLine> it = elementAnnotations.values().iterator();
                while (it.hasNext()) {
                    AnnotateLine line = it.next();
                    String displayName = getDisplayName(line); // NOI18N
                    if (displayName.length() > longestString.length()) {
                        longestString = displayName;
                    }
                }
            }
        }
        char[] data = longestString.toCharArray();
        int w = getGraphics().getFontMetrics().charsWidth(data, 0,  data.length);
        return w + 4;
    }

    private String getDisplayName(AnnotateLine line) {
        return line.getRevision() + "  " + line.getAuthor(); // NOI18N
    }

    /**
     * Pair method to {@link #annotate}. It releases
     * all resources.
     */
    private void release() {
        editorUI.removePropertyChangeListener(this);
        textComponent.removeComponentListener(this);
        doc.removeDocumentListener(this);
        caret.removeChangeListener(this);
        if (caretTimer != null) {
            caretTimer.removeActionListener(this);
        }
        elementAnnotations = null;
        // cancel running annotation task if active
        if(latestAnnotationTask != null) {
            latestAnnotationTask.cancel();
        }
        AnnotationMarkProvider amp = AnnotationMarkInstaller.getMarkProvider(textComponent);
        if (amp != null) {
            amp.setMarks(Collections.<AnnotationMark>emptyList());
        }

        clearRecentFeedback();
    }

    /**
     * Paints one view that corresponds to a line (or
     * multiple lines if folding takes effect).
     */
    private void paintView(View view, Graphics g, int yBase) {
        JTextComponent component = editorUI.getComponent();
        if (component == null) return;
        BaseTextUI textUI = (BaseTextUI)component.getUI();

        Element rootElem = textUI.getRootView(component).getElement();
        int line = rootElem.getElementIndex(view.getStartOffset());

        String annotation = "";  // NOI18N
        AnnotateLine al = null;
        if (elementAnnotations != null) {
            al = getAnnotateLine(line);
            if (al != null) {
                annotation = getDisplayName(al);  // NOI18N
            }
        } else {
            annotation = elementAnnotationsSubstitute;
        }

        if (al != null && al.getRevision().equals(recentRevision)) {
            g.setColor(selectedColor());
        } else {
            g.setColor(foregroundColor());
        }
        g.drawString(annotation, 2, yBase + editorUI.getLineAscent());
    }

    /**
     * Presents commit message as tooltips.
     */
    public String getToolTipText (MouseEvent e) {
        if (editorUI == null)
            return null;
        int line = getLineFromMouseEvent(e);

        StringBuffer annotation = new StringBuffer();
        if (elementAnnotations != null) {
            AnnotateLine al = getAnnotateLine(line);

            if (al != null) {
                String escapedAuthor = NbBundle.getMessage(AnnotationBar.class, "TT_Annotation"); // NOI18N
                try {
                    escapedAuthor = XMLUtil.toElementContent(al.getAuthor());
                } catch (CharConversionException e1) {
                    Git.LOG.log(Level.INFO, "HG.AB: can not HTML escape: ", al.getAuthor());  // NOI18N
                }

                // always return unique string to avoid tooltip sharing on mouse move over same revisions -->
                annotation.append("<html><!-- line=" + line++ + " -->" + al.getRevision()  + " - <b>" + escapedAuthor + "</b>"); // NOI18N
                if (al.getDate() != null) {
                    annotation.append(" " + DateFormat.getDateInstance().format(al.getDate())); // NOI18N                    
                }
                if (al.getCommitMessage() != null) {
                    String escaped = null;
                    try {
                        escaped = XMLUtil.toElementContent(al.getCommitMessage());
                    } catch (CharConversionException e1) {
                        Git.LOG.log(Level.INFO, "HG.AB: can not HTML escape: ", al.getCommitMessage());  // NOI18N
                    }
                    if (escaped != null) {
                        String lined = escaped.replaceAll(System.getProperty("line.separator"), "<br>");  // NOI18N
                        annotation.append("<p>" + lined); // NOI18N
                    }
                }
            }
        } else {
            annotation.append(elementAnnotationsSubstitute);
        }

        return annotation.toString();
    }

    /**
     * Locates AnnotateLine associated with given line. The
     * line is translated to Element that is used as map lookup key.
     * The map is initially filled up with Elements sampled on
     * annotate() method.
     *
     * <p>Key trick is that Element's identity is maintained
     * until line removal (and is restored on undo).
     *
     * @param line
     * @return found AnnotateLine or <code>null</code>
     */
    private AnnotateLine getAnnotateLine(int line) {
        StyledDocument sd = (StyledDocument) doc;
        int lineOffset = NbDocument.findLineOffset(sd, line);
        Element element = sd.getParagraphElement(lineOffset);
        AnnotateLine al = elementAnnotations.get(element);

        if (al != null) {
            int startOffset = element.getStartOffset();
            int endOffset = element.getEndOffset();
            try {
                int len = endOffset - startOffset;
                String text = doc.getText(startOffset, len -1);
                String content = al.getContent();
                if (text.equals(content)) {
                    return al;
                }
            } catch (BadLocationException e) {
                Git.LOG.log(Level.INFO, "HG.AB: can not locate line annotation.");  // NOI18N
            }
        }

        return null;
    }

    /**
     * GlyphGutter copy pasted bolerplate method.
     * It invokes {@link #paintView} that contains
     * actual business logic.
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Rectangle clip = g.getClipBounds();

        JTextComponent component = editorUI.getComponent();
        if (component == null) return;

        BaseTextUI textUI = (BaseTextUI)component.getUI();
        View rootView = Utilities.getDocumentView(component);
        if (rootView == null) return;

        g.setColor(backgroundColor());
        g.fillRect(clip.x, clip.y, clip.width, clip.height);

        AbstractDocument doc = (AbstractDocument)component.getDocument();
        doc.readLock();
        try{
            foldHierarchy.lock();
            try{
                int startPos = textUI.getPosFromY(clip.y);
                int startViewIndex = rootView.getViewIndex(startPos, Position.Bias.Forward);
                int rootViewCount = rootView.getViewCount();

                if (startViewIndex >= 0 && startViewIndex < rootViewCount) {
                    // find the nearest visible line with an annotation
                    Rectangle rec = textUI.modelToView(component, rootView.getView(startViewIndex).getStartOffset());
                    int y = (rec == null) ? 0 : rec.y;

                    int clipEndY = clip.y + clip.height;
                    for (int i = startViewIndex; i < rootViewCount; i++){
                        View view = rootView.getView(i);
                        paintView(view, g, y);
                        y += editorUI.getLineHeight();
                        if (y >= clipEndY) {
                            break;
                        }
                    }
                }

            } finally {
                foldHierarchy.unlock();
            }
        } catch (BadLocationException ble){
            Git.LOG.log(Level.WARNING, null, ble);
        } finally {
            doc.readUnlock();
        }
    }

    private Color backgroundColor() {
        if (textComponent != null) {
            return textComponent.getBackground();
        }
        return backgroundColor;
    }

    private Color foregroundColor() {
        if (textComponent != null) {
            return textComponent.getForeground();
        }
        return foregroundColor;
    }

    private Color selectedColor() {
        if (backgroundColor == backgroundColor()) {
            return selectedColor;
        }
        if (textComponent != null) {
            return textComponent.getForeground();
        }
        return selectedColor;

    }


    /** GlyphGutter copy pasted utility method. */
    private int getLineFromMouseEvent(MouseEvent e){
        int line = -1;
        if (editorUI != null) {
            try{
                JTextComponent component = editorUI.getComponent();
                BaseTextUI textUI = (BaseTextUI)component.getUI();
                int clickOffset = textUI.viewToModel(component, new Point(0, e.getY()));
                line = Utilities.getLineOffset(doc, clickOffset);
            }catch (BadLocationException ble){
            }
        }
        return line;
    }

    /** Implementation */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt == null) return;
        String id = evt.getPropertyName();
        if (EditorUI.COMPONENT_PROPERTY.equals(id)) {  // NOI18N
            if (evt.getNewValue() == null){
                // component deinstalled, lets uninstall all isteners
                release();
            }
        }

    }

    /** Implementation */
    public void changedUpdate(DocumentEvent e) {
    }

    /** Implementation */
    public void insertUpdate(DocumentEvent e) {
        // handle new lines,  Enter hit at end of line changes
        // the line element instance
        // XXX Actually NB document implementation triggers this method two times
        //  - first time with one removed and two added lines
        //  - second time with two removed and two added lines
        if (elementAnnotations != null) {
            Element[] elements = e.getDocument().getRootElements();
            synchronized(elementAnnotations) { // atomic change
                for (int i = 0; i < elements.length; i++) {
                    Element element = elements[i];
                    DocumentEvent.ElementChange change = e.getChange(element);
                    if (change == null) continue;
                    Element[] removed = change.getChildrenRemoved();
                    Element[] added = change.getChildrenAdded();

                    if (removed.length == added.length) {
                        for (int c = 0; c<removed.length; c++) {
                            AnnotateLine recent = elementAnnotations.get(removed[c]);
                            if (recent != null) {
                                elementAnnotations.remove(removed[c]);
                                elementAnnotations.put(added[c], recent);
                            }
                        }
                    } else if (removed.length == 1 && added.length > 0) {
                        Element key = removed[0];
                        AnnotateLine recent = elementAnnotations.get(key);
                        if (recent != null) {
                            elementAnnotations.remove(key);
                            elementAnnotations.put(added[0], recent);
                        }
                    }
                }
            }
        }
        repaint();
    }

    /** Implementation */
    public void removeUpdate(DocumentEvent e) {
        if (e.getDocument().getLength() == 0) { // external reload
            hideBar();
        }
        repaint();
    }

    /** Caret */
    public void stateChanged(ChangeEvent e) {
        assert e.getSource() == caret;
        caretTimer.restart();
    }

    /** Timer */
    public void actionPerformed(ActionEvent e) {
        assert e.getSource() == caretTimer;
        onCurrentLine();
    }

    /** on JTextPane */
    public void componentHidden(ComponentEvent e) {
    }

    /** on JTextPane */
    public void componentMoved(ComponentEvent e) {
    }

    /** on JTextPane */
    public void componentResized(ComponentEvent e) {
        revalidate();
    }

    /** on JTextPane */
    public void componentShown(ComponentEvent e) {
    }

    public void setLogs(GitLogMessage [] logs) {
        this.logs = logs;
    }
}
