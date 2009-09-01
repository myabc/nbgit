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

import java.io.IOException;
import org.nbgit.junit.RepositoryTestCase;
import org.spearce.jgit.dircache.DirCache;
import org.spearce.jgit.dircache.DirCacheEntry;

public class IndexBuilderTest extends RepositoryTestCase {

    public IndexBuilderTest() {
        super(IndexBuilderTest.class.getSimpleName());
    }

    public void testCreate() throws Exception {
        assertNotNull(IndexBuilder.create(repository));
        assertNotNull(IndexBuilder.create(workDir));
    }

    public void testWriteUnmodified() throws Exception {
        copyFile(toGitDirFile("index"), toGitDirFile("index.orig"));
        IndexBuilder.create(repository).write();
        assertFile(toGitDirFile("index.orig"), toGitDirFile("index"));
    }

    public void testAdd() throws Exception {
        IndexBuilder.create(repository).
                add(toWorkDirFile("d")).
                write();
        compareIndexFiles();
    }

    public void testAddAll() throws Exception {
        IndexBuilder.create(repository).
                addAll(toFiles(workDir, "b/e/f", "d", "g/h")).
                write();
        compareIndexFiles();
    }

    public void testAddAllUnordered() throws Exception {
        IndexBuilder.create(repository).
                addAll(toFiles(workDir, "2", "3", "1")).
                write();
        compareIndexFiles();
    }

    public void testDelete() throws Exception {
        IndexBuilder.create(repository).
                delete(toWorkDirFile("b/e/f")).
                write();
        compareIndexFiles();
    }

    public void testDeleteAll() throws Exception {
        IndexBuilder.create(repository).
                deleteAll(toFiles(workDir, "a", "b/e/f", "d")).
                write();
        compareIndexFiles();
    }

    public void testDeleteAllUnordered() throws Exception {
        IndexBuilder.create(repository).
                deleteAll(toFiles(workDir, "d", "b/e/f", "b/c")).
                write();
        compareIndexFiles();
    }

    public void testMove() throws Exception {
        toWorkDirFile("b/e/f").renameTo(toWorkDirFile("f"));
        IndexBuilder.create(repository).
                move(toWorkDirFile("b/e/f"), toWorkDirFile("f")).
                write();
        compareIndexFiles();
    }

    private void compareIndexFiles() throws IOException {
        refDirCache(DirCache.read(repository));
        compareReferenceFiles();
    }

    private void refDirCache(DirCache index) {
        for (int i = 0; i < index.getEntryCount(); i++)
            refDirCacheEntry(index.getEntry(i));
    }

    private void refDirCacheEntry(DirCacheEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append(entry.getFileMode().toString()).
                append(" ").append(entry.getObjectId().name()).
                append(" ").append(entry.getStage()).
                append("\t").append(entry.getPathString());
        ref(builder.toString());
    }

}