#! /bin/sh

dir=$(dirname $0)
cd $dir

java -cp 'build:libs/*:*' ca.dioo.java.motqueser.Motqueser $@
