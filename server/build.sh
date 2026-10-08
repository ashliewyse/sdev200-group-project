#!/bin/sh
set -eu
task_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
mkdir -p "$task_dir/build"
javac --release 17 -Xlint:all,-classfile -cp "$task_dir/lib/gson-2.14.0.jar" -d "$task_dir/build" "$task_dir"/src/*.java
echo 'Server compiled successfully.'
