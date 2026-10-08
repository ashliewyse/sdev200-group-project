#!/bin/sh
set -eu
task_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
sh "$task_dir/build.sh"
task_cp="$task_dir/build:$task_dir/lib/gson-2.14.0.jar"
javac --release 17 -Xlint:all,-classfile -cp "$task_cp" -d "$task_dir/build" "$task_dir"/tests/*.java
java -cp "$task_cp" GameRulesTest
java -cp "$task_cp" HttpApiTest
