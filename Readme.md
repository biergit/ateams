###### Build teams based on sympathy between candidates and other criteria 

This is a project to play with [Google OR-Tools](https://developers.google.com/optimization), Kotlin and Docker.

Run
1. `init.sh` to build a base image capable of running OR-Tools.
2. `mvn package` to create your `ateams` image.
3. `docker run --name ateams ateams`.

Lots of todos exists like writing tests, structuring the code, maybe adding some documentation and of course extending the feature set
