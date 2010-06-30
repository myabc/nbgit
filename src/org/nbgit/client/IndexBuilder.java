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
package org.nbgit.client;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import org.nbgit.OutputLogger;
import org.eclipse.jgit.lib.GitIndex.Entry;
import org.eclipse.jgit.lib.GitIndex;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

/**
 * Wrapper for JGit's index API.
 */
public class IndexBuilder extends ClientBuilder {

    private static String ADDING = "A %s"; // NOI18N
    private static String UPDATING = "M %s"; // NOI18N
    private static String DELETING = "D %s"; // NOI18N
    private static String MOVING = "R %s -> %s"; // NOI18N
    private final GitIndex index;

    private IndexBuilder(Repository repository, GitIndex index) {
        super(repository);
        this.index = index;
    }

    public static IndexBuilder create(Repository repository) throws IOException {
        return new IndexBuilder(repository, repository.getIndex());
    }

    public static IndexBuilder create(File workDir) throws IOException {
        return create(toRepository(workDir));
    }

    public IndexBuilder add(File file) throws IOException {
        GitIndex.Entry entry = index.getEntry(toPath(file));
        String action = entry == null ? ADDING : UPDATING;
        log(action, file);
        addOrUpdateEntry(entry, file);
        return this;
    }

    public IndexBuilder addAll(Collection<File> files) throws IOException {
        for (File file : files)
            add(file);
        return this;
    }

    public IndexBuilder move(File src, File dst) throws IOException {
        log(MOVING, src, dst);
        removeEntry(src);
        addOrUpdateEntry(null, dst);
        return this;
    }

    public IndexBuilder delete(File file) {
        log(DELETING, file);
        removeEntry(file);
        return this;
    }

    public IndexBuilder deleteAll(Collection<File> files) {
        for (File file : files)
            delete(file);
        return this;
    }

    public void write() throws IOException {
        index.write();
    }

    public ObjectId writeTree() throws IOException {
        return index.writeTree();
    }

    public IndexBuilder log(OutputLogger logger) {
        return log(IndexBuilder.class, logger);
    }

    private void removeEntry(File file) {
        try {
            index.remove(repository.getWorkDir(), file);
        } catch (IOException willNeverHappen) {
        }
    }

    private void addOrUpdateEntry(GitIndex.Entry entry, File file) throws IOException {
        if (entry == null)
            entry = index.add(repository.getWorkDir(), file);
        else
            entry.update(file);
        entry.setAssumeValid(false);
    }

}
