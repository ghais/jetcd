#!/bin/bash

#This is form the original jetcd at github.com/justinsb/jetcd

mkdir -p external
cd external

git clone https://github.com/coreos/etcd
cd etcd
echo "Building etcd"
./build
echo "Done!"

nohup bash -c './etcd &'
