script_dir=$(dirname "$BASH_SOURCE")
echo "Building base image in $script_dir ..."
docker build -t ateams-base "$script_dir"
echo "Creating temporary container ..."
id=$(docker create ateams-base)
echo "Copying ortools.jar to $script_dir/lib ..."
mkdir -p "$script_dir/lib"
docker cp $id:/ortools/com.google.ortools.jar "$script_dir/lib"
echo "Removing container $id ..."
docker rm -v $id > /dev/null 2>&1