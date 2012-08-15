#! /bin/bash

CP=src
for JAR in `ls lib/*.jar`
do
  CP=$CP:$JAR
done

groovy -cp $CP shell.groovy