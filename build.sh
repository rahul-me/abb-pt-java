mkdir build
javac -d "build" -cp lib\nrjavaserial-3.11.0.jar src\com\abb\evci\payment\*.java src\com\gridscape\nayax\log\LogUtility.java src\com\gridscape\nayax\*.java
cd build
jar -cf abb_pt_java.jar com\abb\evci\payment\*.class com\gridscape\nayax\log\LogUtility.class com\gridscape\nayax\*.class
rm -rf com
cd ..
cp lib\nrjavaserial-3.11.0.jar build\