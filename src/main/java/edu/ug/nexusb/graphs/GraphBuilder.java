git checkout sub-team-d
git pull origin sub-team-d
mvn exec:java -Dexec.args="--init-db"
mvn clean test
git add src/main/java/edu/ug/nexusb/graphs/GraphBuilder.java
git add src/test/java/edu/ug/nexusb/graphs/GraphBuilderTest.java
git commit -m "graphs: implement DB-backed graph builder (T038)"
git push origin sub-team-d
