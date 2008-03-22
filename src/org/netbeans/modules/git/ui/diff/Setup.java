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
package org.netbeans.modules.git.ui.diff;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import org.netbeans.api.diff.DiffController;
import org.netbeans.api.diff.StreamSource;
import org.netbeans.modules.git.FileInformation;
import org.netbeans.modules.git.Git;
import org.openide.util.NbBundle;

/**
 * Represents on DIFF setup.
 *
 * @author Maros Sandor
 * @author alexbcoles
 */
public final class Setup {

    /**
     * What was locally changed? The right pane contains local file.
     *
     * <p>Local addition, removal or change is displayed in
     * the right pane as addition, removal or change respectively
     * (i.e. not reversed as removal, addition or change).
     *
     * <pre>
     * diff from-BASE to-LOCAL
     * </pre>
     */
    public static final int DIFFTYPE_LOCAL = 0;
    /**
     * What was remotely changed? The right pane contains remote file.
     *
     * <p>Remote addition, removal or change is displayed in
     * the right pane as addition, removal or change respectively
     * (i.e. not reversed as removal, addition or change).
     *
     * <pre>
     * diff from-BASE to-HEAD
     * </pre>
     */
    public static final int DIFFTYPE_REMOTE = 1;
    /**
     * What was locally changed comparing to recent head?
     * The Right pane contains local file.
     *
     * <p> Local addition, removal or change is displayed in
     * the right pane as addition, removal or change respectively
     * (i.e. not reversed as removal, addition or change).
     *
     * <pre>
     * diff from-HEAD to-LOCAL
     * </pre>
     */
    public static final int DIFFTYPE_ALL = 2;
    public static final String REVISION_PRISTINE = "PRISTINE"; // NOI18N

    public static final String REVISION_BASE = "BASE"; // NOI18N

    public static final String REVISION_CURRENT = "LOCAL"; // NOI18N

    public static final String REVISION_HEAD = "HEAD"; // NOI18N

    private final File baseFile;
    /**
     * Name of the file's property if the setup represents a property diff setup, null otherwise.
     */
    private final String propertyName;
    private final String firstRevision;
    private final String secondRevision;
    private FileInformation info;
    private DiffStreamSource firstSource;
    private DiffStreamSource secondSource;
    private DiffController view;
    private DiffNode node;
    private String title;

    public Setup(File baseFile, String propertyName, int type) {
        this.baseFile = baseFile;
        this.propertyName = propertyName;
        info = Git.getInstance().getFileStatusCache().getStatus(baseFile);
        int status = info.getStatus();

        ResourceBundle loc = NbBundle.getBundle(Setup.class);
        String firstTitle;
        String secondTitle;

        // the first source

        switch (type) {
            case DIFFTYPE_LOCAL:

                // from-BASE

                if (match(status, FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY | FileInformation.STATUS_VERSIONED_ADDEDLOCALLY)) {
                    firstRevision = REVISION_BASE;

                    firstTitle = loc.getString("MSG_DiffPanel_LocalNew"); // NOI18N

                } else if (match(status, FileInformation.STATUS_VERSIONED_NEWINREPOSITORY)) {
                    firstRevision = null;
                    firstTitle = NbBundle.getMessage(Setup.class, "LBL_Diff_NoLocalFile"); // NOI18N

                } else if (match(status, FileInformation.STATUS_VERSIONED_DELETEDLOCALLY | FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY)) {
                    firstRevision = REVISION_BASE;
                    firstTitle = MessageFormat.format(loc.getString("MSG_DiffPanel_BaseRevision"), new Object[]{firstRevision                            }); // NOI18N

                } else {
                    firstRevision = REVISION_BASE;
                    firstTitle = MessageFormat.format(loc.getString("MSG_DiffPanel_BaseRevision"), new Object[]{firstRevision                            }); // NOI18N

                }

                break;

            default:
                throw new IllegalArgumentException("Unknow diff type: " + type); // NOI18N

        }


        // the second source

        switch (type) {
            case DIFFTYPE_LOCAL:

                // to-LOCAL

                if (match(status, FileInformation.STATUS_VERSIONED_CONFLICT)) {
                    secondRevision = REVISION_CURRENT;
                    secondTitle = MessageFormat.format(loc.getString("MSG_DiffPanel_LocalConflict"), new Object[]{secondRevision                            }); // NOI18N

                } else if (match(status, FileInformation.STATUS_NOTVERSIONED_NEWLOCALLY | FileInformation.STATUS_VERSIONED_ADDEDLOCALLY)) {
                    secondRevision = REVISION_CURRENT;
                    secondTitle = loc.getString("MSG_DiffPanel_LocalNew"); // NOI18N

                } else if (match(status, FileInformation.STATUS_VERSIONED_NEWINREPOSITORY)) {
                    secondRevision = null;
                    secondTitle = NbBundle.getMessage(Setup.class, "LBL_Diff_NoLocalFile"); // NOI18N

                } else if (match(status, FileInformation.STATUS_VERSIONED_DELETEDLOCALLY | FileInformation.STATUS_VERSIONED_REMOVEDLOCALLY)) {
                    secondRevision = null;
                    secondTitle = loc.getString("MSG_DiffPanel_LocalDeleted"); // NOI18N

                } else {
                    secondRevision = REVISION_CURRENT;
                    secondTitle = MessageFormat.format(loc.getString("MSG_DiffPanel_LocalModified"), new Object[]{secondRevision                            }); // NOI18N

                }
                break;

            default:
                throw new IllegalArgumentException("Unknow diff type: " + type); // NOI18N

        }

        firstSource = new DiffStreamSource(baseFile, firstRevision, firstTitle);
        secondSource = new DiffStreamSource(baseFile, secondRevision, secondTitle);
        title = "<html>" + Git.getInstance().getGitAnnotator().annotateNameHtml(baseFile, info); // NOI18N

    }

    /**
     * Text file setup for arbitrary revisions.
     * @param firstRevision first revision or <code>null</code> for inital.
     * @param secondRevision second revision
     */
    public Setup(File baseFile, String firstRevision, String secondRevision) {
        this.baseFile = baseFile;
        this.propertyName = null;
        this.firstRevision = firstRevision;
        this.secondRevision = secondRevision;
        firstSource = new DiffStreamSource(baseFile, firstRevision, firstRevision);
        secondSource = new DiffStreamSource(baseFile, secondRevision, secondRevision);
    }

    public String getPropertyName() {
        return propertyName;
    }

    public File getBaseFile() {
        return baseFile;
    }

    public FileInformation getInfo() {
        return info;
    }

    public void setView(DiffController view) {
        this.view = view;
    }

    public DiffController getView() {
        return view;
    }

    public StreamSource getFirstSource() {
        return firstSource;
    }

    public StreamSource getSecondSource() {
        return secondSource;
    }

    public void setNode(DiffNode node) {
        this.node = node;
    }

    public DiffNode getNode() {
        return node;
    }

    public String toString() {
        return title;
    }

    /**
     * Loads data
     * @param group that carries shared state. Note that this group must not be executed later on. 
     */
    void initSources() throws IOException {
        if (firstSource != null) {
            firstSource.init();
        }
        if (secondSource != null) {
            secondSource.init();
        }
    }

    private static boolean match(int status, int mask) {
        return (status & mask) != 0;
    }
}
