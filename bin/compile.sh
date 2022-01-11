#!/bin/bash

javac -cp "..lib/*.jar" ./*/*.java
javac -cp "..lib/*.jar" ./*/*/*.java
javac -cp "..lib/*.jar" ./*/*/*/*.java
