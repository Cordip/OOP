SHORT_PATH="app/src/main/java/org/graph"
javac -d app/myBuild/classes $SHORT_PATH/*.java
jar --create --file app/myBuild/jar/App.jar --main-class=org.graph.App -C app/myBuild/classes .
javadoc -d app/myBuild/javadoc $SHORT_PATH/*.java 
java -jar app/myBuild/jar/App.jar