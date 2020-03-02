# Ubuntu 18.04 based base image that contains the needed glibc library in the right version
FROM openjdk:8-jdk-buster
# Get the ortools binaries and make them available inside the "ortools" directory
RUN curl -s -L https://github.com/google/or-tools/releases/download/v7.5/or-tools_ubuntu-18.04_v7.5.7466.tar.gz \
| tar xz -C /tmp \
&& mkdir /ortools \
&& mv /tmp/or-tools_Ubuntu-18.04-64bit_v7.5.7466/lib/* /ortools