---
layout: default
title: NBGit | Git Support for NetBeans
---

<p class="title">{{page.title}}</p>

NbGit is a module for the NetBeans IDE that adds support for working with the
Git version control system. It uses the JGit library created as part of EGit to
interact with Git repositories. Because the module is Java code all the way, it
should work better cross-platform modulo platform specific differences, such as
file system behavior. It is based on the NetBeans Mercurial module.

### Official Oracle-developed Plugin

Oracle have now begun development of an official NetBeans Git plugin. You can
follow the development of this plugin on the NetBeans wiki:

 - <http://netbeans.org/projects/versioncontrol/pages/Git_main>

You can also subscribe to the mailing list for more detailed discussion of the
plugin's development:

 -  git@versioncontrol.netbeans.org

Code for the plugin has already landed in the NetBeans "silver" repository.
Unfortunately, NetBeans and official plugins are not developed with Git, but
with Mercurial (hg). To clone the repository:

    hg clone http://hg.netbeans.org/main-silver/

Please note: the repository is large and cloning can take a considerable amount
of time.
