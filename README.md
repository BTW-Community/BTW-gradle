# BTW-gradle

BTW-gradle is a development environment for the Better Than Wolves mod

## ⚠️ DO NOT CLONE THIS REPOSITORY, READ THE README AND DOWNLOAD THE CORRECT ZIP ⚠️

## Quick Start

* Download [the example](https://github.com/BTW-Community/BTW-gradle/archive/refs/heads/example.zip)
* Change `settings.gradle` and `build.gradle` to your liking
* Run gradlew build

## BTW Source Code

To get access to the Better Than Wolves source code, build your project and look in `build/minecraft`. The [BTW-Public repository](https://github.com/BTW-Community/BTW-Public) also offers its own way to generate source if you just need access to the source.

## Addon Development

BTW-gradle was developed in IntelliJ IDEA and it's also the environment I recommend. After [downloading and unpacking the example](https://github.com/BTW-Community/BTW-gradle/archive/refs/heads/example.zip), just point IDEA at the top folder (the one that has build.gradle) and it should pick up Gradle by itself.

Next, you want to look at `settings.gradle` and configure it to your liking. You can also override these settings in `build.gradle` (where the Cthulhu references are in the `btw {}` block.

Next, you want to open the Gradle panel on and find the `build` task in the `btw-gradle` section and run it. This will take about 5-10 minutes, after which the example should be compiled and ready to run (it only takes this long the first time and every time you clean the build directory, as it needs to decompile and patch the Minecraft source code).

If you look at the `build/distributions` folder, you will notice the zip file that you can use to share your mod with the world. It will be named according to what you have set up previously.

## Eclipse

I am waiting for someone with experience with Eclipse and Gradle to look into the project and figure out how to set it up. Currently it looks like Eclipse is not able to handle a complex project like BTW-gradle expects with multiple source sets.

## Troubleshooting

**Gradlew is taking a long time**

It's supposed to the first time. It takes about 5 minutes on my machine to produce the first build. After that it should be drastically faster.

**I have a weird bug I can't track down**

Try running the `clean` task, either from your IDE or by running `gradlew clean`.

**I get dependency errors**

Clean your build first, like in the previous question, then run `gradlew --refresh-dependencies`.
