/*
 * Copyright (C) 2002-2006 Les Hazlewood
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the
 *
 * Free Software Foundation, Inc.
 * 59 Temple Place, Suite 330
 * Boston, MA 02111-1307
 * USA
 *
 * Or, you may view it online at
 * http://www.opensource.org/licenses/lgpl-license.php
 */
package org.netbeans.modules.git;

import java.util.regex.Pattern;


public class GitMail {
   
    /* Validation of email address from www.leshazlewood.com */

    //RFC 2822 token definitions for valid email - only used together to form a java Pattern object:
    private static final String sp = "\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~"; // NOI18N
    private static final String atext = "[a-zA-Z0-9" + sp + "]"; // NOI18N
    private static final String atom = atext + "+";  // NOI18N //one or more atext chars
    private static final String dotAtom = "\\." + atom; // NOI18N
    private static final String localPart = atom + "(" + dotAtom + ")*";  // NOI18N //one atom followed by 0 or more dotAtoms.

    //RFC 1035 tokens for domain names:
    private static final String letter = "[a-zA-Z]"; // NOI18N
    private static final String letDig = "[a-zA-Z0-9]"; // NOI18N
    private static final String letDigHyp = "[a-zA-Z0-9-]"; // NOI18N
    public static final String rfcLabel = letDig + letDigHyp + "{0,61}" + letDig; // NOI18N
    private static final String domain = rfcLabel + "(\\." + rfcLabel + ")*\\." + letter + "{2,6}"; // NOI18N

    //Combined together, these form the allowed email regexp allowed by RFC 2822:
    private static final String addrSpec = "^" + localPart + "@" + domain + "$"; // NOI18N


    //now compile it:
    public static final Pattern VALID_PATTERN = Pattern.compile( addrSpec );

    public static Boolean isEmailValid(String email) {
        /*
         * Email in the form username@domain is allowed
         */
        email = email.trim();
        return VALID_PATTERN.matcher(email).matches();
    } 
}