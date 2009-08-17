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

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;
import org.nbgit.Git;
import org.nbgit.GitProgressSupport;
import org.nbgit.OutputLogger;
import org.nbgit.util.GitCommand;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.filter.AndRevFilter;
import org.spearce.jgit.revwalk.filter.AuthorRevFilter;
import org.spearce.jgit.revwalk.filter.MessageRevFilter;
import org.spearce.jgit.revwalk.filter.RevFilter;

/**
 * Executes searches in Search History panel.
 *
 * @author Maros Sandor
 */
class SearchExecutor implements Runnable {

    public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");  // NOI18N
    static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");  // NOI18N
    static final DateFormat[] dateFormats = new DateFormat[]{
        fullDateFormat,
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"), // NOI18N
        simpleDateFormat,
        new SimpleDateFormat("yyyy-MM-dd"), // NOI18N
    };
    private final SearchHistoryPanel master;
    private Map<String, Set<File>> workFiles;
    private Map<String, File> pathToRoot;
    private final SearchCriteriaPanel criteria;
    private boolean filterUsername;
    private boolean filterMessage;
    private int completedSearches;
    private boolean searchCanceled;
    private List<RepositoryRevision> results = new ArrayList<RepositoryRevision>();

    public SearchExecutor(SearchHistoryPanel master) {
        this.master = master;
        criteria = master.getCriteria();
        filterUsername = criteria.getUsername() != null;
        filterMessage = criteria.getCommitMessage() != null;

        pathToRoot = new HashMap<String, File>();
        if (searchingUrl()) {
            String rootPath = Git.getInstance().getTopmostManagedParent(master.getRoots()[0]).toString();
            pathToRoot.put(rootPath, master.getRoots()[0]);
        } else {
            workFiles = new HashMap<String, Set<File>>();
            for (File file : master.getRoots()) {
                String rootPath = Git.getInstance().getTopmostManagedParent(file).toString();

                Set<File> set = workFiles.get(rootPath);
                if (set == null) {
                    set = new HashSet<File>(2);
                    workFiles.put(rootPath, set);
                }
                set.add(file);
            }
        }

    }

    public void run() {

        final String fromRevision = criteria.getFrom();
        final String toRevision = criteria.getTo();

        completedSearches = 0;
        if (searchingUrl()) {
            RequestProcessor rp = Git.getInstance().getRequestProcessor(master.getRepositoryUrl());
            GitProgressSupport support = new GitProgressSupport() {

                public void perform() {
                    OutputLogger logger = getLogger();
                    search(master.getRepositoryUrl(), null, fromRevision, toRevision, this, logger);
                }
            };
            support.start(rp, master.getRepositoryUrl(), NbBundle.getMessage(SearchExecutor.class, "MSG_Search_Progress")); // NOI18N
        } else {
            for (final String rootUrl : workFiles.keySet()) {
                final Set<File> files = workFiles.get(rootUrl);
                RequestProcessor rp = Git.getInstance().getRequestProcessor(rootUrl);
                GitProgressSupport support = new GitProgressSupport() {

                    public void perform() {
                        OutputLogger logger = getLogger();
                        search(rootUrl, files, fromRevision, toRevision, this, logger);
                    }
                };
                support.start(rp, rootUrl, NbBundle.getMessage(SearchExecutor.class, "MSG_Search_Progress")); // NOI18N
            }
        }
    }

    private void search(String rootUrl, Set<File> files, String fromRevision,
            String toRevision, GitProgressSupport progressSupport, OutputLogger logger) {
        if (progressSupport.isCanceled()) {
            searchCanceled = true;
            return;
        }

        RepositoryRevision.Walk walk;
        /*
        if (master.isIncomingSearch()) {
        messages = GitCommand.getIncomingMessages(rootUrl, toRevision, master.isShowMerges(), logger);
        }else if (master.isOutSearch()) {
        messages = GitCommand.getOutMessages(rootUrl, master.isShowMerges(), logger);
        } else {
         */
        walk = GitCommand.getLogMessages(rootUrl, files, fromRevision, toRevision,
                master.isShowMerges(), logger);
        //}
        if (walk != null) {
            appendResults(rootUrl, walk);
        }
    }

    private void setupRevFilter(RepositoryRevision.Walk walk) {
        RevFilter filter = walk.getRevFilter();

        if (filterUsername) {
            RevFilter author = AuthorRevFilter.create(criteria.getUsername());

            if (filter == RevFilter.ALL) {
                filter = author;
            } else {
                filter = AndRevFilter.create(filter, author);
            }
        }

        if (filterMessage) {
            RevFilter message = MessageRevFilter.create(criteria.getCommitMessage());
            if (filter == RevFilter.ALL) {
                filter = message;
            } else {
                filter = AndRevFilter.create(filter, message);
            }
        }

        walk.setRevFilter(filter);
    }

    /**
     * Processes search results from a single repository.
     *
     * @param rootUrl repository root URL
     * @param logMessages events in chronological order
     */
    private synchronized void appendResults(String rootUrl, RepositoryRevision.Walk walk) {
        Map<String, String> historyPaths = new HashMap<String, String>();
        RevFilter filter = walk.getRevFilter();

        setupRevFilter(walk);

        // traverse in reverse chronological order
        for (RevCommit commit : walk) {
            RepositoryRevision rev = (RepositoryRevision) commit;
            for (RepositoryRevision.Event event : rev.createEvents(walk)) {
                if (event.getChangedPath().getAction() == 'A' && event.getChangedPath().getCopySrcPath() != null) {
                    // TBD: Need to handle Copy status
                    String existingMapping = historyPaths.get(event.getChangedPath().getPath());
                    if (existingMapping == null) {
                        existingMapping = event.getChangedPath().getPath();
                    }
                    historyPaths.put(event.getChangedPath().getCopySrcPath(), existingMapping);
                }
                String originalFilePath = event.getChangedPath().getPath();
                for (String srcPath : historyPaths.keySet()) {
                    if (originalFilePath.startsWith(srcPath) &&
                            (originalFilePath.length() == srcPath.length() || originalFilePath.charAt(srcPath.length()) == '/')) {
                        originalFilePath = historyPaths.get(srcPath) + originalFilePath.substring(srcPath.length());
                        break;
                    }
                }
                File file = new File(rootUrl + File.separator + originalFilePath);
                event.setFile(file);
            }
            results.add(rev);
        }
        if (results.isEmpty()) {
            results = null;
        }
        checkFinished();
    }

    private boolean searchingUrl() {
        return master.getRepositoryUrl() != null;
    }

    private void checkFinished() {
        completedSearches++;
        if (searchingUrl() && completedSearches >= 1 || workFiles.size() == completedSearches) {
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    master.setResults(results);
                }
            });
        }
    }
}
