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

import org.nbgit.junit.RepositoryTestCase;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;

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
        assertFalse(cache.isExcluded(toWorkDirFile("README")));
    }

    public void testComplexWorkingDirectory() throws Exception {
        TreeWalk walk = new TreeWalk(repository);
        walk.reset();
        walk.addTree(new FileTreeIterator(workDir));
        walk.setRecursive(true);
        while (walk.next()) {
            String path = walk.getPathString();
            if (!cache.isExcluded(toWorkDirFile(path)))
                ref(path);
        }

        compareReferenceFiles();
    }

}
