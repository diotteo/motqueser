#! /bin/sh

dir=$(dirname $0)
cd $dir

java -cp '*' ca.dioo.java.SurveillanceServer.Server $@
