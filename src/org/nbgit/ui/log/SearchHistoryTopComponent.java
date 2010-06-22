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
import java.io.File;
import java.util.Collection;
import org.nbgit.ui.diff.DiffSetupSource;
import org.netbeans.modules.versioning.spi.VCSContext;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.eclipse.jgit.lib.Constants;

/**
 * @author Maros Sandor
 */
public class SearchHistoryTopComponent extends TopComponent implements DiffSetupSource {

    private SearchHistoryPanel shp;
    private SearchCriteriaPanel scp;

    public SearchHistoryTopComponent() {
        setIcon(org.openide.util.ImageUtilities.loadImage("org/nbgit/resources/icons/gitvcs-icon.png"));  // NOI18N
        getAccessibleContext().setAccessibleName(NbBundle.getMessage(SearchHistoryTopComponent.class, "ACSN_SearchHistoryT_Top_Component")); // NOI18N
        getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(SearchHistoryTopComponent.class, "ACSD_SearchHistoryT_Top_Component")); // NOI18N
    }

    public SearchHistoryTopComponent(VCSContext context) {
        this(context, null, null, null, null);
    }

    public SearchHistoryTopComponent(VCSContext context, String commitMessage, String username, String from, String to) {
        this();
        File[] roots = context.getRootFiles().toArray(new File[0]);
        initComponents(roots, commitMessage, username, from, to);
    }

    public SearchHistoryTopComponent(String repositoryUrl, File localRoot, String revision) {
        this();
        initComponents(repositoryUrl, localRoot, revision);
    }

    public void search(boolean showSearchCriteria) {
        shp.executeSearch();
        shp.setSearchCriteria(showSearchCriteria);
    }

    public void searchOut() {
        shp.setOutSearch();
        shp.executeSearch();
        shp.setSearchCriteria(false);
        scp.setFrom("");
        scp.setTo("");
    }

    public void searchIncoming() {
        shp.setIncomingSearch();
        scp.setFrom("");
        scp.setTo("");
    }

    private void initComponents(String repositoryUrl, File localRoot, String revision) {
        setLayout(new BorderLayout());
        scp = new SearchCriteriaPanel(repositoryUrl);
        scp.setFrom(revision);
        scp.setTo(revision);
        shp = new SearchHistoryPanel(repositoryUrl, localRoot, scp);
        add(shp);
    }

    private void initComponents(File[] roots, String commitMessage, String username, String from, String to) {
        setLayout(new BorderLayout());
        scp = new SearchCriteriaPanel(roots);
        scp.setCommitMessage(commitMessage);
        scp.setUsername(username);
        if (from == null) {
            from = Constants.HEAD;
        }
        scp.setFrom(from);
        if (to == null) {
            to = "";
        }
        scp.setTo(to);
        shp = new SearchHistoryPanel(roots, scp);
        add(shp);
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_NEVER;
    }

    @Override
    protected void componentClosed() {
        //((DiffMainPanel) getComponent(0)).componentClosed();
        super.componentClosed();
    }

    @Override
    protected String preferredID() {
        if (shp.isIncomingSearch()) {
            return "Git.IncomingSearchHistoryTopComponent";
        } else if (shp.isOutSearch()) {
            return "Git.OutSearchHistoryTopComponent";
        }
        return "Git.SearchHistoryTopComponent";    // NOI18N
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(getClass());
    }

    public Collection getSetups() {
        return shp.getSetups();
    }

    public String getSetupDisplayName() {
        return getDisplayName();
    }
}
