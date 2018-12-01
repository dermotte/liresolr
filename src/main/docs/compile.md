# Compiling LireSolr
*Version: 7.5.0*

The whole build system has been changed to gradle. So basically you just need an IDE with [Gradle](https://gradle.org/) support and you are ready to go. I am using [Jetbrains IntelliJ IDEA](https://www.jetbrains.com/idea/), which is great for me, but Eclipse and NetBeans do support Gradle. 

## The Purist Way 
1. Install [Gradle](https://gradle.org/)
2. Run `gradle jar` in the root directory, where also the `build.gradle` file is found
3. Proceed with installing the resulting jar from `build/libs` to your Solr installation.

## The IDE way
1. Install [Gradle](https://gradle.org/)
2. Use *Open* or *Import* from your IDE and point it to the `build.gradle` file.
3. Use your IDE to build the jar
4. Proceed with installing the resulting jar from `build/libs` to your Solr installation.