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
package org.nbgit.ui.custom;

import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import org.nbgit.Git;
import org.nbgit.GitProgressSupport;
import org.nbgit.OutputLogger;
import org.nbgit.StatusCache;
import org.nbgit.StatusInfo;
import org.nbgit.ui.ContextAction;
import org.nbgit.util.GitUtils;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.eclipse.jgit.lib.Repository;

/**
 * Custom action container.
 */
public class CustomAction extends ContextAction {

    private final File path;
    private final String args;
    private final boolean showOutput;
    private final boolean showDirty;
    private final boolean workDirRoot;

    CustomAction(CustomActionBuilder builder) {
        super(builder.getName(), builder.getContext());
        this.path = new File(builder.getPath());
        this.args = builder.getArgs();
        this.showOutput = builder.isShowOutput();
        this.showDirty = builder.isShowDirty();
        this.workDirRoot = builder.isWorkDirRoot();
    }

    @Override
    public boolean isEnabled() {
        final File root = GitUtils.getProjectFile(context);
        if (root == null) {
            return false;
        }
        if (showDirty) {
            return true;
        }
        StatusCache cache = Git.getInstance().getStatusCache();
        return !cache.containsFileOfStatus(context, StatusInfo.STATUS_LOCAL_CHANGE);
    }

    @Override
    protected void performAction(ActionEvent event) {
        File root = GitUtils.getProjectFile(context);
        RequestProcessor rp = Git.getInstance().getRequestProcessor(root.getAbsolutePath());
        Repository repo = Git.getInstance().getRepository(root);
        final List<String> command = new ArrayList<String>();

        command.add(path.getAbsolutePath());
        for (String arg : args.split(" ")) {
            if (arg.equals("{head}")) {
                try {
                    command.add(repo.getFullBranch());
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            } else if (arg.equals("{files}")) {
                for (File file : context.getFiles()) {
                    command.add(file.getAbsolutePath());
                }
            } else {
                command.add(arg);
            }
        }

        final File execDir;
        if (context.getRootFiles().size() == 1 && !workDirRoot) {
            execDir = context.getRootFiles().iterator().next();
        } else {
            execDir = root;
        }

        GitProgressSupport support = new GitProgressSupport() {

            public void perform() {
                execute(command, execDir, showOutput ? getLogger() : null);
            }

        };

        /* TODO: Use MessageFormat */
        support.start(rp, root.getAbsolutePath(),
            NbBundle.getMessage(CustomAction.class, "CustomActionProgress") + " " + path.getName()); // NOI18N
    }

    private static void execute(List<String> command, File dir, OutputLogger logger) {
        BufferedReader input = null;
        Process proc = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(dir);
            proc = builder.start();

            if (logger != null) {
                input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while ((line = input.readLine()) != null) {
                    logger.output(line);
                }
                input.close();
                input = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                while ((line = input.readLine()) != null) {
                    logger.output(line);
                }
                input.close();
                input = null;
            }

            try {
                proc.waitFor();
            } catch (InterruptedException e) {
            }
        } catch (InterruptedIOException e) {
            if (proc != null) {
                try {
                    proc.getInputStream().close();
                    proc.getOutputStream().close();
                    proc.getErrorStream().close();
                } catch (IOException ioex) {
                    // Just ignore. Closing streams.
                }
                proc.destroy();
            }
        } catch (IOException e) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ioex) {
                    // Just ignore. Closing streams.
                }
                input = null;
            }
        }
    }

}
