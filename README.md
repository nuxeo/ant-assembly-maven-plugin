# Introduction

This plugins provides an advanced Ant integration within Maven, in the manner of 
[nuxeo-distribution-tools](https://github.com/nuxeo/nuxeo-distribution-tools) itself inspired from
[maven-antrun-extended-plugin](https://java.net/projects/maven-antrun-extended-plugin).  
It extends [maven-antrun-plugin] with Maven related tasks for managing artifacts.

The main functionality is the management of Maven artifacts graph.

[http://www.eclipse.org/aether](http://www.eclipse.org/aether)

## Available Ant tasks

[All standard Ant tasks](http://ant.apache.org/manual/tasklist.html) are available. 

[Ant contrib tasks](http://ant-contrib.sourceforge.net/) are also included.

All the Nuxeo Ant tasks are listed in [src/main/resources/org/nuxeo/build/antlib.xml](src/main/resources/org/nuxeo/build/antlib.xml) and 
[src/main/resources/org/nuxeo/build/artifact/antlib.xml](src/main/resources/org/nuxeo/build/artifact/antlib.xml). Here's quick listing:

- nx:templates (org.nuxeo.build.ant.ftl.ProcessTemplateTask)
- nx:profile (org.nuxeo.build.ant.profile.ProfileTask)
- nx:archetype (org.nuxeo.build.ant.archetype.ArchetypeTask)
- nx:if (org.nuxeo.build.ant.IfTask)
- nx:regexp (org.nuxeo.build.ant.PropertyRegexp)
- nx:preprocess (org.nuxeo.build.ant.processor.NuxeoBuildProcessor)
- nx:rename (org.nuxeo.build.ant.RenameTask)
- nx:rmdups (org.nuxeo.build.ant.RemoveDuplicateTask)
- nx:zipdiff (org.nuxeo.build.ant.ZipDiffTask)

- artifact:settings (org.nuxeo.build.ant.artifact.SettingsTask)
- artifact:set (org.nuxeo.build.ant.artifact.ArtifactSet)
- artifact:file (org.nuxeo.build.ant.artifact.ArtifactFile)
- artifact:graph (org.nuxeo.build.ant.artifact.GraphTask)
- artifact:expand (org.nuxeo.build.ant.artifact.ExpandTask)
- artifact:nuxeo-expand (org.nuxeo.build.ant.artifact.NuxeoExpandTask)
- artifact:export (org.nuxeo.build.ant.artifact.GraphExportTask)
- artifact:attach (org.nuxeo.build.ant.artifact.AttachArtifactTask)
- artifact:foreach (org.nuxeo.build.ant.artifact.ArtifactForeach)
- artifact:resolveFile (org.nuxeo.build.ant.artifact.ResolveFile)
- artifact:dependencies (org.nuxeo.build.ant.artifact.ArtifactDependencies)
- artifact:print (org.nuxeo.build.ant.artifact.PrintGraphTask)
- artifact:resolveFiles (org.nuxeo.build.ant.artifact.ResolveFiles)

### Graph tasks

 - `<artifact:graph src="" resolves="" />`  
 `src`: file containing a list of artifact keys (GAV)  
 `resolves`: comma-separated list of artifact keys (GAV)  
 Nested elements: `org.nuxeo.build.ant.artifact.Expand`  
 Build an expanded graph with the given artifacts as root nodes.
 - `<artifact:expand key="" depth="1" >`  
 `key`: the root node. The current graph is used if null.  
 `depth`: expansion depth (default="1"). Set "all" to expand at max. 
 Nested elements: `org.nuxeo.build.maven.filter.AndFilter`  
 Expand artifact nodes in the current graph if key is null or in the graph which key is root node.


### Artifact File Resources

Artifact file resources are used to select the file for the specified
artifacts.

You can use classifiers if you want a specific file.

There are four artifact file resource types:

    <artifact:file>         -> selects a single artifact
    <artifact:resolveFile>  -> selects a single remote artifact that is not specified by the graph. This is not using the graph but directly the Maven repositories.
    <artifact:set>          -> selects a set of artifacts. Can use includes and excludes clauses (filters are supported).
    <artifact:dependencies> -> selects the dependencies of an artifact (the depth can be controlled and filters are supported).


`<artifact:file>` have the following attributes:

  - groupId
  - artifactId
  - version
  - type
  - classifier
  - key

You must specify at least the 'key' attribute or one or more of the other
attributes.

If both key and other attributes are specified the 'key' take precedence.

The key format is the same as the node artifact key format described above.

Example:

    <artifact:file key="nuxeo-runtime"> will get the file of the first artifact found having the artifactId == "nuxeo-runtime"

    <artifact:file key="org.nuxeo.runtime:nuxeo-runtime"> will get the file of the first artifact found having the groupId == "org.nxueo.runtime" and artifactId == "nuxeo-runtime"

    <artifact:file key="nuxeo-runtime;allinone"> - the ';' is a shortcut to be able to specify the classifier inside a node key.

    <artifact:file artifactId="nuxeo-runtime" classifier="allinone"> this is identical to the previous example.

Note: using 'key' may generate faster lookups. (it's a prefix search on a tree map).

Example:

    <copy todir="${maven.project.build.directory}">
    <artifact:file artifactId="nuxeo-runtime"/>
    <artifact:dependencies artifactId="nuxeo-runtime">
      <excludes>
        <artifact scope="test"/>
        <artifact scope="provided"/>
      </excludes>
    </artifact:dependencies>
    </copy>


# Usage

## Examples

The whole [nuxeo-distribution](https://github.com/nuxeo/nuxeo-distribution/)
project is using nuxeo-distribution-tools for building Nuxeo distributions, running tests, ...

Look at `nuxeo-distribution/*/pom.xml` and
`nuxeo-distribution/*/src/main/assemble/assembly.xml` files for concrete usage samples.

## Basics

The following Maven properties are exposed in the Ant build file with a `maven.` prefix:

  - basedir -> maven.basedir
  - project.name -> maven.project.name
  - project.artifactId -> maven.project.artifactId
  - project.groupId -> maven.project.groupId
  - project.version -> maven.project.version
  - project.packaging -> maven.project.packaging
  - project.id -> maven.project.id
  - project.build.directory -> maven.project.build.directory
  - project.build.outputDirectory -> maven.project.build.outputDirectory
  - project.build.finalName -> maven.project.build.finalName

Any user defined Maven property will be available as an Ant property.

For every active Maven profile, a property of the following form is created:

    maven.profile.X = true

where X is the profile name.

This can be used in conditional Ant constructs like:

    <target if="maven.profile.X">

or

    <target unless="maven.profile.X">

to make task execution depending on whether a profile is active or not.

Maven profiles are also exported as Ant profiles so you can use the custom
nx:profile tasks to conditionally execute code. Example:

    <nx:profile name="X">
      ... that statement will be executed only if profile X is active ...
    </nx:profile>

The current Maven POM (project) is put as a root into the artifact graph.

If expand > 0, then the project node will be expanded using a depth equals to the
expand property. Example: if you use expand=1, then the direct dependencies of 
the project are added to the graph.

## Thread safety

Different Mojo instances can be used in different threads, each of them will
have its own graph. (The Mojo is bound to a thread variable so that Ant will
use the Mojo bound to the current thread).


# Build and tests

    mvn clean package [-o] [-DskipTests] [-DskipITs] [-DdebugITs=true] [-Dinvoker.test=...] [-Pdebug]

See:  
- [maven-invoker-plugin](http://maven.apache.org/plugins/maven-invoker-plugin/)  
- [maven-surefire-plugin](http://maven.apache.org/surefire/maven-surefire-plugin/)

## Build and run all Unit and integration tests

    mvn clean integration-test [-o] [-DdebugITs=true]

## Build with no test

    mvn clean package -DskipTests -DskipITs

## Build and run Unit tests only (default)

    mvn clean package -DskipITs
    
## Build and run integration tests only

    mvn clean integration-test -DskipTests

## Run only some integration tests

    mvn invoker:run -Dinvoker.test=test1
    
or (if you want the code being compiled again):
    
    mvn clean integration-test -Dinvoker.test=test1

Use comma separator. Wildcards are accepted. 

## Integration tests results

Results are in target/it/* sub-folders.

## Integration tests debug

Add `-DdebugITs=true` on the command line

## Use mvnDebug (start the JVM in debug attach mode)

Use `-Pdebug` profile

Default listening for transport dt_socket at address: 8000

