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
package org.nbgit.ui.log;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.UIManager;
import org.openide.util.NbBundle;

/**
 * @author Maros Sandor
 */
class Divider extends JPanel {

    public static final int DIVIDER_CLICKED = 1;
    public static final int DOWN = 0;
    public static final int UP = 1;
    private Color bkg;
    private Color sbkg;
    private Color arrowColor;
    private Color selectedArrowColor;
    private ActionListener listener;
    private int arrowDirection;

    public Divider(ActionListener listener) {
        this.listener = listener;
        enableEvents(MouseEvent.MOUSE_ENTERED | MouseEvent.MOUSE_EXITED | MouseEvent.MOUSE_CLICKED);
        bkg = getBackground();
        sbkg = UIManager.getColor("TextField.selectionBackground"); // NOI18N
        selectedArrowColor = UIManager.getColor("TextField.selectionForeground"); // NOI18N
        arrowColor = UIManager.getColor("TextField.inactiveForeground"); // NOI18N
        getAccessibleContext().setAccessibleName(NbBundle.getMessage(Divider.class, "ACSN_Divider"));
        getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(Divider.class, "ACSD_Divider"));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(Integer.MAX_VALUE, 6);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, 6);
    }

    public void setArrowDirection(int direction) {
        arrowDirection = direction;
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        super.processMouseEvent(e);
        if (e.getID() == MouseEvent.MOUSE_ENTERED) {
            setBackground(sbkg);
            repaint();
        }
        if (e.getID() == MouseEvent.MOUSE_EXITED) {
            setBackground(bkg);
            repaint();
        }
        if (e.getID() == MouseEvent.MOUSE_CLICKED) {
            listener.actionPerformed(new ActionEvent(this, DIVIDER_CLICKED, ""));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension dim = getSize();
        if (getBackground().equals(bkg)) {
            g.setColor(arrowColor);
        } else {
            g.setColor(selectedArrowColor);
        }
        int mid = dim.width / 2;
        if (arrowDirection == DOWN) {
            g.drawLine(mid - 4, 1, mid + 4, 1);
            g.drawLine(mid - 3, 2, mid + 3, 2);
            g.drawLine(mid - 2, 3, mid + 2, 3);
            g.drawLine(mid - 1, 4, mid + 1, 4);
        } else if (arrowDirection == UP) {
            g.drawLine(mid - 4, 4, mid + 4, 4);
            g.drawLine(mid - 3, 3, mid + 3, 3);
            g.drawLine(mid - 2, 2, mid + 2, 2);
            g.drawLine(mid - 1, 1, mid + 1, 1);
        }
    }
}
