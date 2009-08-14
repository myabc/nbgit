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
package org.nbgit.util.exclude;

import java.io.File;
import org.nbgit.junit.RepositoryTestCase;

public class ExcludeCacheTest extends RepositoryTestCase {

    ExcludeCache cache;

    public ExcludeCacheTest() {
        super("Test of the exclude cache infrastructure");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        cache = ExcludeCache.create(repository);
    }

    public void testEmptyCache() {
        assertIncluded("README");
    }

    public void testComplexWorkingDirectory() {
        String[] included = {
            "#comment",
            ".gitignore",
            ".gitmodules",
            ".hidden",
            "README.txt",
            "all-ignored-but-gitignore/.gitignore",
            "build.xml",
            "docs/.gitattributes",
            "docs/.gitignore",
            "docs/NOT-IGNORED.txt",
            "manifest.mf",
            "nbproject/build-impl.xml",
            "nbproject/genfiles.properties",
            "nbproject/private/private.properties",
            "nbproject/project.properties",
            "nbproject/project.xml",
            "path/.gitignore",
            "path/also/to/other.file",
            "path/to/file",
            "src/org/example/File.java",
            "test/org/example/FileTest.java",
        };
        String[] excluded = {
            "IGNORED.txt",
            "all-ignored/.gitignore",
            "build/classes/org/example/File.class",
            "build/test/unit/classes/org/example/FileTest.class",
            "misc/.gitignore",
            "misc/a",
            "misc/b",
            "misc/c",
            "path/three/a",
            "path/to/other.file",
            "random.pdf",
        };
        for (String include : included)
            assertIncluded(include);
        for (String exclude : excluded)
            assertExcluded(exclude);
    }

    private void assertExcluded(String path) {
        if (!cache.isExcluded(toFile(path)))
            fail("path is not excluded: " + path);
    }

    private void assertIncluded(String path) {
        if (cache.isExcluded(toFile(path)))
            fail("path is not included: " + path);
    }

    private File toFile(String path) {
        return new File(workDir, path.replace('/', File.separatorChar));
    }
}
