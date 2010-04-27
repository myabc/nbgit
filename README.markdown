NetBeans Git Module
===================

A versioning plugin for working with Git repositories in Netbeans. Presently,
it supports the basic tasks of status, diff, commit, and log viewing. Future
versions will increase functionality to provide a full set of 'porcelain'
commands.

It uses the JGit library for accessing repositories. To ease installation
a JGit jar is distributed with the project source in release/modules/ext/.

More information and documentation are available in the form of JavaHelp files
which can be found under:

  - javahelp/org/nbgit/docs/

They will also be accessible via the Help menu when the plugin has been
installed.

To download the latest version, check the current status of development, or
report an issue visit the project page at:

 - <http://nbgit.googlecode.com/>

Installation
------------

This module is still under development and thus may randomly crash, eat
all your memory, etc. So consider yourself warned! Before installing or
upgrading make sure you read the release notes and list of known issues.
They can be found in:

 - javahelp/org/nbgit/docs/news.html
 - javahelp/org/nbgit/docs/issues.html

To install from source you need to install the "NetBeans Plugin Development"
plugin from the plugins menu. Afterwards clone the repository and use
File > Open Project to add it to your project list in Netbeans. Then right
click on the new project and select "Install/Reload in Development IDE". You
are advised to first test the plugin by "running" the project.

Getting the Source
------------------

Performing a git clone on either of the following repositories will get you
the latest source:

    git clone git://github.com/myabc/nbgit.git
    git clone git://gitorious.org/nbgit/mainline.git (on gitorious)

The following additional mirrors are available:

    git://repo.or.cz/nbgit.git
    http://repo.or.cz/r/nbgit.git

Licensing and Copyright
-----------------------

This code is dual-licensed under the **COMMON DEVELOPMENT AND DISTRIBUTION
LICENSE (CDDL) Version 1.0** and the **GNU Public License (GPL) v2**. Please see
LICENSE for licensing and copyright information.

Support
-------

 * **nbgit**: <http://nbgit.org/>
 * **nbgit Mailing List**: <http://groups.google.com/group/nbgit>
