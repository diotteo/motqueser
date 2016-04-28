Compiling:

-Get monitor-lib.jar
-make all

Running:

-rename dir.conf.sample to dir.conf and write the path to your directory as the first line
-make run ARGS='--help'

In another terminal, run the client to add messages:
-make run ARGS='-p {server-port} -c {client-message}'
