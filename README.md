*Bad Clade Deletion* (BCD) Supertrees is a *java* library and command line tool providing the
Bad Clade Deletion supertree algorithm for rooted input trees.
It provides several strategies to weight the clades that have to be removed during the micut phase.
Bad Clade Deletion (BCD) Supertrees can use the *Greedy Strict Consensus Merger* (GSCM) algorithm
to preprocess the input trees. For the GSCM algorithm it provides several scoring functions to determine
in which oder the input trees get merged. Combining different scorings is also implemented as well as a
randomized version of the algorithm. For more detailed information about the algorithm see the Literature.

### Literature


[1] Markus Fleischauer and Sebastian Böcker.   
**Bad Clade Deletion Supertrees: A Fast and Accurate Supertree Algorithm.**   
*[Mol Biol Evol, 34:2408-2421, 2017](https://academic.oup.com/mbe/article/34/9/2408/3925278)*

[2] Markus Fleischauer and Sebastian Böcker.   
**BCD Beam Search: Considering suboptimal partial solutions in Bad Clade Deletion supertrees**   
*in review*

[3] Markus Fleischauer and Sebastian Böcker.   
**Collecting reliable clades using the Greedy Strict Consensus Merger**   
*[PeerJ (2016) 4:e2172](https://peerj.com/articles/2172/)*


# Download Links

BCD Supertrees commandline tool v1.1.3
* for [Windows](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/phylo/bcd-cli/bcdSupertrees-1.1.3-Win.zip)
* for [Linux/Unix/Mac](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/phylo/bcd-cli/bcdSupertrees-1.1.3-Nix.zip)
* as [jar file](https://bio.informatik.uni-jena.de/repository/dist-release-local/de/unijena/bioinf/phylo/bcd-cli/bcdSupertrees-1.1.3-Jar.zip)

The **Source Code** can be found on [GitHub](https://github.com/boecker-lab/bcd-supertrees)

# Installation

## Windows

The bcdSupertrees.exe should hopefully work out of
the box. To execute BCD from every location you have to add the
location of the *bcdSupertrees.exe* to your **PATH** environment variable.


## Linux and MacOSX

To start BCD Supertrees you just have to start the *bcd* start script from the command line:

    /path/to/bcd/bcd LIST_OF_BCD_ARGS

The BCD Supertrees directory contains another start script named bcdVM. This script allows you to run 
BCD Supertrees with specific JVM (Java Virtual Machine) arguments.
The command to run the bcdVM start script type:

    /path/to/bcd/bcdVM "LIST_OF_JVM_ARGS" "LIST_OF_BCD_ARGS"

To execute BCD Supertrees from every location you have to add the location of
the BCD start script to your **PATH** variable. Open the file `~/.profile`
in an editor and add the following line (replacing the placeholder
path):

    export PATH-$PATH:/path/to/bcd
   
   
## Jar (any OS)

Alternatively, you can run the jar file using java with the
command:

    java -jar /path/to/bcdSupertrees/bcdSupertrees.jar


# Running BCD Supertrees command line tool

You can always use the `--help` option to get a documentation about
the available commands and options.

Generally you only need to specify the input trees as input.
If your input data contains bootstrap values we recommend the *BOOTSTRAP_VALUES*
weighting
Other options are listet below or be see via `--help` option

## Supported Filtypes

The BCD Supertrees command line tool handles trees in **NEWICK** and **NEXUS** format.
For an automatic file format detection use the common file extension
for **NEWICK** *(tree|TREE|tre|TRE|phy|PHY|nwk|NWK)* and **NEXUS** *(nex|NEX|ne|NE|nexus|NEXUS)*.
Per default the output tree format equals the input format. To specify a different
output format you can use the option `--outFileType` or the short form`-d`.

## Supported Commands


### Usage:
```
bcd [options...] INPUT_TREE_FILE
    The only required argument is the input tree file
 
bcd [options...] INPUT_TREE_FILE GUIDE_TREE_FILE
    Additionally, a guide tree can be specified. Otherwise the GSCM tree will be calculated as default guide tree
```
### General options:

```
  PATH                                        : Path of the file containing the input
                                                trees
  PATH                                        : Path of the file containing the guide
                                                tree
  -H (--HELP)                                 : Full usage message including
                                                nonofficial Options (default: false)
  -O (--fullOutput) PATH                      : Output file containing full output
  -V (--VERBOSE)                              : many console output
  -b (--bootstrapThreshold) N                 : Minimal bootstrap value of a
                                                tree-node to be considered during the
                                                supertree calculation (default: 0)
  -d (--outFileType) [NEXUS | NEWICK | AUTO]  : Output file type (default: AUTO)
                                       
  -f (--fileType) [NEXUS | NEWICK | AUTO]     : Type of input files and if not
                                                specified otherwise also of the
                                                output file (default: AUTO)
  -h (--help)                                 : usage message (default: true)
  -j (--supportValues)                        : Calculate Split Fit for every clade
                                                of the supertree(s)  (default: false)
  -o (--outputPath) PATH                      : Output file
  -p (--workingDir) PATH                      : Path of the working directory. All
                                                relative paths will be rooted here.
                                                Absolute paths are not effected
  -s (--scm) VALUE                            : Use SCM-tree as guide tree (default:
                                                true)
  -v (--verbose)                              : some more console output
  -w (--weighting) [UNIT_WEIGHT |             : Weighting strategy
     TREE_WEIGHT | BRANCH_LENGTH |             
     BOOTSTRAP_WEIGHT | LEVEL |                
     BRANCH_AND_LEVEL | BOOTSTRAP_AND_LEVEL]                                         
  -t (--threads) N                            : Set a positive number of Threads that
                                                should be used
  -T (--singleThreaded)                       : starts in single threaded mode, equal
                                                to "-t 1"
  -B (--disableProgressbar)                   : Disables progress bar (cluster/backgro
                                                und mode)
```

# BCD Java Library


You can integrate the BCD library in your java project, either by
using Maven [1] or by including the jar file directly. The latter is
not recommended, as the BCD jar contains also dependencies to other
external libraries.


## Maven Integration


Add the following repository to your pom file:

```xml
   <distributionManagement>
     <repository>
         <id>bioinf-jena</id>
         <name>bioinf-jena-releases</name>
         <url>https://bio.informatik.uni-jena.de/repository/libs-releases-local</url>
     </repository>
   </distributionManagement>
```
Now you can integrate BCD in your project by adding the following
dependency:

Library containing all algorithms

```xml
   <dependency>
     <groupId>de.unijena.bioinf.phylo</groupId>
     <artifactId>flipcut-lib</artifactId>
     <version>1.1.1</version>
   </dependency>
```
Whole project containing the algorithm (bcd-lib) and the command line interface (bcd-cli)

```xml
   <dependency>
     <groupId>de.unijena.bioinf.phylo</groupId>
     <artifactId>flipcut</artifactId>
     <version>1.1.1</version>
   </dependency>
```

## Main API (WIP)


The main class in the BCD library is `phylo.tree.algorithm.flipcut.AbstractFlipCut`.
It specifies the main API of all provided algorithm implementation. To run the algorithm you
just have to specify the input trees.

There is currently 1 implementation of `phylo.tree.algorithm.flipcut.AbstractFlipCut`:


### Algorithm Implemetation(s):

`phylo.tree.algorithm.flipcut.FlipCutSingleCutSimpleWeight`

This class provides the basic Bad Clade Deletion algorithm.
Parameters:
* **input** -- List of rooted input trees.
* **weight** -- clade weighting to use

Returns:
   The bcd supertree


### Clade Weightings:

The interface `phylo.tree.algorithm.flipcut.costComputer.FlipCutWeights` provides
different weightings. The package `phylo.tree.algorithm.flipcut.costComputer` contains implementations of these weightings.

```
UNIT_WEIGHT
TREE_WEIGHT
BRANCH_LENGTH
BOOTSTRAP_VALUES
LEVEL
BRANCH_AND_LEVEL
BOOTSTRAP_AND_LEVEL
```


**The in Fleischauer et al. [1] presented scorings are:**

```
UNIT_WEIGHT
BRANCH_LENGTH
BOOTSTRAP_VALUES
```

# Changelog
### 1.1.2
* better cut sampling
* upper bound for cut sampling (optimal cut)
* much less memory consuption with cut sampling (as good as Vazirani)
* improved performance for cut sampling (character merging)       
* bug with too less iterations in recursive cut sampling fixed
* recursive cut sampling algorithm is now the default cut sampling algorithm

### 1.1.1
* Completely new and memory efficient data structure for BCD Graph
    * The Beam Search agorithm can now run on a standard notebook (even for serveral thousand taxa).     
* Multiple bugfixes in the Beam Search

### 1.1
* Beam Search algorithm to consider suboptimal partial soulutions in BCD algorithm
    * Cut Enumeration (Vaziranis Algorthm)
    * Cut Sampling

### 1.0.1
* Low Overlap: BCD now returns a warning for input tree sets with low overlap instead of not calculating them.
* bcd now supports both bootstrap notations of the newick file format.
* some minor fixes

### 1.0
* release version

## Acknowledgments
* We thank Stefano Scerra for providing us [his implementation](http://www.stefanoscerra.it/java-max-flow-graph-algorithm/) of the Ahuja-Orlin max flow algorithm
