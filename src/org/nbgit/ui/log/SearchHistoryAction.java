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

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.SwingUtilities;
import org.nbgit.StatusInfo;
import org.nbgit.ui.ContextAction;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.netbeans.modules.versioning.util.Utils;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

/**
 * Opens Search History Component.
 *
 * @author Maros Sandor
 */
public class SearchHistoryAction extends ContextAction {

    static final int DIRECTORY_ENABLED_STATUS = StatusInfo.STATUS_MANAGED & ~StatusInfo.STATUS_NOTVERSIONED_EXCLUDED & ~StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY;
    static final int FILE_ENABLED_STATUS = StatusInfo.STATUS_MANAGED & ~StatusInfo.STATUS_NOTVERSIONED_EXCLUDED & ~StatusInfo.STATUS_NOTVERSIONED_NEWLOCALLY;

    public SearchHistoryAction(String name, VCSContext context) {
        super(name, context);
    }

    protected String getBaseName(Node[] activatedNodes) {
        return "CTL_MenuItem_SearchHistory"; // NOI18N
    }

    protected int getFileEnabledStatus() {
        return FILE_ENABLED_STATUS;
    }

    protected int getDirectoryEnabledStatus() {
        return DIRECTORY_ENABLED_STATUS;
    }

    protected boolean asynchronous() {
        return false;
    }

    public void performAction(ActionEvent e) {
        String title = NbBundle.getMessage(SearchHistoryAction.class, "CTL_SearchHistory_Title", Utils.getContextDisplayName(context)); // NOI18N
        openHistory(context, title);
    }

    public static void openHistory(final VCSContext context, final String title) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                if (context == null) {
                    return;
                }
                SearchHistoryTopComponent tc = new SearchHistoryTopComponent(context);
                tc.setDisplayName(title);
                tc.open();
                tc.requestActive();
                tc.search(true);
            }
        });
    }

    /**
     * Opens the Search History panel to view Git Changesets that will be sent on next Pull from remote repo
     * using: git incoming - to get the data
     *
     * @param title title of the search
     * @param commitMessage commit message to search for
     * @param username user name to search for
     * @param date date of the change in question
     */
    public static void openIncoming(final VCSContext context, final String title) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                if (context == null) {
                    return;
                }
                SearchHistoryTopComponent tc = new SearchHistoryTopComponent(context);
                tc.setDisplayName(title);
                tc.open();
                tc.requestActive();
                tc.searchIncoming();
            }
        });
    }

    /**
     * Opens the Search History panel to view Git Out Changesets that will be sent on next Push to remote repo
     * using: git out - to get the data
     *
     * @param title title of the search
     * @param commitMessage commit message to search for
     * @param username user name to search for
     * @param date date of the change in question
     */
    public static void openOut(final VCSContext context, final String title) {
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                if (context == null) {
                    return;
                }
                SearchHistoryTopComponent tc = new SearchHistoryTopComponent(context);
                tc.setDisplayName(title);
                tc.open();
                tc.requestActive();
                tc.searchOut();
            }
        });
    }

    /**
     * Opens the Search History panel with given pre-filled values. The search is executed in default context
     * (all open projects).
     *
     * @param title title of the search
     * @param commitMessage commit message to search for
     * @param username user name to search for
     * @param rev the revision of the change in question
     */
    public static void openSearch(String title, String commitMessage, String username, String rev) {
        openSearch(getDefaultContext(), title, commitMessage, username, rev);
    }

    public static void openSearch(VCSContext context, String title, String commitMessage, String username, String from) {
        String to = from + "~20";

        if (commitMessage != null && commitMessage.indexOf('\n') != -1) {
            commitMessage = commitMessage.substring(0, commitMessage.indexOf('\n'));
        }
        SearchHistoryTopComponent tc = new SearchHistoryTopComponent(context, commitMessage, username, from, to);
        String tcTitle = NbBundle.getMessage(SearchHistoryAction.class, "CTL_SearchHistory_Title", title); // NOI18N
        tc.setDisplayName(tcTitle);
        tc.open();
        tc.requestActive();
        tc.search(false);
    }

    private static VCSContext getDefaultContext() {
        Node[] nodes = TopComponent.getRegistry().getActivatedNodes();

        return nodes != null ? VCSContext.forNodes(nodes) : VCSContext.EMPTY;
    }

    /**
     * Opens search panel in the context of the given repository URL.
     *
     * @param repositoryUrl URL to search
     * @param localRoot local working copy root that corresponds to the repository URL
     * @param revision revision to search for
     */
    public static void openSearch(String repositoryUrl, File localRoot, String revision) {
        SearchHistoryTopComponent tc = new SearchHistoryTopComponent(repositoryUrl, localRoot, revision);
        String tcTitle = NbBundle.getMessage(SearchHistoryAction.class, "CTL_SearchHistory_Title", repositoryUrl); // NOI18N
        tc.setDisplayName(tcTitle);
        tc.open();
        tc.requestActive();
        tc.search(false);
    }
}
