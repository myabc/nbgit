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
import org.nbgit.Git;
import org.nbgit.OutputLogger;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;

public class ClientBuilder {

    protected final Repository repository;
    protected OutputLogger logger;
    private Boolean trustFileMode;
    private int loggedLines;

    protected ClientBuilder(Repository repository) {
        this.repository = repository;
    }

    protected <T extends ClientBuilder> T log(Class<T> clazz, OutputLogger logger) {
        this.logger = logger;
        return (T) this;
    }

    protected void log(String format, Object... args) {
        if (logger != null && loggedLines < OutputLogger.MAX_LINES_TO_PRINT) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof File)
                    args[i] = toPath((File) args[i]);
            }
            logger.output(String.format(format, args));
            loggedLines++;
        }
    }

    protected String toPath(File file) {
        return Repository.stripWorkDir(repository.getWorkDir(), file);
    }

    protected static Repository toRepository(File workDir) {
        return Git.getInstance().getRepository(workDir);
    }

    protected boolean isExecutable(File file) {
        return trustFileMode() && FS.INSTANCE.canExecute(file);
    }

    protected boolean setExecutable(File file, boolean executable) {
        return trustFileMode() && FS.INSTANCE.setExecute(file, executable);
    }

    private boolean trustFileMode() {
        if (trustFileMode == null) {
            boolean supported = FS.INSTANCE.supportsExecute();
            boolean configured = repository.getConfig().getBoolean("core", null, "filemode", true);
            trustFileMode = supported && configured;
        }
        return trustFileMode.booleanValue();
    }

}
