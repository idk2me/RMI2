BUILD_DIR="build"
LIB_DIR="build/lib"
SERVER_BUILD_DIR="$BUILD_DIR"

echo "Starting RMI registry..."
rmiregistry -J-Djava.class.path=$BUILD_DIR:$LIB_DIR &

echo "Starting server..."
java -cp $SERVER_BUILD_DIR server.Server

echo "Cleaning up..."
pkill rmiregistry
