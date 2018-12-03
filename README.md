# Distributed Failure Detector

- Run Introducer first on master node, via:
~~~
./gradlew mmpIntro
~~~

- Run Membership server on all other nodes via:
~~~
./gradlew mmpServer
~~~

- See local IP in Command line interface:

~~~
self
~~~

- See all membership list in Command line interface:

~~~
status
~~~

- Leave the group voluntarily in CLI. Note: this would only decommission the current node.

~~~
decommission
~~~

- Sender & Receiver's log files are created in the parent directory of this mp

~~~shell
//Assume you are in mp2-weilinz2-ys26
cat ../receiver.log
Or
cat ../sender.log
~~~
