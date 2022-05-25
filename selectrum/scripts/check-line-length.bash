#!/usr/bin/env bash

set -e
set -o pipefail

find=(
    find .
    -name .git -prune -o
    -name "*.elc" -o
    -name "*.png" -o
    -type f -print
)

readarray -t files < <("${find[@]}" | sed 's#./##' | sort)

set +o pipefail

for file in "${files[@]}"; do
    echo "[longlines] $file" >&2
    cat "${file}"                                      \
        | nl -ba                                       \
        | sed '/[l]onglines-start/,/longlines-stop/d'  \
        | grep -E -v 'https?://'                       \
        | grep -E $'\t.{80}'                           \
        | sed -E "s# *([0-9]+)\t#${file}:\1:#"
done | (! grep .)
