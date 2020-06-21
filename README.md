# FitNesse Plugin for Jenkins


This plugin can be used to both execute and report on [FitNesse](http://fitnesse.org/) tests so that they can be integrated
into a Jenkins build.  
Contributions are welcome, both bug fixes and new features. Just raise a pull request via GitHub.

[CI Server](https://ci.jenkins.io/job/Plugins/job/fitnesse-plugin/)

## Configuration

#### **Global**

-   You could define a JDK installation (not mandatory). All JDKs will
    be available in the job configuration.

#### **Slave node**

-   If your job runs on a slave and launch FitNesse, you should add the
    HOST\_NAME environment variable in slave configuration and set its
    value to the slave’s hostname or IP. You can name it as
    FITNESSE\_HOST\_NAME (whatever you like). This environment variable
    will be used when you set up fitnesse instance below (replace
    localhost by $FITNESSE\_HOST\_NAME).

![](https://wiki.jenkins.io/download/attachments/43090053/fitnesse_config_slave_1.png?version=1&modificationDate=1415698337000&api=v2)

-   You could also override JDK location used, by set a *Tool location*:

![](https://wiki.jenkins.io/download/attachments/43090053/fitnesse_config_slave_2.png?version=1&modificationDate=1415698373000&api=v2)

or by overridden JAVA\_HOME environment variable.

## Usage

#### **Project settings in build step**

-   **For existing FitNesse instance**: host and port where FitNesse is
    running

![](https://wiki.jenkins.io/download/attachments/43090053/fitnesse_config_job_1.png?version=1&modificationDate=1415698337000&api=v2)

-   **For new FitNesse instance**:
    -   **JDK**: selected JDK, JVM args and Java working directory
    -   **Paths**: fitnesse.jar and FitNesseRoot path
    -   **Fitnesse**: port use and command line args

![](https://wiki.jenkins.io/download/attachments/43090053/fitnesse_config_job_2.png?version=1&modificationDate=1415698337000&api=v2)

-   **In all cases:**
    -   Target page
    -   HTTP and test timeout
    -   Results file name

![](https://wiki.jenkins.io/download/attachments/43090053/fitnesse_config_job_3.png?version=1&modificationDate=1415698337000&api=v2)

#### **Project settings in post-build step**

-   Results file name: the name of the result file ; if there is several
    files, you can use wildcards.

![](https://wiki.jenkins.io/download/attachments/43090053/fitnesse_config_job_post.png?version=1&modificationDate=1415698337000&api=v2)

## Result

-   On project page : a new chart with result trend and a little summary

![](https://wiki.jenkins.io/download/attachments/43090053/fitnesse_result_summary.png?version=1&modificationDate=1415698372000&api=v2)

-   All tests result in a page:

![](https://wiki.jenkins.io/download/attachments/43090053/fitnesse_result_detail.png?version=1&modificationDate=1415698373000&api=v2)

-   And finally, captured details of a test:

![](https://wiki.jenkins.io/download/attachments/43090053/fitnesse_result_catured.png?version=1&modificationDate=1415698373000&api=v2)

## Todo

-   ![(star)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/star_yellow.svg)
    Run fitnesse tests using "-c" option when starting new fitnesse
    instance
-   ![(star)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/star_yellow.svg)
    Add more control over fitnesse start-up params
-   ![(star)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/star_yellow.svg)
    Using glob to collect (potentially) multiple results.xml files
-   ![(star)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/star_yellow.svg)
    Nest Sub-suites and tests-within-suites within the uber-parent
    FitnesseResults instance
-   ![(star)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/star_yellow.svg)
    Allow direct URL access to sub-suites and tests-within-suites so
    that every level can have its history graph
-   ![(star)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/star_yellow.svg)
    Run multiple test suites from one project configuration

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/fitnesse-plugin/master)](https://ci.jenkins.io/job/Plugins/job/fitnesse-plugin/job/master/)

## Change Log

Older versions of this plugin may not be safe to use. Please review the
following warnings before using an older version:

-   **1.33** (2019-09-19)
    -   **Fixed**: [Stored XSS vulnerability](https://jenkins.io/security/advisory/2020-04-07/#SECURITY-1801)
-   **1.31** (2019-09-19)
    -   **Fixed**: [XXE vulnerability](https://jenkins.io/security/advisory/2020-02-12/#SECURITY-1751)
-   **1.30** (2019-09-19)
    -   **Fixed**: [JENKINS-58923](https://issues.jenkins-ci.org/browse/JENKINS-58923) Cannot browse result of tests when publishing several fitnesse result files on Windows
    -   **Fixed**:
        [JENKINS-58430](https://issues.jenkins-ci.org/browse/JENKINS-58430) Publishing
        two fitnesse xml files with same test name confuses the results
    -   Removed some log warnings
-   **1.29** (2019-08-11)
    -   ****![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)**** Added: Changes
        to read and write results in distributed Jenkins
        ([PR-37](https://github.com/jenkinsci/fitnesse-plugin/pull/37))
        (Fixes:  Fitnesse should run on the slave of the build, not on
        the head node
         [JENKINS-13696](https://issues.jenkins-ci.org/browse/JENKINS-13696))
-   **1.28 **(2019-01-12)
    -   ****![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)**** Added: Produces
        junit report results from the fitnesse results
        ([PR-36](https://github.com/jenkinsci/fitnesse-plugin/pull/36))
-   **1.27 **(2018-12-20)
    -   **![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)** Fixed:
        FitNesse history is not rendering on builds generated by latest
        plugin versions
-   **1.25** (2018-12-05)  
    -   **![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)** Fixed:
        FitNesse history not rendering in some cases
-   **1.24** (2018-11-08)
    -   **![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg) **Added:
        Support for remote FitNesse over HTTPS 
        ([PR-27](https://github.com/jenkinsci/fitnesse-plugin/pull/27))
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg) Added:
        Add environment variables support for fitnesse hostname and port
        ([PR-28](https://github.com/jenkinsci/fitnesse-plugin/pull/28))
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg) Added:
        Feature/pipeline compatibility
        ([PR-30](https://github.com/jenkinsci/fitnesse-plugin/pull/30))
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg) Improvement:
        Try to gracefully terminate the running test in case of an
        exception
        ([PR-31](https://github.com/jenkinsci/fitnesse-plugin/pull/31))
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg) Added:
        Ability to access to a protected remote Fitnesse
        ([PR-32](https://github.com/jenkinsci/fitnesse-plugin/pull/32))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg) Fixed:
        Fixes for
        [JEP-200](https://jenkins.io/blog/2018/01/13/jep-200/) ([PR-35](https://github.com/jenkinsci/fitnesse-plugin/pull/35))
-   **1.16** (2015-06-26)
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg) Fixed FitNesse
        1.13 does not render properly FitNesse history
        ([JENKINS-29019](https://issues.jenkins-ci.org/browse/JENKINS-29019))
-   **1.15** (2015-06-22)
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Improvement: Add ability to define fitnesse port as enironment
        variable
        ([JENKINS-27955](https://issues.jenkins-ci.org/browse/JENKINS-27955))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: FitNesse History doesn't render properly
        ([JENKINS-29019](https://issues.jenkins-ci.org/browse/JENKINS-29019))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Clean code: remove useless library, remove warnings and
        deprecated methods
-   **1.14** (2015-06-21)
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: In result detail page, can't expand collapsed scenario
        ([JENKINS-27938](https://issues.jenkins-ci.org/browse/JENKINS-27938))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: manage JDK 1.8 (increase core plugin version)
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: manage severals FitNesse test results in the same job
        ([JENKINS-27936](https://issues.jenkins-ci.org/browse/JENKINS-27936) -
        [pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/25))
-   **1.13** (2015-06-02)
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: manage FitNesse old versions (without summary and page
        duration fields in XML result)
        ([JENKINS-28316](https://issues.jenkins-ci.org/browse/JENKINS-28316))
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Improve FitNesse History page: reverse column order & add sort
        on column header
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: use FitNesse plugin with contionnal steps(multiple)
        plugin
        ([JENKINS-21636](https://issues.jenkins-ci.org/browse/JENKINS-21636))
-   **1.12** (2015-03-31)
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Improve captured detail renderer (use FitNesse CSS & JS)
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Added: FitNesse tests history page ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/23))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: execution of test page ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/22))
-   **1.11** (2014-11-10)
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Improve result table and captured detail renderer ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/19))
-   **1.10** (2014-10-27)
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: avoid OOM on hudge result files ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/16))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: support Jenkins slave with a different OS than master's
        one ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/18))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: avoid NPE when no JDK is defined in global configuration
        ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/18))
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Added: retrieve and display duration for all tests ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/17))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: test port availability to check if FitNesse is started
        instead of scraping stdout ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/13))
-   **1.9** (2014-03-26)
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: launch FitNesse if no JDK is configured in Jenkins ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/11))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Improve performance on huge result file ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/10))
-   **1.8** (2013-10-21)
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Added support for Jenkins slaves ([pull
        request](https://github.com/swestcott/fitnesse-plugin/pull/8))
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Expose FitNesse tests results throught Jenkins API ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/2))
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Selectable JDK for FitNesse ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/3))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Configuration bugfix ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/4))
-   **1.7**
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: avoid NPE when build aborts prematurely and produces no
        results ([pull
        request](https://github.com/swestcott/fitnesse-plugin/pull/6))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Use the HTTP timeout inside the connection ([pull
        request](https://github.com/jenkinsci/fitnesse-plugin/pull/1))
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Don't show broken image when no test results are available
        ([pull
        request](https://github.com/swestcott/fitnesse-plugin/pull/7))
-   **1.6**
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Added support for multiple FitNesse reports and drilling down
        into HTML output ([pull
        request](https://github.com/swestcott/fitnesse-plugin/pull/4))
-   **1.5**
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Report exceptions as failures
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Upgrade minimum Jenkins version from 1.353 to 1.401 to benefit
        from bug fixes in hudson.Proc
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Added support for FitNesse options -d, -r & -p.
-   **1.4**
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Added ability to specify path to fitnesse.jar and path to
        FitNesseRoot relative to the workspace
-   **1.3.1**
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed bug where counts with X right and Y ignores were being
        treated as ignored not right
-   **1.3**
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Added ability to specify HTTP timeout (default: 60,000 ms)
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Added ability to specify java working directory (default:
        location of fitnesse.jar)
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Added page or suite name to build page summary link
-   **1.2**
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Added prettier tabular format for results
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Results file without path will be written to / read from
        workspace
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Log incremental console output as FitNesse results are coming in
        over HTTP
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: Unable to unpack fitnesse.jar
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: Build hangs when http get stalls
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: Unexpected EOF while reading http bytes
        `catch IOException`
-   **1.1**
    -   ![(thumbs
        up)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/thumbs_up.svg)
        Fixed: unable to parse xml with BOM: error
        "`content is not allowed in prolog`"
-   **1.0**
    -   ![(plus)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/add.svg)
        Brand new
        ![(smile)](https://wiki.jenkins.io/s/en_GB/8100/5084f018d64a97dc638ca9a178856f851ea353ff/_/images/icons/emoticons/smile.svg)
