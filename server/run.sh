#!/bin/sh
set -eu
task_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
sh "$task_dir/build.sh"
exec java -cp "$task_dir/build:$task_dir/lib/gson-2.14.0.jar" GameServer "$@"
