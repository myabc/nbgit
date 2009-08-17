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
package org.nbgit;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import org.openide.awt.HtmlBrowser;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

/**
 *
 * @author Tomas Stupka
 */
public class OutputLogger {

    private InputOutput log;
    private String repositoryRootString;
    private static final RequestProcessor rp = new RequestProcessor("GitOutput", 1);
    public static final int MAX_LINES_TO_PRINT = 500;
    private static final String MSG_TOO_MANY_LINES = "The number of output lines is greater than 500; see message log for complete output";

    public static OutputLogger getLogger(String repositoryRoot) {
        if (repositoryRoot != null) {
            return new OutputLogger(repositoryRoot);
        } else {
            return new NullLogger();
        }
    }

    private OutputLogger(String repositoryRoot) {
        repositoryRootString = repositoryRoot;
        log = IOProvider.getDefault().getIO(repositoryRootString, false);
    }

    private OutputLogger() {
    }

    public void closeLog() {
        rp.post(new Runnable() {

            public void run() {
                log.getOut().flush();
                log.getOut().close();
                log.getErr().flush();
                log.getErr().close();
            }
        });
    }

    public void flushLog() {
        rp.post(new Runnable() {

            public void run() {
                log.getOut().flush();
                log.getErr().flush();
            }
        });
    }

    /**
     * Print contents of list to OutputLogger's tab
     *
     * @param list to print out
     *
     */
    public void output(final List<String> list) {
        if (list.isEmpty()) {
            return;
        }
        rp.post(new Runnable() {

            public void run() {
                log.select();
                OutputWriter out = log.getOut();

                int lines = list.size();
                if (lines > MAX_LINES_TO_PRINT) {
                    out.println(list.get(1));
                    out.println(list.get(2));
                    out.println(list.get(3));
                    out.println("...");
                    out.println(list.get(list.size() - 1));
                    out.println(MSG_TOO_MANY_LINES);
                    for (String s : list) {
                        Git.LOG.log(Level.WARNING, s);
                    }
                } else {
                    for (String s : list) {
                        out.println(s);
                    }
                }
                out.flush();
            }
        });
    }

    /**
     * Print msg to OutputLogger's tab
     *
     * @param String msg to print out
     *
     */
    public void output(final String msg) {
        if (msg == null) {
            return;
        }
        rp.post(new Runnable() {

            public void run() {
                log.select();

                log.getOut().println(msg);
                log.getOut().flush();
            }
        });
    }

    /**
     * Print msg to OutputLogger's tab in Red
     *
     * @param String msg to print out
     *
     */
    public void outputInRed(final String msg) {
        if (msg == null) {
            return;
        }
        rp.post(new Runnable() {

            public void run() {
                log.select();
                log.getErr().println(msg);
                log.getErr().flush();
            }
        });
    }

    /**
     * Print URL to OutputLogger's tab as an active Hyperlink
     *
     * @param String sURL to print out
     *
     */
    public void outputLink(final String sURL) {
        if (sURL == null) {
            return;
        }
        rp.post(new Runnable() {

            public void run() {
                log.select();
                try {
                    OutputWriter out = log.getOut();

                    OutputListener listener = new OutputListener() {

                        public void outputLineAction(OutputEvent ev) {
                            try {
                                HtmlBrowser.URLDisplayer.getDefault().showURL(new URL(sURL));
                            } catch (IOException ex) {
                                // Ignore
                            }
                        }

                        public void outputLineSelected(OutputEvent ev) {
                        }

                        public void outputLineCleared(OutputEvent ev) {
                        }
                    };
                    out.println(sURL, listener, true);
                    out.flush();
                } catch (IOException ex) {
                    // Ignore
                }
            }
        });
    }

    /**
     * Select and Clear OutputLogger's tab
     *
     * @param list to print out
     *
     */
    public void clearOutput() {
        rp.post(new Runnable() {

            public void run() {
                log.select();
                OutputWriter out = log.getOut();

                try {
                    out.reset();
                } catch (IOException ex) {
                    // Ignore Exception
                }
                out.flush();
            }
        });
    }

    private static class NullLogger extends OutputLogger {

        @Override
        public void closeLog() {
        }

        @Override
        public void flushLog() {
        }

        @Override
        public void output(List<String> list) {
        }

        @Override
        public void output(String msg) {
        }

        @Override
        public void outputInRed(String msg) {
        }

        @Override
        public void outputLink(final String sURL) {
        }

        @Override
        public void clearOutput() {
        }
    }
}
