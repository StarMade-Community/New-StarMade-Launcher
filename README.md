# StarMade Launcher Version 3

This is the launcher for [StarMade](https://www.star-made.org/). It is a Java application that will download and install
StarMade for you. It will also keep StarMade up to date and can be used to switch branches.
The launcher can also automatically download and install the correct Java version as well. Game versions below 0.300.100
use Java 8, while versions 0.300.100 and above use Java 23.
This launcher was created due to the old one being outdated and only being able to use Java 7.


# Running a Server

1. Command


# Building from source

1. Clone the Repo (duh)
    1. run `https://github.com/garretreichenbach/New-StarMade-Launcher.git`, or
    2. Clone `https://github.com/garretreichenbach/New-StarMade-Launcher` in GitHub Desktop
2. Package the project
    1. run `gradlew package` in the root directory of the project. This will create an executable in `./release-builds/<OS>/StarMade-Launcher/`
        Note: The package task will only package for your specific platform. Additionally, some platforms may require additional setup to package for.

# Credits

- Schine for making such a great game
- The StarLoader Team for making a new launcher
  Note: Pretty much every class in `src/starmade` was copied over from the main repo.
