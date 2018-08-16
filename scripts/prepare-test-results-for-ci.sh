#!/bin/zsh

if [ -n "$SHIPPABLE" ]; then
  ls modules/*/target/test-reports/* 1> /dev/null 2>&1
  if [ $? -eq 0 ]; then
    mkdir -p shippable/testresults
    cp modules/*/target/test-reports/* $PWD/shippable/testresults/
  fi
  cat target/scala-2.12/coverage-report/cobertura.xml 1> /dev/null 2>&1
  if [ $? -eq 0 ]; then
    mkdir -p shippable/codecoverage
    cp target/scala-2.12/coverage-report/cobertura.xml $PWD/shippable/codecoverage/
 fi
fi
