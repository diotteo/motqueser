# motqueser
motion capture helper server

Helper server to motion: http://lavrsen.dk/foswiki/bin/view/Motion/WebHome

Android client: https://play.google.com/store/apps/details?id=ca.dioo.android.motqueser_client


## Compiling

-Get libmotqueser.jar, dioo-commons.jar, java-getopt.jar
-make dist -j
-Unpack the tarball in build/dist/ where you want or run motqueser.sh directly
from the dev environment

## Running

-rename motqueser.conf.sample to motqueser.conf and configure your
installation as per the instructions

-Run the server: ./motqueser.sh [args...]
-For help: ./motqueser.sh --help

In another terminal, run the client to add messages:
-./motqueser.sh -c {ID of the motion event to send into the queue}
