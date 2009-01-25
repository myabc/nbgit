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
package org.nbgit.ui.status;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.io.File;
import java.io.Serializable;
import java.util.logging.Level;
import javax.swing.SwingUtilities;
import org.nbgit.Git;
import org.nbgit.util.GitUtils;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Top component of the Versioning view.
 *
 * @author Maros Sandor
 */
public class GitVersioningTopComponent extends TopComponent {

    private static final long serialVersionUID = 1L;
    private VersioningPanel syncPanel;
    private VCSContext context;
    private String contentTitle;
    private String branchTitle;
    private long lastUpdateTimestamp;
    private static final String PREFERRED_ID = "gitversioning"; // NOI18N
    private static GitVersioningTopComponent instance;

    public GitVersioningTopComponent() {
        putClientProperty("SlidingName", NbBundle.getMessage(GitVersioningTopComponent.class, "CTL_Versioning_TopComponent_Title")); //NOI18N

        setName(NbBundle.getMessage(GitVersioningTopComponent.class, "CTL_Versioning_TopComponent_Title")); // NOI18N
        setIcon(org.openide.util.ImageUtilities.loadImage("org/nbgit/resources/icons/gitvcs-icon.png"));  // NOI18N
        setLayout(new BorderLayout());
        getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(GitVersioningTopComponent.class, "CTL_Versioning_TopComponent_Title")); // NOI18N
        syncPanel = new VersioningPanel(this);
        add(syncPanel);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(getClass());
    }

    @Override
    protected void componentActivated() {
        updateTitle();
        syncPanel.focus();
    }

    @Override
    protected void componentOpened() {
        super.componentOpened();
        refreshContent();
    }

    @Override
    protected void componentClosed() {
        super.componentClosed();
    }

    private void refreshContent() {
        if (syncPanel == null) {
            return;  // the component is not showing => nothing to refresh
        }
        updateTitle();
        syncPanel.setContext(context);
    }

    /**
     * Sets the 'content' portion of Versioning component title.
     * Title pattern: Versioning[ - contentTitle[ - branchTitle]] (10 minutes ago)
     *
     * @param contentTitle a new content title, e.g. "2 projects" // NOI18N
     */
    public void setContentTitle(String contentTitle) {
        this.contentTitle = contentTitle;
        updateTitle();
    }

    /**
     * Sets the 'branch' portion of Versioning component title.
     * Title pattern: Versioning[ - contentTitle[ - branchTitle]] (10 minutes ago)
     *
     * @param branchTitle a new content title, e.g. "release40" branch // NOI18N
     */
    void setBranchTitle(String branchTitle) {
        this.branchTitle = branchTitle;
        updateTitle();
    }

    public void contentRefreshed() {
        lastUpdateTimestamp = System.currentTimeMillis();
        updateTitle();
    }

    private void updateTitle() {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                if (contentTitle == null) {
                    setName(NbBundle.getMessage(GitVersioningTopComponent.class, "CTL_Versioning_TopComponent_Title")); // NOI18N
                } else {
                    File baseFile = GitUtils.getRootFile(context);
                    String name = "";
                    if (baseFile != null) {
                        name = baseFile.getName();
                    }

                    if (branchTitle == null) {
                        setName(NbBundle.getMessage(GitVersioningTopComponent.class,
                                "CTL_Versioning_TopComponent_MultiTitle",
                                contentTitle, name.equals(contentTitle) ? "" : "[" + name + "]"));  // NOI18N
                    } else {
                        setName(NbBundle.getMessage(GitVersioningTopComponent.class,
                                "CTL_Versioning_TopComponent_Title_ContentBranch",
                                contentTitle, name.equals(contentTitle) ? "" : "[" + name + "] ", branchTitle)); // NOI18N
                    }
                }
            }
        });
    }

    String getContentTitle() {
        return contentTitle;
    }

    /**
     * Gets default instance. Do not use directly: reserved for *.settings files only,
     * i.e. deserialization routines; otherwise you could get a non-deserialized instance.
     * To obtain the singleton instance, use {@link findInstance}.
     */
    public static synchronized GitVersioningTopComponent getDefault() {
        if (instance == null) {
            instance = new GitVersioningTopComponent();
        }
        return instance;
    }

    /**
     * Obtain the GitVersioningTopComponent  instance. Never call {@link #getDefault} directly!
     */
    public static synchronized GitVersioningTopComponent findInstance() {
        TopComponent win = WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        if (win == null) {
            Git.LOG.log(Level.FINE, "Cannot find " + PREFERRED_ID + " component. It will not be located properly in the window system."); // NOI18N
            return getDefault();
        }
        if (win instanceof GitVersioningTopComponent) {
            return (GitVersioningTopComponent) win;
        }
        Git.LOG.log(Level.FINE,
                "There seem to be multiple components with the '" + PREFERRED_ID + // NOI18N
                "' ID. That is a potential source of errors and unexpected behavior."); // NOI18N
        return getDefault();
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    /** replaces this in object stream */
    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    final static class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 1L;

        public Object readResolve() {
            return GitVersioningTopComponent.getDefault();
        }
    }

    /**
     * Programmatically invokes the Refresh action.
     */
    public void performRefreshAction() {
        syncPanel.performRefreshAction();
    }

    /**
     * Sets files/folders the user wants to synchronize. They are typically activated (selected) nodes.
     *
     * @param ctx new context of the Versioning view
     */
    public void setContext(VCSContext ctx) {
        syncPanel.cancelRefresh();

        if (ctx == null) {
            setName(NbBundle.getMessage(GitVersioningTopComponent.class, "MSG_Preparing")); // NOI18N
            setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        } else {
            setEnabled(true);
            setCursor(Cursor.getDefaultCursor());
            context = ctx;
            syncPanel.setContext(ctx);
            setBranchTitle(NbBundle.getMessage(GitVersioningTopComponent.class, "CTL_VersioningView_UnnamedBranchTitle")); // NOI18N
            refreshContent();
        }
        setToolTipText(getContextFilesList(ctx));
    }

    private String getContextFilesList(VCSContext ctx) {
        if (ctx == null || ctx.getRootFiles().size() == 0) {
            return NbBundle.getMessage(GitVersioningTopComponent.class, "CTL_Versioning_TopComponent_Title"); // NOI18N
        }
        int size = ctx.getRootFiles().size();

        StringBuffer sb = new StringBuffer(200);
        if (size > 1) {
            sb.append("<html>"); // NOI18N
        }
        for (File file : ctx.getRootFiles()) {
            sb.append(file.getAbsolutePath());
            size--;
            if (size > 0) {
                sb.append("<br>"); // NOI18N
            }
        }
        return sb.toString();
    }

    /** Tests whether it shows some content. */
    public boolean hasContext() {
        return context != null && context.getRootFiles().size() > 0;
    }
}