package org.nbgit.ui.log;

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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.openide.util.Exceptions;
import org.spearce.jgit.errors.CorruptObjectException;
import org.spearce.jgit.errors.IncorrectObjectTypeException;
import org.spearce.jgit.errors.MissingObjectException;
import org.spearce.jgit.lib.AnyObjectId;
import org.spearce.jgit.lib.ObjectId;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.RevCommit;
import org.spearce.jgit.revwalk.RevWalk;
import org.spearce.jgit.treewalk.TreeWalk;

/**
 * Describes log information for a file. This is the result of doing a
 * cvs log command. The fields in instances of this object are populated
 * by response handlers.
 *
 * @author Maros Sandor
 */
public class RepositoryRevision extends RevCommit {

    private final Repository repo;
    /**
     * List of events associated with the revision.
     */
    private final List<Event> events = new ArrayList<Event>(1);

    private RepositoryRevision(AnyObjectId id, Repository repo) {
        super(id);
        this.repo = repo;
    }

    public String getRepositoryRootUrl() {
        return repo.getDirectory().getAbsolutePath();
    }

    Iterable<Event> createEvents(Walk walk) {
        try {
            initEvents(walk.getTreeWalk());
        } catch (MissingObjectException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IncorrectObjectTypeException ex) {
            Exceptions.printStackTrace(ex);
        } catch (CorruptObjectException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }

        return events;
    }

    private ObjectId[] getTrees() {
        final ObjectId[] r = new ObjectId[getParentCount() + 1];
        for (int i = 0; i < r.length - 1; i++) {
            r[i] = getParent(i).getTree().getId();
        }
        r[r.length - 1] = getTree().getId();
        return r;
    }

    private char getStatus(TreeWalk walk, int mode0, int mode1) {
        if (mode0 == 0 && mode1 != 0) {
            return 'A';
        } else if (mode0 != 0 && mode1 == 0) {
            return 'D';
        } else if (!walk.idEqual(0, 1)) {
            return 'M';
        } else if (mode0 != mode1) {
            return 'm';
        }
        return 0;
    }

    private void initEvents(final TreeWalk walk)
            throws MissingObjectException, IncorrectObjectTypeException,
            CorruptObjectException, IOException {
        ObjectId[] trees = getTrees();
        final int revTree = trees.length - 1;

        walk.reset(trees);

        switch (trees.length) {
            case 1:
                /* Inital commit. */
                while (walk.next()) {
                    events.add(new Event(walk.getPathString(), 'A'));
                }
                break;
            case 2:
                while (walk.next()) {
                    int mode0 = walk.getRawMode(0);
                    int mode1 = walk.getRawMode(1);
                    char status = getStatus(walk, mode0, mode1);
                    if (status == 0) {
                        continue;
                    }
                    events.add(new Event(walk.getPathString(), status));
                }
                break;
            default:
                /* Merge. */
                while (walk.next()) {
                    int mode0 = 0;
                    int mode1 = walk.getRawMode(revTree);
                    int i;

                    for (i = 0; i < revTree; i++) {
                        int mode = walk.getRawMode(i);
                        if (mode == mode1 && walk.idEqual(i, revTree)) {
                            break;
                        }
                        mode0 |= mode;
                    }

                    if (i != revTree) {
                        continue;
                    }
                    char status = getStatus(walk, mode0, mode1);
                    if (status == 0) {
                        continue;
                    }
                    events.add(new Event(walk.getPathString(), status));
                }
                break;
        }

    }

    public List<Event> getEvents() {
        return events;
    }

    public String getAuthor() {
        return getAuthorIdent().getName();
    }

    String getMessage() {
        return getFullMessage();
    }

    public String getRevision() {
        return getId().name();
    }

    public Date getDate() {
        return getCommitterIdent().getWhen();
    }

    public class Event {

        /**
         * The file or folder that this event is about. It may be null if the File cannot be computed.
         */
        private File file;
        private GitLogMessageChangedPath changedPath;
        private String name;
        private String path;

        private Event(GitLogMessageChangedPath changedPath) {
            this.changedPath = changedPath;
            name = changedPath.getPath().substring(changedPath.getPath().lastIndexOf('/') + 1);

            int indexPath = changedPath.getPath().lastIndexOf('/');
            if (indexPath > -1) {
                path = changedPath.getPath().substring(0, indexPath);
            } else {
                path = "";
            }
        }

        private Event(String pathString, char c) {
            this(new GitLogMessageChangedPath(pathString, c));
        }

        public RepositoryRevision getLogInfoHeader() {
            return RepositoryRevision.this;
        }

        public GitLogMessageChangedPath getChangedPath() {
            return changedPath;
        }

        /** Getter for property file.
         * @return Value of property file.
         */
        public File getFile() {
            return file;
        }

        /** Setter for property file.
         * @param file New value of property file.
         */
        public void setFile(File file) {
            this.file = file;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            StringBuffer text = new StringBuffer();
            text.append("\t");
            text.append(getPath());
            return text.toString();
        }
    }

    public static class Walk extends RevWalk {

        private final TreeWalk walk;

        public Walk(Repository repo) {
            super(repo);
            walk = new TreeWalk(repo);
            walk.setRecursive(true);
        }

        @Override
        protected RevCommit createCommit(final AnyObjectId id) {
            return new RepositoryRevision(id, getRepository());
        }

        private TreeWalk getTreeWalk() {
            return walk;
        }
    }
}
