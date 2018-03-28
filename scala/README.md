

```
java -Xms8g -Xmx8g \
  -Djava.net.preferIPv4Stack=true \
  \                                             ###### ADD SOME -D parameters to configure the test behaviour
  -cp conf:`echo lib/*.jar | sed 's/ /:/g'` \
  io.gatling.app.Gatling \
  -s com.datastax.alexott.bootcamp.GatlingLoadSim \
  -rf results \                                 ###### Write the results to this folder (called results)
  \                                             ###### Optionally add -nr to NOT generate a report
  -on bootcamp-poc-gatling

```

or

```
~/work/bootcamp-code/scala/run-test.sh -DconcurrentSessionCount=100 -DtestDuration=5 -DrampUpTime=1
```

