SbtTestTasks="coverage lock test it:test coverageReport coverageAggregate scalastyle"

alias \
  sbt="${0:a:h}/sbt" \
  sbtTestAll="sbt $SbtTestTasks " \
;
