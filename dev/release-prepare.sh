#!/bin/sh

function warn_and_exit {
	echo "$1" 1>&2
	exit 1
}

function warn_print_usage_and_exit {
	echo "$1" 1>&2
	echo "" 1>&2
	echo "Usage: $0 MATSCI_DIR TYPE VERSION JAVA_HOME" 1>&2
	exit 1
}

function init_env() {
	echo "********************** Initializing environment ************************"
	TMP=/tmp

	# /mnt/scratch is a tmpfs mount point for faster builds on schubby
	if [ -w "/mnt/scratch" ] ; then
		TMP=/mnt/scratch
	fi

	MATSCI_DIR=`readlink -f $1`
	TYPE=$2
	VERSION=$3
	JAVA_HOME=$4
	if [ ! -z "$5" ] ; then
		TMP=$5
	fi

	TMP_DIR=""

	echo " [i] MATSCI_DIR: $MATSCI_DIR"
	echo " [i] TYPE:          $TYPE"
	echo " [i] VERSION:       $VERSION"
	echo " [i] JAVA_HOME:     $JAVA_HOME"
	echo " [i] TMP:           $TMP"

	if [ -z "$MATSCI_DIR" ] ; then
		warn_print_usage_and_exit "MATSCI_DIR is not defined"
	fi
	if [ -z "$TYPE" ] ; then
			warn_print_usage_and_exit "TYPE is not defined"
		fi

	if [ -z "$VERSION" ] ; then
		warn_print_usage_and_exit "VERSION is not defined"
	fi

	if [ -z "$JAVA_HOME" ] ; then
		warn_print_usage_and_exit "JAVA_HOME is not defined"
	fi

	export JAVA_HOME=${JAVA_HOME}

	# name of the directory that contains the full scheduling content  (also set in release-create.sh)
	MATSCI_FULL_NAME=${TYPE}_Connector-${VERSION}_full
}

function copy_to_tmp() {
	echo "********************** Copying the product to tmp dir ************************"
	TMP_DIR="${TMP}/${MATSCI_FULL_NAME}"
	output=$(mkdir ${TMP_DIR} 2>&1)
	if [ "$?" -ne 0 ] ; then
		if [ -e ${TMP_DIR} ] ; then
			echo " [w] ${TMP_DIR} already exists. Delete it !"
			rm -rf ${TMP_DIR}
			mkdir ${TMP_DIR}
			if [ "$?" -ne 0 ] ; then
				warn_and_exit "Cannot create ${TMP_DIR}: $output"
			fi
		else
			warn_and_exit "Cannot create ${TMP_DIR}"
		fi
	fi

	echo Copying files to $TMP_DIR
	cp -Rf ${MATSCI_DIR}/* ${TMP_DIR}
	#cp -Rf ${MATSCI_DIR}/.classpath ${TMP_DIR}
	#cp -Rf ${MATSCI_DIR}/.project ${TMP_DIR}
}

function replace_version() {
	echo "********************** Replacing version ************************"
	cd ${TMP_DIR} || warn_and_exit "Cannot move in ${TMP_DIR}"

	sed -i "s/{version}/$VERSION/" README.txt
}

function build_and_clean() {
	echo "********************** Cleaning the product ************************"

	cd ${TMP_DIR} || warn_and_exit "Cannot move in ${TMP_DIR}"
	if [ "$(find src/ -name "*.java" | xargs grep serialVersionUID | grep -v `echo $VERSION | sed 's@\(.\)\.\(.\)\..@\1\2@'` | wc -l)" -gt 0 ] ; then
		if [ -z "${RELAX}" ] ; then
			find src/ -name "*.java" | xargs grep serialVersionUID | grep -v `echo $VERSION | sed 's@\(.\)\.\(.\)\..@\1\2@'`
			warn_and_exit " [E] Previous files does not define proper serialVersionUID !"
		fi
	fi

	# Subversion & Git
	find . -type d -a -name ".svn" -exec rm -rf {} \;
	# MatSci temp files
    find . -type d -a -name ".PAScheduler" -exec rm -rf {} \;


	echo "********************** Building the product ***********************"
	# Replace version tag in main java file
	sed -i "s/{matsci-version-main}/$VERSION/" src/matsci/src/org/ow2/proactive/scheduler/ext/matsci/Main.java

	cd compile || warn_and_exit "Cannot move in compile"
	./build clean
	if [ "$TYPE" = "Matlab" ] ; then
	    ./build -Dversion="${VERSION}" deploy.matlab deploy.plugin.matlab
	    ./build -Dversion="${VERSION}" deploy.Matlab.docs
	else
	    ./build -Dversion="${VERSION}" deploy.scilab deploy.plugin.scilab
	    ./build -Dversion="${VERSION}" deploy.Scilab.docs
	fi


	echo "********************** Building the product ***********************"

	cd ${TMP_DIR} || warn_and_exit "Cannot move in ${TMP_DIR}"
	echo " [i] Clean"

	# Git
	rm -rf .git

	# matsci tmp files
	find . -type d -a -name ".PAScheduler" -exec rm -rf {} \;

	# Remove useless parts of ProActive
	find . -type f -a -name "*.svg" -exec rm {} \; # svg are converted in png by hands

	find . -type f -a -name "*.pgm" -exec rm {} \; # svg are converted in png by hands

	# Remove non GPL stuff
	rm -rf ./compile/lib/clover.*

	# Remove temporary files
	rm -f compile/junit*properties
	rm -rf classes/
	rm -rf docs/tmp/

	rm -f *.log
	rm -f *.lck
	rm -f *.iml
	rm -f *.tst
	rm -f *.xml

	# Remove dev directory
	rm -rf dev/
}



init_env $*
copy_to_tmp
replace_version
build_and_clean
