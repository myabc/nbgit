/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008 Jonas Fonseca <fonseca@diku.dk>
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
package org.nbgit.ui.commit;

import org.junit.Test;
import static org.junit.Assert.*;

public class StripSpaceTest {

    @Test
    public void endOfLine() {
        assertEquals("abc\n", CommitAction.stripSpace("abc \t \r\n"));
        assertEquals("a\nbc\n", CommitAction.stripSpace("a \nbc \t \n"));
    }

    @Test
    public void emptyLines() {
        assertEquals("abc\n", CommitAction.stripSpace("\n\n\nabc \t \n"));
        assertEquals("a\nbc\n", CommitAction.stripSpace("a \nbc \t \n\n\n\n\n"));
        assertEquals("", CommitAction.stripSpace(" \t \n   \n\n\n\n"));
    }

    @Test
    public void consecutiveEmptyLines() {
        assertEquals("a\n\nb\nc\n", CommitAction.stripSpace("\n\n\na\n\n\n\nb\nc \t \n"));
        assertEquals("a\n\nbc\n", CommitAction.stripSpace("a \n\n\n\n\nbc \t \n\n\n\n\n"));
    }

}
