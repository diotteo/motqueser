#! /bin/sh

dir=$(dirname $0)
cd $dir

java -cp 'dist/Server/*:*' ca.dioo.java.SurveillanceServer.Server $@
