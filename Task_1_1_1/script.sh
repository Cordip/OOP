SHORT_PATH="app/src/main/java/sorter"
javac -d app/myBuild/classes $SHORT_PATH/*.java
jar --create --file app/myBuild/jar/App.jar --main-class=sorter.App -C app/myBuild/classes .
javadoc -d app/myBuild/javadoc $SHORT_PATH/*.java 
echo "Programm for sorting array of integers with given length" > app/myBuild/scr-doc.txt
cat app/myBuild/scr-doc.txt
java -jar app/myBuild/jar/App.jar