#!/bin/zsh

source "${0:a:h}/.common.sh"

pushd `dirname "${0:a:h}"`
sbtTestAll
popd
