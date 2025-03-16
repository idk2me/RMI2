mkdir -p build

echo "Compiling Java files..."
find src -name "*.java" >sources.txt
javac -d build @sources.txt

rm sources.txt
