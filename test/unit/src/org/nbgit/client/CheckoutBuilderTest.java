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

import java.io.FileNotFoundException;
import org.nbgit.junit.RepositoryTestCase;

public class CheckoutBuilderTest extends RepositoryTestCase {

    public CheckoutBuilderTest() {
        super(CheckoutBuilderTest.class.getSimpleName());
    }

    public void testCreate() throws Exception {
        assertNotNull(CheckoutBuilder.create(repository));
        assertNotNull(CheckoutBuilder.create(workDir));
    }

    public void testFileSamePath() throws Exception {
        CheckoutBuilder.create(repository).
                revision("A").
                file(toWorkDirFile("a"), toWorkDirFile("a.txt")).
                checkout();
        assertFile(getGoldenFile(), toWorkDirFile("a.txt"));
    }

    public void testFileOtherPath() throws Exception {
        CheckoutBuilder.create(repository).
                revision("A").
                file(toWorkDirFile("a"), toWorkDirFile("other/file.txt")).
                checkout();
        assertFile(getGoldenFile(), toWorkDirFile("other/file.txt"));
    }

    public void testFileSupportsExecutable() throws Exception {
        CheckoutBuilder.create(repository).
                revision("A").
                file(toWorkDirFile("a"), toWorkDirFile("a.txt")).
                checkout();
        assertTrue("File is not executable", !isExecutable(toWorkDirFile("a.txt")));

        CheckoutBuilder.create(repository).
                revision("B").
                file(toWorkDirFile("a"), toWorkDirFile("a.exe")).
                checkout();
        assertTrue("File is executable", isExecutable(toWorkDirFile("a.exe")));
    }

    public void testFiles() throws Exception {
        CheckoutBuilder.create(repository).
                revision("A").
                files(toFiles(workDir, "a", "b/c", "d")).
                checkout();
        IndexBuilder.create(repository).
                addAll(toFiles(workDir, "a", "b/c", "d")).
                write();
        compareIndexFiles();
    }

    public void testBackup() throws Exception {
        toWorkDirFile("a").createNewFile();
        CheckoutBuilder.create(repository).
                revision("A").
                file(toWorkDirFile("a"), toWorkDirFile("a")).
                backup(true).
                checkout();
        assertTrue("Checked out file exists", toWorkDirFile("a").exists());
        assertTrue("Backup file exists", toWorkDirFile("a.orig").exists());
    }

    public void testBackupExisting() throws Exception {
        toWorkDirFile("a").createNewFile();
        toWorkDirFile("a.orig").createNewFile();
        CheckoutBuilder.create(repository).
                revision("A").
                file(toWorkDirFile("a"), toWorkDirFile("a")).
                backup(true).
                checkout();
        assertTrue("Checked out file exists", toWorkDirFile("a").exists());
        assertTrue("Backup file exists", toWorkDirFile("a.0.orig").exists());
    }

    public void testRevisionNonExisting() throws Exception {
        try {
            CheckoutBuilder.create(repository).
                    revision("fail");
            fail("No exception thrown");
        } catch (IllegalArgumentException error) {
            assertEquals("fail", error.getMessage());
        }
    }

    public void testFileNonExisting() throws Exception {
        try {
            CheckoutBuilder.create(repository).
                    revision("A").
                    file(toWorkDirFile("fail"), toWorkDirFile("fail.txt"));
            fail("No exception thrown");
        } catch (FileNotFoundException error) {
            assertEquals("fail", error.getMessage());
        }
    }

    public void testFilesNonExisting() throws Exception {
        try {
            CheckoutBuilder.create(repository).
                    revision("A").
                    files(toFiles(workDir, "fail/1", "fail.2"));
            fail("No exception thrown");
        } catch (FileNotFoundException error) {
            assertEquals("fail/1", error.getMessage());
        }
    }

}
