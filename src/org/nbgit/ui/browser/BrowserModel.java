/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Jonas Fonseca <fonseca@diku.dk>
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file.
 *
 * This particular file is subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
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
package org.nbgit.ui.browser;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import org.nbgit.Git;
import org.spearce.jgit.lib.Ref;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.revwalk.RevCommitList;
import org.spearce.jgit.treewalk.filter.PathFilterGroup;
import org.spearce.jgit.treewalk.filter.TreeFilter;

public class BrowserModel {

    public static final String CONTENT_ID = "ID";
    private final Document document = new PlainDocument();
    private final Repository repository;
    private final Set<String> paths = new HashSet<String>();

    private RevCommitList commitList;
    private final String[] ids;

    public BrowserModel(Set<File> fileSet, String... ids) {
        File[] files = fileSet.toArray(new File[fileSet.size()]);
        File root = Git.getInstance().getTopmostManagedParent(files[0]);
        repository = Git.getInstance().getRepository(root);
        for (File file : files) {
            file = file.getAbsoluteFile();
            // If the work directory root is included disable path limiting.
            if (file.getPath().length() == root.getPath().length()) {
                paths.clear();
                break;
            }
            paths.add(Repository.stripWorkDir(root, file));
        }
        this.ids = ids;
    }

    public Repository getRepository() {
        return repository;
    }

    public boolean hasPaths() {
        return !paths.isEmpty();
    }

    public TreeFilter createPathFilter() {
        return PathFilterGroup.createFromStrings(paths);
    }

    public Document getDocument() {
        return document;
    }

    public void setContentId(String id) {
        document.putProperty(CONTENT_ID, id);
    }

    public void setContent(String str) {
        try {
            document.remove(0, document.getLength());
            document.insertString(0, str, SimpleAttributeSet.EMPTY);
        } catch (BadLocationException ex) {
        }
    }

    public Set<Ref> getReferences() {
        Set<Ref> refs = new HashSet<Ref>();
        for (String id : ids) {
            refs.add(repository.getAllRefs().get(id));
        }
        if (refs.isEmpty())
            refs.addAll(repository.getAllRefs().values());
        return refs;
    }

    public RevCommitList getCommitList() {
        return commitList;
    }

    public void setCommitList(RevCommitList commitList) {
        this.commitList = commitList;
    }

}
