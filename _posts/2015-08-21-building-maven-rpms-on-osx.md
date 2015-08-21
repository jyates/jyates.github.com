---
layout: post
title: Building RPMs from Maven projects on OSX
location: San Francisco, CA
subtitle:
description: Packaging software is a necessary evil, and for enterprise software RPMs even more so, but you might as well find out how to manage it when you really want to just work all from your Mac.
tags: osx, rpm, centos, vagrant, build, maven
---

Packaging software is a necessary evil, and for enterprise software RPMs even more so, but you might as well find out how to manage it when you really want to just work all from your Mac.

# Starting Out

The quick and easy solution would be to leverage the rpm (and rpmbuild) tool can be installed via homebrew and the [rpm-maven-plugin](http://www.mojohaus.org/rpm-maven-plugin). Unfortunately, this is in no way recommended as a way to build packages for production. Futher, I've found that any package I built could not be installed on a CentOS box, even though it was built with no specific architecture; when the RPM is packaged, the source operating system was imprinted on the RPM, preventing it from being installed.

# Preparing for production

I still recommend using the [rpm-maven-plugin](http://www.mojohaus.org/rpm-maven-plugin) - its pretty convenient and works mostly as expected, nothing crazy to point out here (beyond a decently documented maven component!).

To build packages for production, you really need to build the software in an environment matching that on which you will run. This ensures correctly library linking and native bits. During my time at Salesforce, we were actually building on a completely different OS than we were running and had only gotten lucky that the same C-library was used on both the build and production operating systems (specifically, we were building snappy). We only found the issue when we upgraded production and suddenly our packages no longer worked as expected! Lesson learned.

Fortunately, there is a solution for this, and its come quite a long way in the last few years: [Vagrant](https://www.vagrantup.com)!

Originally, I wasn't even going to post this because it was so very simple to spin up a VM - a mere two hours from conception to working instance. However, there were some subtleties that are worth pointing out.

My target OS is Centos 6.5.3 - the latest commonly available CentOS release - as I'm trying to build for RHEL, but don't want to spend the money (at least right now) on a RHEL subscription.

## Build requirements

The build I was working with required a few elements:

 * rpmbuild
 * maven 3
 * java 8
 * protobuf 2.6.1
 * custom forks of open source libraries

rpmbuild is a standard yum package, but from there the components become increasingly more complicated: maven needs to downloaded from a release tarball, java8 requires a special cookie on download and protobuf requires a full build from source (as of writing, only 2.3.1 is readily available).

As its just me, I've set to estabilish an accessible maven repository, so my custom forks are instead leveraged from the local .m2 repository. As such, I've also linked my local .m2 repository to the VM's path. The upside to this is that we don't need to rebuild or redownload any jars when we want to build our project, instead relying on the ones stored on the host OS (it saves space, time and bandwidth). Remember, this is fine because compiled java jars are OS agnostic, so we can leverage the same jars regardless of whence they originated.

Here are the scripts I used:

{% highlight ruby %}
# -*- mode: ruby -*-
# vi: set ft=ruby :
VAGRANTFILE_API_VERSION = "2" 

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
  # Default provider VirtualBox
  config.vm.box = "CentOS-6.5-x86_64"
  config.vm.box_url = "https://github.com/2creatives/vagrant-centos/releases/download/v6.5.3/centos65-x86_64-20140116.box"

  config.vm.provider "virtualbox" do |vb|
      vb.memory = 2048
      vb.customize ["modifyvm", :id, "--ioapic", "on", "--cpus", 2]
  end 
  config.vm.provider "parallels" do |prl|
      prl.customize ["set", :id, "--memsize", 1024]
      prl.customize ["set", :id, "--cpus", 2]
  end 

  # add the maven repository as a synced directory. Saves from rebuilding custom dependencies and downloading the internet again
  config.vm.synced_folder "~/.m2/", "/home/vagrant/.m2"

  config.vm.provision "shell", path: "provision.sh"
end
{% endhighlight %}


and the provisioning script:
 
{% highlight bash %}
# !/bin/bash
# Provisioning file for the Vagrant-based VM for building RPMs
# This will install all the necessary software to build the rpm

set -o nounset

function install {
  rpm -qa | grep -q $1
  if [ $? -ne 0 ]; then
    echo "Installing $1 ..."
    sudo yum install -y $1
  fi  
}

function add_to_path {
  echo "export PATH=${1}:\$PATH" >> ~vagrant/.bashrc
}

function install_mvn {
  ls -1 /opt/apache-maven-$1 &> /dev/null
  if [ $# -ne 0 ]; then
    echo "Installing Apache Maven $1"
    cd /tmp &&
    wget -q http://archive.apache.org/dist/maven/binaries/apache-maven-$1-bin.tar.gz &&
    cd /opt &&
    sudo tar -xzf /tmp/apache-maven-$1-bin.tar.gz

    add_to_path "/opt/apache-maven-$1/bin/"
  fi  
}

function install_protobuf {
  if [ $# -eq 0 ]; then
   echo "No value given for protobuf installation!"
   exit 1;
  fi  
  echo "Installing Google Protocol Buffers $1"
  protobuf=protobuf-$1
  cd /tmp &&
  echo "-> downloading..." &&
  wget -q https://github.com/google/protobuf/releases/download/v$1/$protobuf.tar.gz &&
  sudo tar -xzf /tmp/$protobuf.tar.gz &&
  cd $protobuf &&
  echo "-> configuring..." &&
  ./configure &> protobuf-configure.log &&
  echo "-> building..." &&
  make &>  protobuf-make.log &&
  echo "-> installing..." &&
  sudo make install &> protobuf-install.log
  
  add_to_path "/usr/local/bin/"
  echo "... done!"
}

install rpm-build
install wget

echo "Installing java8..."
JAVA_RPM=jdk-8u60-linux-x64.rpm
wget -q --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u60-b27/$JAVA_RPM
install $JAVA_RPM

install_mvn 3.0.4

# protoc
install gcc-c++
install_protobuf 2.6.1


echo "Provisioning completed."
{% endhighlight %}
