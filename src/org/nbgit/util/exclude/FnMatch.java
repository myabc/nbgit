/*
 * Copyright (c) 1989, 1993, 1994
 *      The Regents of the University of California.  All rights reserved.
 *
 * This code is derived from software contributed to Berkeley by
 * Guido van Rossum.
 * Ported to Java by Jonas Fonseca.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
/*      $OpenBSD: fnmatch.c,v 1.13 2006/03/31 05:34:14 deraadt Exp $        */
package org.nbgit.util.exclude;

import java.util.EnumSet;

/*
 * Function fnmatch() as specified in POSIX 1003.2-1992, section B.6.
 * Compares a filename or pathname to a pattern.
 */
public class FnMatch {

    public static enum Flag {

        /** Disable backslash escaping. */
        NOESCAPE,
        /** Slash must be matched by slash. */
        PATHNAME,
        /** Period must be matched by period. */
        PERIOD,
        /** Ignore /<tail> after Imatch. */
        LEADING_DIR,
        /** Case insensitive search. */
        CASEFOLD
    }
    private static final int RANGE_ERROR = -1;
    private static final int RANGE_NOMATCH = 0;

    public static boolean fnmatch(String pattern, String string, EnumSet<Flag> flags) {
        return match(pattern, 0, string, 0, flags);
    }

    public static boolean fnmatch(String pattern, String string, int stringPos, Flag flag) {
        return match(pattern, 0, string, stringPos, EnumSet.of(flag));
    }

    public static boolean fnmatch(String pattern, String string, int stringPos) {
        return match(pattern, 0, string, stringPos, EnumSet.noneOf(Flag.class));
    }

    public static boolean fnmatch(String pattern, String string) {
        return fnmatch(pattern, string, 0);
    }

    private static boolean match(String pattern, int patternPos,
            String string, int stringPos, EnumSet<Flag> flags) {
        char c;

        while (true) {
            if (patternPos >= pattern.length()) {
                if (flags.contains(Flag.LEADING_DIR) && string.charAt(stringPos) == '/') {
                    return true;
                }
                return stringPos == string.length();
            }
            c = pattern.charAt(patternPos++);
            switch (c) {
                case '?':
                    if (stringPos >= string.length()) {
                        return false;
                    }
                    if (string.charAt(stringPos) == '/' && flags.contains(Flag.PATHNAME)) {
                        return false;
                    }
                    if (hasLeadingPeriod(string, stringPos, flags)) {
                        return false;
                    }
                    ++stringPos;
                    continue;
                case '*':
                    /* Collapse multiple stars. */
                    while (patternPos < pattern.length() &&
                            (c = pattern.charAt(patternPos)) == '*') {
                        patternPos++;
                    }

                    if (hasLeadingPeriod(string, stringPos, flags)) {
                        return false;
                    }

                    /* Optimize for pattern with * at end or before /. */
                    if (patternPos == pattern.length()) {
                        if (flags.contains(Flag.PATHNAME)) {
                            return flags.contains(Flag.LEADING_DIR) ||
                                    string.indexOf('/', stringPos) == -1;
                        }
                        return true;
                    } else if (c == '/' && flags.contains(Flag.PATHNAME)) {
                        stringPos = string.indexOf('/', stringPos);
                        if (stringPos == -1) {
                            return false;
                        }
                        continue;
                    }

                    /* General case, use recursion. */
                    while (stringPos < string.length()) {
                        if (flags.contains(Flag.PERIOD)) {
                            flags = EnumSet.copyOf(flags);
                            flags.remove(Flag.PERIOD);
                        }
                        if (match(pattern, patternPos, string, stringPos, flags)) {
                            return true;
                        }
                        if (string.charAt(stringPos) == '/' && flags.contains(Flag.PATHNAME)) {
                            break;
                        }
                        ++stringPos;
                    }
                    return false;

                case '[':
                    if (stringPos >= string.length()) {
                        return false;
                    }
                    if (string.charAt(stringPos) == '/' && flags.contains(Flag.PATHNAME)) {
                        return false;
                    }
                    if (hasLeadingPeriod(string, stringPos, flags)) {
                        return false;
                    }

                    int result = matchRange(pattern, patternPos, string.charAt(stringPos), flags);
                    if (result == RANGE_ERROR) /* not a good range, treat as normal text */ {
                        break;
                    }

                    if (result == RANGE_NOMATCH) {
                        return false;
                    }

                    patternPos = result;
                    ++stringPos;
                    continue;

                case '\\':
                    if (!flags.contains(Flag.NOESCAPE)) {
                        if (patternPos >= pattern.length()) {
                            c = '\\';
                        } else {
                            c = pattern.charAt(patternPos++);
                        }
                    }
                    break;
            }

            if (stringPos >= string.length()) {
                return false;
            }
            if (c != string.charAt(stringPos) &&
                    !(flags.contains(Flag.CASEFOLD) &&
                    Character.toLowerCase(c) == Character.toLowerCase(string.charAt(stringPos)))) {
                return false;
            }
            ++stringPos;
        }
        /* NOTREACHED */
    }

    private static boolean hasLeadingPeriod(String string, int stringPos, EnumSet<Flag> flags) {
        return string.charAt(stringPos) == '.' && flags.contains(Flag.PERIOD) &&
                (stringPos == 0 ||
                (flags.contains(Flag.PATHNAME) && string.charAt(stringPos - 1) == '/'));
    }

    private static int matchRange(String pattern, int patternPos, char test, EnumSet<Flag> flags) {
        boolean negate, ok;
        char c, c2;

        /*
         * A bracket expression starting with an unquoted circumflex
         * character produces unspecified results (IEEE 1003.2-1992,
         * 3.13.2).  This implementation treats it like '!', for
         * consistency with the regular expression syntax.
         * J.T. Conklin (conklin@ngai.kaleida.com)
         */
        negate = pattern.charAt(patternPos) == '!' || pattern.charAt(patternPos) == '^';
        if (negate) {
            ++patternPos;
        }

        if (flags.contains(Flag.CASEFOLD)) {
            test = Character.toLowerCase(test);
        }

        /*
         * A right bracket shall lose its special meaning and represent
         * itself in a bracket expression if it occurs first in the list.
         * -- POSIX.2 2.8.3.2
         */
        ok = false;
        while (true) {
            if (patternPos >= pattern.length()) {
                return RANGE_ERROR;
            }

            c = pattern.charAt(patternPos++);
            if (c == ']') {
                break;
            }

            if (c == '\\' && !flags.contains(Flag.NOESCAPE)) {
                c = pattern.charAt(patternPos++);
            }
            if (c == '/' && flags.contains(Flag.PATHNAME)) {
                return RANGE_NOMATCH;
            }
            if (flags.contains(Flag.CASEFOLD)) {
                c = Character.toLowerCase(c);
            }
            if (pattern.charAt(patternPos) == '-' &&
                    patternPos + 1 < pattern.length() &&
                    (c2 = pattern.charAt(patternPos + 1)) != ']') {
                patternPos += 2;
                if (c2 == '\\' && !flags.contains(Flag.NOESCAPE)) {
                    if (patternPos >= pattern.length()) {
                        return RANGE_ERROR;
                    }
                    c = pattern.charAt(patternPos++);
                }
                if (flags.contains(Flag.CASEFOLD)) {
                    c2 = Character.toLowerCase(c2);
                }
                if (c <= test && test <= c2) {
                    ok = true;
                }
            } else if (c == test) {
                ok = true;
            }
        }

        return ok == negate ? RANGE_NOMATCH : patternPos;
    }
}
